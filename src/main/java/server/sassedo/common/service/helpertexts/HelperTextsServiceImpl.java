package server.sassedo.common.service.helpertexts;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import server.sassedo.common.data.dto.HelperText;
import server.sassedo.common.data.dto.HelperTextType;
import server.sassedo.common.repository.HelperTextsRepository;
import server.sassedo.model.GenericException;
import server.sassedo.model.GenericExceptionCode;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Objects;

@Service
public class HelperTextsServiceImpl implements HelperTextsService {

    @Autowired
    private HelperTextsRepository helperTextsRepository;

    @PostConstruct
    public void init() {
        long count = helperTextsRepository.count();
        if (count == 0) {
            helperTextsRepository.save(new HelperText(HelperTextType.TERMS_AND_CONDITIONS.id, ""));
            helperTextsRepository.save(new HelperText(HelperTextType.PRIVACY_POLICY.id, ""));
            helperTextsRepository.save(new HelperText(HelperTextType.GDPR.id, ""));
            helperTextsRepository.save(new HelperText(HelperTextType.WHO_WE_ARE.id, ""));
            helperTextsRepository.save(new HelperText(HelperTextType.OUR_TEAM.id, ""));
            helperTextsRepository.save(new HelperText(HelperTextType.MAIN_TITLE.id, ""));
            helperTextsRepository.save(new HelperText(HelperTextType.MAIN_TEXT.id, ""));
        }
    }

    @Override
    public List<HelperText> getHelperTexts(Long id) throws GenericException {
        if (id == null) {
            return helperTextsRepository.findAll();
        }
        return List.of(Objects.requireNonNull(helperTextsRepository.findById(id).orElseThrow(() -> new GenericException(GenericExceptionCode.HELPER_TEXT_NOT_FOUND, "Helper text not found."))));
    }

    @Override
    public HelperText updateHelperText(Long id, String value) throws GenericException {
        HelperText helperText = helperTextsRepository.findById(id).orElseThrow(() -> new GenericException(GenericExceptionCode.HELPER_TEXT_NOT_FOUND, "Helper text not found."));
        helperText.setValue(value);
        return helperTextsRepository.save(helperText);
    }
}
