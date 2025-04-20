function setReferrer() {
    // 현재 페이지의 URL을 로컬 스토리지에 저장
    sessionStorage.setItem('referrer', window.location.href);

    const form = document.createElement('form');
    form.method = 'POST';
    form.action = '/user/setReferrer';
    document.body.appendChild(form);
    form.submit();
}