package checker;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public enum SSRFPayloadsSetting {
    INSTANCE;

    @Getter
    @Setter
    private List<String> payloads = new ArrayList<>(Arrays.asList(
            "collaborator", 
            "dnslog"   
    ));

    @Getter
    @Setter
    private List<String> checkStrings = new ArrayList<>();
}
