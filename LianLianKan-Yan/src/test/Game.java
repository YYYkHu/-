import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

/**
 * @author 狐狸半面添
 * @create 2022-12-02 1:18
 */
public class Game {
    private static GamePanel gamePanel;

    public static void main(String[] args) throws IOException {
        //画一个窗体
        JFrame gameFrame = new JFrame("连连看");
        //设置宽和高
        gameFrame.setSize(GamePanel.LENGTH * 51, GamePanel.LENGTH * 54);
        //设置一个菜单条
        JMenuBar menuBar = new JMenuBar();
        //设置一个菜单组件
        JMenu jMenu = new JMenu("设置");
        //设置菜单项
        JMenuItem restartItem = new JMenuItem("重新开始");
        //增加重新开始监听事件 --> 重新初始化地图
        restartItem.addActionListener(e-> {
            //初始化地图
            gamePanel.initMap();
            //刷新版本号
            gamePanel.nowVersion = System.currentTimeMillis();
            //刷新页面
            gamePanel.repaint();
        });
        //设置菜单项 - 退出
        JMenuItem exitItem = new JMenuItem("退出");
        exitItem.addActionListener(e-> {
            int option = JOptionPane.showConfirmDialog(gameFrame, "您确认退出吗？", "确认框", JOptionPane.YES_NO_OPTION);
            if (option == JOptionPane.YES_OPTION) {
                //选择了确定则退出程序
                System.exit(0);
            }
        });
        //将组件整合
        jMenu.add(restartItem);
        jMenu.add(exitItem);
        menuBar.add(jMenu);
        gameFrame.setJMenuBar(menuBar);

        //往窗口添加一个自定义容器
        gamePanel = new GamePanel(gameFrame);
        gameFrame.add(gamePanel);

        //开启线程监听当前是否死局
        new Thread(gamePanel).start();

        //设置窗口的图标/logo
        gameFrame.setIconImage(ImageIO.read(new File(Game.class.getResource("/").getPath()+"img/logo.png")));

        //展示窗体
        gameFrame.setVisible(true);
        //点击 x 即可退出程序
        gameFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        //设置窗口大小不可改变
        gameFrame.setResizable(false);
        //设置窗口位置到屏幕中间
        gameFrame.setLocationRelativeTo(null);
    }
}



//——————————————————————————————————————————————————————————————————————————————
class GamePanel extends JPanel implements Runnable {
    /**
     * 设置地图长度
     */
    public static final int LENGTH = 14;
    /**
     * 设置随机范围 --> 随机的图片数目
     */
    private final int RANDOM_NUM = 8;
    /**
     * 存储加载的RANDOM_NUM张图片
     */
    private Image[] images;
    /**
     * 地图
     */
    private int[][] map;
    /**
     * 临时地图去判断死局
     */
    private int[][] runMap;
    /**
     * 记录被选中的点的坐标
     */
    private Point selectedPoint;
    /**
     * 是否被选中，默认为 false
     */
    private boolean isSelected;
    /**
     * 保存当前地图的访问信息
     */
    private int[][] isVisited;
    /**
     * 保存临时地图的访问信息
     */
    private int[][] isVisitedForThread;
    /**
     * 保存访问路径
     * [i][0] 是横坐标
     * [i][1] 是纵坐标
     */
    private final int[][] tempPath = new int[100][2];
    /**
     * 保存最短的访问路径
     * [i][0] 是纵坐标
     * [i][1] 是横坐标
     */
    private final int[][] minPath = new int[100][2];

    /**
     * dx是保存四个方向在横坐标上的位置
     * 方向数组：对应 右 下 左 上
     */
    private final int[] dx = {1, 0, -1, 0};
    /**
     * dy是保存四个方向在横坐标上的位置
     * 方向数组：对应 右 下 左 上
     */
    private final int[] dy = {0, 1, 0, -1};
    /**
     * 记录最小步数
     */
    private int minStep;
    /**
     * 临时变量
     */
    private int temp;

    /**
     * 当前地图上剩余的图片数目
     */
    private int count;

    private final JFrame parentFrame;

    /**
     * 当前版本号
     * 判断一个地图是否为死局需要时间，而在这个过程中如果进行其它操作比如“重新开始游戏”就可能会显示上一张地图是死锁
     * 这是没有必要的，因此加个版本号进行判断
     */
    public long nowVersion;


    /**
     * 初始化画板
     *
     * @param parentFrame 父窗口
     * @throws IOException 异常
     */

//    构造方法
//    ——————————————————————————————————————————————————————————————————————————
    public GamePanel(JFrame parentFrame) throws IOException {
        //设置父节点
        this.parentFrame = parentFrame;
        //获取类路径
        String imgBasePath = this.getClass().getResource("/").getPath() + "img/";

        //加载图片存入数组
        images = new Image[RANDOM_NUM + 1];
        for (int i = 0; i <= RANDOM_NUM; i++) {
            images[i] = ImageIO.read(new File(imgBasePath + i + ".jpg"));
        }

        //初始化地图
        initMap();
        //创建选择点
        selectedPoint = new Point();

        //连连看监听
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                //获取当前的点击坐标
                int x = e.getX() / 50;
                int y = e.getY() / 50;
                //如果超出真实地图范围则取消下一步操作
                if (x >= LENGTH || y >= LENGTH) {
                    return;
                }

                Graphics g = getGraphics();
                g.setColor(Color.RED);

                //如果不是空，就可以 选中/连接 判断
                if (map[y][x] != 0) {
                    //如果当前没有选中的图像
                    if (!isSelected) {
                        //就设置当前点击到的图像为选中图像
                        isSelected = true;
                        selectedPoint.x = x;
                        selectedPoint.y = y;
                        g.drawRect(50 * x, 50 * y, 50, 50);
                    } else if (selectedPoint.x == x && selectedPoint.y == y) {
                        //如果之前选中了图片，现在是重复点击，就取消选中图片的选中
                        isSelected = false;
                        repaint();
                    } else {
                        //进入else说明选中了两张不同位置的图片

                        //如果两张图片不相同
                        if (map[selectedPoint.y][selectedPoint.x] != map[y][x]) {
                            //就取消已选中的图片的选中
                            isSelected = false;
                            repaint();
                        } else {
                            //进入else说明要去真正判断是否可连了

                            //如果可以直连，即无折点
                            if (isLineLink(x, y, selectedPoint.x, selectedPoint.y)) {
                                //就画线连接
                                g.drawRect(50 * x, 50 * y, 50, 50);
                                g.drawLine(50 * x + 25, 50 * y + 25, selectedPoint.x * 50 + 25, selectedPoint.y * 50 + 25);
                                try {
                                    //暂停1s，主要是为了能看清画线
                                    Thread.sleep(1000);
                                } catch (InterruptedException ex) {
                                    ex.printStackTrace();
                                }

                                //将两个点的坐标标记为0即空白了
                                map[y][x] = 0;
                                map[selectedPoint.y][selectedPoint.x] = 0;

                                //减少当前地图剩余图片的数量
                                count -= 2;

                                //修改版本号
                                nowVersion = System.currentTimeMillis();
                            }
                            else if (isLinkByOne(x, y, selectedPoint.x, selectedPoint.y, g)) {
                                //如果可以在只经过一个折点的情况下直连

                                g.drawRect(50 * x, 50 * y, 50, 50);
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException ex) {
                                    throw new RuntimeException(ex);
                                }
                                map[y][x] = 0;
                                map[selectedPoint.y][selectedPoint.x] = 0;
                                count -= 2;
                                nowVersion = System.currentTimeMillis();

                            }
                            else {

                                //初始化深度优先搜索需要用到的访问情况数组与最短路径
                                fillZeroAndSetMinPath();

                                //进行深度优先搜索  --> 找到了基于两个折点的最短路径就会改变 minStep
                                isLinkByTwo(x, y, selectedPoint.x, selectedPoint.y, 0, -1, 0);
                                //如果不是 Integer.MAX_VALUE，说明找到了最大值
                                if (minStep != Integer.MAX_VALUE) {
                                    int ky = minPath[0][0], kx = minPath[0][1];

                                    //进行路径的显示连接在窗口上
                                    for (int i = 1; i <= minStep; i++) {
                                        g.drawLine(50 * kx + 25, 50 * ky + 25, minPath[i][1] * 50 + 25, minPath[i][0] * 50 + 25);
                                        ky = minPath[i][0];
                                        kx = minPath[i][1];
                                    }

                                    try {
                                        Thread.sleep(1000);
                                    } catch (InterruptedException ex) {
                                        throw new RuntimeException(ex);
                                    }
                                    map[y][x] = 0;
                                    map[selectedPoint.y][selectedPoint.x] = 0;
                                    count -= 2;
                                    nowVersion = System.currentTimeMillis();
                                }
                            }

                            //不管是否可连，都需要取消当前选中
                            isSelected = false;
                            //重绘画板
                            repaint();
                            //发现当前无剩余图片就弹出提醒：游戏结束
                            if (count == 0) {
                                JOptionPane.showMessageDialog(parentFrame, "游戏结束！", "success", JOptionPane.INFORMATION_MESSAGE);
                            }
                        }
                    }
                }
            }
        });
    }

    /**
     * 绘制地图
     *
     * @param g  the <code>Graphics</code> context in which to paint
     */
    @Override
    public void paint(Graphics g) {
        for (int i = 0; i < LENGTH; i++) {
            for (int j = 0; j < LENGTH; j++) {
                g.drawImage(images[map[i][j]], 50 * j, 50 * i, 50, 50, null);
            }
        }
    }

    /**
     * 判断是否可以直连
     *
     * @param x1 当前位置点的横坐标
     * @param y1 当前位置点的纵坐标
     * @param x2 目标位置点的横坐标
     * @param y2 目标位置点的纵坐标
     * @return 是否可连
     */
    public boolean isLineLink(int x1, int y1, int x2, int y2) {
        if (x1 == x2) {
            int minY = Math.min(y1, y2) + 1;
            int maxY = Math.max(y1, y2);
            while (minY < maxY) {
                if (map[minY][x1] != 0) {
                    return false;
                }
                minY++;
            }
            return true;
        } else if (y1 == y2) {
            int minX = Math.min(x1, x2) + 1;
            int maxX = Math.max(x1, x2);
            while (minX < maxX) {
                if (map[y2][minX] != 0) {
                    return false;
                }
                minX++;
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * 判断是否可以通过一次折点连通
     *
     * @param x1 当前位置点的横坐标
     * @param y1 当前位置点的纵坐标
     * @param x2 目标位置点的横坐标
     * @param y2 目标位置点的纵坐标
     * @param g 画笔
     * @return 是否可连
     */
    public boolean isLinkByOne(int x1, int y1, int x2, int y2, Graphics g) {
        g.setColor(Color.RED);
        if (isLineLink(x1, y1, x1, y2) && map[y2][x1] == 0 && isLineLink(x1, y2, x2, y2)) {
            g.drawLine(x1 * 50 + 25, y1 * 50 + 25, x1 * 50 + 25, y2 * 50 + 25);
            g.drawLine(x1 * 50 + 25, y2 * 50 + 25, x2 * 50 + 25, y2 * 50 + 25);
            return true;
        } else if (isLineLink(x1, y1, x2, y1) && map[y1][x2] == 0 && isLineLink(x2, y1, x2, y2)) {
            g.drawLine(x1 * 50 + 25, y1 * 50 + 25, x2 * 50 + 25, y1 * 50 + 25);
            g.drawLine(x2 * 50 + 25, y1 * 50 + 25, x2 * 50 + 25, y2 * 50 + 25);
            return true;
        }
        return false;
    }

    /**
     * 深度优先搜索
     * 判断是否可以通过两次折点连通 - 如果存在，在此基础上得到最短路径
     *
     * @param x1 当前位置点的横坐标
     * @param y1 当前位置点的纵坐标
     * @param x2 目标位置点的横坐标
     * @param y2 目标位置点的纵坐标
     * @param inflectionPointNum 当前折点的次数
     * @param direction 当前方向：无（-1） 右（0） 下（1） 左（2） 右（3）
     * @param step 当前走的步数
     */
    public void isLinkByTwo(int x1, int y1, int x2, int y2, int inflectionPointNum, int direction, int step) {

        //发现折点次数达到三次就返回，表示路径不行
        if (inflectionPointNum == 3) {
            return;
        }

        //记录该点位置到路径数组中
        tempPath[step][0] = y1;
        tempPath[step][1] = x1;

        //查看是否到达目标点
        if (x1 == x2 && y1 == y2) {

            //说明找到了更短的，就将这个更短路径放入到最短路径数组中存储
            if (step < minStep) {
                minStep = step;
                for (int i = 0; i <= minStep; i++) {
                    minPath[i][0] = tempPath[i][0];
                    minPath[i][1] = tempPath[i][1];
                }
            }
            return;
        }

        //标记当前点已经访问
        isVisited[y1][x1] = 1;

        int tx;
        int ty;
        //尝试不同的方向
        for (int i = 0; i < 4; i++) {
            tx = x1 + dx[i];
            ty = y1 + dy[i];
            /*
                1.
                    tx >= 0 && tx < LENGTH && ty >= 0 && ty < LENGTH 保证不越界
                    map[ty][tx] == 0 保证是空白区
                    isVisited[ty][tx] == 0 保证是未访问的
                2.
                    tx == x2 && ty == y2 说明是目标点
             */
            if (tx >= 0 && tx < LENGTH && ty >= 0 && ty < LENGTH && map[ty][tx] == 0 && isVisited[ty][tx] == 0 || tx == x2 && ty == y2) {
                //标记即将查找的点为 已访问
                isVisited[ty][tx] = 1;

                if (direction == -1) {
                    //如果无方向，这是第一次深度搜索的状态
                    isLinkByTwo(tx, ty, x2, y2, inflectionPointNum, i, step + 1);
                } else if (i != direction) {
                    //如果即将走的点的方向与原来行走的点方向不一致
                    isLinkByTwo(tx, ty, x2, y2, inflectionPointNum + 1, i, step + 1);
                } else {
                    //如果方向一致：可以与第一个if的代码合并，但为了层次更为清晰，这里分开写
                    isLinkByTwo(tx, ty, x2, y2, inflectionPointNum, i, step + 1);
                }

                //访问完毕，回溯回来就标记为未访问，给后面的点来查找
                isVisited[ty][tx] = 0;
            }
        }
    }

    /**
     * 初始化最短步数和访问数组（都标记为未访问）
     */
    public void fillZeroAndSetMinPath() {
        for (int i = 0; i < LENGTH; i++) {
            Arrays.fill(isVisited[i], 0);
        }
        minStep = Integer.MAX_VALUE;
    }

    /**
     * 初始化判断死局的临时地图的访问数组中的位置都为未访问
     */
    public void fillZeroForThread() {
        for (int i = 0; i < LENGTH; i++) {
            Arrays.fill(isVisitedForThread[i], 0);
        }
    }

    /**
     * 交换地图上的两个点的图片
     *
     * @param x1 第一个点的横坐标
     * @param y1 第一个点的纵坐标
     * @param x2 第二个点的横坐标
     * @param y2 第二个点的横坐标
     * @param map 地图
     */
    public void swap(int x1, int y1, int x2, int y2, int[][] map) {
        temp = map[y1][x1];
        map[y1][x1] = map[y2][x2];
        map[y2][x2] = temp;
    }

    /**
     * 初始化地图
     */
    public void initMap() {
        //初始化 10 * 10 的游戏界面，初始值为 0
        map = new int[LENGTH][LENGTH];
        runMap = new int[LENGTH][LENGTH];

        isVisited = new int[LENGTH][LENGTH];
        isVisitedForThread = new int[LENGTH][LENGTH];

        Random r = new Random();
        int randomNum;

        //随机数成对连续布局
        for (int i = 1; i <= LENGTH - 2; i++) {
            for (int j = 1; j <= LENGTH - 2; j++) {
                randomNum = r.nextInt(RANDOM_NUM) + 1;
                map[i][j] = randomNum;
                map[i][++j] = randomNum;
            }
        }

        //打乱图片
        shuffle(map);
        //重置当前剩余图片数量
        count = (LENGTH - 2) * (LENGTH - 2);
        //取消选中
        isSelected = false;
        //修改版本号
        nowVersion = System.currentTimeMillis();
    }

    /**
     * 用于监听地图是否死局
     */
    @Override
    public void run() {
        //死循环判断，除非程序退出
        while (true) {

            try {
                //每隔五秒判断一次地图状态
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            //获取到当前的版本号
            long version = nowVersion;

            //拷贝一份当前地图放入到临时地图
            for (int i = 1; i <= LENGTH - 2; i++) {
                for (int j = 1; j <= LENGTH - 2; j++) {
                    runMap[i][j] = map[i][j];
                }
            }

            //查看是否有可连点
            boolean isFind = pointSearch();

            //如果没有找到并且地图上还有点并且版本号还是原来的说明是原地图，就显示死局
            if (!isFind && count != 0 && version == nowVersion) {

                JOptionPane.showMessageDialog(parentFrame, "当前死局，将进行洗牌", "提醒", JOptionPane.PLAIN_MESSAGE);

                //进行洗牌，直到有可连点
                while (!isFind&& version == nowVersion) {

                    //洗牌
                    shuffle(map);

                    //获取临时地图
                    for (int i = 1; i <= LENGTH - 2; i++) {
                        for (int j = 1; j <= LENGTH - 2; j++) {
                            runMap[i][j] = map[i][j];
                        }
                    }

                    isFind = pointSearch();
                }

                //洗牌完毕，重绘地图
                if(version == nowVersion) {
                    repaint();
                    nowVersion = System.currentTimeMillis();
                }
            }

        }
    }

    /**
     * 对 map 地图进行洗牌，随机交换打乱位置
     * @param map 地图
     */
    public void shuffle(int[][] map) {
        int tx, ty;
        int num = 0;
        Random r = new Random();
        while (num++ < LENGTH) {
            for (int y = 1; y <= LENGTH - 2; y++) {
                for (int x = 1; x <= LENGTH - 2; x++) {
                    if(map[y][x]!=0) {
                        tx = r.nextInt(LENGTH - 2) + 1;
                        ty = r.nextInt(LENGTH - 2) + 1;
                        if(map[ty][tx]!=0) {
                            swap(tx, ty, x, y, map);
                        }
                    }
                }
            }
        }
    }

    /**
     * 搜索是否存在点可连
     *
     * @return 为true说明存在，false不存在
     */
    public boolean pointSearch() {
        boolean isFind = false;
        for (int i = 1; i <= LENGTH - 2; i++) {
            for (int j = 1; j <= LENGTH - 2; j++) {
                if (runMap[i][j] != 0) {
                    fillZeroForThread();
                    isFind = search(j, i, runMap[i][j], 0, -1, 0);
                }
                if (isFind) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 深度优先搜索
     * 搜索有没有值为value并与最初点可连的点
     *
     * @param x1 当前点的横坐标
     * @param y1 当前点的纵坐标
     * @param value 最初点的值
     * @param inflectionPointNum 拐点个数
     * @param direction 当前方向
     * @param step 当前步数
     * @return 是否找到
     */
    public boolean search(int x1, int y1, int value, int inflectionPointNum, int direction, int step) {
        if (inflectionPointNum == 3) {
            return false;
        }

        /*
            步数不为0是保证与刚开始的搜索不会冲突，否则刚已进入最初的搜索就会表明找到
         */
        if (value == runMap[y1][x1] && step != 0) {
            //说明找到了一条
            return true;
        }
        isVisitedForThread[y1][x1] = 1;

        int tx;
        int ty;
        boolean isFind = false;
        //尝试不同的方向
        for (int i = 0; i < 4 && !isFind; i++) {
            tx = x1 + dx[i];
            ty = y1 + dy[i];

            /*
                tx >= 0 && tx < LENGTH && ty >= 0 && ty < LENGTH && isVisitedForThread[ty][tx] == 0 不越界并且未被访问
                runMap[ty][tx] == 0 临时地图的该坐标为null则可走
                value == runMap[ty][tx] 临时地图的该坐标就为目标相等值则可走
             */
            if (tx >= 0 && tx < LENGTH && ty >= 0 && ty < LENGTH && isVisitedForThread[ty][tx] == 0 && (runMap[ty][tx] == 0 || value == runMap[ty][tx])) {
                isVisitedForThread[ty][tx] = 1;
                if (direction == -1) {
                    isFind = search(tx, ty, value, inflectionPointNum, i, step + 1);
                } else if (i != direction) {
                    isFind = search(tx, ty, value, inflectionPointNum + 1, i, step + 1);
                } else {
                    isFind = search(tx, ty, value, inflectionPointNum, i, step + 1);
                }
                isVisitedForThread[ty][tx] = 0;
            }
        }
        return isFind;
    }
}
