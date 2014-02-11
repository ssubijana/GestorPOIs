package com.example.gestorpois;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.example.gestorpois.datos.Ruta;
import com.example.gestorpois.datos.UbicacionesBDAdapter;

public class RutasActivity extends Activity {
	
	private UbicacionesBDAdapter ubicacionesBDAdapter;
	private Cursor rutasCursor;
	private ListView listViewRutas;
	private Button btnVolver;
	
	//Listado de rutas
	private ArrayList<Ruta> rutas;
	//ArrayAdapter que utiliza el listview
	private ArrayAdapter<Ruta> rutasAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.listadorutas);
		
		rutas = new ArrayList<Ruta>();
		listViewRutas = (ListView)findViewById(R.id.listRutas);
		btnVolver = (Button)findViewById(R.id.btnVolver);
		
		btnVolver.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent result = new Intent();
				setResult(MapaActivity.VOLVER_RESULT, result);
				finish();
			}
		});
		
		rutasAdapter = new ArrayAdapter<Ruta>(this, android.R.layout.simple_list_item_1,rutas);
        listViewRutas.setAdapter(rutasAdapter);
        //Registramos el menu contextual para el listview
		registerForContextMenu(listViewRutas);
		
		ubicacionesBDAdapter = new UbicacionesBDAdapter(this);
		ubicacionesBDAdapter.open();
		publicarListaRutas();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		// TODO Auto-generated method stub
		super.onCreateContextMenu(menu, v, menuInfo);
		//Mostramos el menu conceptual
		MenuInflater menuInflater = getMenuInflater();
		menuInflater.inflate(R.menu.opciones_ruta, menu);
	}
	
	
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		AdapterContextMenuInfo info = null;
		Ruta r = null;
		super.onContextItemSelected(item);		
		switch(item.getItemId()) {
		case R.id.menuVer:
			info = (AdapterContextMenuInfo) item.getMenuInfo();
			r = rutas.get(info.position);
			Intent result = new Intent();
			result.putExtra(MapaActivity.COD_ID_RUTA, r.getId());
			result.putExtra(MapaActivity.COD_TITULO_RUTA, r.getTitulo());
			setResult(MapaActivity.VER_RUTA_RESULT, result);
			finish();
			break;
		case R.id.menuEliminar:
			info = (AdapterContextMenuInfo) item.getMenuInfo();
			r = rutas.get(info.position);
			ubicacionesBDAdapter.eliminarRuta(r);
			actualizarListadoRutas();
			break;
		default:
			break;
		}
		return true;
	}

	/************************************************************
	 * MÉTODOS DE MANEJO DE LA LISTA DE RUTAS
	 ***********************************************************/
	 private void publicarListaRutas() {
	        // Recuperamos todos los productos de la base de datos.
	        rutasCursor = ubicacionesBDAdapter.recuperarRutas();
	        startManagingCursor(rutasCursor);
	        
	        // Actualizamos el array.
	        actualizarListadoRutas();	        
	    }

	private void actualizarListadoRutas() {
		// Situamos el cursor y limpiamos el array para empezarlo con los nuevos
		// valores de la base de datos
		rutasCursor.requery();
		rutas.clear();
		// Recorremos los elementos
		if (rutasCursor.moveToFirst()) {
			do {
				// Recuperamos tanto el valor del título de la ruta como el de su id
				String tituloRuta = rutasCursor.getString(rutasCursor
						.getColumnIndex(UbicacionesBDAdapter.KEY_TITULO_RUTA));
				int id = rutasCursor.getInt(rutasCursor
						.getColumnIndex(UbicacionesBDAdapter.KEY_ID_RUTA));
				Ruta r = new Ruta(id, tituloRuta);
				rutas.add(0, r);
			} while (rutasCursor.moveToNext());

		}
		// Notificamos para actualizar el ListView
		rutasAdapter.notifyDataSetChanged();
	}

	
}
