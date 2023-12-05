package com.example.lianliankanyan;

import org.w3c.dom.Node;

import javax.swing.*;
import java.awt.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.*;


public class GamePanel  extends JPanel implements Runnable {
//      写出游戏界面，以及交互的按钮
//      第一步，初始化窗口显示
//    第二步，插入图片，以及打乱顺序
//  第三步，  进行逻辑的判断，以及功能的实现

//    图片
    private Image[] pics;
    // 地图
    private int[][] map;
//    图片的数量
    private final int RANDOM_NUM=9;
//    地图长度
    public static final int LENGTH=12;
//    记录选中点的坐标
    private Point selectPoint1;
//    是否选中，默认false
    private  boolean isSelected;
//     记录消除的数量
    private int count;
//    关键号,记录当前地图的状态
    public long keySize;
//      判断死锁的临时地图
    private int [][] runMap;

//      保存死锁时临时地图的访问信息
    private int [][] temporaryMessageForRunMap;

//       记录临时地图
    private int [][] dis;

    //    四个方向，下，上，右，左
    private final int[] dx = {0, 0, 1, -1};
    private final int[] dy = {1, -1, 0, 0};

    //    每个单元格包含元素:坐标,方向,以及拐点数量
    static class Pair{
        int row,cols,turns, direction;
        Pair(int x,int y,int turns,int direction){
            this.row=x;
            this.cols=y;
            this.turns=turns;
            this.direction=direction;
        }
    }

//————————————————————————————————————————————————————————————-
//    构造方法
    public GamePanel() throws IOException {

//      加载图片
    getPics();

//  初始化地图
    initMap();

//    创造一个点击点
    selectPoint1=new Point();


//    事件监听
    addMouseListener(new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
            int clientX = e.getX() / 50;
            int clientY = e.getY() / 50;
//            范围判断
            if (clientX >= LENGTH || clientY >= LENGTH) {
                return;
            }
//            设施画笔的颜色
            Graphics g = getGraphics();
            g.setColor(Color.green);

            if (map[clientY][clientX] != 0) {
//      之前没被点击过
                if (!isSelected) {

                    selectPoint1.x = clientX;
                    selectPoint1.y = clientY;
                    isSelected = true;
//                    把 X 的值与 Y 的值搞反了，导致了bug,  已解决
                    g.drawRect(50 * clientX, 50 * clientY, 50, 50);
                }
//      两次点击为同一位置
                else if (selectPoint1.x == clientX && selectPoint1.y == clientY) {
//                    取消选中状态
                    isSelected = false;
                    repaint();
                } else {
//                System.exit(0);  调试
//      两张图片不同，并且位置也不同
                    if (map[selectPoint1.y][selectPoint1.x] != map[clientY][clientX]) {
                        isSelected = false;
                        repaint();
                    }
//                  进入消除逻辑
                    else {
//                        System.exit(0);
//                    直连,无折点
                        if(oneLineLink(selectPoint1.x, selectPoint1.y,clientX,clientY)){
                            getGraphics().drawRect(50* clientX,50* clientY,50,50);
                            g.drawLine(50 * clientX + 25, 50 * clientY + 25, selectPoint1.x * 50 + 25, selectPoint1.y * 50 + 25);
    //                        判断为成功连接

                              accessLink(selectPoint1.x, selectPoint1.y,clientX,clientY);
                            repaint();
                            System.out.println(map[selectPoint1.y][selectPoint1.x]+" "+map[clientY][clientX]);
                            System.out.println(101);     ////   1
                        }
//

//                   一个折点
                        else if(oneCornerLink(selectPoint1.x, selectPoint1.y,clientX,clientY)){
                            getGraphics().drawRect(50* clientX,50* clientY,50,50);

    //                        判断为成功连接
                            accessLink(selectPoint1.x, selectPoint1.y,clientX,clientY);
                            repaint();
//                            调试
                            System.out.println(map[selectPoint1.y][selectPoint1.x]+" "+map[clientY][clientX]);

                            System.out.println(102);
                        }
//                    两个折点
                        else if(twoCornerLink(selectPoint1.x, selectPoint1.y,clientX,clientY) ){

                            System.out.println(100);    /////    0
                            accessLink(selectPoint1.x, selectPoint1.y,clientX,clientY);
                            repaint();
                            System.out.println(103);    /////   3
                        }
                    }
                    isSelected=false;
                    repaint();

                    System.out.println(104);

                    if(count==0){
                      String msg="游戏结束！！";
                      int type=JOptionPane.YES_OPTION;
                      String title="恭喜完成游戏！！！";
                      int choice=JOptionPane.showConfirmDialog(null,msg,title,type);
                      if(choice==1){
                          System.exit(0);
                      }else {
                          initMap();
                      }
                    }
                }

            }
        }
    });

    }

//    两个折点

    private  boolean twoCornerLink(int clickX1, int clickY1, int clickX2, int clickY2){

//        初始化map[][]
        dis=new int [map.length][];
        for(int i = 0; i < map.length; i++) {
            dis[i] = map[i].clone();

        }
       System.out.println(Arrays.deepToString(dis));   ////  调试
//       System.out.println(Arrays.deepToString(map));

        System.out.println(100001);  ///////   1
//        声明队列
        LinkedList<Pair>queue=new LinkedList<Pair>();
//        创建数组用来存储路径
        Pair [][]prePosition=new Pair[LENGTH][LENGTH];
//       起点
        Pair pair = new Pair(clickX1, clickY1, 0, -1);
//        遍历路径时的终点
//        prePosition[0][0]= new Pair(-1, -1, 0, -1);

        prePosition[clickX1][clickY1]= new Pair(-1, -1, 0, -1);
//        起点入队
        queue.addLast(pair);
//        标记路线，避免进入死循环
        dis[clickY1][clickX1]=1;
//        将终点加入路径数组
        dis[clickY2][clickX2]=0;

        while (!queue.isEmpty()){
            System.out.println(100002);   /////////   2
//            设置队列的大小
            int size=queue.size();
            for(int i=0;i<size;i++){
                System.out.println(100003);   //////////   3
//                队首元素出队
                Pair poll = queue.poll();
//                dis[poll.cols][poll.row]=1;

//                如果找到终点，调用printLink函数
                if(poll.row==clickX2 && poll.cols==clickY2  && poll.turns<=3){
//                    传入终点的坐标
                    printLink(prePosition,clickX2,clickY2);

                    System.out.println(100004); ///////    4
                    return true;
                }
                 else  {
                    Pair pre= new Pair(poll.row, poll.cols, poll.turns, poll.direction);
//                    遍历四个方向,寻找最短路径
                    for(int j=0;j<4;j++) {
                        System.out.println(100005);  ///////   5
                        int nextX = poll.row + dx[j];
                        int nextY = poll.cols + dy[j];
                        int nextTurns = poll.turns;
                        System.out.println(nextX+" "+ nextY+" "+nextTurns);
//                        System.out.println(dis[nextY][nextX]);
//                        当前结点的方向与前面节点不同时,拐点数量+1
                        if (j==poll.direction){
                            System.out.println(100007);  ////////   7
                        }else{
                            nextTurns++;
                        }
                        if(isBFSVisited(dis,nextX,nextY,nextTurns)){

                            System.out.println(100006);   ///////  6
//                            加入该方向点的坐标,拐点数,方向
                            queue.addLast(new Pair(nextX, nextY, nextTurns, j));
                            System.out.println(nextX+" "+nextY+ " "+nextTurns+" "+j);   ///  调试
                            prePosition[nextX][nextY]=pre;
                        }

                    }
                    dis[poll.cols][poll.row]=1;
                }
            }
        }return false;


    }

//    广度优先 判断是否满足搜索条件
//    代码优化，加入了一个对nextTurns的条件判断，来控制入队的结点数量，降低时间复杂度，减少内存开销
    public static boolean isBFSVisited(int[][] dis,int x,int y,int nextTurns){
//        dis[y][x]  !!!!!!不是dis[x][y]
        return x>=0 && y>=0 && x<=LENGTH-1 && y<=LENGTH-1 && dis[y][x]==0 && nextTurns<4;
    }


//        画出两个拐点的路径
    private void  printLink(Pair[][] prePosition,int row,int cols){
        System.out.println(row +" "+cols);
        if(row!=-1 && cols!=-1){
            Pair pair=prePosition[row][cols];

            int tempX= pair.row;
            int tempY= pair.cols;
            if(tempX!=-1 && tempY!=-1) {
                getGraphics().drawLine(50 * tempX + 25, 50 * tempY + 25, row * 50 + 25, cols * 50 + 25);
            }
            printLink(prePosition,pair.row,pair.cols);
        }
    }




//    一个拐点
    private boolean oneCornerLink(int clickX1,int clickY1,int clickX2,int clickY2){
        getGraphics().setColor(Color.red);
        if(oneLineLink(clickX1,clickY1,clickX2,clickY1)
                && oneLineLink(clickX2,clickY2,clickX2,clickY1)
                    && map[clickY1][clickX2]==0){
            getGraphics().drawLine(clickX1* 50 + 25,  clickY1* 50 + 25,  clickX2* 50 + 25, clickY1* 50 + 25);
            getGraphics().drawLine(clickX2 * 50 + 25, clickY2 * 50 + 25, clickX2 * 50 + 25, clickY1 * 50 + 25);
            return true;
        }else if(oneLineLink(clickX1,clickY1,clickX1,clickY2)
                && oneLineLink(clickX2,clickY2,clickX1,clickY2)
                && map[clickY2][clickX1]==0){
            getGraphics().drawLine(clickX1* 50 + 25,  clickY1* 50 + 25,  clickX1* 50 + 25, clickY2* 50 + 25);
            getGraphics().drawLine(clickX2 * 50 + 25, clickY2 * 50 + 25, clickX1 * 50 + 25, clickY2 * 50 + 25);
            return true;
        }
        else{
            return false;
        }
    }
//          一条直线，无拐点
   private boolean oneLineLink(int clickX1,int clickY1,int clickX2,int clickY2){
       getGraphics().setColor(Color.red);
        if(clickX2==clickX1){
            int maxY=Math.max(clickY1,clickY2);
            int minY=Math.min(clickY1,clickY2);
            for(int i=minY+1;i<maxY;i++){
                if(map[i][clickX1]!=0){
                    return false;
                }
            }return true;
        }
        else if(clickY2==clickY1){
            int maxX=Math.max(clickX1,clickX2);
            int minX=Math.min(clickX1,clickX2);
            for(int i=minX+1;i<maxX;i++){
                if(map[clickY1][i]!=0){
                    return false;
                }
            }return true;
        }
        else{
            return false;
        }

    }

//      判定成功连接后
        private void accessLink(int x1,int y1,int x2, int y2){

            try{
                Thread.sleep(500);
            }catch (InterruptedException ex){
                ex.printStackTrace();
            }
            repaint();
            map[y1][x1]=0;
            map[y2][x2]=0;
            count-=2;
//             记录当前的游戏状态
            keySize=System.currentTimeMillis();
        }

        public void randomBuildMap(int[][] map){
//
            ArrayList elements=new ArrayList();
            for (int i = 1; i <LENGTH-1 ; i++) {
                for(int j=0;j<LENGTH-1;j++){
                    if(map[i][j]!=0){
                        elements.add(map[i][j]);
                    }
                }
            }
            Collections.shuffle(elements);
            for (int i = 1; i <LENGTH-1 ; i++) {
                for(int j=1;j<LENGTH-1;j++) {
                        if(map[i][j]!=0) map[i][j] = (int) elements.remove(elements.size()-1);
                }
            }
        }

    //    获得图片
    private void getPics(){
        pics=new Image[10];
//        int i=1;
        for(int i=1;i<10;i++){
            Toolkit toolkit=Toolkit.getDefaultToolkit();
            this.pics[i]=toolkit.getImage("src/images/pic"+i+".png");
        }
    }


//    画图
    public void paint(Graphics g) {
        for (int i = 1; i < LENGTH-1; i++) {
             for (int j = 1; j < LENGTH-1; j++) {
            g.drawImage(pics[map[i][j]], 50 * j, 50 * i, 50, 50, null);
            }
//             g.fillRect(0,0,1000,750);
        }
    }


//    初始化地图
    public void initMap(){
        map=new int[LENGTH][LENGTH];
//        生成随机数
        Random r=new Random();
        int randomNum;
        for(int i=1;i<LENGTH-1;i++){
            for(int j=1;j<LENGTH-1;j++){
                randomNum=r.nextInt(RANDOM_NUM)+1;
                map[i][j]=randomNum;
                map[i][++j] = randomNum;
            }
        }
        keySize=System.currentTimeMillis();
        //打乱图片
        randomBuildMap(map);
        //重置剩余图片数量
        count = (LENGTH - 2) * (LENGTH - 2);
        //取消选中
        isSelected = false;
    }




//       深度优先搜索监听地图是否死锁
    public void run(){
        int times=0;
        while (true){
            System.out.println(++times);
            try{
                Thread.sleep(1000);
            }catch (InterruptedException e){
                throw new RuntimeException(e);
            }

//            拷贝当前的地图
            runMap=new int [map.length][];
            for(int i = 0; i < map.length; i++) {
                runMap[i] = map[i].clone();

            }

//            寻找是否存在可以连接的点
            boolean haveFind=pointSearch();

            long key=keySize;

//            如果找到，且地图数组未更新  关键号
            if(!haveFind && count==0 ){
                JOptionPane.showMessageDialog(getParent(), "当前地图进入死锁，接下来将重构地图布局", "提示", JOptionPane.PLAIN_MESSAGE);

                while(!haveFind && key==keySize){
//                    重构
                    randomBuildMap(map);

                    for (int i = 1; i <LENGTH - 1; i++) {
                        for (int j = 1; j <LENGTH - 1; j++) {
                            runMap[i][j] = map[i][j];
                        }
                    }

                    haveFind=pointSearch();
                }
//                重构完成，刷新地图
                repaint();
                key=System.currentTimeMillis();
            }

        }
    }

//      判断是否存在可连点，使用深度优先搜索
    public boolean pointSearch(){

        boolean haveFind=false;

        for(int i=1;i<LENGTH-1;i++){
            for(int j=1;j<LENGTH-1;j++) {
                if(runMap[i][j]!=0){
//                    将临时的地图加入队列
                    temporaryMessageForRunMap=new int[runMap.length+2][runMap.length+2];
                    for (int k = 0; k < LENGTH; k++) {
                       temporaryMessageForRunMap[k]=runMap[i].clone();
                    }

//                    dis=new int [map.length][];
//                    for(int i = 0; i < map.length; i++) {
//                        dis[i] = map[i].clone();

                    haveFind=dfsSearch(j,i,runMap[i][j],0,-1,0);
                }
//                每一个方块都将判断
                if(haveFind) return true;
            }

        }
        return  false;
    }
//      判断是否存在可连点，使用深度优先搜索
    public boolean dfsSearch(int x,int y,int value,int Turns,int direction,int path) {
        if (Turns >= 3) {
            return false;

        }
        boolean haveFind = false;
        if (x >= 0 && y >= 0) {
//            说明存在一条路径
            if ( runMap[y][x] == value && path != 0){
                return true;
            }

//            标记为已访问
            temporaryMessageForRunMap[y][x] = 1;
//            四个方向
            int tempX, tempY;


            for (int i = 0; i < 4; i++) {
                tempX = x + dx[i];
                tempY = y + dy[i];

                if (isDFSVisited(tempX, tempY, value)) {
//                满足条件则进行标记
                    temporaryMessageForRunMap[y][x] = 1;

//            第一次递归
                    if (direction == -1) {
                        haveFind = dfsSearch(tempX, tempY, value, Turns, i, path + 1);
                    } else if (i == direction) {
                        haveFind = dfsSearch(tempX, tempY, value, Turns, i, path + 1);
                    } else {
                        haveFind = dfsSearch(tempX, tempY, value, Turns + 1, i, path + 1);
                    }
//            回溯是将其标记为未访问
                    temporaryMessageForRunMap[tempY][tempX] = 0;
                }
            }

        }
        return haveFind;
    }

//    深度优先搜索判断条件
        public  boolean isDFSVisited(int x,int y,int value){

            return x >= 0 && x < LENGTH && y >= 0 && y < LENGTH
                    && temporaryMessageForRunMap[y][x] == 0
                    && (runMap[y][x] == 0 || value == runMap[y][x]);
        }

}
