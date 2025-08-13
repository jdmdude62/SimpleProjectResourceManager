package com.subliminalsearch.simpleprojectresourcemanager.service;

import com.subliminalsearch.simpleprojectresourcemanager.model.EmailConfiguration;
import com.subliminalsearch.simpleprojectresourcemanager.model.Project;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    private final EmailConfiguration configuration;
    
    public EmailService(EmailConfiguration configuration) {
        this.configuration = configuration;
    }
    
    public EmailService() {
        this.configuration = EmailConfiguration.load();
    }
    
    public boolean sendProjectReport(Project project, File reportFile) {
        if (!configuration.isConfigured() || !configuration.isEnabled()) {
            logger.warn("Email not configured or disabled");
            return false;
        }
        
        if (project.getContactEmail() == null || project.getContactEmail().trim().isEmpty()) {
            logger.warn("No contact email for project: {}", project.getProjectId());
            return false;
        }
        
        try {
            // Parse multiple email addresses separated by semicolons
            List<String> recipients = Arrays.stream(project.getContactEmail().split(";"))
                    .map(String::trim)
                    .filter(email -> !email.isEmpty())
                    .collect(Collectors.toList());
            
            if (recipients.isEmpty()) {
                logger.warn("No valid email addresses for project: {}", project.getProjectId());
                return false;
            }
            
            String subject = String.format("Project Report: %s - %s", 
                    project.getProjectId(), 
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
            
            String body = String.format(
                    "Dear %s,\n\n" +
                    "Please find attached the latest project report for:\n\n" +
                    "Project: %s\n" +
                    "Description: %s\n" +
                    "Status: %s\n\n" +
                    "This report was generated on %s.\n\n" +
                    "Best regards,\n" +
                    "%s\n\n" +
                    "This is an automated message. Please do not reply to this email.",
                    project.getContactName() != null ? project.getContactName() : "Team",
                    project.getProjectId(),
                    project.getDescription(),
                    project.getStatus().toString(),
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' hh:mm a")),
                    configuration.getFromName()
            );
            
            // Send to all recipients
            boolean allSent = true;
            for (String recipient : recipients) {
                if (!isInternalEmail(recipient)) {
                    logger.warn("Skipping external email address: {}", recipient);
                    continue;
                }
                
                boolean sent = sendEmail(recipient, subject, body, reportFile);
                if (!sent) {
                    allSent = false;
                    logger.error("Failed to send report to: {}", recipient);
                }
            }
            
            if (allSent) {
                logger.info("Project report sent successfully for: {}", project.getProjectId());
            }
            
            return allSent;
            
        } catch (Exception e) {
            logger.error("Failed to send project report", e);
            return false;
        }
    }
    
    public boolean sendTestEmail(String recipient) {
        if (!configuration.isConfigured()) {
            logger.warn("Email not configured");
            return false;
        }
        
        String subject = "Test Email - Project Resource Manager";
        String body = String.format(
                "This is a test email from Project Resource Manager.\n\n" +
                "Configuration:\n" +
                "- SMTP Server: %s\n" +
                "- Port: %d\n" +
                "- Security: %s\n" +
                "- Username: %s\n" +
                "- From Address: %s\n\n" +
                "If you received this email, your email configuration is working correctly.\n\n" +
                "Timestamp: %s",
                configuration.getSmtpServer(),
                configuration.getSmtpPort(),
                configuration.isUseSSL() ? "SSL" : (configuration.isUseTLS() ? "TLS" : "None"),
                configuration.getUsername(),
                configuration.getFromAddress(),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );
        
        return sendEmail(recipient, subject, body, null);
    }
    
    public boolean sendEmail(String recipient, String subject, String body, File attachment) {
        try {
            // Create session
            Session session = createSession();
            
            // Create message
            Message message = new MimeMessage(session);
            
            // Set from address
            InternetAddress from = new InternetAddress(configuration.getFromAddress(), configuration.getFromName());
            message.setFrom(from);
            
            // Set recipient
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));
            
            // Set subject
            message.setSubject(subject);
            
            if (attachment != null) {
                // Create multipart message with attachment
                Multipart multipart = new MimeMultipart();
                
                // Add text part
                MimeBodyPart textPart = new MimeBodyPart();
                textPart.setText(body);
                multipart.addBodyPart(textPart);
                
                // Add attachment
                MimeBodyPart attachmentPart = new MimeBodyPart();
                attachmentPart.attachFile(attachment);
                multipart.addBodyPart(attachmentPart);
                
                message.setContent(multipart);
            } else {
                // Simple text message
                message.setText(body);
            }
            
            // Send message
            Transport.send(message);
            
            logger.info("Email sent successfully to: {}", recipient);
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to send email to: " + recipient, e);
            return false;
        }
    }
    
    private Session createSession() {
        Properties props = new Properties();
        
        // SMTP settings
        props.put("mail.smtp.host", configuration.getSmtpServer());
        props.put("mail.smtp.port", String.valueOf(configuration.getSmtpPort()));
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.connectiontimeout", String.valueOf(configuration.getConnectionTimeout()));
        props.put("mail.smtp.timeout", String.valueOf(configuration.getConnectionTimeout()));
        
        // Security settings
        if (configuration.isUseSSL()) {
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.socketFactory.port", String.valueOf(configuration.getSmtpPort()));
        } else if (configuration.isUseTLS()) {
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
        }
        
        // Trust all certificates for internal Exchange servers (not recommended for production)
        props.put("mail.smtp.ssl.trust", "*");
        
        // NTLM authentication for Exchange
        if (configuration.isUseNTLMAuth()) {
            props.put("mail.smtp.auth.mechanisms", "NTLM");
            props.put("mail.smtp.auth.ntlm.domain", configuration.getDomain());
        }
        
        // Create authenticator
        Authenticator auth = new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                String username = configuration.getUsername();
                
                // For NTLM, include domain in username if not already present
                if (configuration.isUseNTLMAuth() && 
                    configuration.getDomain() != null && 
                    !configuration.getDomain().isEmpty() &&
                    !username.contains("\\")) {
                    username = configuration.getDomain() + "\\" + username;
                }
                
                return new PasswordAuthentication(username, configuration.getPassword());
            }
        };
        
        return Session.getInstance(props, auth);
    }
    
    private boolean isInternalEmail(String email) {
        // Check if email is internal based on domain
        // This is a simple check - adjust based on your organization's domain
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        
        // Extract domain from email
        int atIndex = email.indexOf('@');
        if (atIndex == -1) {
            return false;
        }
        
        String domain = email.substring(atIndex + 1).toLowerCase();
        
        // Get the username's domain to determine internal domains
        String userDomain = "";
        if (configuration.getUsername() != null && configuration.getUsername().contains("@")) {
            int userAtIndex = configuration.getUsername().indexOf('@');
            userDomain = configuration.getUsername().substring(userAtIndex + 1).toLowerCase();
        }
        
        // Consider email internal if it matches the user's domain
        return !userDomain.isEmpty() && domain.equals(userDomain);
    }
    
    public boolean isConfigured() {
        return configuration != null && configuration.isConfigured() && configuration.isEnabled();
    }
}