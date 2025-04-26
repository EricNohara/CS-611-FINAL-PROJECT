package model;

public class ProportionalGradingStrategy implements GradingStrategy {
    @Override
    public double calculateGrade(double pointsEarned, double maxPoints) {
        return pointsEarned / maxPoints;
    }
}
