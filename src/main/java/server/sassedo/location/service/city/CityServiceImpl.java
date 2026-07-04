package server.sassedo.location.service.city;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.sassedo.location.data.dto.City;
import server.sassedo.location.data.dto.Country;
import server.sassedo.location.data.network.request.AddCityRequest;
import server.sassedo.location.data.network.request.UpdateCityRequest;
import server.sassedo.location.repository.CityRepository;
import server.sassedo.location.repository.CountryRepository;
import server.sassedo.model.GenericException;
import server.sassedo.model.GenericExceptionCode;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CityServiceImpl implements CityService {

    private final CityRepository cityRepository;
    private final CountryRepository countryRepository;

    @Override
    public List<City> getByCountryId(Long countryId) throws GenericException {
        if (!countryRepository.existsById(countryId)) {
            throw new GenericException(GenericExceptionCode.COUNTRY_NOT_FOUND, "Country not found");
        }
        return cityRepository.findByCountryId(countryId);
    }

    @Override
    @Transactional
    public City add(AddCityRequest addCityRequest) throws GenericException {
        Country country = countryRepository.findById(addCityRequest.getCountryId())
                .orElseThrow(() -> new GenericException(GenericExceptionCode.COUNTRY_NOT_FOUND, "Country not found"));
        String nameEn = addCityRequest.getNameEn().trim();
        if (cityRepository.existsByNameEnIgnoreCaseAndCountryId(nameEn, country.getId())) {
            throw new GenericException(GenericExceptionCode.CITY_ALREADY_EXISTS, "City already exists");
        }
        String nameBg = resolveBg(addCityRequest.getNameBg(), nameEn);
        City city = new City(nameEn, nameBg, country);
        return cityRepository.save(city);
    }

    @Override
    @Transactional
    public City update(UpdateCityRequest updateCityRequest) throws GenericException {
        City city = cityRepository.findById(updateCityRequest.getId())
                .orElseThrow(() -> new GenericException(GenericExceptionCode.CITY_NOT_FOUND, "City not found"));
        if (updateCityRequest.getNameEn() != null && !updateCityRequest.getNameEn().isBlank()) {
            String nameEn = updateCityRequest.getNameEn().trim();
            if (!nameEn.equalsIgnoreCase(city.getNameEn())
                    && cityRepository.existsByNameEnIgnoreCaseAndCountryId(nameEn, city.getCountry().getId())) {
                throw new GenericException(GenericExceptionCode.CITY_ALREADY_EXISTS, "City already exists");
            }
            city.setNameEn(nameEn);
        }
        if (updateCityRequest.getNameBg() != null && !updateCityRequest.getNameBg().isBlank()) {
            city.setNameBg(updateCityRequest.getNameBg().trim());
        }
        return cityRepository.save(city);
    }

    @Override
    @Transactional
    public void delete(Long id) throws GenericException {
        City city = cityRepository.findById(id)
                .orElseThrow(() -> new GenericException(GenericExceptionCode.CITY_NOT_FOUND, "City not found"));
        cityRepository.delete(city);
    }

    private String resolveBg(String nameBg, String fallback) {
        if (nameBg == null || nameBg.isBlank()) {
            return fallback;
        }
        return nameBg.trim();
    }
}
