package util;

public class Function {

    // declare the main data members
    final protected int ID;
    protected String function;
    protected GridSystem grid = null;

    private static int Counter = 0;
    public static double increment = 0.000001;

    public Function(String function){
        this.function = function;
        // increment the counter
        ID = ++Counter;
    }

    public Function(String function , GridSystem grid){
        this(function);
        this.grid = grid;
    }

    public Point getPoint(double x){
        double y = getValue(x);
        return new Point(x ,y);

    }

    public double getValue(double x){
        return FunctionValue.getValue(function , x);
    }

    public void setGrid(GridSystem grid) {
        this.grid = grid;
    }

    public double getTangent(double x){
        // return the tangent at the point x
        return (getValue(x + increment/2) - getValue(x - increment/2)) / increment;
    }

    public SimpleLine getTangentLine(double x){
        Point point = new Point(x , getValue(x));
        return new SimpleLine(getTangent(x), point);
    }


    public String getFunction() {
        return function;
    }

    public void setFunction(String function) {
        this.function = function;
    }

    public static double getIncrement() {
        return increment;
    }

    public static void setIncrement(double increment){
        if (increment <= 0){
            return;
        }
        Function.increment = increment;
    }

    @Override
    public String toString() {
        return String.format("%s" , function);
    }

    public int getID() {
        return ID;
    }
}
