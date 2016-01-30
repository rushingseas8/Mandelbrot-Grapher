import java.text.*;
import java.awt.*;
import java.net.*;
import javax.swing.*;
import java.util.*;
import java.awt.event.*;
import java.awt.image.*;

public class Drawer extends JFrame implements Runnable, KeyListener {
    //Variables to store the current viewing dimensions.
    double minX = -2;
    double minY = -2;
    double maxX = 2;
    double maxY = 2;

    //These store the value of how much space every pixel represents, and changes upon resizing and zooming.
    double xIncrement;
    double yIncrement;

    //Your current center position. Conserved upon zoom, but changes when you pan the camera.
    double xCenter;
    double yCenter;

    //The distance from the center to the min and max, used for recalculating when zooming or panning.
    double xDist;
    double yDist;

    //The level of resolution. Every time you enhance resolution, this goes up by one, and increases the number of iterations
    //performed per pixel based on experimental data that shows what differences look nicest. Number of iterations = 64 * 2^resolutionLevel.
    int resolutionLevel = 4;
    int numIterations;

    //Magnification level, approx.
    long magn;

    //Tells you what type of shading to use. 
    //0 is default greyscale; 1 = red, 2 = green, 3 = blue; 4 is vibrant r/g/b, 5 is vibrant c/y/m, 6 is a 6-tone color variant of 4 and 5;
    //7 is dynamically generated based on the current resolution and zoom level; 8 and 9 are water and fire.
    int shading = 0;

    boolean debug = false;

    Toolkit tk = Toolkit.getDefaultToolkit();

    //The main thread; without this, KeyListener doesn't do anything, and nothing will work except for the original generation.
    Thread mainloop;

    //An image used in double buffering, and the graphics object that you use to draw to it. Used in the paint(Graphics g) method.
    BufferedImage backbuffer;
    Graphics2D g2d;

    boolean updated = false;

    /**
     * Sets up the initial size of the JFrame, as well as adds the KeyListener and starts the main loop.
     * Instantiate this when you're ready to begin using Mandelbrot.
     */
    public Drawer() {
        super("Mendelbrot Grapher");
        //setSize((int)tk.getScreenSize().getWidth(), (int)tk.getScreenSize().getHeight());
        setSize(200, 200);
        setUndecorated(true);
        setVisible(true);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE );   

        //Adds key listener.
        addKeyListener(this);               

        //This starts the main thread.
        mainloop = new Thread(this);
        mainloop.start();         
    }

    /**
     * A static main method that just creates a new Drawer Object for you to make life easier.
     */
    public static void main() {
        new Drawer();
    }      

    /**
     * @Override Runs the main loop and tries to update at 60fps. Can/will be less during a calculation.
     */
    public void run() {
        Thread t = Thread.currentThread();
        while (t == mainloop) {
            //Only draw when there's a need to update. No need to do otherwise.
            if (updated) {
                repaint();
                updated = false;
            }

            //This just keeps the thread alive
            try {
                Thread.sleep(1); 
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }

        }        
        //This is here for debugging, to tell you if the Thread has stopped for any reason except for normal closing.
        System.out.println("*******THREAD STOPPED FOR REASONS UNKNOWN*******");
    }

    /**
     * Calls update(), and then draws whatever changes were made there to the backbuffer, which is then drawn to screen.
     * @Override 
     */
    public void paint(Graphics g) {
        long time = System.currentTimeMillis();

        //Calls to redraw to the BufferedImage background.
        update();        
        //Draws the BufferedImage to the foreground.
        g.drawImage(backbuffer, 0, 0, this);

        System.out.println("Render time: " + (System.currentTimeMillis() - time));
    }

    /**
     * This is where the primary drawing logic goes. Accounts for any changes due to zooming or panning, updates the variables accordingly,
     * draws a white background as default (to avoid transparencies), and then draws the actual Mandelbrot graph using the proper shaders.
     */
    public void update() {
        //Tells you how much area every pixel represents, given the current panning size and window resolution.
        xIncrement = Math.abs(maxX - minX) / (double)this.getWidth() ; //the x increment = the width divided by the window size
        yIncrement = Math.abs(maxY - minY) / (double)this.getHeight(); //see above     

        //The current center of the panning window.
        xCenter = (minX + maxX) / 2;
        yCenter = (minY + maxY) / 2;

        //The distance between the center and the left and bottom edge, respectively. Used for calculating zoom/panning.
        xDist = Math.abs(xCenter - minX);
        yDist = Math.abs(yCenter - minY);

        //Updates the amount of iterations to perform based on the current level of resoution.
        numIterations = 4 * (int)Math.pow(2, resolutionLevel);   

        magn = (long)(2/xDist);

        //Prints out the current window/resolution details in a (hopefully) helpful manner. Will possibly be added to the graphing area in a debug menu.
        //System.out.println("X window: " + minX + " to " + maxX + " || " + "Y window: " + minY + " to " + maxY + " || " + "X incr.: " + xIncrement + " || " + "Y incr.: " + yIncrement);           
        //System.out.println("Center = " + xCenter + ", " + yCenter + " || Magnification (apr.): " + magn + " || Iterations: " + numIterations + " || Shade style: " + shading);

        //Creates a new image that is then drawn to. This image is the size of the window, allowing for resizing.
        backbuffer = new BufferedImage(getSize().width, getSize().height, BufferedImage.TYPE_INT_RGB);
        g2d = backbuffer.createGraphics();   

        //Draws the background.
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0,0,getSize().width,getSize().height);        

        //Draws the graph given the current shading settings.
        switch(shading) {
            case 0: drawGreyScale(g2d); break; //Black and white
            case 1: drawColorAlgoI(g2d, Color.RED); break; //Red
            case 2: drawColorAlgoI(g2d, Color.GREEN); break; //Green
            case 3: drawColorAlgoI(g2d, Color.BLUE); break; //Blue
            case 4: drawColorMultiple(g2d, 1); break; //Red, green, blue mix
            case 5: drawColorAlgoI(g2d, Color.CYAN); break;
            case 8: drawWater(g2d); break; //Fades from Blue to Cyan, and repeats.
            case 9: drawFire(g2d); break; //Red to yellow to white to red, repeats.
            default: drawColorAlgoII(g2d, Color.CYAN); break;
        }

        if (debug) {
            g2d.setColor(new Color(128, 128, 128));
            drawInfo(g2d);
        }
    }

    /**
     * Draws the graph, with current zoom/pan settings, in a pure black/white shade. 
     */
    private void drawGreyScale(Graphics2D g2d) {
        //Draws the graph itself. Takes time.
        g2d.setColor(Color.BLACK);

        for(int i = 0; i < this.getWidth(); i++) {
            for(int j = 0; j < this.getHeight(); j++) {
                ComplexNumber temp = new ComplexNumber((i*xIncrement) + minX, (j*yIncrement) + minY); //Was (i-this.getHeight()/2)*xIncrement and vice versa

                if (Logic.greyTest(temp, numIterations)) {
                    g2d.drawLine(i, j, i, j); //Draws this pixel as black
                }
            }
        }
    }

    //Draws the set with the color passed.
    //Algorithm 1 is essentially an extension of what the color drawing used to be: Start at black, get progressively brigher as iterations get
    //higher, and then black for -1 to give a striking appearance. Past 256, this gets very dark except for at very high magnification, but follows the old pattern.
    private void drawColorAlgoI(Graphics2D g2d, Color c) {
        for(int i = 0; i < this.getWidth(); i++) {
            for(int j = 0; j < this.getHeight(); j++) {
                ComplexNumber temp = new ComplexNumber((i*xIncrement) + minX, (j*yIncrement) + minY); //Was (i-this.getHeight()/2)*xIncrement and vice versa 

                int num = Logic.colorTest(temp, numIterations); //The number of iterations                

                int numItTrunc = numIterations;

                if (numItTrunc > 256) {
                    numItTrunc = 256;
                }                

                if (num == -1) {
                    g2d.setColor(Color.BLACK); //The actual Mandelbrot set is black
                } else {
                    if (c.equals(Color.RED)) {
                        g2d.setColor(new Color(num*(int)(256.0 / numIterations), 0, 0));                            
                    } else if (c.equals(Color.GREEN)) {
                        g2d.setColor(new Color(0, num*(int)(256.0 / numIterations), 0));                            
                    } else if (c.equals(Color.BLUE)) {
                        g2d.setColor(new Color(0, 0, num*(int)(256.0 / numIterations)));                            
                    } else if (c.equals(Color.CYAN)) {
                        g2d.setColor(new Color(0, (int)(num * (256.0 / numIterations)), (int)(num * (256.0 / numIterations)))); 
                        //g2d.setColor(new Color(0, (int)((num%256)*(256 / numIterations)), (int)((num%256)*(256 / numIterations))));                            
                    }
                }
                g2d.drawLine(i, j, i, j);
            }
        }    
    }

    //Same as above in purpose.
    //Algorithm 2 is slightly different: Instead of getting darker and darker, it will lock past 256 iterations and then start repeating.
    //Eg: 256 iterations, everything gets mapped from 0-255 as planned. 512 iterations, 1-256 gets mapped normally, and 257-512 gets mapped to 0-255 as well.
    private void drawColorAlgoII(Graphics2D g2d, Color c) {
        if (numIterations <= 256) {
            drawColorAlgoI(g2d, c);
        }       
        else {         
            for(int i = 0; i < this.getWidth(); i++) {
                for(int j = 0; j < this.getHeight(); j++) {

                    ComplexNumber temp = new ComplexNumber((i*xIncrement) + minX, (j*yIncrement) + minY); //Was (i-this.getHeight()/2)*xIncrement and vice versa 

                    int num = Logic.colorTest(temp, numIterations); //The number of iterations                

                    if (num == -1) {
                        g2d.setColor(Color.BLACK); //The actual Mandelbrot set is black
                    } else {
                        if (c.equals(Color.CYAN)) {
                            g2d.setColor(new Color(0, num % 256, num % 256));                          
                        }
                    }
                    g2d.drawLine(i, j, i, j);

                }
            }       
        }
    }

    //Draws the set with several colors.
    private void drawColorMultiple(Graphics2D g2d, int code) {
        switch(code) {
            case 1: //Red, green, blue

            for(int i = 0; i < this.getWidth(); i++) {
                for(int j = 0; j < this.getHeight(); j++) {
                    ComplexNumber temp = new ComplexNumber((i*xIncrement) + minX, (j*yIncrement) + minY); //Was (i-this.getHeight()/2)*xIncrement and vice versa 

                    int num = Logic.colorTest(temp, numIterations); //The number of iterations                 

                    if (num == -1) {
                        g2d.setColor(Color.BLACK);
                    } else if (num % 3 == 0) {
                        g2d.setColor(Color.RED);
                    } else if (num % 3 == 1) {
                        g2d.setColor(Color.GREEN);
                    } else if (num % 3 == 2) {
                        g2d.setColor(Color.BLUE);
                    }

                    g2d.drawLine(i, j, i, j);
                }
            }
        }
    }

    //Fades from Blue to Cyan, then repeats with no transition (on purpose)
    private void drawWater(Graphics2D g2d) {
        for(int i = 0; i < this.getWidth(); i++) {
            for(int j = 0; j < this.getHeight(); j++) {
                ComplexNumber temp = new ComplexNumber((i*xIncrement) + minX, (j*yIncrement) + minY); //Was (i-this.getHeight()/2)*xIncrement and vice versa 

                int num = Logic.colorTest(temp, numIterations); //The number of iterations    

                if (num == -1) {
                    g2d.setColor(Color.BLACK);
                } else {
                    g2d.setColor(new Color(0, (num % 32) * 8, 255)); //Blue fading to cyan
                }

                g2d.drawLine(i, j, i, j);
            }
        }
    }

    private void drawFire(Graphics2D g2d) {
        //To fix this, remove all the thread code; leave only one double-nested for loop; make it go from 0 to getWidth() and 0 to getHeight().
        new Thread() {
            public void run() {        
                for(int i = 0; i < getWidth() / 2; i++) {
                    for(int j = 0; j < getHeight() / 2; j++) {
                        ComplexNumber temp = new ComplexNumber((i*xIncrement) + minX, (j*yIncrement) + minY); //Was (i-this.getHeight()/2)*xIncrement and vice versa 

                        int num = Logic.colorTest(temp, numIterations); //The number of iterations    

                        if (num == -1) {
                            g2d.setColor(Color.BLACK);
                        } else {
                            if (num % (32 * 3)-1 < 32) {
                                g2d.setColor(new Color(255, (num % 32)*8, 0)); //Red fading to Yellow
                            } else if (num % (32 * 3)-1 < 64) {
                                g2d.setColor(new Color(255, 255, (num % 32)*8)); //Yellow fading to White
                            } else {
                                g2d.setColor(new Color(255, 255-(num%32)*8, 255-(num%32)*8)); //White fading to Red
                            }
                        }

                        g2d.drawLine(i, j, i, j);
                    }
                }      
            }
        }.start();

        new Thread() {
            public void run() {        
                for(int i = getWidth() / 2; i < getWidth(); i++) {
                    for(int j = 0; j < getHeight() / 2; j++) {
                        ComplexNumber temp = new ComplexNumber((i*xIncrement) + minX, (j*yIncrement) + minY); //Was (i-this.getHeight()/2)*xIncrement and vice versa 

                        int num = Logic.colorTest(temp, numIterations); //The number of iterations    

                        if (num == -1) {
                            g2d.setColor(Color.BLACK);
                        } else {
                            if (num % (32 * 3)-1 < 32) {
                                g2d.setColor(new Color(255, (num % 32)*8, 0)); //Red fading to Yellow
                            } else if (num % (32 * 3)-1 < 64) {
                                g2d.setColor(new Color(255, 255, (num % 32)*8)); //Yellow fading to White
                            } else {
                                g2d.setColor(new Color(255, 255-(num%32)*8, 255-(num%32)*8)); //White fading to Red
                            }
                        }

                        g2d.drawLine(i, j, i, j);
                    }
                }      
            }
        }.start();

        new Thread() {
            public void run() {        
                for(int i = 0; i < getWidth() / 2; i++) {
                    for(int j = getHeight() / 2; j < getHeight(); j++) {
                        ComplexNumber temp = new ComplexNumber((i*xIncrement) + minX, (j*yIncrement) + minY); //Was (i-this.getHeight()/2)*xIncrement and vice versa 

                        int num = Logic.colorTest(temp, numIterations); //The number of iterations    

                        if (num == -1) {
                            g2d.setColor(Color.BLACK);
                        } else {
                            if (num % (32 * 3)-1 < 32) {
                                g2d.setColor(new Color(255, (num % 32)*8, 0)); //Red fading to Yellow
                            } else if (num % (32 * 3)-1 < 64) {
                                g2d.setColor(new Color(255, 255, (num % 32)*8)); //Yellow fading to White
                            } else {
                                g2d.setColor(new Color(255, 255-(num%32)*8, 255-(num%32)*8)); //White fading to Red
                            }
                        }

                        g2d.drawLine(i, j, i, j);
                    }
                }      
            }
        }.start();

        new Thread() {
            public void run() {        
                for(int i = getWidth() / 2; i < getWidth(); i++) {
                    for(int j = getHeight() / 2; j < getHeight(); j++) {
                        ComplexNumber temp = new ComplexNumber((i*xIncrement) + minX, (j*yIncrement) + minY); //Was (i-this.getHeight()/2)*xIncrement and vice versa 

                        int num = Logic.colorTest(temp, numIterations); //The number of iterations    

                        if (num == -1) {
                            g2d.setColor(Color.BLACK);
                        } else {
                            if (num % (32 * 3)-1 < 32) {
                                g2d.setColor(new Color(255, (num % 32)*8, 0)); //Red fading to Yellow
                            } else if (num % (32 * 3)-1 < 64) {
                                g2d.setColor(new Color(255, 255, (num % 32)*8)); //Yellow fading to White
                            } else {
                                g2d.setColor(new Color(255, 255-(num%32)*8, 255-(num%32)*8)); //White fading to Red
                            }
                        }

                        g2d.drawLine(i, j, i, j);
                    }
                }      
            }
        }.start();
    }

    private void drawInfo(Graphics2D g2d) {
        if (shading >= 1 && shading <= 3) {
            g2d.setColor(Color.WHITE);
        }
        if (shading == 8) {
            g2d.setColor(Color.WHITE);
        }
        if (shading == 9) {
            g2d.setColor(Color.BLUE);
        }
        g2d.drawString(("Cen:~ " + new DecimalFormat("#.####E0").format(xCenter) + ", " + new DecimalFormat("#.####E0").format(yCenter)), 15, 45);
        if (magn == Long.MAX_VALUE) {
            g2d.drawString("Magn. too high!"  + (" @ " + numIterations + " iter."), 15, 60);            
        } else {
            g2d.drawString((new DecimalFormat("#.####E0").format(magn) + " x") + (" @ " + numIterations + " iter."), 15, 60);
        }
    }

    private void zoomIn() {
        minX = xCenter - Math.abs(xDist/4);
        maxX = xCenter + Math.abs(xDist/4);

        minY = yCenter - Math.abs(yDist/4);
        maxY = yCenter + Math.abs(yDist/4);

        update();
    }

    private void zoomOut() {
        minX = xCenter - Math.abs(xDist*4);
        maxX = xCenter + Math.abs(xDist*4);

        minY = yCenter - Math.abs(yDist*4);
        maxY = yCenter + Math.abs(yDist*4); 
        update();
    }

    private void moveUp() {
        double difference = Math.abs(maxY - minY);
        minY-=difference/4;
        maxY-=difference/4;
        update(); 
    }

    private void moveDown() {
        double difference = Math.abs(maxY - minY);
        minY+=difference/4;
        maxY+=difference/4;
        update();         
    }

    private void moveLeft() {
        double difference = Math.abs(maxX - minX);
        minX-=difference/4;
        maxX-=difference/4;
        update();         
    }

    private void moveRight() {
        double difference = Math.abs(maxX - minX);
        minX+=difference/4;
        maxX+=difference/4;
        update();        
    }    

    @Override
    public void keyReleased(KeyEvent key) {
        int k = key.getKeyCode();
        if (k==KeyEvent.VK_ALT || k==KeyEvent.VK_TAB) {
            requestFocus(); try { Thread.sleep(5); } catch(Exception f) {} requestFocus();
        }        
    }

    @Override
    public void keyPressed(KeyEvent key) {
        //Add: H as a help menu (in the top-left corner); 0 for zoom level 1; 1-9 for color settings (greyscale, red/green/blue tints, 3-tone vibrant and alternating,
        //3-tone vibrant [but with cyan, yellow and magenta instead of rgb], 6 or 9 tone vibrant, dynamic [generates based on zoom and resolution], etc)
        int k = key.getKeyCode();        
        if (k == KeyEvent.VK_EQUALS) {
            zoomIn(); updated = true;
        } else if (k==KeyEvent.VK_MINUS){
            zoomOut(); updated = true;
        } else if (k==KeyEvent.VK_UP) {
            moveUp(); updated = true;
        } else if (k==KeyEvent.VK_DOWN) {
            moveDown(); updated = true;
        } else if (k==KeyEvent.VK_LEFT) {
            moveLeft(); updated = true;
        } else if (k==KeyEvent.VK_RIGHT) {
            moveRight(); updated = true;
        } else if (k==81 /*q*/) {
            resolutionLevel++; updated = true;
        } else if (k==87 /*w*/) {
            resolutionLevel--; updated = true;
        } else if (k==69 /*e*/) {
            shading = ((shading + 1) % 10);  updated = true;//Cycles through shaders.
        } else if (k==82 /*r*/) {
            shading = ((shading - 1) % 10);  updated = true;//Same as above, but backwards.
        } else if (k>=48 && k <=57) {
            shading = k-48; updated = true;//Handles the 0-9 keys
        } else if (k==72 /*h*/) {
            debug = !debug; updated = true;
        } else if (k==KeyEvent.VK_ESCAPE) {
            setVisible(false); try { Thread.sleep(2000); } catch(Exception f) {} finally { setVisible(true);}
        } else if (k==KeyEvent.VK_ALT || k==KeyEvent.VK_TAB) {
            requestFocus(); try { Thread.sleep(5); } catch(Exception f) {} requestFocus();
        }
    }

    @Override
    public void keyTyped(KeyEvent key) {
        int k = key.getKeyCode();        
        if (k==KeyEvent.VK_ALT || k==KeyEvent.VK_TAB) {
            requestFocus(); try { Thread.sleep(5); } catch(Exception f) {} requestFocus();
        }    
    }  
}