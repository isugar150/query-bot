<img width="200" height="200" alt="icon (2)" src="https://github.com/user-attachments/assets/2f8e0b7b-0519-462f-af69-131afd45b70d" />

# Query Bot

Query Bot은 스키마 정보를 사전에 학습한 AI가 사용자의 자연어 입력을 기반으로 SQL을 자동 생성·검증하며, 읽기 전용 쿼리를 안전하게 실행해 결과를 제공하는 서비스입니다.
개발자가 아니어도 CRM 담당자, 마케터, 기획자 등 누구나 복잡한 테이블 구조나 JOIN 규칙을 몰라도 원하는 데이터를 정확하고 빠르게 조회할 수 있습니다.

## 스크린샷
<img width="993" height="901" alt="스크린샷 2025-11-24 163126" src="https://github.com/user-attachments/assets/710806f6-df87-4b5d-bb33-c81b39d0418f" />

## 주요 기술 스택
- 백엔드: Java 17, Spring Boot, JPA (SQLite 메타데이터), REST API
- 프론트엔드: React 19, TypeScript, Vite, Chakra UI, Zustand
- 컨테이너: Docker + docker-compose (기본 포트 5213)

## 레포지토리 구조
- `backend/` - API, 인증, DB 메타데이터, 챗/쿼리 실행 로직
- `frontend/` - React 앱 (엔트리: `src/main.tsx`, 페이지: `src/pages/`)
- `docker-compose.yml` / `Dockerfile` / `nginx.conf` / `start.sh` - 전체 스택 컨테이너 구성
- `.env.example` - 필수 환경 변수 예시

## 워크플로우
1. 데이터베이스에 연결하면 대상 DB의 테이블·컬럼 메타데이터를 수집하고 가공해 내부 SQLite 메타 DB에 저장합니다.
2. 사용자가 AI에게 질의를 보내면, 저장된 메타데이터를 질문과 함께 프롬프트로 전달해 SQL을 생성·검증한 뒤 응답을 반환합니다.
3. 질의 결과가 실제 데이터나 스키마와 맞지 않으면 원본 데이터베이스 테이블에 `comment` 필드에 상세 설명을 남겨 `DB 정보 갱신` 버튼을 눌러 갱신 후 다시 질문합니다.

## 사전 준비
- Java 17, Gradle Wrapper
- Node.js 20+와 npm
- (선택) Docker 및 Docker Compose

## 환경 변수
`.env.example`를 복사해 `.env`를 만들고 값을 채워주세요.
- `JWT_SECRET` - JWT 서명용 시크릿
- `OPENAI_API_KEY` - OpenAI API 키
- `OPENAI_MODEL` - 모델 이름 (기본: `gpt-5-mini`)
- `METABASE_URL` - Metabase 베이스 URL (예: `https://metabase.example.com`)
- `METABASE_API_KEY` - Metabase API 키 (`x-api-key`로 전송)
- `METABASE_DATABASE_KEY` - Metabase 대상 DB ID
- `METABASE_COLLECTION_KEY` - Metabase 저장 컬렉션 ID

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

## Docker
```bash
docker compose up --build
```
nginx가 http://localhost:5213 으로 프론트엔드를 서빙하고 백엔드를 프록시합니다.

## 주요 엔드포인트
- DB 스키마 갱신: `POST /api/db/refresh/{id}`
- SQL 실행(읽기 전용): `POST /api/db/execute`
- 챗 질문: `POST /api/chat/ask`
- 대화 히스토리: `GET /api/chat/history/{sessionId}`
- Metabase 카드 생성/업데이트: `POST /api/metabase/card`
  - 요청: `{ sessionId, query, title? }`
  - `sessionId`에 연결된 카드가 없으면 새 카드 생성 시 `title`을 사용(없으면 "새로운 쿼리"); 카드가 있으면 기존 카드 제목을 유지한 채 쿼리만 업데이트
  - 서버는 `METABASE_URL`/`METABASE_API_KEY`/`METABASE_DATABASE_KEY`/`METABASE_COLLECTION_KEY` 설정이 유효할 때만 동작

## 기타 노트
- 시크릿은 커밋하지 말고 `.env`나 환경 변수로 주입하세요.
- 서버가 시작 중이거나 내려가서 503을 반환하면, 프론트엔드에서 서버 오류 페이지를 표시하고 재시도 버튼을 제공합니다.
