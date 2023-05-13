package com.utilsbot.service.dto;

public record LocationResponseDTO(
        String requested_location,
        double longitude,
        double latitude,
        String datetime,
        String timezone_name,
        String timezone_location,
        String timezone_abbreviation,
        float gmt_offset,
        boolean is_dst
) {
    public boolean isEmpty() {
        return requested_location == null &&
                longitude == 0.0f &&
                latitude == 0.0f &&
                datetime == null &&
                timezone_name == null &&
                timezone_location == null &&
                timezone_abbreviation == null &&
                gmt_offset == 0.0f;
    }
}
