package com.example.JustBuyIt.Controllers;

import com.example.JustBuyIt.DTOs.ProductDTO;
import com.example.JustBuyIt.DTOs.ProductPageDTO;
import com.example.JustBuyIt.Models.Product;
import com.example.JustBuyIt.Services.ProductService;
import com.example.JustBuyIt.Services.SecurityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
public class ProductController
{
    @Autowired
    ProductService productService;

    @Autowired
    SecurityService securityService;

    private static final Set<String> ALLOWED_SORTS = Set.of("id", "name", "price", "quantity");

    @RequestMapping("/")
    public ModelAndView Home() {
        String role = securityService.getPresentAuthorizedRole();
        ModelAndView mv = new ModelAndView();

        if (role.equals("ADMIN") || role.equals("SECONDARY_ADMIN")) {
            mv.setViewName("Admin_Pages/AdminDashboad.html");
        }else {
            mv.setViewName("HomePage.html");
        }
        return mv;
    }

    @GetMapping("/products")
    public ResponseEntity<ProductPageDTO>  getProducts(
           @RequestParam(defaultValue = "0") int page,
           @RequestParam(defaultValue = "15") int size,
           @RequestParam(defaultValue = "id") String sortby,
           @RequestParam(defaultValue = "asc") String direction,
           @RequestParam(required = false) String category
    ){
        if (!ALLOWED_SORTS.contains(sortby)) sortby = "id";
        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortby).descending()
                : Sort.by(sortby).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return ResponseEntity.ok(productService.getProducts(category, pageable));
    }

    @GetMapping("/products/{ProdId}")
    public Product getProductById(@PathVariable int ProdId){
        return productService.getProductById(ProdId);
    }

    @PostMapping("/products/upload")
    public ResponseEntity<?> AddProduct(@RequestPart("product") ProductDTO productDTO,
                                        @RequestPart("imagefile") MultipartFile imagefile) throws IOException {
        return ResponseEntity.ok(productService.addProductWithFile(productDTO, imagefile));
    }

    @PostMapping("/products")
    public ResponseEntity<?> AddProduct(@RequestBody ProductDTO productDTO) throws IOException {
        return ResponseEntity.ok(productService.AddProduct(productDTO));
    }

    @PutMapping("/products/{id}/upload")
    public ResponseEntity<?> updateProduct(
            @PathVariable int id,
            @RequestPart("product") ProductDTO productDTO,
            @RequestPart(value = "imagefile", required = false) MultipartFile imagefile) throws IOException {
        return ResponseEntity.ok(productService.UpdateProductWithFile(id, productDTO, imagefile));
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<?> updateProduct(
            @PathVariable int id,
            @RequestBody ProductDTO productDTO) throws IOException {
        return ResponseEntity.ok(productService.UpdateProduct(id, productDTO));
    }

    @PostMapping("/multi_products")
    public ResponseEntity<?> AddMultipleProducts(@RequestBody List<ProductDTO> productDTO){
        return ResponseEntity.ok().body(productService.addProductsBatchWithUrls(productDTO));
    }

    @DeleteMapping("/products/{ProdId}")
    public ResponseEntity<?> deleteProductById(@PathVariable int ProdId) {
        productService.deleteProductById(ProdId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/products/{ProdId}/image")
    public ResponseEntity<byte[]> getProductImageById(@PathVariable int ProdId){
        Product p = productService.getProductById(ProdId);
        return  ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(p.getImagefile());
    }

    @GetMapping("/products/search")
    public ResponseEntity<Map<String, Object>> searchProduct(
            @RequestParam("keyword") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size,
            @RequestParam(defaultValue = "id") String sortby,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(required = false) String category){

        if (!ALLOWED_SORTS.contains(sortby)) sortby = "id";
        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortby).descending()
                : Sort.by(sortby).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Product> productPage = productService.searchProduct(keyword, category, pageable);
        return ResponseEntity.ok(buildPageResponse(productPage));
    }

    @GetMapping("/products/categories")
    public List<String> getCategories() {
        return productService.getAllCategoryNames();   // implement: categoryRepo.findAll().stream().map(Category::getName).toList()
    }

    private Map<String, Object> buildPageResponse(Page<Product> page) {
        Map<String, Object> response = new HashMap<>();
        response.put("content", page.getContent());
        response.put("page", page.getNumber());
        response.put("size", page.getSize());
        response.put("totalElements", page.getTotalElements());
        response.put("totalPages", page.getTotalPages());
        response.put("first", page.isFirst());
        response.put("last", page.isLast());
        return response;
    }
}
