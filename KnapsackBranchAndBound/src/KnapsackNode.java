import java.util.ArrayList;
import java.util.List;

public class KnapsackNode implements Comparable<KnapsackNode> {
    private double profit;
    private double weight;
    private double bound;
    private List<Integer> pickedItems;
    private int level;

    KnapsackNode() {
        this.pickedItems = new ArrayList<Integer>();
    }

    KnapsackNode (KnapsackNode parentNode) {
        this.level = parentNode.level + 1;
        this.pickedItems = new ArrayList<>(parentNode.pickedItems);
        this.bound = parentNode.bound;
        this.profit = parentNode.profit;
        this.weight = parentNode.weight;

    }

    KnapsackNode(double profit, double weight, double bound) {
        this.profit = profit;
        this.weight = weight;
        this.bound = bound;
        this.level = 0;
    }

    public double getBound() {
        return bound;
    }

    public void setBound(double bound) {
        this.bound = bound;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public double getProfit() {
        return profit;
    }

    public void setProfit(double profit) {
        this.profit = profit;
    }

    public List<Integer> getPickedItems() {
        return pickedItems;
    }

    public void addPickedItemToPickedItems(int itemIndex) {
        this.pickedItems.add(new Integer(itemIndex));
    }

    public int getLevel() {
        return level;
    }

    @Override
    public int compareTo(KnapsackNode that) {
        return (int) (that.bound - this.bound);
    }

    @Override
    public String toString() {
        String items = "";
        for (int pickedItem : this.getPickedItems()) {
            items += pickedItem + 1 + " ";
        }
        return "Weight: " + this.getWeight() +
                " Profit: " + this.getProfit() +
                "\nItems: " + items;
    }
}