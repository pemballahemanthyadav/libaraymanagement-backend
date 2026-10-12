package com.librarymanagement.backend;

import com.librarymanagement.backend.entity.*;
import com.librarymanagement.backend.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 3 Integration Tests for Entities and Repositories
 *
 * @DataJpaTest: A Spring Boot test slice that:
 *   - Only loads JPA components (entities, repositories), NOT the full application context
 *   - Uses the H2 in-memory database (configured in src/test/resources/application.properties)
 *   - Rolls back each test in a transaction so tests don't affect each other
 *
 * @ActiveProfiles("test"): Could be used to activate test profile, but here
 *   we rely on the test application.properties being automatically picked up.
 *
 * These tests verify that:
 *   1. Entities map correctly to database tables (Hibernate schema generation works)
 *   2. Repository save/find operations work as expected
 *   3. Relationships between entities are correctly set up
 *   4. Custom query methods work
 */
@DataJpaTest
class LibraryManagementPhase3Tests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BookInventoryRepository inventoryRepository;

    @Autowired
    private LoanRepository loanRepository;

    // =================== USER TESTS ===================

    @Test
    @DisplayName("Should save and retrieve a User with MEMBER role")
    void testSaveAndRetrieveUser() {
        // Arrange: create a new user
        User user = new User("Alice Smith", "alice@example.com", "hashed_password_here", Role.MEMBER);
        user.setPhone("9876543210");

        // Act: save to H2 in-memory database
        User saved = userRepository.save(user);

        // Assert: verify the saved user has an auto-generated ID and correct fields
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Alice Smith");
        assertThat(saved.getEmail()).isEqualTo("alice@example.com");
        assertThat(saved.getRole()).isEqualTo(Role.MEMBER);
        assertThat(saved.getCreatedAt()).isNotNull(); // @PrePersist should have set this
    }

    @Test
    @DisplayName("Should find a User by email")
    void testFindUserByEmail() {
        // Arrange
        User user = new User("Bob Jones", "bob@example.com", "hashed_password", Role.MEMBER);
        userRepository.save(user);

        // Act
        var found = userRepository.findByEmail("bob@example.com");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Bob Jones");
    }

    @Test
    @DisplayName("Should return empty Optional for non-existent email")
    void testFindUserByEmail_NotFound() {
        var found = userRepository.findByEmail("nonexistent@example.com");
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should detect duplicate email")
    void testExistsByEmail() {
        User user = new User("Carol White", "carol@example.com", "hashed_password", Role.MEMBER);
        userRepository.save(user);

        assertThat(userRepository.existsByEmail("carol@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("other@example.com")).isFalse();
    }

    // =================== BOOK TESTS ===================

    @Test
    @DisplayName("Should save and retrieve a Book with auto timestamps")
    void testSaveAndRetrieveBook() {
        // Arrange
        Book book = new Book("Clean Code", "Robert C. Martin", "978-0132350884", BookStatus.AVAILABLE);
        book.setDescription("A handbook of agile software craftsmanship");
        book.setCategory("Computer Science");
        book.setPublisher("Prentice Hall");

        // Act
        Book saved = bookRepository.save(book);

        // Assert
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTitle()).isEqualTo("Clean Code");
        assertThat(saved.getIsbn()).isEqualTo("978-0132350884");
        assertThat(saved.getStatus()).isEqualTo(BookStatus.AVAILABLE);
        assertThat(saved.getCreatedAt()).isNotNull(); // @PrePersist should set this
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should find a Book by ISBN")
    void testFindBookByIsbn() {
        Book book = new Book("The Pragmatic Programmer", "David Thomas", "978-0135957059", BookStatus.AVAILABLE);
        bookRepository.save(book);

        var found = bookRepository.findByIsbn("978-0135957059");
        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("The Pragmatic Programmer");
    }

    @Test
    @DisplayName("Should detect duplicate ISBN")
    void testExistsByIsbn() {
        Book book = new Book("Design Patterns", "Gang of Four", "978-0201633610", BookStatus.AVAILABLE);
        bookRepository.save(book);

        assertThat(bookRepository.existsByIsbn("978-0201633610")).isTrue();
        assertThat(bookRepository.existsByIsbn("000-0000000000")).isFalse();
    }

    @Test
    @DisplayName("Should search books by keyword (title)")
    void testSearchBooksByTitle() {
        bookRepository.save(new Book("Java Programming", "James Gosling", "111-111", BookStatus.AVAILABLE));
        bookRepository.save(new Book("Python Basics", "Guido", "222-222", BookStatus.AVAILABLE));

        var result = bookRepository.searchBooks("java", org.springframework.data.domain.PageRequest.of(0, 10));
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Java Programming");
    }

    // =================== BOOK INVENTORY TESTS ===================

    @Test
    @DisplayName("Should save and retrieve a BookInventory copy")
    void testSaveAndRetrieveInventory() {
        // Arrange: need a Book first
        Book book = new Book("Effective Java", "Joshua Bloch", "978-0134685991", BookStatus.AVAILABLE);
        book = bookRepository.save(book);

        // Act: create a physical copy
        BookInventory copy = new BookInventory(book, "ACC-001", "Shelf B2");
        BookInventory saved = inventoryRepository.save(copy);

        // Assert
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getAccessionNumber()).isEqualTo("ACC-001");
        assertThat(saved.getStatus()).isEqualTo(BookStatus.AVAILABLE); // default
        assertThat(saved.getBook().getId()).isEqualTo(book.getId());
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should find inventory copies by book ID")
    void testFindInventoryByBookId() {
        Book book = new Book("Head First Java", "Kathy Sierra", "978-0596009205", BookStatus.AVAILABLE);
        book = bookRepository.save(book);

        inventoryRepository.save(new BookInventory(book, "ACC-100", "Shelf C1"));
        inventoryRepository.save(new BookInventory(book, "ACC-101", "Shelf C1"));

        var copies = inventoryRepository.findByBookId(book.getId());
        assertThat(copies).hasSize(2);
    }

    @Test
    @DisplayName("Should count available copies for a book")
    void testCountAvailableCopies() {
        Book book = new Book("Spring Boot in Action", "Craig Walls", "978-1617292545", BookStatus.AVAILABLE);
        book = bookRepository.save(book);

        BookInventory copy1 = inventoryRepository.save(new BookInventory(book, "ACC-200", "Shelf D1"));
        BookInventory copy2 = inventoryRepository.save(new BookInventory(book, "ACC-201", "Shelf D1"));

        // Mark one as unavailable
        copy2.setStatus(BookStatus.UNAVAILABLE);
        inventoryRepository.save(copy2);

        long available = inventoryRepository.countByBookIdAndStatus(book.getId(), BookStatus.AVAILABLE);
        assertThat(available).isEqualTo(1);
    }

    @Test
    @DisplayName("Should detect duplicate accession number")
    void testExistsByAccessionNumber() {
        Book book = new Book("SICP", "Abelson", "978-0262510875", BookStatus.AVAILABLE);
        book = bookRepository.save(book);

        inventoryRepository.save(new BookInventory(book, "ACC-999", "Shelf Z1"));

        assertThat(inventoryRepository.existsByAccessionNumber("ACC-999")).isTrue();
        assertThat(inventoryRepository.existsByAccessionNumber("ACC-998")).isFalse();
    }

    // =================== LOAN TESTS ===================

    @Test
    @DisplayName("Should save a Loan with BORROWED status and zero fine by default")
    void testSaveLoan() {
        // Arrange
        User member = userRepository.save(new User("Dave Brown", "dave@example.com", "hash", Role.MEMBER));
        Book book = bookRepository.save(new Book("Refactoring", "Martin Fowler", "978-0201485677", BookStatus.AVAILABLE));
        BookInventory copy = inventoryRepository.save(new BookInventory(book, "ACC-300", "Shelf E1"));

        LocalDate today = LocalDate.now();
        LocalDate dueDate = today.plusDays(14);

        // Act
        Loan loan = new Loan(member, copy, today, dueDate);
        Loan saved = loanRepository.save(loan);

        // Assert
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(LoanStatus.BORROWED); // default
        assertThat(saved.getFineAmount()).isEqualByComparingTo(BigDecimal.ZERO); // default
        assertThat(saved.getReturnDate()).isNull(); // not yet returned
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should find active loans for a member")
    void testFindActiveByMemberId() {
        User member = userRepository.save(new User("Eve Green", "eve@example.com", "hash", Role.MEMBER));
        Book book = bookRepository.save(new Book("Domain Driven Design", "Eric Evans", "978-0321125217", BookStatus.AVAILABLE));
        BookInventory copy = inventoryRepository.save(new BookInventory(book, "ACC-400", "Shelf F1"));

        LocalDate today = LocalDate.now();
        loanRepository.save(new Loan(member, copy, today, today.plusDays(14)));

        var activeLoans = loanRepository.findActiveByMemberId(member.getId());
        assertThat(activeLoans).hasSize(1);
        assertThat(activeLoans.get(0).getStatus()).isEqualTo(LoanStatus.BORROWED);
    }

    @Test
    @DisplayName("Should count active loans for borrow limit enforcement")
    void testCountActiveByMemberId() {
        User member = userRepository.save(new User("Frank Black", "frank@example.com", "hash", Role.MEMBER));
        Book book = bookRepository.save(new Book("The Art of Computer Programming", "Knuth", "978-0201896831", BookStatus.AVAILABLE));

        BookInventory copy1 = inventoryRepository.save(new BookInventory(book, "ACC-500", "Shelf G1"));
        BookInventory copy2 = inventoryRepository.save(new BookInventory(book, "ACC-501", "Shelf G2"));

        LocalDate today = LocalDate.now();
        loanRepository.save(new Loan(member, copy1, today, today.plusDays(14)));
        loanRepository.save(new Loan(member, copy2, today, today.plusDays(14)));

        long count = loanRepository.countActiveByMemberId(member.getId());
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("Should find active loan for a specific inventory copy")
    void testFindActiveLoanByInventoryId() {
        User member = userRepository.save(new User("Grace Hall", "grace@example.com", "hash", Role.MEMBER));
        Book book = bookRepository.save(new Book("Algorithms", "Sedgewick", "978-0321573513", BookStatus.AVAILABLE));
        BookInventory copy = inventoryRepository.save(new BookInventory(book, "ACC-600", "Shelf H1"));

        LocalDate today = LocalDate.now();
        loanRepository.save(new Loan(member, copy, today, today.plusDays(14)));

        var activeLoan = loanRepository.findActiveLoanByInventoryId(copy.getId());
        assertThat(activeLoan).isPresent();
        assertThat(activeLoan.get().getMember().getId()).isEqualTo(member.getId());
    }

    @Test
    @DisplayName("Should NOT find active loan for an inventory copy after return")
    void testNoActiveLoanAfterReturn() {
        User member = userRepository.save(new User("Henry Liu", "henry@example.com", "hash", Role.MEMBER));
        Book book = bookRepository.save(new Book("Operating Systems", "Tanenbaum", "978-0137026098", BookStatus.AVAILABLE));
        BookInventory copy = inventoryRepository.save(new BookInventory(book, "ACC-700", "Shelf I1"));

        LocalDate today = LocalDate.now();
        Loan loan = loanRepository.save(new Loan(member, copy, today, today.plusDays(14)));

        // Simulate return
        loan.setStatus(LoanStatus.RETURNED);
        loan.setReturnDate(today);
        loanRepository.save(loan);

        var activeLoan = loanRepository.findActiveLoanByInventoryId(copy.getId());
        assertThat(activeLoan).isEmpty(); // No active loan after return
    }

    @Test
    @DisplayName("Should find loan by ID and member ID (authorization check)")
    void testFindByIdAndMemberId() {
        User member = userRepository.save(new User("Ivy Chen", "ivy@example.com", "hash", Role.MEMBER));
        User otherMember = userRepository.save(new User("Jack Kim", "jack@example.com", "hash", Role.MEMBER));
        Book book = bookRepository.save(new Book("Computer Networks", "Tanenbaum", "978-0132126953", BookStatus.AVAILABLE));
        BookInventory copy = inventoryRepository.save(new BookInventory(book, "ACC-800", "Shelf J1"));

        Loan loan = loanRepository.save(new Loan(member, copy, LocalDate.now(), LocalDate.now().plusDays(14)));

        // Member can find their own loan
        assertThat(loanRepository.findByIdAndMemberId(loan.getId(), member.getId())).isPresent();

        // Other member cannot find this loan (prevents unauthorized access)
        assertThat(loanRepository.findByIdAndMemberId(loan.getId(), otherMember.getId())).isEmpty();
    }
}
