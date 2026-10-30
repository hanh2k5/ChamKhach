<?php

namespace App\Http\Requests;

use App\Enums\CustomerStatus;
use App\Enums\PotentialLevel;
use App\Support\PhoneNormalizer;
use Closure;
use Illuminate\Foundation\Http\FormRequest;
use Illuminate\Validation\Rule;

class UpdateCustomerRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true;
    }

    public function rules(): array
    {
        return [
            'phone' => [
                'sometimes',
                'required',
                'string',
                function (string $attribute, mixed $value, Closure $fail) {
                    if (! PhoneNormalizer::isValid($value)) {
                        $fail('Số điện thoại không hợp lệ.');
                    }
                },
            ],
            'company_name' => ['sometimes', 'nullable', 'string', 'max:255'],
            'contact_name' => ['sometimes', 'nullable', 'string', 'max:255'],
            'address' => ['sometimes', 'nullable', 'string', 'max:255'],
            'industry' => ['sometimes', 'nullable', 'string', 'max:100'],
            'source' => ['sometimes', 'nullable', 'string', 'max:100'],
            'potential_level' => ['sometimes', 'nullable', Rule::enum(PotentialLevel::class)],
            'status' => ['sometimes', 'required', Rule::enum(CustomerStatus::class)],
        ];
    }
}
