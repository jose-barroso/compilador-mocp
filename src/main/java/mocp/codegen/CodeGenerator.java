package mocp.codegen;

import mocp.ast.*;
import java.util.List;

public class CodeGenerator {

    private StringBuilder sb;
    private int indent = 0;

    // --- FLAGS DE INJEÇÃO CIRÚRGICA ---
    private boolean usaScanner = false;              // Apenas para import e instância do Scanner
    private boolean usaHelperLerString = false;      // Apenas para injetar lerStringParaVetor()
    private boolean usaHelperImprimirString = false; // Apenas para injetar imprimirStringVetor()
    private boolean usaHelperImprimirVetor = false;  // Apenas para injetar imprimirVetor()

    private boolean inMain = false; // Rastreia se estamos dentro da função principal

    private void tab() {
        for (int i = 0; i < indent; i++) sb.append("  ");
    }

    public String gerar(ASTNode ast) {
        System.out.println("DEBUG: CodeGenerator.gerar() foi chamado. Código JAVA esta em output/Programa.java");

        // Reset das flags para o caso de compilarmos vários ficheiros seguidos
        this.usaScanner = false;
        this.usaHelperLerString = false;
        this.usaHelperImprimirString = false;
        this.usaHelperImprimirVetor = false;

        // Passo 1: Construir o corpo do programa PRIMEIRO
        this.sb = new StringBuilder();
        this.indent = 1; // O corpo fica dentro da class Main

        if (ast instanceof ProgramaNode) {
            // 1. Declarações globais
            for (ASTNode child : ((ProgramaNode) ast).getFilhos()) {
                if (child instanceof DeclaracaoNode) gerarDeclaracaoGlobal((DeclaracaoNode) child);
            }
            // 2. Funções
            for (ASTNode child : ((ProgramaNode) ast).getFilhos()) {
                if (child instanceof FuncaoNode) gerarFuncao((FuncaoNode) child);
            }
        }

        // Guardamos o corpo gerado (neste ponto, as flags já sabem exatamente o que foi usado!)
        String corpoDoPrograma = sb.toString();

        // Passo 2: Montar o Ficheiro Final
        StringBuilder ficheiroFinal = new StringBuilder();

        // Só importa o Scanner se houver inputs
        if (usaScanner) ficheiroFinal.append("import java.util.Scanner;\n\n");

        ficheiroFinal.append("class Main {\n");

        if (usaScanner) {
            ficheiroFinal.append("  private static Scanner scanner = new Scanner(System.in);\n\n");
        }

        // --- INJEÇÕES INDIVIDUAIS CIRÚRGICAS ---

        if (usaHelperLerString) {
            ficheiroFinal.append("  public static int[] lerStringParaVetor(String s) {\n");
            ficheiroFinal.append("    int[] v = new int[s.length() + 1];\n");
            ficheiroFinal.append("    for (int i = 0; i < s.length(); i++) {\n");
            ficheiroFinal.append("      v[i] = (int) s.charAt(i);\n");
            ficheiroFinal.append("    }\n");
            ficheiroFinal.append("    v[s.length()] = 0;\n");
            ficheiroFinal.append("    return v;\n");
            ficheiroFinal.append("  }\n\n");
        }

        if (usaHelperImprimirString) {
            ficheiroFinal.append("  public static void imprimirStringVetor(int[] v) {\n");
            ficheiroFinal.append("    for (int i = 0; i < v.length && v[i] != 0; i++) {\n");
            ficheiroFinal.append("      System.out.print((char) v[i]);\n");
            ficheiroFinal.append("    }\n");
            ficheiroFinal.append("  }\n\n");
        }

        if (usaHelperImprimirVetor) {
            ficheiroFinal.append("  public static void imprimirVetor(int[] v) {\n");
            ficheiroFinal.append("    System.out.print(\"{\");\n");
            ficheiroFinal.append("    for (int i = 0; i < v.length; i++) {\n");
            ficheiroFinal.append("      if (i > 0) System.out.print(\",\");\n");
            ficheiroFinal.append("      System.out.print(v[i]);\n");
            ficheiroFinal.append("    }\n");
            ficheiroFinal.append("    System.out.print(\"}\");\n");
            ficheiroFinal.append("  }\n\n");
            ficheiroFinal.append("  public static void imprimirVetor(double[] v) {\n");
            ficheiroFinal.append("    System.out.print(\"{\");\n");
            ficheiroFinal.append("    for (int i = 0; i < v.length; i++) {\n");
            ficheiroFinal.append("      if (i > 0) System.out.print(\",\");\n");
            ficheiroFinal.append("      System.out.print(v[i]);\n");
            ficheiroFinal.append("    }\n");
            ficheiroFinal.append("    System.out.print(\"}\");\n");
            ficheiroFinal.append("  }\n\n");
        }

        // Colamos o corpo do programa logo a seguir às funções auxiliares (se existirem)
        ficheiroFinal.append(corpoDoPrograma);

        ficheiroFinal.append("}\n");

        return ficheiroFinal.toString();
    }

    // ---------- GLOBAIS ----------
    private void gerarDeclaracaoGlobal(DeclaracaoNode dec) {
        for (ASTNode item : dec.getItens()) {
            if (item instanceof DeclaradorNode) {
                DeclaradorNode d = (DeclaradorNode) item;
                tab();
                sb.append("private static ").append(mapTipo(dec.getTipo()));
                if (d.getTamanhoVetor() >= 0) {
                    sb.append("[] ").append(d.getId());
                } else {
                    sb.append(" ").append(d.getId());
                }
                if (d.getInicializador() != null) {
                    sb.append(" = ");
                    if (d.getTamanhoVetor() >= 0) {
                        gerarInicializadorVetor(d.getInicializador());
                    } else {
                        gerarExpressao(d.getInicializador());
                    }
                } else if (d.getTamanhoVetor() > 0) {
                    sb.append(" = new ").append(mapTipo(dec.getTipo())).append("[").append(d.getTamanhoVetor()).append("]");
                }
                sb.append(";\n");
            }
        }
    }

    // ---------- FUNÇÕES ----------
    private void gerarFuncao(FuncaoNode f) {
        if (f.getNome().equals("principal")) {
            gerarMain(f);
        } else {
            tab();
            sb.append("public static ")
                    .append(mapTipo(f.getTipo()))
                    .append(" ")
                    .append(f.getNome())
                    .append("(");
            gerarParametros(f);
            sb.append(") {\n");
            indent++;
            gerarBloco(f.getBloco());
            indent--;
            tab();
            sb.append("}\n\n");
        }
    }

    private void gerarMain(FuncaoNode f) {
        tab();
        sb.append("public static void main(String[] args) {\n");
        indent++;

        this.inMain = true; // LIGAMOS A FLAG
        gerarBloco(f.getBloco());
        this.inMain = false; // DESLIGAMOS A FLAG

        indent--;
        tab();
        sb.append("}\n\n");
    }

    // ---------- PARÂMETROS ----------
    private void gerarParametros(FuncaoNode f) {
        ASTNode params = f.getParametros();
        if (params == null) return;
        if (params instanceof AfirmacaoCompostaNode) {
            boolean first = true;
            for (ASTNode p : ((AfirmacaoCompostaNode) params).getInstrucoes()) {
                if (p instanceof ParametroNode) {
                    ParametroNode param = (ParametroNode) p;
                    if (!first) sb.append(", ");
                    sb.append(mapTipo(param.getTipo()));
                    if (param.isEsVetor()) sb.append("[]");
                    sb.append(" ").append(param.getId());
                    first = false;
                }
            }
        }
    }

    // ---------- BLOCO ----------
    private void gerarBloco(ASTNode bloco) {
        if (bloco == null) return;
        if (bloco instanceof AfirmacaoCompostaNode) {
            for (ASTNode instr : ((AfirmacaoCompostaNode) bloco).getInstrucoes()) {
                gerarInstrucao(instr);
            }
        }
    }

    // ---------- INSTRUÇÕES ----------
    private void gerarInstrucao(ASTNode node) {
        if (node == null) return;
        if (node instanceof DeclaracaoNode) {
            gerarDeclaracaoLocal((DeclaracaoNode) node);
        } else if (node instanceof RetornarNode) {
            gerarReturn((RetornarNode) node);
        } else if (node instanceof SeNode) {
            gerarIf((SeNode) node);
        } else if (node instanceof EnquantoNode) {
            gerarWhile((EnquantoNode) node);
        } else if (node instanceof ParaNode) {
            gerarFor((ParaNode) node);
        } else {
            tab();
            gerarExpressao(node);
            sb.append(";\n");
        }
    }

    // ---------- DECLARAÇÕES LOCAIS ----------
    private void gerarDeclaracaoLocal(DeclaracaoNode dec) {
        for (ASTNode item : dec.getItens()) {
            if (item instanceof DeclaradorNode) {
                DeclaradorNode d = (DeclaradorNode) item;
                tab();
                sb.append(mapTipo(dec.getTipo()));
                if (d.getTamanhoVetor() >= 0) {
                    sb.append("[] ").append(d.getId());
                } else {
                    sb.append(" ").append(d.getId());
                }
                if (d.getInicializador() != null) {
                    sb.append(" = ");
                    if (d.getTamanhoVetor() >= 0) {
                        gerarInicializadorVetor(d.getInicializador());
                    } else {
                        gerarExpressao(d.getInicializador());
                    }
                } else if (d.getTamanhoVetor() > 0) {
                    sb.append(" = new ").append(mapTipo(dec.getTipo())).append("[").append(d.getTamanhoVetor()).append("]");
                }
                sb.append(";\n");
            }
        }
    }

    // ---------- INICIALIZADOR DE VETOR (lista { ... } ou literal string -> ASCII+0) ----------
    private void gerarInicializadorVetor(ASTNode init) {
        if (init instanceof AfirmacaoCompostaNode) {
            sb.append("{");
            List<ASTNode> itens = ((AfirmacaoCompostaNode) init).getInstrucoes();
            for (int i = 0; i < itens.size(); i++) {
                if (i > 0) sb.append(", ");
                gerarExpressao(itens.get(i));
            }
            sb.append("}");
        } else if (init instanceof LiteralStringNode) {
            // Conversão de literal string para int[] de códigos ASCII terminado em 0
            String texto = ((LiteralStringNode) init).getValor();
            sb.append("new int[]{");
            for (int i = 0; i < texto.length(); i++) {
                if (i > 0) sb.append(", ");
                sb.append((int) texto.charAt(i));
            }
            if (texto.length() > 0) sb.append(", ");
            sb.append("0}");
        } else {
            gerarExpressao(init);
        }
    }

    // ---------- RETURN ----------
    private void gerarReturn(RetornarNode ret) {
        tab();

        if (this.inMain) {
            if (ret.getExpressao() != null) {
                sb.append("System.exit(");
                gerarExpressao(ret.getExpressao());
                sb.append(");\n");
            } else {
                sb.append("return;\n");
            }
        } else {
            sb.append("return");
            if (ret.getExpressao() != null) {
                sb.append(" ");
                gerarExpressao(ret.getExpressao());
            }
            sb.append(";\n");
        }
    }

    // ---------- IF ----------
    private void gerarIf(SeNode node) {
        tab();
        sb.append("if (");
        gerarExpressao(node.getCondicao());
        sb.append(") {\n");
        indent++;
        gerarBloco(node.getBlocoSe());
        indent--;
        tab();
        sb.append("}");
        if (node.getBlocoSenao() != null) {
            sb.append(" else {\n");
            indent++;
            gerarBloco(node.getBlocoSenao());
            indent--;
            tab();
            sb.append("}");
        }
        sb.append("\n");
    }

    // ---------- WHILE ----------
    private void gerarWhile(EnquantoNode node) {
        tab();
        sb.append("while (");
        gerarExpressao(node.getCondicao());
        sb.append(") {\n");
        indent++;
        gerarBloco(node.getCorpo());
        indent--;
        tab();
        sb.append("}\n");
    }

    // ---------- FOR ----------
    private void gerarFor(ParaNode node) {
        tab();
        sb.append("for (");
        if (node.getInit() != null) gerarExpressao(node.getInit());
        sb.append("; ");
        if (node.getCondicao() != null) gerarExpressao(node.getCondicao());
        sb.append("; ");
        if (node.getIncremento() != null) gerarExpressao(node.getIncremento());
        sb.append(") {\n");
        indent++;
        gerarBloco(node.getCorpo());
        indent--;
        tab();
        sb.append("}\n");
    }

    // ---------- EXPRESSÕES ----------
    private void gerarExpressao(ASTNode node) {
        if (node == null) return;

        if (node instanceof LiteralIntNode) {
            sb.append(((LiteralIntNode) node).getValor());
        } else if (node instanceof LiteralRealNode) {
            sb.append(((LiteralRealNode) node).getValor());
        } else if (node instanceof LiteralStringNode) {
            sb.append("\"").append(((LiteralStringNode) node).getValor()).append("\"");
        } else if (node instanceof LiteralCharNode) {
            sb.append(((LiteralCharNode) node).getValor());
        } else if (node instanceof IDNode) {
            sb.append(((IDNode) node).getNome());
        } else if (node instanceof OpBinNode) {
            gerarOpBin((OpBinNode) node);
        } else if (node instanceof OpUnNode) {
            gerarOpUn((OpUnNode) node);
        } else if (node instanceof ChamadaFuncaoNode) {
            gerarChamada((ChamadaFuncaoNode) node);
        } else if (node instanceof AcessoVetorNode) {
            gerarAcessoVetor((AcessoVetorNode) node);
        }
    }

    // ---------- OP BINÁRIO ----------
    private void gerarOpBin(OpBinNode op) {
        String operador = op.getOperador();
        if (operador.equals("=") && op.getEsquerda() instanceof AcessoVetorNode) {
            gerarAcessoVetor((AcessoVetorNode) op.getEsquerda());
            sb.append(" = ");
            gerarExpressao(op.getDireita());
            return;
        }
        if (operador.equals("=")) {
            gerarExpressao(op.getEsquerda());
            sb.append(" = ");
            gerarExpressao(op.getDireita());
            return;
        }
        sb.append("(");
        gerarExpressao(op.getEsquerda());
        sb.append(" ").append(operador).append(" ");
        gerarExpressao(op.getDireita());
        sb.append(")");
    }

    // ---------- OP UNÁRIO (inclui casts e booleanos) ----------
    private void gerarOpUn(OpUnNode un) {
        String op = un.getOperador();
        ASTNode expr = un.getExpressao();

        if (op.equals("int2real")) {
            sb.append("(double)(");
            gerarExpressao(expr);
            sb.append(")");
        } else if (op.equals("real2int")) {
            sb.append("(int)(");
            gerarExpressao(expr);
            sb.append(")");
        } else if (op.equals("-")) {
            sb.append("-");
            gerarExpressao(expr);
        } else if (op.equals("!")) {
            sb.append("(");
            gerarExpressao(expr);
            sb.append(" == 0)");
        } else {
            gerarExpressao(expr);
        }
    }

    // ---------- ACESSO A VETOR ----------
    private void gerarAcessoVetor(AcessoVetorNode acesso) {
        sb.append(acesso.getId());
        sb.append("[");
        gerarExpressao(acesso.getIndice());
        sb.append("]");
    }

    // ---------- CHAMADAS (inclui I/O) ----------
    private void gerarChamada(ChamadaFuncaoNode c) {
        String nome = c.getNome();
        List<ASTNode> args = c.getArgumentos();

        switch (nome) {
            case "ler":
                usaScanner = true;
                sb.append("scanner.nextInt()");
                break;
            case "lerc":
                usaScanner = true;
                sb.append("scanner.next().charAt(0)");
                break;
            case "lers":
                usaScanner = true;
                usaHelperLerString = true; // Só precisamos da injeção de leitura!
                sb.append("lerStringParaVetor(scanner.next())");
                break;
            case "escrever":
                sb.append("System.out.print(");
                if (!args.isEmpty()) gerarExpressao(args.get(0));
                sb.append(")");
                break;
            case "escreverc":
                sb.append("System.out.print((char)(");
                if (!args.isEmpty()) gerarExpressao(args.get(0));
                sb.append("))");
                break;
            case "escreverv":
                usaHelperImprimirVetor = true; // Só precisamos da injeção de imprimir vetor numérico!
                if (!args.isEmpty()) {
                    sb.append("imprimirVetor(");
                    gerarExpressao(args.get(0));
                    sb.append(")");
                }
                break;
            case "escrevers":
                if (!args.isEmpty()) {
                    ASTNode arg = args.get(0);
                    if (arg instanceof LiteralStringNode) {
                        sb.append("System.out.print(");
                        gerarExpressao(arg);
                        sb.append(")");
                    } else {
                        usaHelperImprimirString = true; // Só precisamos da injeção de imprimir strings via vetor de ASCII!
                        sb.append("imprimirStringVetor(");
                        gerarExpressao(arg);
                        sb.append(")");
                    }
                }
                break;
            default:
                sb.append(nome).append("(");
                for (int i = 0; i < args.size(); i++) {
                    if (i > 0) sb.append(", ");
                    gerarExpressao(args.get(i));
                }
                sb.append(")");
                break;
        }
    }

    // ---------- MAPEAMENTO DE TIPOS ----------
    private String mapTipo(String tipo) {
        switch (tipo) {
            case "inteiro": return "int";
            case "real": return "double";
            case "vazio": return "void";
            default: return "int";
        }
    }
}