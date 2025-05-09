package kopo.poly.persistance.mongodb;

import kopo.poly.dto.HRecordDTO;
import kopo.poly.dto.PrescriptionDTO;
import kopo.poly.dto.TilkoDTO;

public interface IHealthMapper {

    PrescriptionDTO getLatestRecord(TilkoDTO pDTO) throws Exception;

    int insertRecord(PrescriptionDTO pDTO) throws Exception;

}
