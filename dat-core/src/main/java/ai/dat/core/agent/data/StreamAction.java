package ai.dat.core.agent.data;

import lombok.Getter;
import lombok.NonNull;

import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @Author JunjieM
 * @Date 2025/6/25
 */
@Getter
public class StreamAction implements Iterable<StreamEvent> {

    public static final EventOption FINISHED_EVENT = EventOption.builder()
            .name("__finished__").build();

    private volatile boolean finished = false;

    private final BlockingQueue<StreamEvent> eventQueue = new LinkedBlockingQueue<>();

    /**
     * 添加事件
     */
    public void add(StreamEvent event) {
        if (event != null && !finished) {
            eventQueue.offer(event);
        }
    }

    /**
     * 标记流结束
     */
    public void finished() {
        finished = true;
        // 添加结束标记事件，用于唤醒等待的消费者
        eventQueue.offer(StreamEvent.from(FINISHED_EVENT));
    }

    /**
     * 获取下一个事件，如果没有事件则等待
     *
     * @return 下一个事件，如果流已结束返回null
     */
    public StreamEvent next() {
        try {
            StreamEvent event = eventQueue.take();
            if (FINISHED_EVENT.getName().equals(event.name())) {
                return null; // 流已结束
            }
            return event;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    /**
     * 检查是否还有事件或流未结束
     */
    public boolean hasNext() {
        return !finished || !eventQueue.isEmpty();
    }

    /**
     * 返回阻塞式迭代器
     */
    @Override
    public @NonNull Iterator<StreamEvent> iterator() {
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return StreamAction.this.hasNext();
            }

            @Override
            public StreamEvent next() {
                return StreamAction.this.next();
            }
        };
    }
}
