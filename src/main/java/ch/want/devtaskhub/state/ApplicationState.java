/*
 * Created on 18 Jul 2018
 */
package ch.want.devtaskhub.state;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.springframework.stereotype.Component;

@Component
public class ApplicationState {

    static final int ACTION_BUFFER_SIZE = 50;
    private final CircularFifoQueue<String> fifoQueue;

    public ApplicationState() {
        this.fifoQueue = new CircularFifoQueue<>(ACTION_BUFFER_SIZE);
        addAction("Application started");
    }

    public synchronized void addAction(final String action) {
        final String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        this.fifoQueue.add("[" + timestamp + "] " + action);
    }

    public synchronized List<String> getLastActions() {
        return IteratorUtils.toList(this.fifoQueue.iterator());
    }
}