/* MachinePlayer.java */

package player;

/**
 *  An implementation of an automatic Network player.  Keeps track of moves
 *  made by both players.  Can select a move for itself.
 */
public class MachinePlayer extends Player {
    Board board;
    int searchDepth;
    int ourColor, opponentColor;
    public static final int white = 1;
    public static final int black = 0;


    // Creates a machine player with the given color.  Color is either 0 (black)
    // or 1 (white).  (White has the first move.)
    public MachinePlayer(int color) {
        ourColor = color;
        opponentColor = 1 - color;
        board = new Board(color);
        searchDepth = 4;//TODO: determine suitable default
    }

    // Creates a machine player with the given color and search depth.  Color is
    // either 0 (black) or 1 (white).  (White has the first move.)
    public MachinePlayer(int color, int searchDepth) {
        board = new Board(color);
        this.searchDepth = searchDepth;
    }

    // Returns a new move by "this" player.  Internally records the move (updates
    // the internal game board) as a move by "this" player.
    public Move chooseMove() {
        Best bestMove = minimax(ourColor, -100000, 100000, searchDepth); //TODO: alpha, beta values ok?
        //make the move here instead of calling this.forceMove if we know that the move is valid
        board.move(bestMove.move,ourColor); //TODO: does minimax always return a valid move?
        return bestMove.move;
    }


    /**
     * Minimax algorithm with alpha-beta pruning which returns a Best object with a score and Move
     **/

    public Best minimax(int side, int alpha, int beta, int depth){
        Best myBest = new Best();
        Best reply;
        if (this.board.hasNetwork(side)){
            myBest.score = (side == ourColor ? 100000 : -100000);//temp values for testing
            return myBest;
        }
        if (depth == 0){
            myBest.score = this.board.score(side);
            return myBest;
        }
        if (side == ourColor){
            myBest.score = alpha;
        }else{
            myBest.score = beta;
        }
        AList<Move> allValidMoves = this.board.validMoves(side);
        myBest.move = allValidMoves.get(0);

        for (int i=0; i < allValidMoves.length(); i++){ // validMoves returns a list
            Move m = allValidMoves.get(i);
            this.board.move(m);
            reply = minimax(1 - side, alpha, beta, depth - 1);
            this.board.unMove(m);
            if (side == ourColor && (reply.score > myBest.score)){
                myBest.move = m;
                myBest.score = reply.score;
                alpha = reply.score;
            } else if (side == opponentColor && (reply.score < myBest.score)){
                myBest.move = m;
                myBest.score = reply.score;
                beta = reply.score;
            }
            if (alpha >= beta){
                return myBest;
            }
        }
        return myBest;
    }


    // If the Move m is legal, records the move as a move by the opponent
    // (updates the internal game board) and returns true.  If the move is
    // illegal, returns false without modifying the internal state of "this"
    // player.  This method allows your opponents to inform you of their moves.
    public boolean opponentMove(Move m){
        if (false){//TODO: determine if move is valid
            return false;
        }
        board.opponentMove(m);
        return true;
    }

    // If the Move m is legal, records the move as a move by "this" player
    // (updates the internal game board) and returns true.  If the move is
    // illegal, returns false without modifying the internal state of "this"
    // player.  This method is used to help set up "Network problems" for your
    // player to solve.
    public boolean forceMove(Move m){
        if (false){//TODO: determine if move is valid
            return false;
        }
        board.move(m);
        return true;
    }

    public static void main(String[] args){
        MachinePlayer p1 = new MachinePlayer(white);
        MachinePlayer p2 = new MachinePlayer(black);
        Move m1, m2;
        while (true){
            m1 =  p1.chooseMove();
            System.out.println("player 1 moved: " + m1);
            p2.opponentMove(m1);

            m2 =  p2.chooseMove();
            System.out.println("player 2 moved: " + m2);
            p1.opponentMove(m2);

            if (!p1.board.verify()){
                System.out.println("player 1 has a corrupted board");
            }
            if (!p2.board.verify()){
                System.out.println("player 2 has a corrupted board");
            }
            if (p1.board.hasNetwork(white)){
                System.out.println("player 1(white) wins");
                break;
            }
            if (p1.board.hasNetwork(black)){
                System.out.println("player 2(black) wins");
                break;
            }
        }
    }
}
