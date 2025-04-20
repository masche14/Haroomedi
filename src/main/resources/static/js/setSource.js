function setSource(source) {
    // 세션 스토리지에 값 저장 (필요한 경우)
    sessionStorage.setItem('findIdSource', source);

    // 숨겨진 필드에 값을 설정하여 서버로 전송
    document.getElementById('findIdSource').value = source;

    // 폼 제출
    document.getElementById('findIdForm').submit();
}