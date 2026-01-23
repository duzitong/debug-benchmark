import java.util.ArrayList;
import java.util.List;

public class MessagePublisher {
    private final MessageQueue queue;

    public MessagePublisher(MessageQueue queue) {
        this.queue = queue;
    }

    public void publishBatch(List<String> contents) {
        for (int i = 0; i < contents.size(); i++) {
            MessageOptions opts = MessageOptions.builder()
                    .priority(i % 3)
                    .ttl(60000)
                    .build();
            Message msg = new Message(contents.get(i), opts);
            queue.publish(msg);
        }
    }

    public List<String> receiveAll() {
        List<Message> messages = queue.consume();
        List<String> contents = new ArrayList<>();
        for (Message msg : messages) {
            contents.add(msg.getContent());
        }
        return contents;
    }
}
