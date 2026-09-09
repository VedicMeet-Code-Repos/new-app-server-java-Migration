package com.vedicmeet.appserver.admin;

import com.vedicmeet.appserver.migration.MigrationWrite;
import com.vedicmeet.appserver.security.RequireRole;
import com.vedicmeet.appserver.security.Role;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static com.vedicmeet.appserver.admin.AdminResponses.execute;

@RestController
@RequestMapping("/v2/admin/meditation")
@RequireRole({Role.ADMIN, Role.SUB_ADMIN})
public class AdminMeditationController {

    private final AdminMeditationService service;

    public AdminMeditationController(AdminMeditationService service) {
        this.service = service;
    }

    @PostMapping("/category_add")
    @MigrationWrite
    public ResponseEntity<Map<String, Object>> addCategory(@RequestBody(required = false) Map<String, Object> body) {
        return execute("Meditation category added successfully", () -> service.addMeditationCategory(body));
    }

    @PutMapping("/category_update")
    @MigrationWrite
    public ResponseEntity<Map<String, Object>> updateCategory(@RequestBody(required = false) Map<String, Object> body) {
        return execute("Meditation category updated successfully", () -> service.updateMeditationCategory(body));
    }

    @PutMapping("/category_block_unblock")
    @MigrationWrite
    public ResponseEntity<Map<String, Object>> blockUnblockCategory(@RequestBody(required = false) Map<String, Object> body) {
        return execute("Meditation category updated successfully", () -> service.blockUnblockMeditationCategory(body));
    }

    @GetMapping("/category_list")
    public ResponseEntity<Map<String, Object>> categoryList(@RequestParam Map<String, String> query) {
        return execute("Meditation category list fetched successfully", () -> service.listMedidationCategory(query));
    }

    @GetMapping("/category_details")
    public ResponseEntity<Map<String, Object>> categoryDetails(@RequestParam Map<String, String> query) {
        return execute("Meditation category details fetched successfully", () -> service.getMeditationCategory(query));
    }

    @PostMapping("/media_add")
    @MigrationWrite
    public ResponseEntity<Map<String, Object>> addMedia(@RequestBody(required = false) Map<String, Object> body) {
        return execute("Meditation media added successfully", () -> service.addMeditationMedia(body));
    }

    @PutMapping("/media_update")
    @MigrationWrite
    public ResponseEntity<Map<String, Object>> updateMedia(@RequestBody(required = false) Map<String, Object> body) {
        return execute("Meditation media updated successfully", () -> service.editMeditationMedia(body));
    }

    @PutMapping("/media_block_unblock")
    @MigrationWrite
    public ResponseEntity<Map<String, Object>> blockUnblockMedia(@RequestBody(required = false) Map<String, Object> body) {
        return execute("Meditation media updated successfully", () -> service.blockMeditationMedia(body));
    }

    @GetMapping("/media_details")
    public ResponseEntity<Map<String, Object>> mediaDetails(@RequestParam Map<String, String> query) {
        return execute("Meditation media details fetched successfully", () -> service.getMeditationMedia(query));
    }

    @GetMapping("/media_list")
    public ResponseEntity<Map<String, Object>> mediaList(@RequestParam Map<String, String> query) {
        return execute("Meditation media list fetched successfully", () -> service.listMeditationMedia(query));
    }

    @PostMapping("/user_meditation_history")
    @MigrationWrite
    public ResponseEntity<Map<String, Object>> userMeditationHistory(@RequestBody(required = false) Map<String, Object> body) {
        return execute("User meditation history added successfully", () -> service.userListMeditationHistory(body));
    }

    @GetMapping("/meditation_insights")
    public ResponseEntity<Map<String, Object>> meditationInsights(@RequestParam Map<String, String> query) {
        return execute("Meditation insights fetched successfully", () -> service.getMeditationInsights(query));
    }
}