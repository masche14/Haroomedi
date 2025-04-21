package kopo.poly.persistance.mongodb.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.UpdateResult;
import kopo.poly.dto.UserInfoDTO;
import kopo.poly.persistance.mongodb.AbstractMongoDBComon;
import kopo.poly.persistance.mongodb.IMelonMapper;
import kopo.poly.persistance.mongodb.IUserInfoMapper;
import kopo.poly.util.CmmUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserInfoMapper extends AbstractMongoDBComon implements IUserInfoMapper {

    private final MongoTemplate mongodb;

    @Override
    public int insertUserInfo(String colNm, UserInfoDTO pDTO) throws Exception {

        log.info("{}.insertUserInfo Start", this.getClass().getSimpleName());

        int res;

        if (super.createCollection(mongodb, colNm)) {
            log.info("{} 생성되었습니다.", colNm);
        }

        MongoCollection<Document> col = mongodb.getCollection(colNm);

        col.insertOne(new Document(new ObjectMapper().convertValue(pDTO, Map.class)));

        res = 1;

        log.info("{}.insertUserInfo End", this.getClass().getSimpleName());

        return res;
    }

    @Override
    public boolean checkFieldExists(String colNm, UserInfoDTO pDTO) {
        Query query = new Query(Criteria.where(pDTO.fieldName()).is(pDTO.value()));
        return mongodb.exists(query, colNm);
    }

//    @Override
//    public UserInfoDTO getUserIdExists(String colNm, UserInfoDTO pDTO) throws Exception {
//        return null;
//    }
//
//    @Override
//    public UserInfoDTO getUserNicknameExists(String colNm, UserInfoDTO pDTO) throws Exception {
//        return null;
//    }
//
//    @Override
//    public UserInfoDTO getUserEmailExists(String colNm, UserInfoDTO pDTO) throws Exception {
//        return null;
//    }

    @Override
    public UserInfoDTO getLogin(String colNm, UserInfoDTO pDTO) throws Exception {

        log.info("{}.getLogin Start", this.getClass().getSimpleName());

        if (super.createCollection(mongodb, colNm)) {
            log.info("{} 생성되었습니다.", colNm);
        }

        UserInfoDTO rDTO = null;

        MongoCollection<Document> col = mongodb.getCollection(colNm);

        Document query = new Document();
        query.append("userId", CmmUtil.nvl(pDTO.userId()));

        Document projection = new Document();
        projection.append("userId", "$userId");
        projection.append("password", "$password");
        projection.append("_id", 0);

        FindIterable<Document> rs = col.find(query).projection(projection);

        for (Document doc : rs) {
            if (doc != null) {
                rDTO = UserInfoDTO.builder()
                        .userId(doc.getString("userId"))
                        .password(doc.getString("password"))
                        .build();
                break; // 어차피 하나만 찾을 거니까 첫 번째에서 바로 종료
            }
        }

        return rDTO;
    }

    @Override
    public int updateUserInfo(String colNm, UserInfoDTO pDTO) throws Exception {
        Query query = new Query(Criteria.where("userId").is(pDTO.userId())); // 수정 기준

        Update updateFields = new Update();

        // null 체크 후 업데이트 대상에 추가
        if (pDTO.password() != null && !pDTO.password().isBlank()) {
            updateFields.set("password", pDTO.password());
        }
        if (pDTO.userName() != null && !pDTO.userName().isBlank()) {
            updateFields.set("userName", pDTO.userName());
        }
        if (pDTO.userEmail() != null && !pDTO.userEmail().isBlank()) {
            updateFields.set("userEmail", pDTO.userEmail());
        }
        if (pDTO.userNickname() != null && !pDTO.userNickname().isBlank()) {
            updateFields.set("userNickname", pDTO.userNickname());
        }
        if (pDTO.gender() != null && !pDTO.gender().isBlank()) {
            updateFields.set("gender", pDTO.gender());
        }
        if (pDTO.chgId() != null) {
            updateFields.set("chgId", pDTO.chgId());
        }
        if (pDTO.chgDt() != null) {
            updateFields.set("chgDt", pDTO.chgDt());
        }

        // int는 primitive 타입이라 0이 기본값 → 0이 아니면 수정 대상
        if (pDTO.authNumber() != 0) {
            updateFields.set("authNumber", pDTO.authNumber());
        }

        if (updateFields.getUpdateObject().isEmpty()) {
            return 0; // 수정할 값 없음
        }

        UpdateResult result = mongodb.updateFirst(query, updateFields, colNm);
        return (int) result.getModifiedCount();
    }

    @Override
    public int deleteUserInfo(String colNm, UserInfoDTO pDTO) throws Exception {
        return 0;
    }
}
