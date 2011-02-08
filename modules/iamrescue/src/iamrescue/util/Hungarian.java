/**
 * This work is non-copyrightable
 * @author Myriam Abramson
 * myriam.abramson@nrl.navy.mil
 */

package iamrescue.util;

import java.text.DecimalFormat;
import java.util.Random;

/**
 * Adapted from the website http://216.249.163.93/bob.pilgrim/445/munkres.html
 * roles x agents find best role allocation the coefficient of the matrix are
 * the role preferences of the agents
 */
public class Hungarian {

    private static final int FORBIDDEN_VALUE = 9999;

    private double[][] matrix;

    private int[] rCov;

    private int[] cCov;

    private int[][] stars;

    private int rows;

    private int cols;

    private int dim;

    private int solutions;

    private Random rand = new Random();

    // columns = agents
    // rows = roles

    /**
     * @param rows
     * @param columns
     */
    public Hungarian(int rows, int columns) {
        this.rows = rows;
        this.cols = columns;
        dim = Math.max(rows, columns);
        // solutions = Math.min(rows,columns);
        solutions = dim;
        matrix = new double[dim][dim];
        stars = new int[dim][dim];
        rCov = new int[dim];
        cCov = new int[dim];
        init(rows, columns);
    }

    /**
     * converts x,y to one dimension
     */
    /**
     * @param x
     * @param y
     * @return
     */
    public int two2one(int x, int y) {
        return (x * dim) + y;
    }

    /**
     * @param n
     * @return
     */
    /**
     * @param n
     * @return
     */
    public int one2col(int n) {
        return (n % dim);
    }

    /**
     * @param n
     * @return
     */
    public int one2row(int n) {
        return (n / dim);
    }

    // step 0 transform the matrix from maximization to minimization
    /**
     * 
     */
    public void max2min() {

        double maxVal = Double.MIN_VALUE;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (matrix[i][j] > maxVal) {
                    maxVal = matrix[i][j];
                }
            }
        }
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                matrix[i][j] = maxVal - matrix[i][j];
            }
        }

        // System.out.println ("after max2min");
        // printIt();
    }

    // step1 find the minimum in each row and subtract it

    /**
     * 
     */
    public void rowMin() {

        for (int i = 0; i < dim; i++) {
            double minVal = matrix[i][0];
            for (int j = 1; j < dim; j++) {
                if (minVal > matrix[i][j]) {
                    minVal = matrix[i][j];
                }
            }
            for (int j = 0; j < dim; j++) {
                matrix[i][j] -= minVal;
            }
        }
        // printIt();
        // printStars();
    }

    /**
     * 
     */
    public void colMin() {

        for (int j = 0; j < dim; j++) {
            double minVal = matrix[0][j];
            for (int i = 1; i < dim; i++) {
                if (minVal > matrix[i][j]) {
                    minVal = matrix[i][j];
                }
            }
            for (int i = 0; i < dim; i++) {
                matrix[i][j] -= minVal;
            }
        }
        // printIt();
        // printStars();
    }

    /**
     * 
     */
    public void printStars() {

        for (int i = 0; i < dim; i++) {
            for (int j = 0; j < dim; j++) {
                System.out.print(stars[i][j] + " ");
            }
            System.out.println(rCov[i]);
        }
        for (int j = 0; j < dim; j++) {
            System.out.print(cCov[j] + " ");
        }
        System.out.println();
    }

    // step2 star the zeros

    /**
     */
    public void starZeros() {

        for (int i = 0; i < dim; i++) {
            for (int j = 0; j < dim; j++) {
                if (matrix[i][j] == 0 && cCov[j] == 0 && rCov[i] == 0) {
                    stars[i][j] = 1;
                    cCov[j] = 1;
                    rCov[i] = 1;
                }
            }
        }
        clearCovers();
        // printIt();
        // printStars();
    }

    /**
     * step 3 -- check for solutions
     */
    /**
     * @return
     */
    public int coveredColumns() {

        int k = 0;
        for (int i = 0; i < dim; i++) {
            for (int j = 0; j < dim; j++) {
                if (stars[i][j] == 1) {
                    cCov[j] = 1;
                }
            }
        }
        for (int j = 0; j < dim; j++) {
            k += cCov[j];
        }
        // printIt();
        // printStars();
        return k;
    }

    /**
     * returns -1 if no uncovered zero is found a zero whose row or column is
     * not covered
     */
    /**
     * @return
     */
    public int findUncoveredZero() {
        for (int i = 0; i < dim; i++) {
            for (int j = 0; j < dim; j++) {
                if (matrix[i][j] == 0 && rCov[i] == 0 && cCov[j] == 0) {

                    return two2one(i, j);
                }
            }
        }

        return -1;
    }

    /**
     * returns -1 if not found returns the column if found
     */
    /**
     * @param zeroY
     * @return
     */
    public int foundStarInRow(int zeroY) {
        for (int j = 0; j < dim; j++) {
            if (stars[zeroY][j] == 1) {
                return j;
            }
        }
        return -1;
    }

    /**
     * returns -1 if not found returns the row if found
     */

    /**
     * @param zeroX
     * @return
     */
    public int foundStarInCol(int zeroX) {
        for (int i = 0; i < dim; i++) {
            if (stars[i][zeroX] == 1) {
                return i;
            }
        }
        return -1;
    }

    /**
     * step 4 Cover all the uncovered zeros one by one until no more cover the
     * row and uncover the column
     */

    /**
     * @return
     */
    public boolean coverZeros() {

        int zero = findUncoveredZero();
        while (zero >= 0) {
            int zeroCol = one2col(zero);
            int zeroRow = one2row(zero);
            stars[zeroRow][zeroCol] = 2; // prime it
            int starCol = foundStarInRow(zeroRow);

            if (starCol >= 0) {
                rCov[zeroRow] = 1;
                cCov[starCol] = 0;
            }
            else {
                // printStars();
                starZeroInRow(zero); // step 5
                return false;
            }
            zero = findUncoveredZero();
        }
        // printIt();
        // printStars();
        return true;
    }

    /**
     * @param col
     * @return
     */
    public int findStarInCol(int col) {
        if (col < 0) {
            System.err.println("Invalid column index " + col);
        }
        for (int i = 0; i < dim; i++) {
            if (stars[i][col] == 1) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 
     */
    public void clearCovers() {
        for (int i = 0; i < dim; i++) {
            rCov[i] = 0;
            cCov[i] = 0;
        }
    }

    /**
     * unstar stars star primes
     */

    /**
     */
    public void erasePrimes() {
        for (int i = 0; i < dim; i++) {
            for (int j = 0; j < dim; j++) {
                if (stars[i][j] == 2) {
                    // stars[i][j] = 1;
                    stars[i][j] = 0;
                }
            }
        }
    }

    /**
     * @param path
     * @param kount
     */
    public void convertPath(int[][] path, int kount) {
        // printStars();
        for (int i = 0; i <= kount; i++) {
            int x = path[i][0];
            int y = path[i][1];
            if (stars[x][y] == 1) {
                stars[x][y] = 0;
            }
            else if (stars[x][y] == 2) {
                stars[x][y] = 1;
            }
        }
        // printStars();
    }

    /**
     * returns the column where a prime was found for a given row
     */

    /**
     * @param row
     * @return
     */
    public int findPrimeInRow(int row) {
        for (int j = 0; j < dim; j++) {
            if (stars[row][j] == 2) {
                return j;
            }
        }
        System.err.println("No prime in row " + row + " found");
        forcePrint();
        return -1;
    }

    /**
     * step 5 augmenting path algorithm go back to step 3
     */
    /**
     * @param zero
     */
    public void starZeroInRow(int zero) {
        boolean done = false;
        int zeroRow = one2row(zero); // row
        int zeroCol = one2col(zero); // column

        int kount = 0;
        int[][] path = new int[100][2]; // how to dimension that?
        path[kount][0] = zeroRow;
        path[kount][1] = zeroCol;
        while (!done) {
            int r = findStarInCol(path[kount][1]);
            if (r >= 0) {
                kount++;
                path[kount][0] = r;
                path[kount][1] = path[kount - 1][1];
            }
            else {
                done = true;
                break;
            }
            int c = findPrimeInRow(path[kount][0]);

            kount++;
            path[kount][0] = path[kount - 1][0];
            path[kount][1] = c;
        }
        convertPath(path, kount);
        clearCovers();
        erasePrimes();
        // printIt();
        // printStars();
        // go to step 3
    }

    /**
     * 
     */
    public void solve() {
        // System.out.println ("in solve");
        // forcePrint();
        // printIt();
        max2min();
        rowMin(); // step 1
        colMin();
        starZeros(); // step 2
        boolean done = false;
        while (!done) {
            int covCols = coveredColumns(); // step 3
            // if (covCols == dim) {
            if (covCols >= solutions) {
                // printStarZeros();
                break;
            }

            done = coverZeros(); // step 4 (calls step 5)
            while (done) {
                double smallest = findSmallestUncoveredVal();
                uncoverSmallest(smallest); // step 6
                done = coverZeros();
            }
            // System.out.println ("Continue(y/n)?");
            // System.out.flush();
            // char response = human.readChar();
            // if (response == 'n')
            // break;

        }
    }

    /**
     * @param row
     * @param col
     * @return
     */
    boolean freeRow(int row, int col) {
        for (int i = 0; i < dim; i++) {
            if (i != row && stars[i][col] == 1) {
                return false;
            }
        }
        return true;
    }

    /**
     * @param row
     * @param col
     * @return
     */
    boolean freeCol(int row, int col) {
        for (int j = 0; j < dim; j++) {
            if (j != col && stars[row][j] == 1) {
                return false;
            }
        }
        return true;
    }

    // read from left to right:
    // Role i is assigned to agent j
    /**
     * 
     */
    public void printStarZeros() {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                // check for independence
                if (stars[i][j] == 1 && (freeRow(i, j) || freeCol(i, j))) {
                    System.out.println(i + " assigned to " + j
                            + " is a solution");
                }
            }
        }
    }

    // get the assignments for the agents
    // the matrix is roles x agents
    /**
     * @return
     */
    public int[] getSolutions() {
        int[] solutions = new int[cols];
        for (int j = 0; j < cols; j++) {
            solutions[j] = -1;
            for (int i = 0; i < rows; i++) {
                // test for independence
                // should not be necessary
                if (stars[i][j] == 1 && (freeRow(i, j) || freeCol(i, j))) {
                    solutions[j] = i;
                }
            }
        }
        return solutions;
    }

    /**
     * @return
     */
    public double findSmallestUncoveredVal() {
        double minVal = Double.MAX_VALUE;
        for (int i = 0; i < dim; i++) {
            for (int j = 0; j < dim; j++) {
                if (rCov[i] == 0 && cCov[j] == 0) {
                    if (minVal > matrix[i][j]) {
                        minVal = matrix[i][j];
                    }
                }
            }
        }
        return minVal;
    }

    /**
     * step 6 modify the matrix if the row is covered, add the smallest value if
     * the column is not covered, subtract the smallest value
     */
    /**
     * @param smallest
     */
    public void uncoverSmallest(double smallest) {
        for (int i = 0; i < dim; i++) {
            for (int j = 0; j < dim; j++) {
                if (rCov[i] == 1) {
                    matrix[i][j] += smallest;
                }
                if (cCov[j] == 0) {
                    matrix[i][j] -= smallest;
                }
            }
            // printIt();
            // printStars();
        }
    }

    /**
     * @param rows
     * @param cols
     */
    public void init(int rows, int cols) {
        for (int i = 0; i < dim; i++) {
            for (int j = 0; j < dim; j++) {
                if (i < rows && j < cols) {
                    matrix[i][j] = rand.nextDouble();
                }
                else {
                    matrix[i][j] = FORBIDDEN_VALUE;
                }
            }
            // matrix[0][0] = 0.0;
            // matrix[0][1] = 0.5;
            // matrix[0][2] = 0.0;
            // matrix[0][3] = 0.0;
            // // matrix[0][4] = 0.17;
            // // matrix[0][5] = 0.25;
            // // matrix[0][6] = 0.12;
            // // matrix[0][7] = 0.25;
            // matrix[1][0] = 0.5;
            // matrix[1][1] = 0.0;
            // matrix[1][2] = 0.0;
            // matrix[1][3] = 0.0;
            // // matrix[1][4] = 0.17;
            // // matrix[1][5] = 0.25;
            // // matrix[1][6] = 0.10;
            // // matrix[1][7] = 0.25;
            // matrix[2][0] = 0.0;
            // matrix[2][1] = 0.0;
            // matrix[2][2] = 0.2;
            // matrix[2][3] = 0.3;
            // // matrix[2][4] = 0.12;
            // // matrix[2][5] = 0.17;
            // // matrix[2][6] = 0.10;
            // // matrix[2][7] = 0.25;
        }

    }

    /**
     * 
     */
    public void forcePrint() {
        DecimalFormat form = new DecimalFormat("0.00");
        for (int i = 0; i < dim; i++) {
            for (int j = 0; j < dim; j++) {
                System.out.print(form.format(matrix[i][j]) + " ");
            }
            System.out.println();
        }
    }

    /**
     * 
     */
    public void printIt() {
        DecimalFormat form = new DecimalFormat("0.00");
        for (int i = 0; i < dim; i++) {
            for (int j = 0; j < dim; j++) {
                System.out.print(form.format(matrix[i][j]) + " ");
            }
            System.out.println();
        }
    }

    /**
     * @param index
     * @param preferences
     */
    public void addColumn(int index, double[] preferences) {
        for (int i = 0; i < preferences.length; i++) {
            matrix[i][index] = preferences[i];
        }
    }

    /**
     * @param index
     * @param preferences
     */
    public void addRow(int index, double[] preferences) {
        for (int i = 0; i < preferences.length; i++) {
            matrix[index][i] = preferences[i];
        }
    }
}
