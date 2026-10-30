<?php

namespace Tests\Unit;

use App\Support\PhoneNormalizer;
use PHPUnit\Framework\Attributes\DataProvider;
use PHPUnit\Framework\TestCase;

class PhoneNormalizerTest extends TestCase
{
    #[DataProvider('validPhoneProvider')]
    public function test_normalizes_valid_phone_numbers(string $input, string $expected): void
    {
        $this->assertSame($expected, PhoneNormalizer::normalize($input));
        $this->assertTrue(PhoneNormalizer::isValid($input));
    }

    public static function validPhoneProvider(): array
    {
        return [
            'standard 10 digits' => ['0912345678', '0912345678'],
            'with spaces' => ['0912 345 678', '0912345678'],
            'with dots' => ['0912.345.678', '0912345678'],
            'with dashes' => ['0912-345-678', '0912345678'],
            'with +84' => ['+84912345678', '0912345678'],
            'with 84' => ['84912345678', '0912345678'],
            'with +84 and spaces' => ['+84 912 345 678', '0912345678'],
            'with +84 and dots' => ['+84.912.345.678', '0912345678'],
            'standard 11 digits' => ['01234567890', '01234567890'],
            '11 digits with +84' => ['+841234567890', '01234567890'],
            '11 digits with 84' => ['841234567890', '01234567890'],
        ];
    }

    #[DataProvider('invalidPhoneProvider')]
    public function test_returns_null_for_invalid_phone_numbers(?string $input): void
    {
        $this->assertNull(PhoneNormalizer::normalize($input));
        $this->assertFalse(PhoneNormalizer::isValid($input));
    }

    public static function invalidPhoneProvider(): array
    {
        return [
            'null' => [null],
            'empty string' => [''],
            'spaces only' => ['   '],
            'too short (9 digits)' => ['091234567'],
            'too short 8 digits' => ['09123456'],
            'too long (12 digits)' => ['0912345678901'],
            'letters only' => ['abcdefghij'],
            'letters mixed' => ['0912abc678'],
            'letter suffix' => ['0912345678x'],
            'letter prefix' => ['x0912345678'],
            'special chars only' => ['+--.'],
            'plus in the middle' => ['0912+345678'],
            'not starting with 0 or 84' => ['1900123456'],
            'international non-VN' => ['+1234567890'],
        ];
    }
}
