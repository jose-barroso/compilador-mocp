# Compilador MOCP2JAVA

Implementação de um compilador para a linguagem MOCP, uma variante da linguagem C com sintaxe em português, que traduz um programa escrito em `.mocp` para código Java funcional e para código intermédio otimizado (Three-address code).

## Arquitetura
O compilador segue um pipeline estruturado e modular:
1. **Frontend:** Análise Léxica (Lexer) e Sintática (Parser) com construção da Árvore Sintática Abstrata (AST) através de uma gramática desenvolvida para ANTLR4.
2. **Análise Semântica:** Validação estrita de tipos, verificação dinâmica de escopos (escopos aninhados) e consistência total de declarações e assinaturas.
3. **Middle-end (TAC):** Geração de Código de Três Endereços (Three-Address Code) achatado e linearizado.
4. **Otimizador de TAC:** Aplicação iterativa de técnicas de otimização em código intermédio (*Constant Folding*, *Constant Propagation* e *Dead Code Elimination*) até atingir um estado de estabilidade (ponto fixo).
5. **Backend (Transpiler):** Geração de código Java estruturado (`Main.java`) a partir da AST validada, aplicando técnicas modernas de transpilação e empacotamento de dependências.

## Requisitos
- Java JDK 17 ou superior
- ANTLR 4.13.2 <- Pode ser colocado no mesmo diretório do MakeFile
- GNU Make

## Como correr o programa

### Passo 1: Compilação do Compilador
Na raiz do projeto, execute o comando abaixo para gerar os ficheiros do ANTLR e compilar as classes Java para a diretoria `build/`:
```
make
```
*Este passo cria as pastas `build/` e `src/generated/`, compilando toda a infraestrutura necessária.*

### Passo 2: Executar sobre um ficheiro MOCP
Para correr o compilador sobre um ficheiro de teste `.mocp`, utilize a regra `run` especificando o caminho do ficheiro em `FILE`:
```
make run FILE=src/test/teste_completo.mocp
```
### Limpar os ficheiros gerados
Para remover todas as diretorias temporárias, ficheiros gerados pelo ANTLR e binários compilados:
```
make clean
```
## Resultado
O programa lê o ficheiro `.mocp`, imprime a representação textual da Árvore Sintática Abstrata (AST) no terminal e realiza uma análise semântica rigorosa.

* **Se forem detetados erros** (léxicos, sintáticos ou semânticos), o compilador imprime as respetivas mensagens de erro detalhadas com indicação de linha/coluna no terminal e **interrompe o processo imediatamente**, garantindo que não é gerado código intermédio ou final inválido.
* **Se não houver erros**, o compilador prossegue para as fases seguintes:
    * **Geração de Código Intermédio (TAC):** Traduz a AST validada para uma lista linear de instruções de três endereços, estruturada com rótulos (*labels*) e variáveis temporárias.
    * **Otimização:** Aplica sucessivamente os módulos de otimização até atingir um ponto fixo. O TAC otimizado e limpo é exibido no terminal para fins de validação lógica.
    * **Output Final & Transpilação:** Grava autonomamente na raiz do projeto o ficheiro transpilado **`Main.java`**, totalmente compatível com a semântica original e pronto a ser compilado diretamente pelo `javac` e executado na JVM.

> [!NOTE]
> O ANTLR deve estar devidamente configurado e acessível no sistema através do comando `antlr4` (ou mapeado no `Makefile`).
> Os ficheiros gerados pelo ANTLR (`.java` e `.tokens`) estão omitidos do repositório através do `.gitignore`, devendo ser gerados localmente no Passo 1.

## Destaques Técnicos e Funcionalidades Avançadas

O compilador foi blindado contra casos limite e otimizado com mecânicas avançadas dignas de um ambiente de produção:

* **Injeção Cirúrgica de Dependências (Tree-Shaking):** O gerador de código Java analisa dinamicamente a AST do utilizador e apenas injeta a biblioteca de input (`import java.util.Scanner;`) e as respetivas funções auxiliares de manipulação de vetores/strings (`lerStringParaVetor`, `imprimirStringVetor`, `imprimirVetor`) se as mesmas forem efetivamente invocadas no código fonte. Programas simples geram ficheiros Java 100% nativos, sem código morto ou imports desnecessários.
* **Mapeamento de Códigos de Saída (System.exit):** De forma a suportar flexivelmente assinaturas do tipo `vazio principal()` e `inteiro principal()`, o compilador rastreia se o fluxo está dentro da função principal. Instruções como `retornar 0;` ou `retornar variavel;` dentro da `principal` são transpiladas inteligentemente para `System.exit(0);` ou `System.exit(variavel);`, replicando fielmente o comportamento de retorno de estados ao Sistema Operativo nativo da linguagem C.
* **Verificação Estrita de Protótipos:** O analisador semântico valida se todos os protótipos de funções declarados no topo do ficheiro possuem a sua respetiva implementação de bloco de código (corpo) no programa. Caso haja uma "promessa" de função não cumprida, a compilação é abortada com um erro de referência.
* **Obrigatoriedade de Retorno Semântico:** Funções com tipos de retorno bem definidos (`inteiro` ou `real`) são inspecionadas através de um algoritmo de travessia profunda na AST. Se a função não possuir instruções `retornar` compatíveis com o seu tipo em todos os seus ramos acessíveis, um erro semântico é disparado.

## Definição da linguagem MOCP (My Own C in Português)

Na MOCP, a sintaxe formal da linguagem é totalmente portuguesa:
* Todas as palavras-chave, tipos e funções reservadas devem ser escritas em português, conforme as tabelas de correspondência.
* A utilização de palavras-chave da linguagem C original (como `int`, `if`, `else`, `while`, `return`, `void`) é capturada explicitamente e constitui um **erro**.
* Não existem diretivas de pré-processamento `#` (como `#include`).
* Apenas existem os tipos primitivos `inteiro` e `real`.
* As variáveis podem ser simples ou vetores de tamanho fixo.
* **Tratamento de Caracteres e Strings:** Seguindo a semântica de baixo nível do C, os inteiros podem representar diretamente caracteres (armazenando o respetivo código ASCII). Vetores de inteiros representam strings dinâmicas (sendo obrigatoriamente terminadas com o valor nulo `0`).
* Os comentários seguem a norma padrão do C (comentários de linha `//` e de bloco `/* ... */`).
* Os operadores mantêm-se idênticos aos da linguagem C original (`&&`, `||`, `!`, `==`, `!=`, `%`, etc.).

### Tabela de Correspondências Clássicas

| Linguagem C original | Equivalente em MOCP |
| :--- | :--- |
| `int` | `inteiro` |
| `double` / `float` | `real` |
| `void` | `vazio` |
| `main` | `principal` |
| `if` / `else` | `se` / `senao` |
| `while` / `for` | `enquanto` / `para` |
| `return` | `retornar` |
| Input de Dados | `ler()`, `lerc()`, `lers()` |
| Output de Dados | `escrever()`, `escreverc()`, `escreverv()`, `escrevers()` |
