package com.project.travel.servlet;

import com.google.appengine.api.utils.SystemProperty;

import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


public class SendConfirmationEmailServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(
            SendConfirmationEmailServlet.class.getName());

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String email = request.getParameter("email");
        String travelInfo = request.getParameter("travelInfo");
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);
        String body = "Hi, you have created a following trip.\n" + travelInfo;
        try {
            Message message = new MimeMessage(session);
            InternetAddress from = new InternetAddress(
                    String.format("noreply@%s.appspotmail.com",
                            SystemProperty.applicationId.get()), "Confirmation from Group10");
            message.setFrom(from);
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(email, ""));
            message.setSubject("You created a new trip!");
            message.setText(body);
            Transport.send(message);
        } catch (MessagingException e) {
            LOG.log(Level.WARNING, String.format("Failed to send an mail to %s", email), e);
            throw new RuntimeException(e);
        }
    }
}
