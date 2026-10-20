package com.example.model;
import javax.persistence.*;
import java.util.*;

@Entity
@Table(name = "salles")
public class Salle {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String nom;
    @Column(nullable = false)
    private int capacite;
    @OneToMany(mappedBy = "salle", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Reservation> reservations = new HashSet<>();
    public Salle() {}
    public Salle(String nom, int capacite) { this.nom = nom; this.capacite = capacite; }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public int getCapacite() { return capacite; }
    public void setCapacite(int capacite) { this.capacite = capacite; }
    public Set<Reservation> getReservations() { return Collections.unmodifiableSet(reservations); }
    public void setReservations(Set<Reservation> values) {
        Set<Reservation> copy = new HashSet<>(Objects.requireNonNull(values));
        for (Reservation r : new HashSet<>(reservations)) r.setSalle(null);
        for (Reservation r : copy) addReservation(r);
    }
    public void addReservation(Reservation r) { Objects.requireNonNull(r).setSalle(this); }
    void attach(Reservation r) { reservations.add(r); }
    void detach(Reservation r) { reservations.remove(r); }
    @Override public String toString() { return nom + " (capacite=" + capacite + ")"; }
}
