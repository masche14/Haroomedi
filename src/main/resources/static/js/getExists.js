var approveResult = "N";
var existYn;
var checkEmail;
var finalEmail;

function getEmailExists(type) {
    approveResult = "N";
    existYn = undefined;
    checkEmail = undefined;
    finalEmail = undefined;

    const fieldName = $("#" + type).attr("name");
    const value = $("#" + type).val();

    console.log("fieldName:", fieldName, "/ value:", value); // {} 쓰면 안 돼. 그냥 ,로 구분해야 콘솔에 나옴

    if (!value.trim()) {
        alert("이메일을 입력해주세요");
        return;
    }

    if (!value.includes("@")) {
        alert("이메일 형식이 올바르지 않습니다.");
        return;
    }

    const jsonData = {
        fieldName: fieldName,
        value: value
    };

    $.ajax({
        url: "/user/getEmailExists",
        type: "POST",
        contentType: "application/json",
        data: JSON.stringify(jsonData),
        success: function (json) {
            checkEmail = value;
            alert("이메일로 인증번호가 발송되었습니다.\n받은 메일의 인증번호를 입력하기 바랍니다.");
        },
        error: function (xhr, status, error) {
            console.error('Error:', error);
            alert('서버 요청 중 오류가 발생했습니다.');
        }
    });
}

function approveCode() {
    const authNumber = $("#email_confirm").val();

    console.log(authNumber);

    const jsonData = {
        authNumber: authNumber
    };

    $.ajax({
        url: "/user/approveCode",
        type: "POST",
        contentType: "application/json",
        dataType: "JSON",
        data: JSON.stringify(jsonData),
    }).then(
        function (json) {  // 성공 시 콜백 함수
            const res = json.data.result;
            if (res !== 0) {
                approveResult = "Y";
                if (res===1){
                    existYn = "N";
                }else{
                    existYn = "Y";
                }
            }
            alert(json.data.msg); // 메시지 표시
        }
    );

}

function handleSubmit(event) {
    event.preventDefault();

    finalEmail = $("#input_email").val();

    console.log(approveResult);

    if (approveResult === "Y") {
        if (finalEmail === checkEmail){
            $("#emailVerificationForm").submit();
        } else {
            alert("인증받은 이메일과 다른 이메일을 입력하였습니다.");
        }
    } else {
        alert("이메일 인증 여부를 확인하세요.");
    }
}

$(document).ready(function () {
    $("#nextButton").on("click", handleSubmit);
});
