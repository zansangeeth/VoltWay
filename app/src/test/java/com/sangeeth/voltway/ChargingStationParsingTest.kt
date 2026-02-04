package com.sangeeth.voltway

import com.sangeeth.voltway.model.ChargingStationResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ChargingStationParsingTest {

    private val json = """
{
  "type": "FeatureCollection",
  "features": [
    {
      "type": "Feature",
      "geometry": {
        "type": "Point",
        "coordinates": [-121.920559, 37.367285]
      },
      "properties": {
        "location": {
          "country_code": "US",
          "party_id": "MBX",
          "id": "dXJuOm1ieHBvaToxMjM0NQ==",
          "publish": false,
          "name": "Hudson Concourse",
          "address": "1741 Technology Drive",
          "city": "San Jose",
          "postal_code": "95110",
          "state": "CA",
          "country": "USA",
          "coordinates": {
            "latitude": "37.3672850",
            "longitude": "-121.9205590"
          },
          "evses": [
            {
              "uid": "82686",
              "evse_id": "82686",
              "status": "AVAILABLE",
              "connectors": [
                {
                  "id": "5",
                  "standard": "IEC_62196_T1",
                  "format": "CABLE",
                  "power_type": "AC_2_PHASE",
                  "max_voltage": 240,
                  "max_amperage": 27,
                  "max_electric_power": 6.5,
                  "last_updated": "2022-08-11T14:03:44"
                }
              ],
              "coordinates": {
                "latitude": "37.3672850",
                "longitude": "-121.9205590"
              },
              "last_updated": "2022-08-11T14:03:44"
            }
          ],
          "operator": {
            "name": "ChargePoint"
          },
          "owner": {
            "name": "ChargePoint"
          },
          "time_zone": "America/Los_Angeles",
          "opening_times": {
            "twentyfourseven": true
          },
          "last_updated": "2022-08-11T14:03:44"
        },
        "proximity": {
          "latitude": 37.364714,
          "longitude": -121.924238,
          "distance": 0.43294229650723554
        }
      }
    }
  ]
}
    """

    @Test
    fun parseChargingStationsJson() {
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
        val adapter = moshi.adapter(ChargingStationResponse::class.java)
        
        val response = adapter.fromJson(json)
        
        assertNotNull(response)
        assertEquals("FeatureCollection", response?.type)
        assertEquals(1, response?.features?.size)
        
        val feature = response?.features?.first()
        assertEquals("Feature", feature?.type)
        
        val location = feature?.properties?.location
        assertEquals("Hudson Concourse", location?.name)
        assertEquals("1741 Technology Drive", location?.address)
        assertEquals("San Jose", location?.city)
        assertEquals("AVAILABLE", location?.evses?.first()?.status)
        
        val proximity = feature?.properties?.proximity
        assertEquals(0.43294229650723554, proximity?.distance)
    }
}
