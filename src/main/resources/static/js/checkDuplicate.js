// 중복 확인 상태를 저장할 변수 (전역으로 선언)
var idChecked = false;
var nicknameChecked = false;
var check_id = "";
var check_nick = "";

// 중복 확인 함수
function checkDuplicate(type) {
    // 입력된 값과 메시지를 표시할 요소를 가져옵니다.
    const inputElement = document.getElementById(type);
    const value = inputElement.value;
    const messageElement = document.getElementById(type + 'Message');

    // 입력값이 없을 때 알림 표시
    if (!value.trim()) {
        if (type === "input_nickname"){
            alert('닉네임을 입력해주세요.');
            return;
        } else if (type === "input_id") {
            alert('아이디를 입력해주세요.');
            return;
        }
    }

    // 서버에 중복 여부를 확인하기 위한 요청
    fetch('check-duplicate', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
        },
        body: new URLSearchParams({ type: type, value: value }) // type을 input 박스의 id로 설정
    })
        .then(response => response.json())
        .then(data => {
            console.log('서버 응답 데이터:', data);
            // 서버로부터 받은 메시지를 해당 HTML 요소에 표시합니다.
            messageElement.textContent = data.message;

            // 중복된 아이디/닉네임일 경우 입력 필드 값 지우기 및 상태 초기화
            if (isDuplicateMessage(data.message)) {
                messageElement.style.color = 'red';
                inputElement.value = '';  // 중복일 경우 입력 필드 값 지우기

                // 중복 확인 상태를 false로 초기화
                if (type === 'input_id') {
                    idChecked = false;
                } else if (type === 'input_nickname') {
                    nicknameChecked = false;
                }

            } else {
                messageElement.style.color = 'green';
                // 중복 확인이 성공하면 확인 상태를 true로 설정
                if (type === 'input_id') {
                    check_id = data.checkId;
                    idChecked = true;
                } else if (type === 'input_nickname') {
                    check_nick=data.checkNick;
                    nicknameChecked = true;
                }
            }
            messageElement.style.display = 'block';  // 메시지가 있을 때만 표시
        })
        .catch(error => {
            console.error('Error:', error);
            messageElement.textContent = '오류가 발생했습니다. 다시 시도해주세요.';
            messageElement.style.color = 'red';
            messageElement.style.display = 'block';  // 메시지가 있을 때만 표시
        });
}

// 중복 여부 메시지를 확인하여 중복된 항목인지 여부를 반환하는 함수
function isDuplicateMessage(message) {
    return message.includes('이미 존재합니다');
}
