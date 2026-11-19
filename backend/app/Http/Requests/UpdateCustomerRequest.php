<?php

namespace App\Http\Requests;

use App\Support\PhoneNormalizer;
use App\Enums\PotentialLevel;
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
            'active_since' => ['sometimes', 'nullable', 'string', 'max:255'],
            'industry' => ['sometimes', 'nullable', 'string', 'max:100'],
            'needs' => ['sometimes', 'nullable', 'array'],
            'needs.*' => ['required', 'string', 'distinct', Rule::in(array_column(\App\Enums\NeedType::cases(), 'value'))],
            'source' => ['sometimes', 'nullable', 'string', 'max:100'],
            'potential_level' => ['sometimes', 'nullable', Rule::enum(PotentialLevel::class)],
        ];
    }
}
