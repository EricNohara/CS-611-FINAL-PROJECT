package utils;

import io.github.cdimascio.dotenv.Dotenv;
import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;
import java.util.List;

public class EmailSender {

    private static final Dotenv dotenv = Dotenv.load();
    private static final String fromEmail = dotenv.get("EMAIL_USER");
    private static final String appPassword = dotenv.get("EMAIL_PASS");

    private static final Session session;

    static {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true"); // TLS
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(fromEmail, appPassword);
            }
        });
    }

    public static void sendEmail(List<String> toEmails, String subject, String bodyText) {
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromEmail));

            // Convert List<String> to array of InternetAddress
            InternetAddress[] recipientAddresses = toEmails.stream()
                    .map(email -> {
                        try {
                            return new InternetAddress(email);
                        } catch (AddressException e) {
                            throw new RuntimeException("Invalid email address: " + email, e);
                        }
                    })
                    .toArray(InternetAddress[]::new);

            message.setRecipients(Message.RecipientType.TO, recipientAddresses);
            message.setSubject(subject);
            message.setText(bodyText);

            Transport.send(message);
            System.out.println("Email sent to: " + String.join(", ", toEmails));
        } catch (MessagingException e) {
            System.err.println("Failed to send email.");
            e.printStackTrace();
        }
    }

    public static void sendEmail(String toEmail, String subject, String bodyText) {
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromEmail));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
            message.setSubject(subject);
            message.setText(bodyText);
    
            Transport.send(message);
            System.out.println("Email sent to: " + toEmail);
        } catch (MessagingException e) {
            System.err.println("Failed to send email.");
            e.printStackTrace();
        }
    }
}

