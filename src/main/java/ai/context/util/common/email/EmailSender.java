package ai.context.util.common.email;

import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;
import javax.activation.*;

public class EmailSender{

    public static void send(String from, String to, String subject, String messageText){
        String host = "localhost";
        Properties properties = System.getProperties();
        properties.setProperty("mail.smtp.host", host);
        Session session = Session.getDefaultInstance(properties);

        try{
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.addRecipient(Message.RecipientType.TO,
                    new InternetAddress(to));

            message.setSubject(subject);
            message.setText(messageText);

            Transport.send(message);
            System.out.println("Sent message successfully....");
        }catch (MessagingException mex) {
            mex.printStackTrace();
        }
    }

    public static void send(Email email) {
        send(email.getFrom(), email.getTo(), email.getSubject(), email.getText());
    }
}