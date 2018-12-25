package loop.model.simulationengine.distributions;

import java.util.ArrayList;
import java.util.List;

public class testMain {

    public static void main(String[] args) {
        //poisson distribution
        double lambda = 2.5;
        int n = 100000;
        
        PoissonDistribution dist = new PoissonDistribution(lambda);
        List<Integer> numbers = dist.getPicker().pickMany(n);
        
        double mean = 0;
        for (Integer i: numbers) {
            mean += i;
        }
        mean /= n;
        System.out.println("mean: " + mean + " (should be ~" + lambda + ").");
        System.out.println(dist.getSupportMin(0.7) + " , " + dist.getSupportMax(0.7));
        
        //binomial distribution
        
        //uniform distribution
        
        List<Integer> testlist = new ArrayList<Integer>();
        testlist.add(1);
        testlist.add(2);
        testlist.sort((Integer i1, Integer i2) -> (i1 - i2));
        System.out.println(testlist);
    }

}
