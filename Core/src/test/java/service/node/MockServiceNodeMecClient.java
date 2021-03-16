package service.node;

import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

class MockServiceNodeMecClient extends ServiceNodeMecClient {
    public MockServiceNodeMecClient() {
        super(URI.create("ws://localhost:2000"));
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onMessage(String message) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onError(Exception ex) {
        throw new UnsupportedOperationException();
    }
}
