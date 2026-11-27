package com.smartpantry.exception;

public class PantryItemNotFoundException extends RuntimeException {

    public PantryItemNotFoundException(Long id) {
        super("Pantry item with id " + id + " was not found.");
    }
}
