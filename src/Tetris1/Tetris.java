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

//화면 프레임 만들기 - 메인 프레임
class Game extends JFrame {
//   프레임 가로: SIZE(30)*HOR(12)=360
   final static int WIDTH = PlayPanel.Block.SIZE * PlayPanel.NUM_OF_BLOCKS_HOR + 20;
//   프레임 세로: SIZE(30)*VER(20)+20=620
   final static int HEIGHT = PlayPanel.Block.SIZE * PlayPanel.NUM_OF_BLOCKS_VER + 40;

   StartPanel startPanel = new StartPanel();
   PlayPanel playPanel;

   public Game() { // 생성자
      setVisible(true);
      setResizable(false); // 프레임 사이즈 고정
      setTitle("Tetris");
      
      add(startPanel); // 처음 시작 화면
      setSize(WIDTH, HEIGHT);

      //어느 버튼이 클릭되었는지 확인
      startPanel.easy.addActionListener(new MyActionListener());
      startPanel.normal.addActionListener(new MyActionListener());
      startPanel.hard.addActionListener(new MyActionListener());
      setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

   }

   // StartPanel에서 GamePanel로 패널 전환 -> 게임 바로 시작
   public void change(String level) { 
      getContentPane().removeAll(); // 현재 패널(즉, StartPanel)을 removeAll
      playPanel = new PlayPanel(level); // GamePanel을 add하고 이를 CENTER에 위치시킴
      getContentPane().add(playPanel, BorderLayout.CENTER);
      playPanel.requestFocus();

      // 화면 지우기
      revalidate(); // 지연 자동 레이아웃
      repaint(); //너비/높이에 영향을주는 속성을 변경할 때 호출되고 모양에 영향을주는 속성을 변경할 때 repaint()를 호출
   }

   // 버튼 클릭 시 화면을 전환하도록 만듦
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
class StartPanel extends JPanel { // 시작 화면
   ImageIcon icon = new ImageIcon("title.png"); // 배경을 이미지로 설정
   // 시작 버튼 생성
   StartButton easy = new StartButton("EASY"); 
   StartButton normal = new StartButton("NORMAL");
   StartButton hard = new StartButton("HARD");

   public StartPanel() {
      add(easy);
      add(normal);
      add(hard);
   }
   
   // 화면을 그림
   public void paintComponent(Graphics g) { 
      g.drawImage(icon.getImage(), 0, 0, null);
      setOpaque(false);
      super.paintComponent(g);
      setLayout(null);
      
      // 버튼 위치 설정
      easy.setBounds(130, 315, 100, 35);
      normal.setBounds(115, 355, 130, 35);
      hard.setBounds(130, 395, 100, 35);
   }
   
   // 시작 버튼
   class StartButton extends JButton {
      ImageIcon icon;

      public StartButton(String level) {
         setText(level);
         if (level.equals("EASY")) {
            icon = new ImageIcon("EasyButton.png"); // 이미지 버튼
         } else if (level.equals("NORMAL")) {
            icon = new ImageIcon("NormalButton.png");
         } else if (level.equals("HARD")) {
            icon = new ImageIcon("HardButton.png");
         }
         setFont(new Font("맑은 고딕", Font.BOLD, 0));
         setContentAreaFilled(false); // 버튼 내부 투명하게 만들기
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

//게임 화면 프레임  - 보조 프레임
@SuppressWarnings("serial")
class PlayPanel extends JPanel {
   static int NUM_OF_BLOCKS_HOR = 12; // 가로 12 블록
   static int NUM_OF_BLOCKS_VER = 20; // 세로 20 블록

   //딜레이를 넣어서 블록 내려오는 사이에 여유 시간 제공
   static int DELAY = 400; //블록 내려오는 지연 시간
   final static int SUB_DELAY = 200; //바닿에 닿은 후 움직일 수 있게 하는 지연 시간

   static int[][][] SHAPE = { // 3차원 배열로 블록 생김새 지정
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
   JPanel scorePanel; // 점수와 게임 종료 문구를 나타내는 패널
   JLabel scoreLabel, gameover, gameclear; // 패널에 넣을 점수 레이블과 게임 오버 레이블 생성

   // 가로8, 세로8
   public PlayPanel(String level) {
      setLevel(level);
      setVisible(true);
      setLayout(new BorderLayout()); // 중앙 배열을 위해 BorderLayout 사용
      // 점수를 알려주는 패널
      scorePanel = new JPanel();
      scorePanel.setLayout(new BorderLayout());
      scorePanel.setOpaque(false); // 투명 프레임

      // 패널에 넣을 점수 Lable 생성
      scoreLabel = new JLabel();
      scoreLabel.setForeground(Color.LIGHT_GRAY);
      scoreLabel.setFont(new Font(null, Font.BOLD, 20));
      scoreLabel.setHorizontalAlignment(JLabel.CENTER); // Label 중앙 배치
      scorePanel.add(scoreLabel, BorderLayout.CENTER); // scorePanel에 scoreLabel을 넣음

      add(scorePanel, BorderLayout.NORTH); // scorePanel을 가장 위쪽에 배치
      revalidate();

      gameover = new JLabel(">>GAME OVER<<"); // 게임 오버 Label 생성
      gameover.setFont(new Font(null, Font.BOLD, 30));
      gameover.setForeground(Color.RED.darker());
      gameover.setHorizontalAlignment(JLabel.CENTER);

      gameclear = new JLabel(">>GAME CLEAR<<"); // 게임 클리어 Label 생성
      gameclear.setFont(new Font(null, Font.BOLD, 15));
      gameclear.setForeground(Color.BLUE.brighter());
      gameclear.setHorizontalAlignment(JLabel.CENTER);

      blocks = new ArrayList<Block>();
      genShape(); // 새로운 블록 생성
      gameThread = new GameThread();

      gameThread.start();
      requestFocus();
      setFocusable(true);
      addKeyListener(new KeyListener()); // 키를 받도록 함
      setBackground(new Color(0, 0, 0)); // 배경 화면을 검은색으로 지정
   }

   public void setLevel(String text) { // 레벨 설정
      if (text.equals("EASY"))
         DELAY = 600;
      else if (text.equals("HARD"))
         DELAY = 150;
      else
         return;
   }

   class GameThread extends Thread { // 블록이 내려오는 스레드
      public void run() { // 게임 실행
         while (isAlive) { // 살아있는 동안, 경계선에 블록이 닿지 않았을 동안
            while (!shape.isFallen()) { // 블럭이 떨어지지 않은 동안
               try {
                  Thread.sleep(DELAY); // 상태제어, 지정된 밀리초동안 현재 스레드를 지연
               } catch (InterruptedException e) {
                  e.printStackTrace();
               }

               if (!shape.isFallen()) { // 만약 블록이 떨어지지 않았다면, Move를 이용해 한 칸 내려감
                  shape.Move(0, 1);
                  repaint(); // 내려간 후, repaint로 변경된 블록의 위치를 그림
               }
               
               //기회를 한 번 더 주기 위해 확인함
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
            	// 블럭이 내려온 후 스레드 정지 상태를 줌.. 이 try-catch문을 써줘야 게임이 완전히 종료되고, 게임 오버 레이블이 뜸
               sleep(10);
            } catch (InterruptedException e) {
               // TODO Auto-generated catch block
               e.printStackTrace();
            }

            for (Block block : shape.nowBlocks) { // 현재 있는 블록들을 하나씩 받아와서 넣음
               blocks.add(block); // 받은 블록을 blocks에 넣음
            }
            
            if (isGameOver()) { // 게임 종료 시, 현재 상태를 false로 바꾸고, 게임 오버 레이블을 넣음
               isAlive = false;
               add(gameover, BorderLayout.CENTER);
               break;
            }

            if (isLineEmpty(19)) {
               isAlive = false;
               add(gameclear, BorderLayout.CENTER);
               break;
            }

            // 착지한 블록이 속한 가로줄들을 저장하여 일일이 확인하도록 함
            int[] linesToRemove = shape.getEffectiveYs();

            // 한 블럭이 착지했을 때 ->다 찼는 지 체크
            for (int line : linesToRemove) { // 블록이 착지했을 때, 블록이 떨어진 줄이 다 채워져 있는 지를 확인
               if (isLineFull(line)) { // 만약 그 줄이 포화 상태라면,
                  removeLine(line); // removeLine으로 해당 줄을 삭제
               }
            }

            genShape(); // 다음 블록 모양을 만들어서 이어서 게임 진행
            repaint(); // 방금 만든 블록의 위치를 그림
         }

         // 게임 오버 됐을 때 모두 지움
         repaint();
         revalidate();

      }

   }

   class KeyListener extends KeyAdapter { // 키를 입력받음
      public void keyPressed(KeyEvent e) {

         int key = e.getKeyCode();

         switch (key) { // switch문으로 입력받은 키를 확인하고 해당 기능 수행
         case KeyEvent.VK_UP: { // 블록 회전
            shape.tryRotate();
         }
            break;
         case KeyEvent.VK_DOWN: { // 블록을 한 칸 아래로
            if (shape.canMove(0, 1)) {
               shape.Move(0, 1);
               shapeShadow.setLocation(shape.nowBlocks); // 그림자 블록의 위치를 현재 블록의 위치를 이용해 설정
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

         repaint(); //변경된 위치를 repaint
      }
   }

   class Block { // 게임에서 블록의 클래스
      final static int SIZE = 30; // 한 블록의 크기
      int x, y;
      int type;

      public Block(int type, int x, int y) { // 매개변수 3개인 생성자
         this.type = type;
         this.x = x;
         this.y = y;
      }

      public Block(int type) { // 매개변수 1개인 생성자 
         this.type = type;
      }

      public Block(int x, int y) { // 매개변수 2개인 생성자
         this.x = x;
         this.y = y;
      }

      // 블록이 존재하지 않으면 블록을 이동
      public void tryMove(int deltax, int deltay) {

    	  // 바닥이 아니고 스페이스바를 누르지 않았다면, y+deltay로 이동
         if (deltay != 0 && !isFallen()) {
            y += deltay;
         }

         // 이동하려는 위치에 장애물이 있는지 확인
         else {
            if (!isBlockAt(x + deltax, y + deltay)) {

               x += deltax;
               y += deltay;
            }
         }
      }

      public boolean isFallen() { // 블록이 떨어진 경우
         if (isBlockAt(x, y + 1) || y == NUM_OF_BLOCKS_VER) { // 쌓여있는 블록과 진짜 바닥 모두 바닥으로 생각
            return true;
         } else
            return false;
      }

      public void draw(Graphics g) { // 블록을 만듦
         for (Block block : blocks) {  // 뒤에서 차례로 객체를 꺼내서 앞에다가 넣는다. 뒤에서 꺼낼 객체가 없을 때까지
            setTypeColor(g, block.getType());
            g.fillRect((block.getX() * Block.SIZE) - Block.SIZE, (block.getY() * Block.SIZE) - Block.SIZE,
                  Block.SIZE, Block.SIZE);

            setTypeColorDarker(g, 7);
            g.drawRect((block.getX() * Block.SIZE) - Block.SIZE, (block.getY() * Block.SIZE) - Block.SIZE,
                  Block.SIZE, Block.SIZE);

         }
      }

      public void setX(int x) { // X를 지정
         this.x = x;
      }

      public void setY(int y) { // Y를 지정
         this.y = y;
      }

      public int getX() { // X 좌표 가져옴
         return x;
      }

      public int getY() { // Y 좌표 가져옴
         return y;
      }

      public int getType() { // 현재 블록의 모양을 가져옴
         return type;
      }
   }

   class Shape { // 블록의 모양
      Block[] nowBlocks; // 현재 있는 블록들
      Random random;
      int type;
      int[][] shape;

      public Shape() {
         random = new Random();
         type = random.nextInt(7); // 7개의 도형 종류
         if (type == 7)
            type = 2; // I의 확률을 2배
         nowBlocks = new Block[4];
         shape = new int[4][2];

         System.arraycopy(SHAPE[type], 0, shape, 0, SHAPE[type].length);
         // arraycopy: 배열 복사하기, SHAPE_COORD[type]를 shapeCoord에 복사, 0은 처음부터 복사하고 처음부터 넣기
         for (int i = 0; i < 4; i++) { // 처음 생성될 때 화면의 중앙에 위치
            nowBlocks[i] = new Block(type);
            nowBlocks[i].setX(shape[i][0] + NUM_OF_BLOCKS_HOR / 2);
            nowBlocks[i].setY(shape[i][1] + 2);
         }
      }
      
      // 도형을 회전시키는 메소드
      public void tryRotate() {
         boolean canRotate = true;
         if (getType() == 6) { // O 모양은 회전해도 그대로이므로 회전 필요 없음
            return;
         }

         int[][] tempPoint = new int[4][2]; // 바뀔 위치를 임의로 저장

         for (int i = 0; i < 4; i++) {
            tempPoint[i][0] = -shape[i][1];// 바뀐 원시 좌표의 X
            tempPoint[i][1] = shape[i][0]; // 바뀐 원시 좌표의 Y

            if (isBlockAt(tempPoint[i][0] + nowBlocks[0].getX(), tempPoint[i][1] + nowBlocks[0].getY())) {
               canRotate = false;
               break;
            }
         }

         if (canRotate) { // 회전이 가능하다면
            for (int i = 0; i < 4; i++) { // tempPoint의 좌표를 shapeCoord에 넣음
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

      public int[] getEffectiveYs() { // 블록이 존재하는 Y번째 줄을 확인하는 용도
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
            // i번째 y값이 중복이 아니면 입력되고,
            // 중복이면 100인채로 패스
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
            setTypeColorDarker(g, block.getType()); // 테두리의 정체
            g.fillRect((block.getX() * Block.SIZE) - Block.SIZE, (block.getY() * Block.SIZE) - Block.SIZE, // getX, getY는 블럭의 우측 하단 좌표
                  Block.SIZE, Block.SIZE); // getY는 블럭의 우측 하단 좌표
            setTypeColor(g, block.getType()); // 채움
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

      public ShapeShadow(Block[] nowBlocks) { // 그림자 생성
         for (int i = 0; i < 4; i++) {
            this.nowBlocks[i] = new Block(nowBlocks[i].getX(), nowBlocks[i].getY());
         }
         drop(); // shape랑 같은 좌표로 생성된 뒤 떨궈버려.
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

      public void setLocation(Block[] nowBlocks) { // 이후에 바뀔 때 사용 ex) 키 이벤트
    	 // System.arraycopy(nowBlocks, 0, this.nowBlocks, 0, nowBlocks.length);
         for (int i = 0; i < 4; i++) {
            this.nowBlocks[i] = new Block(nowBlocks[i].getX(), nowBlocks[i].getY());
         }
         drop(); // 위치를 20으로 지정하는 게 아니라 그냥 떨군 거임
      }

   }

   public void newGame() {
      isAlive = true;
      score = 0;
      blocks = new ArrayList<Block>();
      genShape();
      remove(gameover); // gameover = 게임 오버 글자
      remove(gameclear); // gameclear = 게임 클리어 글자
      gameThread = new GameThread();
      gameThread.start();
   }

   public boolean isGameOver() {
      for (int i = 0; i < 4; i++) {
         if (blocks.get(blocks.size() - 1 - i).getY() <= 4) // 긴 막대의 경우 4이므로
            return true;
      }
      return false;
   }

   public boolean isBlockAt(int x, int y) { // (x,y)에 블락이 있으면 true
      for (Block block : blocks) { // 다른 블럭이랑 부딪히는지 확인
         if (block.x == x && block.y == y) {
            return true;
         }
      }

      if (x == 0 || x == NUM_OF_BLOCKS_HOR + 1) // 좌우 벽이랑 부딪히는지 확인
         return true;
      if (y == NUM_OF_BLOCKS_VER + 1) // 바닥에 닿는지 확인
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

   // y번째 줄의 블럭을 없앤다.
   public void removeLine(int y) {
      int count = 0;
      int index = 0;
      while (count < NUM_OF_BLOCKS_HOR) {
         if (blocks.get(index).getY() == y) {
            blocks.remove(index); // ArrayList는 remove 할 때 삭제해서 빈 자리를 자동으로 채운다. 그래서 index 안 올려줌
            count++;
         }

         else
            index++;
      }

      for (int i = 0; i < blocks.size(); i++) { // 한줄 지우고 지운 줄보다 위에 있는 건 내려줌
         if (blocks.get(i).getY() < y) {
            blocks.get(i).setY(blocks.get(i).getY() + 1);
         }
      }

      score += 100; // 한 줄 지우면 +100점!
      scoreLabel.setText("현재 점수 : " + score);
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
      case 8: // ShapeShadow 색
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
      case 8: // ShapeShadow Border 색
         g.setColor(Color.DARK_GRAY.darker());
         break;
      }
   }

   // 테두리 만들기
   public void drawBorder(Graphics g) {
      setTypeColor(g, 7);
      g.drawRect(0, 0, Block.SIZE * NUM_OF_BLOCKS_HOR, Block.SIZE * NUM_OF_BLOCKS_VER);
      g.drawLine(0, 4 * Block.SIZE, Block.SIZE * NUM_OF_BLOCKS_HOR, 4 * Block.SIZE);
   }

   // 블럭 생성
   public void genShape() {
      shape = new Shape();
      shapeShadow = new ShapeShadow(shape.nowBlocks);

      score += 40;
      scoreLabel.setText("현재 점수 : " + score);
   }

   // 색 바꾸는 것
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