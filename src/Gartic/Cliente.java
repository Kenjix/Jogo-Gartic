package Gartic;

import java.net.Socket;

/**
 *
 * @author dougl
 */
public class Cliente {

    private Socket socket;
    private String nick;
    private int pontos;

    public Cliente(Socket socket, String nick, int pontos) {
        this.socket = socket;
        this.nick = nick;
        this.pontos = pontos;
    }

    public Socket getSocket() {
        return socket;
    }

    public String getNick() {
        return nick;
    }

    public int getPontos() {
        return pontos;
    }

    public void setPontos(int pontos) {
        this.pontos = pontos;
    }

    @Override
    public String toString() {
        return nick + " (" + pontos + " Pts),";
    }
}
