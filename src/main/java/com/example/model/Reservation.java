package com.example.model;
import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservations")
public class Reservation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "salle_id", nullable = false)
    private Salle salle;
    @Column(nullable = false)
    private LocalDateTime debut;
    @Column(nullable = false)
    private LocalDateTime fin;
    public Reservation() {}
    public Reservation(LocalDateTime debut, LocalDateTime fin) {
        verifierPeriode(debut, fin); this.debut = debut; this.fin = fin;
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Salle getSalle() { return salle; }
    public void setSalle(Salle value) {
        if (salle == value) return;
        Salle old = salle; salle = value;
        if (old != null) old.detach(this);
        if (value != null) value.attach(this);
    }
    public LocalDateTime getDebut() { return debut; }
    public void setDebut(LocalDateTime debut) { this.debut = debut; }
    public LocalDateTime getFin() { return fin; }
    public void setFin(LocalDateTime fin) { this.fin = fin; }
    public static void verifierPeriode(LocalDateTime debut, LocalDateTime fin) {
        if (debut == null || fin == null || !debut.isBefore(fin))
            throw new IllegalArgumentException("Start must be strictly before end.");
    }
    @PrePersist @PreUpdate private void validate() { verifierPeriode(debut, fin); }
}
