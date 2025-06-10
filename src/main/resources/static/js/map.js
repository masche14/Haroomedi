let map, userLocation, openOverlay = null;
let polygons = [];
let detailMode = false;
let currentLevel = 5;
let pharmacyMarkers = [], resultList = [];

function getDistanceFromLatLonInKm(lat1, lon1, lat2, lon2) {
    const R = 6371;
    const dLat = (lat2 - lat1) * Math.PI / 180;
    const dLon = (lon2 - lon1) * Math.PI / 180;
    const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
        Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
        Math.sin(dLon / 2) * Math.sin(dLon / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c;
}

$(document).ready(function () {
    if (SS_USER == null) {
        alert("로그인 후 이용 가능합니다.");
        setReferrer();
        return;
    }

    map = new kakao.maps.Map(document.getElementById('map'), {
        center: new kakao.maps.LatLng(37.5665, 126.9780),
        level: currentLevel
    });

    if (navigator.geolocation) {
        navigator.geolocation.getCurrentPosition(function (pos) {
            const lat = pos.coords.latitude;
            const lng = pos.coords.longitude;
            userLocation = new kakao.maps.LatLng(lat, lng);
            map.setCenter(userLocation);

            const userMarkerImage = new kakao.maps.MarkerImage(
                "https://t1.daumcdn.net/localimg/localimages/07/mapapidoc/marker_red.png",
                new kakao.maps.Size(40, 42),
                { offset: new kakao.maps.Point(13, 42) }
            );

            new kakao.maps.Marker({
                position: userLocation,
                map: map,
                title: "내 위치",
                image: userMarkerImage
            });

            searchNearbyPharmacies(userLocation);
        });
    }

    $('#regionSearchBtn').on('click', function () {
        clearMarkers();         // 마커 제거
        removePolygons();       // 폴리곤 제거

        if (openOverlay) {
            openOverlay.setMap(null); // ✅ 오버레이 제거
            openOverlay = null;
        }

        map.setLevel(13);
        map.setCenter(new kakao.maps.LatLng(36.5, 127.9)); // 전국 중심
        loadPolygons("/geojson/sido.json");
        detailMode = false;
    });
});

function searchNearbyPharmacies(center) {
    const places = new kakao.maps.services.Places();

    if (!userLocation) return; // 사용자 위치가 없으면 종료

    const lat1 = userLocation.getLat(); // ✅ 무조건 사용자 위치 기준
    const lng1 = userLocation.getLng();

    places.keywordSearch("약국", function (data, status) {
        if (status === kakao.maps.services.Status.OK) {
            resultList = [];
            data.forEach(p => {
                const lat2 = parseFloat(p.y);
                const lng2 = parseFloat(p.x);
                const distance = getDistanceFromLatLonInKm(lat1, lng1, lat2, lng2);

                const latlng = new kakao.maps.LatLng(lat2, lng2);
                const marker = new kakao.maps.Marker({ map, position: latlng });

                const overlay = new kakao.maps.CustomOverlay({
                    position: latlng,
                    content: `<div class="custom-infowindow"><a href="https://map.kakao.com/link/map/${p.id}" target="_blank">${p.place_name}</a></div>`,
                    yAnchor: 1
                });

                kakao.maps.event.addListener(marker, 'click', function () {
                    if (openOverlay) openOverlay.setMap(null);
                    overlay.setMap(map);
                    openOverlay = overlay;
                });

                pharmacyMarkers.push(marker);
                resultList.push({ name: p.place_name, latlng, marker, overlay, distance: distance.toFixed(2) });
            });

            resultList.sort((a, b) => a.distance - b.distance);
            renderPharmacyList();
        }
    }, {
        location: center, // 🔸 지도에 표시할 중심은 클릭한 구역이지만
        radius: 2000       // 🔹 거리 계산 기준은 userLocation
    });
}

function renderPharmacyList() {
    const $list = $('#pharmacy-list');
    $list.empty();

    resultList.forEach(item => {
        const $el = $(`<div class="pharmacy-item"><strong>${item.name}</strong><br>${item.distance} km</div>`);
        $el.on('click', function () {
            $('.pharmacy-item').removeClass('active');
            $el.addClass('active');
            map.panTo(item.latlng);
            if (openOverlay) openOverlay.setMap(null);
            item.overlay.setMap(map);
            openOverlay = item.overlay;
        });
        $list.append($el);
    });
}

function loadPolygons(path) {
    $.getJSON(path, function (geojson) {
        const areas = geojson.features.map(unit => {
            const coords = unit.geometry.coordinates[0];
            return {
                name: unit.properties.SIG_KOR_NM,
                code: unit.properties.SIG_CD,
                path: coords.map(c => new kakao.maps.LatLng(c[1], c[0]))
            };
        });

        areas.forEach(area => {
            const polygon = new kakao.maps.Polygon({
                map,
                path: area.path,
                strokeWeight: 2,
                strokeColor: '#004c80',
                fillColor: '#fff',
                fillOpacity: 0.7
            });

            polygons.push(polygon);

            const overlay = new kakao.maps.CustomOverlay({
                content: `<div class="area-label">${area.name}</div>`,
                map: null,
                xAnchor: 0.5,
                yAnchor: 1.5
            });

            let overlayVisible = true; // 🟢 해당 polygon에 대한 label 표시 여부

            kakao.maps.event.addListener(polygon, 'mouseover', e => {
                if (!overlayVisible) return;
                polygon.setOptions({ fillColor: '#09f' });
                overlay.setPosition(e.latLng);
                overlay.setMap(map);
            });

            kakao.maps.event.addListener(polygon, 'mouseout', () => {
                polygon.setOptions({ fillColor: '#fff' });
                overlay.setMap(null);
            });

            kakao.maps.event.addListener(polygon, 'click', e => {
                overlayVisible = false; // 🟢 클릭 시 이후 label 비표시
                overlay.setMap(null);

                if (!detailMode) {
                    detailMode = true;
                    map.setLevel(10);
                    map.panTo(e.latLng);
                    removePolygons();
                    loadPolygons("/geojson/sig.json");
                } else {
                    map.setLevel(5);
                    map.panTo(e.latLng);
                    removePolygons();
                    clearMarkers();
                    searchNearbyPharmacies(e.latLng);
                }
            });
        });
    });
}

function removePolygons() {
    polygons.forEach(p => p.setMap(null));
    polygons = [];
}

function clearMarkers() {
    pharmacyMarkers.forEach(m => m.setMap(null));
    pharmacyMarkers = [];
    $('#pharmacy-list').empty();
}
