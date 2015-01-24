package ai.context.util.common.email;

import java.util.concurrent.ArrayBlockingQueue;

public class EmailSendingService implements Runnable{
    private ArrayBlockingQueue<Email> emails = new ArrayBlockingQueue<Email>(1000);

    @Override
    public void run() {
        while (true){
            Email email = null;
            try {
                email = emails.take();
                EmailSender.send(email);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void queueEmail(String from, String to, String subject, String text){
        emails.add(new Email(from, to, subject, text));
    }
}
