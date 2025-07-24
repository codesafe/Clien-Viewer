# ClienApp Android 프로젝트 분석 문서

## 프로젝트 구조 개요

ClienApp은 Android 네이티브 앱으로, Kotlin과 Jetpack Compose를 사용하여 개발되었습니다.

## 프로젝트 디렉토리 구조

```
/mnt/d/private/clien/
├── ClienApp/                    # 메인 Android 프로젝트
│   ├── app/                     # 앱 모듈
│   │   ├── build.gradle.kts     # 앱 레벨 빌드 설정
│   │   ├── proguard-rules.pro  # ProGuard 규칙
│   │   └── src/main/
│   │       ├── AndroidManifest.xml
│   │       ├── java/com/example/clienapp/
│   │       │   ├── MainActivity.kt      # 메인 액티비티
│   │       │   ├── CacheManager.kt      # 캐시 관리
│   │       │   ├── SSLHelper.kt         # SSL 설정
│   │       │   ├── SwipeGesture.kt      # 스와이프 제스처
│   │       │   └── UrlUtils.kt          # URL 유틸리티
│   │       └── res/             # 리소스 파일들
│   ├── build.gradle.kts         # 프로젝트 레벨 빌드 설정
│   ├── settings.gradle.kts      # 프로젝트 설정
│   └── gradle.properties        # Gradle 속성
├── android-app/                 # 별도 Android 앱 디렉토리
└── ios-app/                     # iOS 앱 디렉토리
```

## 빌드 구성

### 프로젝트 레벨 (build.gradle.kts)
- Android Gradle Plugin: 8.11.1
- Kotlin: 1.9.20

### 앱 레벨 구성
- **Target SDK**: 34 (Android 14)
- **Min SDK**: 24 (Android 7.0)
- **Compile SDK**: 34
- **Java Version**: 17
- **Kotlin JVM Target**: 17
- **Compose Compiler**: 1.5.4

## 주요 의존성 라이브러리

### UI 프레임워크
- **Jetpack Compose**: 최신 Android UI 툴킷
  - compose-bom: 2023.10.01
  - Material 3 디자인 시스템

### 네트워킹
- **Retrofit2**: 2.9.0 (REST API 클라이언트)
- **OkHttp3**: 4.12.0 (HTTP 클라이언트)
- **Gson**: JSON 파싱용 컨버터
- **Scalars**: 문자열 응답 처리용 컨버터

### 기타 주요 라이브러리
- **JSoup**: 1.17.2 (HTML 파싱)
- **Coroutines**: 1.7.3 (비동기 프로그래밍)
- **Navigation Compose**: 2.7.7 (화면 전환)
- **Coil**: 2.5.0 (이미지 로딩)

## 아키텍처 패턴

현재 코드베이스에서 확인된 주요 컴포넌트:

1. **MainActivity.kt**: 앱의 진입점이자 메인 화면
2. **CacheManager.kt**: 캐시 관리 로직
3. **SSLHelper.kt**: HTTPS 통신을 위한 SSL 설정
4. **SwipeGesture.kt**: 스와이프 제스처 처리
5. **UrlUtils.kt**: URL 관련 유틸리티 함수

## 보안 및 네트워크 설정

### AndroidManifest.xml 주요 설정
- 인터넷 권한 사용
- 네트워크 상태 접근 권한
- Clear text traffic 허용 (usesCleartextTraffic="true")
- 커스텀 네트워크 보안 설정 (network_security_config.xml)

## 프로젝트 특징

1. **모던 Android 개발**: Jetpack Compose 사용
2. **Kotlin 전용**: 100% Kotlin으로 작성
3. **웹 스크래핑 지원**: JSoup 라이브러리 포함
4. **비동기 처리**: Coroutines 사용
5. **네트워크 통신**: Retrofit + OkHttp 조합

## 추가 확인 필요 사항

1. 앱의 정확한 목적과 기능
2. 데이터 모델 및 비즈니스 로직 구조
3. 네트워크 API 엔드포인트 및 통신 프로토콜
4. UI 구성 및 화면 플로우
5. 캐시 정책 및 오프라인 지원 여부

## 다음 단계 제안

1. MainActivity.kt 및 기타 주요 Kotlin 파일 상세 분석
2. 네트워크 보안 설정 파일 검토
3. UI 레이아웃 및 테마 분석
4. 실제 API 통신 로직 파악
5. 앱의 전체적인 아키텍처 패턴 확인 (MVVM, MVI 등)