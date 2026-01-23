import java.util.ArrayList;
import java.util.List;

public class ClientTest {
    public static void main(String[] args) {
        MessageQueue queue = new MessageQueue();
        MessagePublisher publisher = new MessagePublisher(queue);

        List<String> sent = new ArrayList<>();
        sent.add("MSG-1");
        sent.add("MSG-2");
        sent.add("MSG-3");
        sent.add("MSG-4");
        sent.add("MSG-5");
        sent.add("MSG-6");

        publisher.publishBatch(sent);

        List<String> received = publisher.receiveAll();

        boolean orderMatches = true;
        for (int i = 0; i < sent.size(); i++) {
            if (!sent.get(i).equals(received.get(i))) {
                orderMatches = false;
                break;
            }
        }

        if (orderMatches) {
            System.out.println("OK: All messages in order");
            System.exit(0);
        } else {
            System.out.println("ERROR: Message order mismatch");
            System.out.println("Sent:     " + sent);
            System.out.println("Received: " + received);
            System.exit(1);
        }
    }
}
