package org.example.projet_pi.Dto;

public class AnnualAmortissementDTO {
    private int year;
    private double capitalDebut;
    private double interet;
    private double amortissement;
    private double annuite;
    private double capitalFin;

    public AnnualAmortissementDTO(int year,
                                  double capitalDebut,
                                  double interet,
                                  double amortissement,
                                  double annuite,
                                  double capitalFin) {
        this.year = year;
        this.capitalDebut = capitalDebut;
        this.interet = interet;
        this.amortissement = amortissement;
        this.annuite = annuite;
        this.capitalFin = capitalFin;
    }

    public int getYear() { return year; }
    public double getCapitalDebut() { return capitalDebut; }
    public double getInteret() { return interet; }
    public double getAmortissement() { return amortissement; }
    public double getAnnuite() { return annuite; }
    public double getCapitalFin() { return capitalFin; }

}
