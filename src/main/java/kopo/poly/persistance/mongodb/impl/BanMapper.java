package kopo.poly.persistance.mongodb.impl;

import com.mongodb.client.MongoCollection;
import kopo.poly.dto.BanDTO;
import kopo.poly.persistance.mongodb.AbstractMongoDBComon;
import kopo.poly.persistance.mongodb.IBanMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BanMapper extends AbstractMongoDBComon implements IBanMapper {

    private final MongoTemplate mongodb;

    @Override
    public int insertBanInfo(String colNm, BanDTO pDTO) throws Exception {

        log.info("{}.insertBanInfo Start", this.getClass().getSimpleName());

        int res;

        if (super.createCollection(mongodb, colNm)) {
            log.info("{} 생성되었습니다.", colNm);
        }

        MongoCollection<Document> col = mongodb.getCollection(colNm);
        Document doc = new Document();
        doc.append("userId", pDTO.getUserId());
        doc.append("userName", pDTO.getUserName());
        doc.append("userEmail", pDTO.getUserEmail());
        doc.append("phoneNumber", pDTO.getPhoneNumber());
        doc.append("reason", pDTO.getReason());
        doc.append("banBy", pDTO.getBanBy());
        doc.append("banAt", pDTO.getBanAt());

        col.insertOne(doc);

        res = 1;

        return res;
    }

    @Override
    public List<BanDTO> getBanList(String colNm) throws Exception {
        return List.of();
    }
}
