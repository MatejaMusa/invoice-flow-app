package com.matejamusa.InvoiceFlow.controller;

import com.matejamusa.InvoiceFlow.dto.UserDTO;
import com.matejamusa.InvoiceFlow.model.Customer;
import com.matejamusa.InvoiceFlow.model.HttpResponse;
import com.matejamusa.InvoiceFlow.model.Invoice;
import com.matejamusa.InvoiceFlow.report.CustomerReport;
import com.matejamusa.InvoiceFlow.service.CustomerService;
import com.matejamusa.InvoiceFlow.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.time.LocalDateTime.now;

@RestController
@RequestMapping("/customer")
@RequiredArgsConstructor
public class CustomerController {
    private final CustomerService customerService;
    private final UserService userService;

    @GetMapping("/list")
    public ResponseEntity<HttpResponse> getCustomers(@AuthenticationPrincipal UserDTO user, @RequestParam Optional<Integer> page, @RequestParam Optional<Integer> size) {
        return ResponseEntity.ok(
                HttpResponse.builder()
                        .timeStamp(now().toString())
                        .data(Map.of("user", userService.getUserByEmail(user.getEmail()),
                                "page", customerService.getCustomers(page.orElse(0), size.orElse(10)),
                                "stats", customerService.getStats()))
                        .message("Customers retrieved")
                        .status(HttpStatus.OK)
                        .statusCode(HttpStatus.OK.value())
                        .build());
    }

    @PostMapping("/create")
    public ResponseEntity<HttpResponse> createCustomer(@AuthenticationPrincipal UserDTO user, @RequestBody Customer customer) {
        return ResponseEntity.created(URI.create("")).body(
                HttpResponse.builder()
                        .timeStamp(now().toString())
                        .data(Map.of("user", userService.getUserByEmail(user.getEmail()),
                                "customer", customerService.createCustomer(customer)))
                        .message("Customer created")
                        .status(HttpStatus.CREATED)
                        .statusCode(HttpStatus.CREATED.value())
                        .build());
    }

    @GetMapping("/get/{id}")
    public ResponseEntity<HttpResponse> getCustomer(@AuthenticationPrincipal UserDTO user, @PathVariable("id") Long id) {
        return ResponseEntity.ok(
                HttpResponse.builder()
                        .timeStamp(now().toString())
                        .data(Map.of("user", userService.getUserByEmail(user.getEmail()),
                                "customer", customerService.getCustomer(id)))
                        .message("Customer retrieved")
                        .status(HttpStatus.OK)
                        .statusCode(HttpStatus.OK.value())
                        .build());
    }

    @GetMapping("/search")
    public ResponseEntity<HttpResponse> searchCustomer(@AuthenticationPrincipal UserDTO user, Optional<String> name, @RequestParam Optional<Integer> page, @RequestParam Optional<Integer> size) {
        return ResponseEntity.ok(
                HttpResponse.builder()
                        .timeStamp(now().toString())
                        .data(Map.of("user", userService.getUserByEmail(user.getEmail()),
                                "page", customerService.searchCustomer(name.orElse(""), page.orElse(0), size.orElse(10))))
                        .message("Customers retrieved")
                        .status(HttpStatus.OK)
                        .statusCode(HttpStatus.OK.value())
                        .build());
    }

    @PutMapping("/update")
    public ResponseEntity<HttpResponse> updateCustomer(@AuthenticationPrincipal UserDTO user, @RequestBody Customer customer) {
        return ResponseEntity.ok(
                HttpResponse.builder()
                        .timeStamp(now().toString())
                        .data(Map.of("user", userService.getUserByEmail(user.getEmail()),
                                "customer", customerService.updateCustomer(customer)))
                        .message("Customer updated")
                        .status(HttpStatus.OK)
                        .statusCode(HttpStatus.OK.value())
                        .build());
    }

    @PostMapping("/invoice/create")
    public ResponseEntity<HttpResponse> createInvoice(@AuthenticationPrincipal UserDTO user, @RequestBody Invoice invoice) {
        return ResponseEntity.created(URI.create("")).body(
                HttpResponse.builder()
                        .timeStamp(now().toString())
                        .data(Map.of("user", userService.getUserByEmail(user.getEmail()),
                                "invoice", customerService.createInvoice(invoice)))
                        .message("Invoice created")
                        .status(HttpStatus.CREATED)
                        .statusCode(HttpStatus.CREATED.value())
                        .build());
    }

    @GetMapping("/invoice/new")
    public ResponseEntity<HttpResponse> newInvoice(@AuthenticationPrincipal UserDTO user) {
        return ResponseEntity.ok(
                HttpResponse.builder()
                        .timeStamp(now().toString())
                        .data(Map.of("user", userService.getUserByEmail(user.getEmail()),
                                "customers", customerService.getCustomers()))
                        .message("Customers retrieved")
                        .status(HttpStatus.OK)
                        .statusCode(HttpStatus.OK.value())
                        .build());
    }

    @GetMapping("/invoice/list")
    public ResponseEntity<HttpResponse> getInvoices(@AuthenticationPrincipal UserDTO user, @RequestParam Optional<Integer> page, @RequestParam Optional<Integer> size) {
        return ResponseEntity.ok(
                HttpResponse.builder()
                        .timeStamp(now().toString())
                        .data(Map.of("user", userService.getUserByEmail(user.getEmail()),
                                "page", customerService.getInvoices(page.orElse(0), size.orElse(10))))
                        .message("invoices retrieved")
                        .status(HttpStatus.OK)
                        .statusCode(HttpStatus.OK.value())
                        .build());
    }

    @GetMapping("/invoice/get/{id}")
    public ResponseEntity<HttpResponse> getInvoice(@AuthenticationPrincipal UserDTO user, @PathVariable("id") Long id) {
        Invoice invoice = customerService.getInvoice(id);
        return ResponseEntity.ok(
                HttpResponse.builder()
                        .timeStamp(now().toString())
                        .data(Map.of("user", userService.getUserByEmail(user.getEmail()),
                                "invoice", invoice,
                                "customer", invoice.getCustomer()))
                        .message("Invoice retrieved")
                        .status(HttpStatus.OK)
                        .statusCode(HttpStatus.OK.value())
                        .build());
    }

    @PostMapping("/invoice/addtocustomer/{id}")
    public ResponseEntity<HttpResponse> addInvoiceToCustomer(@AuthenticationPrincipal UserDTO user, @PathVariable("id") Long id, @RequestBody Invoice invoice) {
        customerService.addInvoiceToCustomer(id, invoice);
        return ResponseEntity.ok(
                HttpResponse.builder()
                        .timeStamp(now().toString())
                        .data(Map.of("user", userService.getUserByEmail(user.getEmail()),
                                "customers", customerService.getCustomers()))
                        .message(String.format("Invoice added to customer with ID: %s", id))
                        .status(HttpStatus.OK)
                        .statusCode(HttpStatus.OK.value())
                        .build());
    }

    @GetMapping("/download/report")
    public ResponseEntity<Resource> downloadReport() {
        List<Customer> customers = new ArrayList<>();
        customerService.getCustomers().iterator().forEachRemaining(customers::add);
        CustomerReport report = new CustomerReport(customers);
        HttpHeaders headers = new HttpHeaders();
        headers.add("File-Name", "customer-report.xlsx");
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment;File-Name=customer-report.xlsx");

        return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
                .headers(headers)
                .body(report.export());
    }
}
