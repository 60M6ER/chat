package ru.gb.chat.client;

import ru.gb.chat.common.Common;
import ru.gb.javatwo.network.SocketThread;
import ru.gb.javatwo.network.SocketThreadListener;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;

public class ClientGUI extends JFrame implements ActionListener, Thread.UncaughtExceptionHandler, SocketThreadListener, ListSelectionListener {

    private static final int WIDTH = 600;
    private static final int HEIGHT = 300;

    private final JTextArea log = new JTextArea();
    private final JPanel panelTop = new JPanel(new BorderLayout());
    private final JPanel panelLogin = new JPanel(new GridLayout(2, 3));
    private final JPanel panelCenter = new JPanel(new BorderLayout());
    private final JPanel panelChangeNick = new JPanel(new BorderLayout());
    private final JTextField tfIPAddress = new JTextField("127.0.0.1");
    private final JTextField tfPort = new JTextField("8189");
    private final JCheckBox cbAlwaysOnTop = new JCheckBox("Always on top");
    private final JTextField tfLogin = new JTextField("boris");
    private final JPasswordField tfPassword = new JPasswordField("123");
    private final JButton btnLogin = new JButton("Login");
    private final JButton btnChangeNick = new JButton("Change");
    private final JButton btnNewNick = new JButton("New nick");
    private final JButton btnAllUsers = new JButton("All");
    private final JTextField tfNewNick = new JTextField("123");

    private final JPanel panelBottom = new JPanel(new BorderLayout());
    private final JButton btnDisconnect = new JButton("<html><b>Disconnect</b></html>");
    private final JTextField tfMessage = new JTextField();
    private final JButton btnSend = new JButton("Send");
    private final JPanel panelRight = new JPanel(new BorderLayout());
    private final JPanel panelRightTop = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));

    private final JList<String> userList = new JList<>();
    private boolean shownIoErrors = false;
    private SocketThread socketThread;
    private final DateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss: ");
    private final String WINDOW_TITLE = "Chat";

    private boolean FWError = false;

    private ClientGUI() {
        Thread.setDefaultUncaughtExceptionHandler(this);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setSize(WIDTH, HEIGHT);
        setTitle(WINDOW_TITLE);
        log.setEditable(false);
        log.setLineWrap(true);
        JScrollPane scrollLog = new JScrollPane(log);
        JScrollPane scrollUser = new JScrollPane(userList);
        scrollUser.setPreferredSize(new Dimension(100, 0));

        cbAlwaysOnTop.addActionListener(this);
        btnSend.addActionListener(this);
        tfMessage.addActionListener(this);
        btnLogin.addActionListener(this);
        btnDisconnect.addActionListener(this);
        btnChangeNick.addActionListener(this);
        btnNewNick.addActionListener(this);
        btnAllUsers.addActionListener(this);
        userList.addListSelectionListener(this);

        btnAllUsers.setPreferredSize(new Dimension(100, 25));
        btnNewNick.setPreferredSize(new Dimension(100, 25));
        panelChangeNick.setPreferredSize(new Dimension(0, 30));
        btnAllUsers.setVisible(false);
        btnNewNick.setVisible(false);

        panelLogin.add(tfIPAddress);
        panelLogin.add(tfPort);
        panelLogin.add(cbAlwaysOnTop);
        panelLogin.add(tfLogin);
        panelLogin.add(tfPassword);
        panelLogin.add(btnLogin);
        panelTop.add(panelLogin, BorderLayout.NORTH);
        panelTop.add(panelChangeNick, BorderLayout.SOUTH);
        panelCenter.add(scrollLog);
        panelCenter.add(panelRight, BorderLayout.EAST);
        panelRightTop.add(btnNewNick);
        panelRightTop.add(btnAllUsers);
        panelRightTop.setPreferredSize(new Dimension(0, 0));
        panelRight.add(panelRightTop, BorderLayout.NORTH);
        panelRight.add(scrollUser, BorderLayout.CENTER);
        panelBottom.add(btnDisconnect, BorderLayout.WEST);
        panelBottom.add(tfMessage, BorderLayout.CENTER);
        panelBottom.add(btnSend, BorderLayout.EAST);
        panelChangeNick.add(tfNewNick, BorderLayout.CENTER);
        panelChangeNick.add(btnChangeNick, BorderLayout.EAST);
        panelChangeNick.setVisible(false);
        panelBottom.setVisible(false);

        //add(panelChangeNick, BorderLayout.NORTH);
        add(panelCenter, BorderLayout.CENTER);
        add(panelTop, BorderLayout.NORTH);
        add(panelBottom, BorderLayout.SOUTH);

        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() { // Event Dispatching Thread
                new ClientGUI();
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
            loadHistory();
            connect();
        } else if (src == btnDisconnect) {
            socketThread.close();
        } else if (src == btnAllUsers) {
            userList.clearSelection();
            hideAllBtn();
        } else if (src == btnNewNick) {
            panelChangeNick.setVisible(true);
        } else if (src == btnChangeNick) {
            changeNick();
        } else {
            throw new RuntimeException("Unknown source: " + src);
        }
    }

    private void loadHistory() {
        File file = new File(HistoryService.getPrefix() + tfLogin.getText() + HistoryService.getPostfix());
        if (file.exists()) {
            log.selectAll();
            log.replaceSelection("");
            try {
                List<String> historyLogs = HistoryService.getHistoryService(file).getHistory();
                for (int i = 0; i < historyLogs.size(); i++) {
                    log.append(historyLogs.get(i) + "\n");
                }
                log.setCaretPosition(log.getDocument().getLength());
            } catch (IOException e) {
                System.out.println("");
            }
        }
    }

    private void hideAllBtn() {
        panelRightTop.setPreferredSize(new Dimension(0, panelRightTop.getHeight() - 25));
        btnAllUsers.setVisible(false);
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        Object src = e.getSource();
        if (src == userList) {
            if (userList.getLeadSelectionIndex() > -1) {
                panelRightTop.setPreferredSize(new Dimension(0, panelRightTop.getHeight() + 25));
                btnAllUsers.setVisible(true);
            }
        }else {
            throw new RuntimeException("Unknown source: " + src);
        }
    }

    private void connect() {
        try {
            Socket socket = new Socket(tfIPAddress.getText(), Integer.parseInt(tfPort.getText()));
            socketThread = new SocketThread(this, "Client", socket);
        } catch (IOException exception) {
            showException(Thread.currentThread(), exception);
        }
    }

    private void sendMessage() {
        String msg = tfMessage.getText();
        if ("".equals(msg)) return;
        tfMessage.setText(null);
        tfMessage.grabFocus();
        if (userList.getSelectedIndex() > -1)
            socketThread.sendMessage(Common.getTypeClientPrivate("", userList.getSelectedValue(), msg));
        else
            socketThread.sendMessage(Common.getTypeBcastClient(msg));
    }

    private void wrtMsgToLogFile(String msg, String username) {
        try (FileWriter out = new FileWriter("log.txt", true)) {
            out.write(username + ": " + msg + "\n");
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
                try {
                    if (!FWError)
                        HistoryService.getHistoryService(tfLogin.getText()).writeLog(msg);
                } catch (IOException e) {
                    log.append("Unable to write history to file!\n");
                    FWError = true;
                    e.printStackTrace();
                }
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
        System.exit(1);
    }

    @Override
    public void onSocketStart(SocketThread thread, Socket socket) {
        putLog("Start");
    }

    @Override
    public void onSocketStop(SocketThread thread) {
        panelBottom.setVisible(false);
        panelLogin.setVisible(true);
        setTitle(WINDOW_TITLE);
        userList.setListData(new String[0]);
    }

    @Override
    public void onSocketReady(SocketThread thread, Socket socket) {
        panelBottom.setVisible(true);
        panelLogin.setVisible(false);
        panelRightTop.setPreferredSize(new Dimension(0, panelRightTop.getHeight() + 25));
        btnNewNick.setVisible(true);
        String login = tfLogin.getText();
        String password = new String(tfPassword.getPassword());
        thread.sendMessage(Common.getAuthRequest(login, password));
    }

    @Override
    public void onReceiveString(SocketThread thread, Socket socket, String msg) {
        handleMessage(msg);
    }

    @Override
    public void onSocketException(SocketThread thread, Exception exception) {
        showException(thread, exception);
    }

    private void handleMessage(String msg) {
        String[] arr = msg.split(Common.DELIMITER);
        String msgType = arr[0];
        switch (msgType) {
            case Common.AUTH_ACCEPT:
                setTitle(WINDOW_TITLE + " entered with nickname: " + arr[1]);
                break;
            case Common.AUTH_DENIED:
                putLog(msg);
                break;
            case Common.MSG_FORMAT_ERROR:
                putLog(msg);
                socketThread.close();
                break;
            case Common.TYPE_BROADCAST:
                putLog(DATE_FORMAT.format(Long.parseLong(arr[1])) +
                        arr[2] + ": " + arr[3]);
                break;
            case Common.USER_LIST:
                String users = msg.substring(Common.USER_LIST.length() +
                        Common.DELIMITER.length());
                String[] usersArr = users.split(Common.DELIMITER);
                updateUsers(usersArr);
                break;
            case Common.TYPE_CLIENT_PRIVATE:
                putLog(String.format("%s приват от %s: %s",
                        DATE_FORMAT.format(Long.parseLong(arr[1])),
                        arr[2],
                        arr[4]));
                break;
            default:
                throw new RuntimeException("Unknown message type: " + msg);
        }
    }

    private void updateUsers(String[] usersArr){
        String nickname = null;
        if (userList.getSelectedIndex() > -1)
            nickname = userList.getSelectedValue();
        Arrays.sort(usersArr);
        userList.setListData(usersArr);
        if (nickname != null) {
            for (int i = 0; i < usersArr.length; i++) {
                if (nickname.equals(usersArr[i])) {
                    userList.setSelectedValue(nickname, true);
                }
            }
        }else {
            hideAllBtn();
        }

    }

    private void changeNick() {
        if (tfNewNick.getText().equals(""))
            return;
        socketThread.sendMessage(Common.getChangeNickname(tfNewNick.getText()));
    }


}
