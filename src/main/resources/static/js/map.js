let map, userLocation, openOverlay = null;
let polygons = [];
let detailMode = false;
let currentLevel = 5;
let pharmacyMarkers = [], resultList = [];
let originalPharmacyMarkers = []; // 모든 마커를 저장하기 위한 배열 추가
let currentActiveId = null; // 현재 활성화된 pharmacy item의 ID를 저장

function getDistanceFromLatLonInKm(lat1, lon1, lat2, lon2) {
    const R = 6371; // 지구 반지름 (킬로미터)
    const dLat = (lat2 - lat1) * Math.PI / 180;
    const dLon = (lon2 - lon1) * Math.PI / 180;
    const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
        Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
        Math.sin(dLon / 2) * Math.sin(dLon / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c;
}

// 약국 아이템 활성화 함수 (ID를 파라미터로 받음)
function activatePharmacyItem(id) {
    const item = resultList.find(r => String(r.id) === String(id));
    if (!item) {
        console.error("activatePharmacyItem: Item with ID not found:", id);
        return;
    }

    console.log("Activating item:", item.name, "ID:", item.id);
    console.log("Current Active ID (before check):", currentActiveId ? currentActiveId : "None");

    if (currentActiveId && String(currentActiveId) === String(id)) {
        console.log("Re-clicking active item. Deactivating...");
        deactivatePharmacyItem();
        return;
    }

    originalPharmacyMarkers.forEach(m => m.setMap(null));
    item.marker.setMap(map);

    $('.pharmacy-item').removeClass('active');
    const $elToActivate = $(`div.pharmacy-item[data-id="${item.id}"]`);
    $elToActivate.addClass('active');

    map.panTo(item.latlng);

    if (openOverlay) openOverlay.setMap(null);
    item.overlay.setMap(map);
    openOverlay = item.overlay;

    currentActiveId = id;
    console.log("currentActiveId updated to:", currentActiveId);

    const $list = $('#pharmacy-list');
    if ($elToActivate.length > 0) {
        $list.animate({
            scrollTop: $elToActivate.offset().top - $list.offset().top + $list.scrollTop()
        }, 500);
    } else {
        console.warn("Pharmacy list item not found for scrolling:", item.name);
    }
}

// 약국 아이템 비활성화 함수
function deactivatePharmacyItem() {
    console.log("Deactivating current item and showing all markers.");
    $('.pharmacy-item').removeClass('active');

    originalPharmacyMarkers.forEach(m => m.setMap(map));

    if (openOverlay) {
        openOverlay.setMap(null);
        openOverlay = null;
    }
    currentActiveId = null;
    console.log("currentActiveId is now null.");
}


$(document).ready(function () {
    if (SS_USER == null) {
        alert("로그인 후 이용 가능합니다.");
        setReferrer();
        return;
    }

    // Kakao Maps SDK 로딩 완료 대기
    window.kakao.maps.load(function() {
        map = new kakao.maps.Map(document.getElementById('map'), {
            center: new kakao.maps.LatLng(37.5665, 126.9780), // 서울 시청 기본 위치
            level: currentLevel
        });

        if (navigator.geolocation) {
            navigator.geolocation.getCurrentPosition(function (pos) {
                const lat = pos.coords.latitude;
                const lng = pos.coords.longitude;
                userLocation = new kakao.maps.LatLng(lat, lng);
                map.setCenter(userLocation); // 사용자 위치로 지도 중심 이동

                const userMarkerImage = new kakao.maps.MarkerImage(
                    "https://t1.daumcdn.net/localimg/localimages/07/mapapidoc/marker_red.png", // 사용자 위치 마커 이미지
                    new kakao.maps.Size(40, 42),
                    { offset: new kakao.maps.Point(13, 42) }
                );

                new kakao.maps.Marker({
                    position: userLocation,
                    map: map,
                    title: "내 위치",
                    image: userMarkerImage
                });

                // 초기 로드시 사용자 위치 주변 2km 약국 검색 (allInPolygon = false)
                searchNearbyPharmacies(userLocation, null, false);
            }, function(error) { // Geolocation 실패 시 처리
                console.error("Geolocation error:", error);
                alert("위치 정보를 가져올 수 없습니다. 기본 위치로 지도를 표시합니다.");
                // Geolocation 실패 시에도 기본 위치 주변 2km 약국 검색 (allInPolygon = false)
                searchNearbyPharmacies(new kakao.maps.LatLng(37.5665, 126.9780), null, false);
            });
        } else {
            console.warn("Geolocation is not supported by this browser.");
            alert("이 브라우저에서는 위치 정보를 지원하지 않습니다. 기본 위치로 지도를 표시합니다.");
            // Geolocation 지원 안 할 시에도 기본 위치 주변 2km 약국 검색 (allInPolygon = false)
            searchNearbyPharmacies(new kakao.maps.LatLng(37.5665, 126.9780), null, false);
        }

        $('#regionSearchBtn').on('click', function () {
            clearMarkers();
            removePolygons();
            deactivatePharmacyItem();

            map.setLevel(13); // 시도 단위로 보기 위해 줌 레벨 조정
            map.setCenter(new kakao.maps.LatLng(36.5, 127.9)); // 전국 중심 정도로 이동
            loadPolygons("/geojson/sido.json"); // 시도 폴리곤 로드
            detailMode = false; // 시도 선택 모드
        });
    }); // window.kakao.maps.load 끝
});


// searchCenter: 검색의 중심 좌표 (LatLng 객체)
// searchPolygon: (선택 사항) 검색 결과를 필터링할 폴리곤 (Polygon 객체)
// allInPolygon: (추가 파라미터) 이 값이 true면, 폴리곤 내 모든 약국을 찾기 위해 검색 반경 제한 대신 bounds 사용
function searchNearbyPharmacies(searchCenter, searchPolygon = null, allInPolygon = false) {
    const places = new kakao.maps.services.Places();

    if (!userLocation && !searchCenter) {
        console.error("Cannot search pharmacies: No user location and no center provided.");
        return;
    }

    let options = {};
    options.query = "약국"; // 검색 키워드

    // 거리 계산의 기준점 (약국 리스트에 표시될 "몇 km"를 계산할 때 사용)
    // 일반적으로는 사용자 위치 기준이지만, 폴리곤 검색의 경우 해당 폴리곤의 중심점으로 대체될 수 있음.
    // `userLocation`이 있다면 계속 사용자 위치를 기준으로 거리를 계산합니다.
    // `userLocation`이 없으면 `searchCenter` (폴리곤 중심점)를 기준으로 합니다.
    const distCalcLat = userLocation ? userLocation.getLat() : searchCenter.getLat();
    const distCalcLng = userLocation ? userLocation.getLng() : searchCenter.getLng();


    // allInPolygon 플래그에 따라 검색 옵션 분기
    if (allInPolygon && searchPolygon && typeof searchPolygon.getBounds === 'function') {
        // 폴리곤 내 모든 약국을 찾을 때: 폴리곤의 경계 박스를 검색 범위로 사용
        options.bounds = searchPolygon.getBounds();
        console.log("searchNearbyPharmacies: Searching with polygon bounds.", options.bounds);
    } else {
        // 사용자 위치 주변 2km 약국을 찾을 때: 중심점과 반경 사용
        options.location = searchCenter;
        options.radius = 2000; // 2km 반경
        console.log("searchNearbyPharmacies: Searching with center and radius.", options.location.getLat(), options.location.getLng(), "radius:", options.radius);
    }

    places.keywordSearch(options.query, function (data, status) {
        if (status === kakao.maps.services.Status.OK) {
            resultList = [];
            pharmacyMarkers = [];
            originalPharmacyMarkers = [];
            deactivatePharmacyItem(); // 기존 활성 아이템 비활성화

            data.forEach(p => {
                const lat2 = parseFloat(p.y);
                const lng2 = parseFloat(p.x);
                const latlng = new kakao.maps.LatLng(lat2, lng2);

                // searchPolygon이 제공되고, 약국이 해당 폴리곤 내부에 없으면 건너뜜
                // (경계 박스로 검색했기 때문에 폴리곤 외부의 약국이 포함될 수 있음)
                if (searchPolygon && typeof searchPolygon.contains === 'function' && !searchPolygon.contains(latlng)) {
                    return; // 다음 약국으로 넘어감
                }

                const distance = getDistanceFromLatLonInKm(distCalcLat, distCalcLng, lat2, lng2);

                const marker = new kakao.maps.Marker({ map, position: latlng, zIndex: 100 });

                // 오버레이 컨텐츠 생성 (이전과 동일)
                const $contentDiv = $('<div>').addClass('custom-infowindow');
                const $link = $('<a>')
                    .attr('href', `https://map.kakao.com/link/map/${p.id}`)
                    .attr('target', '_blank')
                    .text(p.place_name);

                $link.on('click', function(e) {
                    e.stopPropagation();
                    e.stopImmediatePropagation();
                    const clickedId = $(this).attr('href').split('/').pop();
                    console.log("--- CustomOverlay link clicked. Extracted ID:", clickedId, "---");
                });

                $contentDiv.append($link);
                $contentDiv.css('pointer-events', 'none');
                $link.css('pointer-events', 'auto');

                const overlay = new kakao.maps.CustomOverlay({
                    position: latlng,
                    content: $contentDiv[0],
                    yAnchor: 1
                });

                originalPharmacyMarkers.push(marker); // 모든 약국 마커 저장

                kakao.maps.event.addListener(marker, 'click', function () {
                    console.log("--- Marker clicked:", p.place_name, "---");
                    activatePharmacyItem(p.id);
                });

                pharmacyMarkers.push(marker);
                resultList.push({ id: p.id, name: p.place_name, latlng, marker, overlay, distance: distance.toFixed(2) });
            });

            resultList.sort((a, b) => a.distance - b.distance); // 거리순 정렬
            renderPharmacyList();
        } else {
            console.warn("Keyword search failed or no results:", status);
            if (status === kakao.maps.services.Status.ZERO_RESULT) {
                alert("선택하신 지역에 약국을 찾을 수 없습니다.");
            } else if (status === kakao.maps.services.Status.ERROR) {
                alert("약국 검색 중 오류가 발생했습니다.");
            }
        }
    }, options); // options 객체를 세 번째 인자로 전달하는 것이 중요합니다!
}

function renderPharmacyList() {
    const $list = $('#pharmacy-list');
    $list.empty();

    resultList.forEach(item => {
        const $el = $(`<div class="pharmacy-item" data-id="${item.id}"><strong>${item.name}</strong><br>${item.distance} km</div>`);

        $el.on('click', function () {
            console.log("--- Pharmacy list item clicked:", item.name, "---");
            activatePharmacyItem(item.id);
        });
        $list.append($el);
    });
}

function loadPolygons(path) {
    $.getJSON(path, function (geojson) {
        // geojson.features 데이터 구조 확인 (시도와 시군구의 properties.name이 다를 수 있음)
        const areas = geojson.features.map(unit => {
            const coords = unit.geometry.coordinates[0];
            return {
                // GeoJSON 데이터에 따라 정확한 속성 이름을 사용해야 합니다.
                // 예를 들어, 시도: CTP_KOR_NM, 시군구: SIG_KOR_NM 일 수 있습니다.
                name: unit.properties.CTP_KOR_NM || unit.properties.SIG_KOR_NM,
                code: unit.properties.CTPRVN_CD || unit.properties.SIG_CD,
                path: coords.map(c => new kakao.maps.LatLng(c[1], c[0]))
            };
        });

        areas.forEach(area => {
            const kakaoMapPolygon = new kakao.maps.Polygon({
                map,
                path: area.path,
                strokeWeight: 2,
                strokeColor: '#004c80',
                fillColor: '#fff',
                fillOpacity: 0.7,
                clickable: false
            });

            area.kakaoPolygon = kakaoMapPolygon; // area 객체에 polygon 인스턴스를 직접 저장
            polygons.push(kakaoMapPolygon);

            const overlay = new kakao.maps.CustomOverlay({
                content: `<div class="area-label">${area.name}</div>`,
                map: null,
                xAnchor: 0.5,
                yAnchor: 1.5
            });

            let overlayVisible = true; // 마우스 오버 오버레이 가시성 제어

            kakao.maps.event.addListener(kakaoMapPolygon, 'mouseover', e => {
                if (!overlayVisible) return;
                kakaoMapPolygon.setOptions({ fillColor: '#09f' });
                overlay.setPosition(e.latLng);
                overlay.setMap(map);
            });

            kakao.maps.event.addListener(kakaoMapPolygon, 'mouseout', () => {
                kakaoMapPolygon.setOptions({ fillColor: '#fff' });
                overlay.setMap(null);
            });

            // 폴리곤 클릭 시 동작
            kakao.maps.event.addListener(kakaoMapPolygon, 'click', e => {
                console.log("Polygon clicked:", area.name);
                overlayVisible = false; // 클릭 후 마우스 오버 오버레이 비활성화
                overlay.setMap(null); // 혹시 열려있을 오버레이 닫기

                if (!detailMode) { // 현재 시도 선택 모드 (detailMode = false)
                    detailMode = true; // 시군구 선택 모드로 전환
                    map.setLevel(10); // 시군구를 보기 적합한 줌 레벨로
                    map.panTo(e.latLng); // 지도를 클릭된 시도 중심으로 이동
                    removePolygons(); // 기존 시도 폴리곤 제거
                    loadPolygons("/geojson/sig.json"); // 시군구 폴리곤 로드
                } else { // 현재 시군구 선택 모드 (detailMode = true)
                    map.setLevel(5); // 약국 마커를 보기 적합한 줌 레벨로
                    map.panTo(e.latLng); // 지도를 클릭된 시군구 중심으로 이동
                    removePolygons(); // 시군구 폴리곤 제거
                    clearMarkers(); // 기존 약국 마커 및 리스트 클리어

                    // 클릭된 시군구 폴리곤 내의 모든 약국 검색 (allInPolygon = true)
                    searchNearbyPharmacies(e.latLng, area.kakaoPolygon, true);
                }
            });
        });
    }).fail(function(jqxhr, textStatus, error) {
        console.error("Error loading GeoJSON from " + path + ": " + textStatus + ", " + error);
    });
}

function removePolygons() {
    polygons.forEach(p => p.setMap(null));
    polygons = [];
}

function clearMarkers() {
    pharmacyMarkers.forEach(m => m.setMap(null));
    pharmacyMarkers = [];
    originalPharmacyMarkers.forEach(m => m.setMap(null));
    originalPharmacyMarkers = [];
    $('#pharmacy-list').empty();
    deactivatePharmacyItem();
}