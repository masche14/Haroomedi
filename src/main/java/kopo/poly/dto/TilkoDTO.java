package kopo.poly.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Data
public class TilkoDTO {

    private String userId;

    @JsonProperty("PrivateAuthType")
    private String privateAuthType;

    @JsonProperty("UserName")
    private String userName;

    @JsonProperty("BirthDate")
    private String birthDate;

    @JsonProperty("UserCellphoneNumber")
    private String userCellphoneNumber;

    @JsonProperty("selectedImageSrc")
    private String selectedImageSrc;

    @JsonProperty("selectedImageAlt")
    private String selectedImageAlt;

    @JsonProperty("CxId")
    private String cxId;

    @JsonProperty("ReqTxId")
    private String reqTxId;

    @JsonProperty("Token")
    private String token;

    @JsonProperty("TxId")
    private String txId;
}
