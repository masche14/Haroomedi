var approveResult;

function getEmailExists(type) {
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
            if (json.data.result == 1) {
                approveResult = "Y";
            }else{
                approveResult="N";
            }
            alert(json.data.msg); // 메시지 표시
        }
    );

}

function handleSubmit(event) {
    event.preventDefault();

    console.log(approveResult);

    if (approveResult === "Y") {
        $("#emailVerificationForm").submit();
    } else {
        alert("이메일 인증 여부를 확인하세요.");
    }
}

$(document).ready(function () {
    $("#nextButton").on("click", handleSubmit);
});
