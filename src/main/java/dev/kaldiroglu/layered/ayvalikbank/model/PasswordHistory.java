package dev.kaldiroglu.layered.ayvalikbank.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "password_history")
public class PasswordHistory {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "hashed_password", nullable = false)
    private String hashedPassword;

    @Column(nullable = false)
    private int position;

    public PasswordHistory() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
    public String getHashedPassword() { return hashedPassword; }
    public void setHashedPassword(String hashedPassword) { this.hashedPassword = hashedPassword; }
    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
}
