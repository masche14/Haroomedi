package kopo.poly.persistance.mongodb;

import kopo.poly.dto.HRecordDTO;
import kopo.poly.dto.TilkoDTO;

public interface IHealthMapper {

    HRecordDTO getLatestRecord(HRecordDTO pDTO) throws Exception;

    int insertRecord(HRecordDTO pDTO) throws Exception;

}
