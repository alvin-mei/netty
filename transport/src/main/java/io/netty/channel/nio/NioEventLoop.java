/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.channel.nio;

import io.netty.channel.*;
import io.netty.util.IntSupplier;
import io.netty.util.concurrent.RejectedExecutionHandler;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.ReflectionUtil;
import io.netty.util.internal.SystemPropertyUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * {@link SingleThreadEventLoop} implementation which register the {@link Channel}'s to a
 * {@link Selector} and so does the multi-plexing of these in the event loop.
 * 继承 SingleThreadEventLoop 抽象类，NIO EventLoop 实现类，
 * 实现对注册到其中的 Channel 的就绪的 IO 事件，和对用户提交的任务进行处理。
 */
public final class NioEventLoop extends SingleThreadEventLoop {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(NioEventLoop.class);

    private static final int CLEANUP_INTERVAL = 256; // XXX Hard-coded value, but won't need customization.
    // 是否禁用 SelectionKey 的优化，默认开启
    private static final boolean DISABLE_KEYSET_OPTIMIZATION =
            SystemPropertyUtil.getBoolean("io.netty.noKeySetOptimization", false);
    // 少于该 N 值，不开启空轮询重建新的 Selector 对象的功能
    private static final int MIN_PREMATURE_SELECTOR_RETURNS = 3;
    //  NIO Selector 空轮询该 N 次后，重建新的 Selector 对象
    private static final int SELECTOR_AUTO_REBUILD_THRESHOLD;

    private final IntSupplier selectNowSupplier = new IntSupplier() {
        @Override
        public int get() throws Exception {
            return selectNow();
        }
    };
    private final Callable<Integer> pendingTasksCallable = new Callable<Integer>() {
        @Override
        public Integer call() throws Exception {
            return NioEventLoop.super.pendingTasks();
        }
    };

    // Workaround for JDK NIO bug.
    //
    // See:
    // - http://bugs.sun.com/view_bug.do?bug_id=6427854
    // - https://github.com/netty/netty/issues/203
    static {
        // 解决 Selector#open() 方法
        final String key = "sun.nio.ch.bugLevel";
        final String buglevel = SystemPropertyUtil.get(key);
        if (buglevel == null) {
            try {
                AccessController.doPrivileged(new PrivilegedAction<Void>() {
                    @Override
                    public Void run() {
                        System.setProperty(key, "");
                        return null;
                    }
                });
            } catch (final SecurityException e) {
                logger.debug("Unable to get/set System Property: " + key, e);
            }
        }
        // 初始化
        int selectorAutoRebuildThreshold = SystemPropertyUtil.getInt("io.netty.selectorAutoRebuildThreshold", 512);
        if (selectorAutoRebuildThreshold < MIN_PREMATURE_SELECTOR_RETURNS) {
            selectorAutoRebuildThreshold = 0;
        }

        SELECTOR_AUTO_REBUILD_THRESHOLD = selectorAutoRebuildThreshold;

        if (logger.isDebugEnabled()) {
            logger.debug("-Dio.netty.noKeySetOptimization: {}", DISABLE_KEYSET_OPTIMIZATION);
            logger.debug("-Dio.netty.selectorAutoRebuildThreshold: {}", SELECTOR_AUTO_REBUILD_THRESHOLD);
        }
    }

    /**
     * The NIO {@link Selector}.
     * NIO中的多路复用器Selector
     */
    private Selector selector;
    private Selector unwrappedSelector;
    // 就绪事件的键值对，优化时使用
    private SelectedSelectionKeySet selectedKeys;
    // selector的工厂
    private final SelectorProvider provider;

    /**
     * Boolean that controls determines if a blocked Selector.select should
     * break out of its selection process. In our case we use a timeout for
     * the select method and the select method will block for that time unless
     * waken up.
     * 唤醒标记，由于select()方法会阻塞
     */
    private final AtomicBoolean wakenUp = new AtomicBoolean();
    // 选择策略
    private final SelectStrategy selectStrategy;
    // IO任务占总任务(IO+普通任务)比例
    private volatile int ioRatio = 50;
    // 取消的键数目
    private int cancelledKeys;
    private boolean needsToSelectAgain;

    NioEventLoop(NioEventLoopGroup parent, Executor executor, SelectorProvider selectorProvider,
                 SelectStrategy strategy, RejectedExecutionHandler rejectedExecutionHandler) {
        // 调用父类的构造方法
        // DEFAULT_MAX_PENDING_TASKS 任务队列初始化容量值，默认值为：Integer.MAX_VALUE
        // 若不想使用默认值的话，那么就得自己配置 io.netty.eventLoop.maxPendingTasks 属性值为自己想要的值
        super(parent, executor, false, DEFAULT_MAX_PENDING_TASKS, rejectedExecutionHandler);
        if (selectorProvider == null) {
            throw new NullPointerException("selectorProvider");
        }
        if (strategy == null) {
            throw new NullPointerException("selectStrategy");
        }
        // 这个对象在NioEventLoopGroup的构造函数中通过SelectorProvider.provider()获得，然后一路传参到此类
        provider = selectorProvider;
        // 通过调用JDK底层类库，为每个NioEventLoop配备一个多路复用器
        final SelectorTuple selectorTuple = openSelector();
        selector = selectorTuple.selector;
        unwrappedSelector = selectorTuple.unwrappedSelector;
        selectStrategy = strategy;
    }

    private static final class SelectorTuple {
        // 未包装的 Selector 对象
        final Selector unwrappedSelector;
        final Selector selector;

        SelectorTuple(Selector unwrappedSelector) {
            this.unwrappedSelector = unwrappedSelector;
            this.selector = unwrappedSelector;
        }

        SelectorTuple(Selector unwrappedSelector, Selector selector) {
            this.unwrappedSelector = unwrappedSelector;
            this.selector = selector;
        }
    }

    private SelectorTuple openSelector() {
        // 创建Selector对象，作为 unwrappedSelector
        final Selector unwrappedSelector;
        try {
            // 通过 provider 调用底层获取一个多路复用器对象
            unwrappedSelector = provider.openSelector();
        } catch (IOException e) {
            throw new ChannelException("failed to open a new selector", e);
        }
        // 禁用 SelectionKey 的优化，则直接返回 SelectorTuple 对象。
        // 即，selector 也使用 unwrappedSelector 。
        if (DISABLE_KEYSET_OPTIMIZATION) {
            return new SelectorTuple(unwrappedSelector);
        }
        // 执行到此，说明需要优化选择器集合，首先创建一个选择器集合
        Object maybeSelectorImplClass = AccessController.doPrivileged(new PrivilegedAction<Object>() {

            // 然后通过反射找到SelectorImpl对象
            @Override
            public Object run() {
                try {
                    // 成功，则返回该类
                    return Class.forName(
                            "sun.nio.ch.SelectorImpl",
                            false,
                            PlatformDependent.getSystemClassLoader());
                } catch (Throwable cause) {
                    return cause;
                }
            }
        });

        // 获得 SelectorImpl 类失败，则直接返回 SelectorTuple 对象。
        // 即，selector 也使用 unwrappedSelector 。
        if (!(maybeSelectorImplClass instanceof Class) ||
                // ensure the current selector implementation is what we can instrument.
                !((Class<?>) maybeSelectorImplClass).isAssignableFrom(unwrappedSelector.getClass())) {
            if (maybeSelectorImplClass instanceof Throwable) {
                Throwable t = (Throwable) maybeSelectorImplClass;
                logger.trace("failed to instrument a special java.util.Set into: {}", unwrappedSelector, t);
            }
            return new SelectorTuple(unwrappedSelector);
        }

        final Class<?> selectorImplClass = (Class<?>) maybeSelectorImplClass;
        // 创建 SelectedSelectionKeySet 对象
        final SelectedSelectionKeySet selectedKeySet = new SelectedSelectionKeySet();

        // 设置 SelectedSelectionKeySet 对象到 unwrappedSelector 中
        Object maybeException = AccessController.doPrivileged(new PrivilegedAction<Object>() {
            @Override
            public Object run() {
                try {
                    // 获得 "selectedKeys" "publicSelectedKeys" 的 Field
                    Field selectedKeysField = selectorImplClass.getDeclaredField("selectedKeys");
                    Field publicSelectedKeysField = selectorImplClass.getDeclaredField("publicSelectedKeys");

                    // 设置 Field 可访问
                    Throwable cause = ReflectionUtil.trySetAccessible(selectedKeysField, true);
                    if (cause != null) {
                        return cause;
                    }
                    cause = ReflectionUtil.trySetAccessible(publicSelectedKeysField, true);
                    if (cause != null) {
                        return cause;
                    }

                    // 设置 SelectedSelectionKeySet 对象到 unwrappedSelector 的 Field 中
                    selectedKeysField.set(unwrappedSelector, selectedKeySet);
                    publicSelectedKeysField.set(unwrappedSelector, selectedKeySet);
                    return null;
                } catch (NoSuchFieldException e) {
                    return e;
                } catch (IllegalAccessException e) {
                    return e;
                }
            }
        });

        // 设置 SelectedSelectionKeySet 对象到 unwrappedSelector 中失败，
        // 则直接返回 SelectorTuple 对象。即，selector 也使用 unwrappedSelector 。
        if (maybeException instanceof Exception) {
            selectedKeys = null;
            Exception e = (Exception) maybeException;
            logger.trace("failed to instrument a special java.util.Set into: {}", unwrappedSelector, e);
            return new SelectorTuple(unwrappedSelector);
        }
        selectedKeys = selectedKeySet;
        logger.trace("instrumented a special java.util.Set into: {}", unwrappedSelector);
        // 创建 SelectedSelectionKeySetSelector 对象
        // 创建 SelectorTuple 对象。即，selector 也使用 SelectedSelectionKeySetSelector 对象。
        return new SelectorTuple(unwrappedSelector,
                new SelectedSelectionKeySetSelector(unwrappedSelector, selectedKeySet));
    }

    /**
     * Returns the {@link SelectorProvider} used by this {@link NioEventLoop} to obtain the {@link Selector}.
     */
    public SelectorProvider selectorProvider() {
        return provider;
    }

    //创建任务队列
    @Override
    protected Queue<Runnable> newTaskQueue(int maxPendingTasks) {
        // This event loop never calls takeTask()
        // 由于默认是没有配置io.netty.eventLoop.maxPendingTasks属性值的，所以maxPendingTasks默认值为Integer.MAX_VALUE；
        // 那么最后配备的任务队列的大小也就自然使用无参构造队列方法
        return maxPendingTasks == Integer.MAX_VALUE ? PlatformDependent.<Runnable>newMpscQueue()
                : PlatformDependent.<Runnable>newMpscQueue(maxPendingTasks);
    }

    // 获得待执行的任务数量
    // 因为 MpscQueue 仅允许单消费，所以获得队列的大小，仅允许在 EventLoop 的线程中调
    @Override
    public int pendingTasks() {
        // As we use a MpscQueue we need to ensure pendingTasks() is only executed from within the EventLoop as
        // otherwise we may see unexpected behavior (as size() is only allowed to be called by a single consumer).
        // See https://github.com/netty/netty/issues/5297
        if (inEventLoop()) {
            return super.pendingTasks();
        } else {
            return submit(pendingTasksCallable).syncUninterruptibly().getNow();
        }
    }

    /**
     * Registers an arbitrary {@link SelectableChannel}, not necessarily created by Netty, to the {@link Selector}
     * of this event loop.  Once the specified {@link SelectableChannel} is registered, the specified {@code task} will
     * be executed by this event loop when the {@link SelectableChannel} is ready.
     * <p>
     * 注册 Java NIO Channel ( 不一定需要通过 Netty 创建的 Channel )到 Selector 上，相当于说，也注册到了 EventLoop 上
     * 用户自定义处理
     */
    public void register(final SelectableChannel ch, final int interestOps, final NioTask<?> task) {
        if (ch == null) {
            throw new NullPointerException("ch");
        }
        if (interestOps == 0) {
            throw new IllegalArgumentException("interestOps must be non-zero.");
        }
        if ((interestOps & ~ch.validOps()) != 0) {
            throw new IllegalArgumentException(
                    "invalid interestOps: " + interestOps + "(validOps: " + ch.validOps() + ')');
        }
        if (task == null) {
            throw new NullPointerException("task");
        }

        if (isShutdown()) {
            throw new IllegalStateException("event loop shut down");
        }

        try {
            ch.register(selector, interestOps, task);
        } catch (Exception e) {
            throw new EventLoopException("failed to register a channel", e);
        }
    }

    /**
     * Returns the percentage of the desired amount of time spent for I/O in the event loop.
     */
    public int getIoRatio() {
        return ioRatio;
    }

    /**
     * Sets the percentage of the desired amount of time spent for I/O in the event loop.  The default value is
     * {@code 50}, which means the event loop will try to spend the same amount of time for I/O as for non-I/O tasks.
     */
    public void setIoRatio(int ioRatio) {
        if (ioRatio <= 0 || ioRatio > 100) {
            throw new IllegalArgumentException("ioRatio: " + ioRatio + " (expected: 0 < ioRatio <= 100)");
        }
        this.ioRatio = ioRatio;
    }

    /**
     * Replaces the current {@link Selector} of this event loop with newly created {@link Selector}s to work
     * around the infamous epoll 100% CPU bug.
     */
    public void rebuildSelector() {
        // 只允许在 EventLoop 的线程中执行
        if (!inEventLoop()) {
            execute(new Runnable() {
                @Override
                public void run() {
                    rebuildSelector0();
                }
            });
            return;
        }
        rebuildSelector0();
    }

    private void rebuildSelector0() {
        final Selector oldSelector = selector;
        // 创建新的 Selector 对象
        final SelectorTuple newSelectorTuple;

        if (oldSelector == null) {
            return;
        }
        // new一个新的selector，将之前注册到老的selector上的的channel重新转移到新的selector上
        try {
            newSelectorTuple = openSelector();
        } catch (Exception e) {
            logger.warn("Failed to create a new Selector.", e);
            return;
        }

        // Register all channels to the new Selector.
        int nChannels = 0;
        // 将注册在 NioEventLoop 上的所有 Channel ，注册到新创建 Selector 对象上
        for (SelectionKey key : oldSelector.keys()) {
            Object a = key.attachment();
            try {
                // 拿到有效的key
                if (!key.isValid() || key.channel().keyFor(newSelectorTuple.unwrappedSelector) != null) {
                    continue;
                }

                int interestOps = key.interestOps();
                // 取消该key在旧的selector上的事件注册
                key.cancel();
                // 将该key对应的channel注册到新的selector上
                SelectionKey newKey = key.channel().register(newSelectorTuple.unwrappedSelector, interestOps, a);
                if (a instanceof AbstractNioChannel) {
                    // Update SelectionKey
                    // 重新绑定channel和新的key的关系
                    ((AbstractNioChannel) a).selectionKey = newKey;
                }
                nChannels++;
            } catch (Exception e) {
                logger.warn("Failed to re-register a Channel to the new Selector.", e);
                // 关闭发生异常的 Channel
                if (a instanceof AbstractNioChannel) {
                    AbstractNioChannel ch = (AbstractNioChannel) a;
                    ch.unsafe().close(ch.unsafe().voidPromise());
                } else {
                    // 调用 NioTask 的取消注册事件
                    @SuppressWarnings("unchecked")
                    NioTask<SelectableChannel> task = (NioTask<SelectableChannel>) a;
                    invokeChannelUnregistered(task, key, e);
                }
            }
        }

        // 修改 selector 和 unwrappedSelector 指向新的 Selector 对象
        selector = newSelectorTuple.selector;
        unwrappedSelector = newSelectorTuple.unwrappedSelector;

        // 关闭老的 Selector 对象
        try {
            // time to close the old selector as everything else is registered to the new one
            oldSelector.close();
        } catch (Throwable t) {
            if (logger.isWarnEnabled()) {
                logger.warn("Failed to close the old Selector.", t);
            }
        }

        if (logger.isInfoEnabled()) {
            logger.info("Migrated " + nChannels + " channel(s) to the new Selector.");
        }
    }

    @Override
    protected void run() {
        //reactor线程大概做的事情分为对三个步骤不断循环
        // 1.首先轮询注册到reactor线程对用的selector上的所有的channel的IO事件
        // 2.处理产生网络IO事件的channel
        //  3.处理任务队列
        for (; ; ) {
            try {
                switch (selectStrategy.calculateStrategy(selectNowSupplier, hasTasks())) {
                    case SelectStrategy.CONTINUE:
                        continue;
                    case SelectStrategy.SELECT:
                        //
                        // 选择( 查询 )任务
                        // wakenUp 表示是否应该唤醒正在阻塞的select操作，可以看到netty在进行一次新的loop之前
                        // 都会将wakeUp 被设置成false，标志新的一轮loop的开始，具体的select操作我们也拆分开来看
                        select(wakenUp.getAndSet(false));

                        // 'wakenUp.compareAndSet(false, true)' is always evaluated
                        // before calling 'selector.wakeup()' to reduce the wake-up
                        // overhead. (Selector.wakeup() is an expensive operation.)
                        //
                        // However, there is a race condition in this approach.
                        // The race condition is triggered when 'wakenUp' is set to
                        // true too early.
                        //
                        // 'wakenUp' is set to true too early if:
                        // 1) Selector is waken up between 'wakenUp.set(false)' and
                        //    'selector.select(...)'. (BAD)
                        // 2) Selector is waken up between 'selector.select(...)' and
                        //    'if (wakenUp.get()) { ... }'. (OK)
                        //
                        // In the first case, 'wakenUp' is set to true and the
                        // following 'selector.select(...)' will wake up immediately.
                        // Until 'wakenUp' is set to false again in the next round,
                        // 'wakenUp.compareAndSet(false, true)' will fail, and therefore
                        // any attempt to wake up the Selector will fail, too, causing
                        // the following 'selector.select(...)' call to block
                        // unnecessarily.
                        //
                        // To fix this problem, we wake up the selector again if wakenUp
                        // is true immediately after selector.select(...).
                        // It is inefficient in that it wakes up the selector for both
                        // the first case (BAD - wake-up required) and the second case
                        // (OK - no wake-up required).
                        // 唤醒。原因
                        if (wakenUp.get()) {
                            selector.wakeup();
                        }
                        // fall through
                    default:
                }

                cancelledKeys = 0;
                needsToSelectAgain = false;
                final int ioRatio = this.ioRatio;
                if (ioRatio == 100) {
                    try {
                        // 处理Channel感兴趣的就绪IO事件
                        processSelectedKeys();
                    } finally {
                        // 运行所有普通任务和定时任务，不限制时间
                        // Ensure we always run tasks.
                        runAllTasks();
                    }
                } else {
                    final long ioStartTime = System.nanoTime();
                    try {
                        // 处理Channel感兴趣的就绪IO事件
                        processSelectedKeys();
                    } finally {
                        // 运行所有普通任务和定时任务，限制时间
                        // Ensure we always run tasks.
                        final long ioTime = System.nanoTime() - ioStartTime;
                        runAllTasks(ioTime * (100 - ioRatio) / ioRatio);
                    }
                }
            } catch (Throwable t) {
                handleLoopException(t);
            }
            // Always handle shutdown even if the loop processing threw an exception.
            try {
                if (isShuttingDown()) {
                    closeAll();
                    if (confirmShutdown()) {
                        return;
                    }
                }
            } catch (Throwable t) {
                handleLoopException(t);
            }
        }
    }

    private static void handleLoopException(Throwable t) {
        logger.warn("Unexpected exception in the selector loop.", t);

        // Prevent possible consecutive immediate failures that lead to
        // excessive CPU consumption.
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // Ignore.
        }
    }

    private void processSelectedKeys() {
        if (selectedKeys != null) {
            // 意味着使用优化的 SelectedSelectionKeySetSelector
            processSelectedKeysOptimized();
        } else {
            processSelectedKeysPlain(selector.selectedKeys());
        }
    }

    @Override
    protected void cleanup() {
        try {
            selector.close();
        } catch (IOException e) {
            logger.warn("Failed to close a selector.", e);
        }
    }

    void cancel(SelectionKey key) {
        key.cancel();
        cancelledKeys++;
        if (cancelledKeys >= CLEANUP_INTERVAL) {
            cancelledKeys = 0;
            needsToSelectAgain = true;
        }
    }

    @Override
    protected Runnable pollTask() {
        Runnable task = super.pollTask();
        if (needsToSelectAgain) {
            selectAgain();
        }
        return task;
    }

    // 基于 Java NIO 原生 Selecotr ，处理 Channel 新增就绪的 IO 事件
    private void processSelectedKeysPlain(Set<SelectionKey> selectedKeys) {
        // check if the set is empty and if so just return to not create garbage by
        // creating a new Iterator every time even if there is nothing to process.
        // See https://github.com/netty/netty/issues/597
        if (selectedKeys.isEmpty()) {
            return;
        }

        // 遍历 SelectionKey 迭代器
        Iterator<SelectionKey> i = selectedKeys.iterator();
        for (; ; ) {
            final SelectionKey k = i.next();
            final Object a = k.attachment();
            i.remove();
            // 处理一个Channel就绪的IO事件
            // IO事件由Netty框架处理
            if (a instanceof AbstractNioChannel) {
                processSelectedKey(k, (AbstractNioChannel) a);
                // 使用 NioTask 处理一个 Channel 就绪的 IO 事件
                // IO事件由用户自定义任务处理
                // 这两种处理方式由register()决定
            } else {
                @SuppressWarnings("unchecked")
                NioTask<SelectableChannel> task = (NioTask<SelectableChannel>) a;
                processSelectedKey(k, task);
            }

            // 无下一个节点，结束
            if (!i.hasNext()) {
                break;
            }

            if (needsToSelectAgain) {
                selectAgain();
                selectedKeys = selector.selectedKeys();

                // Create the iterator again to avoid ConcurrentModificationException
                if (selectedKeys.isEmpty()) {
                    break;
                } else {
                    i = selectedKeys.iterator();
                }
            }
        }
    }

    // SelectedSelectionKeySetSelector 处理 Channel 新增就绪的 IO 事件
    private void processSelectedKeysOptimized() {
        // 遍历selectedKey
        for (int i = 0; i < selectedKeys.size; ++i) {
            final SelectionKey k = selectedKeys.keys[i];
            // null out entry in the array to allow to have it GC'ed once the Channel close
            // See https://github.com/netty/netty/issues/2363
            selectedKeys.keys[i] = null;

            final Object a = k.attachment();

            // 处理一个 Channel 就绪的 IO 事件
            if (a instanceof AbstractNioChannel) {
                // Netty框架处理
                processSelectedKey(k, (AbstractNioChannel) a);
            } else {
                // 使用 NioTask 处理一个 Channel 就绪的 IO 事件
                @SuppressWarnings("unchecked")
                NioTask<SelectableChannel> task = (NioTask<SelectableChannel>) a;
                // 用户自定义处理
                processSelectedKey(k, task);
            }
            // 有必要重新选择
            if (needsToSelectAgain) {
                // null out entries in the array to allow to have it GC'ed once the Channel close
                // See https://github.com/netty/netty/issues/2363
                // 将上一次遍历集合中未处理元素置null，帮助GC回收，防止泄露
                selectedKeys.reset(i + 1);
                // 未处理元素也将添加到数组中
                selectAgain();
                i = -1;
            }
        }
    }

    private void processSelectedKey(SelectionKey k, AbstractNioChannel ch) {
        // 如果 SelectionKey 是不合法的，则关闭 Channel
        final AbstractNioChannel.NioUnsafe unsafe = ch.unsafe();
        if (!k.isValid()) {
            final EventLoop eventLoop;
            try {
                eventLoop = ch.eventLoop();
            } catch (Throwable ignored) {
                // If the channel implementation throws an exception because there is no event loop, we ignore this
                // because we are only trying to determine if ch is registered to this event loop and thus has authority
                // to close ch.
                return;
            }
            // Only close ch if ch is still registered to this EventLoop. ch could have deregistered from the event loop
            // and thus the SelectionKey could be cancelled as part of the deregistration process, but the channel is
            // still healthy and should not be closed.
            // See https://github.com/netty/netty/issues/5125
            // channel已不再该EventLoop，直接返回
            if (eventLoop != this || eventLoop == null) {
                return;
            }
            // close the channel if the key is not valid anymore
            // channel还在EventLoop，关闭channel
            unsafe.close(unsafe.voidPromise());
            return;
        }

        try {
            // 获得就绪的 IO 事件的 ops
            int readyOps = k.readyOps();
            // 客户端连接事件
            // We first need to call finishConnect() before try to trigger a read(...) or write(...) as otherwise
            // the NIO JDK channel implementation may throw a NotYetConnectedException.
            if ((readyOps & SelectionKey.OP_CONNECT) != 0) {
                // remove OP_CONNECT as otherwise Selector.select(..) will always return without blocking
                // See https://github.com/netty/netty/issues/924
                // 移除对 OP_CONNECT 感兴趣
                int ops = k.interestOps();
                ops &= ~SelectionKey.OP_CONNECT;
                k.interestOps(ops);
                // 完成连接
                unsafe.finishConnect();
            }

            // OP_WRITE 事件就绪
            // Process OP_WRITE first as we may be able to write some queued buffers and so free memory.
            if ((readyOps & SelectionKey.OP_WRITE) != 0) {
                // Call forceFlush which will also take care of clear the OP_WRITE once there is nothing left to write
                // 向 Channel 写入数据
                // 在写入到 Channel 到对端，若 TCP 数据发送缓冲区已满，这将导致 Channel 不写可，
                // 此时会注册对该 Channel 的 SelectionKey.OP_WRITE 事件感兴趣。从而实现，再在 Channel 可写后，进行强制 flush
                ch.unsafe().forceFlush();
            }

            // SelectionKey.OP_READ 或 SelectionKey.OP_ACCEPT 就绪
            // readyOps == 0 是对 JDK Bug 的处理，防止空的死循环
            // 当 (readyOps & SelectionKey.OP_ACCEPT) != 0 时，这就是服务端 NioServerSocketChannel 的 boss EventLoop 线程轮询到有新的客户端连接接入
            if ((readyOps & (SelectionKey.OP_READ | SelectionKey.OP_ACCEPT)) != 0 || readyOps == 0) {
                // NioMessageUnsafe#read()
                unsafe.read();
            }
        } catch (CancelledKeyException ignored) {
            unsafe.close(unsafe.voidPromise());
        }
    }

    private static void processSelectedKey(SelectionKey k, NioTask<SelectableChannel> task) {
        // 未执行
        int state = 0;
        try {
            // 调用 NioTask 的 Channel 就绪事件
            task.channelReady(k.channel(), k);
            state = 1;
        } catch (Exception e) {
            // SelectionKey 取消
            k.cancel();
            // 执行 Channel 取消注册
            invokeChannelUnregistered(task, k, e);
            state = 2; // 执行异常
        } finally {
            switch (state) {
                case 0: // SelectionKey 取消
                    k.cancel();
                    // 执行 Channel 取消注册
                    invokeChannelUnregistered(task, k, null);
                    break;
                case 1:
                    // SelectionKey 不合法，则执行 Channel 取消注册
                    if (!k.isValid()) { // Cancelled by channelReady()
                        invokeChannelUnregistered(task, k, null);
                    }
                    break;
            }
        }
    }

    private void closeAll() {
        selectAgain();
        Set<SelectionKey> keys = selector.keys();
        Collection<AbstractNioChannel> channels = new ArrayList<AbstractNioChannel>(keys.size());
        for (SelectionKey k : keys) {
            Object a = k.attachment();
            if (a instanceof AbstractNioChannel) {
                channels.add((AbstractNioChannel) a);
            } else {
                k.cancel();
                @SuppressWarnings("unchecked")
                NioTask<SelectableChannel> task = (NioTask<SelectableChannel>) a;
                invokeChannelUnregistered(task, k, null);
            }
        }

        for (AbstractNioChannel ch : channels) {
            ch.unsafe().close(ch.unsafe().voidPromise());
        }
    }

    private static void invokeChannelUnregistered(NioTask<SelectableChannel> task, SelectionKey k, Throwable cause) {
        try {
            task.channelUnregistered(k.channel(), cause);
        } catch (Exception e) {
            logger.warn("Unexpected exception while running NioTask.channelUnregistered()", e);
        }
    }

    //当run方法调用select()方法时，每次都将唤醒标记设置为假，这样线程将阻塞在selector.select(timeoutMillis)方法上。
    // 阻塞期间如果用户使用外部线程提交一个任务，会调用上述的wakeup()方法，由于wakenUp唤醒标记为假，
    // selector.wakeup()方法调用，线程唤醒从下一个break跳出，从而执行提交任务。阻塞期间如果外部线程提交多个任务，
    // 使用wakenUp唤醒标记使selector.wakeup()操作只执行一次，因为它是一个昂贵的操作，从而提高性能
    //
    @Override
    protected void wakeup(boolean inEventLoop) {
        if (!inEventLoop && wakenUp.compareAndSet(false, true)) {
            selector.wakeup();
        }
    }

    Selector unwrappedSelector() {
        return unwrappedSelector;
    }

    int selectNow() throws IOException {
        try {
            // 立即( 无阻塞 )返回 Channel 新增的感兴趣的就绪 IO 事件数量
            return selector.selectNow();
        } finally {
            // 若wakeUp为true 则 唤醒 Selector
            // 会使用我们对Selector的唤醒，所以需要进行复原
            // restore wakeup state if needed
            if (wakenUp.get()) {
                selector.wakeup();
            }
        }
    }

    // 选择( 查询 )任务
    private void select(boolean oldWakenUp) throws IOException {
        // 记录selector对象
        Selector selector = this.selector;
        try {
            // 获得 select 操作的计数器。主要用于记录 Selector 空轮询次数，所以每次在正在轮询完成
            int selectCnt = 0;
            long currentTimeNanos = System.nanoTime();
            // 计算 select 操作的截止时间
            // delayNanos返回的为下一个定时任务距离现在的时间，如果不存在定时任务，则默认返回 1000 ms
            long selectDeadLineNanos = currentTimeNanos + delayNanos(currentTimeNanos);

            // 直到符合如下任一一种情况后结束
            for (; ; ) {
                // 1.定时任务截至事时间快到了，中断本次轮询
                // 计算本次 select 的超时时长
                long timeoutMillis = (selectDeadLineNanos - currentTimeNanos + 500000L) / 1000000L;
                // 如果超时时长，则结束 select
                if (timeoutMillis <= 0) {
                    // 如果是首次 select ，selectNow 一次，非阻塞
                    if (selectCnt == 0) {
                        selector.selectNow();
                        selectCnt = 1;
                    }
                    break;
                }

                // 2.轮询过程中发现有任务加入，中断本次轮询，非阻塞
                //  若有新的任务加入,本次轮询会结束
                // If a task was submitted when wakenUp value was true, the task didn't get a chance to call
                // Selector#wakeup. So we need to check task queue again before executing select operation.
                // If we don't, the task might be pended until select operation was timed out.
                // It might be pended until idle timeout if IdleStateHandler existed in pipeline.
                if (hasTasks() && wakenUp.compareAndSet(false, true)) {
                    // selectNow 一次，非阻塞
                    selector.selectNow();
                    selectCnt = 1;
                    break;
                }

                // 3.阻塞式select操作,查询 Channel 是否有就绪的 IO 事件
                // 说明netty任务队列里面队列为空，并且所有定时任务延迟时间还未到(大于0.5ms)，
                // 于是，在这里进行一次阻塞select操作，截止到第一个定时任务的截止时间
                int selectedKeys = selector.select(timeoutMillis);
                // select 计数器 ++
                selectCnt++;

                // 结束 select ，如果满足下面任一一个条件
                if (selectedKeys != 0 || oldWakenUp || wakenUp.get() || hasTasks() || hasScheduledTasks()) {
                    //  selectedKeys != 0 时，表示有 Channel 新增的就绪的 IO 事件，所以结束 select ，很好理解。
                    // - waken up by user, or oldWakenUp || wakenUp.get() 时，表示 Selector 被唤醒，所以结束 select
                    // - the task queue has a pending task. hasTasks() || hasScheduledTasks() ，表示有普通任务或定时任务，所以结束 select
                    // - a scheduled task is ready for processing 主要是 select 超时或者发生空轮询
                    break;
                }

                // 线程被打断。一般情况下不会出现，出现基本是 bug ，或者错误使用。
                if (Thread.interrupted()) {
                    // Thread was interrupted so reset selected keys and break so we not run into a busy loop.
                    // As this is most likely a bug in the handler of the user or it's client library we will
                    // also log it.
                    //
                    // See https://github.com/netty/netty/issues/2426
                    if (logger.isDebugEnabled()) {
                        logger.debug("Selector.select() returned prematurely because " +
                                "Thread.currentThread().interrupt() was called. Use " +
                                "NioEventLoop.shutdownGracefully() to shutdown the NioEventLoop.");
                    }
                    selectCnt = 1;
                    break;
                }

                // 4.解决jdk的nio bug
                // netty 会在每次进行 selector.select(timeoutMillis) 之前记录一下开始时间currentTimeNanos，在select之后记录一下结束时间，
                // 判断select操作是否至少持续了timeoutMillis秒（这里将time - TimeUnit.MILLISECONDS.toNanos(timeoutMillis) >= currentTimeNanos改成time - currentTimeNanos >= TimeUnit.MILLISECONDS.toNanos(timeoutMillis)或许更好理解一些）,
                //如果持续的时间大于等于timeoutMillis，说明就是一次有效的轮询，重置selectCnt标志，
                // 否则，表明该阻塞方法并没有阻塞这么长时间，可能触发了jdk的空轮询bug，当空轮询的次数超过一个阀值的时候，默认是512
                // select()方法并不阻塞而直接返回且返回值为0，从而出现空轮询使CPU完全耗尽。
                // Netty解决的办法是：对select返回0的操作计数，如果次数大于阈值SELECTOR_AUTO_REBUILD_THRESHOLD就新建一个selector
                // ，将注册到老的selector上的channel重新注册到新的selector上。阈值SELECTOR_AUTO_REBUILD_THRESHOLD可由用户使用系统变量io.netty.selectorAutoRebuildThreshold配置，
                // 默认为512。这里注意for()循环中大量使用了break，含有break的部分才是关键操作，其他部分（其实就只有一处）是为了解决JDK BUG。

                long time = System.nanoTime();
                // 符合 select 超时条件，重置 selectCnt 为 1
                if (time - TimeUnit.MILLISECONDS.toNanos(timeoutMillis) >= currentTimeNanos) {
                    // timeoutMillis elapsed without anything selected.
                    selectCnt = 1;
                    // 不符合 select 超时的提交，若 select 次数到达重建 Selector 对象的上限，进行重建
                    // 这就是 Netty 判断发生 NIO Selector 空轮询的方式，N ( 默认 512 )次 select 并未阻塞超时这么长，
                    // 那么就认为发生 NIO Selector 空轮询。过多的 NIO Selector 将会导致 CPU 100%
                } else if (SELECTOR_AUTO_REBUILD_THRESHOLD > 0 &&
                        selectCnt >= SELECTOR_AUTO_REBUILD_THRESHOLD) {
                    // The selector returned prematurely many times in a row.
                    // Rebuild the selector to work around the problem.
                    logger.warn(
                            "Selector.select() returned prematurely {} times in a row; rebuilding Selector {}.",
                            selectCnt, selector);

                    rebuildSelector();
                    selector = this.selector;

                    // Select again to populate selectedKeys.
                    // 立即 selectNow 一次，非阻塞
                    selector.selectNow();
                    selectCnt = 1;
                    break;
                }

                currentTimeNanos = time;
            }

            if (selectCnt > MIN_PREMATURE_SELECTOR_RETURNS) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Selector.select() returned prematurely {} times in a row for Selector {}.",
                            selectCnt - 1, selector);
                }
            }
        } catch (CancelledKeyException e) {
            if (logger.isDebugEnabled()) {
                logger.debug(CancelledKeyException.class.getSimpleName() + " raised by a Selector {} - JDK bug?",
                        selector, e);
            }
            // Harmless exception - log anyway
        }
    }

    private void selectAgain() {
        needsToSelectAgain = false;
        try {
            selector.selectNow();
        } catch (Throwable t) {
            logger.warn("Failed to update SelectionKeys.", t);
        }
    }
}
