JAVAC=javac
JFLAGS=-g# -Xlint:all

.PHONY: anna interactive-test mod_figlet mod_log mod_test runitest
default: anna interactive-test mod_figlet mod_log mod_test

ANNA_SRC = $(wildcard anna/src/net/basdon/anna/*/*.java)
INTERACTIVETEST_SRC = $(wildcard interactive-test/src/net/basdon/anna/*/*.java)
MOD_FIGLET_SRC = $(wildcard mod_figlet/src/annamod/*.java)
MOD_LOG_SRC = $(wildcard mod_log/src/annamod/*.java)
MOD_TEST_SRC = $(wildcard mod_test/src/annamod/*.java)

out/anna.jar: $(ANNA_SRC)
	@mkdir -p anna/out && ([ -d "out" ] || mkdir -p out)
	$(JAVAC) $(JFLAGS) -d anna/out $(ANNA_SRC)
	jar cfe out/anna.jar net.basdon.anna.internal.Main -C anna/out .
anna: out/anna.jar

out/interactive-test.jar: out/anna.jar $(INTERACTIVETEST_SRC)
	@mkdir -p interactive-test/out && ([ -d "out" ] || mkdir -p out)
	$(JAVAC) $(JFLAGS) -d interactive-test/out -cp anna/out $(INTERACTIVETEST_SRC)
	jar cfm out/interactive-test.jar interactive-test/Manifest.txt -C interactive-test/out .
interactive-test: out/interactive-test.jar

out/mod_figlet.jar: out/anna.jar $(MOD_FIGLET_SRC)
	@mkdir -p mod_figlet/out && ([ -d "out" ] || mkdir -p out)
	$(JAVAC) $(JFLAGS) -d mod_figlet/out -cp anna/out $(MOD_FIGLET_SRC)
	cp mod_figlet/src/annamod/figletfont.txt mod_figlet/out/annamod/figletfont.txt
	jar cfM out/mod_figlet.jar -C mod_figlet/out .
mod_figlet: out/mod_figlet.jar

out/mod_log.jar: out/anna.jar $(MOD_LOG_SRC)
	@mkdir -p mod_log/out && ([ -d "out" ] || mkdir -p out)
	$(JAVAC) $(JFLAGS) -d mod_log/out -cp anna/out $(MOD_LOG_SRC)
	jar cfM out/mod_log.jar -C mod_log/out .
mod_log: out/mod_log.jar

out/mod_test.jar: out/anna.jar $(MOD_TEST_SRC)
	@mkdir -p mod_test/out && ([ -d "out" ] || mkdir -p out)
	$(JAVAC) $(JFLAGS) -d mod_test/out -cp anna/out $(MOD_TEST_SRC)
	jar cfM out/mod_test.jar -C mod_test/out .
mod_test: out/mod_test.jar

runitest: default
	cd out && java -jar interactive-test.jar

clean:
	rm -r anna/out/*
	rm -r interactive-test/out/*
	rm -r mod_figlet/out/*
	rm -r mod_log/out/*
	rm -r mod_test/out/*
	rm -r out/*.jar

