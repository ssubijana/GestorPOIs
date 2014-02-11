package com.example.gestorpois.datos;

import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class UbicacionesBDAdapter {
	
	private static final String DATABASE_NAME = "ubicacionesRutas.db";
    private static final String DATABASE_TABLE_RUTA = "ruta";
    private static final String DATABASE_TABLE_UBICACION = "ubicacion";
    private static final int DATABASE_VERSION = 1;
    
    private SQLiteDatabase db;
    private final Context context;
    private UbicacionesBDOpenHelper bdHelper;
    
    //Nombre de los campos de las tablas
    public static final String KEY_ID_RUTA = "_id";
    public static final String KEY_TITULO_RUTA = "titulo";
    
    public static final String KEY_ID_UBICACION = "_id";
    public static final String KEY_ID_RUTA_UBICACION = "_idRuta";
    public static final String KEY_LAT_UBICACION = "latitud";
    public static final String KEY_LONG_UBICACION = "longitud";
    public static final String KEY_ORDEN_UBICACION = "orden";
    
    public UbicacionesBDAdapter(Context _context) {
        this.context = _context;
        bdHelper = new UbicacionesBDOpenHelper(_context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    
    public void close() {
        db.close();
    }
    
    public void open() throws SQLiteException {
        try 
        {
        	db = bdHelper.getWritableDatabase();
        }
        
        catch (SQLiteException ex)
        {
        	db = bdHelper.getReadableDatabase();
        }
    }
    
    /************************************************
     * MÉTODOS DE BD PARA RUTA Y UBICACIONES
     ***********************************************/    
    
    /**
     * Inserta una nueva ruta y sus puntos de interés en el sistema.
     * @param r Ruta a insertar
     * @param ubicaciones Ubicaciones que forman la ruta
     */
    public void insertarRuta(Ruta r, List<Ubicacion> ubicaciones){
    	long idRuta = -1;
    	// Creamos una nueva fila con los valores a insertar.
        ContentValues newRutaValues = new ContentValues();
        
        // Se asignan los valores de cada columna.
        newRutaValues.put(KEY_TITULO_RUTA, r.getTitulo());
        // Se inserta la fila.
        idRuta = db.insert(DATABASE_TABLE_RUTA, null, newRutaValues);
    	r.setId(idRuta);
    	
    	if(ubicaciones != null && idRuta > 0) {
    		for (Ubicacion ubicacion : ubicaciones) {
				ContentValues newUbicacionValues = new ContentValues();
				newUbicacionValues.put(KEY_ID_RUTA_UBICACION, idRuta);
				newUbicacionValues.put(KEY_LAT_UBICACION, ubicacion.getLatitud());
				newUbicacionValues.put(KEY_LONG_UBICACION, ubicacion.getLongitud());
				newUbicacionValues.put(KEY_ORDEN_UBICACION, ubicacion.getOrden());
				long idUbicacion = db.insert(DATABASE_TABLE_UBICACION, null, newUbicacionValues);
				ubicacion.setId(idUbicacion);
			}
    	}
    }
    
    /**
     * Elimina la ruta y sus puntos de interés.
     * @param r Ruta a eliminar
     * @return Indica si se ha realizado el borrado de algún registro
     */
    public boolean eliminarRuta(Ruta r) {
    	//Eliminamos los puntos de interés de la ruta
    	db.delete(DATABASE_TABLE_UBICACION, KEY_ID_RUTA_UBICACION + "=" + r.getId(), null);    	
    	//Eliminamos la ruta
    	return db.delete(DATABASE_TABLE_RUTA, KEY_ID_RUTA + "=" + r.getId(), null) > 0;
    }
    
    /**
     * Actualiza la ruta con las nuevas ubicaciones
     * @param r Ruta a actualizar
     * @param nuevasUbicaciones
     */
    public void actualizarRuta(Ruta r, List<Ubicacion> nuevasUbicaciones) {
    	//Eliminamos los puntos de interés antiguos de la ruta
    	db.delete(DATABASE_TABLE_UBICACION, KEY_ID_RUTA_UBICACION + "=" + r.getId(), null);    
    	long idRuta = (r != null) ? r.getId() : -1;
    	if(nuevasUbicaciones != null && idRuta > 0) {
    		for (Ubicacion ubicacion : nuevasUbicaciones) {
    			ContentValues newUbicacionValues = new ContentValues();
				newUbicacionValues.put(KEY_ID_RUTA_UBICACION, idRuta);
				newUbicacionValues.put(KEY_LAT_UBICACION, ubicacion.getLatitud());
				newUbicacionValues.put(KEY_LONG_UBICACION, ubicacion.getLongitud());
				newUbicacionValues.put(KEY_ORDEN_UBICACION, ubicacion.getOrden());
				long idUbicacion = db.insert(DATABASE_TABLE_UBICACION, null, newUbicacionValues);
				ubicacion.setId(idUbicacion);
				ubicacion.setIdRuta(idRuta);
			}
    	}
    }
    
    public Cursor recuperarUbicacionesRuta(Ruta r) {
    	String[] campos = {KEY_ID_UBICACION,KEY_ID_RUTA_UBICACION,KEY_LAT_UBICACION,KEY_LONG_UBICACION,KEY_ORDEN_UBICACION};
    	String select = KEY_ID_RUTA_UBICACION + "=" + r.getId();
    	Cursor c = db.query(DATABASE_TABLE_UBICACION, campos, select, null, null, null, null);
    	return c;
    }
    
    public Cursor recuperarRutas() {
    	String[] campos = {KEY_ID_RUTA,KEY_TITULO_RUTA};
    	Cursor c = db.query(DATABASE_TABLE_RUTA, campos, null, null, null, null, null);
    	return c;
    }
    
    /**
     * Clase que crea la base de datos que almacena la información de las rutas
     *
     */
    private static class UbicacionesBDOpenHelper extends SQLiteOpenHelper {

    	private static final String DATABASE_RUTA_CREATE = "create table " + DATABASE_TABLE_RUTA
    			+ " (" + KEY_ID_RUTA + " integer primary key autoincrement, " + KEY_TITULO_RUTA + " text not null);";
    	
    	private static final String DATABASE_UBICACION_CREATE = "create table " + DATABASE_TABLE_UBICACION
    			+ " (" + KEY_ID_UBICACION + " integer primary key autoincrement, " 
    			+ KEY_ID_RUTA_UBICACION + " integer, "
    			+ KEY_LAT_UBICACION + " real not null, "
    			+ KEY_LONG_UBICACION + " real not null, "    			
    			+ KEY_ORDEN_UBICACION + " integer not null,"
    			+ " FOREIGN KEY ("+KEY_ID_RUTA_UBICACION+") REFERENCES "+DATABASE_TABLE_RUTA+" ("+KEY_ID_RUTA+"));";
    	
		public UbicacionesBDOpenHelper(Context context, String name,
				CursorFactory factory, int version) {
			super(context, name, factory, version);
		}

		@Override
		public void onCreate(SQLiteDatabase _db) {
			_db.execSQL(DATABASE_RUTA_CREATE);
			_db.execSQL(DATABASE_UBICACION_CREATE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase _db, int _oldVersion, int _newVersion) {
			Log.w("UbicacionesDBAdapter", "Upgrading from version " +
                    _oldVersion + " to " +
                    _newVersion + ", which will destroy all old data");    
			_db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_UBICACION);
			_db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_RUTA);
			onCreate(_db);
		}
    	
    }

}
