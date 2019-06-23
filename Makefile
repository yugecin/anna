JAVAC=javac
JFLAGS=-g# -Xlint:all

default: .anna .mod_test

outdirs:
	mkdir -p anna/out
	mkdir -p mod_test/out

.anna: outdirs $(wildcard anna/src/net/basdon/anna/api/*.java) $(wildcard anna/src/net/basdon/anna/internal/*.java)
	$(JAVAC) $(JFLAGS) -d anna/out $(filter-out $<,$^)
	jar cfe out/anna.jar net.basdon.anna.internal.Main -C anna/out .

.mod_test: outdirs $(wildcard mod_test/src/annamod/*.java)
	$(JAVAC) $(JFLAGS) -d mod_test/out -cp anna/out $(filter-out $<,$^)
	jar cfM out/mod_test.jar -C mod_test/out .

clean:
	rm -r anna/out/*
	rm -r mod_test/out/*

