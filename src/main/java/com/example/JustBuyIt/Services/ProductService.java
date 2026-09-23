package com.example.JustBuyIt.Services;

import com.example.JustBuyIt.DTOs.ProductDTO;
import com.example.JustBuyIt.DTOs.ProductPageDTO;
import com.example.JustBuyIt.Models.Category;
import com.example.JustBuyIt.Models.Product;
import com.example.JustBuyIt.Models.QuantityUnit;
import com.example.JustBuyIt.Repository.CategoryRepo;
import com.example.JustBuyIt.Repository.ProductRepo;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class ProductService {

    private final ProductRepo repo;
    private final CategoryRepo categoryRepo;
    private final ImageService imageService;

    public ProductService(ProductRepo repo, CategoryRepo categoryRepo, ImageService imageService) {
        this.repo = repo;
        this.categoryRepo = categoryRepo;
        this.imageService = imageService;
    }

    @Cacheable(value = "products",
            key = "'page:' + #pageable.pageNumber "
                    + "+ ':' + #pageable.pageSize "
                    + "+ ':' + #pageable.sort.toString() "
                    + "+ ':cat:' + (#category != null ? #category : 'all')")
    @Transactional(readOnly = true)
    public ProductPageDTO getProducts(String category, Pageable pageable) {
        if (category == null || category.isBlank() || category.equalsIgnoreCase("All")) {
            return toDTO(repo.findAll(pageable));
        }
        return toDTO(repo.findByCategory_NameIgnoreCase(category, pageable));
    }

    @Cacheable(value = "products",key = "#prodId")
    @Transactional(readOnly = true)
    public Product getProductById(int prodId) {
        return repo.findById(prodId).orElseThrow(() -> new UsernameNotFoundException("Product not found."));
    }

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public Product addProductWithFile(ProductDTO dto, MultipartFile file) throws IOException {
        Product product = mapDtoToProduct(dto);

        if (file != null && !file.isEmpty()) {
            product.setImagefile(file.getBytes());
        }

        return repo.save(product);
    }

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public Product AddProduct(ProductDTO productDTO) throws IOException {
        try {
            Product product = mapDtoToProduct(productDTO);
            if (productDTO.getImageUrl() != null && !productDTO.getImageUrl().isBlank()) {
                try {
                    product.setImagefile(imageService.downloadImageFromUrl(productDTO.getImageUrl()));
                }catch (Exception e){
                    System.out.println("Failed downloading image for " + productDTO.getName() + ": " + e.getMessage());
                    System.err.println("Failed downloading image for " + productDTO.getName() + ": " + e.getMessage());
                    product.setImagefile(null);
                }
            }
            return  repo.save(product);
        }catch (Exception e){
            System.out.println(e.getMessage());
        }
        return  repo.save(mapDtoToProduct(productDTO));
    }

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public Product  UpdateProductWithFile(int prodId, ProductDTO dto, MultipartFile imagefile) throws IOException {
        Product existingProduct = getProductById(prodId);
        existingProduct.setName(dto.getName());
        existingProduct.setPrice(dto.getPrice());
        existingProduct.setDescription(dto.getDescription());
        existingProduct.setQuantity(dto.getQuantity());

        if (dto.getQunatityUnit() != null) {
            try {
                existingProduct.setQuantityUnit(QuantityUnit.valueOf(dto.getQunatityUnit().toUpperCase().trim()));
            } catch (IllegalArgumentException e) {
                existingProduct.setQuantityUnit(null);
            }
        }

        if (dto.getCategoryName() != null && !dto.getCategoryName().isBlank()) {
            Category category = categoryRepo.findByName(dto.getCategoryName())
                    .orElseGet(() -> categoryRepo.save(new Category(dto.getCategoryName())));
            existingProduct.setCategory(category);
        }

        if (imagefile != null && !imagefile.isEmpty()) {
            existingProduct.setImagefile(imagefile.getBytes());
        } else {
            Product existing = repo.findById(existingProduct.getId()).orElse(null);
            if (existing != null) {
                existingProduct.setImagefile(existing.getImagefile());
            }
        }

        return repo.save(existingProduct);
    }

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public Product  UpdateProduct(int prodId, ProductDTO dto) throws IOException {
        Product existingProduct = getProductById(prodId);
        existingProduct.setName(dto.getName());
        existingProduct.setPrice(dto.getPrice());
        existingProduct.setDescription(dto.getDescription());
        existingProduct.setQuantity(dto.getQuantity());

        if (dto.getQunatityUnit() != null) {
            try {
                existingProduct.setQuantityUnit(QuantityUnit.valueOf(dto.getQunatityUnit().toUpperCase().trim()));
            } catch (IllegalArgumentException e) {
                existingProduct.setQuantityUnit(null);
            }
        }

        if (dto.getCategoryName() != null && !dto.getCategoryName().isBlank()) {
            Category category = categoryRepo.findByName(dto.getCategoryName())
                    .orElseGet(() -> categoryRepo.save(new Category(dto.getCategoryName())));
            existingProduct.setCategory(category);
        }

        if (dto.getImageUrl() != null && !dto.getImageUrl().isEmpty()) {
            existingProduct.setImagefile(imageService.downloadImageFromUrl(dto.getImageUrl()));
        } else {
            Product existing = repo.findById(existingProduct.getId()).orElse(null);
            if (existing != null) {
                existingProduct.setImagefile(existing.getImagefile());
            }
        }

        return repo.save(existingProduct);
    }

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public void deleteProductById(int prodId) {
        repo.deleteById(prodId);
    }

    @Transactional(readOnly = true)
    public Page<Product> searchProduct(String keyword, String category, Pageable pageable) {
        String cat = (category == null || category.isBlank() || category.equalsIgnoreCase("All"))
                ? null : category;
        return repo.searchByKeywordAndCategory(keyword,cat,pageable);
    }

    @Transactional(readOnly = true)
    public List<Product> searchProduct(String keyword) {
        return repo.searchProductByKeyword(keyword);
    }

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public List<Product> addProductsBatchWithUrls(List<ProductDTO> productDTOs) {
        List<Product> productsToSave = new ArrayList<>();

        for (ProductDTO dto : productDTOs) {
            Product product = mapDtoToProduct(dto);
            if (dto.getImageUrl() != null && !dto.getImageUrl().isBlank()) {
                try {
                    product.setImagefile(imageService.downloadImageFromUrl(dto.getImageUrl()));
                }catch (Exception e){
                    System.out.println("Failed downloading image for " + dto.getName() + ": " + e.getMessage());
                    System.err.println("Failed downloading image for " + dto.getName() + ": " + e.getMessage());
                    product.setImagefile(null);
                }
            }
            productsToSave.add(product);
        }
        return repo.saveAll(productsToSave);
    }

    private Product mapDtoToProduct(ProductDTO dto) {
        Product product = new Product();
        product.setName(dto.getName());
        product.setPrice(dto.getPrice());
        product.setDescription(dto.getDescription());
        product.setQuantity(dto.getQuantity());

        if (dto.getQunatityUnit() != null) {
            try {
                product.setQuantityUnit(QuantityUnit.valueOf(dto.getQunatityUnit().toUpperCase().trim()));
            } catch (IllegalArgumentException e) {
                product.setQuantityUnit(null);
            }
        }

        if (dto.getCategoryName() != null && !dto.getCategoryName().isBlank()) {
            Category category = categoryRepo.findByName(dto.getCategoryName())
                    .orElseGet(() -> categoryRepo.save(new Category(dto.getCategoryName())));
            product.setCategory(category);
        }
        return product;
    }

    private ProductPageDTO toDTO(Page<Product> page) {
        return new ProductPageDTO(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }

    public List<String> getAllCategoryNames() {
        return categoryRepo.findAllName();
    }
}
