package mocp;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;

// Error listener personalizado para reportar erros léxicos e sintáticos
public class MOCPErrorListener extends BaseErrorListener {

  private int numErros = 0;
  private boolean silenciarCascata = false; // NOVA FLAG: O nosso silenciador

  @Override
  public void syntaxError(Recognizer<?, ?> recognizer,
                          Object offendingSymbol,
                          int line, int charPositionInLine,
                          String msg,
                          RecognitionException e) {

    // Se já detetámos um erro estrutural grave, ignoramos os erros de "pânico" do ANTLR
    if (silenciarCascata) {
      return;
    }

    numErros++;

    // 1. Se a mensagem contiver "token recognition error", é um erro léxico genérico
    if (msg != null && msg.contains("token recognition error")) {
      System.err.println("Erro léxico: " + msg + " na linha " + line + ", coluna " + charPositionInLine);
      return;
    }

    // 2. Apanha o "Token Venenoso" das keywords de C
    if (offendingSymbol instanceof Token) {
      Token token = (Token) offendingSymbol;

      if (token.getType() == MOCPLexer.ERR_C_KEYWORD) {
        System.err.println("[Erro Léxico/Sintático] Linha " + line + ", coluna " + charPositionInLine +
                ": Palavra-chave de C '" + token.getText() +
                "' não é permitida. Na linguagem MOCP utilize o equivalente em português!");

        // silenciador, daqui para a frente nenhum erro do parser é impresso.
        silenciarCascata = true;
        return;
      }
    }

    // 3. Apanha instruções soltas no escopo global
    if (msg != null && msg.contains("expecting {<EOF>, 'inteiro', 'real', 'vazio'}")) {
      System.err.println("[Erro Sintático] Linha " + line + ", coluna " + charPositionInLine +
              ": Instrução solta inválida. No escopo global (fora de funções) apenas podes declarar protótipos, funções ou variáveis globais iniciadas por um tipo.");

      // Ativamos o silenciador aqui também para evitar "lixo" no terminal
      silenciarCascata = true;
      return;
    }

    // 4. Trata como um erro sintático normal (fallback)
    System.err.println("Erro na linha " + line + ", coluna " + charPositionInLine + ": " + msg);
  }

  // Retorna true se encontrar erros durante a análise
  public boolean temErros() {
    return numErros > 0;
  }

  // Retorna o número total de erros encontrados
  public int getNumErros() {
    return numErros;
  }
}