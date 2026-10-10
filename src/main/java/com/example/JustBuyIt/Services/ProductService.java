package com.example.JustBuyIt.Services;

import com.example.JustBuyIt.DTOs.ProductDTO;
import com.example.JustBuyIt.DTOs.ProductPageDTO;
import com.example.JustBuyIt.DTOs.UserPrincipalDto;
import com.example.JustBuyIt.Models.*;
import com.example.JustBuyIt.Repository.CategoryRepo;
import com.example.JustBuyIt.Repository.ProductRepo;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
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
    private final UsersService usersService;
    private final ImageService imageService;
    private final SecurityService securityService;
    private final StringRedisTemplate stringRedisTemplate;

    public ProductService(ProductRepo repo, CategoryRepo categoryRepo, UsersService usersService, ImageService imageService, SecurityService securityService, StringRedisTemplate stringRedisTemplate) {
        this.repo = repo;
        this.categoryRepo = categoryRepo;
        this.usersService = usersService;
        this.imageService = imageService;
        this.securityService = securityService;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    private static final String PRODUCT_VERSION_KEY = "products:version";

    private volatile String cachedVersion = "1";
    private volatile long versionFetchedAt = 0;
    private static final long VERSION_TTL_MS = 5_000;

    public String currentVersion() {
        long now = System.currentTimeMillis();
        if (now - versionFetchedAt > VERSION_TTL_MS) {
            String v = stringRedisTemplate.opsForValue().get(PRODUCT_VERSION_KEY);
            cachedVersion = (v != null) ? v : "1";
            versionFetchedAt = now;
        }
        return cachedVersion;
    }

    public void bumpVersion() {
        stringRedisTemplate.opsForValue().increment(PRODUCT_VERSION_KEY);
    }

    @Cacheable(value = "products",
            key = "'v' + @productService.currentVersion() "
                    + "+ ':page:' + #pageable.pageNumber "
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

    @Cacheable(value = "products", key = "'v' + @productService.currentVersion() + ':' + #prodId")    @Transactional(readOnly = true)
    public Product getProductById(int prodId) {
        return loadProduct(prodId);
    }

    private Product loadProduct(int prodId) {
        return repo.findById(prodId)
                .orElseThrow(() -> new UsernameNotFoundException("Product not found."));
    }

    @Transactional
    public Product addProductWithFile(ProductDTO dto, MultipartFile file) throws IOException {
        UserPrincipalDto actor = currentActor();
        securityService.assertCanCreateProduct(actor);

        Users owner = usersService.getReferenceById(actor.getId());
        Product product = mapDtoToProduct(dto,owner);

        if (file != null && !file.isEmpty()) {
            product.setImagefile(file.getBytes());
        }
        Product saved = repo.save(product);
        bumpVersion();
        return saved;
    }

    @Transactional
    public Product AddProduct(ProductDTO productDTO) throws IOException {
        UserPrincipalDto actor = currentActor();
        securityService.assertCanCreateProduct(actor);

        Users owner = usersService.getReferenceById(actor.getId());
        Product saved;
        try {
            Product product = mapDtoToProduct(productDTO,owner);
            if (productDTO.getImageUrl() != null && !productDTO.getImageUrl().isBlank()) {
                try {
                    product.setImagefile(imageService.downloadImageFromUrl(productDTO.getImageUrl()));
                }catch (Exception e){
                    System.out.println("Failed downloading image for " + productDTO.getName() + ": " + e.getMessage());
                    System.err.println("Failed downloading image for " + productDTO.getName() + ": " + e.getMessage());
                    product.setImagefile(null);
                }
            }
            saved = repo.save(product);
        }catch (Exception e){
            System.out.println(e.getMessage());
            saved = repo.save(mapDtoToProduct(productDTO,owner));
        }
        bumpVersion();
        return saved;
    }

    @Transactional
    public Product  UpdateProductWithFile(int prodId, ProductDTO dto, MultipartFile imagefile) throws IOException {
        Product existingProduct = loadProduct(prodId);

        UserPrincipalDto actor = currentActor();
        securityService.assertCanModifyOrDeleteProduct(existingProduct, actor);

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

        Product saved = repo.save(existingProduct);
        bumpVersion();
        return saved;
    }

    @Transactional
    public Product  UpdateProduct(int prodId, ProductDTO dto) throws IOException {
        Product existingProduct = loadProduct(prodId);

        UserPrincipalDto actor = currentActor();
        securityService.assertCanModifyOrDeleteProduct(existingProduct, actor);

        existingProduct.setName(dto.getName());
        existingProduct.setPrice(dto.getPrice());
        existingProduct.setDescription(dto.getDescription());
        existingProduct.setQuantity(dto.getQuantity());
        existingProduct.setAddedBy(usersService.getUser(dto.getAddedById()));

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

        Product saved = repo.save(existingProduct);
        bumpVersion();
        return saved;
    }

    @Transactional
    public void deleteProductById(int prodId) {
        Product existing = loadProduct(prodId);

        UserPrincipalDto actor = currentActor();
        securityService.assertCanModifyOrDeleteProduct(existing, actor);

        repo.deleteById(prodId);
        bumpVersion();
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
    public List<Product> addProductsBatchWithUrls(List<ProductDTO> productDTOs) {
        UserPrincipalDto actor = currentActor();

        if (productDTOs == null || productDTOs.isEmpty()) {
            throw new IllegalStateException("No products supplied for batch import.");
        }

        // Single shared guard: ADMIN unlimited, SECONDARY_ADMIN capped at 3 total.
        securityService.assertCanBatchCreateProducts(actor, productDTOs.size());

        Users owner = usersService.getReferenceById(actor.getId());
        List<Product> productsToSave = new ArrayList<>();

        for (ProductDTO dto : productDTOs) {
            Product product = mapDtoToProduct(dto, owner);
            if (dto.getImageUrl() != null && !dto.getImageUrl().isBlank()) {
                try {
                    product.setImagefile(imageService.downloadImageFromUrl(dto.getImageUrl()));
                } catch (Exception e) {
                    System.out.println("Failed downloading image for " + dto.getName() + ": " + e.getMessage());
                    System.err.println("Failed downloading image for " + dto.getName() + ": " + e.getMessage());
                    product.setImagefile(null);
                }
            }
            productsToSave.add(product);
        }

        List<Product> savedProducts = repo.saveAll(productsToSave);
        bumpVersion();
        return savedProducts;
    }

    private Product mapDtoToProduct(ProductDTO dto, Users owner) {
        Product product = new Product();
        product.setName(dto.getName());
        product.setPrice(dto.getPrice());
        product.setDescription(dto.getDescription());
        product.setQuantity(dto.getQuantity());
        product.setAddedBy(owner);

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

    private UserPrincipalDto currentActor() {
        return securityService.getPresentAuthorizedUser();
    }
}
