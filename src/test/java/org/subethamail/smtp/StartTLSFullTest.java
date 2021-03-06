package org.subethamail.smtp;

import static org.subethamail.smtp.TestUtil.EMAIL_FROM;
import static org.subethamail.smtp.TestUtil.EMAIL_TO;
import static org.subethamail.smtp.TestUtil.PORT;
import static org.subethamail.smtp.TestUtil.createTlsSslContext;
import static org.subethamail.smtp.TestUtil.getKeyManagers;
import static org.subethamail.smtp.TestUtil.getTrustManagers;
import static org.subethamail.smtp.TestUtil.send;

import java.io.InputStream;
import java.util.concurrent.Executors;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.subethamail.smtp.TestUtil.ConnectionType;
import org.subethamail.smtp.server.SMTPServer;

public class StartTLSFullTest {

    @Test
    public void testStartTLS() throws Exception {
        // the server is started using keyStore.jks and trustStore.jks on the
        // classpath

        // the trustStore contains the keyStore certificate (the server trusts
        // itself)

        // the send method uses the same trustStore (and the default keyStore?)
        // to send

        // System.setProperty("javax.net.debug", "all");

        KeyManager[] keyManagers = getKeyManagers();
        TrustManager[] trustManagers = getTrustManagers();
        SSLContext sslContext = createTlsSslContext(keyManagers, trustManagers);

        // mock a MessageHandlerFactory to check for delivery
        MessageHandlerFactory mhf = Mockito.mock(MessageHandlerFactory.class);
        MessageHandler mh = Mockito.mock(MessageHandler.class);
        Mockito.when(mhf.create(ArgumentMatchers.any(MessageContext.class))).thenReturn(mh);

        SMTPServer server = SMTPServer //
                .port(PORT) //
                .hostName("email-server.me.com") //
                .requireTLS() //
                .enableTLS() //
                .messageHandlerFactory(mhf) //
                .executorService(Executors.newSingleThreadExecutor()) //
                .startTlsSocketFactory(sslContext) //
                .build();
        try {
            server.start();
            send(trustManagers, ConnectionType.START_TLS);
        } finally {
            server.stop();
        }
        InOrder o = Mockito.inOrder(mhf, mh);
        o.verify(mhf).create(ArgumentMatchers.any(MessageContext.class));
        o.verify(mh).from(EMAIL_FROM);
        o.verify(mh).recipient(EMAIL_TO);
        o.verify(mh).data(ArgumentMatchers.any(InputStream.class));
        o.verify(mh).done();
        o.verifyNoMoreInteractions();
    }

}
