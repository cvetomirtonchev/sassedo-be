package server.sassedo.common.service.helpertexts;

import org.springframework.lang.Nullable;
import server.sassedo.common.data.dto.HelperText;
import server.sassedo.model.GenericException;

import java.util.List;

public interface HelperTextsService {

    List<HelperText> getHelperTexts(@Nullable Long id) throws GenericException;


    HelperText updateHelperText(Long id, String value) throws GenericException;
}
