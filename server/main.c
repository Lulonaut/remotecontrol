#include <arpa/inet.h>
#include <stdio.h>
#include <errno.h>
#include <string.h>
#include <stdarg.h>
#include <sys/socket.h>
#include <unistd.h>
#include <X11/Xlib.h>
#include <X11/extensions/XTest.h>
#include <stdlib.h>
#include <sys/ioctl.h>

#define PORT        7926
#define MAX_LINE    1000
#define LISTENQ     1024

ssize_t read_string(int sockd, void *vptr, size_t maxlen, int stop_on_new_line);

ssize_t write_string(int sockd, const void *vptr, size_t n);

int starts_with(char *string, char *prefix);

int string_eq(char *str, ...);

void move_mouse(Display *display, int x, int y);

void press_key(Display *display, int keycode, int shift);

int main() {
    int list_s, conn_s;
    struct sockaddr_in servaddr;
    char buffer[MAX_LINE];

    if ((list_s = socket(AF_INET, SOCK_STREAM, 0)) < 0) {
        fputs("Error creating listening socket.\n", stderr);
        return 1;
    }

    memset(&servaddr, 0, sizeof(servaddr));
    servaddr.sin_family = AF_INET;
    servaddr.sin_addr.s_addr = htonl(INADDR_ANY);
    servaddr.sin_port = htons(PORT);

    if (bind(list_s, (struct sockaddr *) &servaddr, sizeof(servaddr)) < 0) {
        fputs("Error calling bind\n", stderr);
        return 1;
    }

    if (listen(list_s, LISTENQ) < 0) {
        fputs("Error calling listen\n", stderr);
        return 1;
    }

    Display *display = XOpenDisplay(NULL);
    fputs("Now accepting connections...\n", stderr);
    if ((conn_s = accept(list_s, NULL, NULL)) < 0) {
        fputs("Error calling accept.\n", stderr);
        return 1;
    }
    printf("accepted\n");
    int next_mouse_move_x = 0;
    int next_mouse_move_y = 0;
    int bytes_0_count = 0;
    int keep_alive_tick = 0;
    int last_client_keep_alive = 0;
    while (1) {
        int bytes;
        ioctl(conn_s, FIONREAD, &bytes);
        memset(buffer, 0, MAX_LINE);
        if (bytes > 0) {
            last_client_keep_alive = 0;
            bytes_0_count = 0;
            read_string(conn_s, buffer, MAX_LINE - 1, 1);
            buffer[strcspn(buffer, "\n")] = '\0';
            printf("Buffer: %s\n", buffer);
            if (starts_with(buffer, "mouse") == 0) {
                char *split;
                strtok(buffer, " ");
                split = strtok(NULL, " ");

                char *second_arg = (char *) malloc(MAX_LINE);
                strcpy(second_arg, split);
                split = strtok(NULL, " ");
                char *third_arg = (char *) malloc(MAX_LINE);
                strcpy(third_arg, split);

                int x = 0;
                int y = 0;
                if (strcmp(third_arg, "left") == 0) {
                    x = -1;
                } else if (strcmp(third_arg, "right") == 0) {
                    x = 1;
                } else if (strcmp(third_arg, "up") == 0) {
                    y = -1;
                } else if (strcmp(third_arg, "down") == 0) {
                    y = 1;
                }

                if (strcmp(second_arg, "start") == 0) {
                    next_mouse_move_x = x;
                    next_mouse_move_y = y;
                } else if (strcmp(second_arg, "stop") == 0) {
                    next_mouse_move_x = 0;
                    next_mouse_move_y = 0;
                }
                strcpy(buffer, "ok");
            } else if (starts_with(buffer, "keyboard") == 0) {
                puts("keyboard");
                int keycode, shift;
                char *split;
                strtok(buffer, " ");
                split = strtok(NULL, " ");

                char *second_arg = (char *) malloc(MAX_LINE);
                strcpy(second_arg, split);
                split = strtok(NULL, " ");
                keycode = atoi(second_arg);

                char *third_arg = (char *) malloc(MAX_LINE);
                strcpy(third_arg, split);
                shift = atoi(third_arg);
                press_key(display, keycode, shift);
                strcpy(buffer, "ok");
            } else if (strcmp(buffer, "__KEEP_ALIVE") == 0) {
                last_client_keep_alive = 0;
            } else {
                strcpy(buffer, "invalid command");
            }
            write_string(conn_s, buffer, strlen(buffer));
        } else bytes_0_count++;
        keep_alive_tick++;
        last_client_keep_alive++;
        if (keep_alive_tick >= 1000) {
            char *message = "__KEEP_ALIVE";
            write_string(conn_s, message, strlen(message));
            keep_alive_tick = 0;
        }
        if (last_client_keep_alive > 5000) {
            puts("Client failed to send a keep alive packet on time.");
            exit(0);
        }
        usleep(10000);
        move_mouse(display, next_mouse_move_x, next_mouse_move_y);
    }
}

/*
 * reads a string from the provided socket
 */
ssize_t read_string(int sockd, void *vptr, size_t maxlen, int stop_on_new_line) {
    ssize_t n, rc;
    char c, *buffer;

    buffer = vptr;

    for (n = 1; n < (long) maxlen; n++) {
        if ((rc = read(sockd, &c, 1)) == 1) {
            *buffer++ = c;
            if (stop_on_new_line && c == '\n')
                break;
        } else if (rc == 0) {
            if (n == 1)
                return 0;
            else
                break;
        } else {
            if (errno == EINTR)
                continue;
            return -1;
        }
    }

    *buffer = 0;
    return n;
}

/*
 * writes a string to the provided socket
 */
ssize_t write_string(int sockd, const void *vptr, size_t n) {
    size_t nleft = n;
    ssize_t nwritten;
    const char *buffer;

    buffer = vptr;

    while (nleft > 0) {
        if ((nwritten = write(sockd, buffer, nleft)) <= 0) {
            if (errno == EINTR)
                nwritten = 0;
            else
                return -1;
        }
        nleft -= nwritten;
        buffer += nwritten;
    }

    return (long) n;
}

/*
 * Checks if string starts with prefix
 */
int starts_with(char *string, char *prefix) {
    while (*prefix) {
        if (*prefix != *string) return 1;
        prefix++;
        string++;
    }
    return 0;
}

/*
 * compares all arguments against the first string using strcmp
 */
int string_eq(char *str, ...) {
    va_list arg;
    va_start(arg, str);
    char *comp = str;

    str = va_arg(arg, char*);
    while (str) {
        if (strcmp(comp, str) == 0) {
            (void) arg;
            return 0;
        } else {
            str = va_arg(arg, char*);
        }
    }
    (void) arg;
    return 1;
}

/*
 * moves the mouse relative to the current position
 */
void move_mouse(Display *display, int x, int y) {
    XTestFakeRelativeMotionEvent(display, x, y, 0);
    XFlush(display);
}

/*
 * simulates a normal key press, optionally with holding shift
 */
void press_key(Display *display, int keycode, int shift) {
    if (shift) {
        XTestFakeKeyEvent(display, 50, 1, 0);
    }
    XTestFakeKeyEvent(display, keycode, 1, 0);
    usleep(10000);
    XTestFakeKeyEvent(display, keycode, 0, 0);
    if (shift) {
        XTestFakeKeyEvent(display, 50, 0, 0);
    }
}
