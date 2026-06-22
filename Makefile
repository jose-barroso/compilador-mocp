# Auto-detect ANTLR jar
ANTLR_JAR := $(firstword \
    $(wildcard antlr-4.*-complete.jar) \
    $(wildcard /usr/local/lib/antlr-4.*-complete.jar) \
    $(wildcard /usr/share/java/antlr-4.*-complete.jar) \
    $(wildcard /opt/homebrew/Cellar/antlr/*/antlr-4.*-complete.jar) \
)

ifeq ($(ANTLR_JAR),)
$(error "ANTLR jar not found. Install it or place antlr-4.x-complete.jar in project root.")
endif

GRAMMAR = src/main/antlr/MOCP.g4
GEN_DIR = src/generated/java
SRC_DIR = src/main/java
BUILD_DIR = build
PACKAGE = mocp
MAIN_CLASS = mocp.Main

# ==============================================================================
# DETEÇÃO ROBUSTA DE SISTEMA OPERATIVO
# ==============================================================================
# Se a variável OS contiver "Windows" ou se a COMSPEC estiver definida, assume Windows
ifneq ($(findstring Windows,$(OS)),)
    CP_SEP := ;
else ifdef ComSpec
    CP_SEP := ;
else
    CP_SEP := :
endif
# ==============================================================================

all: antlr compile

antlr:
	mkdir -p $(GEN_DIR)/$(PACKAGE)
	java -jar $(ANTLR_JAR) -Dlanguage=Java -visitor -package $(PACKAGE) -o $(GEN_DIR)/$(PACKAGE) $(GRAMMAR)

compile:
	mkdir -p $(BUILD_DIR)
	javac -cp $(ANTLR_JAR) -d $(BUILD_DIR) \
       $(shell find $(SRC_DIR) -name "*.java") \
       $(shell find $(GEN_DIR) -name "*.java")

run:
	java -cp "$(BUILD_DIR)$(CP_SEP)$(ANTLR_JAR)" $(MAIN_CLASS) $(FILE)

# ==============================================================================
# GERAÇÃO DE CÓDIGO JAVA (AST -> Programa.java -> javac -> execução)
# ==============================================================================
OUTPUT_DIR = output

# Gera Programa.java a partir de um ficheiro MOCP, compila com javac e executa
gerar: compile
	@mkdir -p $(OUTPUT_DIR)
	java -cp "$(BUILD_DIR)$(CP_SEP)$(ANTLR_JAR)" $(MAIN_CLASS) $(FILE)
	javac $(OUTPUT_DIR)/Programa.java -d $(OUTPUT_DIR)/
	@echo "--- Programa compilado com sucesso ---"

executar:
	java -cp $(OUTPUT_DIR) Main

# Atalho: gerar + executar
gerar-executar: gerar executar

# ==============================================================================
# TESTES AUTOMATIZADOS
# ==============================================================================
TEST_DIR = src/test

test-sucesso: compile
	@mkdir -p $(OUTPUT_DIR)
	@PASS=0; FAIL=0; \
	for f in $(TEST_DIR)/caso_sucesso_*.mocp $(TEST_DIR)/javagenerator_1.mocp $(TEST_DIR)/teste_completo.mocp; do \
		name=$$(basename "$$f"); \
		java -cp "$(BUILD_DIR)$(CP_SEP)$(ANTLR_JAR)" $(MAIN_CLASS) "$$f" > /dev/null 2>&1; \
		if [ $$? -ne 0 ]; then echo "FAIL (mocp) $$name"; FAIL=$$((FAIL+1)); continue; fi; \
		javac $(OUTPUT_DIR)/Programa.java -d $(OUTPUT_DIR)/ 2>&1; \
		if [ $$? -eq 0 ]; then echo "PASS $$name"; PASS=$$((PASS+1)); \
		else echo "FAIL (javac) $$name"; FAIL=$$((FAIL+1)); fi; \
	done; \
	echo ""; echo "Sucesso: $$PASS passaram, $$FAIL falharam"

test-insucesso: compile
	@PASS=0; FAIL=0; \
	for f in $(TEST_DIR)/caso_insucesso_*.mocp; do \
		name=$$(basename "$$f"); \
		java -cp "$(BUILD_DIR)$(CP_SEP)$(ANTLR_JAR)" $(MAIN_CLASS) "$$f" > /dev/null 2>&1; \
		if [ $$? -ne 0 ]; then echo "REJECT (esperado) $$name"; PASS=$$((PASS+1)); \
		else echo "ACCEPT (inesperado!) $$name"; FAIL=$$((FAIL+1)); fi; \
	done; \
	echo ""; echo "Insucesso: $$PASS rejeitados, $$FAIL aceites inesperadamente"

test-execucao: compile
	@mkdir -p $(OUTPUT_DIR)
	@echo "=== Execução dos programas gerados (sem stdin) ===" ; \
	for f in $(TEST_DIR)/caso_sucesso_*.mocp $(TEST_DIR)/javagenerator_1.mocp; do \
		name=$$(basename "$$f" .mocp); \
		java -cp "$(BUILD_DIR)$(CP_SEP)$(ANTLR_JAR)" $(MAIN_CLASS) "$$f" > /dev/null 2>&1 || continue; \
		javac $(OUTPUT_DIR)/Programa.java -d $(OUTPUT_DIR)/ 2>/dev/null || continue; \
		echo "--- $$name ---"; \
		java -cp $(OUTPUT_DIR) Main 2>&1 || true; \
		echo ""; \
	done

test: test-sucesso test-insucesso

clean:
	rm -rf $(BUILD_DIR) $(GEN_DIR) $(OUTPUT_DIR)/*.class