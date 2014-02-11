package com.example.gestorpois;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import com.example.gestorpois.datos.Ruta;
import com.example.gestorpois.datos.Ubicacion;
import com.example.gestorpois.datos.UbicacionesBDAdapter;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

public class MapaActivity extends android.support.v4.app.FragmentActivity {

	public static final int DIALOG_NOMBRE_RUTA = 1;
	public static final int TXT_NOMBRE_RUTA = 1;
	public static final int MODO_ANADIR = 0;
	public static final int MODO_BORRADO = 1;
	public static final int VER_RUTAS_ACTIVITY = 1;
	public static final int VOLVER_RESULT = 1;
	public static final int VER_RUTA_RESULT = 2;
	public static final String COD_ID_RUTA = "idRuta";
	public static final String COD_TITULO_RUTA = "tituloRuta";

	private GoogleMap mapaRuta;
	private LocationManager locationManager;
	private String bestProvider;
	
	private UbicacionesBDAdapter ubicacionesBDAdapter;
	private Cursor ubicacionesRutaCursor;
	private boolean habilitaGuardar;
	// Flag que marcará si nos encontramos en modo edición o borrado de
	// ubicaciones
	private int modo = MODO_ANADIR;
	private int ordenRuta = 0;

	private Ruta ruta;
	private Map<Integer, Ubicacion> ubicaciones;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		ubicaciones = new HashMap<Integer, Ubicacion>();
		ubicacionesBDAdapter = new UbicacionesBDAdapter(this);
		mapaRuta = ((SupportMapFragment) getSupportFragmentManager()
				.findFragmentById(R.id.map)).getMap();

		// Habilitamos en el mapa poder mostrar la ubicación actual
		mapaRuta.setMyLocationEnabled(true);

		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_COARSE);
		criteria.setPowerRequirement(Criteria.POWER_LOW);
		criteria.setAltitudeRequired(false);
		criteria.setBearingRequired(false);
		criteria.setSpeedRequired(false);
		criteria.setCostAllowed(true);

		bestProvider = locationManager.getBestProvider(criteria, true);
		if (bestProvider == null) {
			criteria.setAccuracy(Criteria.ACCURACY_COARSE);
			criteria.setPowerRequirement(Criteria.POWER_MEDIUM);
			criteria.setAltitudeRequired(false);
			criteria.setBearingRequired(false);
			criteria.setSpeedRequired(false);
			criteria.setCostAllowed(true);
			bestProvider = locationManager.getBestProvider(criteria, true);
		}

		Location mostRecentLocation = locationManager
				.getLastKnownLocation(bestProvider);
		centrarMapa(mostRecentLocation);

		locationManager.requestLocationUpdates(bestProvider,// LocationManager.GPS_PROVIDER,
				1, 1, new LocListener());

		// Establecemos el manejador del evento al hacer click sobre el mapa
		mapaRuta.setOnMapClickListener(new OnMapClickListener() {

			@Override
			public void onMapClick(LatLng point) {
				if (modo == MODO_ANADIR) {
					String direccion = updateLocation(point);
					// Al seleccionar un punto en el mapa añadimos el marcador
					mapaRuta.addMarker(new MarkerOptions().position(point).title(
							direccion));
					Ubicacion ubicacionPunto = new Ubicacion(0, 0, ordenRuta,
							point.latitude, point.longitude);
					ubicaciones.put(ubicacionPunto.hashCode(), ubicacionPunto);
					ordenRuta++;
				}				
			}
		});

		mapaRuta.setOnMarkerClickListener(new OnMarkerClickListener() {

			@Override
			public boolean onMarkerClick(Marker marker) {
				if (modo == MODO_BORRADO) {
					Ubicacion ubicacionPunto = new Ubicacion(0, 0, 0,
							marker.getPosition().latitude, marker.getPosition().longitude);
					//Borramos el marker
					ubicaciones.remove(ubicacionPunto.hashCode());
					marker.remove();
				}
				else {
					marker.showInfoWindow();
				}
				return true;
			}
		});

		// Abrimos o creamos la base de datos
		ubicacionesBDAdapter.open();

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.mapa, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.getItem(3).setVisible(habilitaGuardar);
		menu.getItem(4).setVisible(habilitaGuardar);
		//menu.getItem(5).setVisible(habilitaGuardar);
		menu.getItem(1).setEnabled(habilitaGuardar);
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		List<Ubicacion> ubicacionesList = new ArrayList<Ubicacion>(
				ubicaciones.values());
		Toast toast = null;
		super.onMenuItemSelected(featureId, item);
		switch (item.getItemId()) {
		case R.id.menuNuevo:
			mapaRuta.clear();
			ruta = new Ruta(0, null);
			ubicaciones = new HashMap<Integer, Ubicacion>();
			habilitaGuardar = true;
			ordenRuta = 0;
			// Invalidamos el menú para que habilita la opción de guardar
			invalidateOptionsMenu();
			break;
		case R.id.menuGuardar:
			
			if (ruta.getId() > 0) {
				//Actualizamos la ruta
				ubicacionesBDAdapter.actualizarRuta(ruta, ubicacionesList);
				toast = Toast.makeText(getApplicationContext(), "Ruta "
						+ ruta.getTitulo() + " actualizada", Toast.LENGTH_SHORT);
				toast.show();
			}
			else {
				showDialog(DIALOG_NOMBRE_RUTA);
			}			
			break;
		case R.id.menuSelect:
			Intent intentRutas = new Intent(getApplicationContext(), RutasActivity.class);
			//startActivity(intentSecundaria);
			startActivityForResult(intentRutas, VER_RUTAS_ACTIVITY);
			break;
		case R.id.menuAdd:
			modo = MODO_ANADIR;
			toast = Toast.makeText(getApplicationContext(),
					"Modo añadir POIs ON", Toast.LENGTH_SHORT);
			toast.show();
			break;
		case R.id.menuBorrado:
			modo = MODO_BORRADO;
			toast = Toast.makeText(getApplicationContext(),
					"Modo borrado POIs ON", Toast.LENGTH_SHORT);
			toast.show();
			break;
		case R.id.menuVerRuta:
			muestraIndicacionesRuta();
			break;
		default:
			break;
		}
		return true;
	}

	@Override
	protected Dialog onCreateDialog(int id) {

		switch (id) {
		case DIALOG_NOMBRE_RUTA:
			return crearDialogoNuevaRuta();
		default:
			return null;
		}
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {

		switch (id) {
		case DIALOG_NOMBRE_RUTA:
			// Clear the input box.
			EditText text = (EditText) dialog.findViewById(TXT_NOMBRE_RUTA);
			text.setText("");
			break;
		}
	}
	
	

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		switch(resultCode) {
		case VOLVER_RESULT:
			mapaRuta.clear();
			ruta = new Ruta(0, null);
			ordenRuta = 0;
			ubicaciones = new HashMap<Integer, Ubicacion>();
			habilitaGuardar = false;
			invalidateOptionsMenu();
			break;
		case VER_RUTA_RESULT:
			//Recuperar la ruta y sus puntos
			long idRuta = data.getLongExtra(COD_ID_RUTA,0);
			String tituloRuta = data.getStringExtra(COD_TITULO_RUTA);
			ruta = new Ruta(idRuta, tituloRuta);
			ordenRuta = 0;
			ubicacionesRutaCursor = ubicacionesBDAdapter.recuperarUbicacionesRuta(ruta);
			muestraUbicacionesRuta(ubicacionesRutaCursor);
			habilitaGuardar = true;
			invalidateOptionsMenu();
			ubicacionesRutaCursor.close();
			Toast toast = Toast.makeText(getApplicationContext(),
					"Ruta " + tituloRuta + " seleccionada", Toast.LENGTH_SHORT);
			toast.show();
			break;
		default:
			break;
		}
	}

	/******************************************************
	 * MÉTODOS DE GEOLOCALIZACION
	 ******************************************************/
	private String updateLocation(LatLng point) {
		StringBuilder sbDirecciones = new StringBuilder();
		try {
			Geocoder gc = new Geocoder(getApplicationContext(),
					Locale.getDefault());
			List<Address> addresses = gc.getFromLocation(point.latitude,
					point.longitude, 1);

			int maxIntentos = 10;

			while (maxIntentos > 0 && addresses.size() == 0) {
				addresses = gc.getFromLocation(point.latitude, point.longitude,
						1);
				maxIntentos--;
			}
			if (addresses.size() > 0) {
				Address direccion = addresses.get(0);
				String dirFisica = direccion.getAddressLine(0);
				sbDirecciones.append(dirFisica);
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return sbDirecciones.toString();
	}
	
	private void muestraUbicacionesRuta(Cursor ubicacionesRutaCursor) {
		ubicacionesRutaCursor.requery();
		mapaRuta.clear();
		ubicaciones = new HashMap<Integer, Ubicacion>();
		// Recorremos los elementos
		if (ubicacionesRutaCursor.moveToFirst()) {
			do {
				double latitudUbicacion = ubicacionesRutaCursor.getDouble(ubicacionesRutaCursor.getColumnIndex(UbicacionesBDAdapter.KEY_LAT_UBICACION));
				double longitudUbicacion = ubicacionesRutaCursor.getDouble(ubicacionesRutaCursor.getColumnIndex(UbicacionesBDAdapter.KEY_LONG_UBICACION));
				LatLng point = new LatLng(latitudUbicacion, longitudUbicacion);
				String direccion = updateLocation(point);
				// Al seleccionar un punto en el mapa añadimos el marcador
				mapaRuta.addMarker(new MarkerOptions().position(point).title(
						direccion));
				Ubicacion ubicacionPunto = new Ubicacion(0, 0, 0,
						point.latitude, point.longitude);
				ubicaciones.put(ubicacionPunto.hashCode(), ubicacionPunto);
				
			} while (ubicacionesRutaCursor.moveToNext());

		}
	}

	/******************************************************
	 * MÉTODOS DE DIÁLOGO
	 ******************************************************/

	private Dialog crearDialogoNuevaRuta() {
		final List<Ubicacion> ubicacionesList = new ArrayList<Ubicacion>(
				ubicaciones.values());
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Nueva ruta");
		builder.setMessage("Introduzca el nombre de la nueva ruta");

		// Use an EditText view to get user input.
		final EditText input = new EditText(this);
		input.setId(TXT_NOMBRE_RUTA);
		builder.setView(input);

		builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
				String value = input.getText().toString();
				Log.d("MapaActivity", "Nombre nueva ruta: " + value);
				ruta = new Ruta(0, value);
				
				//Insertamos la ruta
				ubicacionesBDAdapter.insertarRuta(ruta, ubicacionesList);
				Toast toast = Toast.makeText(getApplicationContext(), "Ruta "
						+ ruta.getTitulo() + " insertada", Toast.LENGTH_SHORT);
				toast.show();
				
				
				
				return;
			}
		});

		builder.setNegativeButton("Cancel",
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						return;
					}
				});

		return builder.create();
	}
	
	/******************************************************
	 * CLASE PARA ORDENACIÓN DE POIs
	 ******************************************************/
	
	private class PuntosInteresComparator implements Comparator<Ubicacion> {

		@Override
		public int compare(Ubicacion u1, Ubicacion u2) {
			// TODO Auto-generated method stub
			Integer orden1 = u1.getOrden();
			Integer orden2 = u2.getOrden();
			return orden1.compareTo(orden2);
		}
		
	}

	/******************************************************
	 * MÉTODOS DE GEOLOCALIZACIÓN
	 ******************************************************/

	/**
	 * Centra el mapa en la ubicación indicada.
	 * 
	 * @param ubicacion
	 *            Posición en la que centrar el mapa
	 */
	private void centrarMapa(Location ubicacion) {
		LatLng posicion = new LatLng(ubicacion.getLatitude(),
				ubicacion.getLongitude());
		CameraPosition camPos = new CameraPosition.Builder().target(posicion) // Centramos
																				// el
																				// mapa
																				// en
																				// la
																				// ubicación
																				// actual
				.zoom(15) // Establecemos el zoom en 15
				.build();
		CameraUpdate camUpd = CameraUpdateFactory.newCameraPosition(camPos);
		mapaRuta.animateCamera(camUpd);
	}
	
	private void muestraIndicacionesRuta() {
		List<Ubicacion> ubicacionesList = new ArrayList<Ubicacion>(
				ubicaciones.values());
		Collections.sort(ubicacionesList, new PuntosInteresComparator());
		if (ubicacionesList.size() > 0) {
			Location mostRecentLocation = locationManager
					.getLastKnownLocation(bestProvider);
			Polyline line = mapaRuta.addPolyline(new PolylineOptions()
		     .add(new LatLng(mostRecentLocation.getLatitude(), mostRecentLocation.getLongitude()), new LatLng(ubicacionesList.get(0).getLatitud(), ubicacionesList.get(0).getLongitud()))
		     .width(5)
		     .color(Color.RED));
		}
		for (int pos = 0; pos < ubicacionesList.size() -1; pos++) {
			Ubicacion origen = ubicacionesList.get(pos);
			Ubicacion destino = ubicacionesList.get(pos+1);
			Polyline line = mapaRuta.addPolyline(new PolylineOptions()
		     .add(new LatLng(origen.getLatitud(), origen.getLongitud()), new LatLng(destino.getLatitud(), destino.getLongitud()))
		     .width(5)
		     .color(Color.RED));
		}
		
		
	}

	/**
	 * Clase que va a encargarse de actualizar las coordenadas de latitud y
	 * longitud
	 */
	private class LocListener implements LocationListener {

		@Override
		public void onLocationChanged(Location loc) {
			centrarMapa(loc);
		}

		@Override
		public void onProviderDisabled(String provider) {
			centrarMapa(null);
		}

		@Override
		public void onProviderEnabled(String provider) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			// TODO Auto-generated method stub

		}

	}

}
