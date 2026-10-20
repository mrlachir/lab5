package com.example.repository;
import com.example.model.*;
import javax.ejb.Stateless;
import javax.persistence.*;
import javax.persistence.criteria.*;
import java.time.LocalDateTime;
import java.util.*;

@Stateless
public class SalleRepositoryImpl implements SalleRepository {
    @PersistenceContext(unitName = "SallePU")
    private EntityManager entityManager;
    public SalleRepositoryImpl() {}
    public SalleRepositoryImpl(EntityManager em) { setEntityManager(em); }
    public void setEntityManager(EntityManager em) { entityManager = Objects.requireNonNull(em); }
    private EntityManager em() {
        if (entityManager == null) throw new IllegalStateException("EntityManager not injected");
        return entityManager;
    }
    @Override public List<Salle> findAvailableRooms(LocalDateTime debut, LocalDateTime fin) {
        Reservation.verifierPeriode(debut, fin);
        return em().createQuery("SELECT s FROM Salle s WHERE s.id NOT IN "
                + "(SELECT r.salle.id FROM Reservation r WHERE :debut < r.fin AND :fin > r.debut) "
                + "ORDER BY s.nom, s.id", Salle.class)
                .setParameter("debut", debut).setParameter("fin", fin).getResultList();
    }
    private void validate(Integer capacity, LocalDateTime debut, LocalDateTime fin) {
        if (capacity != null && capacity < 0) throw new IllegalArgumentException("Capacity must be non-negative");
        if (debut != null || fin != null) Reservation.verifierPeriode(debut, fin);
    }
    // Build new expressions for each query's own root (including the count query).
    private Predicate[] filters(CriteriaBuilder cb, CriteriaQuery<?> query, Root<Salle> root,
                                String nom, Integer capaciteMin, LocalDateTime debut, LocalDateTime fin) {
        validate(capaciteMin, debut, fin);
        List<Predicate> predicates = new ArrayList<>();
        if (nom != null && !nom.trim().isEmpty()) {
            // Treat name as a literal substring, not a user-supplied LIKE pattern.
            String escaped = nom.trim().toLowerCase(Locale.ROOT)
                    .replace("!", "!!").replace("%", "!%").replace("_", "!_");
            predicates.add(cb.like(cb.lower(root.<String>get("nom")), "%" + escaped + "%", '!'));
        }
        if (capaciteMin != null) predicates.add(cb.ge(root.<Integer>get("capacite"), capaciteMin));
        if (debut != null) {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<Reservation> r = sub.from(Reservation.class);
            sub.select(r.get("salle").get("id")).where(cb.and(
                    cb.lessThan(r.<LocalDateTime>get("debut"), fin),
                    cb.greaterThan(r.<LocalDateTime>get("fin"), debut)));
            predicates.add(cb.not(root.get("id").in(sub)));
        }
        return predicates.toArray(new Predicate[0]);
    }
    @Override public List<Salle> searchRooms(String nom, Integer capaciteMin, LocalDateTime debut, LocalDateTime fin, int page, int size) {
        if (page < 1 || size < 1) throw new IllegalArgumentException("Page and size must be >= 1");
        long offset = (long)(page - 1) * size;
        if (offset > Integer.MAX_VALUE) throw new IllegalArgumentException("Page offset too large");
        CriteriaBuilder cb = em().getCriteriaBuilder();
        CriteriaQuery<Salle> query = cb.createQuery(Salle.class);
        Root<Salle> root = query.from(Salle.class);
        query.select(root).where(filters(cb, query, root, nom, capaciteMin, debut, fin));
        // Stable ordering is necessary for consistent pagination.
        query.orderBy(cb.asc(root.get("nom")), cb.asc(root.get("id")));
        return em().createQuery(query).setFirstResult((int)offset).setMaxResults(size).getResultList();
    }
    @Override public long countRooms(String nom, Integer capaciteMin, LocalDateTime debut, LocalDateTime fin) {
        CriteriaBuilder cb = em().getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<Salle> root = query.from(Salle.class);
        query.select(cb.count(root)).where(filters(cb, query, root, nom, capaciteMin, debut, fin));
        return em().createQuery(query).getSingleResult();
    }
}
