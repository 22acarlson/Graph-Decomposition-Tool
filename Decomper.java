//I have done my best to make this algorithm readable. If there are any questions, email me: ajcarls2@mtu.edu

import java.io.*;
import java.util.*;

public class Decomposer {
    static class Graph {
        //We keep track of the vertices as a total number instead of representing them as a data structure
        int vertices;
        List<int[]> edges = new ArrayList<>();

        Graph(int v) {
            vertices = v;
        }

        void addEdge(int u, int v) {
            edges.add(new int[]{Math.min(u, v), Math.max(u, v)});
        }
    }

    //The graph data that is pulled from the graphs_H_edges.txt file
    static class Page {
        String name;
        Graph graph;

        Page(String name, Graph graph) {
            this.name = name;
            this.graph = graph;
        }
    }

    static class Solve {
        int n;
        int h;
        int totalEdges;
        int copiesNeeded;
        int[][] edgeIndex;
        int[] edgeLowVal;
        int[] edgeHighVal;
        boolean[] firstCopy;
        int[] firstCopyEdges;
        boolean[][] degreeReachable;
        List<int[]> hEdges;
        List<List<Integer>> neighbors = new ArrayList<>();
        List<List<int[]>> edgePlacements = new ArrayList<>();
        boolean[] remaining;
        List<int[]> decomposition = new ArrayList<>();

        //H is the starter that we are decomposing the complete graph into
        Solve(int n, Graph H) {
            //Here we store the order of the complete graph
            this.n = n;
            this.h = H.vertices;
            this.hEdges = H.edges;
            totalEdges = n*(n - 1)/2;
            edgeIndex = new int[n][n];
            edgeLowVal = new int[totalEdges];
            edgeHighVal = new int[totalEdges];
            int k = 0;
          
            for(int i = 0; i < n; i++)
                for(int j = i + 1; j < n; j++) {
                    edgeIndex[i][j] = edgeIndex[j][i] = k;
                    edgeLowVal[k] = i;
                    edgeHighVal[k] = j;
                    k++;
                }

            firstCopy = new boolean[totalEdges];
            int edgeCountOfFirstStarter = 0;

            //Since every first placement of H is isomorphic, we choose one specific starter to optimize the process
            for(int[] edge : H.edges) {
                int e = edgeIndex[edge[0]][edge[1]];
                if(!firstCopy[e]) {
                    firstCopy[e] = true;
                    edgeCountOfFirstStarter++;
                }
            }
            firstCopyEdges = new int[edgeCountOfFirstStarter];
            int spot = 0;
            for(int e = 0; e < totalEdges; e++) {
                if(firstCopy[e]) {
                  firstCopyEdges[spot++] = e;
                }
            }

            //Checks to make sure the edges of the starter satisfies the necessary conditions
            if(edgeCountOfFirstStarter == 0 || totalEdges % edgeCountOfFirstStarter != 0) {
                copiesNeeded = -1;
                degreeReachable = null;
                return;
            }
            copiesNeeded = totalEdges/edgeCountOfFirstStarter;
          
            int[] deg = new int[h];
            for(int[] edges : H.edges) {
                deg[edges[0]]++;
                deg[edges[1]]++;
            }

            //degreeReachable[i][j] is only true if a K_n vertex with j uncovered edges can still be covered using at most i more copies of H.
            degreeReachable = new boolean[copiesNeeded + 1][n];
            degreeReachable[0][0] = true;
          
            //Checks each addition of an arbitrarily placed H to see if it violates the degree constraints
            for(int i = 1; i <= copiesNeeded; i++) {
                for(int j = 0; j < n; j++) {
                    if(degreeReachable[i - 1][j]) {
                        degreeReachable[i][j] = true;
                    }
                    for(int iter : deg)
                        if(iter <= j && degreeReachable[i - 1][j - iter]) {
                            degreeReachable[i][j] = true;
                            break;
                        }
                }
            }

            for(int i = 0; i < h; i++) {
                neighbors.add(new ArrayList<>());
            }
            //Each edge is added only once by limiting the addition of each edge to the lower label.
            for(int[] e : H.edges) {
                neighbors.get(e[1]).add(e[0]);
            }

            createPossibleEdgePlacements();
        }

        private void createPossibleEdgePlacements() {
            List<int[]> allPlacements = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            placeHEdges(0, new int[h], new boolean[n], new boolean[totalEdges], allPlacements, seen);

            for(int edge = 0; edge < totalEdges; edge++) {
                edgePlacements.add(new ArrayList<>());
            }
            for(int[] placement : allPlacements) {
                for(int edge : placement) {
                    edgePlacements.get(edge).add(placement);
                }
            }
        }



        //map[i] is the vertex of K_n that vertex i of H gets sent to
        private void placeHEdges(int vertex, int[] map, boolean[] usedVertex, boolean[] placedEdges, List<int[]> allPlacements, Set<String> seen) {
            if(vertex == h) {
                int count = 0;
              
                for(boolean isPlaced : placedEdges) {
                    if(isPlaced) count++;
                }
              
                int[] placement = new int[count];
                int possibleNext = 0;

                for(int e = 0; e < totalEdges; e++) {
                    if(placedEdges[e]) placement[possibleNext++] = e;
                }
              
                String key = Arrays.toString(placement);
              
                if(!seen.contains(key)) {
                    seen.add(key);
                    allPlacements.add(placement);
                }
              
                return;
            }

            for(int v = 0; v < n; v++) {
                if(usedVertex[v]) {
                    continue;
                }
              
                boolean[] canidateEdges = placedEdges.clone();
              
                for(int u : neighbors.get(vertex)) {
                    canidateEdges[edgeIndex[v][map[u]]] = true;
                }

                boolean hitsFirstCopy = false;
              
                for(int e = 0; e < totalEdges; e++) {
                    if(canidateEdges[e] && firstCopy[e]){
                        hitsFirstCopy = true;
                    }
                }
              
                if(hitsFirstCopy) {
                    continue;
                }
              
                usedVertex[v] = true;
                map[vertex] = v;
                placeHEdges(vertex + 1, map, usedVertex, canidateEdges, allPlacements, seen);
                usedVertex[v] = false;
            }
        }

        //Here we recursively build every automorphism of H, assigning an image to each vertex in order. A partial label is abandoned as soon as the labeling breaks the edge structure of H.
        private void depthFirstSearchOfAutomorphisms(int vertex, int[] sentVertex, boolean[] usedVertex, boolean[][] adjacent, List<int[]> outputedAutomorphism) {
            if(vertex == h) {
                outputedAutomorphism.add(sentVertex.clone());
                return;
            }

            for(int v = 0; v < h; v++) {
                if(usedVertex[v]) {
                    continue;
                }

                boolean noContradiction = true;

                for(int k = 0; k < vertex && noContradiction; k++) {
                    noContradiction = adjacent[vertex][k] == adjacent[v][sentVertex[k]];
                }

                if(!noContradiction) {
                    continue;
                }

                usedVertex[v] = true;
                sentVertex[vertex] = v;
                depthFirstSearchOfAutomorphisms(vertex + 1, sentVertex, usedVertex, adjacent, outputedAutomorphism);
                usedVertex[v] = false;
            }
        }

        //Finds the automorphisms of H. Each automorphism is represented as an int[].
        private List<int[]> createAutomorphismsOfHEdges() {
            boolean[][] adj = new boolean[h][h];

            for(int[] edges : hEdges) {
                adj[edges[0]][edges[1]] = adj[edges[1]][edges[0]] = true;
            }

            List<int[]> automorphismList = new ArrayList<>();
            depthFirstSearchOfAutomorphisms(0, new int[h], new boolean[h], adj, automorphismList);
            return automorphismList;
        }

        private int[] creatEdgePermutation(int[] automorphism) {
            int[] edgePlacement = new int[totalEdges];
            for(int edge = 0; edge < totalEdges; edge++) {
                int lowerVertex = automorphism[edgeLowVal[edge]];
                int upperVertex = automorphism[edgeHighVal[edge]];
                edgePlacement[edge] = edgeIndex[lowerVertex][upperVertex];
            }
            return edgePlacement;
        }

        private boolean findDecomposition(int copiesLeft, List<int[]> placements) {
            int lowestUncoveredEdge = -1;
            for(int e = 0; e < totalEdges; e++) {
                if(remaining[e]) {
                    lowestUncoveredEdge = e;
                    break;
                }
            }
            if(lowestUncoveredEdge == -1) {
                decomposition = new ArrayList<>(placements);
                return true;
            }

            int[] uncovered = new int[n];
            for(int e = 0; e < totalEdges; e++) {
                if(remaining[e]) {
                    uncovered[edgeLowVal[e]]++;
                    uncovered[edgeHighVal[e]]++;
                }
            }
            for(int v = 0; v < n; v++) {
                if(!degreeReachable[copiesLeft][uncovered[v]]) {
                    return false;
                }
            }

            for(int[] placement : edgePlacements.get(lowestUncoveredEdge)) {
                boolean isFits = true;
              
                for(int e : placement) {
                    if(!remaining[e]) isFits = false;
                }
              
                if(!isFits) {
                    continue;
                }

                for(int e : placement){
                    remaining[e] = false;
                }

                placements.add(placement);
                if(findDecomposition(copiesLeft - 1, placements)) {
                    return true;
                }

                placements.remove(placements.size() - 1);
                for(int e : placement) remaining[e] = true;
            }
            return false;
        }

        //Canonical ordering to remove redundant cases when iterating through every possible case
        private boolean checkCanonicalLabeling(int[] placement, List<int[]> stabilizerPermutationSet) {
            boolean[] original = new boolean[totalEdges];

            for(int e : placement){
                original[e] = true;
            }

            for(int[] edgePermutation : stabilizerPermutationSet) {
                boolean[] symmetric = new boolean[totalEdges];
                for(int e : placement) {
                    symmetric[edgePermutation[e]] = true;
                }

                for(int e = totalEdges - 1; e >= 0; e--) {
                    if(symmetric[e] != original[e]) {
                        if(original[e]) return false;
                        break;
                    }
                }
            }
            return true;
        }

        //Convert the edges into readable text
        private String convertTheEdgesToText(int[] edges) {
            List<String> listOfEdges = new ArrayList<>();
          
            for(int edgeInUse : edges) {
                listOfEdges.add("(" + edgeLowVal[edgeInUse] + "," + edgeHighVal[edgeInUse] + ")");
            }
          
            return listOfEdges.toString();
        }

        //Where we attempt the decomposition
        public boolean decomposeCompleteGraphIntoH() {
            //This checks that a decomposition could even be possible, we set the number of necessary copies of a starter that does not satisfy the necessary conditions to -1.
            if(copiesNeeded < 0) {
                return false;
            }

            remaining = new boolean[totalEdges];
            int secondCopyEdge = -1;
          
            for(int e = 0; e < totalEdges; e++) {
                remaining[e] = !firstCopy[e];
                if(remaining[e] && secondCopyEdge == -1) {
                    secondCopyEdge = e;
                }
            }

            if(secondCopyEdge == -1) {
                return true;
            }

            //Builds a list for the ways to relabel H such that the fixed edge is mapped to itself.
            List<int[]> stabilizer = new ArrayList<>();
            for(int[] automorphism : createAutomorphismsOfHEdges()) {
                int[] edgePermutation = creatEdgePermutation(automorphism);

                if(edgePermutation[secondCopyEdge] == secondCopyEdge) {
                    stabilizer.add(edgePermutation);
                }
            }

            //Every possible second copy, skipping the ones that are isomorphic to another case
            for(int[] iter : edgePlacements.get(secondCopyEdge)) {
                boolean isFits = true;

                for(int e : iter) {
                    if(!remaining[e]){
                        isFits = false;
                    }
                }

                if(!isFits || !checkCanonicalLabeling(iter, stabilizer)) {
                    continue;
                }

                for(int e : iter) {
                    remaining[e] = false;
                }
                List<int[]> placements = new ArrayList<>();
                placements.add(iter);
                if(findDecomposition(copiesNeeded - 2, placements)) {
                    return true;
                }
                for(int e : iter) remaining[e] = true;
            }
            return false;
        }

        //If a solution is found then print the decomposition
        public void printSolution() {
            System.out.println("Decomposition of K_" + n + " into " + (decomposition.size() + 1) + " copies:");
            System.out.println("Copy 1 edges: " + convertTheEdgesToText(firstCopyEdges));
            for(int i = 0; i < decomposition.size(); i++)
                System.out.println("Copy " + (i + 2) + " edges: " + convertTheEdgesToText(decomposition.get(i)));
        }
    }

    //This is a simple parser that grabs the graph code out of graphs_H_edges.txt
    public static List<Page> loadPages(String filename,int n) {
        List<Page> pages = new ArrayList<>();
        try(BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            Page current = null;
            while((line = br.readLine()) != null) {
                line = line.trim();

                if(line.startsWith("Page:")) {
                    if(current != null) pages.add(current);
                    current = new Page(line.substring(5).trim(), new Graph(n));
                } else if(line.startsWith("H.addEdge")) {
                    int l = line.indexOf('('), r = line.indexOf(')');

                    if(l >= 0 && r >= 0 && current != null) {
                        String[] p = line.substring(l + 1, r).split(",");
                        current.graph.addEdge(Integer.parseInt(p[0].trim()), Integer.parseInt(p[1].trim()));
                    }
                }
            }
            if(current != null) {
                pages.add(current);
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
        return pages;
    }

    public static void main(String[] args) {
        //Here is where you set the order of the complete graph
        int n = 9;

        List<Page> pages = loadPages("graphs_H_edges.txt",n);

        for(Page page : pages) {
            System.out.println("---------------------------------------------------------------------------------");
            System.out.println("Graph " + page.name);

            //Skip any graph that uses a vertex label that is not in K_n
            int maxLabel = 0;
            for (int[] edge : page.graph.edges) {
                maxLabel = Math.max(maxLabel, edge[1]);
            }
            if(maxLabel >= n) {
                System.out.println("H has " + (maxLabel-n+1) + " more vertices than are in K_" + n + ". A decomposition can not exist!");
                continue;
            }

            Solve decomposition = new Solve(n, page.graph);
            if(decomposition.decomposeCompleteGraphIntoH()) {
                System.out.println("Labeling found");
                decomposition.printSolution();
            } else {
                System.out.println("No decomposition exists");
            }
        }
    }
}
