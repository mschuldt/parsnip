/* MachinePlayer.java */
// javac -g -cp ../ ../player/*.java ../list/*.java

package cs61bayn.player;

import cs61bayn.dict.HashTable;
import cs61bayn.dict.Entry;
import cs61bayn.dict.HistoryTable;
import player.Player;
import player.Move;


/**
 *  An implementation of an automatic Network player.  Keeps track of moves
 *  made by both players.  Can select a move for itself.
 */
public class MachinePlayer extends Player {
    Board board;
    int searchDepth;
    int ourColor, opponentColor;
    int generation;
    public static final int white = 1;
    public static final int black = 0;
    private static final int VAR_DEPTH = -1;
    //minimum depths to search to
    // private static final int ADD_DEPTH = 4;
    // private static final int STEP_DEPTH = 3;

    // must decrease for java '-prof' profiler option
    private static final int ADD_DEPTH = 2;
    private static final int STEP_DEPTH = 1;

    private static final int MAX_DEPTH = 20; //should be impossibly large

    private static final int OUT_OF_TIME = 99999999;//should be > then any score
    MoveList[] movesLists;

    //this is set to true while iteratively deepening
    boolean iterativeDeepining = false;
    double startTime; //time the minimax search started

    HashTable ht = new HashTable(1000000);
    long ourBoard, oppBoard, hashCode;
    long[] entry;
    //indexes for entry objects
    private static final int ENTRY_SCORE = 0;
    private static final int ENTRY_OURBITBOARD = 1;
    private static final int ENTRY_OPPBITBOARD = 2;
    private static final int ENTRY_GENERATION = 3;
    private static final int ENTRY_EVALED_BOARDS = 4;
    private static final int ENTRY_DEPTH = 5;

    HistoryTable moveHistory = new HistoryTable();
    long[] moveScores = new long[488];

    // Creates a machine player with the given color.  Color is either 0 (black)
    // or 1 (white).  (White has the first move.)
    public MachinePlayer(int color) {
        this(color, VAR_DEPTH);
    }

    // Creates a machine player with the given color and search depth.  Color is
    // either 0 (black) or 1 (white).  (White has the first move.)
    public MachinePlayer(int color, int searchDepth) {
        if (color != white && color != black){
            System.out.println("ERROR: invalid color: " + color);
        }
        ourColor = color;
        opponentColor = 1 - color;
        board = new Board(color);
        this.searchDepth = searchDepth;

        movesLists = new MoveList[MAX_DEPTH];
        for (int i = 0; i < MAX_DEPTH; i++){
            movesLists[i] = new MoveList();
        }
        generation = 0;
    }

    public static String timeSince(double time){
        return "" + ((System.currentTimeMillis() - time)/1000.0);
    }

    private Move copyMove(Move move){
        if (move == null){
            return null;
        }
        Move ret = new Move();
        ret.moveKind = move.moveKind;
        ret.x1 = move.x1;
        ret.y1 = move.y1;
        if (move.moveKind == Move.STEP){
            ret.x2 = move.x2;
            ret.y2 = move.y2;
        }
        return ret;
    }

    // Returns a new move by "this" player.  Internally records the move (updates
    // the internal game board) as a move by "this" player.
    public Move chooseMove() {
        generation++;

        ht.collisions =  0;

        int depth = searchDepth;
        if (depth == VAR_DEPTH){
            return chooseMoveIterativelyDeepening();
        }
        iterativeDeepining = false;
        Best bestMove = minimax(ourColor, -100000, 100000, depth);

        if (bestMove.move == null){ //this happens sometimes...why?
            MoveList validmoves = new MoveList();
            board.validMoves(ourColor, validmoves);
            if (validmoves.length() == 0){
                //System.out.println("no more moves");
                Move ret = new Move(0,0);
                ret.moveKind = Move.QUIT;
                //System.out.println("move time: " + timeSince(chooseMoveStart));
                return ret;
            }
            bestMove.move = validmoves.get(0);
            System.out.println("fixed null move");
        }

        //make the move here instead of calling this.forceMove if we know that the move is valid
        board.move(bestMove.move, ourColor);

        //decrease relevance of past moves
        moveHistory.decay();
        return bestMove.move;
    }

    public Move chooseMoveIterativelyDeepening(){
        iterativeDeepining = true;
        double chooseMoveStart = System.currentTimeMillis();
        int depth = searchDepth;
        if (board.getNumPieces(ourColor) < 10-STEP_DEPTH){
            depth = ADD_DEPTH;
        }else{
            depth = STEP_DEPTH;
        }
        double endTime = 0.0;
        Best bestMove = null;
        Best tmpBest = null;
        startTime = System.currentTimeMillis();
        while (true){
            tmpBest = minimax(ourColor, -100000, 100000, depth);
            tmpBest.move = copyMove(tmpBest.move);
            if ((tmpBest.score == OUT_OF_TIME) && (bestMove == null)){
                System.out.println("ran out of time. never found a move");
            }
            if (tmpBest.score != OUT_OF_TIME){
                bestMove = tmpBest;
            }else{
                // System.out.println("ran out of time");
                depth--;
                break;
            }
            if ((System.currentTimeMillis() - startTime)/1000.0 > 4.9){
                break;
            }
            depth++;
            if (depth >= MAX_DEPTH){
                System.out.println("error: exceeded max depth in choose move(" +depth+")");
            }
        }
        System.out.println("searched to depth " + depth);


        if (bestMove == null){
            System.out.println("bestMove == null. exiting");
            return null;
        }
        else if (bestMove.move == null){ //this happens sometimes...why?
            MoveList validmoves = new MoveList();
            board.validMoves(ourColor, validmoves);
            if (validmoves.length() == 0){
                //System.out.println("no more moves");
                Move ret = new Move(0,0);
                ret.moveKind = Move.QUIT;
                //System.out.println("move time: " + timeSince(chooseMoveStart));
                return ret;
            }
            bestMove.move = validmoves.get(0);
            System.out.println("fixed null move");
        }

        //make the move here instead of calling this.forceMove if we know that the move is valid
        board.move(bestMove.move, ourColor);

        //decrease relevance of past moves
        moveHistory.decay();
        System.out.println("move time: " + timeSince(chooseMoveStart));
        return bestMove.move;
    }


    /**
     * Minimax algorithm with alpha-beta pruning which returns a Best object with a score and Move
     **/

    public Best minimax(int side, int alpha, int beta, int depth){
        Best myBest = new Best();
        Best reply;
        int evaledBoards = 0;

        if (iterativeDeepining
            && (System.currentTimeMillis() - startTime)/1000.0 > 4.9){
            myBest.score = OUT_OF_TIME;
            return myBest;
        }
        if (board.hasNetwork(opponentColor)){
            myBest.score = -10000 - 100*depth;
            return myBest;
        }

        if (board.hasNetwork(ourColor)){
            myBest.score = 10000+100*depth; //temp values for testing
            return myBest;
        }

        if (depth == 0){
            myBest.score = board.score();
            return myBest;
        }

        if (side == ourColor){
            myBest.score = alpha;
        }else{
            myBest.score = beta;
        }

        if (depth >= MAX_DEPTH){ //TEMPORARY
            System.out.println("error: exceeded max depth  in minimax (" +depth+")");
        }
        MoveList allValidMoves = movesLists[depth];
        board.validMoves(side, allValidMoves);

        //sort valid moves according to score (history heuristic)
        int len = allValidMoves.length();

        assert len <= 488;
        for (int i = 0; i < len;  i++){
            moveScores[i] = moveHistory.find(allValidMoves.get(i));
        }
        allValidMoves.sorted(moveScores);
        assert len == allValidMoves.length();

        myBest.move = copyMove(allValidMoves.get(0));

        int score=0;

        Move m;
        len = allValidMoves.length();
        //for (Move m : allValidMoves){
        for (int i = (len-1); i >= 0; i--){
            m = allValidMoves.get(i);

            board.move(m, side);

            ///***  memoization code
            hashCode = board.hash();
            ourBoard = board.getOurBitBoard();
            oppBoard = board.getOpponentBitBoard();
            entry = ht.find(hashCode, ourBoard, oppBoard, generation);
            if (entry != null
                && entry[ENTRY_DEPTH] >= depth){
                score = (int)entry[ENTRY_SCORE];
                evaledBoards += (int) entry[ENTRY_EVALED_BOARDS]; //?
            }else{
                reply = minimax(1 - side, alpha, beta, depth - 1);
                score = reply.score;
                evaledBoards += reply.evaledBoards;
                ht.insert(hashCode, score, ourBoard, oppBoard,
                          reply.evaledBoards, depth-1, generation);
            }

            //*** normal code
            // reply = minimax(1 - side, alpha, beta, depth - 1);
            // score = reply.score;
            // evaledBoards += reply.evaledBoards;

            board.unMove(m);
            if (score == OUT_OF_TIME){
                myBest.score = OUT_OF_TIME;
                myBest.evaledBoards = evaledBoards;
                moveHistory.increment(myBest.move, 2^depth);
                return myBest;
            }
            if ((side == ourColor) && (score > myBest.score)){
                myBest.move = copyMove(m);
                myBest.score = score;
                alpha = score;
            } else if ((side == opponentColor) && (score < myBest.score)){
                myBest.move = copyMove(m);
                myBest.score = score;
                beta = score;
            }
            if (alpha >= beta){
                myBest.evaledBoards = evaledBoards;
                moveHistory.increment(myBest.move, 2^depth);
                return myBest;
            }
        }
        myBest.evaledBoards = evaledBoards;
        moveHistory.increment(myBest.move, 2^depth);
        return myBest;
    }




    // If the Move m is legal, records the move as a move by the opponent
    // (updates the internal game board) and returns true.  If the move is
    // illegal, returns false without modifying the internal state of "this"
    // player.  This method allows your opponents to inform you of their moves.
    public boolean opponentMove(Move m){
        if (board.isValidMove(m, opponentColor)){
            board.opponentMove(m);
            return true;
        }
        return false;
    }

    // If the Move m is legal, records the move as a move by "this" player
    // (updates the internal game board) and returns true.  If the move is
    // illegal, returns false without modifying the internal state of "this"
    // player.  This method is used to help set up "Network problems" for your
    // player to solve.
    public boolean forceMove(Move m){
        if (board.isValidMove(m, ourColor)){
            board.move(m);
            return true;
        }
        return false;
    }

    public static void runGame(){

        MachinePlayer p1 = new MachinePlayer(white);
        MachinePlayer p2 = new MachinePlayer(black, 4);
        Move m1, m2;
        int winner=0;
        while (true){
            m1 =  p1.chooseMove();
            System.out.println("player 1 moved: " + m1);
            p2.opponentMove(m1);

            if (p1.board.hasNetwork(white)){
                System.out.println("player 1(white) wins");
                break;
            }

            m2 =  p2.chooseMove();
            System.out.println("player 2 moved: " + m2);
            p1.opponentMove(m2);

            if (p1.board.hasNetwork(black)){
                System.out.println("player 2(black) wins");
                break;
            }

            if (!p1.board.verify()){
                System.out.println("player 1 has a corrupted board");
            }
            if (!p2.board.verify()){
                System.out.println("player 2 has a corrupted board");
            }
        }
    }
    /** use ForceMove to setup the board described by BOARDSTRING
     */
    public void forceBoard(String boardString){
        int whiteCount=0, blackCount=0;
        Move m;
        boardString = boardString.toLowerCase();
        if (boardString.length() != 64){
            System.out.println("Error --Board.Board(int, String)-- invalid board string:\n"+boardString);
        }
        char[] chars = boardString.toCharArray();
        for (int x = 0; x < 8; x++){
            for (int y = 0; y < 8; y++){
                switch (boardString.charAt(y*8 + x)){
                case ' ' :
                    continue;
                case 'x': case 'b':
                    m = new Move(x, y);
                    if (ourColor == black){
                        forceMove(m);
                    }else{
                        opponentMove(m);
                    }
                    blackCount++;
                    continue;
                case 'o': case 'w':
                    m = new Move(x,y);
                    if (ourColor == white){
                        forceMove(m);
                    }else{
                        opponentMove(m);
                    }
                    whiteCount++;
                    continue;
                default:
                    System.out.println("Error - Board.Board(int, String)- invalid char");
                }
            }
        }
        if (whiteCount > 10 || blackCount > 10){
            System.out.println("Error - Board.Board(int, String) - constructed illegal board");
        }
    }

    public void interactiveDebug(){
        board.interactiveDebug(this);
    }

    public static void benchMark(){
        int emptyDepth = 6;
        int fullDepth = 4;
        double start=0,end=0;

        MachinePlayer p = new MachinePlayer(white, 0);
        p.forceBoard("        " +
                         "        " +
                         "        " +
                         "        " +
                         "        " +
                         "        " +
                         "        " +
                         "        "
                     );
        System.out.println("choosing move via iterative deepening(empty)");
        p.chooseMoveIterativelyDeepening();

        for (int depth = 2; depth < 7; depth++){
            p = new MachinePlayer(white, depth);
            p.forceBoard("        " +
                         "        " +
                         "        " +
                         "        " +
                         "        " +
                         "        " +
                         "        " +
                         "        "
                         );

            start = System.currentTimeMillis();
            p.chooseMove();
            end = System.currentTimeMillis();

            p.board.verify();

            System.out.println("from empty [depth: " + depth + "]: "+ (end - start)/1000.0 + "s ");
        }


        MachinePlayer p2 = new MachinePlayer(white, 19);
        p2.forceBoard("     x  " +
                      " oxxo   " +
                      "    o o " +
                      "  ox  x " +
                      "  oxoo  " +
                      "     x  " +
                      " x  oxo " +
                      " x      "
                      );
        System.out.println("choosing move via iterative deepening(full)");
        p2.chooseMoveIterativelyDeepening();
        for (int depth = 2; depth < 7; depth++){
            p2 = new MachinePlayer(white, depth);
            p2.forceBoard("     x  " +
                          " oxxo   " +
                          "    o o " +
                          "  ox  x " +
                          "  oxoo  " +
                          "     x  " +
                          " x  oxo " +
                          " x      "
                          );

            start = System.currentTimeMillis();
            p2.chooseMove();
            end = System.currentTimeMillis();

            p2.board.verify();
            System.out.println("from full [depth: " + depth + "]: "+ (end - start)/1000.0 + "s ");
        }

    }

    public static void main(String[] args){

        MachinePlayer p = new MachinePlayer(white);
        // p.forceBoard("        " +
        //              "        " +
        //              "        " +
        //              "o ox  x " +
        //              "  ox    " +
        //              "o    x  " +
        //              "    ox  " +
        //              "        "
        //              );
        p.forceBoard("     x  " +
                     " oxxo   " +
                     "    o o " +
                     "  ox  x " +
                     "   xoo  " +
                     "  o  x  " +
                     " x  oxo " +
                     " x      ");
        //runGame();
        //p.interactiveDebug();
        benchMark();
    }
}
