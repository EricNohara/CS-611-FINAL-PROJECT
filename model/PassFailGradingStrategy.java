package model;

public class PassFailGradingStrategy implements GradingStrategy {
    private final double passThreshold; //0.6 - 60%

    public PassFailGradingStrategy(double passThreshold) {
        this.passThreshold = passThreshold;
    }

    @Override
    public double calculateGrade(double pointsEarned, double maxPoints) {
        return (pointsEarned / maxPoints) >= passThreshold ? 1.0 : 0.0;
    }
}
