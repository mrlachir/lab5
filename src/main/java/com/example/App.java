package com.example;
import com.example.model.*;
import com.example.service.*;
import com.example.util.PaginationResult;
import javax.persistence.*;
import java.time.LocalDateTime;

public class App {
    public static void main(String[] args) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("SallePU");
        try {
            EntityManager em = emf.createEntityManager();
            try {
                LocalDateTime debut = LocalDateTime.of(2024, 6, 1, 9, 0);
                LocalDateTime fin = debut.plusHours(2);
                seed(em, debut);
                em.clear();
                SalleServiceImpl service = new SalleServiceImpl();
                service.setEntityManager(em);
                System.out.println("=== DONNEES : 5 salles, 3 reservations ===");
                System.out.println("=== DISPONIBILITE JPQL : " + debut + " -> " + fin + " ===");
                System.out.println(service.getAvailableRooms(debut, fin));
                System.out.println("=== CRITERIA : nom=salle, capacite>=20, disponible ===");
                System.out.println(service.searchRooms("salle", 20, debut, fin, 1, 10));
                System.out.println("=== COMPTAGE FILTRE ===");
                System.out.println("Total filtre = " + service.countRooms("salle", 20, debut, fin));
                System.out.println("=== PAGINATION : toutes les salles, taille=2 ===");
                for (int page = 1; page <= 3; page++) {
                    PaginationResult<Salle> result = service.searchRoomsPage(null, null, null, null, page, 2);
                    System.out.println("Page " + result.getCurrentPage() + "/" + result.getTotalPages()
                            + ", total=" + result.getTotalItems() + " : " + result.getItems());
                }
                System.out.println("=== BORNE : debut a 11h, fin reservation a 11h ===");
                System.out.println(service.getAvailableRooms(fin, fin.plusHours(1)));
                System.out.println("DEMO TERMINEE");
            } finally { em.close(); }
        } finally { emf.close(); }
    }
    private static void seed(EntityManager em, LocalDateTime debut) {
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Salle a = new Salle("Salle A", 10), b = new Salle("Salle B", 20),
                    c = new Salle("Salle C", 30), d = new Salle("Salle D", 40),
                    e = new Salle("Salle E", 50);
            a.addReservation(new Reservation(debut, debut.plusHours(2)));
            b.addReservation(new Reservation(debut.minusHours(1), debut));
            c.addReservation(new Reservation(debut.plusHours(1), debut.plusHours(3)));
            em.persist(a); em.persist(b); em.persist(c); em.persist(d); em.persist(e);
            tx.commit();
        } catch (RuntimeException failure) {
            if (tx.isActive()) tx.rollback();
            throw failure;
        }
    }
}
