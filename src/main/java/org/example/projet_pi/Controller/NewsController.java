package org.example.projet_pi.Controller;

import org.example.projet_pi.Service.INewsService;
import org.example.projet_pi.entity.News;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/news")
public class NewsController {

    @Autowired
    private INewsService newsService;

    //crude
    
    // Ajouter une news
    @PostMapping("/add")
    public News addNews(@RequestBody News news) {
        return newsService.addNews(news);
    }

    // Modifier une news
    @PutMapping("/update")
    public News updateNews(@RequestBody News news) {
        return newsService.updateNews(news);
    }

    // Supprimer une news
    @DeleteMapping("/delete/{id}")
    public void deleteNews(@PathVariable Long id) {
        newsService.deleteNews(id);
    }

    // Récupérer une news par ID
    @GetMapping("/{id}")
    public News getNewsById(@PathVariable Long id) {
        return newsService.getNewsById(id);
    }

    // Récupérer toutes les news
    @GetMapping("/all")
    public List<News> getAllNews() {
        return newsService.getAllNews();
    }
}
