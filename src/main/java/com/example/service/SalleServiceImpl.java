package com.example.service;
import com.example.model.Salle;
import com.example.repository.*;
import com.example.util.PaginationResult;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.*;

@Stateless
public class SalleServiceImpl implements SalleService {
    @Inject private SalleRepository salleRepository;
    public SalleServiceImpl() {}
    public SalleServiceImpl(SalleRepository repository) { salleRepository = Objects.requireNonNull(repository); }
    // Manual wiring for the handout's Java SE entry point; no EJB container runs here.
    public void setEntityManager(EntityManager em) { salleRepository = new SalleRepositoryImpl(em); }
    private SalleRepository repository() {
        if (salleRepository == null) throw new IllegalStateException("Repository not injected");
        return salleRepository;
    }
    public List<Salle> getAvailableRooms(LocalDateTime debut, LocalDateTime fin) {
        return repository().findAvailableRooms(debut, fin);
    }
    public List<Salle> searchRooms(String nom, Integer capaciteMin, LocalDateTime debut, LocalDateTime fin, int page, int size) {
        return repository().searchRooms(nom, capaciteMin, debut, fin, page, size);
    }
    public long countRooms(String nom, Integer capaciteMin, LocalDateTime debut, LocalDateTime fin) {
        return repository().countRooms(nom, capaciteMin, debut, fin);
    }
    public PaginationResult<Salle> searchRoomsPage(String nom, Integer capaciteMin, LocalDateTime debut, LocalDateTime fin, int page, int size) {
        List<Salle> items = searchRooms(nom, capaciteMin, debut, fin, page, size);
        return new PaginationResult<>(items, page, size, countRooms(nom, capaciteMin, debut, fin));
    }
}
