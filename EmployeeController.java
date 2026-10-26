
package com.knowgrow.knowgrow.employee;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
@CrossOrigin
public class EmployeeController {

    private final EmployeeRepository repository;

    public EmployeeController(EmployeeRepository repository) {
        this.repository = repository;
    }

    // Get all employees
    @GetMapping
    public List<Employee> getAllEmployees() {
        return repository.findAll();
    }

    // Get employee by ID
    @GetMapping("/{id}")
    public Employee getEmployeeById(@PathVariable Long id) {
        return repository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Employee not found: " + id));
    }

    // Create employee
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Employee createEmployee(@Valid @RequestBody Employee employee) {
        employee.setId(null);
        return repository.save(employee);
    }

    // Update employee
    @PutMapping("/{id}")
    public Employee updateEmployee(
            @PathVariable Long id,
            @Valid @RequestBody Employee input) {

        Employee employee = repository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Employee not found: " + id));

        employee.setName(input.getName());
        employee.setEmail(input.getEmail());
        employee.setDepartment(input.getDepartment());
        employee.setRole(input.getRole());
        employee.setExperienceYears(input.getExperienceYears());

        return repository.save(employee);
    }

    // Delete employee
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEmployee(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("Employee not found: " + id);
        }

        repository.deleteById(id);
    }
}
