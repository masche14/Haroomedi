package kopo.poly.persistance.mongodb.impl;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.UpdateResult;
import kopo.poly.dto.ChatDTO;
import kopo.poly.dto.ChatMessageDTO;
import kopo.poly.dto.UserInfoDTO;
import kopo.poly.persistance.mongodb.AbstractMongoDBComon;
import kopo.poly.persistance.mongodb.IChatMapper;
import kopo.poly.util.CmmUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMapper extends AbstractMongoDBComon implements IChatMapper {

    private final MongoTemplate mongodb;

    @Override
    public int insertChatSession(String colNm, ChatDTO chatDTO) throws Exception {

        log.info("{}.insertChatSession Start", this.getClass().getSimpleName());

        if (super.createCollection(mongodb, colNm)) {
            log.info("{} 컬렉션이 생성되었습니다.", colNm);
        }

        int res = 0;

        String newSessionId = UUID.randomUUID().toString();
        chatDTO.setSessionId(newSessionId);

        Document doc = new Document();
        doc.append("sessionId", chatDTO.getSessionId());
        doc.append("userId", chatDTO.getUserId());
        doc.append("startAt", chatDTO.getStartAt());
        doc.append("endAt", chatDTO.getEndAt());
        doc.append("summary", chatDTO.getSummary());

        // ✅ ChatMessageDTO → Document 리스트로 변환
        List<Document> messageDocs = new ArrayList<>();
        if (chatDTO.getMessages() != null) {
            for (ChatMessageDTO msg : chatDTO.getMessages()) {
                Document msgDoc = new Document();
                msgDoc.append("sender", msg.getSender());
                msgDoc.append("content", msg.getContent());
                msgDoc.append("timestamp", msg.getTimestamp());
                messageDocs.add(msgDoc);
            }
        }

        doc.append("messages", messageDocs);

        MongoCollection<Document> col = mongodb.getCollection(colNm);
        col.insertOne(doc);

        res = 1;

        log.info("{}.insertChatSession End", this.getClass().getSimpleName());

        return res;
    }


    @Override
    public int appendMessage(String colNm, ChatMessageDTO msgDTO) throws Exception {

        log.info("{}.appendMessage Start", this.getClass().getSimpleName());

        if (super.createCollection(mongodb, colNm)) {
            log.info("{} 컬렉션이 생성되었습니다.", colNm);
        }

        int res = 0;

        Query query = new Query(Criteria.where("sessionId").is(msgDTO.getSessionId()));
        Update update = new Update()
                .push("messages", msgDTO)
                .set("endAt", msgDTO.getTimestamp());

        UpdateResult result = mongodb.updateFirst(query, update, colNm);

        res = (int) result.getModifiedCount();

        log.info("{}.appendMessage End", this.getClass().getSimpleName());

        return res;
    }

    @Override
    public ChatDTO getChatBySessionId(String colNm, String sessionId) throws Exception {

        if (super.createCollection(mongodb, colNm)) {
            log.info("{} 컬렉션이 생성되었습니다.", colNm);
        }

        log.info("{}.getChatBySessionId Start", this.getClass().getSimpleName());

        ChatDTO rDTO = new ChatDTO();

        Query query = new Query(Criteria.where("sessionId").is(sessionId));
        rDTO = mongodb.findOne(query, ChatDTO.class, colNm);

        log.info("{}.getChatBySessionId End", this.getClass().getSimpleName());

        return rDTO;
    }

    @Override
    public List<ChatDTO> getChatListByUserId(String colNm, String userId) throws Exception {
        log.info("{}.getChatListByUserId Start", this.getClass().getSimpleName());

        if (super.createCollection(mongodb, colNm)) {
            log.info("{} 컬렉션이 생성되었습니다.", colNm);
        }

        Query query = new Query(Criteria.where("userId").is(userId))
                .with(Sort.by(Sort.Direction.DESC, "endAt"));

        query.fields().exclude("messages"); // 이 부분을 이렇게 분리해서 사용

        List<ChatDTO> rList = mongodb.find(query, ChatDTO.class, colNm);

        log.info("{}.getChatListByUserId End", this.getClass().getSimpleName());
        return rList;
    }

    @Override
    public List<ChatMessageDTO> getChatMessageList(String colNm, String sessionId) throws Exception {

        log.info("{}.getChatMessageList Start", this.getClass().getSimpleName());

        if (super.createCollection(mongodb, colNm)) {
            log.info("{} 컬렉션이 생성되었습니다.", colNm);
        }

        Query query = new Query(Criteria.where("sessionId").is(sessionId));
        query.fields().include("messages"); // 메시지만 가져옴

        MongoCollection<Document> col = mongodb.getCollection(colNm);

        Document doc = col.find(query.getQueryObject()).first();
        List<ChatMessageDTO> rList = new ArrayList<>();

        if (doc != null && doc.containsKey("messages")) {
            List<Document> msgDocs = (List<Document>) doc.get("messages");

            for (Document msg : msgDocs) {
                ChatMessageDTO dto = new ChatMessageDTO();
                dto.setSender(msg.getString("sender"));
                dto.setContent(msg.getString("content"));
                dto.setTimestamp(msg.getDate("timestamp"));

                rList.add(dto);
            }
        }

        log.info("{}.getChatMessageList End", this.getClass().getSimpleName());

        return rList;
    }

    @Override
    public int deleteAllChat(String colNm, UserInfoDTO pDTO) throws Exception {

        log.info("{}.deleteAllChat Start", this.getClass().getSimpleName());

        // Mongo 컬렉션 가져오기
        MongoCollection<Document> col = mongodb.getCollection(colNm);

        // 삭제 조건 (userId 기준)
        String userId = CmmUtil.nvl(pDTO.getUserId());

        // 실제 삭제 실행 (딱 하나만)
        long deletedCount = col.deleteMany(Filters.eq("userId", userId)).getDeletedCount();

        log.info("Deleted Prescription count: {}", deletedCount);

        log.info("{}.deleteAllChat End", this.getClass().getSimpleName());

        return (int) deletedCount;
    }

    @Override
    public int deleteChat(String colNm, ChatDTO pDTO) throws Exception {
        log.info("{}.deleteChat Start", this.getClass().getSimpleName());

        // Mongo 컬렉션 가져오기
        MongoCollection<Document> col = mongodb.getCollection(colNm);

        // 삭제 조건 (userId 기준)
        String sessionId = CmmUtil.nvl(pDTO.getSessionId());

        // 실제 삭제 실행 (딱 하나만)
        long deletedCount = col.deleteOne(Filters.eq("sessionId", sessionId)).getDeletedCount();

        log.info("Deleted Prescription count: {}", deletedCount);

        log.info("{}.deleteAllChat End", this.getClass().getSimpleName());

        return (int) deletedCount;
    }
}
