package org.example.projet_pi.entity;

import jakarta.persistence.*;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonBackReference;


@Entity
public class Credit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long creditId;

    private double amount;
    private double interestRate;
    private double monthlyPayment;
    private int durationInMonths;
    @Temporal(TemporalType.DATE)
    private java.util.Date startDate;

    @Temporal(TemporalType.DATE)
    private java.util.Date endDate;

    @Enumerated(EnumType.STRING)
    private CreditStatus status;

    @JsonBackReference("client-credit")
    @ManyToOne
    private Client client;

    @JsonBackReference("agent-credit")
    @ManyToOne
    private AgentFinance agentFinance;

    public Long getCreditId() {
        return creditId;
    }
    public void setCreditId(Long creditId) {
        this.creditId = creditId;
    }

    public double getAmount() {
        return amount;
    }
    public void setAmount(double amount) {
        this.amount = amount;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }



    public double getInterestRate() {
        return interestRate;
    }

    public CreditStatus getStatus() {
        return status;
    }

    public void setStatus(CreditStatus status) {
        this.status = status;
    }

    public void setInterestRate(double interestRate) {
        this.interestRate = interestRate;
    }

    public AgentFinance getAgentFinance() {
        return agentFinance;
    }

    public void setAgentFinance(AgentFinance agentFinance) {
        this.agentFinance = agentFinance;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public double getMonthlyPayment() {
        return monthlyPayment;
    }

    public void setMonthlyPayment(double monthlyPayment) {
        this.monthlyPayment = monthlyPayment;
    }

    public int getDurationInMonths() {
        return durationInMonths;
    }

    public void setDurationInMonths(int durationInMonths) {
        this.durationInMonths = durationInMonths;
    }


}

