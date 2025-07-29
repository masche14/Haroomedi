# 프로젝트 소개
- 프로젝트 명 : 하루약속(Haroomedi)
- 시연 영상 : [https://youtu.be/oUuN_a7BO98?si=rfn3-ApKK4MtZk1c](https://youtu.be/OzoKi9t20LI?si=E-CLR5JJN_4PuIxQ)
- 주소 : https://haroomedi.com/
- 개요 : 복약 관리 및 약품 추천 서비스
# 개발 배경
- 약을 먹어야 하는데 깜빡하고 넘어가는 경우가 많음
- 약을 제 때에 먹지 않으면 위험한 사람들도 있음
- 복약 일정을 자동으로 생성해주고 복약 시간이 되면 알려주는 서비스가 필요함
# 주요 기능
- API를 활용하여 국민건강보험공단 처방 내역 조회 기능
- 복약 일정 자동 생성 기능
- 복약 시간 알림 기능
- AI 챗봇을 활용한 사용자 의료상담 기능 (약사 관점)
- 주변 약국 찾기 기능
# 적용 기술
- 프레임워크 : 스프링부트
- 사용언어 : Java, HTML5, CSS, JavaScript
- 데이터베이스 : MongoDB
- 클라우드 : AWS EC2
- 프로토콜 : HTTPS
- 기타 : jQuery, Ajax
- IDE : IntelliJ
- API:
  - **TILKO API**: 본인 인증 및 사용자 처방 내역 조회
  - **OPENAI API**: 처방 데이터 전처리 / AI 상담 챗봇
  - **KAKAO MAP API**: 사용자 위치 주변 / 타 지역 약국 찾기
