package com.eqinov.recrutement.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eqinov.recrutement.data.DataPoint;
import com.eqinov.recrutement.data.DataPointId;
import com.eqinov.recrutement.data.Site;

public interface DataPointRepository  extends JpaRepository<DataPoint, DataPointId> {	
	
	List<DataPoint> findBySite(Site site);
	
	List<DataPoint> findBySiteAndTimeBetween(Site site, LocalDateTime start, LocalDateTime end);
	
	DataPoint findTopBySiteOrderByTimeDesc(Site site);
	
	DataPoint findTopBySiteOrderByTimeAsc(Site site);
}