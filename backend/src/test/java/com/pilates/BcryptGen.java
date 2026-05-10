package com.pilates;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import com.pilates.common.security.hash.HashingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
public class BcryptGen {
    @Test
    public void verifyMemberHash() {
        BCryptPasswordEncoder enc = new BCryptPasswordEncoder();
        String memberHash = "$2a$12$xNSI0kNgrNbq/f8rp2jY0uncqvm6iwPfbRx8YD3JUluQyYz4/41xC";
        for (String pw : new String[]{"test1234", "Test1234!", "test1234!", "Test1234", "TEST1234", "test12345"}) {
            System.out.println("PWCHECK " + pw + " -> " + enc.matches(pw, memberHash));
        }
    }
}
@SpringBootTest
@ActiveProfiles("local")
class HashLookup {
    @Autowired private HashingService hashing;
    @Test
    public void findPhones() {
        // member id 31, 32, plus all members - find target phone hashes
        String[] targets = {
            "3f2df09f2659acd466834d46c4fbd21bde81875f6c82445bd325574acee1a540",  // id 31
            "95ca5afda2651c09fc86b57eaf67d801c626ef7f3780197c387f5b095075dbda"   // id 32
        };
        // try common test phone patterns
        String[] candidates = {
            "010-1111-1111", "010-2222-2222", "010-3333-3333", "010-1234-5678",
            "01011111111", "01022222222", "01012345678",
            "010-1111-2222", "010-3333-4444", "010-5555-6666",
            "010-0000-0001", "010-0000-0002", "010-0000-0003",
            "010-1111-0001", "010-2222-0002", "010-3333-0003",
            "010-9999-1111", "010-9999-2222", "010-9999-9999",
            "010-1234-0001", "010-1234-0002", "010-1234-0003",
            "010-7777-7777", "010-8888-8888", "010-1010-1010",
            "010-0101-0101", "010-1004-1004", "010-1000-0001",
            "010-test-new", "010-9000-0000"
        };
        for (String target : targets) {
            for (String c : candidates) {
                String h = hashing.hash(c);
                if (target.equals(h)) {
                    System.out.println("PHONEMATCH " + target.substring(0, 12) + " = " + c);
                }
            }
        }
        System.out.println("PHONEDONE");
    }
}
