package common;

import burp.api.montoya.http.message.HttpRequestResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;




@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomizeRequestResult {
    private Integer id;
    private Boolean success;
    private HttpRequestResponse httpRequestResponse;
}
