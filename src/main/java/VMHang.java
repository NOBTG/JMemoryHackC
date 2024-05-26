package org.example;

import memory.me.xdark.shell.ShellcodeRunner;

import java.io.IOException;
import java.util.Scanner;

public class VMHang {
    private static class K {
        public static void a() {
            System.out.println("Hello World");
        }
        public static Object b() {
            return new Object();
        }
        public static int c() {
            return 114514;
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        K.a();
        System.out.println(K.b());
        System.out.println(K.c());
        new Scanner(System.in).next();

        ShellcodeRunner.rev(K.class);

        K.a();
        System.out.println(K.b());
        System.out.println(K.c());
        new Scanner(System.in).next();
    }
}
