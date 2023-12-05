package com.example.lianliankanyan;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;

public class GameClient extends JFrame {

    private static GamePanel gamePanel;


    public  GameClient() throws IOException {

        //    菜单条
        JMenuBar menuBar=new JMenuBar();

        JMenu jMenu=new JMenu("设置");

        JMenuItem restartItem=new JMenuItem("重新开始");

        JMenuItem exitItem=new JMenuItem("退出");

//        重新开始事件监听
        restartItem.addActionListener(e -> {
//            初始化地图
            gamePanel.initMap();
//                页面刷新
            gamePanel.repaint();
//            关键号
            gamePanel.keySize=System.currentTimeMillis();

        });

//        退出事件监听
        exitItem.addActionListener(e -> {
            int option =JOptionPane.showConfirmDialog(gamePanel,"是否确定退出？","确认框",JOptionPane.YES_NO_OPTION);
            if(option==JOptionPane.YES_OPTION){
//                确定退出
                System.exit(0);
            }
        });

//        自定义构造器
        gamePanel=new GamePanel();


//        加入画框
        this.add(gamePanel);

//        添加线程监听是否死锁
         new Thread(gamePanel).start();

//     菜单组件整合
        jMenu.add(restartItem);
        jMenu.add(exitItem);
        menuBar.add(jMenu);
        this.setJMenuBar(menuBar);


        this.setTitle("连连看小游戏");
//          设置界面大小
        this.setSize(GamePanel.LENGTH*51,GamePanel.LENGTH*54);
        //刷新页面
//        this.repaint();
//        按 x 之后结束进程
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        面板可展示
        this.setVisible(true);
//    设置窗口大小不可改变
        this.setResizable(false);
//        窗口居中
        this.setLocationRelativeTo(null);

    }

    public static void main(String[] args) throws IOException {
        new GameClient();


    }

}
