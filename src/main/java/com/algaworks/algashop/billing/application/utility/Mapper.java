package com.algaworks.algashop.billing.application.utility;

public interface Mapper {
    <T> T convert(Object object, Class<T> destinationType);
}