package org.geometerplus.android.fbreader.subscription;

import java.util.List;

public abstract class PeriodicalsDatabase {

	public abstract void insertEntity(PeriodicalEntity pEntity);
	public abstract PeriodicalEntity getEntity(PeriodicalEntity pEntity);
	public abstract List<PeriodicalEntity> getAllEntities();
	public abstract PeriodicalEntity deleteEntity(PeriodicalEntity pEntity);
}
