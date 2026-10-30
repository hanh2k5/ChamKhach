<?php

namespace App\Http\Requests;

use Illuminate\Foundation\Http\FormRequest;
use Illuminate\Validation\Rule;

class PatchFollowUpRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true;
    }

    public function rules(): array
    {
        return [
            'action' => ['required', 'string', Rule::in(['complete', 'cancel', 'reschedule'])],
            'remind_at' => [
                Rule::requiredIf(fn () => $this->input('action') === 'reschedule'),
                'nullable',
                'date',
                'after:now',
            ],
        ];
    }

    public function messages(): array
    {
        return [
            'action.required' => 'Hành động là bắt buộc.',
            'action.in' => 'Hành động không hợp lệ.',
            'remind_at.required_if' => 'Thời gian nhắc là bắt buộc khi dời lịch.',
            'remind_at.date' => 'Thời gian nhắc không đúng định dạng.',
            'remind_at.after' => 'Thời gian nhắc phải ở tương lai.',
        ];
    }
}
