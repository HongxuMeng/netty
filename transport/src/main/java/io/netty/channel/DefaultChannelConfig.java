/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.channel;

import io.netty.buffer.ByteBufAllocator;
import io.netty.util.internal.ObjectUtil;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import static io.netty.channel.ChannelOption.ALLOCATOR;
import static io.netty.channel.ChannelOption.AUTO_CLOSE;
import static io.netty.channel.ChannelOption.AUTO_READ;
import static io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS;
import static io.netty.channel.ChannelOption.MAX_MESSAGES_PER_READ;
import static io.netty.channel.ChannelOption.MAX_MESSAGES_PER_WRITE;
import static io.netty.channel.ChannelOption.MESSAGE_SIZE_ESTIMATOR;
import static io.netty.channel.ChannelOption.RCVBUF_ALLOCATOR;
import static io.netty.channel.ChannelOption.SINGLE_EVENTEXECUTOR_PER_GROUP;
import static io.netty.channel.ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK;
import static io.netty.channel.ChannelOption.WRITE_BUFFER_LOW_WATER_MARK;
import static io.netty.channel.ChannelOption.WRITE_BUFFER_WATER_MARK;
import static io.netty.channel.ChannelOption.WRITE_SPIN_COUNT;
import static io.netty.util.internal.ObjectUtil.checkNotNull;
import static io.netty.util.internal.ObjectUtil.checkPositive;
import static io.netty.util.internal.ObjectUtil.checkPositiveOrZero;

import java.util.logging.Logger;
import org.json.JSONObject;
import org.json.JSONTokener;
// import java.io.FileReader;
// import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Constructor;

/**
 * The default {@link ChannelConfig} implementation.
 */
public class DefaultChannelConfig implements ChannelConfig {
    private Logger LOGGER = Logger.getLogger("InfoLogging");
    private static final MessageSizeEstimator DEFAULT_MSG_SIZE_ESTIMATOR = DefaultMessageSizeEstimator.DEFAULT;

    private static final int DEFAULT_CONNECT_TIMEOUT = 30000;

    private static final AtomicIntegerFieldUpdater<DefaultChannelConfig> AUTOREAD_UPDATER =
            AtomicIntegerFieldUpdater.newUpdater(DefaultChannelConfig.class, "autoRead");
    private static final AtomicReferenceFieldUpdater<DefaultChannelConfig, WriteBufferWaterMark> WATERMARK_UPDATER =
            AtomicReferenceFieldUpdater.newUpdater(
                    DefaultChannelConfig.class, WriteBufferWaterMark.class, "writeBufferWaterMark");

    protected final Channel channel;

    private volatile ByteBufAllocator allocator = ByteBufAllocator.DEFAULT;

    private volatile RecvByteBufAllocator rcvBufAllocator;

    private volatile MessageSizeEstimator msgSizeEstimator = DEFAULT_MSG_SIZE_ESTIMATOR;

    private volatile int connectTimeoutMillis = DEFAULT_CONNECT_TIMEOUT;

    private volatile int writeSpinCount = 16;

    private volatile int maxMessagesPerWrite = Integer.MAX_VALUE;

    @SuppressWarnings("FieldMayBeFinal")
    private volatile int autoRead = 1;

    private volatile boolean autoClose = true;

    private volatile WriteBufferWaterMark writeBufferWaterMark = WriteBufferWaterMark.DEFAULT; //incomplete

    private volatile boolean pinEventExecutor = true;

    public DefaultChannelConfig(Channel channel) {
        this(channel, new AdaptiveRecvByteBufAllocator());
    }

    protected DefaultChannelConfig(Channel channel, RecvByteBufAllocator allocator) {
        this.channel = channel;
        try {
            // System.out.println("Working Directory = " + System.getProperty("user.dir"));
            LOGGER.warning("[INJECTING CTEST] START INJECTION");
            File file = new File("ctest.json");
            InputStream is = new FileInputStream(file);
            JSONTokener tokener = new JSONTokener(is);
            JSONObject injection_map =  new JSONObject(tokener);
            setRecvByteBufAllocator(allocator, channel.metadata());

            if (injection_map.has("connectTimeoutMillis")) {
                LOGGER.warning("[INJECTING CTEST] INJECTING connectTimeoutMillis");
                connectTimeoutMillis = Integer.parseInt(injection_map.getString("connectTimeoutMillis"));
            }
            if (injection_map.has("maxMessagesPerRead")) {
                LOGGER.warning("[INJECTING CTEST] INJECTING maxMessagesPerRead");
                setMaxMessagesPerRead(Integer.parseInt(injection_map.getString("maxMessagesPerRead")));
            }
            if (injection_map.has("maxMessagesPerWrite")) {
                LOGGER.warning("[INJECTING CTEST] INJECTING maxMessagesPerWrite");
                maxMessagesPerWrite = Integer.parseInt(injection_map.getString("maxMessagesPerWrite"));
            }
            if (injection_map.has("writeSpinCount")) {
                LOGGER.warning("[INJECTING CTEST] INJECTING writeSpinCount");
                writeSpinCount = Integer.parseInt(injection_map.getString("writeSpinCount"));
            }
            if (injection_map.has("allocator")) {
                LOGGER.warning("[INJECTING CTEST] INJECTING allocator");
                Class<?> clazz = Class.forName(injection_map.getString("allocator"));
                Constructor<?> ctor = clazz.getConstructor();
                Object obj = ctor.newInstance();
                setAllocator((ByteBufAllocator) obj);
            }
            if (injection_map.has("recvByteBufAllocator")) {
                LOGGER.warning("[INJECTING CTEST] INJECTING recvByteBufAllocator");
                Class<?> clazz = Class.forName(injection_map.getString("recvByteBufAllocator"));
                Constructor<?> ctor = clazz.getConstructor();
                Object obj = ctor.newInstance();
                setRecvByteBufAllocator((RecvByteBufAllocator) obj);
            }
            if (injection_map.has("autoRead")) {
                LOGGER.warning("[INJECTING CTEST] INJECTING autoRead");
                autoRead = Integer.parseInt(injection_map.getString("autoRead"));
            }
            if (injection_map.has("autoClose")) {
                LOGGER.warning("[INJECTING CTEST] INJECTING autoClose");
                autoClose = injection_map.getString("autoRead").equals("false")?false:true;
            }
            if (injection_map.has("writeBufferHighWaterMark")) {
                LOGGER.warning("[INJECTING CTEST] INJECTING writeBufferHighWaterMark");
                setWriteBufferHighWaterMark(Integer.parseInt(injection_map.getString("writeBufferHighWaterMark")));
            }
            if (injection_map.has("writeBufferLowWaterMark")) {
                LOGGER.warning("[INJECTING CTEST] INJECTING writeBufferLowWaterMark");
                setWriteBufferLowWaterMark(Integer.parseInt(injection_map.getString("writeBufferLowWaterMark")));
            }
            if (injection_map.has("writeBufferWaterMark")) {
                LOGGER.warning("[INJECTING CTEST] INJECTING writeBufferWaterMark");
                Class<?> clazz = Class.forName(injection_map.getString("writeBufferWaterMark"));
                Constructor<?> ctor = clazz.getConstructor(new Class[] { int.class, int.class });
                Object obj = ctor.newInstance(new Object[] { 32 * 1024, 64 * 1024 });
                setWriteBufferWaterMark((WriteBufferWaterMark) obj);
            }
            if (injection_map.has("messageSizeEstimator")) {
                LOGGER.warning("[INJECTING CTEST] INJECTING messageSizeEstimator");
                Class<?> clazz = Class.forName(injection_map.getString("messageSizeEstimator"));
                Constructor<?> ctor = clazz.getConstructor(new Class[] { int.class });
                Object obj = ctor.newInstance(new Object[] { 8 });
                setMessageSizeEstimator((MessageSizeEstimator) obj);
            }
            if (injection_map.has("pinEventExecutor")) {
                LOGGER.warning("[INJECTING CTEST] INJECTING pinEventExecutor");
                pinEventExecutor = injection_map.getString("pinEventExecutor").equals("false")?false:true;
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private String getStackTrace() {
        String stacktrace = " ";
        for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
          stacktrace = stacktrace.concat(element.getClassName() + "\t");
        }
        return stacktrace;
    }

    @Override
    @SuppressWarnings("deprecation")
    public Map<ChannelOption<?>, Object> getOptions() {
        return getOptions(
                null,
                CONNECT_TIMEOUT_MILLIS, MAX_MESSAGES_PER_READ, WRITE_SPIN_COUNT,
                ALLOCATOR, AUTO_READ, AUTO_CLOSE, RCVBUF_ALLOCATOR, WRITE_BUFFER_HIGH_WATER_MARK,
                WRITE_BUFFER_LOW_WATER_MARK, WRITE_BUFFER_WATER_MARK, MESSAGE_SIZE_ESTIMATOR,
                SINGLE_EVENTEXECUTOR_PER_GROUP, MAX_MESSAGES_PER_WRITE);
    }

    protected Map<ChannelOption<?>, Object> getOptions(
            Map<ChannelOption<?>, Object> result, ChannelOption<?>... options) {
        if (result == null) {
            result = new IdentityHashMap<ChannelOption<?>, Object>();
        }
        for (ChannelOption<?> o: options) {
            result.put(o, getOption(o));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean setOptions(Map<ChannelOption<?>, ?> options) {
        ObjectUtil.checkNotNull(options, "options");

        boolean setAllOptions = true;
        for (Entry<ChannelOption<?>, ?> e: options.entrySet()) {
            if (!setOption((ChannelOption<Object>) e.getKey(), e.getValue())) {
                setAllOptions = false;
            }
        }

        return setAllOptions;
    }

    @Override
    @SuppressWarnings({ "unchecked", "deprecation" })
    public <T> T getOption(ChannelOption<T> option) {
        ObjectUtil.checkNotNull(option, "option");
        if (option == CONNECT_TIMEOUT_MILLIS) {
            return (T) Integer.valueOf(getConnectTimeoutMillis());
        }
        if (option == MAX_MESSAGES_PER_READ) {
            return (T) Integer.valueOf(getMaxMessagesPerRead());
        }
        if (option == WRITE_SPIN_COUNT) {
            return (T) Integer.valueOf(getWriteSpinCount());
        }
        if (option == ALLOCATOR) {
            return (T) getAllocator();
        }
        if (option == RCVBUF_ALLOCATOR) {
            return (T) getRecvByteBufAllocator();
        }
        if (option == AUTO_READ) {
            return (T) Boolean.valueOf(isAutoRead());
        }
        if (option == AUTO_CLOSE) {
            return (T) Boolean.valueOf(isAutoClose());
        }
        if (option == WRITE_BUFFER_HIGH_WATER_MARK) {
            return (T) Integer.valueOf(getWriteBufferHighWaterMark());
        }
        if (option == WRITE_BUFFER_LOW_WATER_MARK) {
            return (T) Integer.valueOf(getWriteBufferLowWaterMark());
        }
        if (option == WRITE_BUFFER_WATER_MARK) {
            return (T) getWriteBufferWaterMark();
        }
        if (option == MESSAGE_SIZE_ESTIMATOR) {
            return (T) getMessageSizeEstimator();
        }
        if (option == SINGLE_EVENTEXECUTOR_PER_GROUP) {
            return (T) Boolean.valueOf(getPinEventExecutorPerGroup());
        }
        if (option == MAX_MESSAGES_PER_WRITE) {
            return (T) Integer.valueOf(getMaxMessagesPerWrite());
        }
        return null;
    }

    @Override
    @SuppressWarnings("deprecation")
    public <T> boolean setOption(ChannelOption<T> option, T value) {
        validate(option, value);

        if (option == CONNECT_TIMEOUT_MILLIS) {
            setConnectTimeoutMillis((Integer) value);
        } else if (option == MAX_MESSAGES_PER_READ) {
            setMaxMessagesPerRead((Integer) value);
        } else if (option == WRITE_SPIN_COUNT) {
            setWriteSpinCount((Integer) value);
        } else if (option == ALLOCATOR) {
            setAllocator((ByteBufAllocator) value);
        } else if (option == RCVBUF_ALLOCATOR) {
            setRecvByteBufAllocator((RecvByteBufAllocator) value);
        } else if (option == AUTO_READ) {
            setAutoRead((Boolean) value);
        } else if (option == AUTO_CLOSE) {
            setAutoClose((Boolean) value);
        } else if (option == WRITE_BUFFER_HIGH_WATER_MARK) {
            setWriteBufferHighWaterMark((Integer) value);
        } else if (option == WRITE_BUFFER_LOW_WATER_MARK) {
            setWriteBufferLowWaterMark((Integer) value);
        } else if (option == WRITE_BUFFER_WATER_MARK) {
            setWriteBufferWaterMark((WriteBufferWaterMark) value);
        } else if (option == MESSAGE_SIZE_ESTIMATOR) {
            setMessageSizeEstimator((MessageSizeEstimator) value);
        } else if (option == SINGLE_EVENTEXECUTOR_PER_GROUP) {
            setPinEventExecutorPerGroup((Boolean) value);
        } else if (option == MAX_MESSAGES_PER_WRITE) {
            setMaxMessagesPerWrite((Integer) value);
        } else {
            return false;
        }

        return true;
    }

    protected <T> void validate(ChannelOption<T> option, T value) {
        ObjectUtil.checkNotNull(option, "option").validate(value);
    }

    @Override
    public int getConnectTimeoutMillis() {
        LOGGER.warning("[CTEST][GET-PARAM] connectTimeoutMillis");
        return connectTimeoutMillis;
    }

    @Override
    public ChannelConfig setConnectTimeoutMillis(int connectTimeoutMillis) {
        checkPositiveOrZero(connectTimeoutMillis, "connectTimeoutMillis");
        this.connectTimeoutMillis = connectTimeoutMillis;
        LOGGER.warning("[CTEST][SET-PARAM] connectTimeoutMillis" + getStackTrace());
        return this;
    }

    /**
     * {@inheritDoc}
     * <p>
     * @throws IllegalStateException if {@link #getRecvByteBufAllocator()} does not return an object of type
     * {@link MaxMessagesRecvByteBufAllocator}.
     */
    @Override
    @Deprecated
    public int getMaxMessagesPerRead() {
        try {
            MaxMessagesRecvByteBufAllocator allocator = getRecvByteBufAllocator();
            LOGGER.warning("[CTEST][GET-PARAM] maxMessagesPerRead");
            return allocator.maxMessagesPerRead();
        } catch (ClassCastException e) {
            throw new IllegalStateException("getRecvByteBufAllocator() must return an object of type " +
                    "MaxMessagesRecvByteBufAllocator", e);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * @throws IllegalStateException if {@link #getRecvByteBufAllocator()} does not return an object of type
     * {@link MaxMessagesRecvByteBufAllocator}.
     */
    @Override
    @Deprecated
    public ChannelConfig setMaxMessagesPerRead(int maxMessagesPerRead) {
        try {
            MaxMessagesRecvByteBufAllocator allocator = getRecvByteBufAllocator();
            allocator.maxMessagesPerRead(maxMessagesPerRead);
            LOGGER.warning("[CTEST][SET-PARAM] MaxMessagesPerRead" + getStackTrace());
            return this;
        } catch (ClassCastException e) {
            throw new IllegalStateException("getRecvByteBufAllocator() must return an object of type " +
                    "MaxMessagesRecvByteBufAllocator", e);
        }
    }

    /**
     * Get the maximum number of message to write per eventloop run. Once this limit is
     * reached we will continue to process other events before trying to write the remaining messages.
     */
    public int getMaxMessagesPerWrite() {
        LOGGER.warning("[CTEST][GET-PARAM] maxMessagesPerWrite");
        return maxMessagesPerWrite;
    }

     /**
     * Set the maximum number of message to write per eventloop run. Once this limit is
     * reached we will continue to process other events before trying to write the remaining messages.
     */
    public ChannelConfig setMaxMessagesPerWrite(int maxMessagesPerWrite) {
        this.maxMessagesPerWrite = ObjectUtil.checkPositive(maxMessagesPerWrite, "maxMessagesPerWrite");
        LOGGER.warning("[CTEST][SET-PARAM] maxMessagesPerWrite" + getStackTrace());
        return this;
    }

    @Override
    public int getWriteSpinCount() {
        LOGGER.warning("[CTEST][GET-PARAM] writeSpinCount");
        return writeSpinCount;
    }

    @Override
    public ChannelConfig setWriteSpinCount(int writeSpinCount) {
        checkPositive(writeSpinCount, "writeSpinCount");
        // Integer.MAX_VALUE is used as a special value in the channel implementations to indicate the channel cannot
        // accept any more data, and results in the writeOp being set on the selector (or execute a runnable which tries
        // to flush later because the writeSpinCount quantum has been exhausted). This strategy prevents additional
        // conditional logic in the channel implementations, and shouldn't be noticeable in practice.
        if (writeSpinCount == Integer.MAX_VALUE) {
            --writeSpinCount;
        }
        this.writeSpinCount = writeSpinCount;
        LOGGER.warning("[CTEST][SET-PARAM] writeSpinCount" + getStackTrace());
        return this;
    }

    @Override
    public ByteBufAllocator getAllocator() {
        LOGGER.warning("[CTEST][GET-PARAM] allocator");
        return allocator;
    }

    @Override
    public ChannelConfig setAllocator(ByteBufAllocator allocator) {
        this.allocator = ObjectUtil.checkNotNull(allocator, "allocator");
        LOGGER.warning("[CTEST][SET-PARAM] allocator" + getStackTrace());
        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends RecvByteBufAllocator> T getRecvByteBufAllocator() {
        LOGGER.warning("[CTEST][GET-PARAM] recvByteBufAllocator");
        return (T) rcvBufAllocator;
    }

    @Override
    public ChannelConfig setRecvByteBufAllocator(RecvByteBufAllocator allocator) {
        rcvBufAllocator = checkNotNull(allocator, "allocator");
        LOGGER.warning("[CTEST][SET-PARAM] recvByteBufAllocator" + getStackTrace());
        return this;
    }

    /**
     * Set the {@link RecvByteBufAllocator} which is used for the channel to allocate receive buffers.
     * @param allocator the allocator to set.
     * @param metadata Used to set the {@link ChannelMetadata#defaultMaxMessagesPerRead()} if {@code allocator}
     * is of type {@link MaxMessagesRecvByteBufAllocator}.
     */
    private void setRecvByteBufAllocator(RecvByteBufAllocator allocator, ChannelMetadata metadata) {
        checkNotNull(allocator, "allocator");
        checkNotNull(metadata, "metadata");
        if (allocator instanceof MaxMessagesRecvByteBufAllocator) {
            ((MaxMessagesRecvByteBufAllocator) allocator).maxMessagesPerRead(metadata.defaultMaxMessagesPerRead());
        }
        setRecvByteBufAllocator(allocator);
        LOGGER.warning("[CTEST][SET-PARAM] recvByteBufAllocator" + getStackTrace());
    }

    @Override
    public boolean isAutoRead() {
        LOGGER.warning("[CTEST][GET-PARAM] autoRead");
        return autoRead == 1;
    }

    @Override
    public ChannelConfig setAutoRead(boolean autoRead) {
        boolean oldAutoRead = AUTOREAD_UPDATER.getAndSet(this, autoRead ? 1 : 0) == 1;
        if (autoRead && !oldAutoRead) {
            channel.read();
        } else if (!autoRead && oldAutoRead) {
            autoReadCleared();
        }
        LOGGER.warning("[CTEST][SET-PARAM] autoRead" + getStackTrace());
        return this;
    }

    /**
     * Is called once {@link #setAutoRead(boolean)} is called with {@code false} and {@link #isAutoRead()} was
     * {@code true} before.
     */
    protected void autoReadCleared() { }

    @Override
    public boolean isAutoClose() {
        LOGGER.warning("[CTEST][GET-PARAM] autoClose");
        return autoClose;
    }

    @Override
    public ChannelConfig setAutoClose(boolean autoClose) {
        this.autoClose = autoClose;
        LOGGER.warning("[CTEST][SET-PARAM] autoClose" + getStackTrace());
        return this;
    }

    @Override
    public int getWriteBufferHighWaterMark() {
        LOGGER.warning("[CTEST][GET-PARAM] writeBufferHighWaterMark");
        return writeBufferWaterMark.high();
    }

    @Override
    public ChannelConfig setWriteBufferHighWaterMark(int writeBufferHighWaterMark) {
        checkPositiveOrZero(writeBufferHighWaterMark, "writeBufferHighWaterMark");
        LOGGER.warning("[CTEST][SET-PARAM] writeBufferHighWaterMark" + getStackTrace());
        for (;;) {
            WriteBufferWaterMark waterMark = writeBufferWaterMark;
            if (writeBufferHighWaterMark < waterMark.low()) {
                throw new IllegalArgumentException(
                        "writeBufferHighWaterMark cannot be less than " +
                                "writeBufferLowWaterMark (" + waterMark.low() + "): " +
                                writeBufferHighWaterMark);
            }
            if (WATERMARK_UPDATER.compareAndSet(this, waterMark,
                    new WriteBufferWaterMark(waterMark.low(), writeBufferHighWaterMark, false))) {
                return this;
            }
        }
    }

    @Override
    public int getWriteBufferLowWaterMark() {
        LOGGER.warning("[CTEST][GET-PARAM] writeBufferLowWaterMark");
        return writeBufferWaterMark.low();
    }

    @Override
    public ChannelConfig setWriteBufferLowWaterMark(int writeBufferLowWaterMark) {
        checkPositiveOrZero(writeBufferLowWaterMark, "writeBufferLowWaterMark");
        LOGGER.warning("[CTEST][SET-PARAM] writeBufferLowWaterMark" + getStackTrace());
        for (;;) {
            WriteBufferWaterMark waterMark = writeBufferWaterMark;
            if (writeBufferLowWaterMark > waterMark.high()) {
                throw new IllegalArgumentException(
                        "writeBufferLowWaterMark cannot be greater than " +
                                "writeBufferHighWaterMark (" + waterMark.high() + "): " +
                                writeBufferLowWaterMark);
            }
            if (WATERMARK_UPDATER.compareAndSet(this, waterMark,
                    new WriteBufferWaterMark(writeBufferLowWaterMark, waterMark.high(), false))) {
                return this;
            }
        }
    }

    @Override
    public ChannelConfig setWriteBufferWaterMark(WriteBufferWaterMark writeBufferWaterMark) {
        this.writeBufferWaterMark = checkNotNull(writeBufferWaterMark, "writeBufferWaterMark");
        LOGGER.warning("[CTEST][SET-PARAM] writeBufferWaterMark" + getStackTrace());
        return this;
    }

    @Override
    public WriteBufferWaterMark getWriteBufferWaterMark() {
        LOGGER.warning("[CTEST][GET-PARAM] writeBufferWaterMark");
        return writeBufferWaterMark;
    }

    @Override
    public MessageSizeEstimator getMessageSizeEstimator() {
        LOGGER.warning("[CTEST][GET-PARAM] messageSizeEstimator");
        return msgSizeEstimator;
    }

    @Override
    public ChannelConfig setMessageSizeEstimator(MessageSizeEstimator estimator) {
        this.msgSizeEstimator = ObjectUtil.checkNotNull(estimator, "estimator");
        LOGGER.warning("[CTEST][SET-PARAM] messageSizeEstimator" + getStackTrace());
        return this;
    }

    private ChannelConfig setPinEventExecutorPerGroup(boolean pinEventExecutor) {
        this.pinEventExecutor = pinEventExecutor;
        LOGGER.warning("[CTEST][SET-PARAM] pinEventExecutorPerGroup" + getStackTrace());
        return this;
    }

    private boolean getPinEventExecutorPerGroup() {
        LOGGER.warning("[CTEST][GET-PARAM] pinEventExecutorPerGroup");
        return pinEventExecutor;
    }

}
