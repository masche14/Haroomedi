package kopo.poly.persistance.mongodb.impl;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.UpdateResult;
import kopo.poly.dto.PrescriptionDTO;
import kopo.poly.dto.ReminderDTO;
import kopo.poly.dto.UserInfoDTO;
import kopo.poly.persistance.mongodb.AbstractMongoDBComon;
import kopo.poly.persistance.mongodb.IReminderMapper;
import kopo.poly.util.CmmUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.types.ObjectId;
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
public class ReminderMapper extends AbstractMongoDBComon implements IReminderMapper {

    private final MongoTemplate mongodb;

    @Override
    public int insertReminder(String colNm, ReminderDTO pDTO) throws Exception {
        log.info("{}.insertReminder Start", this.getClass().getSimpleName());

        int res;

        if (super.createCollection(mongodb, colNm)) {
            log.info("{} 생성되었습니다.", colNm);
        }

        MongoCollection<Document> col = mongodb.getCollection(colNm);

        // MongoDB에 넣을 Document로 변환
        Document doc = new Document();
        doc.append("prescriptionId", pDTO.getPrescriptionId());
        doc.append("userId", pDTO.getUserId());
        doc.append("mealTime", pDTO.getMealTime());
        doc.append("toIntakeCnt", pDTO.getToIntakeCnt());
        doc.append("dailyToIntakeCnt", pDTO.getDailyToIntakeCnt());
        doc.append("intakeCnt", pDTO.getIntakeCnt());
        doc.append("leftIntakeCnt", pDTO.getLeftIntakeCnt());
        doc.append("intakeLog", pDTO.getIntakeLog());

        // Insert 실행
        col.insertOne(doc);

        res = 1;

        log.info("{}.insertReminder End", this.getClass().getSimpleName());

        return res;
    }

    @Override
    public int deleteReminder(String colNm, PrescriptionDTO pDTO) throws Exception {

        log.info("{}.deleteReminder Start", this.getClass().getSimpleName());

        MongoCollection<Document> col = mongodb.getCollection(colNm);

        String prescriptionId = CmmUtil.nvl(pDTO.getPrescriptionId());

        long deletedCount = col.deleteOne(Filters.eq("prescriptionId", prescriptionId)).getDeletedCount();

        log.info("Deleted user count: {}", deletedCount);
        log.info("{}.deleteUserInfo End", this.getClass().getSimpleName());

        return (int) deletedCount;
    }

    @Override
    public List<ReminderDTO> getReminderListToSend(String colNm, Date start, Date end) throws Exception {
        log.info("{}.getReminderListToSend Start", this.getClass().getSimpleName());

        // intakeLog 배열 안에서 intakeTime이 현재 시간 범위에 속하고 intakeYn이 N인 문서 조회
        Query query = new Query(Criteria.where("intakeLog")
                .elemMatch(Criteria.where("intakeTime").gte(start).lt(end).and("intakeYn").is("N")));

        List<ReminderDTO> rList = mongodb.find(query, ReminderDTO.class, colNm);

        log.info("조회된 알림 대상 수 : {}", rList.size());
        log.info("{}.getReminderListToSend End", this.getClass().getSimpleName());

        return rList;
    }

    @Override
    public List<ReminderDTO> getReminderListWithLeftIntake(String colNm) throws Exception {
        log.info("{}.getReminderListWithLeftIntake Start", this.getClass().getSimpleName());

        // 컬렉션 가져오기
        MongoCollection<Document> col = mongodb.getCollection(colNm);

        // leftIntakeCnt가 0보다 큰 문서만 필터링
        Document query = new Document("leftIntakeCnt", new Document("$gt", 0));

        // 필요한 필드만 조회하도록 projection 설정
        Document projection = new Document("prescriptionId", 1)
                .append("userId", 1)
                .append("mealTime", 1);

        FindIterable<Document> docs = col.find(query).projection(projection);

        List<ReminderDTO> rList = new ArrayList<>();

        for (Document doc : docs) {
            ReminderDTO rDTO = new ReminderDTO();

            rDTO.setPrescriptionId(doc.getString("prescriptionId"));
            rDTO.setUserId(doc.getString("userId"));

            List<String> mealTime = doc.getList("mealTime", String.class);
            rDTO.setMealTime(mealTime);

            rList.add(rDTO);
        }

        log.info("조회된 건수 : {}", rList.size());
        log.info("{}.getReminderListWithLeftIntake End", this.getClass().getSimpleName());

        return rList;
    }


    @Override
    public int appendIntakeLog(String colNm, ReminderDTO pDTO) throws Exception {

        log.info("{}.appendIntakeLog Start", this.getClass().getSimpleName());

        int res = 0;

        MongoCollection<Document> col = mongodb.getCollection(colNm);

        // prescriptionId는 일반 String 필드로 저장되어 있으므로 그대로 사용
        String prescriptionId = CmmUtil.nvl(pDTO.getPrescriptionId());

        Document update = new Document("$push", new Document("intakeLog", new Document("$each", pDTO.getIntakeLog())));

        UpdateResult result = col.updateOne(Filters.eq("prescriptionId", prescriptionId), update);

        if (result.getMatchedCount() == 0) {
            log.warn("업데이트 대상 문서가 존재하지 않습니다. prescriptionId: {}", prescriptionId);
            return res;
        }

        res = 1;

        log.info("{}.appendIntakeLog End", this.getClass().getSimpleName());

        return res;
    }

    @Override
    public int updateMealTime(String colNm, UserInfoDTO pDTO) throws Exception {

        log.info("{}.updateMealTime Start", this.getClass().getSimpleName());

        int res = 0;

        MongoCollection<Document> col = mongodb.getCollection(colNm);

        String userId = pDTO.getOrgId();
        List<String> mealTime = pDTO.getMealTime();

        Document query = new Document("userId", userId);
        Document update = new Document("$set", new Document("mealTime", mealTime));

        UpdateResult result = col.updateMany(query, update);

        int toUpdateCnt = (int) result.getMatchedCount();
        int updatedCnt = (int) result.getModifiedCount();

        log.info("업데이트 대상 문서 수 : {}", toUpdateCnt);
        log.info("실제 업데이트된 문서 수 : {}", updatedCnt);

        if(toUpdateCnt == updatedCnt) {
            res = 1;
        }

        log.info("{}.updateMealTimeByUserId End", this.getClass().getSimpleName());

        return res;
    }

    @Override
    public int updateUserId(String colNm, UserInfoDTO pDTO) throws Exception {

        log.info("{}.updateMealTime Start", this.getClass().getSimpleName());

        int res = 0;

        MongoCollection<Document> col = mongodb.getCollection(colNm);

        String userId = pDTO.getOrgId();
        String updateUserId = pDTO.getUserId();

        Document query = new Document("userId", userId);
        Document update = new Document("$set", new Document("userId", updateUserId));

        UpdateResult result = col.updateMany(query, update);

        int toUpdateCnt = (int) result.getMatchedCount();
        int updatedCnt = (int) result.getModifiedCount();

        log.info("업데이트 대상 문서 수 : {}", toUpdateCnt);
        log.info("실제 업데이트된 문서 수 : {}", updatedCnt);

        if(toUpdateCnt == updatedCnt) {
            res = 1;
        }

        log.info("{}.updateMealTimeByUserId End", this.getClass().getSimpleName());

        return res;
    }
}
