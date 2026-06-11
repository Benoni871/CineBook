package com.cinebook.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Create/update payload for a movie. Field constraints mirror the {@code movies}
 * table column limits; {@code genre} and {@code languages} are stored as CSV
 * (e.g. "Action,Thriller", "Telugu,Hindi,English").
 */
public class MovieRequest {

    @NotBlank
    @Size(max = 150)
    private String title;

    @NotBlank
    @Size(max = 50)
    private String genre;

    @NotNull
    @Positive
    private Integer durationMins;

    @NotBlank
    @Size(max = 250)
    private String languages;

    @NotBlank
    @Size(max = 500)
    private String posterUrl;

    @NotBlank
    @Size(max = 500)
    private String trailerUrl;

    @NotNull
    @PositiveOrZero
    private BigDecimal price;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }

    public Integer getDurationMins() { return durationMins; }
    public void setDurationMins(Integer durationMins) { this.durationMins = durationMins; }

    public String getLanguages() { return languages; }
    public void setLanguages(String languages) { this.languages = languages; }

    public String getPosterUrl() { return posterUrl; }
    public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }

    public String getTrailerUrl() { return trailerUrl; }
    public void setTrailerUrl(String trailerUrl) { this.trailerUrl = trailerUrl; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
}
