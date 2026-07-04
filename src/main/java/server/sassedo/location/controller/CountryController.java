package server.sassedo.location.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.sassedo.location.data.dto.City;
import server.sassedo.location.data.dto.Country;
import server.sassedo.location.data.network.request.AddCityRequest;
import server.sassedo.location.data.network.request.AddCountryRequest;
import server.sassedo.location.data.network.request.UpdateCityRequest;
import server.sassedo.location.data.network.request.UpdateCountryRequest;
import server.sassedo.location.data.network.response.CityResponse;
import server.sassedo.location.data.network.response.CountryResponse;
import server.sassedo.location.service.city.CityService;
import server.sassedo.location.service.country.CountryService;
import server.sassedo.model.GenericException;

import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/countries")
@RequiredArgsConstructor
public class CountryController {

    private final CountryService countryService;
    private final CityService cityService;

    @GetMapping
    public ResponseEntity<?> getCountries() {
        List<CountryResponse> countries = countryService.getAll().stream()
                .map(this::mapCountryToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(countries);
    }

    @GetMapping("/{id}/cities")
    public ResponseEntity<?> getCities(@PathVariable Long id) {
        try {
            List<CityResponse> cities = cityService.getByCountryId(id).stream()
                    .map(this::mapCityToResponse)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(cities);
        } catch (GenericException e) {
            return ResponseEntity.badRequest().body(e.getErrorResponse());
        }
    }

    @PostMapping("/admin/add")
    public ResponseEntity<?> addCountry(@Valid @RequestBody AddCountryRequest addCountryRequest) {
        try {
            Country country = countryService.add(addCountryRequest);
            return ResponseEntity.ok(mapCountryToResponse(country));
        } catch (GenericException e) {
            return ResponseEntity.badRequest().body(e.getErrorResponse());
        }
    }

    @PutMapping("/admin/update")
    public ResponseEntity<?> updateCountry(@Valid @RequestBody UpdateCountryRequest updateCountryRequest) {
        try {
            Country country = countryService.update(updateCountryRequest);
            return ResponseEntity.ok(mapCountryToResponse(country));
        } catch (GenericException e) {
            return ResponseEntity.badRequest().body(e.getErrorResponse());
        }
    }

    @DeleteMapping("/admin/{id}")
    public ResponseEntity<?> deleteCountry(@PathVariable Long id) {
        try {
            countryService.delete(id);
            return ResponseEntity.ok().build();
        } catch (GenericException e) {
            return ResponseEntity.badRequest().body(e.getErrorResponse());
        }
    }

    @PostMapping("/admin/cities/add")
    public ResponseEntity<?> addCity(@Valid @RequestBody AddCityRequest addCityRequest) {
        try {
            City city = cityService.add(addCityRequest);
            return ResponseEntity.ok(mapCityToResponse(city));
        } catch (GenericException e) {
            return ResponseEntity.badRequest().body(e.getErrorResponse());
        }
    }

    @PutMapping("/admin/cities/update")
    public ResponseEntity<?> updateCity(@Valid @RequestBody UpdateCityRequest updateCityRequest) {
        try {
            City city = cityService.update(updateCityRequest);
            return ResponseEntity.ok(mapCityToResponse(city));
        } catch (GenericException e) {
            return ResponseEntity.badRequest().body(e.getErrorResponse());
        }
    }

    @DeleteMapping("/admin/cities/{id}")
    public ResponseEntity<?> deleteCity(@PathVariable Long id) {
        try {
            cityService.delete(id);
            return ResponseEntity.ok().build();
        } catch (GenericException e) {
            return ResponseEntity.badRequest().body(e.getErrorResponse());
        }
    }

    private CountryResponse mapCountryToResponse(Country country) {
        List<CityResponse> cities = country.getCities().stream()
                .map(this::mapCityToResponse)
                .collect(Collectors.toList());
        return new CountryResponse(country.getId(), country.getNameEn(), country.getNameBg(), country.getCode(), cities);
    }

    private CityResponse mapCityToResponse(City city) {
        return new CityResponse(city.getId(), city.getNameEn(), city.getNameBg(), city.getCountry().getId());
    }
}
