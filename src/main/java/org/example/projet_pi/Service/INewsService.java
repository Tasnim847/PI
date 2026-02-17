package org.example.projet_pi.Service;

import org.example.projet_pi.entity.News;

import java.util.List;

public interface INewsService {
    //Iservice

    News addNews(News news);

    News updateNews(News news);

    void deleteNews(Long id);

    News getNewsById(Long id);

    List<News> getAllNews();
}
