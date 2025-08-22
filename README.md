# Sudoku em Java (Console)

Um jogo de Sudoku implementado em **Java** utilizando Programação Orientada a Objetos (POO), manipulação de estruturas de dados e interação via terminal.

## 🎮 Como jogar

### 1. Compilação
```bash
javac Sudoku.java
```

### 2. Execução
```bash
java Sudoku
```

Ao iniciar, um tabuleiro é gerado automaticamente na dificuldade **média**. O jogo será exibido no terminal com coordenadas para facilitar a escolha das posições.

Exemplo de tabuleiro:
```
    1 2 3   4 5 6   7 8 9
  +-------+-------+-------+
1 | . . . | . . . | . . . |
2 | . . . | . . . | . . . |
3 | . . . | . . . | . . . |
  |       |       |       |
4 | . . . | . . . | . . . |
...
  +-------+-------+-------+
Legenda: '.' = vazio, números fixos = não editáveis
```

## ⌨️ Comandos disponíveis

- `show` → mostra o tabuleiro atual
- `set r c v` → define valor (1–9) na **linha** `r` e **coluna** `c`  
  Exemplo: `set 1 3 9`
- `clear r c` → limpa a célula (se não for fixa)
- `hint` → mostra uma dica segura (ou revela um valor possível)
- `solve` → resolve automaticamente o Sudoku
- `reset` → reinicia o tabuleiro para o estado inicial
- `new easy|medium|hard` → gera um novo jogo na dificuldade escolhida
- `help` → mostra a lista de comandos
- `exit` → encerra o jogo

## 🔑 Funcionalidades implementadas
- **Gerador** de tabuleiro com solução única
- **Solver** baseado em backtracking
- **Validação** de jogadas (linha, coluna, caixa 3x3)
- **Dicas** para auxiliar o jogador
- **Reset e novo jogo** com escolha de dificuldade

## 📂 Estrutura do código
- `SudokuBoard` → modelo de dados
- `BoardRenderer` → renderização no terminal
- `SudokuSolver` → resolvedor por backtracking
- `SudokuGenerator` → geração de tabuleiros com solução única
- `SudokuHints` → sistema de dicas
- `Game` → loop principal de interação

---

👨‍💻 Desenvolvido para fins de estudo de **POO em Java** e prática de **lógica de programação** com jogos no terminal.

