package com.librarymanagement.backend.config;

import com.librarymanagement.backend.entity.*;
import com.librarymanagement.backend.repository.BookInventoryRepository;
import com.librarymanagement.backend.repository.BookRepository;
import com.librarymanagement.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@Profile("!test")
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final BookInventoryRepository bookInventoryRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           BookRepository bookRepository,
                           BookInventoryRepository bookInventoryRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
        this.bookInventoryRepository = bookInventoryRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            log.info("No users found in database. Initializing default demo accounts...");

            // 1. Admin account
            User admin = new User();
            admin.setName("System Administrator");
            admin.setEmail("admin@library.com");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setPhone("9876543210");
            admin.setRole(Role.ADMIN);
            userRepository.save(admin);

            // 2. Demo Member account
            User member = new User();
            member.setName("John Reader");
            member.setEmail("member@library.com");
            member.setPassword(passwordEncoder.encode("member123"));
            member.setPhone("9123456780");
            member.setRole(Role.MEMBER);
            userRepository.save(member);

            log.info("Initialized default users: admin@library.com and member@library.com");
        }

        if (bookRepository.count() == 0) {
            log.info("No books found in database. Initializing starter catalog and inventory copies...");

            seedBook(
                    "Clean Code",
                    "Robert C. Martin",
                    "978-0132350884",
                    "Even bad code can function. But if code isn't clean, it can bring a development organization to its knees.",
                    "Prentice Hall",
                    LocalDate.of(2008, 8, 1),
                    "Technology",
                    new String[]{"ACC-CC-01", "ACC-CC-02"},
                    new String[]{"Stack Tech-1A", "Stack Tech-1B"}
            );

            seedBook(
                    "The Pragmatic Programmer",
                    "Andrew Hunt, David Thomas",
                    "978-0201616224",
                    "One of the most significant books on programming, covering software engineering best practices.",
                    "Addison-Wesley",
                    LocalDate.of(1999, 10, 20),
                    "Technology",
                    new String[]{"ACC-PP-01", "ACC-PP-02"},
                    new String[]{"Stack Tech-2A", "Stack Tech-2B"}
            );

            seedBook(
                    "Design Patterns: Elements of Reusable Object-Oriented Software",
                    "Erich Gamma, Richard Helm, Ralph Johnson, John Vlissides",
                    "978-0201633610",
                    "Capturing a wealth of experience about the design of object-oriented software.",
                    "Addison-Wesley",
                    LocalDate.of(1994, 10, 21),
                    "Computer Science",
                    new String[]{"ACC-DP-01", "ACC-DP-02", "ACC-DP-03"},
                    new String[]{"Stack CS-1", "Stack CS-2", "Stack CS-3"}
            );

            seedBook(
                    "A Brief History of Time",
                    "Stephen Hawking",
                    "978-0553380163",
                    "From the Big Bang to black holes, exploring the cosmos and the frontiers of theoretical physics.",
                    "Bantam Books",
                    LocalDate.of(1988, 4, 1),
                    "Science",
                    new String[]{"ACC-BHT-01"},
                    new String[]{"Stack SCI-4"}
            );

            seedBook(
                    "Sapiens: A Brief History of Humankind",
                    "Yuval Noah Harari",
                    "978-0062316097",
                    "From a renowned historian comes a groundbreaking narrative of humanity's creation and evolution.",
                    "Harper",
                    LocalDate.of(2014, 2, 10),
                    "History",
                    new String[]{"ACC-SAP-01", "ACC-SAP-02"},
                    new String[]{"Stack HIST-2A", "Stack HIST-2B"}
            );

            seedBook(
                    "Zero to One: Notes on Startups",
                    "Peter Thiel, Blake Masters",
                    "978-0804139298",
                    "The great secret of our time is that there are still uncharted frontiers and new inventions to create.",
                    "Crown Business",
                    LocalDate.of(2014, 9, 16),
                    "Business",
                    new String[]{"ACC-ZTO-01"},
                    new String[]{"Stack BIZ-1"}
            );

            log.info("Catalog initialization complete.");
        }
    }

    private void seedBook(String title, String author, String isbn, String description,
                           String publisher, LocalDate publicationDate, String category,
                           String[] accessionNumbers, String[] shelfLocations) {
        Book book = new Book();
        book.setTitle(title);
        book.setAuthor(author);
        book.setIsbn(isbn);
        book.setDescription(description);
        book.setPublisher(publisher);
        book.setPublicationDate(publicationDate);
        book.setCategory(category);
        book.setStatus(BookStatus.AVAILABLE);

        Book savedBook = bookRepository.save(book);

        for (int i = 0; i < accessionNumbers.length; i++) {
            BookInventory copy = new BookInventory();
            copy.setBook(savedBook);
            copy.setAccessionNumber(accessionNumbers[i]);
            copy.setShelfLocation(shelfLocations[i]);
            copy.setStatus(BookStatus.AVAILABLE);
            bookInventoryRepository.save(copy);
        }
    }
}
