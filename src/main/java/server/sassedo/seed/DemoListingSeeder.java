package server.sassedo.seed;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import server.sassedo.listing.common.ExtraService;
import server.sassedo.listing.common.LeaseTerm;
import server.sassedo.listing.common.ListingStatus;
import server.sassedo.listing.common.NearbyAmenity;
import server.sassedo.listing.common.OccupationPreference;
import server.sassedo.listing.common.PetPolicy;
import server.sassedo.listing.common.PropertyAmenity;
import server.sassedo.listing.common.PropertyType;
import server.sassedo.listing.common.RoomAmenity;
import server.sassedo.listing.common.SmokerPreference;
import server.sassedo.listing.common.SmokingPolicy;
import server.sassedo.listing.common.Utility;
import server.sassedo.listing.rental.data.dto.RentalListing;
import server.sassedo.listing.rental.data.dto.RentalListingPhoto;
import server.sassedo.listing.rental.repository.RentalListingRepository;
import server.sassedo.listing.roommate.data.dto.RoommateListing;
import server.sassedo.listing.roommate.data.dto.RoommateListingPhoto;
import server.sassedo.listing.roommate.repository.RoommateListingRepository;
import server.sassedo.listing.search.data.dto.ApartmentSearch;
import server.sassedo.listing.search.repository.ApartmentSearchRepository;
import server.sassedo.location.data.dto.City;
import server.sassedo.location.data.dto.Country;
import server.sassedo.location.repository.CityRepository;
import server.sassedo.location.repository.CountryRepository;
import server.sassedo.user.data.dto.ERole;
import server.sassedo.user.data.dto.JobStatus;
import server.sassedo.user.data.dto.Language;
import server.sassedo.user.data.dto.Occupation;
import server.sassedo.user.data.dto.Role;
import server.sassedo.user.data.dto.Sex;
import server.sassedo.user.data.dto.User;
import server.sassedo.user.repository.RoleRepository;
import server.sassedo.user.repository.UserRepository;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Populates the database with demo listings for local testing. Guarded by
 * {@code sassedo.seed.demo-listings.enabled=true} (default off). Creates 100 listings of each type
 * (roommate, rental, apartment search) owned by generated demo users, with property photos
 * downloaded once from the internet and stored as BLOBs.
 *
 * <p>Idempotent: if the demo owners already exist the seeder assumes it has run and exits, so
 * restarting the app will not duplicate data. To re-seed, delete the demo users/listings first.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "sassedo.seed.demo-listings.enabled", havingValue = "true")
public class DemoListingSeeder implements ApplicationRunner {

    private static final int PER_TYPE = 100;
    private static final int OWNER_COUNT = 12;
    private static final String OWNER_EMAIL_PREFIX = "demoowner";
    private static final String OWNER_EMAIL_DOMAIN = "@sassedo.demo";
    private static final String OWNER_PASSWORD = "Password123!";
    private static final String DEMO_MARKER = "[DEMO] ";

    // Curated Unsplash property/interior photos (direct CDN URLs, downloaded once and reused).
    private static final List<String> IMAGE_URLS = List.of(
            "https://images.unsplash.com/photo-1505691938895-1758d7feb511?auto=format&fit=crop&w=1200&q=70",
            "https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?auto=format&fit=crop&w=1200&q=70",
            "https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=1200&q=70",
            "https://images.unsplash.com/photo-1493809842364-78817add7ffb?auto=format&fit=crop&w=1200&q=70",
            "https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?auto=format&fit=crop&w=1200&q=70",
            "https://images.unsplash.com/photo-1554995207-c18c203602cb?auto=format&fit=crop&w=1200&q=70",
            "https://images.unsplash.com/photo-1484154218962-a197022b5858?auto=format&fit=crop&w=1200&q=70",
            "https://images.unsplash.com/photo-1493663284031-b7e3aefcae8e?auto=format&fit=crop&w=1200&q=70",
            "https://images.unsplash.com/photo-1516455590571-18256e5bb9ff?auto=format&fit=crop&w=1200&q=70",
            "https://images.unsplash.com/photo-1502005229762-cf1b2da7c5d6?auto=format&fit=crop&w=1200&q=70",
            "https://images.unsplash.com/photo-1512917774080-9991f1c4c750?auto=format&fit=crop&w=1200&q=70",
            "https://images.unsplash.com/photo-1568605114967-8130f3a36994?auto=format&fit=crop&w=1200&q=70",
            "https://images.unsplash.com/photo-1580587771525-78b9dba3b914?auto=format&fit=crop&w=1200&q=70",
            "https://images.unsplash.com/photo-1600585154340-be6161a56a0c?auto=format&fit=crop&w=1200&q=70",
            "https://images.unsplash.com/photo-1600566753086-00f18fb6b3ea?auto=format&fit=crop&w=1200&q=70",
            "https://images.unsplash.com/photo-1600607687939-ce8a6c25118c?auto=format&fit=crop&w=1200&q=70"
    );

    private static final String[][] CITIES = {
            {"Sofia", "София"},
            {"Plovdiv", "Пловдив"},
            {"Varna", "Варна"},
            {"Burgas", "Бургас"},
            {"Ruse", "Русе"}
    };

    private static final String[] NEIGHBORHOODS = {
            "Lozenets", "Mladost", "Center", "Studentski grad", "Vitosha",
            "Sea Garden", "Kamenitsa", "Trakia", "Izgrev", "Druzhba"
    };

    private static final String[] FIRST_NAMES = {
            "Alexander", "Maria", "Nikolay", "Elena", "Georgi", "Ivana",
            "Petar", "Sofia", "Dimitar", "Viktoria", "Stefan", "Gabriela"
    };

    private static final String[] LAST_NAMES = {
            "Ivanov", "Petrova", "Georgiev", "Dimitrova", "Kolev", "Stoyanova",
            "Todorov", "Nikolova", "Angelov", "Marinova", "Vasilev", "Ilieva"
    };

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CountryRepository countryRepository;
    private final CityRepository cityRepository;
    private final RoommateListingRepository roommateListingRepository;
    private final RentalListingRepository rentalListingRepository;
    private final ApartmentSearchRepository apartmentSearchRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${sassedo.listings.ttl-days:30}")
    private long listingTtlDays;

    private final Random random = new Random(42);
    private final Map<String, byte[]> imageCache = new HashMap<>();

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.existsByEmail(OWNER_EMAIL_PREFIX + "1" + OWNER_EMAIL_DOMAIN)) {
            log.info("[demo-seed] Demo owners already exist; skipping demo listing seeding.");
            return;
        }

        log.info("[demo-seed] Seeding demo data: {} owners and {} listings per type...",
                OWNER_COUNT, PER_TYPE);

        List<City> cities = ensureCities();
        List<User> owners = createOwners(cities);
        preloadImages();

        for (int i = 0; i < PER_TYPE; i++) {
            roommateListingRepository.save(buildRoommate(owners, cities, i));
            rentalListingRepository.save(buildRental(owners, cities, i));
            apartmentSearchRepository.save(buildApartmentSearch(owners, cities, i));
        }

        log.info("[demo-seed] Done. Created {} roommate, {} rental and {} apartment-search listings.",
                PER_TYPE, PER_TYPE, PER_TYPE);
    }

    // ---- Reference data --------------------------------------------------

    private List<City> ensureCities() {
        Country bulgaria = countryRepository.findAll().stream()
                .filter(c -> "Bulgaria".equalsIgnoreCase(c.getNameEn()))
                .findFirst()
                .orElseGet(() -> countryRepository.save(new Country("Bulgaria", "България", "BG")));

        List<City> cities = new ArrayList<>();
        for (String[] city : CITIES) {
            City existing = cityRepository.findByCountryId(bulgaria.getId()).stream()
                    .filter(c -> city[0].equalsIgnoreCase(c.getNameEn()))
                    .findFirst()
                    .orElse(null);
            cities.add(existing != null ? existing
                    : cityRepository.save(new City(city[0], city[1], bulgaria)));
        }
        return cities;
    }

    private List<User> createOwners(List<City> cities) {
        Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                .orElseThrow(() -> new IllegalStateException(
                        "ROLE_USER not found. Start the app once so roles are seeded."));

        List<User> owners = new ArrayList<>();
        for (int i = 1; i <= OWNER_COUNT; i++) {
            User user = new User();
            String first = FIRST_NAMES[(i - 1) % FIRST_NAMES.length];
            String last = LAST_NAMES[(i - 1) % LAST_NAMES.length];
            user.setEmail(OWNER_EMAIL_PREFIX + i + OWNER_EMAIL_DOMAIN);
            user.setPassword(passwordEncoder.encode(OWNER_PASSWORD));
            user.setName(first + " " + last);
            user.setFirstName(first);
            user.setLastName(last);
            user.setPhone("+3598" + String.format("%08d", i));
            user.setAge(20 + (i % 25));
            user.setSex(pick(Sex.values()));
            user.setLanguages(new LinkedHashSet<>(List.of(Language.ENGLISH, Language.BULGARIAN)));
            user.setJobStatus(pick(JobStatus.values()));
            user.setOccupation(pick(Occupation.values()));
            user.setSmokingPreference(pick(SmokerPreference.values()));
            user.setPetPolicy(pick(PetPolicy.values()));
            user.setShortDescription("Demo owner profile #" + i);
            if (cities != null && !cities.isEmpty()) {
                City preferredCity = cities.get(i % cities.size());
                user.getPreferences().setPreferredCity(preferredCity);
                user.getPreferences().setPreferredCountry(preferredCity.getCountry());
            }
            user.setEnabled(true);
            user.setBlocked(false);
            user.setTermsAndConditionsAccepted(true);
            user.setGdprAccepted(true);
            user.setTermsAndConditionsAcceptedAt(LocalDateTime.now());
            user.setGdprAcceptedAt(LocalDateTime.now());
            Set<Role> roles = new LinkedHashSet<>();
            roles.add(userRole);
            user.setRoles(roles);
            owners.add(userRepository.save(user));
        }
        return owners;
    }

    private void preloadImages() {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        for (String url : IMAGE_URLS) {
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                        .timeout(Duration.ofSeconds(20))
                        .GET()
                        .build();
                HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
                if (response.statusCode() == 200 && response.body().length > 0) {
                    imageCache.put(url, response.body());
                } else {
                    log.warn("[demo-seed] Image download returned {} for {}", response.statusCode(), url);
                }
            } catch (Exception e) {
                log.warn("[demo-seed] Failed to download image {}: {}", url, e.getMessage());
            }
        }
        log.info("[demo-seed] Cached {}/{} demo images.", imageCache.size(), IMAGE_URLS.size());
    }

    // ---- Listing builders ------------------------------------------------

    private RoommateListing buildRoommate(List<User> owners, List<City> cities, int index) {
        RoommateListing l = new RoommateListing();
        l.setOwnerId(pick(owners).getId());
        l.setStatus(ListingStatus.ACTIVE);
        l.setExpiresAt(LocalDateTime.now().plusDays(listingTtlDays));
        l.setPropertyType(pick(PropertyType.values()));
        City city = pick(cities);
        l.setCountry(city.getCountry());
        l.setCity(city);
        l.setNeighborhood(pick(NEIGHBORHOODS));
        l.setAddress((100 + random.nextInt(200)) + " Demo St.");
        l.setRent(BigDecimal.valueOf(300 + random.nextInt(900)));
        l.setCostsIncluded(random.nextBoolean());
        l.setDeposit(BigDecimal.valueOf(300 + random.nextInt(600)));
        l.setRoomsCount(1 + random.nextInt(4));
        l.setFurnished(random.nextBoolean());
        l.setPetsAllowed(random.nextBoolean());
        setAvailability(index, l::setAvailableAsap, l::setAvailableFrom);
        l.setBedrooms(1 + random.nextInt(4));
        l.setBathrooms(1 + random.nextInt(3));
        l.setOwner(random.nextBoolean());
        l.setIncludedUtilities(randomSubset(Utility.values(), 1, 4));
        l.setExtraServices(randomSubset(ExtraService.values(), 0, 3));
        l.setNearbyAmenities(randomSubset(NearbyAmenity.values(), 1, 4));
        l.setRoomAmenities(randomSubset(RoomAmenity.values(), 1, 4));

        // Step 4 requirements - varied (and sometimes absent) so match scores spread out.
        l.setPreferredSex(maybe(0.6) ? pick(Sex.values()) : null);
        if (maybe(0.7)) {
            int min = 18 + random.nextInt(15);
            l.setAgeMin(min);
            l.setAgeMax(min + 5 + random.nextInt(15));
        }
        l.setSmokingPreference(maybe(0.7) ? pick(SmokerPreference.values()) : null);
        l.setOccupationPreference(maybe(0.7) ? pick(OccupationPreference.values()) : null);
        l.setPetPolicy(maybe(0.7) ? pick(PetPolicy.values()) : null);
        l.setEmploymentStatus(maybe(0.6) ? pick(JobStatus.values()) : null);
        l.setSpokenLanguages(maybe(0.7)
                ? randomSubset(new Language[]{Language.ENGLISH, Language.BULGARIAN, Language.GERMAN,
                Language.SPANISH, Language.FRENCH}, 1, 2)
                : new LinkedHashSet<>());
        l.setPeopleInProperty(1 + random.nextInt(4));
        l.setAdditionalRequirements("Looking for a tidy, friendly roommate.");
        l.setAboutMe("I'm a demo lister who enjoys a calm home.");

        l.setTitle(DEMO_MARKER + "Roommate wanted in " + l.getNeighborhood() + " #" + (index + 1));
        l.setDescription("Demo roommate listing for testing the matching system. "
                + "Comfortable room in a shared apartment.");
        attachRoommatePhotos(l);
        return l;
    }

    private RentalListing buildRental(List<User> owners, List<City> cities, int index) {
        RentalListing l = new RentalListing();
        l.setOwnerId(pick(owners).getId());
        l.setStatus(ListingStatus.ACTIVE);
        l.setExpiresAt(LocalDateTime.now().plusDays(listingTtlDays));
        l.setPropertyType(pick(PropertyType.values()));
        City city = pick(cities);
        l.setCountry(city.getCountry());
        l.setCity(city);
        l.setNeighborhood(pick(NEIGHBORHOODS));
        l.setAddress((100 + random.nextInt(200)) + " Demo Blvd.");
        l.setRent(BigDecimal.valueOf(400 + random.nextInt(1600)));
        setAvailability(index, l::setAvailableAsap, l::setAvailableFrom);
        l.setBedrooms(1 + random.nextInt(4));
        l.setBathrooms(1 + random.nextInt(3));
        l.setSharedBedroom(random.nextBoolean());
        l.setSharedBathroom(random.nextBoolean());
        l.setOwner(random.nextBoolean());
        l.setPetPolicy(pick(PetPolicy.values()));
        l.setSmokingPolicy(pick(SmokingPolicy.values()));
        l.setIncludedUtilities(randomSubset(Utility.values(), 1, 4));
        l.setExtraServices(randomSubset(ExtraService.values(), 0, 3));
        l.setLeaseTerms(randomSubset(LeaseTerm.values(), 1, 3));
        l.setNearbyAmenities(randomSubset(NearbyAmenity.values(), 1, 4));
        l.setPropertyAmenities(randomSubset(PropertyAmenity.values(), 2, 6));
        l.setAdditionalDetails("Bright and well-maintained property.");
        l.setTitle(DEMO_MARKER + "Rental in " + l.getNeighborhood() + " #" + (index + 1));
        l.setDescription("Demo rental listing for testing. Spacious property in a great location.");
        attachRentalPhotos(l);
        return l;
    }

    private ApartmentSearch buildApartmentSearch(List<User> owners, List<City> cities, int index) {
        ApartmentSearch s = new ApartmentSearch();
        s.setOwnerId(pick(owners).getId());
        s.setStatus(ListingStatus.ACTIVE);
        s.setExpiresAt(LocalDateTime.now().plusDays(listingTtlDays));
        s.setPropertyType(pick(PropertyType.values()));
        City city = pick(cities);
        s.setCountry(city.getCountry());
        s.setCity(city);
        s.setNeighborhood(pick(NEIGHBORHOODS));
        int budgetMin = 300 + random.nextInt(400);
        s.setBudgetMin(BigDecimal.valueOf(budgetMin));
        s.setBudgetMax(BigDecimal.valueOf(budgetMin + 200 + random.nextInt(800)));
        setAvailability(index, s::setAvailableAsap, s::setAvailableFrom);
        s.setLeaseTerms(randomSubset(LeaseTerm.values(), 1, 3));
        s.setAge(20 + random.nextInt(30));
        s.setSex(pick(Sex.values()));
        s.setProfession(pick(new String[]{"Engineer", "Student", "Designer", "Teacher", "Nurse"}));
        s.setSmoker(pick(SmokerPreference.values()));
        s.setHasPets(random.nextBoolean());
        s.setTitle(DEMO_MARKER + "Looking for a place in " + city.getNameEn() + " #" + (index + 1));
        s.setDescription("Demo apartment search for testing. Responsible tenant looking for a home.");
        return s;
    }

    // ---- Photos ----------------------------------------------------------

    private void attachRoommatePhotos(RoommateListing listing) {
        List<byte[]> images = pickImages();
        for (int i = 0; i < images.size(); i++) {
            RoommateListingPhoto photo = new RoommateListingPhoto();
            photo.setData(images.get(i));
            photo.setContentType("image/jpeg");
            photo.setMain(i == 0);
            photo.setListing(listing);
            listing.getPhotos().add(photo);
        }
    }

    private void attachRentalPhotos(RentalListing listing) {
        List<byte[]> images = pickImages();
        for (int i = 0; i < images.size(); i++) {
            RentalListingPhoto photo = new RentalListingPhoto();
            photo.setData(images.get(i));
            photo.setContentType("image/jpeg");
            photo.setMain(i == 0);
            photo.setListing(listing);
            listing.getPhotos().add(photo);
        }
    }

    private List<byte[]> pickImages() {
        List<byte[]> available = new ArrayList<>(imageCache.values());
        if (available.isEmpty()) {
            return List.of();
        }
        int count = 1 + random.nextInt(3);
        List<byte[]> chosen = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            chosen.add(available.get(random.nextInt(available.size())));
        }
        return chosen;
    }

    // ---- Helpers ---------------------------------------------------------

    private void setAvailability(int index, java.util.function.Consumer<Boolean> asapSetter,
                                 java.util.function.Consumer<LocalDate> fromSetter) {
        if (index % 3 == 0) {
            asapSetter.accept(true);
            fromSetter.accept(null);
        } else {
            asapSetter.accept(false);
            fromSetter.accept(LocalDate.now().plusDays(7L + random.nextInt(60)));
        }
    }

    @SafeVarargs
    private <T> T pick(T... values) {
        return values[random.nextInt(values.length)];
    }

    private <T> T pick(List<T> values) {
        return values.get(random.nextInt(values.size()));
    }

    private boolean maybe(double probability) {
        return random.nextDouble() < probability;
    }

    private <T> Set<T> randomSubset(T[] values, int min, int max) {
        List<T> pool = new ArrayList<>(Arrays.asList(values));
        java.util.Collections.shuffle(pool, random);
        int count = min + (max > min ? random.nextInt(max - min + 1) : 0);
        count = Math.min(count, pool.size());
        return new LinkedHashSet<>(pool.subList(0, count));
    }
}
