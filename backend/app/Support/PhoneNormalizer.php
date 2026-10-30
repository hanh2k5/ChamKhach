<?php

namespace App\Support;

class PhoneNormalizer
{
    /**
     * Chuẩn hóa số điện thoại theo quy tắc SRS mục 5.2.
     *
     * 1. Cắt khoảng trắng; loại bỏ ký tự không phải số (cho phép '+' ở đầu).
     * 2. Nếu bắt đầu bằng '+84' hoặc '84' (phần còn lại 9-10 số) -> thay bằng '0'.
     * 3. Kết quả phải khớp regex ^0\d{9,10}$.
     *
     * @param  string|null  $phone
     * @return string|null Trả về chuỗi số đã chuẩn hóa hoặc null nếu không hợp lệ.
     */
    public static function normalize(?string $phone): ?string
    {
        if ($phone === null) {
            return null;
        }

        $phone = trim($phone);
        if ($phone === '') {
            return null;
        }

        // Nếu có ký tự chữ cái -> không hợp lệ
        if (preg_match('/[a-zA-Z]/', $phone)) {
            return null;
        }

        // Dấu '+' chỉ được phép xuất hiện ở ký tự đầu tiên
        if (strpos(substr($phone, 1), '+') !== false) {
            return null;
        }

        $isPlus = str_starts_with($phone, '+');
        $digitsOnly = preg_replace('/\D/', '', $phone);

        if ($digitsOnly === null || $digitsOnly === '') {
            return null;
        }

        // Nếu bắt đầu bằng +84 hoặc 84 và phần còn lại 9-10 số -> thay bằng '0'
        if ($isPlus && str_starts_with($digitsOnly, '84')) {
            $rest = substr($digitsOnly, 2);
            $restLen = strlen($rest);
            if ($restLen >= 9 && $restLen <= 10) {
                $digitsOnly = '0'.$rest;
            }
        } elseif (str_starts_with($digitsOnly, '84')) {
            $rest = substr($digitsOnly, 2);
            $restLen = strlen($rest);
            if ($restLen >= 9 && $restLen <= 10) {
                $digitsOnly = '0'.$rest;
            }
        }

        // Kiểm tra khớp ^0\d{9,10}$ (10 hoặc 11 chữ số, bắt đầu bằng 0)
        if (preg_match('/^0\d{9,10}$/', $digitsOnly)) {
            return $digitsOnly;
        }

        return null;
    }

    /**
     * Kiểm tra xem số điện thoại có hợp lệ hay không.
     */
    public static function isValid(?string $phone): bool
    {
        return self::normalize($phone) !== null;
    }
}
