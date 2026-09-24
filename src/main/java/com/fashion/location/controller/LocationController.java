package com.fashion.location.controller;

import com.fashion.location.dto.LocationResult;
import com.fashion.location.service.GeocodingService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/location")
@RequiredArgsConstructor
public class LocationController {
    private final GeocodingService geocoding;

    @GetMapping("/search")
    public List<LocationResult> search(@RequestParam String q) { return geocoding.search(q); }

    @GetMapping("/reverse")
    public LocationResult reverse(@RequestParam double lat, @RequestParam double lng) { return geocoding.reverse(lat, lng); }
}
