const imageBox = document.getElementById('imageBox'); // 이미지 박스를 선택
const fileInput = document.getElementById('fileInput'); // 파일 입력 요소를 선택
const previewImage = document.getElementById('previewImage'); // 미리보기 이미지 요소 선택

// 이미지 박스를 클릭하면 파일 입력창을 연다
imageBox.addEventListener('click', () => {
    fileInput.click(); // 이미지 박스를 클릭하면 fileInput이 클릭되도록 트리거
});

// 파일이 선택되면 이미지 미리보기를 업데이트하고 박스 크기를 조정
fileInput.addEventListener('change', function(event) {
    const file = event.target.files[0]; // 사용자가 선택한 첫 번째 파일을 가져옴
    if (file && file.type.startsWith('image/')) { // 파일이 존재하고, 이미지 파일인지 확인
        const reader = new FileReader(); // FileReader 객체를 사용해 파일을 읽음
        reader.onload = function (e) {
            previewImage.src = e.target.result; // 읽은 파일의 URL을 미리보기 이미지의 src에 할당
            previewImage.style.display = 'block'; // 미리보기 이미지가 표시되도록 설정
            imageBox.querySelector('p').style.display = 'none'; // 이미지 첨부 안내 문구를 숨김

            // 이미지가 로드되면 박스 및 이미지 크기 조정
            previewImage.onload = function() {
                // 이미지의 원래 너비와 높이를 가져옴
                const imageWidth = previewImage.naturalWidth;
                const imageHeight = previewImage.naturalHeight;

                // 박스의 최대 크기 (width: 500px, height: 400px)
                const boxMaxWidth = 500;
                const boxMaxHeight = 400;

                // 이미지의 비율을 유지하면서 박스 크기에 맞춰 조정할 최종 크기를 계산
                let finalWidth, finalHeight;

                // 가로가 세로보다 길 경우, 가로에 맞추고 세로를 비율에 맞게 조정
                if (imageWidth / imageHeight > boxMaxWidth / boxMaxHeight) {
                    finalWidth = boxMaxWidth; // 가로는 박스 최대 크기에 맞춤
                    finalHeight = (imageHeight / imageWidth) * finalWidth; // 세로는 비율에 맞춰 조정
                } else {
                    // 세로가 더 길 경우, 세로에 맞추고 가로를 비율에 맞게 조정
                    finalHeight = boxMaxHeight; // 세로는 박스 최대 크기에 맞춤
                    finalWidth = (imageWidth / imageHeight) * finalHeight; // 가로는 비율에 맞춰 조정
                }

                // 계산된 최종 크기로 이미지 크기를 설정
                previewImage.style.width = finalWidth + 'px';
                previewImage.style.height = finalHeight + 'px';

                // 이미지 크기에 맞춰 박스 크기도 조정
                imageBox.style.width = finalWidth + 'px';
                imageBox.style.height = finalHeight + 'px';
            };
        };
        reader.readAsDataURL(file); // FileReader가 파일을 읽어서 base64 인코딩 데이터 URL을 만듦
    }
});
