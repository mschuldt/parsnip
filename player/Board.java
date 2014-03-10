// player/Board.java

package player; //this with Piece.java don't seem like they belong here...

import java.util.List;
import java.util.ArrayList;

public class Board{
    Piece[][] pieceArray;
    int color;
    int opponentColor;
    private Piece edge;

    public Board(int c){
        color = c; //0 for black, 1 for white
        opponentColor = 1-c;
        edge = new Piece(0,0,0);
        pieceArray = new Piece[66][66];

        for (int x = 0; x < 66; x++){
            pieceArray[x][0] = edge;
            pieceArray[x][65] = edge;
        }

        for (int y = 0; y < 66; y++){
            pieceArray[0][y] = edge;
            pieceArray[65][y] = edge;
        }

        pieceArray[1][1] = edge;
        pieceArray[64][1] = edge;
        pieceArray[64][64] = edge;
        pieceArray[1][64] = edge;
    }

    //? public/protected?
    //This assumes that MOVE is valid
    private void move(Move move, int color){
        int toX, toY;
        switch (move.moveKind){
        case Move.ADD :
            toX = move.x1 + 1;
            toY = move.y1 + 1;
            //TODO: asserts to check index validity
            assert pieceArray[toX][toY] == null : "square is already full";
            pieceArray[toX][toY] = new Piece(color, move.x1, move.y1);
            break;
        case Move.STEP :
            int fromX = move.x2 + 1,
                fromY = move.y2 + 1;
            toX = move.x1 + 1;
            toY = move.y1 + 1;

            assert pieceArray[toX][toY] == null : "square is already full";
            assert pieceArray[fromX][fromY] != null : "square is empty";
            pieceArray[toX][toY] = pieceArray[fromX][fromY];
            pieceArray[fromX][fromY] = null;
            break;
        case Move.QUIT :
            //TODO
            break;
        }
    }
    //seporate methods for moveing our pieces and moving their pieces
    //so that we don't have to pass the color of the piece we intend
    //to move every time. (the Move object does not have a color field
    //but the Piece objects do)
    public void move(Move move){
        move(move, color);
    }

    public void opponentMove(Move move){
        move(move, opponentColor);
    }

    void unMove(Move move){
        switch (move.moveKind){
        case Move.ADD :
            int x = move.x1 + 1,
                y = move.y1 + 1;
            //TODO: asserts to check index validity
            assert pieceArray[x][x] != null : "square should not be empty";
            pieceArray[x][y] = null;
            break;
        case Move.STEP :
            int toX = move.x2 + 1,
                toY = move.y2 + 1,
                fromX = move.x1 + 1,
                fromY = move.y1 + 1;
            
            assert pieceArray[toX][toY] == null : "square is already full";
            assert pieceArray[fromX][fromY] != null : "square is empty";
            pieceArray[toX][toY] = pieceArray[fromX][fromY];
            pieceArray[fromX][fromY] = null;
            break;
        case Move.QUIT :
            //TODO
            break;
        }
    }
    
    private int countEdgePieces (Piece[] pieces){
        int c = 0;
        for (Piece p : pieces){
            if (p == edge) {
                c+=1;
            }
        }
        return c;
    }

    private Piece[] removeEdgePieces(Piece[] pieces){
        Piece [] ret = new Piece[countEdgePieces(pieces)];
        int i=0;
        for (Piece p : pieces){
            if (!( p == edge)){
                ret[i++] = p;
            }
        }
        return ret;
    }

    public Piece[] adjacentPieces(int x, int y){
        x++; y++;
        //TODO: use an ArrayList to avoid having to traverse this array twice
        Piece [] pieces = {pieceArray[x][y-1],   //top
                           pieceArray[x+1][y-1],  //top right
                           pieceArray[x+1][y],   //right
                           pieceArray[x+1][y+1],  //bottom right
                           pieceArray[x][y+1],   //bottom
                           pieceArray[x-1][y+1],  //bottom left
                           pieceArray[x-1][y],   //left
                           pieceArray[x-1][y-1]}; //top left

        return removeEdgePieces(pieces);
    }

    public Piece[] adjacentPieces(Piece P){
        return adjacentPieces(P.x, P.y);
    }


    public boolean pieceAt(int x, int y){
        //TODO: bounds checking
        return pieceArray[x+1][y+1] != null;
    }

    public Piece getPiece(int x, int y){
        //TODO: bounds checking
        return pieceArray[x+1][y+1];
    }

    public Piece[] connectedPieces(int x, int y){
        List<Piece> pieces = new ArrayList<Piece>();
        int startX = x + 1;
        int startY = y + 1;
        int currentX = startX, currentY = startY -1;
        Piece current = pieceArray[currentX][currentY];
        int xInc, yInc;
        
        int[][] increments = {{0,-1}, //above
                              {0, 1}, //below
                              {-1,0}, //left
                              {0, 1}, //right
                              {1,-1}, //right upper diagonal
                              {1, 1}, //right lower diagonal
                              {-1,-1}, //left upper diagonal
                              {-1,1}}; //left lower diagonal

        for (int[] inc : increments){
            xInc = inc[0];
            yInc = inc[1];
            currentX = startX + xInc;
            currentY = startY + yInc;
            current = pieceArray[currentX][currentY];

            while (current == null){
                currentX += xInc;
                currentY += yInc;
                current = pieceArray[currentX][currentY];
            }
            if (current != edge){
                pieces.add(current);
            }
        }
        return (Piece[])pieces.toArray();
    }

    public Piece[] connectedPieces(Piece P){
        return connectedPieces(P.x, P.y);
    }
}

