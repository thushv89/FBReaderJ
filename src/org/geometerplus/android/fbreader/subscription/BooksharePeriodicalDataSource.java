package org.geometerplus.android.fbreader.subscription;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class BooksharePeriodicalDataSource extends PeriodicalsDatabase{

	private SQLiteDatabase periodicalDb;
	private PeriodicalsSQLiteHelper dbHelper;
	private String[] allCols={PeriodicalsSQLiteHelper.SUBSCRIBED_COLUMN_ID,PeriodicalsSQLiteHelper.SUBSCRIBED_COLUMN_TITLE,PeriodicalsSQLiteHelper.SUBSCRIBED_COLUMN_LATEST_EDITION,PeriodicalsSQLiteHelper.SUBSCRIBED_COLUMN_LATEST_REVISION};

	public BooksharePeriodicalDataSource(Context context){
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

	
	//convert entry at cursor to a Periodical Entity in DB
	public SubscribedDbPeriodicalEntity cursorToPeriodical(Cursor cursor){
		if(cursor.getCount()>0){
			SubscribedDbPeriodicalEntity periodical=new SubscribedDbPeriodicalEntity();
			periodical.setId(cursor.getString(0));
			periodical.setTitle(cursor.getString(1));
			periodical.setLatestEdition(cursor.getString(2));
			periodical.setLatestRevision(cursor.getInt(3));

			return periodical;
		}
		return null;
	}

	@Override
	public void insertEntity(PeriodicalEntity pEntity) {
		SubscribedDbPeriodicalEntity entity = (SubscribedDbPeriodicalEntity) pEntity;
		String periodicalId= entity.getId();
		Cursor cursor = periodicalDb.query(PeriodicalsSQLiteHelper.TABLE_SUBSCRIBED_PERIODICALS, allCols, PeriodicalsSQLiteHelper.SUBSCRIBED_COLUMN_ID + "="+periodicalId, null, null, null, null);

		ContentValues values=new ContentValues();
		values.put(PeriodicalsSQLiteHelper.SUBSCRIBED_COLUMN_ID, periodicalId);
		values.put(PeriodicalsSQLiteHelper.SUBSCRIBED_COLUMN_TITLE, entity.getTitle());
		values.put(PeriodicalsSQLiteHelper.SUBSCRIBED_COLUMN_LATEST_EDITION, entity.getLatestEdition());
		values.put(PeriodicalsSQLiteHelper.SUBSCRIBED_COLUMN_LATEST_REVISION, entity.getLatestRevision());
		
		//insert only if the entry is not there already in the db
		if(cursor.getCount()<=0){
			periodicalDb.insert(PeriodicalsSQLiteHelper.TABLE_SUBSCRIBED_PERIODICALS, null, values);
		}else{
			periodicalDb.update(PeriodicalsSQLiteHelper.TABLE_SUBSCRIBED_PERIODICALS, values, PeriodicalsSQLiteHelper.SUBSCRIBED_COLUMN_ID+"=?", new String[]{periodicalId+""});
		}
		
		cursor = periodicalDb.query(PeriodicalsSQLiteHelper.TABLE_SUBSCRIBED_PERIODICALS, allCols, 
		PeriodicalsSQLiteHelper.SUBSCRIBED_COLUMN_ID +"="+periodicalId, null, null, null, null);
		cursor.moveToFirst();
		SubscribedDbPeriodicalEntity periodical = cursorToPeriodical(cursor);
		cursor.close();
	}
	
	

	@Override
	public PeriodicalEntity getEntity(PeriodicalEntity pEntity) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<PeriodicalEntity> getAllEntities() {
		ArrayList<PeriodicalEntity> entities = new ArrayList<PeriodicalEntity>();
		
		Cursor cursor = periodicalDb.query(PeriodicalsSQLiteHelper.TABLE_SUBSCRIBED_PERIODICALS, allCols, null, null, null, null, null);
		cursor.moveToFirst();
		
		while(!cursor.isAfterLast()){
			PeriodicalEntity entity = new PeriodicalEntity();
			entities.add(cursorToPeriodical(cursor));
			cursor.moveToNext();
		}
		
		return entities;
		
	}

	@Override
	public PeriodicalEntity deleteEntity(PeriodicalEntity pEntity) {
		Cursor cursor = periodicalDb.query(PeriodicalsSQLiteHelper.TABLE_SUBSCRIBED_PERIODICALS, allCols, PeriodicalsSQLiteHelper.SUBSCRIBED_COLUMN_ID + "="+pEntity.getId(), null, null, null, null);
		SubscribedDbPeriodicalEntity delEntry=cursorToPeriodical(cursor);
		int delRows=periodicalDb.delete(PeriodicalsSQLiteHelper.TABLE_SUBSCRIBED_PERIODICALS, PeriodicalsSQLiteHelper.SUBSCRIBED_COLUMN_ID+ " = " + pEntity.getId(), null);

		return delEntry;
	}
}
