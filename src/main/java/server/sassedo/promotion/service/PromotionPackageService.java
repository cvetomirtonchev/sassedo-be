package server.sassedo.promotion.service;

import server.sassedo.model.GenericException;
import server.sassedo.promotion.data.dto.PromotionPackage;
import server.sassedo.promotion.data.network.request.PromotionPackageRequest;

import java.util.List;

public interface PromotionPackageService {

    List<PromotionPackage> getActive();

    List<PromotionPackage> getAll();

    PromotionPackage getById(Long id) throws GenericException;

    PromotionPackage create(PromotionPackageRequest request);

    PromotionPackage update(Long id, PromotionPackageRequest request) throws GenericException;

    PromotionPackage setActive(Long id, boolean active) throws GenericException;
}
