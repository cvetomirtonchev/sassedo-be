package server.sassedo.location.service.city;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import server.sassedo.listing.rental.repository.RentalListingRepository;
import server.sassedo.listing.roommate.repository.RoommateListingRepository;
import server.sassedo.listing.search.repository.ApartmentSearchRepository;
import server.sassedo.location.data.dto.City;
import server.sassedo.location.data.dto.Country;
import server.sassedo.location.data.network.request.AddCityRequest;
import server.sassedo.location.data.network.request.UpdateCityRequest;
import server.sassedo.location.repository.CityRepository;
import server.sassedo.location.repository.CountryRepository;
import server.sassedo.model.GenericException;
import server.sassedo.model.GenericExceptionCode;
import server.sassedo.utils.ImageUploadValidator;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CityServiceImpl implements CityService {

    private final CityRepository cityRepository;
    private final CountryRepository countryRepository;
    private final RentalListingRepository rentalListingRepository;
    private final RoommateListingRepository roommateListingRepository;
    private final ApartmentSearchRepository apartmentSearchRepository;

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

    @Override
    @Transactional(readOnly = true)
    public List<CityWithCount> getPopular(int limit) {
        int effectiveLimit = limit <= 0 ? 5 : limit;

        Map<Long, Long> counts = new HashMap<>();
        accumulate(counts, rentalListingRepository.countActiveByCity());
        accumulate(counts, roommateListingRepository.countActiveByCity());
        accumulate(counts, apartmentSearchRepository.countActiveByCity());

        return cityRepository.findAll().stream()
                .map(city -> new CityWithCount(city, counts.getOrDefault(city.getId(), 0L)))
                .sorted(Comparator
                        .comparingLong(CityWithCount::listingCount).reversed()
                        .thenComparing(cwc -> cwc.city().getNameBg(), String.CASE_INSENSITIVE_ORDER))
                .limit(effectiveLimit)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public City getById(Long id) throws GenericException {
        return cityRepository.findById(id)
                .orElseThrow(() -> new GenericException(GenericExceptionCode.CITY_NOT_FOUND, "City not found"));
    }

    @Override
    @Transactional
    public City updateImage(Long id, MultipartFile file) throws GenericException, IOException {
        City city = cityRepository.findById(id)
                .orElseThrow(() -> new GenericException(GenericExceptionCode.CITY_NOT_FOUND, "City not found"));
        if (file == null || file.isEmpty()) {
            throw new GenericException(GenericExceptionCode.INVALID_FILE, "No file provided");
        }
        ImageUploadValidator.validate(file);
        city.setImage(file.getBytes());
        city.setImageContentType(file.getContentType());
        return cityRepository.save(city);
    }

    private void accumulate(Map<Long, Long> counts, List<Object[]> rows) {
        for (Object[] row : rows) {
            if (row.length < 2 || row[0] == null) {
                continue;
            }
            Long cityId = ((Number) row[0]).longValue();
            long count = ((Number) row[1]).longValue();
            counts.merge(cityId, count, Long::sum);
        }
    }

    private String resolveBg(String nameBg, String fallback) {
        if (nameBg == null || nameBg.isBlank()) {
            return fallback;
        }
        return nameBg.trim();
    }
}
