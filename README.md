# Query Bot

스키마 정보를 학습한 AI가 SQL을 작성·검증하고, 읽기 전용 쿼리를 실행할 수 있는 서비스입니다. Spring Boot 기반 백엔드와 React + Chakra UI 프론트엔드로 구성됩니다.

## 주요 기술 스택
- 백엔드: Java 17, Spring Boot, JPA (SQLite 메타데이터), REST API
- 프론트엔드: React 19, TypeScript, Vite, Chakra UI, Zustand
- 컨테이너: Docker + docker-compose (nginx 역방향 프록시, 기본 포트 5213)

## 레포지토리 구조
- `backend/` - API, 인증, DB 메타데이터, 챗/쿼리 실행 로직
- `frontend/` - React 앱 (엔트리: `src/main.tsx`, 페이지: `src/pages/`)
- `docker-compose.yml` / `Dockerfile` / `nginx.conf` / `start.sh` - 전체 스택 컨테이너 구성
- `.env.example` - 필수 환경 변수 예시

## 사전 준비
- Java 17, Gradle Wrapper
- Node.js 20+와 npm
- (선택) Docker 및 Docker Compose

## 환경 변수
`.env.example`를 복사해 `.env`를 만들고 값을 채워주세요.
- `JWT_SECRET` - JWT 서명용 시크릿
- `OPENAI_API_KEY` - OpenAI API 키
- `OPENAI_MODEL` - 모델 이름 (기본: `gpt-5-mini`)

SQLite 메타데이터 DB는 기본적으로 `./data/querybot.db`에 저장됩니다. 다른 경로를 쓰려면 `APP_DATA_DIR`를 환경 변수로 설정하세요.

## 백엔드
```bash
cd backend
./gradlew bootRun                # API 실행 (기본 포트 8080, 변경: --args='--server.port=8090')
./gradlew test                   # 백엔드 테스트
```
모든 API는 `/api` 아래에 노출됩니다. 쿼리 실행은 읽기 전용 SQL(`SELECT`/`WITH`)만 허용하며, DML/DDL은 서버에서 차단합니다.

## 프론트엔드
```bash
cd frontend
npm install
npm run dev                      # Vite 개발 서버 (기본 포트 5173)
npm run build                    # 타입 체크 및 프로덕션 빌드
```

## Docker (풀스택)
```bash
docker compose up --build
```
nginx가 http://localhost:5213 으로 프론트엔드를 서빙하고 백엔드를 프록시합니다.

## 주요 엔드포인트
- DB 스키마 갱신: `POST /api/db/refresh/{id}`
- SQL 실행(읽기 전용): `POST /api/db/execute`
- 챗 질문: `POST /api/chat/ask`
- 대화 히스토리: `GET /api/chat/history/{sessionId}`

## 기타 노트
- 시크릿은 커밋하지 말고 `.env`나 환경 변수로 주입하세요.
- 서버가 시작 중이거나 내려가서 503을 반환하면, 프론트엔드에서 서버 오류 페이지를 표시하고 재시도 버튼을 제공합니다.
