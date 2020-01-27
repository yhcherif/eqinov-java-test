package com.eqinov.recrutement.controller;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
 * Controller Spring permettant l'affichage des données dans la seule vue de
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
	 * Point d'entrée de la vue, page d'accueil de l'application
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
	 * Rafraichi le contenu de la page sur changement d'année
	 * 
	 * @param year  l'année
	 * @param model model transportant les données
	 * @return le fragment a retourné
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
	 * Méthode interne permettant d'ajouter les données du site pour l'année à
	 * afficher
	 * 
	 * @param site        site à afficher
	 * @param currentYear année sélectionnée
	 * @param model       model transportant les données
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
	 * Retourne les points de consommation d'une année au format json pour highstock
	 * 
	 * @param year année
	 * @return
	 */
	@GetMapping(value = "/data/conso", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public List<double[]> getConso(@RequestParam Integer year) {
		Optional<Site> site = siteRepository.findById(1l);
		List<double[]> result = new ArrayList<>();
		if (site.isPresent()) {
			List<DataPoint> points = dataPointRepository.findBySiteAndTimeBetween(site.get(),
					LocalDate.of(year, 1, 1).atStartOfDay(),
					LocalDate.of(year, 12, 31).atStartOfDay().with(LocalTime.MAX));
			result = points.stream().map(point -> {
				double[] array = new double[2];
				array[0] = DateUtils.secondsFromEpoch(point.getTime()) * 1000l;
				array[1] = point.getValue();
				return array;
			}).collect(Collectors.toList());
		}
		return result;
	}

}