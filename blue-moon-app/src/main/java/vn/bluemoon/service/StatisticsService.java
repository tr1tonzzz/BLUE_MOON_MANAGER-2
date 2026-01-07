package vn.bluemoon.service;

import vn.bluemoon.exception.DbException;
import vn.bluemoon.repository.ApartmentRepository;
import vn.bluemoon.repository.ResidentRepository;
import vn.bluemoon.repository.FeeCollectionRepository;
import vn.bluemoon.repository.HouseholdRepository;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for Statistics
 */
public class StatisticsService {
    private final ApartmentRepository apartmentRepository = new ApartmentRepository();
    private final ResidentRepository residentRepository = new ResidentRepository();
    private final FeeCollectionRepository feeRepository = new FeeCollectionRepository();
    private final HouseholdRepository householdRepository = new HouseholdRepository();
    
    /**
     * Get dashboard statistics
     */
    public Map<String, Object> getDashboardStats() throws DbException {
        Map<String, Object> stats = new HashMap<>();
        
        // Total apartments
        int totalApartments = apartmentRepository.countAll();
        stats.put("totalApartments", totalApartments);
        
        // Total households
        int totalHouseholds = householdRepository.countAll();
        stats.put("totalHouseholds", totalHouseholds);
        
        // Total residents (only owners)
        int totalResidents = residentRepository.countAll();
        stats.put("totalResidents", totalResidents);
        
        // Fee statistics
        List<vn.bluemoon.model.entity.FeeCollection> allFees = feeRepository.findAll();
        long totalFees = allFees.size();
        long paidFees = allFees.stream().filter(f -> "paid".equals(f.getStatus())).count();
        long unpaidFees = totalFees - paidFees;
        
        BigDecimal totalAmount = allFees.stream()
            .map(f -> f.getAmount() != null ? f.getAmount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal paidAmount = allFees.stream()
            .filter(f -> "paid".equals(f.getStatus()))
            .map(f -> f.getPaidAmount() != null ? f.getPaidAmount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        stats.put("totalFees", totalFees);
        stats.put("paidFees", paidFees);
        stats.put("unpaidFees", unpaidFees);
        stats.put("totalAmount", totalAmount);
        stats.put("paidAmount", paidAmount);
        
        // Recent fee updates (last 3)
        List<vn.bluemoon.model.entity.FeeCollection> recentFees = allFees.stream()
            .sorted((a, b) -> {
                if (a.getUpdatedAt() == null && b.getUpdatedAt() == null) return 0;
                if (a.getUpdatedAt() == null) return 1;
                if (b.getUpdatedAt() == null) return -1;
                return b.getUpdatedAt().compareTo(a.getUpdatedAt());
            })
            .limit(3)
            .collect(java.util.stream.Collectors.toList());
        stats.put("recentFees", recentFees);
        
        // Population fluctuation (last 6 months)
        Map<String, Integer> populationFluctuation = getPopulationFluctuation();
        stats.put("populationFluctuation", populationFluctuation);
        
        return stats;
    }
    
    /**
     * Get population fluctuation data for chart
     */
    private Map<String, Integer> getPopulationFluctuation() throws DbException {
        Map<String, Integer> data = new HashMap<>();
        // This is a simplified version - in real app, you'd query by month
        int currentHouseholds = householdRepository.countAll();
        for (int i = 0; i < 6; i++) {
            // Simplified: use current count for all months
            // In production, you'd query historical data
            data.put("Thg " + (i + 1), currentHouseholds);
        }
        return data;
    }
}





