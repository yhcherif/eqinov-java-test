package com.eqinov.recrutement.controller;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.eqinov.recrutement.support.HistoryResponseMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.eqinov.recrutement.data.DataPoint;
import com.eqinov.recrutement.data.Site;
import com.eqinov.recrutement.repository.DataPointRepository;
import com.eqinov.recrutement.repository.SiteRepository;
import com.eqinov.recrutement.utils.DateUtils;

/**
 * Controller Spring permettant l'affichage des donn�es dans la seule vue de
 * l'application
 * 
 * @author Guillaume SIMON - EQINOV
 * @since 27 janv. 2020
 *
 */
@Controller
public class WelcomeController {

	@Autowired
	private SiteRepository siteRepository;

	@Autowired
	private DataPointRepository dataPointRepository;

	/**
	 * Point d'entr�e de la vue, page d'accueil de l'application
	 */
	@GetMapping("/")
	public String main(Model model) {
		Optional<Site> site = siteRepository.findById(1l);
		if (site.isPresent()) {
			Integer maxYear = dataPointRepository.findTopBySiteOrderByTimeDesc(site.get()).getTime().getYear();
			initModel(site.get(), maxYear, model);
		}
		return "welcome";
	}

	/**
	 * Rafraichi le contenu de la page sur changement d'ann�e
	 * 
	 * @param year  l'ann�e
	 * @param model model transportant les donn�es
	 * @return le fragment a retourn�
	 */
	@GetMapping("/view/refresh")
	public String refresh(@RequestParam Integer year, Model model) {
		Optional<Site> site = siteRepository.findById(1l);
		if (site.isPresent()) {
			initModel(site.get(), year, model);
		}
		return "welcome:: result";
	}

	/**
	 * M�thode interne permettant d'ajouter les donn�es du site pour l'ann�e �
	 * afficher
	 * 
	 * @param site        site � afficher
	 * @param currentYear ann�e s�lectionn�e
	 * @param model       model transportant les donn�es
	 */
	private void initModel(Site site, Integer currentYear, Model model) {
		Integer minYear = dataPointRepository.findTopBySiteOrderByTimeAsc(site).getTime().getYear();
		Integer maxYear = dataPointRepository.findTopBySiteOrderByTimeDesc(site).getTime().getYear();
		List<Integer> years = Stream.iterate(minYear, n -> n + 1).limit((maxYear - minYear) + 1l).map(n -> n)
				.collect(Collectors.toList());
		model.addAttribute("years", years);
		model.addAttribute("currentYear", currentYear);
		model.addAttribute("site", site);
	}

	/**
	 * Retourne les points de consommation d'une ann�e au format json pour highstock
	 * 
	 * @param year ann�e
	 * @return
	 */
	@GetMapping(value = "/data/conso", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public List<double[]> getConso(@RequestParam Integer year) {
		Optional<Site> site = siteRepository.findById(1l);
		List<double[]> result = new ArrayList<>();
		if (site.isPresent()) {
			List<DataPoint> points = getConsumptionRecordsOf(year, site.get());

			result = points.stream().map(point -> {
				double[] array = new double[2];
				array[0] = DateUtils.secondsFromEpoch(point.getTime()) * 1000l;
				array[1] = point.getValue();
				return array;
			}).collect(Collectors.toList());
		}
		return result;
	}

	/**
	 * Return the monthly average consumption for
     * every months of the given year.
	 *
	 * @param year ann�e
	 * @return
	 */
	@GetMapping(value = "/data/conso/months", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public HashMap<Integer, Double> getMonthlyConsumption(@RequestParam Integer year){
		Optional<Site> site = siteRepository.findById(1l);

		if (site.isPresent()) {
			List<DataPoint> points = getConsumptionRecordsOf(year, site.get());

            return getMonthlyAvgConsumption(
                    groupPointsByMonths(points)
            );

		}

		return new HashMap<Integer, Double>();
	}

    /**
     * Retourne la consommation annuelle
     *
     * @param year ann�e
     * @return
     */
    @GetMapping(value = "/data/conso/annual", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Double getAnnualConsumption(@RequestParam Integer year){
        Optional<Site> site = siteRepository.findById(1l);
        Double annualConsumption = 0.0;

        if (site.isPresent()) {
            HashMap<Integer, List<DataPoint>> grouped = groupPointsByMonths(
                    getConsumptionRecordsOf(year, site.get())
            );

            HashMap<Integer, Double> monthsConsumption = getMonthlyAvgConsumption(grouped);

            for(Double value : monthsConsumption.values()){
                annualConsumption += value;
            }

            // Get annual average
            annualConsumption = annualConsumption / monthsConsumption.keySet().size();
        }

        return annualConsumption;
    }

    /**
     * Fetch all consumption records int he database
     * for the given year in the given site.
     *
     * @param year
     * @param site
     * @return
     */
    private List<DataPoint> getConsumptionRecordsOf(@RequestParam Integer year, Site site) {
        return dataPointRepository.findBySiteAndTimeBetween(site,
                LocalDate.of(year, 1, 1).atStartOfDay(),
                LocalDate.of(year, 12, 31).atStartOfDay().with(LocalTime.MAX));
    }


    /**
     * Fetch consumption history from third party
     * and returned most updated years list.
     *
     * @return
     */
    @GetMapping(value = "/data/conso/history", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<Integer> getHistory(){
        Optional<Site> site = siteRepository.findById(1l);

        dataPointRepository.saveAll(
                fetchConsumptionHistoryForSite(site.get())
        );

        return fetchYears(site.get());
    }

    /**
     * Fetch all years of consumption records in the database.
     *
     * @param site
     * @return
     */
    private List<Integer> fetchYears(Site site) {
        Integer minYear = dataPointRepository.findTopBySiteOrderByTimeAsc(site).getTime().getYear();
        Integer maxYear = dataPointRepository.findTopBySiteOrderByTimeDesc(site).getTime().getYear();

        return Stream.iterate(minYear, n -> n + 1).limit((maxYear - minYear) + 1l).map(n -> n)
                .collect(Collectors.toList());
    }

    /**
     * Compute monthly average consumption on given consumption list.
     *
     * @param grouped
     * @return
     */
    private HashMap<Integer, Double> getMonthlyAvgConsumption(HashMap<Integer, List<DataPoint>> grouped){
        HashMap<Integer, Double> result = new HashMap<>();

        for(int i : grouped.keySet()){
            HashMap<String, Integer> groupedHours = new HashMap<>();
            HashMap<String, Double> groupedHoursConso = new HashMap<>();
            Double total = 0.0;

            for (int a = 0; a < grouped.get(i).size(); a++){
                DataPoint current = grouped.get(i).get(a);

                String key = current.getTime().getDayOfMonth() + "-" + current.getTime().getHour();

                if (groupedHours.get(key) == null){
                    groupedHours.put(key, 1);
                    groupedHoursConso.put(key, current.getValue());
                    continue;
                }

                groupedHours.put(key, groupedHours.get(key) + 1);
                groupedHoursConso.put(key, current.getValue() + groupedHoursConso.get(key));

            }

            // Calculate average monthly consumption
            for (String key : groupedHours.keySet()){
                total += groupedHoursConso.get(key) / groupedHours.get(key);
            }

            result.put(i, total);
        }

        return result;
    }

    /**
     * Group the given consumption tracks by months.
     *
     * @param points
     * @return
     */
    private HashMap<Integer, List<DataPoint>> groupPointsByMonths(List<DataPoint> points) {
        HashMap<Integer, List<DataPoint>> group = new HashMap<>();

        for (int i = 1; i < points.size(); i++){
            DataPoint current = points.get(i);
            int month = current.getTime().getMonthValue();

            if (group.get(month) == null){
                List<DataPoint> tempPoints = new ArrayList<>();
                tempPoints.add(current);
                group.put(month, tempPoints);
                continue;
            }

            group.get(month).add(current);
        }

        return group;
    }

    /**
     * Fetch consumption history from third party api
     * and map retrieved data to DataPoint class.
     *
     * @param site
     * @return list
     */
    private List<DataPoint> fetchConsumptionHistoryForSite(Site site) {
        HttpClient httpClient = HttpClientBuilder.create().build();
        List<DataPoint> list = new ArrayList<>();

        try {
            HttpGet request = new HttpGet("http://localhost:2345/api/conso");
            request.addHeader("Accept", "application/json");

            HttpResponse response = httpClient.execute(request);

            String data = EntityUtils.toString(response.getEntity());

            ObjectMapper objectMapper = new ObjectMapper();
            HistoryResponseMapper responseMapper = objectMapper.readValue(data, HistoryResponseMapper.class);

            list = responseMapper.convertValuesToDataPointForSite(site);
        }catch (Exception ex) {
            ex.printStackTrace();
        }

        return list;
    }
}