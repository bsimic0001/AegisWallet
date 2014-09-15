/*
 * Aegis Bitcoin Wallet - The secure Bitcoin wallet for Android
 * Copyright 2014 Bojan Simic and specularX.co, designed by Reuven Yamrom
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.aegiswallet.objects;

/**
 * Created by bsimic on 3/17/14.
 */
public class CurrencyPojo {

    private String currency;
    private Double last;
    private String symbol;

    public CurrencyPojo(String currency, Double last, String symbol){
        this.currency = currency;
        this.last = last;
        this.symbol = symbol;
    }

    public String getCurrency() {
        return currency;
    }

    public Double getLast() {
        return last;
    }

    public String getSymbol() {
        return symbol;
    }
}
