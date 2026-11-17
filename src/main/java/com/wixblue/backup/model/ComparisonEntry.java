package com.wixblue.backup.model;

/**
 * Jämförelseresultat för en enskild fil.
 *
 * @param path   sökväg relativt respektive rotmapp, alltid med snedstreck som avgränsare
 * @param status hur filen förhåller sig mellan källa och backup
 */
public record ComparisonEntry(String path, ComparisonStatus status) {
}
