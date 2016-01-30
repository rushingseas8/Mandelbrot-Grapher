import java.util.Scanner;

public class Tester {
    public static void main() {
        Scanner scanner = new Scanner(System.in);

        while(true) {
            System.out.println();
            
            System.out.print("Enter the number of iterations: ");
            int numIterations = scanner.nextInt();

            System.out.print("\nEnter the returned num iterations: ");
            int num = scanner.nextInt();

            //int toRe = (int)(num * (256.0 / numIterations)); //Original algorithm, useless past 256
            //int toRe = num * (int)(256.0 / numIterations); //Current algo, similar to orig but works past 256. Darker though.
            int toRe = num % 256;
            
            System.out.print("\nReturned: " + toRe + "\n");
        }
    }
}