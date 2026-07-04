package server.sassedo.location.service.country;

import server.sassedo.location.data.dto.Country;
import server.sassedo.location.data.network.request.AddCountryRequest;
import server.sassedo.location.data.network.request.UpdateCountryRequest;
import server.sassedo.model.GenericException;

import java.util.List;

public interface CountryService {

    List<Country> getAll();

    Country getById(Long id) throws GenericException;

    Country add(AddCountryRequest addCountryRequest) throws GenericException;

    Country update(UpdateCountryRequest updateCountryRequest) throws GenericException;

    void delete(Long id) throws GenericException;
}
