package com.project.travel.servlet;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
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

import com.google.appengine.api.utils.SystemProperty;

public class SendInvitationEmail extends HttpServlet {

	private static final Logger LOG = Logger.getLogger(
            SendConfirmationEmailServlet.class.getName());

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String emailList = request.getParameter("email");
        //string to array
        List<String> emails = Arrays.asList(emailList.substring(1, emailList.length() - 1).split(", "));
        
        String travelInfo = request.getParameter("travelInfo");
        String organizer = request.getParameter("organizer");
        
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);
        String body = "Hi, " + organizer + " has created a trip!\n" + travelInfo;
        
        for(String email : emails)
        {
	        try {
	            Message message = new MimeMessage(session);
	            InternetAddress from = new InternetAddress(
	                    String.format("noreply@%s.appspotmail.com",
	                            SystemProperty.applicationId.get()), "Invitation from Group10");
	            message.setFrom(from);
	            message.addRecipient(Message.RecipientType.TO, new InternetAddress(email, ""));
	            message.setSubject(organizer + " has created a trip!");
	            message.setText(body);
	            Transport.send(message);
	        } catch (MessagingException e) {
	            LOG.log(Level.WARNING, String.format("Failed to send an mail to %s", email), e);
	            throw new RuntimeException(e);
	        }
        }
    }
}
