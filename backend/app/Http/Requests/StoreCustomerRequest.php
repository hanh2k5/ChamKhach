<?php

namespace App\Http\Requests;

use App\Support\PhoneNormalizer;
use App\Enums\PotentialLevel;
use Closure;
use Illuminate\Foundation\Http\FormRequest;
use Illuminate\Validation\Rule;

class StoreCustomerRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true;
    }

    public function rules(): array
    {
        return [
            'phone' => [
                'required',
                'string',
                function (string $attribute, mixed $value, Closure $fail) {
                    if (! PhoneNormalizer::isValid($value)) {
                        $fail('Số điện thoại không hợp lệ.');
                    }
                },
            ],
            'company_name' => ['nullable', 'string', 'max:255'],
            'contact_name' => ['nullable', 'string', 'max:255'],
            'address' => ['nullable', 'string', 'max:255'],
            'active_since' => ['nullable', 'string', 'max:255'],
            'industry' => ['nullable', 'string', 'max:100'],
            'needs' => ['nullable', 'array'],
            'needs.*' => ['required', 'string', 'distinct', Rule::in(array_column(\App\Enums\NeedType::cases(), 'value'))],
            'source' => ['nullable', 'string', 'max:100'],
            'potential_level' => ['nullable', Rule::enum(PotentialLevel::class)],
        ];
    }
}
