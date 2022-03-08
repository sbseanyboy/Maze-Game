/* the game is initialized to manual playing mode
 * 
 * use the left, right, top, and bottom arrows to navigate the maze
 * 
 * hit the "r" key to reset the maze to a new random maze, 
 * the game mode will be reset to manual as well.
 * 
 * hit "b" to trigger bfs
 * 
 * hit "d" to trigger dfs
 * 
 * please wait until the board is finished drawing to use bfs or dfs!
 * 
 * the board is prettiest when the supplied integer to bigbang is a multiple of 4.
 * 
 * good luck!
 */

import java.util.ArrayList;

import java.util.Arrays;
import java.util.ArrayDeque;

import tester.*;
import java.util.Random;
import javalib.impworld.*;
import java.awt.Color;
import javalib.worldimages.*;
import java.util.HashMap;
import java.util.Comparator;

//Represents a mutable collection of items
interface ICollection<T> {
  // Is this collection empty?
  boolean isEmpty();

  // EFFECT: adds the item to the collection
  void add(T item);

  // Returns the first item of the collection
  // EFFECT: removes that first item
  T remove();
}

// stack class for the dfs method
class Stack<T> implements ICollection<T> {
  ArrayDeque<T> contents;

  // constructor
  Stack() {
    this.contents = new ArrayDeque<T>();
  }

  // determines if this stack is empty
  public boolean isEmpty() {
    return this.contents.isEmpty();
  }

  // removes the first item from the stack
  public T remove() {
    return this.contents.removeFirst();
  }

  // adds the supplied item to the front of the stack
  public void add(T item) {
    this.contents.addFirst(item);
  }
}

// queue class for the bfs method
class Queue<T> implements ICollection<T> {
  ArrayDeque<T> contents;

  // constructor
  Queue() {
    this.contents = new ArrayDeque<T>();
  }

  // determines if the queue is empty
  public boolean isEmpty() {
    return this.contents.isEmpty();
  }

  // removes the first item from the queue
  public T remove() {
    return this.contents.removeFirst();
  }

  // adds the supplied item to the end of the queue
  public void add(T item) {
    this.contents.addLast(item); // NOTE: Different from Stack!
  }
}

// data representing a vertex in a graph
class Vertex {
  int x;
  int y;
  int size;
  Color color;
  boolean traced;
  // edges
  Edge left;
  Edge right;
  Edge top;
  Edge bottom;

  // constructor with every field supplied
  Vertex(int x, int y, int size, Color color, boolean traced, Edge left, Edge right, Edge top,
      Edge bottom) {
    this.x = x;
    this.y = y;
    this.size = size;
    this.color = Color.gray;
    this.traced = traced;
    this.left = left;
    this.right = right;
    this.top = top;
    this.bottom = bottom;
  }

  // draws a vertex
  WorldImage drawVertex(ArrayList<Edge> list) {
    // horizontal line image
    WorldImage hori = new LineImage(new Posn(size, 0), Color.BLACK);
    // vertical line image
    WorldImage verti = new LineImage(new Posn(0, size), Color.BLACK);
    // square image of the vertex itself
    WorldImage vertex = new RectangleImage(this.size, this.size, OutlineMode.SOLID, this.color);
    // adds a line to denote an edge to each side of the vertex
    if (this.left != null && !list.contains(this.left)) {
      vertex = new BesideImage(verti, vertex);
    }
    if (this.right != null && !list.contains(this.right)) {
      vertex = new BesideImage(vertex, verti);
    }
    if (this.top != null && !list.contains(this.top)) {
      vertex = new AboveImage(hori, vertex);
    }
    if (this.bottom != null && !list.contains(this.bottom)) {
      vertex = new AboveImage(vertex, hori);
    }
    return vertex;
  }

  // // equals override for vertices
  // public boolean equals(Object o) {
  // if (!(o instanceof Vertex)) {
  // return false;
  // }
  // // this cast is safe, because we just checked instanceof
  // Vertex that = (Vertex) o;
  // return this.x == that.x && this.y == that.y && this.color.equals(that.color)
  // && this.traced == that.traced;
  // }

  // modifies the cell to be blue, and traced
  public void modifyCell() {
    this.color = Color.blue;
    this.traced = true;
  }
}

// comparator class for an Edge
class EdgeComparator implements Comparator<Edge> {

  // compares two edges by weight
  public int compare(Edge o1, Edge o2) {
    return o1.compareTo(o2);
  }

}

// class representing an Edge in a graph
class Edge implements Comparable<Edge> {
  Vertex from;
  Vertex to;
  int weight;

  // constructor taking in all fields
  Edge(Vertex from, Vertex to, int weight) {
    this.from = from;
    this.to = to;
    this.weight = weight;
  }

  // modifies the edge if it's bottom/right of the vertex that called it
  public Vertex modifyBREdge() {
    this.to.modifyCell();
    return this.to;
  }

  // modifies the edge if it's the top/left of the vertex that called it
  public Vertex modifyTLEdge() {
    this.from.modifyCell();
    return this.from;
  }

  // the compareTo method comparing two Edges by weight
  public int compareTo(Edge o) {
    return this.weight - o.weight;
  }
}

// MazeWorld class
class MazeWorld extends World {
  int width;
  int height;
  ArrayList<Vertex> vertices;
  ArrayList<Edge> worklist;
  ArrayList<Edge> edgesInTree;
  ArrayList<Integer> weights;
  HashMap<Vertex, Vertex> representatives;
  int xCells;
  int yCells;
  int cellSize;
  int time;
  Vertex curVer;
  String gameMode;

  // initializes the game with just the number of cells per row supplied
  MazeWorld(int xCells) {
    this.width = 800;
    this.height = 600;
    this.vertices = new ArrayList<Vertex>();
    this.worklist = new ArrayList<Edge>();
    this.edgesInTree = new ArrayList<Edge>();
    this.representatives = new HashMap<Vertex, Vertex>();
    this.weights = new ArrayList<Integer>();
    this.xCells = xCells;
    this.yCells = (xCells / 4) * 3;
    this.cellSize = this.width / this.xCells;
    this.time = 0;
    this.curVer = null;
    this.gameMode = "m";
  }

  // initializes the game to an ArrayList of vertices
  MazeWorld(ArrayList<Vertex> vertices, int xCells) {
    this.width = 800;
    this.height = 600;
    this.vertices = vertices;
    this.worklist = new ArrayList<Edge>();
    this.edgesInTree = new ArrayList<Edge>();
    this.representatives = new HashMap<Vertex, Vertex>();
    this.weights = new ArrayList<Integer>();
    this.xCells = xCells;
    this.yCells = (xCells / 4) * 3;
    this.cellSize = this.width / this.xCells;
    this.time = 0;
    this.curVer = null;
    this.gameMode = "m";
  }

  // builds the world based on the inputted number of cells per row of the Maze
  public MazeWorld buildWorld() {
    if (this.xCells < 4) {
      this.yCells = this.xCells;
    }
    // creates a vertex for each value
    for (int y = 0; y < this.yCells; y = y + 1) {
      for (int x = 0; x < this.xCells; x = x + 1) {
        Vertex newVert = (new Vertex(x, y, this.cellSize, Color.gray, false, null, null, null,
            null));
        this.vertices.add(newVert);
        this.representatives.put(newVert, newVert);
      }
    }
    // creates a list of integers to randomize weights with
    for (int i = 0; i < ((this.xCells * this.yCells) * 2) - 1; i++) {
      this.weights.add(i);
    }
    // initializes Vertexes to refer to each other through Edges left, right, top,
    // and bottom
    for (int y = 0; y < this.yCells; y = y + 1) {
      for (int x = 0; x < this.xCells; x = x + 1) {

        Vertex v1 = vertices.get((y * this.xCells) + x);
        Random rand = new Random();

        // EFFECT: initializes the top edge
        if ((y > 0) && v1.top == null) {
          int random1 = rand.nextInt(this.weights.size());
          Edge topEdge = new Edge(this.findTop(x, y), v1, this.weights.get(random1));
          this.weights.remove(random1);
          v1.top = topEdge;
          this.findTop(x, y).bottom = topEdge;
          this.worklist.add(topEdge);
        }

        // EFFECT: initializes the bottom edge
        if ((y < this.yCells - 1) && v1.bottom == null) {
          int random2 = rand.nextInt(this.weights.size());
          Edge bottomEdge = new Edge(v1, this.findBottom(x, y), this.weights.get(random2));
          this.weights.remove(random2);
          v1.bottom = bottomEdge;
          this.findBottom(x, y).top = bottomEdge;
          this.worklist.add(bottomEdge);
        }

        // EFFECT: initializes the left edge
        if ((x > 0) && v1.left == null) {
          int random3 = rand.nextInt(this.weights.size());
          Edge leftEdge = new Edge(this.findLeft(x, y), v1, this.weights.get(random3));
          this.weights.remove(random3);
          v1.left = leftEdge;
          this.findLeft(x, y).right = leftEdge;
          this.worklist.add(leftEdge);
        }

        // EFFECT: initializes the right edge
        if ((x < this.xCells - 1) && v1.right == null) {
          int random4 = rand.nextInt(this.weights.size());
          Edge rightEdge = new Edge(v1, this.findRight(x, y), this.weights.get(random4));
          this.weights.remove(random4);
          v1.right = rightEdge;
          this.findRight(x, y).left = rightEdge;
          this.worklist.add(rightEdge);
        }
      }
    }
    // EFFECT: sorts the worklist by weight
    new ArrayUtils().quicksort(this.worklist, new EdgeComparator());
    // EFFECT: first cell is green
    this.vertices.get(0).color = Color.green;
    // EFFECT: the current operating cell is the first cell
    this.curVer = this.vertices.get(0);
    // EFFECT: last cell is purple
    this.vertices.get(vertices.size() - 1).color = Color.magenta;
    return this;
  }

  // finds the Vertex to the left of the specified coordinates
  public Vertex findLeft(int x, int y) {
    return this.vertices.get((y * this.xCells) + x - 1);
  }

  // finds the Vertex to the top of the specified coordinates
  public Vertex findTop(int x, int y) {
    return this.vertices.get(((y - 1) * this.xCells) + x);
  }

  // finds the Vertex to the right of the specified coordinates
  public Vertex findRight(int x, int y) {
    return this.vertices.get((y * this.xCells) + x + 1);
  }

  // finds the Vertex to the bottom of the specified coordinates
  public Vertex findBottom(int x, int y) {
    return this.vertices.get(((y + 1) * this.xCells) + x);
  }

  // makeScene method for this maze
  public WorldScene makeScene() {
    WorldScene s = new WorldScene(this.width, this.height);
    s.placeImageXY(this.draw(), this.width / 2, this.height / 2);
    return s;
  }

  // visualizes every vertex and edge in the graph
  public WorldImage draw() {
    WorldImage base = new EmptyImage();
    WorldImage winText = new TextImage("YOU WIN!", 50, Color.black);
    for (int i = 0; i < this.vertices.size(); i = i + 1) {
      // offsetAlign the new Vertex image with the previous base
      // offset by multiplying Vertex coordinates by size
      base = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.TOP,
          this.vertices.get(i).drawVertex(this.edgesInTree),
          this.vertices.get(i).x * -this.cellSize, this.vertices.get(i).y * -this.cellSize, base);
    }
    if (this.curVer.equals(this.vertices.get(vertices.size() - 1))) {
      base = new OverlayImage(winText, base);
      // for (int i = 0; i < this.representatives.size(); i++) {
      // Vertex current = this.representatives.get(this.vertices.get(i));
      // if (this.representatives.get(current)
      // .equals(this.representatives.get(this.vertices.get(this.vertices.size() -
      // 1)))) {
      // this.representatives.get(current).color = Color.blue;
      // }
      // }
    }
    return base;
  }

  // onTick method
  // EFFECT: removes walls from smallest to largest weight
  public void onTick() {
    if (this.edgesInTree.size() < this.vertices.size() - 1) {
      Edge curEdge = this.worklist.get(time);
      Vertex curFrom = curEdge.from;
      Vertex curTo = curEdge.to;
      if (!this.sameReps(curFrom, curTo)) {
        this.union(this.representatives, curFrom, curTo);
        this.edgesInTree.add(curEdge);
      }
    }
    // iterates through the game as long as the maze is not complete
    if (this.curVer != this.vertices.get(vertices.size() - 1)) {
      // the bfs game mode
      if (this.gameMode.equals("b")) {
        ArrayList<Vertex> path = this.bfs(this.vertices.get(0),
            this.vertices.get(this.vertices.size() - 1));
        path.add(this.vertices.get(this.vertices.size() - 1));
        if (this.time < path.size()) {
          this.curVer = path.get(this.time);
          this.curVer.color = new Color(137, 207, 240);
          this.curVer.traced = true;
        }
      }
      // the dfs game mode
      if (this.gameMode.equals("d")) {
        ArrayList<Vertex> path = this.dfs(this.vertices.get(0),
            this.vertices.get(this.vertices.size() - 1));
        path.add(this.vertices.get(this.vertices.size() - 1));
        if (this.time < path.size()) {
          this.curVer = path.get(this.time);
          this.curVer.color = new Color(137, 207, 240);
          this.curVer.traced = true;
        }
      }
    }
    this.time++;
  }

  // the on key event method
  public void onKeyEvent(String k) {
    // makes sure the game mode is manual
    if (this.gameMode.equals("m") && this.curVer != this.vertices.get(vertices.size() - 1)) {
      // left key event
      if (k.equals("left") && curVer.left != null && this.edgesInTree.contains(curVer.left)) {
        this.curVer.color = new Color(137, 207, 240);
        curVer = curVer.left.modifyTLEdge();
      }
      // right key event
      if (k.equals("right") && curVer.right != null && this.edgesInTree.contains(curVer.right)) {
        this.curVer.color = new Color(137, 207, 240);
        curVer = curVer.right.modifyBREdge();
      }
      // up key event
      if (k.equals("up") && curVer.top != null && this.edgesInTree.contains(curVer.top)) {
        this.curVer.color = new Color(137, 207, 240);
        curVer = curVer.top.modifyTLEdge();
      }
      // down key event
      if (k.equals("down") && curVer.bottom != null && this.edgesInTree.contains(curVer.bottom)) {
        this.curVer.color = new Color(137, 207, 240);
        curVer = curVer.bottom.modifyBREdge();
      }
    }

    // resets the game to a new random board, and to the manual mode
    if (k.equals("r")) {
      this.vertices = new ArrayList<Vertex>();
      this.worklist = new ArrayList<Edge>();
      this.edgesInTree = new ArrayList<Edge>();
      this.representatives = new HashMap<Vertex, Vertex>();
      this.weights = new ArrayList<Integer>();
      this.time = 0;
      this.buildWorld();
      this.curVer = this.vertices.get(0);
      this.gameMode = "m";
    }
    // resets the game to be in manual mode
    if (k.equals("m") && !this.gameMode.equals("m")) {
      for (int i = 0; i < this.vertices.size() - 1; i++) {
        this.vertices.get(i).color = Color.gray;
        this.vertices.get(i).traced = false;
      }
      this.curVer = this.vertices.get(0);
      this.vertices.get(0).color = Color.green;
      this.vertices.get(0).traced = true;
      this.time = 0;
      this.gameMode = "m";
      this.curVer = this.vertices.get(0);
      this.vertices.get(this.vertices.size() - 1).color = Color.magenta;
      this.vertices.get(this.vertices.size() - 1).traced = false;
    }
    // changes the gamemode to dfs
    if (k.equals("d") && !this.gameMode.equals("d")) {
      for (int i = 1; i < this.vertices.size() - 1; i++) {
        this.vertices.get(i).color = Color.gray;
        this.vertices.get(i).traced = false;
      }
      this.time = 0;
      this.gameMode = "d";
      this.curVer = this.vertices.get(0);
      this.vertices.get(this.vertices.size() - 1).color = Color.magenta;
      this.vertices.get(this.vertices.size() - 1).traced = false;
    }
    // changes the gamemode to bfs
    if (k.equals("b") && !this.gameMode.equals("b")) {
      for (int i = 1; i < this.vertices.size() - 1; i++) {
        this.vertices.get(i).color = Color.gray;
        this.vertices.get(i).traced = false;
      }
      this.time = 0;
      this.gameMode = "b";
      this.curVer = this.vertices.get(0);
      this.vertices.get(this.vertices.size() - 1).color = Color.magenta;
      this.vertices.get(this.vertices.size() - 1).traced = false;
    }
  }

  // determines if the supplied vertexes have the same representatives
  public boolean sameReps(Vertex from, Vertex to) {
    return this.find(this.representatives, from).equals(this.find(this.representatives, to));
  }

  // finds the representative for the supplied vertex
  public Vertex find(HashMap<Vertex, Vertex> representatives, Vertex v) {
    if (v.equals(representatives.get(v))) {
      return v;
    }
    else {
      return this.find(representatives, representatives.get(v));
    }
  }

  // unions the from Vertex's key to the To Vertex's value
  public void union(HashMap<Vertex, Vertex> reps, Vertex from, Vertex to) {
    reps.put(this.find(reps, from), this.find(reps, to));
  }

  // the bfs method
  ArrayList<Vertex> bfs(Vertex from, Vertex to) {
    return searchHelp(from, to, new Queue<Vertex>());
  }

  // the dfs method
  ArrayList<Vertex> dfs(Vertex from, Vertex to) {
    return searchHelp(from, to, new Stack<Vertex>());
  }

  // the searchHelp method for bfs and dfs
  ArrayList<Vertex> searchHelp(Vertex from, Vertex to, ICollection<Vertex> worklist) {
    ArrayList<Vertex> alreadySeen = new ArrayList<Vertex>();

    // Initialize the worklist with the from vertex
    worklist.add(from);
    // As long as the worklist isn't empty...
    while (!worklist.isEmpty()) {
      Vertex next = worklist.remove();
      if (next.equals(to)) {
        return alreadySeen; // Success!
      }
      else if (alreadySeen.contains(next)) {
        // do nothing: we've already seen this one
      }
      else {
        // add all the neighbors of next to the worklist for further processing
        if (this.edgesInTree.contains(next.left) && next.left != null) {
          worklist.add(next.left.from);
        }
        if (this.edgesInTree.contains(next.right) && next.right != null) {
          worklist.add(next.right.to);
        }
        if (this.edgesInTree.contains(next.top) && next.top != null) {
          worklist.add(next.top.from);
        }
        if (this.edgesInTree.contains(next.bottom) && next.bottom != null) {
          worklist.add(next.bottom.to);
        }
        // add next to alreadySeen, since we're done with it
        alreadySeen.add(next);
      }
    }
    // We haven't found the to vertex, and there are no more to try
    return alreadySeen;
  }
}

// ArrayUtils Class
class ArrayUtils {
  // EFFECT: Sorts the given ArrayList according to the given comparator
  <T> void quicksort(ArrayList<T> arr, Comparator<T> comp) {
    quicksortHelp(arr, comp, 0, arr.size());
  }

  // EFFECT: sorts the source array according to comp, in the range of indices
  // [loIdx, hiIdx)
  <T> void quicksortHelp(ArrayList<T> source, Comparator<T> comp, int loIdx, int hiIdx) {
    // Step 0: check for completion
    if (loIdx >= hiIdx) {
      return; // There are no items to sort
    }
    // Step 1: select pivot
    T pivot = source.get(loIdx);
    // Step 2: partition items to lower or upper portions of the temp list
    int pivotIdx = partition(source, comp, loIdx, hiIdx, pivot);
    // Step 3: sort both halves of the list
    quicksortHelp(source, comp, loIdx, pivotIdx);
    quicksortHelp(source, comp, pivotIdx + 1, hiIdx);
  }

  // Returns the index where the pivot element ultimately ends up in the sorted
  // source
  // EFFECT: Modifies the source list in the range [loIdx, hiIdx) such that
  // all values to the left of the pivot are less than (or equal to) the pivot
  // and all values to the right of the pivot are greater than it
  <T> int partition(ArrayList<T> source, Comparator<T> comp, int loIdx, int hiIdx, T pivot) {
    int curLo = loIdx;
    int curHi = hiIdx - 1;
    while (curLo < curHi) {
      // Advance curLo until we find a too-big value (or overshoot the end of the
      // list)
      while (curLo < hiIdx && comp.compare(source.get(curLo), pivot) <= 0) {
        curLo = curLo + 1;
      }
      // Advance curHi until we find a too-small value (or undershoot the start of the
      // list)
      while (curHi >= loIdx && comp.compare(source.get(curHi), pivot) > 0) {
        curHi = curHi - 1;
      }
      if (curLo < curHi) {
        swap(source, curLo, curHi);
      }
    }
    swap(source, loIdx, curHi); // place the pivot in the remaining spot
    return curHi;
  }

  // EFFECT: swaps the two supplied indexes
  <T> void swap(ArrayList<T> source, int loIdx, int curHi) {
    T hi = source.get(curHi);
    T lo = source.get(loIdx);
    source.set(curHi, lo);
    source.set(loIdx, hi);
  }
}

// examples!
class ExamplesMaze {

  // data declaration
  Vertex vNull;
  Vertex vEdge;
  Vertex v2;
  Edge e1;
  Edge e2;
  Edge e3;
  Edge e4;
  Edge e5;
  Edge e6;
  Edge e7;
  Edge e8;
  ArrayList<Edge> el1;
  ArrayList<Edge> el1Sorted;
  WorldImage v1draw;
  WorldImage v2draw;
  WorldImage vertiEdge;
  WorldImage horiEdge;
  ArrayList<Vertex> verts1;
  ArrayList<Vertex> verts2;
  MazeWorld mw1;
  MazeWorld mw2;
  MazeWorld mw3;
  WorldImage r11;
  WorldImage r12;
  WorldImage r13;
  WorldImage rComplete;
  ArrayDeque<Integer> tDeque;

  // initData initialization;
  public void initData() {
    this.vNull = new Vertex(1, 1, 40, Color.gray, false, null, null, null, null);
    this.vEdge = new Vertex(1, 1, 40, Color.gray, false, e1, null, null, null);
    this.v2 = new Vertex(2, 2, 40, Color.gray, false, e1, null, null, null);
    this.e1 = new Edge(vEdge, vNull, 1);
    this.e2 = new Edge(vEdge, vNull, 2);
    this.e3 = new Edge(vEdge, vNull, 3);
    this.e4 = new Edge(vEdge, vNull, 4);
    this.e5 = new Edge(vEdge, vNull, 5);
    this.e6 = new Edge(vEdge, vNull, 6);
    this.e7 = new Edge(vEdge, vNull, 7);
    this.e8 = new Edge(vEdge, vNull, 8);
    this.el1 = new ArrayList<Edge>(
        Arrays.asList(this.e2, this.e3, this.e1, this.e5, this.e8, this.e7, this.e6, this.e4));
    this.el1Sorted = new ArrayList<Edge>(
        Arrays.asList(this.e1, this.e2, this.e3, this.e4, this.e5, this.e6, this.e7, this.e8));
    this.vertiEdge = new LineImage(new Posn(0, 40), Color.black);
    this.horiEdge = new LineImage(new Posn(40, 0), Color.black);
    this.v1draw = new RectangleImage(40, 40, OutlineMode.SOLID, Color.gray);
    this.v2draw = new RectangleImage(200, 200, OutlineMode.SOLID, Color.gray);
    this.verts1 = new ArrayList<Vertex>(
        Arrays.asList(this.vNull, this.vNull, this.vNull, this.vNull, this.vNull, this.vNull,
            this.vNull, this.vNull, this.vNull, this.vNull, this.vNull, this.vNull));
    this.verts2 = new ArrayList<Vertex>(
        Arrays.asList(this.vNull, this.vNull, this.vNull, this.vNull));
    this.mw1 = new MazeWorld(4);
    this.mw2 = new MazeWorld(verts1, 4);
    this.mw3 = new MazeWorld(verts2, 2);

    this.r11 = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.TOP, this.v1draw, -400, -400,
        new EmptyImage());
    this.r12 = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.TOP, this.v1draw, -400, -400,
        this.r11);
    this.r13 = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.TOP, this.v1draw, -400, -400,
        this.r12);
    this.rComplete = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.TOP, this.v1draw, -400,
        -400, this.r13);
    this.tDeque = new ArrayDeque<Integer>();
  }

  // tests the DrawVertex method
  public void testDrawVertex(Tester t) {
    this.initData();
    t.checkExpect(this.vNull.drawVertex(new ArrayList<Edge>()), this.v1draw);
    t.checkExpect(this.vEdge.drawVertex(new ArrayList<Edge>(Arrays.asList(this.e1, this.e2))),
        new BesideImage(vertiEdge, this.v1draw));
  }

  // tests the compareTo method
  public void testCompareTo(Tester t) {
    this.initData();
    t.checkExpect(this.e1.compareTo(this.e2), -1);
    t.checkExpect(this.e2.compareTo(this.e1), 1);
    t.checkExpect(this.e2.compareTo(this.e2), 0);
  }

  // tests the compare method
  public void testCompare(Tester t) {
    this.initData();
    t.checkExpect(new EdgeComparator().compare(this.e1, this.e2), -1);
    t.checkExpect(new EdgeComparator().compare(this.e2, this.e1), 1);
    t.checkExpect(new EdgeComparator().compare(this.e2, this.e2), 0);
  }

  // tests findLeft method
  boolean testFindLeft(Tester t) {
    this.initData();
    this.mw1.buildWorld();
    return t.checkExpect(this.mw1.findLeft(1, 0), this.mw1.vertices.get(0))
        && t.checkExpect(this.mw1.findLeft(2, 0), this.mw1.vertices.get(1))
        && t.checkExpect(this.mw1.findLeft(1, 1), this.mw1.vertices.get(4))
        && t.checkExpect(this.mw1.findLeft(2, 1), this.mw1.vertices.get(5));
  }

  // tests findTop method
  boolean testFindTop(Tester t) {
    this.initData();
    this.mw1.buildWorld();
    return t.checkExpect(this.mw1.findTop(2, 2), this.mw1.vertices.get(6))
        && t.checkExpect(this.mw1.findTop(1, 2), this.mw1.vertices.get(5))
        && t.checkExpect(this.mw1.findTop(2, 1), this.mw1.vertices.get(2))
        && t.checkExpect(this.mw1.findTop(0, 1), this.mw1.vertices.get(0));
  }

  // tests findRight method
  boolean testFindRight(Tester t) {
    this.initData();
    this.mw1.buildWorld();
    return t.checkExpect(this.mw1.findRight(0, 0), this.mw1.vertices.get(1))
        && t.checkExpect(this.mw1.findRight(1, 2), this.mw1.vertices.get(10))
        && t.checkExpect(this.mw1.findRight(0, 2), this.mw1.vertices.get(9))
        && t.checkExpect(this.mw1.findRight(1, 1), this.mw1.vertices.get(6));
  }

  // tests findBottom method
  boolean testFindBottom(Tester t) {
    this.initData();
    this.mw1.buildWorld();
    return t.checkExpect(this.mw1.findBottom(0, 0), this.mw1.vertices.get(4))
        && t.checkExpect(this.mw1.findBottom(0, 1), this.mw1.vertices.get(8))
        && t.checkExpect(this.mw1.findBottom(1, 0), this.mw1.vertices.get(5))
        && t.checkExpect(this.mw1.findBottom(2, 1), this.mw1.vertices.get(10));
  }

  // tests the buildWorld method
  void testBuildWorld(Tester t) {
    this.initData();
    this.mw1.buildWorld();
    t.checkExpect(this.mw1.vertices.size(), 12);
    t.checkExpect(this.mw1.vertices.get(0).color, Color.green);
    t.checkExpect(this.mw1.vertices.get(11).color, Color.magenta);
    t.checkExpect(this.mw1.vertices.get(5).color, Color.gray);
    t.checkExpect(this.mw1.worklist.size(), 17);
    t.checkExpect(this.mw1.vertices.get(0).top, null);
    t.checkExpect(this.mw1.vertices.get(0).left, null);
    t.checkExpect(this.mw1.vertices.get(0).right.from, this.mw1.vertices.get(0));
    t.checkExpect(this.mw1.vertices.get(0).right.to, this.mw1.vertices.get(1));
    t.checkExpect(this.mw1.vertices.get(0).bottom.from, this.mw1.vertices.get(0));
    t.checkExpect(this.mw1.vertices.get(0).bottom.to, this.mw1.vertices.get(4));
    t.checkExpect(this.mw1.vertices.get(5).left.from, this.mw1.vertices.get(4));
    t.checkExpect(this.mw1.vertices.get(5).left.to, this.mw1.vertices.get(5));
    t.checkExpect(this.mw1.vertices.get(5).top.from, this.mw1.vertices.get(1));
    t.checkExpect(this.mw1.vertices.get(5).top.to, this.mw1.vertices.get(5));
  }

  // bigBang method!
  public void testBigBang(Tester t) {
    MazeWorld m1 = new MazeWorld(8).buildWorld();
    double tickRate = 0.01;
    m1.bigBang(m1.width, m1.height, tickRate);
  }

  // tests the quickSort method
  public void testQuickSort(Tester t) {
    this.initData();
    this.mw1.buildWorld();
    t.checkExpect(this.mw1.worklist.get(0).weight < this.mw1.worklist.get(1).weight, true);
    t.checkExpect(this.mw1.worklist.get(5).weight < this.mw1.worklist.get(10).weight, true);
    t.checkExpect(this.mw1.worklist.get(10).weight < this.mw1.worklist.get(5).weight, false);
    t.checkExpect(this.mw1.worklist.get(11).weight > this.mw1.worklist.get(1).weight, true);
  }

  // tests the Equals override for Vertices
  public boolean testEquals(Tester t) {
    return t.checkExpect(vNull.equals(vNull), true) && t.checkExpect(vNull.equals(vEdge), false)
        && t.checkExpect(vNull.equals(v2), false);
  }

  // tests the onTick method;
  public void testOntick(Tester t) {
    this.initData();
    this.mw1.buildWorld();
    t.checkExpect(this.mw1.time >= 0, true);
    t.checkExpect(this.mw1.edgesInTree.size() >= 0, true);
  }

  public void testSameReps(Tester t) {
    this.initData();
    this.mw1.buildWorld();
    t.checkExpect(this.mw1.sameReps(this.mw1.representatives.get(mw1.vertices.get(1)),
        this.mw1.representatives.get(mw1.vertices.get(1))), true);
    t.checkExpect(this.mw1.sameReps(this.mw1.representatives.get(mw1.vertices.get(1)),
        this.mw1.representatives.get(mw1.vertices.get(6))), false);
    t.checkExpect(this.mw1.sameReps(this.mw1.representatives.get(mw1.vertices.get(2)),
        this.mw1.representatives.get(mw1.vertices.get(6))), false);
    t.checkExpect(this.mw1.sameReps(this.mw1.representatives.get(mw1.vertices.get(3)),
        this.mw1.representatives.get(mw1.vertices.get(2))), false);

    this.mw1.union(this.mw1.representatives, this.mw1.representatives.get(mw1.vertices.get(1)),
        this.mw1.representatives.get(mw1.vertices.get(6)));
    t.checkExpect(this.mw1.sameReps(this.mw1.representatives.get(mw1.vertices.get(1)),
        this.mw1.representatives.get(mw1.vertices.get(6))), true);
    this.mw1.union(this.mw1.representatives, this.mw1.representatives.get(mw1.vertices.get(2)),
        this.mw1.representatives.get(mw1.vertices.get(1)));
    t.checkExpect(this.mw1.sameReps(this.mw1.representatives.get(mw1.vertices.get(2)),
        this.mw1.representatives.get(mw1.vertices.get(6))), true);
    t.checkExpect(this.mw1.sameReps(this.mw1.representatives.get(mw1.vertices.get(3)),
        this.mw1.representatives.get(mw1.vertices.get(2))), false);
    this.mw1.union(this.mw1.representatives, this.mw1.representatives.get(mw1.vertices.get(3)),
        this.mw1.representatives.get(mw1.vertices.get(6)));
    t.checkExpect(this.mw1.sameReps(this.mw1.representatives.get(mw1.vertices.get(3)),
        this.mw1.representatives.get(mw1.vertices.get(6))), true);
    t.checkExpect(this.mw1.sameReps(this.mw1.representatives.get(mw1.vertices.get(3)),
        this.mw1.representatives.get(mw1.vertices.get(2))), true);
  }

  // tests the find method
  public void testFind(Tester t) {
    this.initData();
    this.mw1.buildWorld();
    t.checkExpect(this.mw1.find(this.mw1.representatives, this.mw1.vertices.get(1)),
        this.mw1.vertices.get(1));
    this.mw1.union(this.mw1.representatives, this.mw1.vertices.get(1), this.mw1.vertices.get(6));
    t.checkExpect(this.mw1.find(this.mw1.representatives, this.mw1.vertices.get(1)),
        this.mw1.vertices.get(6));
    this.mw1.union(this.mw1.representatives, this.mw1.vertices.get(2), this.mw1.vertices.get(1));
    t.checkExpect(this.mw1.find(this.mw1.representatives, this.mw1.vertices.get(2)),
        this.mw1.vertices.get(6));
    this.mw1.union(this.mw1.representatives, this.mw1.vertices.get(3), this.mw1.vertices.get(2));
    t.checkExpect(this.mw1.find(this.mw1.representatives, this.mw1.vertices.get(3)),
        this.mw1.vertices.get(6));

  }

  // tests the union method
  public void testUnion(Tester t) {
    this.initData();
    this.mw1.buildWorld();
    t.checkExpect(this.mw1.representatives.get(mw1.vertices.get(1)),
        this.mw1.representatives.get(mw1.vertices.get(1)));
    this.mw1.union(mw1.representatives, mw1.vertices.get(1), mw1.vertices.get(2));
    t.checkExpect(this.mw1.representatives.get(mw1.vertices.get(1)),
        this.mw1.representatives.get(mw1.vertices.get(2)));
    this.mw1.union(mw1.representatives, mw1.vertices.get(6), mw1.vertices.get(1));
    t.checkExpect(this.mw1.representatives.get(mw1.vertices.get(6)),
        this.mw1.representatives.get(mw1.vertices.get(2)));
    this.mw1.union(mw1.representatives, mw1.vertices.get(3), mw1.vertices.get(6));
    t.checkExpect(this.mw1.representatives.get(mw1.vertices.get(3)),
        this.mw1.representatives.get(mw1.vertices.get(2)));
  }

  // tests the sort method
  public void testSort(Tester t) {
    this.initData();
    this.mw1.buildWorld();
    t.checkExpect(this.el1, new ArrayList<Edge>(
        Arrays.asList(this.e2, this.e3, this.e1, this.e5, this.e8, this.e7, this.e6, this.e4)));
    new ArrayUtils().quicksort(this.el1, new EdgeComparator());
    t.checkExpect(this.el1, el1Sorted);
  }

  // tests the sortHelp method
  public void testSortHelp(Tester t) {
    this.initData();
    this.mw1.buildWorld();
    t.checkExpect(this.el1, new ArrayList<Edge>(
        Arrays.asList(this.e2, this.e3, this.e1, this.e5, this.e8, this.e7, this.e6, this.e4)));
    new ArrayUtils().quicksortHelp(el1, new EdgeComparator(), 0, 8);
    t.checkExpect(this.el1, el1Sorted);
  }

  // tests the partition method
  // public void testPartition(Tester t) {
  // this.initData();
  // new ArrayUtils().partition(this.el1, new EdgeComparator(), 0, 8, this.e4);
  // t.checkExpect(this.el1, new ArrayList<Edge>(
  // Arrays.asList(this.e2, this.e3, this.e1, this.e5, this.e8, this.e7, this.e6,
  // this.e4)));
  // new ArrayUtils().partition(this.el1, new EdgeComparator(), 4, 8, this.e4);
  // t.checkExpect(this.el1, new ArrayList<Edge>(
  // Arrays.asList(this.e2, this.e3, this.e1, this.e5, this.e8, this.e7, this.e6,
  // this.e4)));
  // }

  // tests the swap method
  public void testSwap(Tester t) {
    this.initData();
    t.checkExpect(this.el1, new ArrayList<Edge>(
        Arrays.asList(this.e2, this.e3, this.e1, this.e5, this.e8, this.e7, this.e6, this.e4)));
    new ArrayUtils().swap(this.el1, 0, 2);
    t.checkExpect(this.el1, new ArrayList<Edge>(
        Arrays.asList(this.e1, this.e3, this.e2, this.e5, this.e8, this.e7, this.e6, this.e4)));
    new ArrayUtils().swap(this.el1, 1, 2);
    t.checkExpect(this.el1, new ArrayList<Edge>(
        Arrays.asList(this.e1, this.e2, this.e3, this.e5, this.e8, this.e7, this.e6, this.e4)));
  }

  // tests the draw method
  public void testDraw(Tester t) {
    this.initData();
    t.checkExpect(this.mw3.draw(), this.rComplete);
  }

  // tests makeScene method
  boolean testMakeScene(Tester t) {
    this.initData();
    int width = 800;
    int height = 600;
    WorldScene s = new WorldScene(width, height);
    s.placeImageXY(this.rComplete, width / 2, height / 2);
    return t.checkExpect(this.mw3.makeScene(), s);
  }

  // tests the onKey method
  void testOnKey(Tester t) {
    this.initData();
    t.checkExpect(this.mw1.gameMode, "m");
    this.mw1.onKeyEvent("b");
    t.checkExpect(this.mw1.gameMode, "b");
    this.mw1.onKeyEvent("d");
    t.checkExpect(this.mw1.gameMode, "d");
  }

  // tests the Add method
  void testAdd(Tester t) {
    this.initData();
    this.tDeque.add(1);
    this.tDeque.add(2);
    this.tDeque.add(3);
    t.checkExpect(this.tDeque.contains(1), true);
    t.checkExpect(this.tDeque.contains(3), true);
    t.checkExpect(this.tDeque.contains(5), false);
  }

  // tests the remove method
  void testRemove(Tester t) {
    this.initData();
    this.tDeque.add(1);
    this.tDeque.add(2);
    this.tDeque.add(3);
    t.checkExpect(this.tDeque.contains(1), true);
    t.checkExpect(this.tDeque.contains(3), true);
    t.checkExpect(this.tDeque.contains(5), false);
    t.checkExpect(this.tDeque.remove(2), true);
    t.checkExpect(this.tDeque.contains(2), false);
    t.checkExpect(this.tDeque.contains(1), true);
    t.checkExpect(this.tDeque.remove(1), true);
    t.checkExpect(this.tDeque.contains(1), false);
  }

  // tests the isEmpty method
  void testEmpty(Tester t) {
    this.initData();
    t.checkExpect(this.tDeque.isEmpty(), true);
    this.tDeque.add(1);
    this.tDeque.add(2);
    this.tDeque.add(3);
    t.checkExpect(this.tDeque.isEmpty(), false);
    this.tDeque.remove(1);
    this.tDeque.remove(2);
    this.tDeque.remove(3);
    t.checkExpect(this.tDeque.isEmpty(), true);
  }

  // tests the bfs method
  void testBFS(Tester t) {
    this.initData();
    this.mw1.buildWorld();
    t.checkExpect(
        this.mw1.bfs(this.mw1.vertices.get(0), this.mw1.vertices.get(this.mw1.vertices.size() - 1))
            .size() > 0,
        true);
  }

  // tests the dfs method
  void testDFS(Tester t) {
    this.initData();
    this.mw1.buildWorld();
    t.checkExpect(
        this.mw1.dfs(this.mw1.vertices.get(0), this.mw1.vertices.get(this.mw1.vertices.size() - 1))
            .size() > 0,
        true);
  }

  // tests the searchHelp method
  void testSearchHelp(Tester t) {
    this.initData();
    this.mw1.buildWorld();
    t.checkExpect(
        this.mw1
            .searchHelp(this.mw1.vertices.get(0),
                this.mw1.vertices.get(this.mw1.vertices.size() - 1), new Queue<Vertex>())
            .size() > 0,
        true);
    t.checkExpect(
        this.mw1.searchHelp(this.mw1.vertices.get(0),
            this.mw1.vertices.get(this.mw1.vertices.size() - 1), new Queue<Vertex>()).get(0),
        this.mw1.vertices.get(0));
    t.checkExpect(
        this.mw1.searchHelp(this.mw1.vertices.get(0),
            this.mw1.vertices.get(this.mw1.vertices.size() - 1), new Stack<Vertex>()).get(0),
        this.mw1.vertices.get(0));
  }
}
