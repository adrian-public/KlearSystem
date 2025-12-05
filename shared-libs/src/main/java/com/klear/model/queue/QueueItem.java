package com.klear.model.queue;

public class QueueItem {
    private QueueItemTypes type;
    private Object item;

    public QueueItem(QueueItemTypes type, Object item) {
        this.type = type;
        this.item = item;
    }

    public Object getItem() {
        return item;
    }

    public void setItem(Object item) {
        this.item = item;
    }

    public QueueItemTypes getType() {
        return type;
    }

    public void setType(QueueItemTypes type) {
        this.type = type;
    }
}
