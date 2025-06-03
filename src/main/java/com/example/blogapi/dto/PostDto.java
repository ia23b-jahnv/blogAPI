// 📁 src/main/java/com/training/blogapi/dto/PostDto.java
package com.example.blogapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object for Post entities.
 * Used for API requests and responses with proper validation.
 */
public class PostDto {

    private Long id;

    @NotBlank(message = "Titel ist erforderlich")
    @Size(max = 255, message = "Titel darf maximal 255 Zeichen haben")
    private String title;

    @NotBlank(message = "Inhalt ist erforderlich")
    @Size(min = 10, message = "Inhalt muss mindestens 10 Zeichen haben")
    @Size(max = 5000, message = "Inhalt darf maximal 5000 Zeichen haben")
    private String content;

    private String imagePath;

    // Default constructor
    public PostDto() {}

    // Constructor without image
    public PostDto(Long id, String title, String content) {
        this.id = id;
        this.title = title;
        this.content = content;
    }

    // Constructor with all fields
    public PostDto(Long id, String title, String content, String imagePath) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.imagePath = imagePath;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    // Optional: toString method for debugging
    @Override
    public String toString() {
        return "PostDto{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", content='" + content + '\'' +
                ", imagePath='" + imagePath + '\'' +
                '}';
    }

    // Optional: equals and hashCode methods
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PostDto postDto = (PostDto) o;
        return java.util.Objects.equals(id, postDto.id);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(id);
    }
}