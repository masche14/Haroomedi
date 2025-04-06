package kopo.poly.persistance.mongodb;

import kopo.poly.dto.MongoDTO;

public interface IMongoMapper {

    int insertData(MongoDTO pDTO, String colNm) throws Exception;
}
