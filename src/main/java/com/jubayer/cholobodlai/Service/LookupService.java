package com.jubayer.cholobodlai.Service;

import com.jubayer.cholobodlai.Model.Truck.Truck;
import com.jubayer.cholobodlai.Model.Truck.TruckType;
import com.jubayer.cholobodlai.Repository.Truck.TruckRepository;
import com.jubayer.cholobodlai.Repository.Truck.TruckTypeRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class LookupService {
    private final TruckTypeRepository truckTypeRepository;
    private final TruckRepository truckRepository;
    private final CurrentUserService currentUserService;

    public LookupService(TruckTypeRepository truckTypeRepository, TruckRepository truckRepository,
                         CurrentUserService currentUserService) {
        this.truckTypeRepository = truckTypeRepository;
        this.truckRepository = truckRepository;
        this.currentUserService = currentUserService;
    }

    public List<TruckType> truckTypes() { return truckTypeRepository.findAll(); }
    public List<Truck> myTrucks() {
        return truckRepository.findByOwnerId(currentUserService.currentUser().getId());
    }
}