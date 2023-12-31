package ru.mngerasimenko.air_carriers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class AirCarriers {
	public static final String DEFAULT_SOURCE_FILE_NAME = "tickets.json";
	public static final String DEFAULT_ORIGINAL_CITY_NAME = "Владивосток";
	public static final String DEFAULT_ARRIVAL_CITY_NAME = "Тель-Авив";

	public static void main(String[] args) {
		String sourceFileName = getSourceFileName(args);
		Tickets tickets = parseTicketsFromFile(sourceFileName);

		String originCityName = getOriginCityName(args);
		String destinationCityName = getArrivalCityName(args);
		Map<String, Long> minTime = getMinimalTimeByCarriers(tickets, originCityName, destinationCityName);
		showMinimalTime(minTime);

		List<Integer> prices = getSortedPrices(tickets, originCityName, destinationCityName);
		if (prices.isEmpty()) {
			System.out.println("Unable to find price for the right flight");
			System.exit(1);
		}
		double averagePrice = getAveragePrice(prices);
		System.out.println("Average price: " + averagePrice);

		double medianPrice = getMedianPrice(prices);
		System.out.println("Median price: " + medianPrice);

		System.out.println("The difference between the average price and the median: "
				+ Math.abs(averagePrice - medianPrice));
	}

	private static String getSourceFileName(String[] args) {
		if (args.length > 0) {
			return args[0];
		}
		return DEFAULT_SOURCE_FILE_NAME;
	}

	private static String getOriginCityName(String[] args) {
		if (args.length > 2) {
			return args[1];
		}
		return DEFAULT_ORIGINAL_CITY_NAME;
	}

	private static String getArrivalCityName(String[] args) {
		if (args.length > 2) {
			return args[2];
		}
		return DEFAULT_ARRIVAL_CITY_NAME;
	}

	private static Tickets parseTicketsFromFile(String sourceFileName) {
		ObjectMapper objectMapper = JsonMapper.builder()
				.addModule(new JavaTimeModule())
				.build();

		Tickets tickets;
		try {
			tickets = objectMapper.readValue(new File(sourceFileName), Tickets.class);
		} catch (FileNotFoundException e) {
			System.out.println("Unable to find file: " + sourceFileName);
			throw new RuntimeException(e);
		} catch (IOException e) {
			System.out.println("Invalid file format. File name: " + sourceFileName);
			throw new RuntimeException(e);
		}

		return tickets;
	}

	private static Map<String, Long> getMinimalTimeByCarriers(Tickets tickets, String originName, String destinationName) {
		Map<String, List<Long>> flightTimeByCarriers = tickets.getTickets().stream()
				.filter(t -> t.getOriginName().equals(originName)
						&& t.getDestinationName().equals(destinationName))
				.collect(Collectors.groupingBy(
								Ticket::getCarrier,
								Collectors.mapping(
										AirCarriers::getFlightTime,
										Collectors.toList())
						)
				);

		Map<String, Long> minTimeByCarriers = new HashMap<>();
		for (Map.Entry<String, List<Long>> pair : flightTimeByCarriers.entrySet()) {
			minTimeByCarriers.put(pair.getKey(), pair.getValue().stream().min(Long::compareTo).get());
		}

		return minTimeByCarriers;
	}

	private static long getFlightTime(Ticket ticket) {
		LocalDateTime departureDateTime = LocalDateTime.of(ticket.getDepartureDate(), ticket.getDepartureTime());
		ZonedDateTime zdt = ZonedDateTime.of(departureDateTime, ZoneId.systemDefault());
		long departureMillis = zdt.toInstant().toEpochMilli();

		LocalDateTime arrivalDateTime = LocalDateTime.of(ticket.getArrivalDate(), ticket.getArrivalTime());
		zdt = ZonedDateTime.of(arrivalDateTime, ZoneId.systemDefault());
		long arrivalMillis = zdt.toInstant().toEpochMilli();

		return arrivalMillis - departureMillis;
	}

	private static void showMinimalTime(Map<String, Long> minTime) {
		System.out.println("Minimal time: ");
		for (Map.Entry<String, Long> pair : minTime.entrySet()) {
			System.out.print(pair.getKey() + ": ");
			long hours = TimeUnit.MILLISECONDS.toHours(pair.getValue());
			long minutes = TimeUnit.MILLISECONDS.toMinutes(pair.getValue()) -
					TimeUnit.HOURS.toMinutes(hours);
			long seconds = TimeUnit.MILLISECONDS.toSeconds(pair.getValue()) -
					TimeUnit.HOURS.toSeconds(hours) -
					TimeUnit.MINUTES.toSeconds(minutes);

			System.out.println(hours + " hours, " + minutes + " minutes, " + seconds + " seconds");
		}
	}

	private static List<Integer> getSortedPrices(Tickets tickets, String originCityName, String destinationName) {
		return tickets.getTickets().stream()
				.filter(t -> t.getOriginName().equals(originCityName)
						&& t.getDestinationName().equals(destinationName))
				.map(Ticket::getPrice).sorted().collect(Collectors.toList());
	}

	private static double getAveragePrice(List<Integer> prices) {
		int sum = prices.stream().mapToInt(i -> i).sum();
		return sum / prices.size();
	}

	private static double getMedianPrice(List<Integer> prices) {
		if (prices.size() % 2 == 0)
			return ((double) prices.get(prices.size() / 2) + (double) prices.get(prices.size() / 2 - 1)) / 2;
		else
			return (double) prices.get(prices.size() / 2);
	}
}
