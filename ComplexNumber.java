public class ComplexNumber
{
    public double irrationalValue;
    public double realValue;

    public ComplexNumber(double a, double b) {
        irrationalValue = a;
        realValue = b;
    }

    public String toString() {
        return irrationalValue + "i + " + realValue;
    }

    public ComplexNumber add(ComplexNumber other) {
        ComplexNumber toReturn = new ComplexNumber(irrationalValue, realValue);
        
        toReturn.irrationalValue += other.irrationalValue;
        toReturn.realValue+= other.realValue;
        
        return toReturn;
    }

    public ComplexNumber square() {
        ComplexNumber toReturn = new ComplexNumber(irrationalValue, realValue);        
        
        toReturn.irrationalValue=2*(realValue * irrationalValue);
        toReturn.realValue=(realValue*realValue) - (irrationalValue*irrationalValue);
        
        return toReturn;
    }
}
