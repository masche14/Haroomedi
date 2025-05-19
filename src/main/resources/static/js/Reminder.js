document.addEventListener("DOMContentLoaded", function () {
    if (SS_USER == null) {
        alert("로그인 후 이용 가능합니다.");
        setReferrer();
        return;
    }

    const referrer = document.referrer;
    if (!referrer.includes("prescriptionList")) {
        alert("올바르지 않은 접근입니다.");
        window.location.href = "/health/prescriptionList";
        return;
    }

    intakeLog.forEach(entry => {
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

    Object.keys(monthMap).forEach(ym => {
        monthMap[ym] = Array.from(monthMap[ym]).sort();
    });

    const latestDate = Object.keys(dateMap).sort().pop();
    currentMonth = latestDate.slice(0, 7);
    updateMonthDisplay(currentMonth);
    renderDateButtons(currentMonth);
    $(`.date-btn[data-date="${latestDate}"]`).addClass('active');
    renderIntakeList(latestDate);

    $('#prev-month').on('click', () => changeMonth(-1));
    $('#next-month').on('click', () => changeMonth(1));
    $('#date-prev').on('click', () => { if (datePage > 0) { datePage--; renderDatePage(); } });
    $('#date-next').on('click', () => {
        const maxPage = Math.ceil(currentMonthDates.length / datesPerPage) - 1;
        if (datePage < maxPage) { datePage++; renderDatePage(); }
    });
});

function updateMonthDisplay(month) {
    const [year, m] = month.split("-");
    $('#month-display').text(`${year}.${m}`);
}

function renderDateButtons(month) {
    $('#date-list').empty();
    datePage = 0;
    if (!monthMap[month]) return;
    currentMonthDates = monthMap[month];
    renderDatePage();
}

function renderDatePage() {
    const start = datePage * datesPerPage;
    const end = start + datesPerPage;
    const slicedDates = currentMonthDates.slice(start, end);

    $('#date-list').empty();
    slicedDates.forEach(day => {
        const ymd = `${currentMonth}-${day}`;
        const btn = $(`<span class="date-btn" data-date="${ymd}">${day}</span>`);
        $('#date-list').append(btn);
    });

    $('.date-btn').off('click').on('click', function () {
        $('.date-btn').removeClass('active');
        $(this).addClass('active');
        const selectedDate = $(this).data('date');
        renderIntakeList(selectedDate);
    });
}

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

function renderIntakeList(date) {
    const logList = dateMap[date];
    if (!logList) return;
    renderIntakeListUniversal(date, logList);
}

function renderIntakeListUniversal(date, logList) {
    if (!Array.isArray(logList)) return;
    $('#intake-list').empty();
    const now = new Date();

    logList.forEach((entry, idx) => {
        const time = entry.time;
        const entryTime = new Date(`${date}T${time}:00`);

        let color = "";
        if (entry.intakeYn === "Y") color = "green";
        else if (entryTime > now) color = "gray";
        else color = "red";

        const checkboxId = `check-${date}-${time.replace(':', '-')}`;

        $('#intake-list').append(`
            <div class="intake-entry ${color}">
                <span>${idx + 1}번째</span>
                <span>${time}</span>
                <label class="custom-checkbox">
                    <input type="checkbox" id="${checkboxId}" data-date="${date}" data-time="${time}" ${entry.intakeYn === "Y" ? "checked" : ""} />
                    <span class="checkmark"></span>
                </label>
            </div>
        `);
    });

    $('#intake-list input[type="checkbox"]').off('change').on('change', function () {
        const date = $(this).data('date');
        const time = $(this).data('time');
        const intakeYn = $(this).is(':checked') ? "Y" : "N";

        const data = {
            prescriptionId: prescriptionId,
            intakeLog: [
                {
                    intakeTime: `${date}T${time}`,
                    intakeYn: intakeYn
                }
            ]
        };

        $.ajax({
            url: '/health/updateIntakeLog',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify(data),
            success: function (res) {
                renderUpdatedLog(res.data.intakeLog);
            },
            error: function (err) {
                console.error("업데이트 실패:", err);
                alert("복약 상태 변경 중 오류가 발생했습니다.");
            }
        });
    });
}

function renderUpdatedLog(updatedLogList) {
    if (!Array.isArray(updatedLogList) || updatedLogList.length === 0) return;

    updatedLogList.forEach(entry => {
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

    const last = updatedLogList[updatedLogList.length - 1];
    const d = new Date(last.intakeTime);
    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    const lastDate = `${year}-${month}-${day}`;
    renderIntakeListUniversal(lastDate, dateMap[lastDate]);
}
