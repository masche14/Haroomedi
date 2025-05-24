package kopo.poly.persistance.mongodb.impl;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.UpdateResult;
import kopo.poly.dto.ChatDTO;
import kopo.poly.dto.ChatMessageDTO;
import kopo.poly.persistance.mongodb.AbstractMongoDBComon;
import kopo.poly.persistance.mongodb.IChatMapper;
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

}
