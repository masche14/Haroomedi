let startDate, endDate;
let myChart;

$(document).ready(function () {
    const today = new Date();
    const year = today.getFullYear();
    const month = today.getMonth() + 1;

    setDateParam(year, month);
    getTodayUserCnt();

    flatpickr("#monthPicker", {
        plugins: [
            new monthSelectPlugin({
                shorthand: false,
                dateFormat: "Y/m",
                altFormat: "Y년 m월"
            })
        ],
        defaultDate: new Date(),
        allowInput: false,
        clickOpens: true,
        onReady: function (selectedDates, dateStr, instance) {
            setTimeout(() => {
                instance.setDate(instance.config.defaultDate, true);
            }, 100);
            setMonthLabelsToNumbers(instance);
            bindMonthClickManually(instance);
        },
        onOpen: function (selectedDates, dateStr, instance) {
            setTimeout(() => {
                setMonthLabelsToNumbers(instance);
                bindMonthClickManually(instance);
            }, 10);
        },
        onYearChange: function (selectedDates, dateStr, instance) {
            setTimeout(() => {
                setMonthLabelsToNumbers(instance);
                bindMonthClickManually(instance);
            }, 10);
        },
        onChange: function (selectedDates, dateStr, instance) {
            if (!dateStr || dateStr.trim() === "") return;
            const [year, month] = dateStr.split("/").map(Number);
            setDateParam(year, month);
            console.log("선택된 년월 기준:", startDate, "~", endDate);
        }
    });
});

function setDateParam(year, month) {
    const mm = String(month).padStart(2, '0');
    const lastDay = new Date(year, month, 0).getDate();
    startDate = `${year}-${mm}-01`;
    endDate = `${year}-${mm}-${String(lastDay).padStart(2, '0')}`;

    $.ajax({
        type: "POST",
        url: "/admin/getDailyUserCntByMonth",
        contentType: "application/json",
        data: JSON.stringify({
            startDate: startDate,
            endDate: endDate
        }),
        success: function (json) {
            dailyCntList = json.data;
            console.log("dailyCntList : ", dailyCntList);
            renderChart(dailyCntList, year, month);
        },
        error: function () {
            alert("오류발생");
        }
    });
}

function getTodayUserCnt() {
    $.ajax({
        type: "POST",
        url: "/admin/getTodayUserCnt",
        contentType: "application/json",
        success: function (json) {
            todayUserCnt = json.data;
            console.log("todayUserCnt : ", todayUserCnt);
            $("#todayUserCnt").text(todayUserCnt + "명");
        },
        error: function () {
            alert("오류발생");
        }
    });
}

function setMonthLabelsToNumbers(instance) {
    const monthLabels = instance.calendarContainer.querySelectorAll(".flatpickr-monthSelect-month");
    monthLabels.forEach((el, i) => {
        el.textContent = String(i + 1).padStart(2, '0');
    });
}

function bindMonthClickManually(instance) {
    const monthLabels = instance.calendarContainer.querySelectorAll(".flatpickr-monthSelect-month");
    monthLabels.forEach((el, i) => {
        const newEl = el.cloneNode(true);
        el.parentNode.replaceChild(newEl, el);
        newEl.addEventListener('click', function (e) {
            e.stopPropagation();
            const selectedYear = parseInt(instance.currentYear);
            const selectedMonth = i + 1;
            const newDate = `${selectedYear}/${String(selectedMonth).padStart(2, '0')}`;
            if (selectedYear && selectedMonth) {
                instance.setDate(newDate, true);
                instance.close();
            }
        });
    });
}

function renderChart(dataList, year, month) {
    const lastDay = new Date(year, month, 0).getDate();
    const labels = [];
    const values = [];
    const valueMap = {};

    $.each(dataList, function (i, item) {
        const d = new Date(item.loginDateString);
        const label = `${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
        valueMap[label] = item.userCount;
    });

    for (let i = 1; i <= lastDay; i++) {
        const day = String(i).padStart(2, '0');
        const label = `${String(month).padStart(2, '0')}-${day}`;
        labels.push(label);
        values.push(valueMap[label] || 0);
    }

    if (myChart) myChart.dispose();
    myChart = echarts.init($('#dailyChart')[0]);

    const option = {
        tooltip: {
            trigger: 'axis',
            axisPointer: {
                type: 'shadow',
                shadowStyle: {
                    color: 'rgba(112, 217, 197, 0.1)'
                }
            },
            confine: true,
            formatter: function (params) {
                let result = params[0];
                return `${result.axisValue}<br/>● ${result.seriesName}  :  ${result.data}`;
            }
        },
        xAxis: {
            type: 'category',
            data: labels,
            boundaryGap: false
        },
        yAxis: {
            type: 'value',
            minInterval: 1,
            max: 5
        },
        grid: {
            left: '3%',
            right: '4%',
            bottom: '10%',
            containLabel: true
        },
        series: [{
            name: '일일 접속자 수',
            type: 'line',
            data: values,
            smooth: true,
            areaStyle: {
                color: 'rgba(112, 217, 197, 0.3)'
            },
            emphasis: {
                focus: 'series'
            },
            symbolSize: 10,
            lineStyle: {
                color: '#70d9c5'
            },
            itemStyle: {
                color: '#70d9c5',
                borderColor: '#70d9c5'
            }
        }]
    };

    myChart.setOption(option);
}
