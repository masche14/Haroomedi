let openOverlay = null;

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

    if (navigator.geolocation) {
        navigator.geolocation.getCurrentPosition(function (position) {
            const lat = position.coords.latitude;
            const lng = position.coords.longitude;
            const userLocation = new kakao.maps.LatLng(lat, lng);

            const container = $('#map')[0];
            const map = new kakao.maps.Map(container, { center: userLocation, level: 5 });

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

            searchNearbyPharmacies(map, userLocation, lat, lng);
        });
    }

    function renderPharmacyList(resultList, map) {
        const $list = $('#pharmacy-list');
        $list.empty();

        resultList.forEach((item) => {
            const $item = $(`<div class="pharmacy-item"><strong>${item.name}</strong><br>${item.distance} km</div>`);
            item.$element = $item;

            $item.on('click', function () {
                $('.pharmacy-item').removeClass('active');
                $item.addClass('active');
                map.setCenter(item.latlng);
                if (openOverlay) openOverlay.setMap(null);
                item.overlay.setMap(map);
                openOverlay = item.overlay;
            });

            $list.append($item);
        });
    }

    function searchNearbyPharmacies(map, userLocation, userLat, userLng) {
        const places = new kakao.maps.services.Places();
        const resultList = [];

        places.keywordSearch('약국', function (results, status) {
            if (status === kakao.maps.services.Status.OK) {
                results.forEach((pharmacy) => {
                    const pLat = parseFloat(pharmacy.y);
                    const pLng = parseFloat(pharmacy.x);
                    const pLatLng = new kakao.maps.LatLng(pLat, pLng);
                    const distance = getDistanceFromLatLonInKm(userLat, userLng, pLat, pLng);

                    if (distance <= 1.0) {
                        const marker = new kakao.maps.Marker({ map: map, position: pLatLng });

                        const overlayContent = `
                            <div class='custom-infowindow'>
                                <a href="https://map.kakao.com/link/map/${pharmacy.id}" target="_blank" style="text-decoration:none; color:#000;">
                                    <span>${pharmacy.place_name}</span>
                                </a>
                            </div>`;

                        const overlay = new kakao.maps.CustomOverlay({
                            position: pLatLng,
                            content: overlayContent,
                            yAnchor: 1
                        });

                        const item = {
                            name: pharmacy.place_name,
                            distance: distance.toFixed(2),
                            latlng: pLatLng,
                            marker: marker,
                            overlay: overlay
                        };

                        kakao.maps.event.addListener(marker, 'click', function () {
                            $('.pharmacy-item').removeClass('active');
                            if (item.$element) {
                                item.$element.addClass('active');
                                $('#pharmacy-list').animate({
                                    scrollTop: item.$element.position().top + $('#pharmacy-list').scrollTop()
                                }, 300);
                            }
                            if (openOverlay) openOverlay.setMap(null);
                            overlay.setMap(map);
                            openOverlay = overlay;
                        });

                        resultList.push(item);
                    }
                });

                resultList.sort((a, b) => a.distance - b.distance);
                renderPharmacyList(resultList, map);
            }
        }, { location: userLocation, radius: 1000 });
    }
});
