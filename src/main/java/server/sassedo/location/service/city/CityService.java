package server.sassedo.location.service.city;

import server.sassedo.location.data.dto.City;
import server.sassedo.location.data.network.request.AddCityRequest;
import server.sassedo.location.data.network.request.UpdateCityRequest;
import server.sassedo.model.GenericException;

import java.util.List;

public interface CityService {

    List<City> getByCountryId(Long countryId) throws GenericException;

    City add(AddCityRequest addCityRequest) throws GenericException;

    City update(UpdateCityRequest updateCityRequest) throws GenericException;

    void delete(Long id) throws GenericException;
}
