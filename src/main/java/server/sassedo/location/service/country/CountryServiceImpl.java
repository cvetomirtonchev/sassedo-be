package server.sassedo.location.service.country;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.sassedo.location.data.dto.Country;
import server.sassedo.location.data.network.request.AddCountryRequest;
import server.sassedo.location.data.network.request.UpdateCountryRequest;
import server.sassedo.location.repository.CountryRepository;
import server.sassedo.model.GenericException;
import server.sassedo.model.GenericExceptionCode;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CountryServiceImpl implements CountryService {

    private final CountryRepository countryRepository;

    @Override
    public List<Country> getAll() {
        return countryRepository.findAll(Sort.by("nameEn"));
    }

    @Override
    public Country getById(Long id) throws GenericException {
        return countryRepository.findById(id)
                .orElseThrow(() -> new GenericException(GenericExceptionCode.COUNTRY_NOT_FOUND, "Country not found"));
    }

    @Override
    @Transactional
    public Country add(AddCountryRequest addCountryRequest) throws GenericException {
        String nameEn = addCountryRequest.getNameEn().trim();
        if (countryRepository.existsByNameEnIgnoreCase(nameEn)) {
            throw new GenericException(GenericExceptionCode.COUNTRY_ALREADY_EXISTS, "Country already exists");
        }
        String nameBg = resolveBg(addCountryRequest.getNameBg(), nameEn);
        Country country = new Country(nameEn, nameBg, normalizeCode(addCountryRequest.getCode()));
        return countryRepository.save(country);
    }

    @Override
    @Transactional
    public Country update(UpdateCountryRequest updateCountryRequest) throws GenericException {
        Country country = getById(updateCountryRequest.getId());
        if (updateCountryRequest.getNameEn() != null && !updateCountryRequest.getNameEn().isBlank()) {
            String nameEn = updateCountryRequest.getNameEn().trim();
            if (!nameEn.equalsIgnoreCase(country.getNameEn()) && countryRepository.existsByNameEnIgnoreCase(nameEn)) {
                throw new GenericException(GenericExceptionCode.COUNTRY_ALREADY_EXISTS, "Country already exists");
            }
            country.setNameEn(nameEn);
        }
        if (updateCountryRequest.getNameBg() != null && !updateCountryRequest.getNameBg().isBlank()) {
            country.setNameBg(updateCountryRequest.getNameBg().trim());
        }
        if (updateCountryRequest.getCode() != null) {
            country.setCode(normalizeCode(updateCountryRequest.getCode()));
        }
        return countryRepository.save(country);
    }

    @Override
    @Transactional
    public void delete(Long id) throws GenericException {
        Country country = getById(id);
        countryRepository.delete(country);
    }

    private String resolveBg(String nameBg, String fallback) {
        if (nameBg == null || nameBg.isBlank()) {
            return fallback;
        }
        return nameBg.trim();
    }

    private String normalizeCode(String code) {
        if (code == null) {
            return null;
        }
        String trimmed = code.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
