package net.redheademile.deployment.sevices;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import net.redheademile.deployment.models.ServiceModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.util.List;

@Service
public class EmailService implements IEmailService {
    private final JavaMailSender mailSender;

    @Autowired
    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    private void sendEmail(List<String> recipients, String subject, String content) {
        if (recipients.isEmpty())
            return;

        try {
            Address[] addresses = new Address[recipients.size()];
            for (int i = 0; i < addresses.length; i++)
                addresses[i] = new InternetAddress(recipients.get(i));

            MimeMessage message = mailSender.createMimeMessage();
            message.setFrom(new InternetAddress("robot@emilien-girolet.fr", "Deployment Bot"));
            message.setRecipients(Message.RecipientType.TO, addresses);
            message.setSubject(subject);

            Multipart multipart = new MimeMultipart("related");

            BodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setContent(content, "text/plain");
            messageBodyPart.setDisposition(MimeBodyPart.INLINE);
            multipart.addBodyPart(messageBodyPart, 0);

            message.setContent(multipart);
            mailSender.send(message);
        }
        catch (MessagingException | UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    @Async
    public void sendDeploymentSuccessEmailAsync(List<String> recipients, ServiceModel status) {
        sendEmail(
                recipients,
                "[Deployment] " + status.getServiceName() + ": Success",
                "Deployment succeed for " + status.getServiceName()
        );
    }

    @Override
    @Async
    public void sendDeploymentFailureEmailAsync(List<String> recipients, ServiceModel status) {
        sendEmail(
                recipients,
                "[Deployment] " + status.getServiceName() + ": Failure",
                "Deployment failed for " + status.getServiceName() + "\n" + status.getMessage()
        );
    }
}
