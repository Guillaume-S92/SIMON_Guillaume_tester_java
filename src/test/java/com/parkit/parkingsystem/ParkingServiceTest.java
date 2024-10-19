package com.parkit.parkingsystem;

import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ParkingServiceTest {

    private ParkingService parkingService;

    @Mock
    private InputReaderUtil inputReaderUtil;
    @Mock
    private ParkingSpotDAO parkingSpotDAO;
    @Mock
    private TicketDAO ticketDAO;



    @BeforeEach
    public void setUpPerTest() {
        try {
            // Stubbings lenient pour éviter les erreurs de stubbing non utilisés dans certains tests
            lenient().when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
            lenient().when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(true);
            lenient().when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true);

            // Création d'une instance ParkingSpot et Ticket pour les stubs communs
            ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, false);
            Ticket ticket = new Ticket();
            ticket.setInTime(new Date(System.currentTimeMillis() - (60 * 60 * 1000))); // 1 heure en arrière
            ticket.setParkingSpot(parkingSpot);
            ticket.setVehicleRegNumber("ABCDEF");

            // Stubbings pour les DAO utilisés dans plusieurs tests
            lenient().when(ticketDAO.getTicket(anyString())).thenReturn(ticket);
            lenient().when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(true);
            lenient().when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true);

            // Initialisation du service avec les mocks
            parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to set up test mock objects");
        }
    }


    @Test
    public void processExitingVehicleTest() {
        when(ticketDAO.getNbTicket(anyString())).thenReturn(2); // Simule un utilisateur récurrent

        parkingService.processExitingVehicle();

        verify(ticketDAO, times(1)).updateTicket(any(Ticket.class));
        verify(parkingSpotDAO, times(1)).updateParking(any(ParkingSpot.class));
        verify(ticketDAO, times(1)).getNbTicket(anyString()); // Vérification de l'appel à getNbTicket()
    }

    @Test
    public void testProcessIncomingVehicle() throws Exception {
        // Simule la sélection de type de véhicule "CAR"
        when(inputReaderUtil.readSelection()).thenReturn(1);  // 1 pour "CAR"
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");

        // Simule la récupération d'une place de parking disponible
        when(parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR)).thenReturn(1);

        // Simule les actions pour mettre à jour le parking et enregistrer un ticket
        when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true);
        when(ticketDAO.saveTicket(any(Ticket.class))).thenReturn(true);

        // Exécute la méthode à tester
        parkingService.processIncomingVehicle();

        // Vérifie les interactions avec les mocks
        verify(parkingSpotDAO, times(1)).getNextAvailableSlot(ParkingType.CAR);
        verify(parkingSpotDAO, times(1)).updateParking(any(ParkingSpot.class));
        verify(ticketDAO, times(1)).saveTicket(any(Ticket.class));
    }


    @Test
    public void processExitingVehicleTestUnableUpdate() {
        when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(false);
        when(ticketDAO.getNbTicket(anyString())).thenReturn(2);

        parkingService.processExitingVehicle();

        verify(ticketDAO, times(1)).updateTicket(any(Ticket.class));
        verify(ticketDAO, times(1)).getNbTicket(anyString());
    }

    @Test
    public void testGetNextParkingNumberIfAvailable() throws Exception {
        // Simule la sélection du type de véhicule "CAR"
        when(inputReaderUtil.readSelection()).thenReturn(1); // 1 pour "CAR"

        // Simule la récupération d'une place de parking avec l'ID 1
        when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(1);

        // Appel de la méthode testée
        ParkingSpot parkingSpot = parkingService.getNextParkingNumberIfAvailable();

        // Vérification que la place de parking n'est pas nulle et que son ID est bien 1
        assertNotNull(parkingSpot);
        assertEquals(1, parkingSpot.getId());
        assertTrue(parkingSpot.isAvailable());

        // Vérification que la méthode DAO a été appelée une fois avec le type de véhicule correct
        verify(parkingSpotDAO, times(1)).getNextAvailableSlot(ParkingType.CAR);
    }


    @Test
    public void testGetNextParkingNumberIfAvailableParkingNumberNotFound() throws Exception {
        // Mock pour simuler la sélection du type de véhicule
        when(inputReaderUtil.readSelection()).thenReturn(1); // Simule la sélection de "CAR"

        // Simulation de l'absence de places disponibles (retourne -1)
        when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(-1);

        // Appel de la méthode à tester
        ParkingSpot parkingSpot = parkingService.getNextParkingNumberIfAvailable();

        // Vérification que la méthode renvoie null lorsque aucune place n'est disponible
        assertNull(parkingSpot);

        // Vérification que la méthode DAO a été appelée une fois
        verify(parkingSpotDAO, times(1)).getNextAvailableSlot(any(ParkingType.class));
    }



    @Test
    public void testGetNextParkingNumberIfAvailableParkingNumberWrongArgument() throws Exception {
        // Simule une entrée utilisateur incorrecte
        when(inputReaderUtil.readSelection()).thenReturn(3); // 3 n'est pas un type valide

        // Vérifie que l'exception est bien levée
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            parkingService.getVehichleType();  // Test direct de getVehichleType()
        });

        // Vérifie que le message de l'exception est correct
        assertEquals("Entered input is invalid", exception.getMessage());

        // Vérifie que le DAO n'a jamais été appelé, car le type de véhicule est invalide
        verify(parkingSpotDAO, never()).getNextAvailableSlot(any(ParkingType.class));
    }


}
