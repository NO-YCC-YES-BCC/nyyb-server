# nyyb-server

nyyb 서비스의 백엔드 API 서버입니다.

## 기술 스택

| 구분 | 내용 |
| --- | --- |
| Language | Java 21 |
| JDK | Microsoft Build of OpenJDK 21 (21.0.10 LTS) |
| Framework | Spring Boot 3.5.5 |
| Build | Gradle 9.5.1 (Wrapper) |
| ORM | Spring Data JPA (Hibernate) |
| Database | MariaDB |
| Security | Spring Security |
| 기타 | Lombok |

## 요구 사항

- JDK 21 (Microsoft Build of OpenJDK 권장)
- MariaDB (클라우드 호스팅 인스턴스, 예: AWS RDS / CloudType)

## 실행 방법

### 1. 로컬 설정 파일 작성

`src/main/resources/application-local.yml` 파일에 DB 접속 정보를 채웁니다.
이 파일은 `.gitignore` 에 의해 커밋되지 않으므로 각자 환경에 맞게 작성해야 합니다.
DB 는 클라우드 인스턴스를 사용하므로 `url` 에는 발급받은 엔드포인트를 넣습니다.

```yaml
spring:
  datasource:
    driver-class-name: org.mariadb.jdbc.Driver
    url: jdbc:mariadb://<host>:<port>/<db>?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
    username: your_username
    password: your_password
```

### 2. 애플리케이션 실행

```bash
./gradlew bootRun
```

기본 프로파일은 `local` 이며, 다른 프로파일로 실행하려면 환경변수로 지정합니다.

```bash
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun
```

### 3. 빌드 / 테스트

```bash
./gradlew build   # 빌드 (테스트 포함)
./gradlew test    # 테스트만 실행
```

## 설정 파일 구조

- `application.yml` — 공통 설정 (애플리케이션 이름, 공통 JPA 설정 등)
- `application-local.yml` — 로컬 개발용 설정 (git 미추적, 각자 작성)
- 운영/개발 서버 — `SPRING_DATASOURCE_*`, `SPRING_PROFILES_ACTIVE` 등 환경변수로 주입

## 패키지 구조

도메인 단위로 패키지를 구성합니다. 각 도메인은 `controller` / `service` / `data` 계층으로 나뉘며,
`data` 하위에 dto·entity·repository·enums·exception 을 둡니다.

```
com.nyyb.nyybserver
├── NyybServerApplication.java      # 애플리케이션 진입점
└── user                            # user 도메인
    ├── controller                  # REST 컨트롤러 (요청/응답)
    ├── service                     # 비즈니스 로직
    └── data                        # 데이터 계층
        ├── dto                     # 데이터 전송 객체
        │   ├── request             # 요청 DTO
        │   └── response            # 응답 DTO
        ├── entity                  # JPA 엔티티
        ├── repository              # JPA 리포지토리
        ├── enums                   # 도메인 열거형
        └── exception               # 도메인 예외
```

새로운 도메인을 추가할 때에도 `user` 도메인과 동일한 구조를 따릅니다.
