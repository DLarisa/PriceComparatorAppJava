package com.market.pricecomparator.util;

import com.market.pricecomparator.model.Discount;
import com.market.pricecomparator.model.Product;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class CsvLoader {
    private static final Logger logger = Logger.getLogger(CsvLoader.class.getName());
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public List<Product> loadProducts(String filePath) {
        List<Product> products = new ArrayList<>();
        try (Stream<String> lines = Files.lines(Paths.get(filePath)).skip(1)) {
            lines.forEach(line -> {
                String[] values = line.split(";");
                Product p = new Product();
                p.setProductId(values[0]);
                p.setProductName(values[1]);
                p.setCategory(values[2]);
                p.setBrand(values[3]);
                p.setQuantity(Double.parseDouble(values[4]));
                p.setUnit(values[5]);
                p.setPrice(Double.parseDouble(values[6]));
                p.setCurrency(values[7]);
                p.setStore(extractStoreNameFromFilename(filePath));
                p.setDate(extractDateFromFilename(filePath));
                products.add(p);
            });
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error at reading CSV Products: ", e);
        }
        return products;
    }

    public List<Discount> loadDiscounts(String filePath) {
        List<Discount> discounts = new ArrayList<>();
        try (Stream<String> lines = Files.lines(Paths.get(filePath)).skip(1)) {
            lines.forEach(line -> {
                String[] values = line.split(";");
                Discount d = new Discount();
                d.setProductId(values[0]);
                d.setProductName(values[1]);
                d.setBrand(values[2]);
                d.setQuantity(Double.parseDouble(values[3]));
                d.setUnit(values[4]);
                d.setCurrency(values[5]);
                d.setFromDate(LocalDate.parse(values[6]));
                d.setToDate(LocalDate.parse(values[7]));
                d.setPercentage(Integer.parseInt(values[8]));
                d.setStore(extractStoreNameFromFilename(filePath));
                discounts.add(d);
            });
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error at reading CSV Discounts: ", e);
        }
        return discounts;
    }

    private LocalDate extractDateFromFilename(String filePath) {
        Pattern datePattern = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
        Matcher matcher = datePattern.matcher(filePath);
        if (matcher.find()) {
            return LocalDate.parse(matcher.group(), DATE_FORMAT);
        }
        return LocalDate.now();
    }

    private String extractStoreNameFromFilename(String filePath) {
        String fileName = Paths.get(filePath).getFileName().toString();
        Pattern storePattern = Pattern.compile("^(.*?)_");
        Matcher matcher = storePattern.matcher(fileName);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new IllegalArgumentException("Cannot extract store name from filename: " + fileName);
    }
}
