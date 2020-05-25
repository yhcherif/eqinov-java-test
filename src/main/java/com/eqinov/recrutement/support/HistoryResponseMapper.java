package com.eqinov.recrutement.support;

import com.eqinov.recrutement.data.DataPoint;
import com.eqinov.recrutement.data.Site;
import com.eqinov.recrutement.utils.DateUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * this class maps the response from the third party api for
 * retrieving consumption history.
 */
public class HistoryResponseMapper {
    public String site;
    public String unit;
    public List<DataPointMapper> values;

    /**
     * Convert mapped data from history api
     * to the DataPoint entity.
     *
     * @param site
     * @return
     */
    public List<DataPoint> convertValuesToDataPointForSite(Site site) {
        List<DataPoint> points = new ArrayList<>();

        for (DataPointMapper point : values){
            DataPoint dataPoint = new DataPoint();
            dataPoint.setSite(site);
            dataPoint.setValue(point.value);
            dataPoint.setTime(DateUtils.convertToDateTime(point.date));
            points.add(dataPoint);
        }

        return points;
    }
}
