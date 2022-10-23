package com.razeticketbot;
public class Ticket {
    public String name;
    public String value;
    public String description;
    public String[] rolesThatCanSeeTicketsDefault;
    public String ticketSpecificMessageOnOpen;
    public Ticket(String n, String val, String desc, String[] rolesToSeeTicket, String onOpenMessage) {
        name = n;
        value = val;
        description = desc;
        rolesThatCanSeeTicketsDefault = rolesToSeeTicket;
        ticketSpecificMessageOnOpen = onOpenMessage;
    }
}
