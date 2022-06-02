package Tetris1;

import java.util.*;
import java.util.List;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class Tetris {
   public static void main(String[] args) {
      Game game = new Game();
   }
}

//ȭ�� ������ ����� - ���� ������
class Game extends JFrame {
//   ������ ����: SIZE(30)*HOR(12)=360
   final static int WIDTH = PlayPanel.Block.SIZE * PlayPanel.NUM_OF_BLOCKS_HOR + 20;
//   ������ ����: SIZE(30)*VER(20)+20=620
   final static int HEIGHT = PlayPanel.Block.SIZE * PlayPanel.NUM_OF_BLOCKS_VER + 40;

   StartPanel startPanel = new StartPanel();
   PlayPanel playPanel;

   public Game() { // ������
      setVisible(true);
      setResizable(false); // ������ ������ ����
      setTitle("Tetris");
      
      add(startPanel); // ó�� ���� ȭ��
      setSize(WIDTH, HEIGHT);

      //��� ��ư�� Ŭ���Ǿ����� Ȯ��
      startPanel.easy.addActionListener(new MyActionListener());
      startPanel.normal.addActionListener(new MyActionListener());
      startPanel.hard.addActionListener(new MyActionListener());
      setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

   }

   // StartPanel���� GamePanel�� �г� ��ȯ -> ���� �ٷ� ����
   public void change(String level) { 
      getContentPane().removeAll(); // ���� �г�(��, StartPanel)�� removeAll
      playPanel = new PlayPanel(level); // GamePanel�� add�ϰ� �̸� CENTER�� ��ġ��Ŵ
      getContentPane().add(playPanel, BorderLayout.CENTER);
      playPanel.requestFocus();

      // ȭ�� �����
      revalidate(); // ���� �ڵ� ���̾ƿ�
      repaint(); //�ʺ�/���̿� �������ִ� �Ӽ��� ������ �� ȣ��ǰ� ��翡 �������ִ� �Ӽ��� ������ �� repaint()�� ȣ��
   }

   // ��ư Ŭ�� �� ȭ���� ��ȯ�ϵ��� ����
   class MyActionListener implements ActionListener {
      public void actionPerformed(ActionEvent e) {
         JButton btn = (JButton) e.getSource();
         if (btn.getText().equals("EASY"))
            change("EASY");
         else if (btn.getText().equals("NORMAL"))
            change("NORMAL");
         else if (btn.getText().equals("HARD"))
            change("HARD");
      }
   }
}

@SuppressWarnings("serial")
class StartPanel extends JPanel { // ���� ȭ��
   ImageIcon icon = new ImageIcon("title.png"); // ����� �̹����� ����
   // ���� ��ư ����
   StartButton easy = new StartButton("EASY"); 
   StartButton normal = new StartButton("NORMAL");
   StartButton hard = new StartButton("HARD");

   public StartPanel() {
      add(easy);
      add(normal);
      add(hard);
   }
   
   // ȭ���� �׸�
   public void paintComponent(Graphics g) { 
      g.drawImage(icon.getImage(), 0, 0, null);
      setOpaque(false);
      super.paintComponent(g);
      setLayout(null);
      
      // ��ư ��ġ ����
      easy.setBounds(130, 315, 100, 35);
      normal.setBounds(115, 355, 130, 35);
      hard.setBounds(130, 395, 100, 35);
   }
   
   // ���� ��ư
   class StartButton extends JButton {
      ImageIcon icon;

      public StartButton(String level) {
         setText(level);
         if (level.equals("EASY")) {
            icon = new ImageIcon("EasyButton.png"); // �̹��� ��ư
         } else if (level.equals("NORMAL")) {
            icon = new ImageIcon("NormalButton.png");
         } else if (level.equals("HARD")) {
            icon = new ImageIcon("HardButton.png");
         }
         setFont(new Font("���� ���", Font.BOLD, 0));
         setContentAreaFilled(false); // ��ư ���� �����ϰ� �����
      }

      public void paintComponent(Graphics g) {
         g.drawImage(icon.getImage(), 0, 0, null);
         setOpaque(false);
         super.paintComponent(g);
         setLayout(null);
         setFocusPainted(false);
      }
   }
}

//���� ȭ�� ������  - ���� ������
@SuppressWarnings("serial")
class PlayPanel extends JPanel {
   static int NUM_OF_BLOCKS_HOR = 12; // ���� 12 ���
   static int NUM_OF_BLOCKS_VER = 20; // ���� 20 ���

   //�����̸� �־ ��� �������� ���̿� ���� �ð� ����
   static int DELAY = 400; //��� �������� ���� �ð�
   final static int SUB_DELAY = 200; //�ٴ꿡 ���� �� ������ �� �ְ� �ϴ� ���� �ð�

   static int[][][] SHAPE = { // 3���� �迭�� ��� ����� ����
		 { { 0, 0 }, { 0, -1 }, { -1, 0 }, { -1, 1 } }, // S
         { { 0, 0 }, { 0, -1 }, { 1, 0 }, { 1, 1 } }, // Z
         { { 0, 0 }, { 0, -1 }, { 0, 2 }, { 0, 1 } }, // I
         { { 0, 0 }, { -1, 0 }, { 1, 0 }, { 0, 1 } }, // T
         { { 0, 0 }, { 0, -1 }, { -1, -1 }, { 0, 1 } }, // J
         { { 0, 0 }, { 0, -1 }, { 1, -1 }, { 0, 1 } }, // L
         { { 0, 0 }, { 1, 0 }, { 0, -1 }, { 1, -1 } } //O
   };

   Shape shape;
   Shape nextShape;
   ShapeShadow shapeShadow;
   List<Block> blocks;
   GameThread gameThread;
   boolean isAlive = true;

   int score;
   JPanel scorePanel; // ������ ���� ���� ������ ��Ÿ���� �г�
   JLabel scoreLabel, gameover, gameclear; // �гο� ���� ���� ���̺�� ���� ���� ���̺� ����

   // ����8, ����8
   public PlayPanel(String level) {
      setLevel(level);
      setVisible(true);
      setLayout(new BorderLayout()); // �߾� �迭�� ���� BorderLayout ���
      // ������ �˷��ִ� �г�
      scorePanel = new JPanel();
      scorePanel.setLayout(new BorderLayout());
      scorePanel.setOpaque(false); // ���� ������

      // �гο� ���� ���� Lable ����
      scoreLabel = new JLabel();
      scoreLabel.setForeground(Color.LIGHT_GRAY);
      scoreLabel.setFont(new Font(null, Font.BOLD, 20));
      scoreLabel.setHorizontalAlignment(JLabel.CENTER); // Label �߾� ��ġ
      scorePanel.add(scoreLabel, BorderLayout.CENTER); // scorePanel�� scoreLabel�� ����

      add(scorePanel, BorderLayout.NORTH); // scorePanel�� ���� ���ʿ� ��ġ
      revalidate();

      gameover = new JLabel(">>GAME OVER<<"); // ���� ���� Label ����
      gameover.setFont(new Font(null, Font.BOLD, 30));
      gameover.setForeground(Color.RED.darker());
      gameover.setHorizontalAlignment(JLabel.CENTER);

      gameclear = new JLabel(">>GAME CLEAR<<"); // ���� Ŭ���� Label ����
      gameclear.setFont(new Font(null, Font.BOLD, 15));
      gameclear.setForeground(Color.BLUE.brighter());
      gameclear.setHorizontalAlignment(JLabel.CENTER);

      blocks = new ArrayList<Block>();
      genShape(); // ���ο� ��� ����
      gameThread = new GameThread();

      gameThread.start();
      requestFocus();
      setFocusable(true);
      addKeyListener(new KeyListener()); // Ű�� �޵��� ��
      setBackground(new Color(0, 0, 0)); // ��� ȭ���� ���������� ����
   }

   public void setLevel(String text) { // ���� ����
      if (text.equals("EASY"))
         DELAY = 600;
      else if (text.equals("HARD"))
         DELAY = 150;
      else
         return;
   }

   class GameThread extends Thread { // ����� �������� ������
      public void run() { // ���� ����
         while (isAlive) { // ����ִ� ����, ��輱�� ����� ���� �ʾ��� ����
            while (!shape.isFallen()) { // ���� �������� ���� ����
               try {
                  Thread.sleep(DELAY); // ��������, ������ �и��ʵ��� ���� �����带 ����
               } catch (InterruptedException e) {
                  e.printStackTrace();
               }

               if (!shape.isFallen()) { // ���� ����� �������� �ʾҴٸ�, Move�� �̿��� �� ĭ ������
                  shape.Move(0, 1);
                  repaint(); // ������ ��, repaint�� ����� ����� ��ġ�� �׸�
               }
               
               //��ȸ�� �� �� �� �ֱ� ���� Ȯ����
               if (shape.isFallen()) {
                  try {
                     Thread.sleep(SUB_DELAY);
                  } catch (InterruptedException e) {
                     // TODO Auto-generated catch block
                     e.printStackTrace();
                  }
                  repaint();
               }
            }

            try {
            	// ���� ������ �� ������ ���� ���¸� ��.. �� try-catch���� ����� ������ ������ ����ǰ�, ���� ���� ���̺��� ��
               sleep(10);
            } catch (InterruptedException e) {
               // TODO Auto-generated catch block
               e.printStackTrace();
            }

            for (Block block : shape.nowBlocks) { // ���� �ִ� ��ϵ��� �ϳ��� �޾ƿͼ� ����
               blocks.add(block); // ���� ����� blocks�� ����
            }
            
            if (isGameOver()) { // ���� ���� ��, ���� ���¸� false�� �ٲٰ�, ���� ���� ���̺��� ����
               isAlive = false;
               add(gameover, BorderLayout.CENTER);
               break;
            }

            if (isLineEmpty(19)) {
               isAlive = false;
               add(gameclear, BorderLayout.CENTER);
               break;
            }

            // ������ ����� ���� �����ٵ��� �����Ͽ� ������ Ȯ���ϵ��� ��
            int[] linesToRemove = shape.getEffectiveYs();

            // �� ���� �������� �� ->�� á�� �� üũ
            for (int line : linesToRemove) { // ����� �������� ��, ����� ������ ���� �� ä���� �ִ� ���� Ȯ��
               if (isLineFull(line)) { // ���� �� ���� ��ȭ ���¶��,
                  removeLine(line); // removeLine���� �ش� ���� ����
               }
            }

            genShape(); // ���� ��� ����� ���� �̾ ���� ����
            repaint(); // ��� ���� ����� ��ġ�� �׸�
         }

         // ���� ���� ���� �� ��� ����
         repaint();
         revalidate();

      }

   }

   class KeyListener extends KeyAdapter { // Ű�� �Է¹���
      public void keyPressed(KeyEvent e) {

         int key = e.getKeyCode();

         switch (key) { // switch������ �Է¹��� Ű�� Ȯ���ϰ� �ش� ��� ����
         case KeyEvent.VK_UP: { // ��� ȸ��
            shape.tryRotate();
         }
            break;
         case KeyEvent.VK_DOWN: { // ����� �� ĭ �Ʒ���
            if (shape.canMove(0, 1)) {
               shape.Move(0, 1);
               shapeShadow.setLocation(shape.nowBlocks); // �׸��� ����� ��ġ�� ���� ����� ��ġ�� �̿��� ����
            }
         }
            break;
         case KeyEvent.VK_LEFT: {
            if (shape.canMove(-1, 0)) {
               shape.Move(-1, 0);
               shapeShadow.setLocation(shape.nowBlocks);
            }
         }
            break;
         case KeyEvent.VK_RIGHT: {
            if (shape.canMove(1, 0)) {
               shape.Move(1, 0);
               shapeShadow.setLocation(shape.nowBlocks);
            }
         }
            break;
         case KeyEvent.VK_SPACE: {
            shape.drop();
            break;
         }
         case KeyEvent.VK_ENTER: {
            newGame();
         }
         }

         repaint(); //����� ��ġ�� repaint
      }
   }

   class Block { // ���ӿ��� ����� Ŭ����
      final static int SIZE = 30; // �� ����� ũ��
      int x, y;
      int type;

      public Block(int type, int x, int y) { // �Ű����� 3���� ������
         this.type = type;
         this.x = x;
         this.y = y;
      }

      public Block(int type) { // �Ű����� 1���� ������ 
         this.type = type;
      }

      public Block(int x, int y) { // �Ű����� 2���� ������
         this.x = x;
         this.y = y;
      }

      // ����� �������� ������ ����� �̵�
      public void tryMove(int deltax, int deltay) {

    	  // �ٴ��� �ƴϰ� �����̽��ٸ� ������ �ʾҴٸ�, y+deltay�� �̵�
         if (deltay != 0 && !isFallen()) {
            y += deltay;
         }

         // �̵��Ϸ��� ��ġ�� ��ֹ��� �ִ��� Ȯ��
         else {
            if (!isBlockAt(x + deltax, y + deltay)) {

               x += deltax;
               y += deltay;
            }
         }
      }

      public boolean isFallen() { // ����� ������ ���
         if (isBlockAt(x, y + 1) || y == NUM_OF_BLOCKS_VER) { // �׿��ִ� ��ϰ� ��¥ �ٴ� ��� �ٴ����� ����
            return true;
         } else
            return false;
      }

      public void draw(Graphics g) { // ����� ����
         for (Block block : blocks) {  // �ڿ��� ���ʷ� ��ü�� ������ �տ��ٰ� �ִ´�. �ڿ��� ���� ��ü�� ���� ������
            setTypeColor(g, block.getType());
            g.fillRect((block.getX() * Block.SIZE) - Block.SIZE, (block.getY() * Block.SIZE) - Block.SIZE,
                  Block.SIZE, Block.SIZE);

            setTypeColorDarker(g, 7);
            g.drawRect((block.getX() * Block.SIZE) - Block.SIZE, (block.getY() * Block.SIZE) - Block.SIZE,
                  Block.SIZE, Block.SIZE);

         }
      }

      public void setX(int x) { // X�� ����
         this.x = x;
      }

      public void setY(int y) { // Y�� ����
         this.y = y;
      }

      public int getX() { // X ��ǥ ������
         return x;
      }

      public int getY() { // Y ��ǥ ������
         return y;
      }

      public int getType() { // ���� ����� ����� ������
         return type;
      }
   }

   class Shape { // ����� ���
      Block[] nowBlocks; // ���� �ִ� ��ϵ�
      Random random;
      int type;
      int[][] shape;

      public Shape() {
         random = new Random();
         type = random.nextInt(7); // 7���� ���� ����
         if (type == 7)
            type = 2; // I�� Ȯ���� 2��
         nowBlocks = new Block[4];
         shape = new int[4][2];

         System.arraycopy(SHAPE[type], 0, shape, 0, SHAPE[type].length);
         // arraycopy: �迭 �����ϱ�, SHAPE_COORD[type]�� shapeCoord�� ����, 0�� ó������ �����ϰ� ó������ �ֱ�
         for (int i = 0; i < 4; i++) { // ó�� ������ �� ȭ���� �߾ӿ� ��ġ
            nowBlocks[i] = new Block(type);
            nowBlocks[i].setX(shape[i][0] + NUM_OF_BLOCKS_HOR / 2);
            nowBlocks[i].setY(shape[i][1] + 2);
         }
      }
      
      // ������ ȸ����Ű�� �޼ҵ�
      public void tryRotate() {
         boolean canRotate = true;
         if (getType() == 6) { // O ����� ȸ���ص� �״���̹Ƿ� ȸ�� �ʿ� ����
            return;
         }

         int[][] tempPoint = new int[4][2]; // �ٲ� ��ġ�� ���Ƿ� ����

         for (int i = 0; i < 4; i++) {
            tempPoint[i][0] = -shape[i][1];// �ٲ� ���� ��ǥ�� X
            tempPoint[i][1] = shape[i][0]; // �ٲ� ���� ��ǥ�� Y

            if (isBlockAt(tempPoint[i][0] + nowBlocks[0].getX(), tempPoint[i][1] + nowBlocks[0].getY())) {
               canRotate = false;
               break;
            }
         }

         if (canRotate) { // ȸ���� �����ϴٸ�
            for (int i = 0; i < 4; i++) { // tempPoint�� ��ǥ�� shapeCoord�� ����
               shape[i][0] = tempPoint[i][0];
               shape[i][1] = tempPoint[i][1];

               nowBlocks[i].setX(shape[i][0] + nowBlocks[0].getX());
               nowBlocks[i].setY(shape[i][1] + nowBlocks[0].getY());

               shapeShadow.setLocation(nowBlocks);

               repaint();
            }
         } else
            System.out.println("unable to rotate ");
      }

      public int[] getEffectiveYs() { // ����� �����ϴ� Y��° ���� Ȯ���ϴ� �뵵
         int[] tempArr = { 100, 100, 100, 100 };
         int temp = 0, length = 0;
         boolean used = false;

         for (int i = 0; i < 4; i++) {
            used = false;

            for (int j = 0; j < i; j++) {
               if (nowBlocks[i].getY() == tempArr[j]) {
                  used = true;
                  break;
               }
            }

            if (!used) {
               tempArr[i] = nowBlocks[i].getY();
               length++;
            }
            // i��° y���� �ߺ��� �ƴϸ� �Էµǰ�,
            // �ߺ��̸� 100��ä�� �н�
         }

         for (int i = 0; i < 3; i++) {
            for (int j = i; j < 4; j++) {
               if (tempArr[i] > tempArr[j]) {
                  temp = tempArr[i];
                  tempArr[i] = tempArr[j];
                  tempArr[j] = temp;
               }
            }
         }

         int[] resultArr = new int[length];
         for (int i = 0; i < length; i++) {
            resultArr[i] = tempArr[i];
         }

         return resultArr;
      }

      public int getType() {
         return type;
      }

      public void draw(Graphics g) {
         for (Block block : nowBlocks) {
            setTypeColorDarker(g, block.getType()); // �׵θ��� ��ü
            g.fillRect((block.getX() * Block.SIZE) - Block.SIZE, (block.getY() * Block.SIZE) - Block.SIZE, // getX, getY�� ���� ���� �ϴ� ��ǥ
                  Block.SIZE, Block.SIZE); // getY�� ���� ���� �ϴ� ��ǥ
            setTypeColor(g, block.getType()); // ä��
            g.fillRect((block.getX() * Block.SIZE) - Block.SIZE + 2, (block.getY() * Block.SIZE) - Block.SIZE + 2,
                  Block.SIZE - 4, Block.SIZE - 4);
         }

      }

      public boolean isFallen() {
         boolean temp = false;

         for (Block block : nowBlocks) {
            if (block.isFallen()) {
               temp = true;
               break;
            }
         }

         return temp;
      }

      public boolean canMove(int deltax, int deltay) {
         boolean isOccupied = false;

         for (Block block : nowBlocks) {
            if (isBlockAt(block.getX() + deltax, block.getY() + deltay)) {
               isOccupied = true;
               break;
            }
         }

         return !isOccupied;
      }

      public void Move(int deltax, int deltay) {
         for (Block block : nowBlocks) {
            block.tryMove(deltax, deltay);
         }
      }

      public void drop() {
         while (!isFallen()) {
            Move(0, 1);
         }
      }

   }

   class ShapeShadow extends Shape {

      public ShapeShadow(Block[] nowBlocks) { // �׸��� ����
         for (int i = 0; i < 4; i++) {
            this.nowBlocks[i] = new Block(nowBlocks[i].getX(), nowBlocks[i].getY());
         }
         drop(); // shape�� ���� ��ǥ�� ������ �� ���Ź���.
      }

      public void draw(Graphics g) {

         for (Block block : nowBlocks) {
            setTypeColorDarker(g, 8);
            g.fillRect((block.getX() * Block.SIZE) - Block.SIZE, (block.getY() * Block.SIZE) - Block.SIZE,
                  Block.SIZE, Block.SIZE);
            setTypeColor(g, 8);
            g.fillRect((block.getX() * Block.SIZE) - Block.SIZE + 2, (block.getY() * Block.SIZE) - Block.SIZE + 2,
                  Block.SIZE - 4, Block.SIZE - 4);
         }
      }

      public void setLocation(Block[] nowBlocks) { // ���Ŀ� �ٲ� �� ��� ex) Ű �̺�Ʈ
    	 // System.arraycopy(nowBlocks, 0, this.nowBlocks, 0, nowBlocks.length);
         for (int i = 0; i < 4; i++) {
            this.nowBlocks[i] = new Block(nowBlocks[i].getX(), nowBlocks[i].getY());
         }
         drop(); // ��ġ�� 20���� �����ϴ� �� �ƴ϶� �׳� ���� ����
      }

   }

   public void newGame() {
      isAlive = true;
      score = 0;
      blocks = new ArrayList<Block>();
      genShape();
      remove(gameover); // gameover = ���� ���� ����
      remove(gameclear); // gameclear = ���� Ŭ���� ����
      gameThread = new GameThread();
      gameThread.start();
   }

   public boolean isGameOver() {
      for (int i = 0; i < 4; i++) {
         if (blocks.get(blocks.size() - 1 - i).getY() <= 4) // �� ������ ��� 4�̹Ƿ�
            return true;
      }
      return false;
   }

   public boolean isBlockAt(int x, int y) { // (x,y)�� ����� ������ true
      for (Block block : blocks) { // �ٸ� ���̶� �ε������� Ȯ��
         if (block.x == x && block.y == y) {
            return true;
         }
      }

      if (x == 0 || x == NUM_OF_BLOCKS_HOR + 1) // �¿� ���̶� �ε������� Ȯ��
         return true;
      if (y == NUM_OF_BLOCKS_VER + 1) // �ٴڿ� ����� Ȯ��
         return true;
      return false;
   }

   public boolean isLineEmpty(int y) {
      int count = 0;
      for (Block block : blocks) {
         if (block.getY() == y)
            return false;
      }
      return true;
   }

   public boolean isLineFull(int y) {
      int count = 0;
      for (Block block : blocks) {
         if (block.getY() == y)
            count++;
      }

      if (count == NUM_OF_BLOCKS_HOR) {
         return true;
      } else
         return false;

   }

   // y��° ���� ���� ���ش�.
   public void removeLine(int y) {
      int count = 0;
      int index = 0;
      while (count < NUM_OF_BLOCKS_HOR) {
         if (blocks.get(index).getY() == y) {
            blocks.remove(index); // ArrayList�� remove �� �� �����ؼ� �� �ڸ��� �ڵ����� ä���. �׷��� index �� �÷���
            count++;
         }

         else
            index++;
      }

      for (int i = 0; i < blocks.size(); i++) { // ���� ����� ���� �ٺ��� ���� �ִ� �� ������
         if (blocks.get(i).getY() < y) {
            blocks.get(i).setY(blocks.get(i).getY() + 1);
         }
      }

      score += 100; // �� �� ����� +100��!
      scoreLabel.setText("���� ���� : " + score);
   }

   public void setTypeColor(Graphics g, int type) {
      switch (type) {
      case 0:
         g.setColor(new Color(65, 105, 225)); // Royal Blue
         break;
      case 1:
         g.setColor(new Color(30, 144, 255)); // Doger Blue
         break;
      case 2:
         g.setColor(new Color(50, 205, 50)); // Lime-Green
         break;
      case 3:
         g.setColor(new Color(255, 69, 0)); // Orange-red
         break;
      case 4:
         g.setColor(new Color(255, 140, 0)); // Dark Orange
         break;
      case 5:
         g.setColor(new Color(123, 104, 238)); // Medium Slate Blue
         break;
      case 6:
         g.setColor(new Color(255, 215, 0)); // Gold
         break;
      case 7:
         g.setColor(Color.LIGHT_GRAY);
         break;
      case 8: // ShapeShadow ��
         g.setColor(Color.DARK_GRAY);
         break;
      }
   }

   public void setTypeColorDarker(Graphics g, int type) {
      switch (type) {
      case 0:
         g.setColor(new Color(65, 105, 225).darker());
         break;
      case 1:
         g.setColor(new Color(30, 144, 255).darker());
         break;
      case 2:
         g.setColor(new Color(50, 205, 50).darker());
         break;
      case 3:
         g.setColor(new Color(255, 69, 0).darker());
         break;
      case 4:
         g.setColor(new Color(255, 140, 0).darker());
         break;
      case 5:
         g.setColor(new Color(123, 104, 238).darker());
         break;
      case 6:
         g.setColor(new Color(255, 215, 0).darker());
         break;
      case 7:
         g.setColor(Color.LIGHT_GRAY.darker());
         break;
      case 8: // ShapeShadow Border ��
         g.setColor(Color.DARK_GRAY.darker());
         break;
      }
   }

   // �׵θ� �����
   public void drawBorder(Graphics g) {
      setTypeColor(g, 7);
      g.drawRect(0, 0, Block.SIZE * NUM_OF_BLOCKS_HOR, Block.SIZE * NUM_OF_BLOCKS_VER);
      g.drawLine(0, 4 * Block.SIZE, Block.SIZE * NUM_OF_BLOCKS_HOR, 4 * Block.SIZE);
   }

   // �� ����
   public void genShape() {
      shape = new Shape();
      shapeShadow = new ShapeShadow(shape.nowBlocks);

      score += 40;
      scoreLabel.setText("���� ���� : " + score);
   }

   // �� �ٲٴ� ��
   public void paintComponent(Graphics g) {
      if (isAlive) {
         super.paintComponent(g);

         shapeShadow.draw(g);
         shape.draw(g);

         for (Block block : blocks) {
            block.draw(g);
         }
         drawBorder(g);
      }

      else {
         super.paintComponent(g);
      }

   }

}