import java.io.*;
import java.net.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class customer{
    public static void main(String args[]){
        new WindowClient();
    }
}

class WindowClient extends JFrame implements Runnable, ActionListener{
    JButton connection,send;
    JTextField inputText;
    JTextArea showResult;
    Socket socket = null;
    DataInputStream in = null;
    DataOutputStream out = null;
    Thread thread;
    WindowClient(){
        socket = new Socket();
        connection = new JButton("连接服务器");
        send = new JButton("发送");
        send.setEnabled(false);
        inputText = new JTextField(6);
        showResult = new JTextArea();
        add(connection,BorderLayout.NORTH);
        JPanel pSouth = new JPanel();
        pSouth.add(new JLabel("输入："));
        pSouth.add(inputText);
        pSouth.add(send);
        add(new JScrollPane(showResult),BorderLayout.CENTER);
        add(pSouth,BorderLayout.SOUTH);
        connection.addActionListener(this);
        send.addActionListener(this);
        thread = new Thread();
        setBounds(10,30,460,400);
        setVisible(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    public void actionPerformed(ActionEvent e){
        if(e.getSource() == connection){
            // 连接
            try {
                if (socket.isConnected()) {
                } else {
                    InetAddress inetAddress = InetAddress.getByName("10.130.146.83");
                    InetSocketAddress inetSocketAddress = new InetSocketAddress(inetAddress, 4331);
                    socket.connect(inetSocketAddress);
                    in = new DataInputStream(socket.getInputStream());
                    out = new DataOutputStream(socket.getOutputStream());
                    send.setEnabled(true);
                    if (!(thread.isAlive())) {
                        thread = new Thread(this);
                    }
                    thread.start();
                }
            }
            catch (IOException eee){
                System.out.println(eee);
                socket = new Socket();
            }
        }
        if(e.getSource() == send) {
            String s = inputText.getText();
            try {
                out.writeUTF(s);
            } catch (IOException ee) {
                System.out.println(ee);
            }
        }
    }
    public void run(){
        String s = null;
        double result = 0;
        while(true){
            try{
                s = in.readUTF();
                showResult.append(s+"\n");
            }
            catch (IOException e){
                showResult.setText("与服务器断开连接");
                socket = new Socket();
                break;
            }
        }
    }
}