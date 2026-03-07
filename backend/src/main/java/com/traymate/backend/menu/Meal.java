// 
package com.traymate.backend.menu;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "meals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Meal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Integer id;

    private String name;

    @Column(length = 1000)
    private String ingredients;

    @Column(length = 1000)
    private String description;

    @Column(name = "image_url")
    private String imageUrl;

    private String mealtype;
    private String mealperiod;

    @Column(name = "time_range")
    private String timeRange;

    @Column(name = "allergen_info", columnDefinition = "TEXT")
    private String allergenInfo;

    @Column(columnDefinition = "TEXT")
    private String tags;

    @Column(name = "isAvailable")
    private boolean isAvailable;

    @Column(name = "isSeasonal")
    private boolean isSeasonal;

    @Column(length = 1000)
    private String nutrition;
}