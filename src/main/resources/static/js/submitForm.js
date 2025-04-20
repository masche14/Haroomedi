function submitForm(actionUrl) {
    const form = document.getElementById('signupForm');
    form.action = actionUrl; // action을 동적으로 설정
    form.submit(); // 폼 제출
}