<?php

namespace App\Http\Requests;

use App\Enums\CallResult;
use App\Enums\NeedType;
use Illuminate\Foundation\Http\FormRequest;
use Illuminate\Validation\Rule;

class StoreInteractionRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true;
    }

    public function rules(): array
    {
        $validResults = array_column(CallResult::cases(), 'value');
        $validNeeds   = array_column(NeedType::cases(), 'value');

        return [
            'result'              => ['required', 'string', Rule::in($validResults)],
            'needs'               => ['required', 'array', 'min:1', 'max:5'],
            'needs.*'             => ['required', 'string', Rule::in($validNeeds)],
            'note'                => ['nullable', 'string', 'max:500'],
            'follow_up'           => ['nullable', 'array'],
            'follow_up.remind_at' => ['required_with:follow_up', 'date', 'after:now'],
        ];
    }

    public function messages(): array
    {
        return [
            'result.required'              => 'Kết quả cuộc gọi là bắt buộc.',
            'result.in'                    => 'Kết quả cuộc gọi không hợp lệ.',
            'needs.required'               => 'Nhu cầu là bắt buộc.',
            'needs.min'                    => 'Phải chọn ít nhất 1 nhu cầu.',
            'needs.*.in'                   => 'Loại nhu cầu không hợp lệ.',
            'follow_up.remind_at.after'    => 'Thời gian nhắc phải ở tương lai.',
            'follow_up.remind_at.required_with' => 'Thời gian nhắc là bắt buộc khi có lịch gọi lại.',
        ];
    }
}
