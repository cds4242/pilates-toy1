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
@Profile({"local-h2", "local"})
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
        for (Instructor instructor : instructors) {
            if (instructor.getPhoneEncrypted() == null && instructor.getPhone() != null) {
                String normalized = instructor.getPhone().replaceAll("[^0-9]", "");
                String encrypted = encryptionService.encrypt(instructor.getPhone());
                String hashed = hashingService.hash(normalized);
                instructor.migratePhone(encrypted, hashed);
                migrated++;
            }
        }
        if (migrated > 0) {
            log.info("[PhoneMigration] 강사 phone 암호화 마이그레이션 완료: {}건", migrated);
        } else {
            log.info("[PhoneMigration] 마이그레이션 대상 없음");
        }
    }
}
