package Gartic;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/**
 *
 * @author dougl
 */
public class GarticJFrame extends javax.swing.JFrame {
    
    private Point acaoMouse;
    private boolean desenhando;
    private boolean turnoDoJogador = false;
    private Color corAtual = Color.decode("#000000");
    private BufferedReader in;
    private PrintWriter out;
    private final int INTERVALO_ATUALIZACAO_MILISSEGUNDOS = 1000;
    private final int TEMPO_TOTAL_SEGUNDOS = 60;
    private int larguraLinha;

    /**
     * Creates new form GarticJFrame
     */
    public GarticJFrame(Socket socket, String nick) {
        initComponents();
        try {
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.out = new PrintWriter(socket.getOutputStream(), true);
            envioMsg("NovoCliente:" + nick);
        } catch (IOException e) {
            e.printStackTrace();
        }
        jPanelDesenho.setEnabled(false);
        jPanelDesenho.setBackground(Color.white);
        jTextAreaPlayers.setEnabled(false);
        jTextAreaChat.setEnabled(false);
        jTextAreaRespostas.setEnabled(false);
        jButtonDica.setEnabled(false);
        jProgressBarTempo.setVisible(false);
        jTextFieldChat.setForeground(Color.decode("#bdbdbd"));
        jTextFieldResposta1.setForeground(Color.decode("#bdbdbd"));
        jTextAreaPlayers.setLineWrap(true);
        jTextAreaPlayers.setWrapStyleWord(true);
        jComboBoxLinha.setSelectedIndex(2);
        initChatListeners();
        initServerListener();
    }
    
    public GarticJFrame() {
        initComponents();
    }
    
    private void initServerListener() {
        Thread listenerThread = new Thread(() -> {
            try {
                while (true) {
                    String serverMessage = in.readLine();
                    
                    if (serverMessage == null) {
                        //servidor encerrou a conexao
                        break;
                    }
                    System.out.println("Mensagem do servidor: " + serverMessage);
                    //processa a mensagem recebida do servidor
                    processaMsg(serverMessage);
                }
            } catch (IOException e) {
                System.out.println("ERRO: " + e.getMessage());
            }
        });
        listenerThread.start();
    }
    
    private void initChatListeners() {
        jTextFieldResposta1.addActionListener((ActionEvent e) -> {
            String textoDigitado = jTextFieldResposta1.getText();
            if (!textoDigitado.isEmpty()) {
                envioMsg("Resposta: " + textoDigitado);
                jTextFieldResposta1.setText("");
            }
        });
        
        jTextFieldChat.addActionListener((ActionEvent e) -> {
            String textoDigitado = jTextFieldChat.getText();
            if (!textoDigitado.isEmpty()) {
                envioMsg("Chat: " + textoDigitado);
                jTextFieldChat.setText("");
            }
        });
    }
    
    private void processaMsg(String msg) {
        String[] tokens = msg.split(":", 2); //divide a mensagem em duas partes no primeiro ":" encontrado

        if (tokens.length < 2) {
            //caso a mensagem nao tenha ":" ou nao tenha conteudo apos o ":"
            return;
        }
        
        String comando = tokens[0];
        String argumento = tokens[1].trim();
        String[] dadosMsg;
        String[] dadosPosicao;
        String[] dadosPixel;
        String[] dadosCor;
        int x1, y1, x2, y2, red, green, blue;
        
        switch (comando) {
            case "AguardandoPlayers":
                jLabelStatus.setText("Aguardando jogadores");
                jTextFieldResposta1.setEnabled(false);
                break;
            case "InicioJogo":
                jLabelStatus.setText("Aguarde seu turno");
                break;
            case "SeuTurno":
                turnoDoJogador = true;
                jLabelDica.setText("");
                jTextAreaRespostas.setText("");
                jLabelStatus.setText("Sua vez de desenhar!");
                jLabelTemaDica.setText("TEMA: " + argumento);
                jTextFieldResposta1.setForeground(Color.decode("#bdbdbd"));
                jTextFieldResposta1.setText("Digite sua resposta");
                jTextFieldResposta1.setEnabled(false);
                jPanelDesenho.setEnabled(true);
                jButtonDica.setEnabled(true);

                //habilita a barra de progresso e inicia a contagem regressiva
                jProgressBarTempo.setVisible(true);
                jProgressBarTempo.setValue(100); //configura inicialmente para 100%
                Timer timer = new Timer(INTERVALO_ATUALIZACAO_MILISSEGUNDOS, new ActionListener() {
                    int tempoRestante = TEMPO_TOTAL_SEGUNDOS;
                    
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        int percentual = (int) ((double) tempoRestante / TEMPO_TOTAL_SEGUNDOS * 100);
                        jProgressBarTempo.setValue(percentual);
                        jLabelTempo.setForeground((tempoRestante > 30) ? Color.decode("#008000") : Color.decode("#f0be00"));
                        jLabelTempo.setText(tempoRestante + "s");
                        
                        if (tempoRestante <= 0) {
                            ((Timer) e.getSource()).stop(); //para o timer quando o tempo acabar
                            jProgressBarTempo.setValue(0);
                            jLabelTempo.setForeground(Color.decode("#8b0000"));
                            jLabelTempo.setText("TEMPO ESGOTADO");
                            envioMsg("TempoEsgotado:");
                        }
                        tempoRestante--;
                    }
                });
                timer.start(); //inicia o timer
                break;
            case "AguardeSeuTurno":
                turnoDoJogador = false;
                apagarDesenho();
                jTextFieldResposta1.setEnabled(true);
                jLabelStatus.setText("Adivinhe o desenho!");
                jTextAreaRespostas.setText("");
                jLabelTemaDica.setText("");
                jProgressBarTempo.setVisible(false);
                jLabelTempo.setText("");
                jButtonDica.setEnabled(false);
                break;
            case "ListaClientes":
                //remove o prefixo "ListaClientes:"
                String listaClientes = argumento;

                //limpa o JTextArea antes de adicionar a nova lista
                jTextAreaPlayers.setText("");

                //adiciona a nova lista ao JTextArea            
                String[] clientes = listaClientes.split(",");
                for (String cliente : clientes) {
                    jTextAreaPlayers.append(cliente + "\n");
                    jTextAreaPlayers.setCaretPosition(jTextAreaPlayers.getDocument().getLength());
                }
                break;
            case "Resposta":
                if (argumento.equalsIgnoreCase("Correto")) {
                    jTextFieldResposta1.setText("Você acertou!");
                    jTextFieldResposta1.setEnabled(false);
                } else if (argumento.equalsIgnoreCase("Venceu")) {
                    jTextFieldResposta1.setText("Você Venceu o jogo!");
                } else {
                    jTextAreaRespostas.append(argumento + "\n");
                    jTextAreaRespostas.setCaretPosition(jTextAreaRespostas.getDocument().getLength());
                }
                break;
            case "Chat":
                jTextAreaChat.append(argumento + "\n");
                jTextAreaChat.setCaretPosition(jTextAreaChat.getDocument().getLength());
                break;
            case "Dica":
                if (!turnoDoJogador) {
                    jLabelDica.setText(argumento);
                }
                break;
            case "Pixel":
                dadosMsg = argumento.split(":");
                dadosPosicao = dadosMsg[0].split("@");
                dadosPixel = dadosPosicao[0].split(";");
                dadosCor = dadosPosicao[1].split(";");
                
                x1 = Integer.parseInt(dadosPixel[0]);
                y1 = Integer.parseInt(dadosPixel[1]);
                x2 = Integer.parseInt(dadosPixel[2]);
                y2 = Integer.parseInt(dadosPixel[3]);
                
                red = Integer.parseInt(dadosCor[0]);
                green = Integer.parseInt(dadosCor[1]);
                blue = Integer.parseInt(dadosCor[2]);
                
                desenhar(new Desenho(x1, y1, x2, y2, red, green, blue));
                
                break;
            case "ListaPixel":
                dadosMsg = argumento.split(":");
                dadosPosicao = dadosMsg[0].split("@");
                dadosPixel = dadosPosicao[0].split(";");
                dadosCor = dadosPosicao[1].split(";");
                
                x1 = Integer.parseInt(dadosPixel[0]);
                y1 = Integer.parseInt(dadosPixel[1]);
                x2 = Integer.parseInt(dadosPixel[2]);
                y2 = Integer.parseInt(dadosPixel[3]);
                
                red = Integer.parseInt(dadosCor[0]);
                green = Integer.parseInt(dadosCor[1]);
                blue = Integer.parseInt(dadosCor[2]);

                //agenda a execucao do desenho na proxima interacao da GUI
                SwingUtilities.invokeLater(() -> desenhar(new Desenho(x1, y1, x2, y2, red, green, blue)));
                break;
            case "JogoEncerrado":
                turnoDoJogador = false;
                jProgressBarTempo.setVisible(false);
                jLabelTempo.setVisible(false);
                jLabelStatus.setText("");
                jTextFieldResposta1.setEnabled(false);
                jButtonDica.setEnabled(false);
                jLabelTemaDica.setText("Jogo encerrado");
                break;
            default:
                System.out.println("Comando invalido: COMANDO REBEBIDO: " + comando + " - ARGUMENTO: " + argumento);
                break;
        }
    }
    
    private void envioMsg(String msg) {
        System.out.println("ENVIANDO AO SERVER: " + msg);
        out.println(msg);
    }
    
    private void desenhar(Desenho d) {
        Graphics g = jPanelDesenho.getGraphics();
        ((Graphics2D) g).setStroke(new BasicStroke(larguraLinha));
        g.setColor(new Color(d.getR(), d.getG(), d.getB()));
        g.drawLine(d.getX1(), d.getY1(), d.getX2(), d.getY2());
    }
    
    private void apagarDesenho() {
        Graphics g = jPanelDesenho.getGraphics();
        //obtem dimensoes do panel
        int width = jPanelDesenho.getWidth();
        int height = jPanelDesenho.getHeight();
        //obtem a cor de fundo do panel
        Color corFundo = jPanelDesenho.getBackground();
        //preenche o panel com a cor de fundo padrao
        g.setColor(corFundo);
        g.fillRect(0, 0, width, height);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel4 = new javax.swing.JPanel();
        jPanelPlayers = new javax.swing.JPanel();
        jScrollPane4 = new javax.swing.JScrollPane();
        jTextAreaPlayers = new javax.swing.JTextArea();
        jPanelDesenho = new javax.swing.JPanel();
        jProgressBarTempo = new javax.swing.JProgressBar();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTextAreaRespostas = new javax.swing.JTextArea();
        jTextFieldResposta1 = new javax.swing.JTextField();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextAreaChat = new javax.swing.JTextArea();
        jTextFieldChat = new javax.swing.JTextField();
        jPanel3 = new javax.swing.JPanel();
        jButtonRed = new javax.swing.JButton();
        jButtonYellow = new javax.swing.JButton();
        jButtonGreen = new javax.swing.JButton();
        jButtonBlue = new javax.swing.JButton();
        jButtonPink = new javax.swing.JButton();
        jButtonPurple = new javax.swing.JButton();
        jButtonBlack = new javax.swing.JButton();
        jButtonWhite = new javax.swing.JButton();
        jButtonDica = new javax.swing.JButton();
        jLabelStatus = new javax.swing.JLabel();
        jLabelTempo = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        jComboBoxLinha = new javax.swing.JComboBox<>();
        jLabelTemaDica = new javax.swing.JLabel();
        jLabelDica = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Gartic");

        jPanel4.setPreferredSize(new java.awt.Dimension(1024, 768));

        jPanelPlayers.setBorder(javax.swing.BorderFactory.createTitledBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0), 1, true), "Players", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Comic Sans MS", 1, 18))); // NOI18N

        jTextAreaPlayers.setColumns(20);
        jTextAreaPlayers.setFont(new java.awt.Font("Comic Sans MS", 0, 14)); // NOI18N
        jTextAreaPlayers.setRows(5);
        jScrollPane4.setViewportView(jTextAreaPlayers);

        javax.swing.GroupLayout jPanelPlayersLayout = new javax.swing.GroupLayout(jPanelPlayers);
        jPanelPlayers.setLayout(jPanelPlayersLayout);
        jPanelPlayersLayout.setHorizontalGroup(
            jPanelPlayersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelPlayersLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 174, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanelPlayersLayout.setVerticalGroup(
            jPanelPlayersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelPlayersLayout.createSequentialGroup()
                .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 560, Short.MAX_VALUE)
                .addContainerGap())
        );

        jPanelDesenho.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        jPanelDesenho.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent evt) {
                jPanelDesenhoMouseDragged(evt);
            }
        });
        jPanelDesenho.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                jPanelDesenhoMousePressed(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                jPanelDesenhoMouseReleased(evt);
            }
        });

        javax.swing.GroupLayout jPanelDesenhoLayout = new javax.swing.GroupLayout(jPanelDesenho);
        jPanelDesenho.setLayout(jPanelDesenhoLayout);
        jPanelDesenhoLayout.setHorizontalGroup(
            jPanelDesenhoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 646, Short.MAX_VALUE)
        );
        jPanelDesenhoLayout.setVerticalGroup(
            jPanelDesenhoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 301, Short.MAX_VALUE)
        );

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createEtchedBorder(), "Respostas", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Comic Sans MS", 1, 18))); // NOI18N

        jTextAreaRespostas.setColumns(20);
        jTextAreaRespostas.setFont(new java.awt.Font("Comic Sans MS", 0, 14)); // NOI18N
        jTextAreaRespostas.setRows(5);
        jScrollPane2.setViewportView(jTextAreaRespostas);

        jTextFieldResposta1.setFont(new java.awt.Font("Comic Sans MS", 0, 14)); // NOI18N
        jTextFieldResposta1.setText("Digite sua resposta");
        jTextFieldResposta1.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                jTextFieldResposta1FocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                jTextFieldResposta1FocusLost(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2)
                    .addComponent(jTextFieldResposta1, javax.swing.GroupLayout.Alignment.TRAILING))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 162, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextFieldResposta1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(16, Short.MAX_VALUE))
        );

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createEtchedBorder(), "Chat", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Comic Sans MS", 1, 18))); // NOI18N

        jTextAreaChat.setColumns(20);
        jTextAreaChat.setFont(new java.awt.Font("Comic Sans MS", 0, 14)); // NOI18N
        jTextAreaChat.setRows(5);
        jScrollPane1.setViewportView(jTextAreaChat);

        jTextFieldChat.setFont(new java.awt.Font("Comic Sans MS", 0, 14)); // NOI18N
        jTextFieldChat.setText("Converse aqui");
        jTextFieldChat.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                jTextFieldChatFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                jTextFieldChatFocusLost(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 344, Short.MAX_VALUE)
                    .addComponent(jTextFieldChat, javax.swing.GroupLayout.Alignment.TRAILING))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 162, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextFieldChat, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)), "Cores", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Comic Sans MS", 1, 18))); // NOI18N

        jButtonRed.setBackground(new java.awt.Color(255, 0, 0));
        jButtonRed.setForeground(new java.awt.Color(255, 0, 0));
        jButtonRed.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButtonRedMouseClicked(evt);
            }
        });

        jButtonYellow.setBackground(new java.awt.Color(255, 255, 0));
        jButtonYellow.setForeground(new java.awt.Color(255, 255, 0));
        jButtonYellow.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButtonYellowMouseClicked(evt);
            }
        });

        jButtonGreen.setBackground(new java.awt.Color(0, 204, 0));
        jButtonGreen.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButtonGreenMouseClicked(evt);
            }
        });

        jButtonBlue.setBackground(new java.awt.Color(0, 0, 255));
        jButtonBlue.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButtonBlueMouseClicked(evt);
            }
        });

        jButtonPink.setBackground(new java.awt.Color(255, 0, 255));
        jButtonPink.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButtonPinkMouseClicked(evt);
            }
        });

        jButtonPurple.setBackground(new java.awt.Color(51, 22, 138));
        jButtonPurple.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButtonPurpleMouseClicked(evt);
            }
        });

        jButtonBlack.setBackground(new java.awt.Color(0, 0, 0));
        jButtonBlack.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButtonBlackMouseClicked(evt);
            }
        });

        jButtonWhite.setBackground(new java.awt.Color(255, 255, 255));
        jButtonWhite.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButtonWhiteMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jButtonRed, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonYellow, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jButtonBlue, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonGreen, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jButtonPurple, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonPink, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                        .addComponent(jButtonBlack, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonWhite, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButtonRed, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonYellow, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jButtonBlue, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonGreen, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jButtonPurple, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonPink, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jButtonBlack, javax.swing.GroupLayout.DEFAULT_SIZE, 30, Short.MAX_VALUE)
                    .addComponent(jButtonWhite, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jButtonDica.setFont(new java.awt.Font("Comic Sans MS", 0, 14)); // NOI18N
        jButtonDica.setText("DICA");
        jButtonDica.setPreferredSize(new java.awt.Dimension(70, 70));
        jButtonDica.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonDicaActionPerformed(evt);
            }
        });

        jLabelStatus.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);

        jLabelTempo.setFont(new java.awt.Font("Segoe UI", 1, 10)); // NOI18N
        jLabelTempo.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);

        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setText("Largura linha");

        jComboBoxLinha.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "1", "2", "3", "4", "5" }));
        jComboBoxLinha.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                jComboBoxLinhaItemStateChanged(evt);
            }
        });

        jLabelTemaDica.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabelTemaDica.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);

        jLabelDica.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabelDica.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jLabelTemaDica, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabelDica, javax.swing.GroupLayout.DEFAULT_SIZE, 239, Short.MAX_VALUE))
                        .addGap(114, 114, 114)
                        .addComponent(jLabelStatus, javax.swing.GroupLayout.PREFERRED_SIZE, 204, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(jPanelPlayers, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel4Layout.createSequentialGroup()
                                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel4Layout.createSequentialGroup()
                                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(jPanelDesenho, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jProgressBarTempo, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jLabelTempo, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jComboBoxLinha, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addGroup(jPanel4Layout.createSequentialGroup()
                                        .addGap(9, 9, 9)
                                        .addComponent(jButtonDica, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(0, 0, Short.MAX_VALUE)))))))
                .addGap(45, 45, 45))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabelStatus, javax.swing.GroupLayout.PREFERRED_SIZE, 52, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(jLabelTemaDica, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabelDica, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jPanelPlayers, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jPanelDesenho, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(jPanel4Layout.createSequentialGroup()
                                .addComponent(jButtonDica, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jComboBoxLinha, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jLabelTempo, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jProgressBarTempo, javax.swing.GroupLayout.DEFAULT_SIZE, 15, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                .addGap(32, 32, 32))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, 999, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(19, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, 727, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(47, Short.MAX_VALUE))
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void jPanelDesenhoMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jPanelDesenhoMouseReleased
        desenhando = false;
    }//GEN-LAST:event_jPanelDesenhoMouseReleased

    private void jPanelDesenhoMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jPanelDesenhoMousePressed
        if (turnoDoJogador) {
            desenhando = true;
            acaoMouse = evt.getPoint();
        }
    }//GEN-LAST:event_jPanelDesenhoMousePressed

    private void jPanelDesenhoMouseDragged(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jPanelDesenhoMouseDragged
        if (desenhando && turnoDoJogador) {
            Point pontoAtual = evt.getPoint();
            Graphics g = jPanelDesenho.getGraphics();
            ((Graphics2D) g).setStroke(new BasicStroke(larguraLinha));
            g.setColor(corAtual);
            g.drawLine(acaoMouse.x, acaoMouse.y, pontoAtual.x, pontoAtual.y);
            envioMsg("Pixel:" + acaoMouse.x + ";" + acaoMouse.y + ";" + pontoAtual.x + ";" + pontoAtual.y
                    + "@" + corAtual.getRed() + ";" + corAtual.getGreen() + ";" + corAtual.getBlue());
            
            acaoMouse = pontoAtual;
        }
    }//GEN-LAST:event_jPanelDesenhoMouseDragged

    private void jTextFieldResposta1FocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jTextFieldResposta1FocusGained
        if (jTextFieldResposta1.getText().isEmpty() || jTextFieldResposta1.getText().equalsIgnoreCase("Digite sua resposta")) {
            jTextFieldResposta1.setText("");
            jTextFieldResposta1.setForeground(Color.decode("#000000"));
        }
    }//GEN-LAST:event_jTextFieldResposta1FocusGained

    private void jTextFieldChatFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jTextFieldChatFocusGained
        if (jTextFieldChat.getText().isEmpty() || jTextFieldChat.getText().equalsIgnoreCase("Converse aqui")) {
            jTextFieldChat.setText("");
            jTextFieldChat.setForeground(Color.decode("#000000"));
        }
    }//GEN-LAST:event_jTextFieldChatFocusGained

    private void jTextFieldResposta1FocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jTextFieldResposta1FocusLost
        if (jTextFieldResposta1.getText().isEmpty()) {
            jTextFieldResposta1.setForeground(Color.decode("#bdbdbd"));
            jTextFieldResposta1.setText("Digite sua resposta");
        }
    }//GEN-LAST:event_jTextFieldResposta1FocusLost

    private void jTextFieldChatFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jTextFieldChatFocusLost
        if (jTextFieldChat.getText().isEmpty()) {
            jTextFieldChat.setForeground(Color.decode("#bdbdbd"));
            jTextFieldChat.setText("Converse aqui");
        }
    }//GEN-LAST:event_jTextFieldChatFocusLost

    private void jButtonRedMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButtonRedMouseClicked
        corAtual = Color.decode("#FF0000");
    }//GEN-LAST:event_jButtonRedMouseClicked

    private void jButtonYellowMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButtonYellowMouseClicked
        corAtual = Color.decode("#FFFF00");
    }//GEN-LAST:event_jButtonYellowMouseClicked

    private void jButtonBlueMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButtonBlueMouseClicked
        corAtual = Color.decode("#0000FF");
    }//GEN-LAST:event_jButtonBlueMouseClicked

    private void jButtonGreenMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButtonGreenMouseClicked
        corAtual = Color.decode("#00CC00");
    }//GEN-LAST:event_jButtonGreenMouseClicked

    private void jButtonPurpleMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButtonPurpleMouseClicked
        corAtual = Color.decode("#33168A");
    }//GEN-LAST:event_jButtonPurpleMouseClicked

    private void jButtonPinkMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButtonPinkMouseClicked
        corAtual = Color.decode("#FF00FF");
    }//GEN-LAST:event_jButtonPinkMouseClicked

    private void jButtonBlackMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButtonBlackMouseClicked
        corAtual = Color.decode("#000000");
    }//GEN-LAST:event_jButtonBlackMouseClicked

    private void jButtonWhiteMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButtonWhiteMouseClicked
        corAtual = Color.decode("#FFFFFF");
    }//GEN-LAST:event_jButtonWhiteMouseClicked

    private void jComboBoxLinhaItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_jComboBoxLinhaItemStateChanged
        larguraLinha = jComboBoxLinha.getSelectedIndex();
    }//GEN-LAST:event_jComboBoxLinhaItemStateChanged

    private void jButtonDicaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonDicaActionPerformed
        envioMsg("Dica:");
    }//GEN-LAST:event_jButtonDicaActionPerformed

    /**
     * @param args the command line arguments
     * @throws javax.swing.UnsupportedLookAndFeelException
     */
    public static void main(String args[]) throws UnsupportedLookAndFeelException {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(GarticJFrame.class
                    .getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        
        java.awt.EventQueue.invokeLater(() -> {
            new GarticJFrame().setVisible(true);
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonBlack;
    private javax.swing.JButton jButtonBlue;
    private javax.swing.JButton jButtonDica;
    private javax.swing.JButton jButtonGreen;
    private javax.swing.JButton jButtonPink;
    private javax.swing.JButton jButtonPurple;
    private javax.swing.JButton jButtonRed;
    private javax.swing.JButton jButtonWhite;
    private javax.swing.JButton jButtonYellow;
    private javax.swing.JComboBox<String> jComboBoxLinha;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabelDica;
    private javax.swing.JLabel jLabelStatus;
    private javax.swing.JLabel jLabelTemaDica;
    private javax.swing.JLabel jLabelTempo;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanelDesenho;
    private javax.swing.JPanel jPanelPlayers;
    private javax.swing.JProgressBar jProgressBarTempo;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JTextArea jTextAreaChat;
    private javax.swing.JTextArea jTextAreaPlayers;
    private javax.swing.JTextArea jTextAreaRespostas;
    private javax.swing.JTextField jTextFieldChat;
    private javax.swing.JTextField jTextFieldResposta1;
    // End of variables declaration//GEN-END:variables
}
