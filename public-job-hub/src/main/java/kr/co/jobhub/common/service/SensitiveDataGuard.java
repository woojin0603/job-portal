package kr.co.jobhub.common.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.regex.Pattern;

/** 공개될 수 있는 사용자 입력에 불필요한 식별정보가 포함되지 않게 한다. */
@Service
public class SensitiveDataGuard {
    private static final Pattern RESIDENT_ID = Pattern.compile("(?<!\\d)\\d{6}-?[1-8]\\d{6}(?!\\d)");
    private static final Pattern PHONE = Pattern.compile("(?<!\\d)(?:01[016789][ -]?\\d{3,4}[ -]?\\d{4}|0\\d{1,2}[ -]?\\d{3,4}[ -]?\\d{4})(?!\\d)");
    private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w.-]+\\.[A-Za-z]{2,}");
    private static final Pattern FORBIDDEN_LABEL = Pattern.compile(
            "(?i)(주민(?:등록)?번호|전화번호|휴대폰|연락처|수험번호|자격증\\s*(?:번호|등록번호)|학위\\s*번호|여권번호|운전면허번호|계좌번호|상세주소)");

    public void reject(Collection<String> values) {
        for (String value : values) {
            String text = value == null ? "" : value.trim();
            if (RESIDENT_ID.matcher(text).find() || PHONE.matcher(text).find()
                    || EMAIL.matcher(text).find() || FORBIDDEN_LABEL.matcher(text).find())
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "전화번호·이메일·주민번호·수험번호 등 개인정보는 저장할 수 없습니다.");
        }
    }
}
