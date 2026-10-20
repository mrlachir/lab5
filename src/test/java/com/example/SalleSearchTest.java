package com.example;
import com.example.model.*;
import com.example.service.*;
import com.example.util.PaginationResult;
import javax.persistence.*;
import org.junit.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import static org.junit.Assert.*;

public class SalleSearchTest {
    private EntityManagerFactory emf;
    private EntityManager em;
    private SalleServiceImpl service;
    private final LocalDateTime start = LocalDateTime.of(2024,6,1,9,0);
    @Before public void setup() {
        Map<String,Object> config = new HashMap<>();
        config.put("javax.persistence.jdbc.url","jdbc:h2:mem:lab5_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        config.put("hibernate.show_sql","false");
        emf = Persistence.createEntityManagerFactory("SallePU",config);
        em = emf.createEntityManager();
        em.getTransaction().begin();
        Salle a = new Salle("Salle A",10), b = new Salle("Salle B",20), c = new Salle("Salle C",30),
                d = new Salle("Salle D",40), e = new Salle("Salle E",50);
        a.addReservation(new Reservation(start,start.plusHours(2)));
        b.addReservation(new Reservation(start.minusHours(1),start));
        c.addReservation(new Reservation(start.plusHours(1),start.plusHours(3)));
        for(Salle s : Arrays.asList(a,b,c,d,e)) em.persist(s);
        em.getTransaction().commit(); em.clear();
        service = new SalleServiceImpl(); service.setEntityManager(em);
    }
    @After public void close() {
        try {
            if(em != null && em.isOpen()) {
                if(em.getTransaction().isActive()) em.getTransaction().rollback();
                em.close();
            }
        } finally { if(emf != null && emf.isOpen()) emf.close(); }
    }
    private List<String> names(List<Salle> rooms) {
        return rooms.stream().map(Salle::getNom).collect(Collectors.toList());
    }
    @Test public void jpqlExcludesOverlappingRoomsAndIncludesEmptyRooms() {
        assertEquals(Arrays.asList("Salle B","Salle D","Salle E"),
                names(service.getAvailableRooms(start,start.plusHours(2))));
    }
    @Test public void adjacentBookingsAreAllowed() {
        assertEquals(Arrays.asList("Salle A","Salle B","Salle D","Salle E"),
                names(service.getAvailableRooms(start.plusHours(2),start.plusHours(3))));
    }
    @Test public void criteriaAndJpqlAgree() {
        assertEquals(names(service.getAvailableRooms(start,start.plusHours(2))),
                names(service.searchRooms(null,null,start,start.plusHours(2),1,10)));
    }
    @Test public void combinedFiltersAndCountAgree() {
        assertEquals(Arrays.asList("Salle D","Salle E"),
                names(service.searchRooms("SALLE",30,start,start.plusHours(2),1,10)));
        assertEquals(2,service.countRooms("SALLE",30,start,start.plusHours(2)));
    }
    @Test public void optionalFiltersAndEmptySearch() {
        assertEquals(5,service.countRooms(null,null,null,null));
        assertEquals(5,service.countRooms("  ",null,null,null));
        assertEquals(1,service.countRooms("b",null,null,null));
        assertEquals(0,service.countRooms("absent",null,null,null));
        assertEquals(2,service.countRooms(null,40,null,null));
    }
    @Test public void pagesAreStableNonOverlappingAndHaveCorrectTotals() {
        Set<Long> ids = new HashSet<>();
        for(int page=1;page<=3;page++) {
            PaginationResult<Salle> p = service.searchRoomsPage(null,null,null,null,page,2);
            assertEquals(page,p.getCurrentPage()); assertEquals(3,p.getTotalPages());
            assertEquals(5,p.getTotalItems()); assertEquals(page==3?1:2,p.getItems().size());
            for(Salle s:p.getItems()) assertTrue(ids.add(s.getId()));
        }
        assertEquals(5,ids.size());
        assertTrue(service.searchRooms(null,null,null,null,4,2).isEmpty());
    }
    @Test public void filteredPaginationUsesFilteredCount() {
        PaginationResult<Salle> p = service.searchRoomsPage("salle",30,start,start.plusHours(2),2,1);
        assertEquals(2,p.getTotalItems()); assertEquals(2,p.getTotalPages());
        assertEquals("Salle E",p.getItems().get(0).getNom());
        PaginationResult<Salle> empty = service.searchRoomsPage("absent",null,null,null,1,10);
        assertEquals(0,empty.getTotalPages()); assertTrue(empty.getItems().isEmpty());
    }
    @Test public void invalidPeriodsAndPaginationRejected() {
        assertThrows(IllegalArgumentException.class,()->service.getAvailableRooms(start,start));
        assertThrows(IllegalArgumentException.class,()->service.getAvailableRooms(start,start.minusHours(1)));
        assertThrows(IllegalArgumentException.class,()->service.countRooms(null,null,start,null));
        assertThrows(IllegalArgumentException.class,()->service.countRooms(null,-1,null,null));
        assertThrows(IllegalArgumentException.class,()->service.searchRooms(null,null,null,null,0,2));
        assertThrows(IllegalArgumentException.class,()->service.searchRooms(null,null,null,null,1,0));
        assertThrows(IllegalArgumentException.class,()->service.searchRooms(null,null,null,null,Integer.MAX_VALUE,2));
    }
    @Test public void nameSearchTreatsWildcardCharactersLiterally() {
        em.getTransaction().begin(); em.persist(new Salle("Salle 100%_!",60)); em.getTransaction().commit();
        assertEquals(1,service.countRooms("%_!",null,null,null));
    }
    @Test public void lifecycleRejectsInvalidReservation() {
        em.getTransaction().begin();
        try {
            Salle s = new Salle("Invalid",10);
            Reservation r = new Reservation(); r.setDebut(start); r.setFin(start);
            s.addReservation(r);
            assertThrows(RuntimeException.class,()-> { em.persist(s); em.flush(); });
        } finally { if(em.getTransaction().isActive()) em.getTransaction().rollback(); em.clear(); }
        assertEquals(5,service.countRooms(null,null,null,null));
    }
}
