package com.example.JustBuyIt.Controllers;

import com.example.JustBuyIt.DTOs.ProductDTO;
import com.example.JustBuyIt.Models.Product;
import com.example.JustBuyIt.Services.ProductService;
import com.example.JustBuyIt.Services.SecurityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;

@RestController
public class ProductController
{
    @Autowired
    ProductService productService;

    @Autowired
    SecurityService securityService;

    @RequestMapping("/")
    public ModelAndView Home() {
        String role = securityService.getPresentAuthorizedRole();
        ModelAndView mv = new ModelAndView();

        if (role.equals("ADMIN")) {
            mv.setViewName("AdminDashboad.html");
        }else if (role.equals("USER")) {
            mv.setViewName("HomePage.html");
        }
        return mv;
    }

    @GetMapping("/products")
    public List<Product> getProducts(){
       return productService.getProducts();
    }

    @GetMapping("/products/{ProdId}")
    public Product getProductById(@PathVariable int ProdId){
        return productService.getProductById(ProdId);
    }

    @PostMapping("/products/upload")
    public ResponseEntity<?> AddProduct(@RequestPart("product") ProductDTO productDTO,
                                        @RequestPart("imagefile") MultipartFile imagefile){
//        System.out.println("New Product : "+productDTO);
        Product saved;
        try {
            saved = productService.addProductWithFile(productDTO, imagefile);
            return ResponseEntity.ok(saved);
        }catch (Exception e){
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @PostMapping("/products")
    public ResponseEntity<?> AddProduct(@RequestBody ProductDTO productDTO){
//        System.out.println("New Product : "+prod);
        Product saved;
        try {
            saved = productService.AddProduct(productDTO);
            return ResponseEntity.ok(saved);
        }catch (Exception e){
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @PostMapping("/multi_products")
    public ResponseEntity<?> AddMultipleProducts(@RequestBody List<ProductDTO> productDTO){
        return ResponseEntity.ok().body(productService.addProductsBatchWithUrls(productDTO));
    }

    @PutMapping("/products/{id}/upload")
    public ResponseEntity<?> updateProduct(
            @PathVariable int id,
            @RequestPart("product") ProductDTO productDTO ,
            @RequestPart(value = "imagefile", required = false) MultipartFile imagefile) {

        try {
            Product updated = productService.UpdateProductWithFile(id,productDTO,imagefile);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<?> updateProduct(
            @PathVariable int id,
            @RequestBody ProductDTO productDTO) {

        try {
            Product updated = productService.UpdateProduct(id,productDTO);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @DeleteMapping("/products/{ProdId}")
    public void deleteProductById(@PathVariable int ProdId){
        productService.deleteProductById(ProdId);
    }

    @GetMapping("/products/{ProdId}/image")
    public ResponseEntity<byte[]> getProductImageById(@PathVariable int ProdId){
        Product p = productService.getProductById(ProdId);
        return  ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(p.getImagefile());
    }

    @GetMapping("/products/search")
    public ResponseEntity<List<Product>> searchProduct(@RequestParam("keyword") String keyword){
        System.out.println("searching : "+keyword);
        return ResponseEntity.ok(productService.searchProduct(keyword));
    }
}
