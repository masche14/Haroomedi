$(function () {
    // [1] 로그인 여부 확인
    if (SS_USER == null) {
        alert("로그인 후 이용 가능합니다.");
        setReferrer(); // 이전 페이지 저장
        return;
    }

    // [2] 사용자 ID 검증
    if (SS_USER.userId !== userId) {
        alert("올바르지 않은 접근입니다.");
        window.location.href = "/user/index";
        return;
    }

    // [3] intakeLog 데이터를 날짜별/월별로 정리하여 dateMap, monthMap에 저장
    $.each(intakeLog, function (_, entry) {
        const d = new Date(entry.intakeTime);
        const year = d.getFullYear();
        const month = String(d.getMonth() + 1).padStart(2, '0');
        const day = String(d.getDate()).padStart(2, '0');
        const ymd = `${year}-${month}-${day}`;
        const ym = `${year}-${month}`;
        const time = d.toTimeString().slice(0, 5);

        if (!dateMap[ymd]) dateMap[ymd] = [];
        dateMap[ymd].push({ time, intakeYn: entry.intakeYn });

        if (!monthMap[ym]) monthMap[ym] = new Set();
        monthMap[ym].add(day);
    });

    // [4] 월별 날짜 리스트 정렬
    $.each(monthMap, function (ym, set) {
        monthMap[ym] = Array.from(set).sort();
    });

    // [5] 가장 최신 날짜 선택
    const latestDate = Object.keys(dateMap).sort().pop();
    currentMonth = latestDate.slice(0, 7);
    updateMonthDisplay(currentMonth);
    renderDateButtons(currentMonth);

    // [6] 최신 날짜에 해당하는 버튼 활성화 및 리스트 렌더링
    const $latestBtn = $(`.date-btn[data-date="${latestDate}"]`);
    $latestBtn.addClass('active');
    renderIntakeList(latestDate);

    // [7] 최신 날짜 버튼 위치로 스크롤 이동
    setTimeout(() => {
        const wrapper = document.getElementById('date-list-wrapper');
        const target = $latestBtn[0];
        if (wrapper && target) {
            wrapper.scrollLeft = target.offsetLeft - wrapper.offsetLeft;
        }
    }, 100);

    // [8] 월 이동 버튼 이벤트 등록
    $('#prev-month').on('click', () => changeMonth(-1));
    $('#next-month').on('click', () => changeMonth(1));
});

// [9] 월 표기 갱신
function updateMonthDisplay(month) {
    const [year, m] = month.split("-");
    $('#month-display').text(`${year}.${m}`);
}

// [10] 날짜 버튼 목록 렌더링
function renderDateButtons(month) {
    $('#date-list').empty();
    if (!monthMap[month]) return;
    currentMonthDates = monthMap[month];

    $.each(currentMonthDates, function (_, day) {
        const ymd = `${month}-${day}`;
        const btn = $(`<span class="date-btn" data-date="${ymd}">${day}</span>`);
        $('#date-list').append(btn);
    });

    // 날짜 버튼 클릭 시 해당 날짜 복약 기록 렌더링
    $('.date-btn').off('click').on('click', function () {
        $('.date-btn').removeClass('active');
        $(this).addClass('active');
        const selectedDate = $(this).data('date');
        renderIntakeList(selectedDate);
    });
}

// [11] 월 이동 처리
function changeMonth(offset) {
    const months = Object.keys(monthMap).sort();
    let idx = months.indexOf(currentMonth);
    if (idx === -1) return;
    idx += offset;
    if (idx < 0 || idx >= months.length) return;

    currentMonth = months[idx];
    updateMonthDisplay(currentMonth);
    renderDateButtons(currentMonth);

    const firstDay = monthMap[currentMonth][0];
    const newDate = `${currentMonth}-${firstDay}`;
    $(`.date-btn[data-date="${newDate}"]`).addClass('active');
    renderIntakeList(newDate);
}

// [12] 날짜별 복약 기록 렌더링
function renderIntakeList(date) {
    const logList = dateMap[date];
    if (!logList) return;
    renderIntakeListUniversal(date, logList);
}

// [13] 실제 복약 기록 목록 구성 및 표시
function renderIntakeListUniversal(date, logList) {
    if (!Array.isArray(logList)) return;
    $('#intake-list').empty();
    const now = new Date();
    const orgLeftIntakeCnt = reminder.leftIntakeCnt ?? 0; // 남은 복약 수

    $.each(logList, function (idx, entry) {
        const time = entry.time;
        const entryTime = new Date(`${date}T${time}:00`);
        let color = "";

        if (entry.intakeYn === "Y") color = "green";
        else if (entryTime > now) color = "gray";
        else color = "red";

        const checkboxId = `check-${date}-${time.replace(':', '-')}`;
        const isChecked = entry.intakeYn === "Y";
        const isDisabled = !isChecked && orgLeftIntakeCnt === 0; // 이미 체크 안 된 항목은 비활성화

        $('#intake-list').append(`
            <div class="intake-entry ${color}">
                <span>${idx + 1}번째</span>
                <span>${time}</span>
                <label class="custom-checkbox">
                    <input type="checkbox" id="${checkboxId}" data-date="${date}" data-time="${time}"
                        ${isChecked ? "checked" : ""} ${isDisabled ? "disabled" : ""} />
                    <span class="checkmark"></span>
                </label>
            </div>
        `);
    });

    // [14] 체크박스 상태 변경 이벤트 처리
    $('#intake-list input[type="checkbox"]').off('change').on('change', function () {
        const date = $(this).data('date');
        const time = $(this).data('time');
        const intakeYn = $(this).is(':checked') ? "Y" : "N";

        const orgIntakeCnt = reminder.intakeCnt ?? 0;
        const orgLeftIntakeCnt = reminder.leftIntakeCnt ?? 0;
        const toIntakeCnt = reminder.toIntakeCnt;

        // 체크하려는 경우인데 남은 복약 수가 없으면 차단
        if (intakeYn === "Y" && orgLeftIntakeCnt === 0) {
            alert("약을 모두 복용했습니다.");
            $(this).prop('checked', false);
            return;
        }

        const intakeCnt = intakeYn === "Y" ? orgIntakeCnt + 1 : orgIntakeCnt - 1;
        const leftIntakeCnt = toIntakeCnt - intakeCnt;

        const data = {
            prescriptionId: prescriptionId,
            intakeCnt: intakeCnt,
            leftIntakeCnt: leftIntakeCnt,
            intakeLog: [{ intakeTime: `${date}T${time}`, intakeYn }]
        };

        // [15] Ajax 요청으로 서버에 복약 기록 업데이트
        $.ajax({
            url: '/health/updateIntakeLog',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify(data),
            success: function (res) {
                reminder = res.data; // 서버 응답으로 최신 reminder 갱신
                renderUpdatedLog(res.data.intakeLog, date);
            },
            error: function (err) {
                console.error("업데이트 실패:", err);
                alert("복약 상태 변경 중 오류가 발생했습니다.");
            }
        });
    });
}

// [16] 서버 응답으로 받은 최신 intakeLog로 UI 갱신
function renderUpdatedLog(updatedLogList, selectedDate) {
    if (!Array.isArray(updatedLogList) || updatedLogList.length === 0) return;

    $.each(updatedLogList, function (_, entry) {
        const d = new Date(entry.intakeTime);
        const year = d.getFullYear();
        const month = String(d.getMonth() + 1).padStart(2, '0');
        const day = String(d.getDate()).padStart(2, '0');
        const date = `${year}-${month}-${day}`;
        const time = d.toTimeString().slice(0, 5);

        if (!dateMap[date]) dateMap[date] = [];
        const idx = dateMap[date].findIndex(e => e.time === time);
        if (idx !== -1) {
            dateMap[date][idx].intakeYn = entry.intakeYn;
        } else {
            dateMap[date].push({ time, intakeYn: entry.intakeYn });
        }
    });

    // 변경된 데이터를 기반으로 다시 UI 렌더링
    renderIntakeListUniversal(selectedDate, dateMap[selectedDate]);
}
