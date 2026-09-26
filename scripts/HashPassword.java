import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;

/** Administrative, offline hash generation. Never accepts a password in arguments. */
class HashPassword {
    public static void main(String[] args) {
        var console = System.console();
        if (console == null || args.length != 0) {
            throw new IllegalStateException("Run interactively without arguments.");
        }
        char[] password = console.readPassword("New password (input hidden): ");
        if (password == null) return;
        char[] confirmation = console.readPassword("Confirm password: ");
        try {
            if (CharBuffer.wrap(password).toString().isBlank() || !Arrays.equals(password, confirmation)
                    || StandardCharsets.UTF_8.encode(CharBuffer.wrap(password)).remaining() > 72) {
                throw new IllegalArgumentException("Passwords must match and contain 1–72 UTF-8 bytes.");
            }
            System.out.println(PasswordEncoderFactories.createDelegatingPasswordEncoder()
                    .encode(CharBuffer.wrap(password)));
        } finally {
            Arrays.fill(password, '\0');
            if (confirmation != null) Arrays.fill(confirmation, '\0');
        }
    }
}
