package com.pilates.common.security.encryption;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 암호화 키 설정.
 * application.yml의 app.encryption 하위 속성을 바인딩한다.
 * 키는 Base64 인코딩된 32바이트(256비트) 문자열.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.encryption")
public class EncryptionKeyProperties {

    /** Base64 인코딩된 AES-256 키 */
    private String key;

    /** 키 버전 (암호문 prefix로 사용, 예: "v1") */
    private String keyVersion = "v1";
}
