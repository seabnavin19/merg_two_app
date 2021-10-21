package com.samples.flironecamera;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;
import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;


public class SendMail extends AsyncTask{

    private Context context;
    private Session session;
    private String email;
    private String subject;
    String text = "Someone Have a warning temperature!";
    private String message_re;
    MimeMessage messages;

    public SendMail(Context context, String email, String subject, String message){
        this.context = context;
        this.email = email;
        this.subject = subject;
        this.message_re = message;
    }



    @Override
    protected void onPreExecute() {
        super.onPreExecute();

    }

    @Override
    protected void onPostExecute(Object o) {
        super.onPostExecute(o);
        Toast.makeText(context,"Message Sent",Toast.LENGTH_LONG).show();
    }

    @Override
    protected Object doInBackground(Object[] objects) {

        Properties props = new Properties();

        props.put("mail.smtp.auth",            "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host",            "smtp.gmail.com");
        props.put("mail.smtp.port",             "587");

        session=Session.getInstance(props,new javax.mail.Authenticator(){
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(Config.EMAIL,Config.PASSWORD);
            }
        });

        try {

            MimeMessage message = new MimeMessage(session);

            message.setFrom(new InternetAddress(Config.EMAIL));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));
            message.setSubject(subject);
            message.setText(text+"\n"+message_re);
            messages=message;

        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }

        if (messages!=null){

            try {
                Transport.send(messages);
//                        Log.d("mail","ok");
            } catch (MessagingException e) {
                e.printStackTrace();
                Log.d("mail",e.toString());
            }

    }
        return null;
}
}