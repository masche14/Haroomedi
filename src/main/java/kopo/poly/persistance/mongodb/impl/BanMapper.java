package kopo.poly.persistance.mongodb.impl;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import kopo.poly.dto.BanDTO;
import kopo.poly.dto.UserInfoDTO;
import kopo.poly.persistance.mongodb.AbstractMongoDBComon;
import kopo.poly.persistance.mongodb.IBanMapper;
import kopo.poly.util.CmmUtil;
import kopo.poly.util.EncryptUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
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

        log.info("{}.getBanList Start", this.getClass().getSimpleName());

        if (super.createCollection(mongodb, colNm)) {
            log.info("{} 생성되었습니다.", colNm);
        }

        MongoCollection<Document> col = mongodb.getCollection(colNm);

        Document query = new Document();

        Document projection = new Document();
        projection.append("userId", "$userId");
        projection.append("userName", "$userName");
        projection.append("userEmail", "$userEmail");
        projection.append("phoneNumber", "$phoneNumber");
        projection.append("reason", "$reason");
        projection.append("banBy", "$banBy");
        projection.append("banAt", "$banAt");
        projection.append("_id", 0);

        FindIterable<Document> rs = col.find(query).projection(projection);

        List<BanDTO> rList = new ArrayList<BanDTO>();

        for (Document doc : rs) {
            if (doc != null) {
                BanDTO rDTO = new BanDTO();

                String userId = CmmUtil.nvl(doc.getString("userId"));
                String userName = CmmUtil.nvl(doc.getString("userName"));
                String userEmail = EncryptUtil.decAES128CBC(CmmUtil.nvl(doc.getString("userEmail")));
                String phoneNumber = EncryptUtil.decAES128CBC(CmmUtil.nvl(doc.getString("phoneNumber")));
                String reason = CmmUtil.nvl(doc.getString("reason"));
                String banBy = CmmUtil.nvl(doc.getString("banBy"));

                Object dateObj = doc.get("banAt");
                if (dateObj instanceof Date) {
                    rDTO.setBanAt((Date) dateObj);
                } else if (dateObj instanceof Long) {
                    rDTO.setBanAt(new Date((Long) dateObj));
                }

                rDTO.setUserId(userId);
                rDTO.setUserName(userName);
                rDTO.setUserEmail(userEmail);
                rDTO.setPhoneNumber(phoneNumber);
                rDTO.setReason(reason);
                rDTO.setBanBy(banBy);


                rList.add(rDTO);
            }
        }

        log.info("{}.getBanList End", this.getClass().getSimpleName());

        return rList;
    }

    @Override
    public BanDTO checkIfBaned(String colNm, UserInfoDTO pDTO) throws Exception {

        log.info("{}.getBanInfo Start", this.getClass().getSimpleName());

        if (super.createCollection(mongodb, colNm)) {
            log.info("{} 생성되었습니다.", colNm);
        }

        Query query = new Query(Criteria.where(pDTO.getFieldName()).is(pDTO.getValue()));
        query.fields().include("reason").exclude("_id");

        List<Document> rs = mongodb.find(query, Document.class, colNm);

        BanDTO rDTO = new BanDTO();

        for (Document doc : rs) {
            if (doc != null) {
                String existYn = "Y";
                String reason = CmmUtil.nvl(doc.getString("reason"));

                rDTO.setExistYn(existYn);
                rDTO.setReason(reason);
                break;
            }
        }

        log.info("{}.getBanInfo End", this.getClass().getSimpleName());

        return rDTO;
    }

    @Override
    public int cancelBan(String colNm, BanDTO pDTO) throws Exception {

        log.info("{}.cancelBan Start", this.getClass().getSimpleName());

        int res = 0;

        MongoCollection<Document> col = mongodb.getCollection(colNm);

        // 삭제 조건 (userId 기준)
        String userId = CmmUtil.nvl(pDTO.getUserId());

        // 실제 삭제 실행 (딱 하나만)
        long deletedCount = col.deleteOne(Filters.eq("userId", userId)).getDeletedCount();

        log.info("Deleted user count: {}", deletedCount);

        log.info("{}.deleteUserInfo End", this.getClass().getSimpleName());

        if ((int) deletedCount>0) {
            res = 1;
        }

        log.info("{}.cancelBan End", this.getClass().getSimpleName());

        return res;
    }
}
