package kopo.poly.persistance.mongodb.impl;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import kopo.poly.dto.PrescriptionDTO;
import kopo.poly.dto.ReminderDTO;
import kopo.poly.persistance.mongodb.AbstractMongoDBComon;
import kopo.poly.persistance.mongodb.IReminderMapper;
import kopo.poly.util.CmmUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

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
}
