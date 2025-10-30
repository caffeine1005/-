import org.example.user_management.PasswordHasher;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordHasherTest {

    private final PasswordHasher hasher = new PasswordHasher();

    @Test
    void hashProducesSameValueForSameInput() {
        String hash1 = hasher.hash("secret123");
        String hash2 = hasher.hash("secret123");
        assertEquals(hash1, hash2);
    }

    @Test
    void matchesReturnsTrueForCorrectPassword() {
        String hash = hasher.hash("abc12345");
        assertTrue(hasher.matches("abc12345", hash));
    }

    @Test
    void matchesReturnsFalseForDifferentPassword() {
        String hash = hasher.hash("password1");
        assertFalse(hasher.matches("password2", hash));
    }

    @Test
    void hashRejectsNullPassword() {
        assertThrows(IllegalArgumentException.class, () -> hasher.hash(null));
    }

    @Test
    void hashAllowsShortPasswordButProducesHash() {
        String hash = hasher.hash("123");
        assertNotNull(hash);
        assertFalse(hash.isEmpty());
    }

    @Test
    void matchesHandlesInvalidRawPassword() {
        String hash = hasher.hash("longPassword");
        assertFalse(hasher.matches("123", hash));
    }

    @Test
    void hashProducesHexCharactersOnly() {
        String hash = hasher.hash("Symbols!@#123");
        assertTrue(hash.matches("[0-9a-f]+"));
    }

    @Test
    void matchesReturnsFalseWhenStoredHashNull() {
        assertFalse(hasher.matches("anything", null));
    }

    @Test
    void hashHandlesMixedCharacters() {
        assertDoesNotThrow(() -> hasher.hash("Unicode123!@#"));
    }
}