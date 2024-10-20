package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;

public class FareCalculatorService {

    // Méthode existante avec un nouveau paramètre "discount" pour appliquer la remise
    public void calculateFare(Ticket ticket, boolean discount) {
        if (ticket.getOutTime() == null || ticket.getOutTime().before(ticket.getInTime())) {
            throw new IllegalArgumentException("Out time provided is incorrect:" + ticket.getOutTime());
        }

        long inTimeMillis = ticket.getInTime().getTime();
        long outTimeMillis = ticket.getOutTime().getTime();

        double durationInHours = (double) (outTimeMillis - inTimeMillis) / (1000 * 60 * 60);

        // Tarif gratuit si la durée de stationnement est inférieure à 30 minutes
        if (durationInHours < 0.5) {
            ticket.setPrice(0);
            return;
        }

        // Calcul du tarif
        switch (ticket.getParkingSpot().getParkingType()) {
            case CAR:
                ticket.setPrice(durationInHours * Fare.CAR_RATE_PER_HOUR);
                break;
            case BIKE:
                ticket.setPrice(durationInHours * Fare.BIKE_RATE_PER_HOUR);
                break;
            default:
                throw new IllegalArgumentException("Unknown Parking Type");
        }

        // Appliquer une réduction de 5% si l'utilisateur est récurrent
        if (discount) {
            ticket.setPrice(ticket.getPrice() * 0.95); // Réduction de 5%
        }
    }


    // Nouvelle méthode qui appelle la méthode avec discount à false par défaut
    public void calculateFare(Ticket ticket) {
        calculateFare(ticket, false);
    }
}
