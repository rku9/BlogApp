package exceptions;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.util.StringTokenizer;

public class NoPostException extends RuntimeException{
    public NoPostException(String message, long id) {

    }
}
