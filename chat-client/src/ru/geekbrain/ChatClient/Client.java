package ru.geekbrain.ChatClient;

import ru.geekbrain.ChatCommon.Messages;
import ru.geekbrain.Network.SocketThread;
import ru.geekbrain.Network.SocketThreadListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;


public class Client extends JFrame implements ActionListener, Thread.UncaughtExceptionHandler, SocketThreadListener {
    private static final int WIDTH = 600; //Ширина клиента
    private static final int HEIGHT = 300;  //Высота клиента

    private static final String TITLE = "Чат клиент";
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss: ");
    private final JTextArea log = new JTextArea();

    private final JPanel panelTop = new JPanel(new GridLayout(2, 3));
    private final JTextField tfIPAddress = new JTextField("88.151.93.189");    //  127.0.0.1    //мой - 88.151.93.189
    private final JTextField tfPort = new JTextField("8189");  //8189
    private final JCheckBox cbAlwaysOnTop = new JCheckBox("Поверх окон");
    private static final JTextField tfLogin = new JTextField("Aleks");
    private final JPasswordField tfPassword = new JPasswordField("123");
    private final JButton btnLogin = new JButton("Логин");

    private final JPanel panelBottom = new JPanel(new BorderLayout());
    private final JButton btnDisconnect = new JButton("Отключиться");
    private final JTextField tfMessage = new JTextField();
    private final JButton btnSend = new JButton("Отправить");
    private final JList<String> userList = new JList<>();

    private boolean shownIoErrors = false;
    private static SocketThread socketThread;


    private Client() {
        Thread.setDefaultUncaughtExceptionHandler(this);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setSize(WIDTH, HEIGHT);
        setTitle(TITLE);
        log.setEditable(false);
        log.setLineWrap(true);
        JScrollPane spLog = new JScrollPane(log);
        JScrollPane spUsers = new JScrollPane(userList);
        spUsers.setPreferredSize(new Dimension(100, 0));
        cbAlwaysOnTop.addActionListener(this);
        btnSend.addActionListener(this);
        tfMessage.addActionListener(this);
        btnLogin.addActionListener(this);
        btnDisconnect.addActionListener(this);
        panelBottom.setVisible(false);

        panelTop.add(tfIPAddress);
        panelTop.add(tfPort);
        panelTop.add(cbAlwaysOnTop);
        panelTop.add(tfLogin);
        panelTop.add(tfPassword);
        panelTop.add(btnLogin);
        panelBottom.add(btnDisconnect, BorderLayout.WEST);
        panelBottom.add(tfMessage, BorderLayout.CENTER);
        panelBottom.add(btnSend, BorderLayout.EAST);

        add(panelBottom, BorderLayout.SOUTH);
        add(panelTop, BorderLayout.NORTH);
        add(spLog, BorderLayout.CENTER);
        add(spUsers, BorderLayout.EAST);

        setVisible(true);

    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new Client();
            }
        });
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if (src == cbAlwaysOnTop) {
            setAlwaysOnTop(cbAlwaysOnTop.isSelected());
        } else if (src == btnSend || src == tfMessage) {
            sendMessage();
        } else if (src == btnLogin) {
            connect();
        } else if (src == btnDisconnect) {
            socketThread.close();
        } else {
            throw new RuntimeException("Action for component unimplemented");
        }
    }

    private void connect() {
        try {
            Socket socket = new Socket(tfIPAddress.getText(), Integer.parseInt(tfPort.getText()));
            socketThread = new SocketThread(this, "Client", socket);
        } catch (IOException e) {
            showException(Thread.currentThread(), e);
        }
    }

    private void sendMessage() {
        String msg = tfMessage.getText();
        String username = tfLogin.getText();
        if ("".equals(msg)) return;
        tfMessage.setText(null);
        tfMessage.grabFocus();
        socketThread.sendMessage(Messages.getTypeBcastFromClient(msg));
//        tfMessage.requestFocusInWindow();
//        putLog(String.format("%s: %s", username, msg));  //Дублирование с сервере
        wrtMsgToLogFile(msg, username);     //отправка сообщений в лог файл

    }


    private void wrtMsgToLogFile(String msg, String username) {
        File file = new File("History_" + username + ".txt");
        try (FileWriter out = new FileWriter("History_" + username + ".log", true)) {
            out.write(username + ": " + msg + " \n");

            out.flush();
        } catch (IOException e) {
            if (!shownIoErrors) {
                shownIoErrors = true;
                showException(Thread.currentThread(), e);
            }
        }
    }

    private void putLog(String msg) {
        if ("".equals(msg)) return;
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                log.append(msg + "\n");
                log.setCaretPosition(log.getDocument().getLength());
            }
        });
    }

    private void showException(Thread t, Throwable e) {
        String msg;
        StackTraceElement[] ste = e.getStackTrace();
        if (ste.length == 0)
            msg = "Empty Stacktrace";
        else {
            msg = String.format("Exception in \"%s\" %s: %s\n\tat %s",
                    t.getName(), e.getClass().getCanonicalName(), e.getMessage(), ste[0]);
            JOptionPane.showMessageDialog(this, msg, "Exception", JOptionPane.ERROR_MESSAGE);
        }
        JOptionPane.showMessageDialog(null, msg, "Exception", JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        e.printStackTrace();
        showException(t, e);
    }

    @Override
    public void onSocketStart(SocketThread t, Socket s) {
        putLog("Start");        //"Старт" в чат
    }


    @Override
    public void onSocketStop(SocketThread t) {
        panelBottom.setVisible(false);
        panelTop.setVisible(true);
        setTitle(TITLE);
        userList.setListData(new String[0]);
    }

    @Override
    public void onSocketReady(SocketThread t, Socket socket) {
        panelBottom.setVisible(true);
        panelTop.setVisible(false);
        String login = tfLogin.getText();
        String pass = new String(tfPassword.getPassword());
        t.sendMessage(Messages.getAuthRequest(login, pass));
        Message();
    }

    @Override
    public void onReceiveString(SocketThread t, Socket s, String msg) {
        handleMessage(msg);
    }

    void handleMessage(String value) {
        String[] arr = value.split(Messages.DELIMITER);
        String msgType = arr[0];
        switch (msgType) {
            case Messages.AUTH_ACCEPT:
                setTitle(TITLE + " logged in as: " + arr[1]);
                break;
            case Messages.AUTH_DENY:
                putLog(value);
                break;
            case Messages.MSG_FORMAT_ERROR:
                putLog(value);
                socketThread.close();
                break;
            case Messages.USER_LIST:
                String users = value.substring(Messages.DELIMITER.length() +
                        Messages.USER_LIST.length());
                String[] usersArr = users.split(Messages.DELIMITER);
                Arrays.sort(usersArr);
                userList.setListData(usersArr);
                break;
            case Messages.MSG_BROADCAST:
                log.append(DATE_FORMAT.format(Long.parseLong(arr[1])) + ": " + arr[2] + ": " + arr[3] + "\n");
                log.setCaretPosition(log.getDocument().getLength());
                break;
            default:
                throw new RuntimeException("Unknown message type: " + msgType);
        }
    }

    @Override
    public void onSocketException(SocketThread t, Throwable e) {
        showException(t, e);
    }


    private static void Message() {     // Вывод из лог файла в чат
        String login = tfLogin.getText();
        File file = new File("History_" + login + ".log");

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {

                //   int i = (int) ((br.lines().count()) - 100)

                socketThread.sendMessage(Messages.getTypeBcastFromClient(line)); //вывод в чат всего лога сообщений

                // ((br.lines().count()) - 100)
                //     System.out.println(br.lines().count());  //посчитать кол-во строк

            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
