// 중복 확인 상태를 저장할 변수 (전역으로 선언)
var idPossible = false;
var nicknamePossible = false;
var check_id = "";
var check_nick = "";

// 중복 확인 함수
function checkDuplicate(type) {
    const $input = $("#" + type);
    const value = $input.val();
    const fielName = $input.attr("name");
    const $message = $("#" + type + "Message");

    // 입력값이 없을 때 알림 표시
    if (!value.trim()) {
        if (type === "input_nickname") {
            alert('닉네임을 입력해주세요.');
            return;
        } else if (type === "input_id") {
            alert('아이디를 입력해주세요.');
            return;
        }
    }

    const jsonData = {
        fieldName: fielName,
        value: value
    }

    // 서버에 중복 여부 확인 요청 (jQuery 방식)
    $.ajax({
        url: "/user/checkDuplicate",
        type: "POST",
        contentType: "application/json",
        dataType: "JSON",
        data: JSON.stringify(jsonData), // x-www-form-urlencoded 방식
    }).then(
        function (json) {
            $message.text(json.data.msg);

            if (json.data.result == 1){
                $message.css("color", "red")
            }else{
                if (type === "input_id") {
                    check_id = value;
                    idPossible = true;
                } else if (type === "input_nickname") {
                    check_nick = value;
                    nicknamePossible = true;
                }
                $message.css("color", "green")
            }
        }
    );
}
