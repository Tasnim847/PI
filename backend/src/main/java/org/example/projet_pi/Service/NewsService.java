package org.example.projet_pi.Service;

import org.example.projet_pi.Repository.NewsRepository;
import org.example.projet_pi.entity.Admin;
import org.example.projet_pi.entity.News;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NewsService implements INewsService {

    private final NewsRepository newsRepository;

    public NewsService(NewsRepository newsRepository) {
        this.newsRepository = newsRepository;
    }

    @Override
    public News addNews(News news) {
        // Récupérer l'admin connecté
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        Admin admin = newsRepository.findAdminByEmail(email);
        if (admin == null) {
            throw new RuntimeException("Admin connecté non trouvé");
        }

        news.setAdmin(admin);
        news.setPublishDate(new java.util.Date()); // date automatique
        return newsRepository.save(news);
    }

    @Override
    public News updateNews(News news) {
        return newsRepository.save(news);
    }

    @Override
    public void deleteNews(Long id) {
        newsRepository.deleteById(id);
    }

    @Override
    public News getNewsById(Long id) {
        return newsRepository.findById(id).orElse(null);
    }

    @Override
    public List<News> getAllNews() {
        return newsRepository.findAll();
    }
}