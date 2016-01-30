import java.util.ArrayList;

/**
 * This is a helper class that contains a series of static methods to assist with the calculating of the color of any given point.
 * The primary formula used in the Mandelbrot set is z(n+1) = z(n)^2 + c, where c is an irrational number that is being tested.
 */
public class Logic {
    /**
     * You have no need to instantiate this Object; simply use the static methods provided.
     */
    public Logic() {}
    
    /**
     * The default shader's default logic; Does a given amount of iterations on the given number.
     * @param c: The ComplexNumber that will be tested (of the form ai + b)
     * @param numIterations: How many iterations will be tested for.
     * @return true if this number does not go to infinity in the given number of iterations, false otherwise.
     */
    public static boolean greyTest(ComplexNumber c, int numIterations) {
        ComplexNumber temp = c;

        for (int i = 0; i < numIterations; i++) {
            if (escape(temp)) { return false; } //If it breaks out, then it's not an element
            temp = temp.square().add(c); //Otherwise, increment
        }

        return true;
    }
    
    /**
     * The default shader's default testing resolution of 100 iterations per pixel.
     * @param c: The ComplexNumber that will be tested (of the form ai + b)
     * @return true if this number doesn't go to infinity within 100 iterations, false otherwise.
     */
    public static boolean greyTest(ComplexNumber c) {
        return greyTest(c, 100);
    }    
    
    /**
     * The basic tester for anything that uses color. Similar to the greyscale logic, but used for anything that requires color.
     * @param c: The ComplexNumber that will be tested (of the form ai + b)
     * @param numIterations: How many iterations will be tested for (usually a multiple of 8 to make color calculations easier)
     * @return How many iterations it takes for the tested number to escape, or -1 if it does not in the given amount.
     */
    static public int colorTest(ComplexNumber c, int numIterations) {
        ComplexNumber temp = c;
      
        for (int i = 0; i < numIterations; i++) {
            if (escape2(temp)) { return i; } //If it breaks out, then it's not an element
            temp = temp.square().add(c); //Otherwise, increment
        }

        return -1;        
    }
    
    /**
     * The color shader's default testing resolution of 64 iterations per pixel (neatly divisible by 2 to allow for color calculations)
     * @param c: The ComplexNumber that will be tested (of the form ai + b)
     * @return the number of iterations it takes until escape, or -1 otherwise if it doesn't within 64 iterations.
     */    
    static public int colorTest(ComplexNumber c) {
        return colorTest(c, 64);
    }
    
    /**
     * A private helper method that tells you whether or not this number has escaped yet. 
     * @param c: The number to be tested.
     * @return whether or not the real/irrational value is out of the bounds [-2,2].
     */
    private static boolean escape(ComplexNumber c) {
        if (c.realValue >= 2 || c.realValue <= -2) { return true; }
        if (c.irrationalValue >= 2 || c.irrationalValue <= -2) { return true; }
        return false;
    }
    
    private static boolean escape2(ComplexNumber c) {
        if (!(c.realValue >= 2) && !(c.realValue <= -2) && !(c.irrationalValue >= 2) && !(c.irrationalValue <= -2)) return false;
        return true;
    }
}