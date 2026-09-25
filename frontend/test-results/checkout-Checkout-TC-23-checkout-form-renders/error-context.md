# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: checkout.spec.js >> Checkout >> TC-23 checkout form renders
- Location: tests\checkout.spec.js:7:3

# Error details

```
Test timeout of 30000ms exceeded while running "beforeEach" hook.
```

```
Tearing down "context" exceeded the test timeout of 30000ms.
```

# Page snapshot

```yaml
- main [ref=e3]:
  - generic [ref=e4]:
    - generic [ref=e5]:
      - generic [ref=e7]:
        - generic [ref=e8]:
          - generic [ref=e9]: ◇
          - text: FASHION
        - generic [ref=e10]: Members club
      - generic [ref=e11]:
        - paragraph [ref=e12]:
          - text: Phong cách là ngôn ngữ
          - emphasis [ref=e13]: bạn tự chọn cho mình.
        - generic [ref=e14]: Khám phá bộ sưu tập tinh tế, thời thượng và được tuyển chọn cho những người yêu thời trang.
        - generic [ref=e15]:
          - generic [ref=e16]: •Miễn phí vận chuyển
          - generic [ref=e17]: •Đổi trả 30 ngày
          - generic [ref=e18]: •Hàng chính hãng
      - generic [ref=e19]:
        - generic [ref=e20]:
          - strong [ref=e21]: 24h
          - generic [ref=e22]: Giao hàng nhanh
        - generic [ref=e23]:
          - strong [ref=e24]: 4.9/5
          - generic [ref=e25]: Đánh giá khách hàng
    - generic [ref=e27]:
      - paragraph [ref=e28]: Chào mừng trở lại
      - heading [level=1] [ref=e29]:
        - text: Đăng nhập
        - emphasis [ref=e30]: tài khoản
      - paragraph [ref=e31]: Nơi phong cách riêng của bạn bắt đầu.
      - generic "Auth navigation" [ref=e32]:
        - button "Đăng nhập" [ref=e33] [cursor=pointer]
        - button "Đăng ký" [ref=e34] [cursor=pointer]
      - generic [ref=e35]:
        - generic [ref=e36]:
          - text: Email
          - textbox "Email" [ref=e37]:
            - /placeholder: tên@email.com
            - text: e2e-1790268807341-685@example.com
        - generic [ref=e38]:
          - text: Mật khẩu
          - generic [ref=e39]:
            - textbox "Mật khẩu Hiện mật khẩu" [ref=e40]:
              - /placeholder: ••••••••
              - text: E2eTest@12345
            - button "Hiện mật khẩu" [ref=e41] [cursor=pointer]: Hiện
        - generic [ref=e43]:
          - checkbox "Ghi nhớ đăng nhập" [ref=e44]
          - text: Ghi nhớ đăng nhập
        - button "Đang đăng nhập..." [disabled] [ref=e46]
        - generic [ref=e47]:
          - text: Chưa có tài khoản?
          - button "Đăng ký ngay →" [ref=e48] [cursor=pointer]
      - paragraph [ref=e49]: Bằng việc tiếp tục, bạn đồng ý với điều khoản sử dụng và chính sách bảo mật của FASHION.
```