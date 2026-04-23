package org.example.projet_pi.Controller;

import org.example.projet_pi.Service.INewsService;
import org.example.projet_pi.entity.News;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/news")

public class NewsController {

    private static final Logger logger = LoggerFactory.getLogger(NewsController.class);

    @Autowired
    private INewsService newsService;

    // ==================== CRUD ====================

    @PostMapping
    public ResponseEntity<News> createNews(@RequestBody News news) {
        logger.info("📝 POST /api/v1/news - Création d'une news");
        try {
            News created = newsService.addNews(news);
            return new ResponseEntity<>(created, HttpStatus.CREATED);
        } catch (Exception e) {
            logger.error("❌ Erreur lors de la création", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<News> updateNews(@PathVariable Long id, @RequestBody News news) {
        logger.info("✏️ PUT /api/v1/news/{} - Mise à jour", id);
        try {
            news.setNewsId(id);
            News updated = newsService.updateNews(news);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            logger.error("❌ News non trouvée: {}", id);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNews(@PathVariable Long id) {
        logger.info("🗑️ DELETE /api/v1/news/{} - Suppression", id);
        try {
            newsService.deleteNews(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            logger.error("❌ News non trouvée: {}", id);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<News> getNewsById(@PathVariable Long id) {
        logger.info("🔍 GET /api/v1/news/{} - Récupération", id);
        try {
            News news = newsService.getNewsById(id);
            return ResponseEntity.ok(news);
        } catch (RuntimeException e) {
            logger.error("❌ News non trouvée: {}", id);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping
    public ResponseEntity<List<News>> getAllNews() {
        logger.info("📋 GET /api/v1/news - Toutes les news");
        List<News> news = newsService.getAllNews();
        if (news.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return ResponseEntity.ok(news);
    }

    @GetMapping("/paged")
    public ResponseEntity<Page<News>> getPagedNews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "publishDate") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        logger.info("📄 GET /api/v1/news/paged - Page: {}, Taille: {}", page, size);

        Sort sort = direction.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() :
                Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<News> newsPage = newsService.getPagedNews(pageable);

        return ResponseEntity.ok(newsPage);
    }

    // ==================== RECHERCHES ====================

    @GetMapping("/search")
    public ResponseEntity<List<News>> searchNews(@RequestParam String keyword) {
        logger.info("🔎 GET /api/v1/news/search - Recherche: {}", keyword);
        List<News> results = newsService.searchNewsByTitle(keyword);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/latest")
    public ResponseEntity<List<News>> getLatestNews(@RequestParam(defaultValue = "5") int limit) {
        logger.info("🆕 GET /api/v1/news/latest - Dernières {} news", limit);
        List<News> news = newsService.getLatestNews(limit);
        return ResponseEntity.ok(news);
    }

    @GetMapping("/published")
    public ResponseEntity<List<News>> getPublishedNews() {
        logger.info("📢 GET /api/v1/news/published - News publiées");
        List<News> news = newsService.getPublishedNews();
        return ResponseEntity.ok(news);
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<News>> getNewsByCategory(@PathVariable String category) {
        logger.info("📂 GET /api/v1/news/category/{} - Filtrer par catégorie", category);
        List<News> news = newsService.getNewsByCategory(category);
        return ResponseEntity.ok(news);
    }

    @GetMapping("/author/{author}")
    public ResponseEntity<List<News>> getNewsByAuthor(@PathVariable String author) {
        logger.info("✍️ GET /api/v1/news/author/{} - Filtrer par auteur", author);
        List<News> news = newsService.getNewsByAuthor(author);
        return ResponseEntity.ok(news);
    }

    @GetMapping("/most-viewed")
    public ResponseEntity<List<News>> getMostViewedNews(@RequestParam(defaultValue = "5") int limit) {
        logger.info("👁️ GET /api/v1/news/most-viewed - {} news les plus vues", limit);
        List<News> news = newsService.getMostViewedNews(limit);
        return ResponseEntity.ok(news);
    }

    @GetMapping("/date-range")
    public ResponseEntity<List<News>> getNewsByDateRange(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date start,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date end) {

        logger.info("📅 GET /api/v1/news/date-range - Entre {} et {}", start, end);
        List<News> news = newsService.getNewsByDateRange(start, end);
        return ResponseEntity.ok(news);
    }

    // ==================== ACTIONS ====================

    @PatchMapping("/{id}/publish")
    public ResponseEntity<News> publishNews(@PathVariable Long id) {
        logger.info("🚀 PATCH /api/v1/news/{}/publish - Publication", id);
        try {
            News news = newsService.publishNews(id);
            return ResponseEntity.ok(news);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PatchMapping("/{id}/archive")
    public ResponseEntity<News> archiveNews(@PathVariable Long id) {
        logger.info("📦 PATCH /api/v1/news/{}/archive - Archivage", id);
        try {
            News news = newsService.archiveNews(id);
            return ResponseEntity.ok(news);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    // ==================== STATISTIQUES ====================

    @GetMapping("/stats/count")
    public ResponseEntity<Long> getTotalCount() {
        logger.info("📊 GET /api/v1/news/stats/count - Nombre total");
        long count = newsService.countAllNews();
        return ResponseEntity.ok(count);
    }

    @GetMapping("/stats/published")
    public ResponseEntity<Long> getPublishedCount() {
        logger.info("📊 GET /api/v1/news/stats/published - Nombre publié");
        long count = newsService.getPublishedNewsCount();
        return ResponseEntity.ok(count);
    }

    // ==================== UPLOAD IMAGES ====================

    @PostMapping(value = "/{id}/upload-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT_ASSURANCE')")
    public ResponseEntity<Map<String, String>> uploadImage(
            @PathVariable Long id,
            @RequestParam("image") MultipartFile image) {

        logger.info("🖼️ POST /api/v1/news/{}/upload-image - Upload image", id);

        if (image.isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Fichier vide");
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }

        try {
            String imageUrl = newsService.uploadImage(id, image);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Image uploadée avec succès");
            response.put("url", imageUrl);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Erreur lors de l'upload");
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/{id}/image")
    public ResponseEntity<Void> deleteImage(@PathVariable Long id) {
        logger.info("🗑️ DELETE /api/v1/news/{}/image - Suppression image", id);
        try {
            newsService.deleteImage(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}