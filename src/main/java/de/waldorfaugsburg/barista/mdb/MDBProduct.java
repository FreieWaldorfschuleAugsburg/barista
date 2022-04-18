package de.waldorfaugsburg.barista.mdb;

import lombok.ToString;

@ToString
public record MDBProduct(int productId, double money) {
}
