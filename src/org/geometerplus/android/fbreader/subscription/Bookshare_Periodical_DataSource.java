package org.geometerplus.android.fbreader.subscription;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class Bookshare_Periodical_DataSource {

	private SQLiteDatabase periodicalDb;
	private PeriodicalsSQLiteHelper dbHelper;
	private String[] allCols={PeriodicalsSQLiteHelper.SUBSCRIBED_COLUMN_ID,PeriodicalsSQLiteHelper.SUBSCRIBED_COLUMN_TITLE,PeriodicalsSQLiteHelper.SUBSCRIBED_COLUMN_LATEST_EDITION,PeriodicalsSQLiteHelper.SUBSCRIBED_COLUMN_LATEST_REVISION};

	public Bookshare_Periodical_DataSource(Context context){
		dbHelper=new PeriodicalsSQLiteHelper(context);
	}

	//Open database
	public void open(){
		periodicalDb=dbHelper.getWritableDatabase();
	}

	//close database
	public void close(){
		dbHelper.close();
	}

	//add an entry to the Database
	public DbPeriodicalEntity addPeriodical(int periodicalID,String periodicalTitle,String latestEdition,int latestRevision){
		Cursor cursor = periodicalDb.query(PeriodicalsSQLiteHelper.TABLE_SUBSCRIBED_PERIODICALS, allCols, PeriodicalsSQLiteHelper.SUBSCRIBED_COLUMN_ID + "="+periodicalID, null, null, null, null);

		ContentValues values=new ContentValues();
		values.put(PeriodicalsSQLiteHelper.SUBSCRIBED_COLUMN_ID, periodicalID);
		values.put(PeriodicalsSQLiteHelper.SUBSCRIBED_COLUMN_TITLE, periodicalTitle);
		values.put(PeriodicalsSQLiteHelper.SUBSCRIBED_COLUMN_LATEST_EDITION, latestEdition);
		values.put(PeriodicalsSQLiteHelper.SUBSCRIBED_COLUMN_LATEST_REVISION, latestRevision);
		
		//insert only if the entry is not there already in the db
		if(cursor.getCount()<=0){
			periodicalDb.insert(PeriodicalsSQLiteHelper.TABLE_SUBSCRIBED_PERIODICALS, null, values);
		}else{
			periodicalDb.update(PeriodicalsSQLiteHelper.TABLE_SUBSCRIBED_PERIODICALS, values, PeriodicalsSQLiteHelper.SUBSCRIBED_COLUMN_ID+"=?", new String[]{periodicalID+""});
		}
		
		cursor = periodicalDb.query(PeriodicalsSQLiteHelper.TABLE_SUBSCRIBED_PERIODICALS, allCols, 
		PeriodicalsSQLiteHelper.SUBSCRIBED_COLUMN_ID +"="+periodicalID, null, null, null, null);
		cursor.moveToFirst();
		DbPeriodicalEntity periodical = cursorToPeriodical(cursor);
		cursor.close();
		return periodical;

	}

	public Set<String> getAllPeriodicalIds(){
		Set<String> periodicalIds = new HashSet<String>();
		
		Cursor cursor = periodicalDb.query(PeriodicalsSQLiteHelper.TABLE_SUBSCRIBED_PERIODICALS, allCols, null, null, null, null, null);
		cursor.moveToFirst();
		
		while(!cursor.isAfterLast()){
			periodicalIds.add((cursorToPeriodical(cursor).getId())+"");
			cursor.moveToNext();
		}
		
		return periodicalIds;
	} 
	
	
	
	public void deletePeriodical(int periodicalID){
		
		Cursor cursor = periodicalDb.query(PeriodicalsSQLiteHelper.TABLE_SUBSCRIBED_PERIODICALS, allCols, PeriodicalsSQLiteHelper.SUBSCRIBED_COLUMN_ID + "="+periodicalID, null, null, null, null);
		
		//DbPeriodicalEntity delEntry=cursorToPeriodical(cursor);
		int delRows=periodicalDb.delete(PeriodicalsSQLiteHelper.TABLE_SUBSCRIBED_PERIODICALS, PeriodicalsSQLiteHelper.SUBSCRIBED_COLUMN_ID+ " = " + periodicalID, null);
		//return delEntry;
	}
	//convert entry at cursor to a Periodical Entity in DB
	public DbPeriodicalEntity cursorToPeriodical(Cursor cursor){
		if(cursor.getCount()>0){
			DbPeriodicalEntity periodical=new DbPeriodicalEntity();
			periodical.setId(cursor.getInt(0));
			periodical.setTitle(cursor.getString(1));
			periodical.setLatestEdition(cursor.getString(2));
			periodical.setLatestRevision(cursor.getInt(3));

			return periodical;
		}
		return null;
	}
}
