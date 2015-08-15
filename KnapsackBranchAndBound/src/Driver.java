import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * Created by jessenelson on 4/19/15.
 */
public class Driver {
    static Scanner in;

    static String getInput() {
        return in.nextLine();
    }

    private static void print(String msg) {
        System.out.println(msg);
    }

    public static void main (String args[]) {
        String fileName = args[0];
        String pathToFile = System.getProperty("user.dir").replace("\\", "/");
        pathToFile += "/" + fileName;
        try {
            System.setIn(new FileInputStream(new File(pathToFile)));
        } catch (IOException e) {
            print("Error setting system in to " + pathToFile + " " + e.getMessage());
        }
        in = new Scanner(new InputStreamReader(System.in));

        String line = getInput();
        int totalWeight = Integer.parseInt(line);

        line = getInput();
        int numItems = Integer.parseInt(line);

        ArrayList<KnapsackNode> items = buildNodes(numItems);

        KnapSack instance = new KnapSack(totalWeight, numItems, items);
        KnapsackNode winningNode = instance.getBest();
        System.out.println("The optimal result is: ");
        System.out.println(winningNode);
        for (int item : instance.getBestSetSoFar()) {
            System.out.println(item);
        }
    }

    static ArrayList<KnapsackNode> buildNodes(int numLines) {
        int i = 0;
        ArrayList<KnapsackNode> result = new ArrayList<KnapsackNode>();
        while (in.hasNext() && i < numLines) {
            String line [] = getInput().split(" ");
            int profit = Integer.parseInt(line[0]);
            int weight = Integer.parseInt(line[1]);
            KnapsackNode node = new KnapsackNode(profit, weight, 0);
            result.add(i, node);
            ++i;
        }

        return result;
    }
}
