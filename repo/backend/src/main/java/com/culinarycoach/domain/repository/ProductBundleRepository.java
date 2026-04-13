package com.culinarycoach.domain.repository;

import com.culinarycoach.domain.entity.ProductBundle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductBundleRepository extends JpaRepository<ProductBundle, Long> {

    List<ProductBundle> findByActiveTrue();
}
