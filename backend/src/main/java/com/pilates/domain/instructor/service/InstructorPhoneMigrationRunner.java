package com.pilates.domain.instructor.service;

import com.pilates.common.security.encryption.EncryptionService;
import com.pilates.common.security.hash.HashingService;
import com.pilates.domain.instructor.entity.Instructor;
import com.pilates.domain.instructor.repository.InstructorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 강사 phone 평문 → 암호화 마이그레이션 Runner.
 * 시드 데이터의 phone 컬럼을 phone_encrypted/phone_hash로 변환한다.
 */
@Slf4j
@Component
@Profile({"local-h2", "local", "demo"})
@RequiredArgsConstructor
public class InstructorPhoneMigrationRunner {

    private final InstructorRepository instructorRepository;
    private final EncryptionService encryptionService;
    private final HashingService hashingService;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void migrate() {
        log.info("[PhoneMigration] Runner 시작");
        List<Instructor> instructors = instructorRepository.findAllByDeletedAtIsNull();
        log.info("[PhoneMigration] 전체 강사 수: {}", instructors.size());
        int migrated = 0;
        int reencrypted = 0;
        for (Instructor instructor : instructors) {
            // 1) 미암호화 + 평문 존재 → 신규 암호화
            if (instructor.getPhoneEncrypted() == null && instructor.getPhone() != null) {
                String normalized = instructor.getPhone().replaceAll("[^0-9]", "");
                String encrypted = encryptionService.encrypt(instructor.getPhone());
                String hashed = hashingService.hash(normalized);
                instructor.migratePhone(encrypted, hashed);
                migrated++;
                continue;
            }
            // 2) 암호화돼있지만 현재 키로 복호화 실패 → 평문에서 재암호화 (키 변경/시드 잔재 회복)
            if (instructor.getPhoneEncrypted() != null && instructor.getPhone() != null) {
                try {
                    encryptionService.decrypt(instructor.getPhoneEncrypted());
                } catch (Exception ex) {
                    String normalized = instructor.getPhone().replaceAll("[^0-9]", "");
                    String encrypted = encryptionService.encrypt(instructor.getPhone());
                    String hashed = hashingService.hash(normalized);
                    instructor.migratePhone(encrypted, hashed);
                    reencrypted++;
                }
            }
        }
        if (migrated > 0 || reencrypted > 0) {
            log.info("[PhoneMigration] 신규 암호화 {}건, 키 불일치 재암호화 {}건", migrated, reencrypted);
        } else {
            log.info("[PhoneMigration] 마이그레이션 대상 없음");
        }
    }
}
