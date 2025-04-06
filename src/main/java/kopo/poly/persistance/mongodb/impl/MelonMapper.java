package kopo.poly.persistance.mongodb.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import kopo.poly.dto.MelonDTO;
import kopo.poly.persistance.mongodb.AbstractMongoDBComon;
import kopo.poly.persistance.mongodb.IMelonMapper;
import kopo.poly.util.CmmUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class MelonMapper extends AbstractMongoDBComon implements IMelonMapper {

    private final MongoTemplate mongodb;

    @Override
    public int insertSong(List<MelonDTO> pList, String colNm) throws Exception {

        log.info("{}.insertSong Start", this.getClass().getName());

        int res;

        if (super.createCollection(mongodb, colNm, "collectTime")) {
            log.info("{} 생성되었습니다", colNm);
        }

        MongoCollection<Document> col = mongodb.getCollection(colNm);

        for (MelonDTO pDTO : pList) {
            col.insertOne(new Document(new ObjectMapper().convertValue(pDTO, Map.class)));
        }

        res = 1;

        log.info("{}.insertSong End", this.getClass().getName());

        return res;
    }

    @Override
    public List<MelonDTO> getSongList(String colNm) throws Exception {

        log.info("{}.getSongList Start!", this.getClass().getName());

        // 조회 결과를 받기 위한 객체 전체 생성
        List<MelonDTO> rList = new LinkedList<>();

        MongoCollection<Document> col = mongodb.getCollection(colNm);

        // 조회 결과 중 출력할 필드들(SQL의 SELECT절 FROM절 가로친 필드들만 유사함)
        Document projection = new Document();
        projection.append("song", "$song");
        projection.append("singer", "$singer");

        // MongoDB는 무조건 ObjectId가 자동생성되며, ObjectId를 사용하지 않을 때는 조회를 필요가 없음
        // ObjectId를 가지고 오고 싶지 않을 때는 0을 설정
        projection.append("_id", 0);

        // MongoDB의 find 명령어를 통해 조회할 경우 사용
        // 조회하려는 필드만 가지고 조회, 데이터양이 많은 경우 무조건 Aggregate 사용한다.
        FindIterable<Document> rs = col.find(new Document()).projection(projection);

        for (Document doc : rs) {
            String song = CmmUtil.nvl(doc.getString("song"));
            String singer = CmmUtil.nvl(doc.getString("singer"));

            log.info("song : {} / singer : {}", song, singer);

            MelonDTO rDTO = MelonDTO.builder().song(song).singer(singer).build();

            // 조회된 결과를 List에 저장하기
            rList.add(rDTO);
        }

        log.info("{}.getSongList End!", this.getClass().getName());

        return rList;
    }
}
