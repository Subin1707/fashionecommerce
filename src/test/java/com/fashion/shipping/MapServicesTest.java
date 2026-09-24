package com.fashion.shipping;

import com.fashion.location.service.*;
import com.fashion.shipping.service.OsrmRoutingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class MapServicesTest {
    private final MapProviderClient client = mock(MapProviderClient.class);
    private final ObjectMapper mapper = new ObjectMapper();

    @Test void photonSearchMapsCoordinatesAndAddressAndCaches() throws Exception {
        when(client.get(anyString())).thenReturn(mapper.readTree("""
                {"features":[{"geometry":{"coordinates":[105.81,21.01]},
                 "properties":{"name":"CT4","street":"Yen Nghia","city":"Ha Noi","state":"Ha Noi","country":"Viet Nam"}}]}
                """));
        var service = new GeocodingService(client, "https://maps.example", "photon");
        var result = service.search("CT4 Yen Nghia").getFirst();
        assertThat(result.latitude()).isEqualTo(21.01);
        assertThat(result.longitude()).isEqualTo(105.81);
        assertThat(result.displayName()).isEqualTo("CT4, Yen Nghia, Ha Noi, Viet Nam");
        assertThat(service.search("CT4 Yen Nghia")).hasSize(1);
        verify(client, times(1)).get("https://maps.example/api/?limit=5&countrycode=VN&q=CT4+Yen+Nghia");
    }

    @Test void photonReversePreservesPinCoordinates() throws Exception {
        when(client.get(anyString())).thenReturn(mapper.readTree("""
                {"features":[{"geometry":{"coordinates":[105.81,21.01]},"properties":{"name":"Nearby street"}}]}
                """));
        var result = new GeocodingService(client, "https://maps.example", "photon").reverse(21.012345, 105.812345);
        assertThat(result.latitude()).isEqualTo(21.012345);
        assertThat(result.longitude()).isEqualTo(105.812345);
        assertThat(result.displayName()).isEqualTo("Nearby street");
    }

    @Test void photonHandlesEmptyAndMalformedResults() throws Exception {
        when(client.get(anyString())).thenReturn(mapper.readTree("{\"features\":[]}"));
        assertThat(new GeocodingService(client, "https://maps.example", "photon").search("Unknown")).isEmpty();
        assertThatThrownBy(() -> new GeocodingService(client, "https://maps.example", "photon").reverse(21, 105))
                .isInstanceOf(IllegalArgumentException.class);
        when(client.get(anyString())).thenReturn(mapper.readTree("{\"features\":[{\"properties\":{\"name\":\"Missing coordinates\"}}]}"));
        assertThatThrownBy(() -> new GeocodingService(client, "https://maps.example", "photon").search("Unknown"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test void searchCachesResultsAndThrottlesOtherQueries() throws Exception {
        when(client.get(anyString())).thenReturn(mapper.readTree("""
                [{"display_name":"Test Hanoi","lat":"21.01","lon":"105.81"}]
                """));
        var service = new GeocodingService(client, "https://maps.example", "nominatim");
        assertThat(service.search("Test Hanoi").getFirst().latitude()).isEqualTo(21.01);
        assertThat(service.search("Test Hanoi")).hasSize(1);
        verify(client, times(1)).get(contains("q=Test+Hanoi"));
        assertThatThrownBy(() -> service.search("Other address")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void reversePreservesTheSelectedCoordinates() throws Exception {
        when(client.get(anyString())).thenReturn(mapper.readTree("""
                {"display_name":"Nearby street","lat":"21.02","lon":"105.82"}
                """));
        var service = new GeocodingService(client, "https://maps.example", "nominatim");
        var result = service.reverse(21.012345, 105.812345);
        assertThat(result.latitude()).isEqualTo(21.012345);
        assertThat(result.longitude()).isEqualTo(105.812345);
        assertThatThrownBy(() -> service.reverse(Double.NaN, 105)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.search("ab")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void osrmConvertsLongitudeLatitudeAndPreservesRoadBends() throws Exception {
        when(client.get(anyString())).thenReturn(mapper.readTree("""
                {"code":"Ok","routes":[{"distance":250,"duration":40,"geometry":{
                  "coordinates":[[105.8,21.0],[105.81,21.0],[105.81,21.01]]}}]}
                """));
        var result = new OsrmRoutingService(client, "https://routes.example").findRoute(21, 105.8, 21.01, 105.81);
        assertThat(result.points()).hasSize(3);
        assertThat(result.points().get(1).latitude()).isEqualTo(21);
        assertThat(result.points().get(1).longitude()).isEqualTo(105.81);
        assertThat(result.distanceMeters()).isEqualTo(250);
        verify(client).get(contains("105.8,21.0;105.81,21.01?overview=full&geometries=geojson"));
    }

    @Test void noRouteFailsWithoutInventingAStraightLine() throws Exception {
        when(client.get(anyString())).thenReturn(mapper.readTree("{\"code\":\"NoRoute\"}"));
        assertThatThrownBy(() -> new OsrmRoutingService(client, "https://routes.example")
                .findRoute(21, 105, 22, 106)).isInstanceOf(IllegalArgumentException.class);
    }
}
