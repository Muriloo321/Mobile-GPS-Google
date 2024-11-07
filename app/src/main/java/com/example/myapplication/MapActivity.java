package com.example.myapplication;

import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private Marker userMarker;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;
    private Float currentZoomLevel = 15f;  // Zoom inicial padrão

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Carregar a preferência de zoom
        loadZoomPreference();

        // Inicializa o fragmento do mapa
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Configuração do LocationRequest para atualizações contínuas
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(1000); // Atualização a cada 1 segundo
        locationRequest.setFastestInterval(500); // Atualização a cada 0.5 segundos
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null) {
                    Location location = locationResult.getLastLocation();
                    if (location != null) {
                        updateMapWithLocation(location);
                    }
                }
            }
        };

        // Botões de zoom
        Button zoomInButton = findViewById(R.id.zoom_in_button);
        Button zoomOutButton = findViewById(R.id.zoom_out_button);

        zoomInButton.setOnClickListener(v -> adjustZoom(true));  // Aumentar zoom
        zoomOutButton.setOnClickListener(v -> adjustZoom(false)); // Diminuir zoom
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            startLocationUpdates();  // Inicia a atualização contínua da localização
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }

        // Ajustar zoom conforme a preferência salva
        if (currentZoomLevel != null) {
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(
                    new CameraPosition.Builder()
                            .target(new LatLng(0, 0)) // Posição fictícia
                            .zoom(currentZoomLevel)
                            .build()
            ));
        }
    }

    private void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        } else {
            Toast.makeText(this, "Permissão de localização não concedida", Toast.LENGTH_SHORT).show();
        }
    }

    // Método para atualizar a localização no mapa
    private void updateMapWithLocation(Location location) {
        if (mMap != null && location != null) {
            // Cria a localização do usuário como LatLng
            LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());

            // Se o marcador ainda não existir, cria um novo marcador
            if (userMarker == null) {
                userMarker = mMap.addMarker(new MarkerOptions().position(userLocation).title("Você"));
            } else {
                // Se o marcador já existe, apenas atualiza a sua posição
                userMarker.setPosition(userLocation);
            }

            // Atualiza a rotação da câmera com o 'bearing' (direção)
            float bearing = location.getBearing(); // Direção do usuário

            // Ajusta a posição da câmera para seguir o marcador e ajustar o zoom
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(userLocation) // Posição atual do usuário
                    .zoom(currentZoomLevel) // Nível de zoom ajustado
                    .bearing(bearing) // Direção do usuário
                    .tilt(30f) // Inclinação da câmera (opcional)
                    .build();

            // Anima a câmera para a nova posição
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
    }

    // Carregar a preferência de zoom
    private void loadZoomPreference() {
        SharedPreferences preferences = getSharedPreferences("MapPrefs", MODE_PRIVATE);
        currentZoomLevel = preferences.getFloat("zoom_level", 15f); // Zoom padrão 15
    }

    // Salvar a preferência de zoom
    private void saveZoomPreference(float zoom) {
        SharedPreferences preferences = getSharedPreferences("MapPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putFloat("zoom_level", zoom);
        editor.apply();
    }

    // Ajustar o zoom
    private void adjustZoom(boolean zoomIn) {
        if (mMap != null) {
            if (zoomIn) {
                currentZoomLevel = Math.min(currentZoomLevel + 1, mMap.getMaxZoomLevel());
            } else {
                currentZoomLevel = Math.max(currentZoomLevel - 1, mMap.getMinZoomLevel());
            }
            saveZoomPreference(currentZoomLevel);  // Salvar a preferência de zoom
            mMap.animateCamera(CameraUpdateFactory.zoomTo(currentZoomLevel));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                        == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    mMap.setMyLocationEnabled(true);
                    startLocationUpdates();  // Inicia as atualizações de localização
                }
            } else {
                Toast.makeText(this, "Permissão de localização negada", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);  // Parar as atualizações de localização
        }
    }
}

