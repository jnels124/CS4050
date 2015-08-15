import java.util.ArrayList;
import java.util.PriorityQueue;

/**
 * Created by jessenelson on 4/19/15.
 */
public class KnapSack {
    private PriorityQueue<KnapsackNode> priorityQueue;
    private int maxWeight;
    private int numItems;
    private ArrayList<KnapsackNode> origItems;

    private ArrayList<Integer> bestSetSoFar = new ArrayList<Integer>();

    public KnapSack(int maxWeight, int numItems, ArrayList<KnapsackNode> origItems) {
        this.maxWeight = maxWeight;
        this.numItems = numItems;
        this.origItems = origItems;
    }

    public KnapsackNode getBest() {
        KnapsackNode root = new KnapsackNode();
        // Track the node with the most promising data
        KnapsackNode mostPromising = new KnapsackNode();
        root.setBound(calculateBound(root));
        this.priorityQueue = new PriorityQueue<KnapsackNode>();
        this.priorityQueue.add(root);
        while(!this.priorityQueue.isEmpty()) {
            KnapsackNode exploring = priorityQueue.remove();
            System.out.println("Exploring:\n" + exploring + "\n\n");
            KnapsackNode option1 = new KnapsackNode(exploring);
            KnapsackNode option2 = new KnapsackNode(exploring);
            if (exploring.getBound() > mostPromising.getProfit() && exploring.getLevel() < this.origItems.size() - 1) {
                KnapsackNode item = this.origItems.get(exploring.getLevel());
                option1.setWeight(option1.getWeight() + item.getWeight());
                if (option1.getWeight() <= this.maxWeight) {

                    option1.addPickedItemToPickedItems(exploring.getLevel());
                    option1.setProfit(option1.getProfit() + item.getProfit());
                    option1.setBound(calculateBound(option1));

                    if (option1.getProfit() > mostPromising.getProfit()) {
                        mostPromising = option1;
                        System.out.println("Updated most promisting to:\n" + mostPromising + "\n\n");
                    }
                    if (option1.getBound() > mostPromising.getProfit()) {
                        this.priorityQueue.add(option1);
                    }
                }
                option2.setBound(calculateBound(option2));

                if (option2.getBound() > mostPromising.getProfit()) {
                    this.priorityQueue.add(option2);
                }
            }
        }
        return mostPromising;
    }

    public ArrayList<Integer> getBestSetSoFar() {
        return bestSetSoFar;
    }

    public double calculateBound (KnapsackNode current) {
        double bound = current.getProfit();
        double totalWeight = current.getWeight();
        int k = current.getLevel();
        while (k < numItems && totalWeight + this.origItems.get(k).getWeight() <= this.maxWeight) {
            bound += this.origItems.get(k).getProfit();
            totalWeight += this.origItems.get(k).getWeight();
            k++;
        }
        if (k < this.numItems) {
            bound += (this.maxWeight - totalWeight) * (this.origItems.get(k).getProfit() / this.origItems.get(k).getWeight());
        }
        return bound;
    }
}
