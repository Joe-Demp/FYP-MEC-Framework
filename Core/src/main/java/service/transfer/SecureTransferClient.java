package service.transfer;

public class SecureTransferClient {

    /*public SecureTransferClient(URI serverUri, DockerController dockerController) throws Exception {
        TransferClient transferClient = new TransferClient(serverUri,dockerController);

        // load up the key store
        String STORETYPE = "JKS";
        String STOREPASSWORD = "storepassword";
        String KEYPASSWORD = "keypassword";

        KeyStore ks = KeyStore.getInstance(STORETYPE);
        InputStream is = this.getClass().getResourceAsStream("/keystore.jks");
        System.out.println(is.toString());
        ks.load(is, STOREPASSWORD.toCharArray());

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, KEYPASSWORD.toCharArray());
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ks);

        SSLContext sslContext = null;
        sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        SSLSocketFactory factory = sslContext.getSocketFactory();

        transferClient.setSocketFactory(factory);

        transferClient.connectBlocking();

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            String line = reader.readLine();
            if (line.equals("close")) {
                transferClient.closeBlocking();
            } else if (line.equals("open")) {
                transferClient.reconnect();
            } else {
                transferClient.send(line);
            }
        }
    }*/
}
