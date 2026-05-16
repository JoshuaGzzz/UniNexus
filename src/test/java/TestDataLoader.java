import com.studentmarketplace.model.Rental;
import com.studentmarketplace.model.Rental.RentalType;
import com.studentmarketplace.service.RentalService;

public class TestDataLoader {
    public static void main(String[] args) {
        RentalService service = new RentalService();

        Rental dorm = new Rental(1, "Sample Dorm", "Near campus", 
                                 RentalType.DORMITORY, "Sampaloc", 5000);
        dorm.setBedrooms(2);
        dorm.setBathrooms(1);
        dorm.setAreaSqm(25);
        
        service.createRental(dorm);
        System.out.println("Test rental created!");
    }
}
