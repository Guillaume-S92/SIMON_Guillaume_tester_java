package com.parkit.parkingsystem.integration;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {

    private static DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
    private static ParkingSpotDAO parkingSpotDAO;
    private static TicketDAO ticketDAO;
    private static DataBasePrepareService dataBasePrepareService;

    @Mock
    private static InputReaderUtil inputReaderUtil;

    @BeforeAll
    public static void setUp() throws Exception{
        parkingSpotDAO = new ParkingSpotDAO();
        parkingSpotDAO.dataBaseConfig = dataBaseTestConfig;
        ticketDAO = new TicketDAO();
        ticketDAO.dataBaseConfig = dataBaseTestConfig;
        dataBasePrepareService = new DataBasePrepareService();
    }

    @BeforeEach
    public void setUpPerTest() throws Exception {
        when(inputReaderUtil.readSelection()).thenReturn(1); // 1 pour "CAR"
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        dataBasePrepareService.clearDataBaseEntries();
    }

    @AfterAll
    public static void tearDown(){

    }

    @Test
    public void testParkingACar(){
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        parkingService.processIncomingVehicle();

        // Vérifie que le ticket est bien enregistré dans la base de données
        Ticket ticket = ticketDAO.getTicket("ABCDEF");
        assertNotNull(ticket);
        assertEquals("ABCDEF", ticket.getVehicleRegNumber());

        // Vérifie que la place de parking a été mise à jour
        ParkingSpot parkingSpot = ticket.getParkingSpot();
        assertNotNull(parkingSpot);
        assertFalse(parkingSpot.isAvailable());
    }

    @Test
    public void testParkingLotExit() {
        // Simuler l'entrée du véhicule
        testParkingACar();

        // Obtenir le ticket et modifier l'heure de sortie avec une heure simulée
        Ticket ticket = ticketDAO.getTicket("ABCDEF");

        // Simuler une durée de stationnement d'une heure (1 heure après l'entrée)
        Date outTime = new Date(ticket.getInTime().getTime() + (60 * 60 * 1000)); // +1 heure
        ticket.setOutTime(outTime);

        // Mettre à jour l'heure de sortie dans la base de données
        ticketDAO.updateTicket(ticket);

        // Simuler la sortie du véhicule avec la méthode actualisée
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        parkingService.processExitingVehicle();

        // Vérifier que l'heure de sortie est bien enregistrée et que le tarif est supérieur à 0
        ticket = ticketDAO.getTicket("ABCDEF");
        assertNotNull(ticket.getOutTime(), "L'heure de sortie ne doit pas être null");
        assertTrue(ticket.getPrice() > 0, "Le tarif doit être supérieur à 0");
    }





    // Nouveau test pour vérifier la remise de 5% pour un utilisateur récurrent
    @Test
    public void testParkingLotExitRecurringUser() {
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

        // Premier stationnement et sortie
        parkingService.processIncomingVehicle();
        Ticket firstTicket = ticketDAO.getTicket("ABCDEF");

        // Simuler une durée de stationnement de 1 heure
        Date outTimeFirst = new Date(firstTicket.getInTime().getTime() + (60 * 60 * 1000)); // +1 heure
        firstTicket.setOutTime(outTimeFirst);
        ticketDAO.updateTicket(firstTicket);
        parkingService.processExitingVehicle();

        // Deuxième stationnement et sortie (utilisateur récurrent)
        parkingService.processIncomingVehicle();
        Ticket secondTicket = ticketDAO.getTicket("ABCDEF");

        // Simuler une durée de stationnement de 1 heure pour le deuxième ticket
        Date outTimeSecond = new Date(secondTicket.getInTime().getTime() + (60 * 60 * 1000)); // +1 heure
        secondTicket.setOutTime(outTimeSecond);
        ticketDAO.updateTicket(secondTicket);
        parkingService.processExitingVehicle();

        // Vérification de la remise
        Ticket finalTicket = ticketDAO.getTicket("ABCDEF");
        assertNotNull(finalTicket);
        assertTrue(finalTicket.getPrice() > 0, "Le tarif doit être supérieur à 0");
        assertEquals(Fare.CAR_RATE_PER_HOUR * 0.95, finalTicket.getPrice(), 0.01); // Vérifie la remise de 5%
    }

}
