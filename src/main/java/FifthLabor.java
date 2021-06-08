import edu.uci.ics.jung.graph.Graph;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.HashSet;

public class FifthLabor {

    private static final int TRANSACTIONLENGTH = 64;

    // Main method: simply invoke everything
    public static void main(String[] args) throws SQLException, ClassNotFoundException {
        int days = 1;//Integer.parseInt(args[0]);
        long filterAmount = 200000;//Long.parseLong(args[1]);
        ZonedDateTime toTime = ZonedDateTime.now();
        ZonedDateTime fromTime = toTime.minusDays(days);
        System.out.println("We will parse data for " + fromTime.toString() + " to " + toTime.toString());

        GraphExtractor extractor = new GraphExtractor();
        Graph<String, WeightedEdge> graph = extractor.extractGraphFor(fromTime, toTime, 2448);
        HashSet<Object> addresses = new HashSet<>(graph.getVertices());
        Collection<String> vertices = new HashSet<>(graph.getVertices());
        for (String vertex : vertices) {
            if (vertex.length() == TRANSACTIONLENGTH) {
                boolean deleteThisTransaction = true;
                for (WeightedEdge edge : graph.getOutEdges(vertex)) {
                    long value = edge.getValue();
                    if (value > filterAmount) deleteThisTransaction = false;

                }
                if (deleteThisTransaction) {
                    graph.removeVertex(vertex);
                }
            }
        }
        System.out.println("vertex: " + graph.getVertexCount() + " for >" + filterAmount);
        System.out.println("edges: " + graph.getEdgeCount() + " for >" + filterAmount);
        for (String vertex : vertices) {
            if (!(vertex.length() == TRANSACTIONLENGTH)) {
                boolean deleteThisAddress = false;
                for (WeightedEdge edge : graph.getInEdges(vertex)) {
                    long value = edge.getValue();

                    if (value < filterAmount) {
                        deleteThisAddress = true;
                        break;
                    }
                }

                if (deleteThisAddress) {
                    graph.removeVertex(vertex);
                }
            }
        }
        System.out.println("vertex: " + graph.getVertexCount() + " for >" + filterAmount);
        System.out.println("edges: " + graph.getEdgeCount() + " for >" + filterAmount);
    }
}
