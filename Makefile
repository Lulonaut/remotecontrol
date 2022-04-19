CC=gcc
CFLAGS=-std=gnu11 -Wall -Wextra -O3 -g
SRC=server/main.c
LIBS=-lX11 -lXtst
OUT=rsocket

all:
	$(CC) $(SRC) $(CFLAGS) $(LIBS) -o $(OUT)
clean:
	@-rm $(OUT)
