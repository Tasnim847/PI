package org.example.projet_pi.Service;

import org.example.projet_pi.Repository.NewsRepository;
import org.example.projet_pi.entity.News;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NewsService implements INewsService {

    //service

    private final NewsRepository newsRepository;

    public NewsService(NewsRepository newsRepository) {
        this.newsRepository = newsRepository;
    }

    @Override
    public News addNews(News news) {
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
        return newsRepository.findById(id).orElse(null); // ou throw exception si tu préfères
    }

    @Override
    public List<News> getAllNews() {
        return newsRepository.findAll();
    }
}
