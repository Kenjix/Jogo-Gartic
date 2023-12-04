package Gartic;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 *
 * @author dougl
 */
public class Servidor {

    private ArrayList<Cliente> clientes = new ArrayList<>();
    private ArrayList<Cliente> acertouResposta = new ArrayList<>();
    private ArrayList<String> dica = new ArrayList<>();
    private String dicaCliente = "";
    private String temaTurno = "";
    private boolean jogoIniciado = false;
    private boolean dicaOn = false;
    private int turno = 0;
    private Utils utils = new Utils();

    private void atualizaListaClientes() {
        //copia da lista original
        ArrayList<Cliente> clientesTemp = new ArrayList<>(clientes);

        //ordena a lista temporaria pelos pontos em ordem decrescente
        Collections.sort(clientesTemp, Comparator.comparingInt(Cliente::getPontos).reversed());

        //forma a string e envia para os clientes
        StringBuilder listaClientes = new StringBuilder("ListaClientes:");
        for (Cliente c : clientesTemp) {
            if (c.getNick() != null) {
                listaClientes.append(c.getNick()).append(" (").append(c.getPontos()).append(" Pts),");
            }
        }

        for (Cliente c : clientes) {
            enviaMsg(c.getSocket(), listaClientes.toString());
        }
    }

    private void removerCliente(Socket clientSocket) {
        synchronized (Servidor.class) {
            Cliente clienteRemover = null;
            for (Cliente cli : clientes) {
                if (cli.getSocket() == clientSocket) {
                    clienteRemover = cli;
                    break;
                }
            }

            if (clienteRemover != null) {
                clientes.remove(clienteRemover);
                System.out.println("Cliente removido: " + clienteRemover.getNick());
                atualizaListaClientes();
            }
        }
    }

    private void verificaResposta(Cliente remetente, String resposta) {
        if (resposta.equalsIgnoreCase(temaTurno) && !acertouResposta.contains(remetente)) {
            //se a resposta esta correta, pontua o cliente            
            remetente.setPontos(dicaOn ? remetente.getPontos() + 5 : remetente.getPontos() + 10);
            acertouResposta.add(remetente);
            //envia o comando para o remetente indicando que acertou a resposta
            enviaMsg(remetente.getSocket(), "Resposta: " + "Correto");
            atualizaListaClientes();
            //envia o comando para todos os clientes informando que o remetente acertou a resposta
            for (Cliente c : clientes) {
                enviaMsg(c.getSocket(), "Resposta: " + remetente.getNick() + " acertou a resposta!");
            }
        } else {
            //se a resposta esta incorreta, envia o conteudo da tentativa para todos clientes            
            for (Cliente c : clientes) {
                enviaMsg(c.getSocket(), "Resposta: " + remetente.getNick() + ":" + resposta);
            }
        }
    }

    private void processaMsgCliente(String msg, Socket clientSocket) {
        System.out.println("MSG RECEBIDA DE " + clientSocket.getInetAddress() + ":" + clientSocket.getPort() + ": " + msg);
        String[] tokens = msg.split(":", 2);
        if (tokens.length < 2) {
            return;
        }
        String comando = tokens[0];
        String argumento = tokens[1].trim();
        System.out.println("PROCESSANDO COMANDO: " + comando + " ARGUMENTO: " + argumento);
        switch (comando) {
            case "NovoCliente":
                adicionaNovoCliente(argumento, clientSocket);
                break;
            case "TempoEsgotado":
                proximoTurno();
                dicaOn = false;
                break;
            case "Resposta":
                verificaResposta(getClientePorSocket(clientSocket), argumento);
                ArrayList<String> novaLista = utils.verificaPalavra(argumento, temaTurno);
                StringBuilder sb = new StringBuilder();
                //incremente a lista com as letras da tentativa
                for (String item : novaLista) {
                    if (!dica.contains(item)) {
                        dica.add(item);
                    }
                }
                //preenche string com _ nas letras desconecidas
                for (int i = 0; i < temaTurno.length(); i++) {
                    sb.append("_ ");
                }

                for (String item : dica) {
                    String[] info = item.split("&");
                    char letra = info[0].charAt(0);
                    int posicao = Integer.parseInt(info[1]);

                    //preenche a dica com as letras conhecidas
                    sb.setCharAt(posicao * 2, letra);
                }
                dicaCliente = sb.toString();
                break;
            case "Chat":
                for (Cliente c : clientes) {
                    enviaMsg(c.getSocket(), "Chat: " + getClientePorSocket(clientSocket).getNick() + ":" + argumento);
                }
                break;
            case "Pixel":
                //fazer
                break;
            case "Dica":
                dicaOn = true;
                for (Cliente c : clientes) {
                    enviaMsg(c.getSocket(), "Dica: " + dicaCliente);
                }
                break;
            default:
                System.out.println("Comando invalido: COMANDO REBEBIDO: " + comando + " - ARGUMENTO: " + argumento);
                break;
        }
    }

    private void adicionaNovoCliente(String argumento, Socket clientSocket) {
        String nick = argumento;
        synchronized (Servidor.class) {
            Cliente cli = new Cliente(clientSocket, nick, 0);
            clientes.add(cli);
            System.out.println("Novo cliente adicionado: " + cli.getNick());
            atualizaListaClientes();
        }
    }

    private void enviaMsg(Socket clientSocket, String msg) {
        try {
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            System.out.println("MSG ENVIADA AO CLIENTE " + clientSocket.getInetAddress() + ":" + clientSocket.getPort() + ": " + msg);
            out.println(msg);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void proximoTurno() {
        turno = (turno + 1) % clientes.size();
        acertouResposta.clear();
        dica.clear();
        dicaCliente = "";
        //escolhe um novo tema para o turno
        temaTurno = utils.sortearPalavra();
        System.out.println("NOVO TURNO: " + turno + "TEMA ESCOLHIDO: " + temaTurno);
        for (int i = 0; i < clientes.size(); i++) {
            Cliente cliente = clientes.get(i);
            if (i == turno) {
                enviaMsg(cliente.getSocket(), "SeuTurno:" + temaTurno);
            } else {
                enviaMsg(cliente.getSocket(), "AguardeSeuTurno:");
            }
        }
    }

    private Cliente getClientePorSocket(Socket socket) {
        for (Cliente c : clientes) {
            if (c.getSocket() == socket) {
                return c;
            }
        }
        return null;
    }

    public Servidor() {
        final int porta = 1234;
        try ( ServerSocket serverSocket = new ServerSocket(porta)) {
            System.out.println("Servidor TCP esperando por conexões na porta " + porta);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Conexão aceita de " + clientSocket.getInetAddress());

                new Thread(() -> clienteThread(clientSocket)).start();

                synchronized (Servidor.class) {
                    if (!jogoIniciado && clientes.size() > 0) {
                        for (Cliente cliente : clientes) {
                            enviaMsg(cliente.getSocket(), "InicioJogo:");
                        }
                        Collections.shuffle(clientes);
                        jogoIniciado = true;
                        proximoTurno();
                    } else {
                        enviaMsg(clientSocket, "AguardandoPlayers:");
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //thread de cada cliente conectado responsavel por receber comandos e processa-los
    private void clienteThread(Socket clientSocket) {
        while (true) {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                String msgCliente = in.readLine();
                processaMsgCliente(msgCliente, clientSocket);
            } catch (SocketException e) {
                removerCliente(clientSocket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        new Servidor();
    }
}
