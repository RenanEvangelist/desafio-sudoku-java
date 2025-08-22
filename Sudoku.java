// File: Sudoku.java
// Console Sudoku game in Java using OOP. Single-file project for easy compilation.
// Compile: javac Sudoku.java
// Run:     java Sudoku
//
// Features:
// - Generate Sudoku (easy/medium/hard)
// - Validate moves (can't modify givens, respects row/col/box constraints)
// - Commands: set, clear, hint, solve, new <difficulty>, show, reset, help, exit
// - Unique-solution puzzle generation
// - Simple, readable board rendering with coordinates
//
// Author: ChatGPT for Renan Moura Evangelista
// Date: 2025-08-22

import java.util.*;

public class Sudoku {
    public static void main(String[] args) {
        new Game().start();
    }
}

// === Game Loop & I/O ===
class Game {
    private SudokuBoard current;
    private SudokuBoard original; // to allow reset

    void start() {
        println("\n=== SUDOKU (Java, console) ===");
        println("Gerando um tabuleiro inicial (dificuldade: medium)...\n");
        SudokuBoard board = SudokuGenerator.generate(Difficulty.MEDIUM, new Random());
        this.current = board.copy();
        this.original = board.copy();
        render();
        help();
        loop();
    }

    private void loop() {
        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.print("\n> ");
            String line = sc.nextLine().trim();
            if (line.isEmpty()) continue;
            String[] parts = line.split("\\s+");
            String cmd = parts[0].toLowerCase();

            try {
                switch (cmd) {
                    case "set": // set r c v
                        requireArgs(parts, 4, "Uso: set <linha 1-9> <coluna 1-9> <valor 1-9>");
                        int r = parseIndex(parts[1]);
                        int c = parseIndex(parts[2]);
                        int v = parseValue(parts[3]);
                        if (original.isGiven(r, c)) { println("Não é possível alterar uma célula fixa (givens)."); break; }
                        if (!current.isValidMove(r, c, v)) { println("Movimento inválido: viola linha/coluna/box."); break; }
                        current.set(r, c, v);
                        render();
                        if (current.isComplete()) {
                            println("\n🎉 Parabéns! Você completou o Sudoku!");
                            return;
                        }
                        break;
                    case "clear": // clear r c
                        requireArgs(parts, 3, "Uso: clear <linha 1-9> <coluna 1-9>");
                        r = parseIndex(parts[1]);
                        c = parseIndex(parts[2]);
                        if (original.isGiven(r, c)) { println("Não é possível limpar uma célula fixa (givens)."); break; }
                        current.clear(r, c);
                        render();
                        break;
                    case "show":
                        render();
                        break;
                    case "hint":
                        Optional<CellValue> hint = SudokuHints.oneHint(current);
                        if (hint.isPresent()) {
                            CellValue h = hint.get();
                            println("Sugestão: linha " + (h.row+1) + ", coluna " + (h.col+1) + " = " + h.value);
                        } else {
                            println("Sem dica disponível (talvez o tabuleiro esteja completo ou inconsistente).");
                        }
                        break;
                    case "solve":
                        SudokuBoard solved = current.copy();
                        if (SudokuSolver.solve(solved)) {
                            current = solved;
                            render();
                            println("Tabuleiro resolvido pelo solver.");
                        } else {
                            println("Este tabuleiro não tem solução.");
                        }
                        break;
                    case "reset":
                        current = original.copy();
                        render();
                        break;
                    case "new": // new [easy|medium|hard]
                        Difficulty diff = Difficulty.MEDIUM;
                        if (parts.length >= 2) {
                            diff = Difficulty.fromString(parts[1]);
                        }
                        println("Gerando novo Sudoku (" + diff.name().toLowerCase() + ")...");
                        SudokuBoard nb = SudokuGenerator.generate(diff, new Random());
                        current = nb.copy();
                        original = nb.copy();
                        render();
                        break;
                    case "help":
                        help();
                        break;
                    case "exit":
                    case "quit":
                        println("Até mais! 👋");
                        return;
                    default:
                        println("Comando desconhecido. Digite 'help' para ver os comandos.");
                }
            } catch (IllegalArgumentException ex) {
                println(ex.getMessage());
            }
        }
    }

    private void render() {
        println(BoardRenderer.render(current, original));
    }

    private void help() {
        println("Comandos:\n" +
               "  show                 → mostra o tabuleiro\n" +
               "  set r c v            → define valor (1-9) na linha r e coluna c\n" +
               "  clear r c            → limpa célula (se não for fixa)\n" +
               "  hint                 → mostra uma sugestão segura (se houver)\n" +
               "  solve                → resolve automaticamente\n" +
               "  reset                → volta ao estado inicial do tabuleiro\n" +
               "  new [easy|medium|hard] → gera novo jogo\n" +
               "  help                 → mostra esta ajuda\n" +
               "  exit                 → sair");
    }

    private static void requireArgs(String[] parts, int n, String msg) {
        if (parts.length < n) throw new IllegalArgumentException(msg);
    }

    private static int parseIndex(String s) {
        try {
            int x = Integer.parseInt(s);
            if (x < 1 || x > 9) throw new NumberFormatException();
            return x - 1;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Índice inválido: use números 1..9.");
        }
    }

    private static int parseValue(String s) {
        try {
            int v = Integer.parseInt(s);
            if (v < 1 || v > 9) throw new NumberFormatException();
            return v;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Valor inválido: use 1..9.");
        }
    }

    private static void println(String s) { System.out.println(s); }
}

// === Difficulty ===
enum Difficulty {
    EASY(40, 46), MEDIUM(32, 39), HARD(24, 31);

    final int minGivens; // approximate range for given cells
    final int maxGivens;

    Difficulty(int minGivens, int maxGivens) {
        this.minGivens = minGivens;
        this.maxGivens = maxGivens;
    }

    static Difficulty fromString(String s) {
        switch (s.toLowerCase()) {
            case "easy": return EASY;
            case "hard": return HARD;
            default: return MEDIUM;
        }
    }
}

// === Data Model ===
class SudokuBoard {
    private final int[][] grid; // 0 means empty
    private final boolean[][] given; // true if cell is fixed

    SudokuBoard() {
        this.grid = new int[9][9];
        this.given = new boolean[9][9];
    }

    SudokuBoard(int[][] grid, boolean[][] given) {
        this.grid = new int[9][9];
        this.given = new boolean[9][9];
        for (int r = 0; r < 9; r++) {
            System.arraycopy(grid[r], 0, this.grid[r], 0, 9);
            System.arraycopy(given[r], 0, this.given[r], 0, 9);
        }
    }

    SudokuBoard copy() { return new SudokuBoard(this.grid, this.given); }

    int get(int r, int c) { return grid[r][c]; }
    void set(int r, int c, int v) { grid[r][c] = v; }
    void clear(int r, int c) { grid[r][c] = 0; }
    boolean isEmpty(int r, int c) { return grid[r][c] == 0; }

    boolean isGiven(int r, int c) { return given[r][c]; }
    void setGiven(int r, int c, boolean g) { given[r][c] = g; }

    boolean isValidMove(int r, int c, int v) {
        if (v < 1 || v > 9) return false;
        // row
        for (int j = 0; j < 9; j++) if (grid[r][j] == v && j != c) return false;
        // col
        for (int i = 0; i < 9; i++) if (grid[i][c] == v && i != r) return false;
        // box
        int br = (r/3)*3, bc = (c/3)*3;
        for (int i = br; i < br+3; i++)
            for (int j = bc; j < bc+3; j++)
                if (grid[i][j] == v && (i != r || j != c)) return false;
        return true;
    }

    boolean isComplete() {
        for (int r = 0; r < 9; r++)
            for (int c = 0; c < 9; c++)
                if (grid[r][c] == 0) return false;
        return true;
    }

    int[][] raw() { return grid; }
}

// === Rendering ===
class BoardRenderer {
    static String render(SudokuBoard current, SudokuBoard original) {
        StringBuilder sb = new StringBuilder();
        sb.append("    1 2 3   4 5 6   7 8 9\n");
        sb.append("  +-------+-------+-------+\n");
        for (int r = 0; r < 9; r++) {
            sb.append((r+1) + " | ");
            for (int c = 0; c < 9; c++) {
                int v = current.get(r, c);
                boolean isGiven = original.isGiven(r, c);
                String cell = v == 0 ? "." : Integer.toString(v);
                if (isGiven) cell = cell; // could style differently if terminal supports
                sb.append(cell);
                if (c == 2 || c == 5) sb.append(" ");
                sb.append(c == 8 ? " |" : " ");
            }
            sb.append("\n");
            if (r == 2 || r == 5) sb.append("  |       |       |       |\n");
        }
        sb.append("  +-------+-------+-------+\n");
        sb.append("Legenda: '.' = vazio, números fixos = não editáveis\n");
        return sb.toString();
    }
}

// === Solver (backtracking) ===
class SudokuSolver {
    static boolean solve(SudokuBoard board) {
        int[] pos = findEmpty(board);
        if (pos == null) return true; // solved
        int r = pos[0], c = pos[1];
        for (int v = 1; v <= 9; v++) {
            if (board.isValidMove(r, c, v)) {
                board.set(r, c, v);
                if (solve(board)) return true;
                board.clear(r, c);
            }
        }
        return false;
    }

    static int countSolutions(SudokuBoard board, int limit) {
        // Backtracking with early cutoff after 'limit' solutions
        return count(board, limit, new int[]{0});
    }

    private static int count(SudokuBoard b, int limit, int[] found) {
        if (found[0] >= limit) return found[0];
        int[] pos = findEmpty(b);
        if (pos == null) { found[0]++; return found[0]; }
        int r = pos[0], c = pos[1];
        for (int v = 1; v <= 9; v++) {
            if (b.isValidMove(r, c, v)) {
                b.set(r, c, v);
                count(b, limit, found);
                if (found[0] >= limit) return found[0];
                b.clear(r, c);
            }
        }
        return found[0];
    }

    private static int[] findEmpty(SudokuBoard board) {
        for (int r = 0; r < 9; r++)
            for (int c = 0; c < 9; c++)
                if (board.get(r, c) == 0) return new int[]{r, c};
        return null;
    }
}

// === Generator ===
class SudokuGenerator {
    static SudokuBoard generate(Difficulty difficulty, Random rng) {
        // 1) Create a full valid board by shuffling a base pattern
        SudokuBoard full = generateFull(rng);
        // 2) Carve holes while keeping unique solution and target given count
        SudokuBoard puzzle = carve(full, difficulty, rng);
        return puzzle;
    }

    private static SudokuBoard generateFull(Random rng) {
        SudokuBoard b = new SudokuBoard();
        // Fill using randomized backtracking for variety
        fill(b, rng);
        // Mark all as given initially
        for (int r = 0; r < 9; r++) for (int c = 0; c < 9; c++) b.setGiven(r, c, true);
        return b;
    }

    private static boolean fill(SudokuBoard b, Random rng) {
        int[] pos = findEmpty(b);
        if (pos == null) return true;
        int r = pos[0], c = pos[1];
        List<Integer> nums = new ArrayList<>();
        for (int i = 1; i <= 9; i++) nums.add(i);
        Collections.shuffle(nums, rng);
        for (int v : nums) {
            if (b.isValidMove(r, c, v)) {
                b.set(r, c, v);
                if (fill(b, rng)) return true;
                b.clear(r, c);
            }
        }
        return false;
    }

    private static SudokuBoard carve(SudokuBoard full, Difficulty diff, Random rng) {
        SudokuBoard puzzle = full.copy();
        int targetGivens = randBetween(rng, diff.minGivens, diff.maxGivens);

        // Start with all cells given=true, then attempt to remove pairs symmetrically
        List<int[]> cells = new ArrayList<>();
        for (int r = 0; r < 9; r++) for (int c = 0; c < 9; c++) cells.add(new int[]{r,c});
        Collections.shuffle(cells, rng);

        int givens = 81;
        for (int[] rc : cells) {
            if (givens <= targetGivens) break;
            int r = rc[0], c = rc[1];
            int r2 = 8 - r, c2 = 8 - c; // symmetric pair

            if (!puzzle.isGiven(r, c)) continue; // already removed
            int v1 = puzzle.get(r, c);
            int v2 = puzzle.get(r2, c2);

            // Try remove both
            puzzle.set(r, c, 0); puzzle.setGiven(r, c, false);
            if (r != r2 || c != c2) { puzzle.set(r2, c2, 0); puzzle.setGiven(r2, c2, false); }

            // Check uniqueness
            SudokuBoard tmp = puzzle.copy();
            int solutions = SudokuSolver.countSolutions(tmp, 2);
            if (solutions != 1) {
                // Revert removal
                puzzle.set(r, c, v1); puzzle.setGiven(r, c, true);
                if (r != r2 || c != c2) { puzzle.set(r2, c2, v2); puzzle.setGiven(r2, c2, true); }
            } else {
                givens -= (r == r2 && c == c2) ? 1 : 2;
            }
        }

        // Ensure remaining givens are flagged
        for (int r = 0; r < 9; r++)
            for (int c = 0; c < 9; c++)
                if (puzzle.get(r, c) != 0) puzzle.setGiven(r, c, true);
        return puzzle;
    }

    private static int[] findEmpty(SudokuBoard board) {
        for (int r = 0; r < 9; r++)
            for (int c = 0; c < 9; c++)
                if (board.get(r, c) == 0) return new int[]{r, c};
        return null;
    }

    private static int randBetween(Random rng, int a, int b) {
        if (a > b) { int t = a; a = b; b = t; }
        return a + rng.nextInt(b - a + 1);
    }
}

// === Hints ===
class SudokuHints {
    static Optional<CellValue> oneHint(SudokuBoard board) {
        // Simple single-candidate hint: if a cell has exactly one valid value, return it.
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                if (board.isEmpty(r, c)) {
                    List<Integer> candidates = candidates(board, r, c);
                    if (candidates.size() == 1) {
                        return Optional.of(new CellValue(r, c, candidates.get(0)));
                    }
                }
            }
        }
        // Fallback: compute solved board and reveal one differing cell as a hint
        SudokuBoard solved = board.copy();
        if (SudokuSolver.solve(solved)) {
            for (int r = 0; r < 9; r++)
                for (int c = 0; c < 9; c++)
                    if (board.get(r, c) == 0)
                        return Optional.of(new CellValue(r, c, solved.get(r, c)));
        }
        return Optional.empty();
    }

    static List<Integer> candidates(SudokuBoard board, int r, int c) {
        List<Integer> vals = new ArrayList<>();
        for (int v = 1; v <= 9; v++) if (board.isValidMove(r, c, v)) vals.add(v);
        return vals;
    }
}

class CellValue {
    final int row, col, value;
    CellValue(int r, int c, int v) { this.row = r; this.col = c; this.value = v; }
}
