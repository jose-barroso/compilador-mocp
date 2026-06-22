//Efolio B - Equipa: ParseMasters
grammar MOCP;

// ==========================
// PARSER (analise sintatica)
// ==========================

// Correção: Protótipos antes de funções e variáveis globais
// Correção: Agora o parser aceita 3 elementos globais em qualquer ordem
// 			 A regra de que prototipos vêm primeiro é aplicada no Analisador Semântico, (antes o código tinha conflito de Lookahead (LL)*)
// 			 Como prototipo, dec_variavel e funcao comecam da mesma maneirda (tipo ID...) o ANTLR fica baralhado na transicao do primeiro bloco * para o segundo bloco *
// 			 Não sabia quando é que os protótipos acabam e as funções começam.
programa : (prototipo PONTO_VIRG | dec_variavel PONTO_VIRG | funcao)* EOF ;

// Define os tipos de dados básicos
tipo : T_INTEIRO | T_REAL | T_VAZIO ;

// Declaração de variáveis
dec_variavel : tipo dec_item (VIRGULA dec_item)* ;

dec_item : ID (ABRE_RET INT_VAL? FECHA_RET)? (ATRIB inicializador)? ;

inicializador : expressao | ABRE_CHAV expressao (VIRGULA expressao)* FECHA_CHAV ;

// Correção do Vazio nos Protótipos
prototipo : tipo (ID | PRINCIPAL) ABRE_PAR prototipo_params? FECHA_PAR ;

prototipo_params : param_tipo (VIRGULA param_tipo)* | T_VAZIO ;

// param_tipo já não aceita T_VAZIO individualmente
param_tipo : tipo ID? (ABRE_RET FECHA_RET)? ;

funcao : tipo (ID | PRINCIPAL) ABRE_PAR parametros? FECHA_PAR bloco ;

parametros : param_dec (VIRGULA param_dec)* | T_VAZIO ;

param_dec : tipo ID (ABRE_RET FECHA_RET)? ;

bloco : ABRE_CHAV instrucao* FECHA_CHAV ;

instrucao
    : dec_variavel PONTO_VIRG
    | expressao PONTO_VIRG
    | instrucao_se
    | instrucao_enquanto
    | instrucao_para
    | RETORNAR expressao? PONTO_VIRG
    ;

instrucao_se : SE ABRE_PAR expressao FECHA_PAR bloco (SENAO bloco)? ;

instrucao_enquanto : ENQUANTO ABRE_PAR expressao FECHA_PAR bloco ;

instrucao_para : PARA ABRE_PAR expressao? PONTO_VIRG expressao? PONTO_VIRG expressao? FECHA_PAR bloco ;

// REGRAS DE EXPRESSÃO
// Nota: exprVetorAtrib tem que aparecer ANTES de exprVetor.
// No ANTLR a ordem das alternativas define a precedencia: a regra mais
// especifica tem de vir primeiro, senao 'v[i]' faz match com exprVetor e o
// parser nunca chega a ver o '=' seguinte para reconhecer atribuicao a vetor.
expressao
    : ABRE_PAR expressao FECHA_PAR                                  # exprParenteses
    | ABRE_PAR tipo FECHA_PAR expressao                             # exprCast
    | MENOS expressao                                               # exprMenosUnario
    | NAO_LOGICO expressao                                          # exprNao
    | expressao (MULT | DIV | MODULO) expressao                     # exprMultDiv
    | expressao (MAIS | MENOS) expressao                            # exprSomaSub
    | expressao (MAIOR|MENOR|MAIOR_IGUAL|MENOR_IGUAL) expressao     # exprRelacional
    | expressao (IGUAL|DIFERENTE) expressao                         # exprIgualdade
    | expressao E_LOGICO expressao                                  # exprE
    | expressao OU_LOGICO expressao                                  # exprOu
    | <assoc=right> ID ABRE_RET expressao FECHA_RET ATRIB expressao # exprVetorAtrib
    | ID ABRE_RET expressao FECHA_RET                               # exprVetor
    | <assoc=right> ID ATRIB expressao                              # exprAtribuicao

    //Correção: Antes aceitava apenas ID(...), agora ceita ID ou qualquer token de I/O antes do abre parêntesis
    | (ID | LER | LERC | LERS | ESCREVER | ESCREVERC | ESCREVERV | ESCREVERS) ABRE_PAR lista_args? FECHA_PAR # exprChamadaFuncao

    | INT_VAL                                                       # exprInt
    | FLOAT_VAL                                                     # exprReal
    | STRING_VAL                                                    # exprString
    | CHAR_VAL                                                      # exprChar
    | ID                                                            # exprId
    ;

lista_args : expressao (VIRGULA expressao)* ;

// ======================
// LEXER (analise lexica)
// ======================

T_INTEIRO : 'inteiro' ;
T_REAL    : 'real' ;
T_VAZIO   : 'vazio' ;
SE        : 'se' ;
SENAO     : 'senao' ;
ENQUANTO  : 'enquanto' ;
PARA      : 'para' ;
RETORNAR  : 'retornar' ;
PRINCIPAL : 'principal' ;

// Palavras reservadas para I/O
LER       : 'ler' ;
LERC      : 'lerc' ;
LERS      : 'lers' ;
ESCREVER  : 'escrever' ;
ESCREVERC : 'escreverc' ;
ESCREVERV : 'escreverv' ;
ESCREVERS : 'escrevers' ;

// Keywords de C capturadas como erro léxico
// (Como não estão no parser, se o programador as usar, o parser rejeita)
ERR_C_KEYWORD : 'int' | 'if' | 'else' | 'while' | 'for' | 'return' | 'void' | 'float' | 'double' | 'char' ;

ATRIB       : '=' ;
MAIS        : '+' ;
MENOS       : '-' ;
MULT        : '*' ;
DIV         : '/' ;
MODULO      : '%' ;
MAIOR       : '>' ;
MENOR       : '<' ;
MAIOR_IGUAL : '>=' ;
MENOR_IGUAL : '<=' ;
IGUAL       : '==' ;
DIFERENTE   : '!=' ;
E_LOGICO    : '&&' ;
OU_LOGICO   : '||' ;
NAO_LOGICO  : '!' ;

ABRE_PAR    : '(' ;
FECHA_PAR   : ')' ;
ABRE_CHAV   : '{' ;
FECHA_CHAV  : '}' ;
ABRE_RET    : '[' ;
FECHA_RET   : ']' ;
PONTO_VIRG  : ';' ;
VIRGULA     : ',' ;

// ID deve vir sempre depois das palavras reservadas
ID : [a-zA-Z_][a-zA-Z0-9_]* ;
INT_VAL   : [0-9]+ ;
FLOAT_VAL : [0-9]+ '.' [0-9]+ ;

// Literais de String e Char com suporte a escapes
fragment ESCAPE : '\\' [btnfr"'\\] ;
STRING_VAL : '"' ( ESCAPE | ~['"\\\r\n] )* '"' ;
CHAR_VAL   : '\'' ( ESCAPE | ~['\\\r\n] ) '\'' ;

WS : [ \t\r\n]+ -> skip ;
COMMENT : '/*' .*? '*/' -> skip ;
LINE_COMMENT : '//' ~[\r\n]* -> skip ;
