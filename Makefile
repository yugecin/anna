JAVAC=javac
JFLAGS=-g# -Xlint:all

default: .anna .mod_figlet .mod_log .mod_test

outdirs:
	mkdir -p anna/out
	mkdir -p mod_figlet/out
	mkdir -p mod_log/out
	mkdir -p mod_test/out

.anna: outdirs $(wildcard anna/src/net/basdon/anna/api/*.java) $(wildcard anna/src/net/basdon/anna/internal/*.java)
	$(JAVAC) $(JFLAGS) -d anna/out $(filter-out $<,$^)
	jar cfe out/anna.jar net.basdon.anna.internal.Main -C anna/out .

.mod_figlet: outdirs $(wildcard mod_figlet/src/annamod/*.java)
	$(JAVAC) $(JFLAGS) -d mod_figlet/out -cp anna/out $(filter-out $<,$^)
	cp mod_figlet/src/annamod/figletfont.txt mod_figlet/out/annamod/figletfont.txt
	jar cfM out/mod_figlet.jar -C mod_figlet/out .

.mod_log: outdirs $(wildcard mod_log/src/annamod/*.java)
	$(JAVAC) $(JFLAGS) -d mod_log/out -cp anna/out $(filter-out $<,$^)
	jar cfM out/mod_log.jar -C mod_log/out .

.mod_test: outdirs $(wildcard mod_test/src/annamod/*.java)
	$(JAVAC) $(JFLAGS) -d mod_test/out -cp anna/out $(filter-out $<,$^)
	jar cfM out/mod_test.jar -C mod_test/out .

clean:
	rm -r anna/out/*
	rm -r mod_figlet/out/*
	rm -r mod_log/out/*
	rm -r mod_test/out/*

