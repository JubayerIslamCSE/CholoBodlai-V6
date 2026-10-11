package com.jubayer.cholobodlai.Service.Truck;

import com.jubayer.cholobodlai.Component.Mapper.TruckMapper;
import com.jubayer.cholobodlai.DTO.TruckRequest;
import com.jubayer.cholobodlai.DTO.TruckResponse;
import com.jubayer.cholobodlai.EXCEPTION.BusinessRuleException;
import com.jubayer.cholobodlai.EXCEPTION.ResourceNotFoundException;
import com.jubayer.cholobodlai.Model.Emums.Role;
import com.jubayer.cholobodlai.Model.Emums.TruckStatus;
import com.jubayer.cholobodlai.Model.Truck.Truck;
import com.jubayer.cholobodlai.Model.Truck.TruckType;
import com.jubayer.cholobodlai.Model.User;
import com.jubayer.cholobodlai.Repository.Truck.TruckRepository;
import com.jubayer.cholobodlai.Repository.Truck.TruckTypeRepository;
import com.jubayer.cholobodlai.Service.CurrentUserService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class TruckServiceImpl implements TruckService {

    private final TruckRepository truckRepository;
    private final TruckTypeRepository truckTypeRepository;
    private final TruckMapper mapper;
    private final CurrentUserService currentUserService;

    public TruckServiceImpl(TruckRepository truckRepository, TruckTypeRepository truckTypeRepository,
                            TruckMapper mapper, CurrentUserService currentUserService) {
        this.truckRepository = truckRepository;
        this.truckTypeRepository = truckTypeRepository;
        this.mapper = mapper;
        this.currentUserService = currentUserService;
    }



    @Override
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public TruckResponse create(TruckRequest request) {

        throw new BusinessRuleException(
                "Truck details are submitted during owner registration");
    }


    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public TruckResponse update(String id, TruckRequest r) {
        Truck truck = findTruck(id);
        if (truck.getStatus() == TruckStatus.ON_TRIP) {
            throw new BusinessRuleException(
                    "Cannot edit an assigned truck");
        }
        assertOwnerOrAdmin(truck);
        TruckType type = findType(r.truckTypeId());
        truck.setRegistrationNo(r.registrationNo());
        truck.setTruckTypeId(type.getId());
        truck.setTruckTypeName(type.getName());
        truck.setCategoryName(type.getCategoryName());
        return mapper.toResponse(truckRepository.save(truck));
    }

    @Override
    public TruckResponse getById(String id) {
        Truck truck = findTruck(id);
        assertOwnerOrAdmin(truck);
        return mapper.toResponse(truck);
    }

    @Override
    public List<TruckResponse> getAll() {

        User user = currentUserService.currentUser();

        List<Truck> visibleTrucks;

        if (user.getRole() == Role.ADMIN) {
            visibleTrucks = truckRepository.findAll();

        } else if (user.getRole() == Role.OWNER) {
            visibleTrucks = truckRepository.findByOwnerId(user.getId());

        } else {
            visibleTrucks = List.of();
        }

        return visibleTrucks.stream()
                .map(mapper::toResponse)
                .toList();
    }


    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(String id) {
        Truck truck = findTruck(id);
        assertOwnerOrAdmin(truck);
        if (truck.getStatus() == TruckStatus.ON_TRIP) {
            throw new BusinessRuleException(
                    "Cannot delete a truck assigned to an active trip"
            );
        }
        truckRepository.deleteById(id);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public TruckResponse verify(String id) {
        Truck truck = findTruck(id);
        truck.setVerified(true);
        return mapper.toResponse(truckRepository.save(truck));
    }

    private void assertOwnerOrAdmin(Truck truck) {
        User current = currentUserService.currentUser();
        if (current.getRole() != Role.ADMIN && !truck.getOwnerId().equals(current.getId())) {
            throw new AccessDeniedException("You can only manage your own trucks");
        }
    }
    private Truck findTruck(String id) {
        return truckRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Truck", id));
    }
    private TruckType findType(String id) {
        return truckTypeRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("TruckType", id));
    }
}