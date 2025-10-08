package com.product_inventory_service.product_inventory_service.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class JwtService {

    // Lấy chuỗi secret key từ file application.properties
    @Value("${jwt.secret}")
    private String SECRET_KEY;

    //================================================================
    // CÁC HÀM ĐỂ ĐỌC THÔNG TIN TỪ TOKEN
    //================================================================

    /**
     * Trích xuất email (username) từ token.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Trích xuất một thông tin (claim) cụ thể từ token.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    //================================================================
    // CÁC HÀM ĐỂ TẠO TOKEN
    //================================================================

    /**
     * Hàm chính để tạo token cho một user.
     */
    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    /**
     * Hàm tạo token với các thông tin thêm (extra claims).
     */
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername()) // Subject là email/username
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10)) // Token hết hạn sau 10 tiếng
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    //================================================================
    // CÁC HÀM ĐỂ XÁC THỰC TOKEN
    //================================================================

    /**
     * Kiểm tra xem token có hợp lệ không (đúng người dùng và chưa hết hạn).
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    /**
     * Kiểm tra xem token đã hết hạn chưa.
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Lấy ngày hết hạn từ token.
     */
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }


    //================================================================
    // CÁC HÀM PRIVATE HELPER
    //================================================================

    /**
     * Giải mã toàn bộ thông tin trong token.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Tạo hoặc lấy ra signing key từ chuỗi SECRET_KEY.
     */
    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public List<SimpleGrantedAuthority> extractAuthorities(String token) {
        Claims claims = extractAllClaims(token);
        List<String> roles = claims.get("authorities", List.class);
        return roles.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }
}