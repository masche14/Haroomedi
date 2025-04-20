var emailAuthNumber;
var approveResult;
var nicknameChecked;

function getEmailExists(type) {
    const inputElement = document.getElementById(type);
    const value = inputElement.value;

    if (!value.trim()) {
        alert("이메일을 입력해주세요");
        return;
    }

    if (!value.includes("@")) {
        alert("이메일 형식이 올바르지 않습니다.");
        return;
    }

    // 특정 div 내 input 값을 수집하여 json 데이터로 전송
    const jsonData = {
        email: value
    };

    fetch('getEmailExists', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(jsonData)
    })
        .then(response => response.json())
        .then(json => {
            alert("이메일로 인증번호가 발송되었습니다. \n받은 메일의 인증번호를 입력하기 바랍니다.");
            emailAuthNumber = json.authNumber;
            console.log(emailAuthNumber);
        })
        .catch(error => {
            console.error('Error:', error);
            alert('서버 요청 중 오류가 발생했습니다.');
        });
}

function approveCode() {
    const authNumber = document.getElementById('email_confirm').value;

    if (!authNumber.trim()) {
        alert("이메일 인증 코드를 입력하세요.");
        return;
    }

    if (authNumber === String(emailAuthNumber)) {
        approveResult = "y";
        alert("인증이 완료되었습니다.");
    } else {
        approveResult = "n";
        alert("인증에 실패하였습니다. 인증번호를 다시 한번 확인하세요.");
    }
}

// nextButton 클릭 시 동작을 정의하는 함수
function handleSubmit(event) {
    event.preventDefault();

    if (approveResult === "y") {
        document.getElementById('emailVerificationForm').submit();
    } else {
        alert("이메일 인증 여부를 확인하세요.");
    }
}

// DOM이 준비되면 버튼에 클릭 이벤트 핸들러를 추가
document.addEventListener('DOMContentLoaded', function () {
    const nextButton = document.getElementById('nextButton');
    if (nextButton) {
        nextButton.addEventListener('click', handleSubmit);
    }
});
