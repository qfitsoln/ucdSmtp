/**
 * (c) Copyright IBM Corporation 2014, 2017.
 * This is licensed under the following license.
 * The Eclipse Public 1.0 License (http://www.eclipse.org/legal/epl-v10.html)
 * U.S. Government Users Restricted Rights:  Use, duplication or disclosure restricted by GSA ADP Schedule Contract with IBM Corp.
 */

import javax.mail.internet.*;
import javax.mail.*
import javax.activation.*
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;


import com.urbancode.air.AirPluginTool;
import com.urbancode.ud.client.SystemClient;

String SMTP_EXAMPLE_COM = "smtp.example.com"
String SEND_ADDRESS_EXAMPLE = "sender@example.com"
String attachmentName = "path to file"

def apTool = new AirPluginTool(this.args[0], this.args[1]);

// get the step properties
def props = apTool.getStepProperties();


// get the properties from the step definition
def toAddress = props['toList'];
def ccAddress = props['ccList'];
def subject = props['subject'];
def message = props['message'];
def attachment = props['attachment'];

// define the properties we will get from the system configuration
def host = props['host'].trim();
def port = props['port'].trim();
def secure = props['secure'];
def fromAddress = props['fromAddress'].trim();
def username = props['username'].trim();
def password = props['password'];

// create the rest client and get the system configuration
com.urbancode.air.XTrustProvider.install();

if (!host || !port || secure == "none" || !fromAddress) {
    println "[Ok] Retrieving Host, Port, TLS Security and Sender Email Address from the Deploy server's General Settings."
    // get the user, password, and weburl needed to create a rest client
    def udUser = apTool.getAuthTokenUsername();
    def udPass = apTool.getAuthToken();
    def weburl = System.getenv("AH_WEB_URL");

    def client = new SystemClient(new URI(weburl), udUser, udPass);
    def values = client.getSystemConfiguration();

    // find the system configuration properties we are interested in
    values.keys().each() { key ->
    	if (key == "deployMailHost") {
    		host = values.optString(key);
    	}
    	if (key == "deployMailPort") {
    		port = values.optString(key);
    	}
    	if (key == "deployMailSecure") {
    		secure = values.optString(key);
    	}
    	if (key == "deployMailSender") {
    		fromAddress = values.optString(key);
    	}
    }
}

if (host == SMTP_EXAMPLE_COM || fromAddress == SEND_ADDRESS_EXAMPLE) {
    throw new RuntimeException("[Error] Mail Server Settings have not been set. " +
        "Confirm the settings have been configured in IBM UrbanCode Deploy's General Settings.")
}
//tokenize out the recipients in case they came in as a list
StringTokenizer tok = new StringTokenizer(toAddress,",");
ArrayList emailTos = new ArrayList();
while(tok.hasMoreElements()){
  emailTos.add(new InternetAddress(tok.nextElement().toString()));
}

// Added CC Section

StringTokenizer cck = new StringTokenizer(ccAddress,",");
ArrayList emailCcs = new ArrayList();
while(cck.hasMoreElements()){
  emailCcs.add(new InternetAddress(cck.nextElement().toString()));
}

// create a new mail session and message
Properties mprops = new Properties();
mprops.put("mail.smtp.host", host);
mprops.put("mail.smtp.port", port);
mprops.put("mail.smtp.starttls.enable", secure);
Session lSession;
if (username && password) {
    mprops.put("mail.smtp.auth", "true");
    lSession = Session.getInstance(mprops,
        new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
} else {
    lSession = Session.getDefaultInstance(mprops, null);
}

try {
    MimeMessage msg = new MimeMessage(lSession);

    // populate the to, from, subject, and text of the message
    InternetAddress[] to = new InternetAddress[emailTos.size()];
    to = (InternetAddress[]) emailTos.toArray(to);
    msg.setRecipients(MimeMessage.RecipientType.TO,to);

//Added CC
    InternetAddress[] cc = new InternetAddress[emailCcs.size()];
    cc = (InternetAddress[]) emailCcs.toArray(cc);
    msg.setRecipients(MimeMessage.RecipientType.CC,cc);

    msg.setFrom(new InternetAddress(fromAddress));
    msg.setSubject(subject);
    msg.setText(message)
// attache the file

   Multipart multipart = new MimeMultipart();

        MimeBodyPart textBodyPart = new MimeBodyPart();
        textBodyPart.setText("your text");

        MimeBodyPart attachmentBodyPart= new MimeBodyPart();
        DataSource source = new FileDataSource(attachementPath); // ex : "C:\\test.pdf"
        attachmentBodyPart.setDataHandler(new DataHandler(source));
        attachmentBodyPart.setFileName(fileName); // ex : "test.pdf"

        multipart.addBodyPart(textBodyPart);  // add the text part
        multipart.addBodyPart(attachmentBodyPart); // add the attachement part

        msg.setContent(multipart);
    
// send the message
    Transport.send(msg);

} catch (MessagingException e) {
	throw new RuntimeException(e);
}
println "Email(s) sent!"
