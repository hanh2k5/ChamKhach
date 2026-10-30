<?php

namespace App\Services;

use App\Enums\CustomerStatus;
use App\Enums\FollowUpStatus;
use App\Enums\PotentialLevel;
use App\Exceptions\DuplicatePhoneException;
use App\Models\Customer;
use App\Models\FollowUp;
use App\Models\Interaction;
use App\Support\PhoneNormalizer;
use Illuminate\Contracts\Pagination\LengthAwarePaginator;
use Illuminate\Database\Eloquent\Builder;
use Illuminate\Support\Facades\DB;

class CustomerService
{
    /**
     * Lấy danh sách khách hàng có phân trang, lọc, tìm kiếm và sắp xếp.
     */
    public function list(array $filters): LengthAwarePaginator
    {
        $query = Customer::query()
            ->select('customers.*')
            ->withCount('interactions as interaction_count')
            ->withMax('interactions as last_contacted_at', 'contacted_at')
            ->selectSub(
                Interaction::select('result')
                    ->whereColumn('customer_id', 'customers.id')
                    ->orderByDesc('contacted_at')
                    ->orderByDesc('id')
                    ->limit(1),
                'last_result'
            );

        // Lọc theo status: active (mặc định), archived, all
        $status = $filters['status'] ?? 'active';
        if ($status === 'active') {
            $query->where('customers.status', CustomerStatus::Active);
        } elseif ($status === 'archived') {
            $query->where('customers.status', CustomerStatus::Archived);
        }
        // status === 'all' thì không lọc theo status

        // Lọc theo potential_level
        if (! empty($filters['potential'])) {
            $query->where('customers.potential_level', $filters['potential']);
        }

        // Lọc theo last_result
        if (! empty($filters['last_result'])) {
            $query->whereRaw(
                '(' . Interaction::select('result')
                    ->whereColumn('customer_id', 'customers.id')
                    ->orderByDesc('contacted_at')
                    ->orderByDesc('id')
                    ->limit(1)
                    ->toSql() . ') = ?',
                [$filters['last_result']]
            );
        }

        // Tìm kiếm theo q (company_name, contact_name, phone_normalized)
        if (! empty($filters['q'])) {
            $q = trim((string) $filters['q']);
            $escaped = str_replace(['\\', '%', '_'], ['\\\\', '\\%', '\\_'], $q);
            $digitsOnly = preg_replace('/\D/', '', $q);

            $query->where(function (Builder $builder) use ($escaped, $digitsOnly) {
                $builder->where('company_name', 'LIKE', "%{$escaped}%")
                    ->orWhere('contact_name', 'LIKE', "%{$escaped}%");

                if ($digitsOnly !== '') {
                    $builder->orWhere('phone_normalized', 'LIKE', "%{$digitsOnly}%");
                }
            });
        }

        // Sắp xếp: last_contacted_at giảm dần (null cuối), rồi id giảm dần
        $query->orderByRaw('last_contacted_at IS NULL, last_contacted_at DESC')
            ->orderBy('customers.id', 'desc');

        $perPage = (int) ($filters['per_page'] ?? 20);
        if ($perPage <= 0) {
            $perPage = 20;
        } elseif ($perPage > 50) {
            $perPage = 50;
        }

        return $query->paginate($perPage);
    }

    /**
     * Tạo mới một khách hàng.
     *
     * @throws DuplicatePhoneException
     */
    public function create(array $data): Customer
    {
        $rawPhone = trim((string) $data['phone']);
        $normalizedPhone = PhoneNormalizer::normalize($rawPhone);

        $existing = Customer::where('phone_normalized', $normalizedPhone)->first();
        if ($existing) {
            throw new DuplicatePhoneException($existing->id);
        }

        $customer = Customer::create([
            'company_name' => $data['company_name'] ?? null,
            'contact_name' => $data['contact_name'] ?? null,
            'phone' => $rawPhone,
            'phone_normalized' => $normalizedPhone,
            'address' => $data['address'] ?? null,
            'industry' => $data['industry'] ?? null,
            'source' => $data['source'] ?? null,
            'potential_level' => isset($data['potential_level'])
                ? PotentialLevel::from($data['potential_level'])
                : PotentialLevel::Unrated,
            'status' => CustomerStatus::Active,
        ]);

        $customer->interaction_count = 0;
        $customer->last_contacted_at = null;
        $customer->last_result = null;

        return $customer;
    }

    /**
     * Lấy thông tin chi tiết một khách hàng.
     */
    public function find(int $id): Customer
    {
        $customer = Customer::query()
            ->select('customers.*')
            ->withCount('interactions as interaction_count')
            ->withMax('interactions as last_contacted_at', 'contacted_at')
            ->selectSub(
                Interaction::select('result')
                    ->whereColumn('customer_id', 'customers.id')
                    ->orderByDesc('contacted_at')
                    ->orderByDesc('id')
                    ->limit(1),
                'last_result'
            )
            ->with(['followUps' => function ($q) {
                $q->where('status', FollowUpStatus::Pending)
                    ->orderBy('remind_at');
            }])
            ->findOrFail($id);

        return $customer;
    }

    /**
     * Cập nhật thông tin khách hàng.
     *
     * @throws DuplicatePhoneException
     * @return array{customer: Customer, cancelled_follow_up_ids?: array<int>}
     */
    public function update(Customer $customer, array $data): array
    {
        if (isset($data['phone'])) {
            $rawPhone = trim((string) $data['phone']);
            $normalizedPhone = PhoneNormalizer::normalize($rawPhone);

            $existing = Customer::where('phone_normalized', $normalizedPhone)
                ->where('id', '!=', $customer->id)
                ->first();

            if ($existing) {
                throw new DuplicatePhoneException($existing->id);
            }

            $customer->phone = $rawPhone;
            $customer->phone_normalized = $normalizedPhone;
        }

        if (array_key_exists('company_name', $data)) {
            $customer->company_name = $data['company_name'];
        }
        if (array_key_exists('contact_name', $data)) {
            $customer->contact_name = $data['contact_name'];
        }
        if (array_key_exists('address', $data)) {
            $customer->address = $data['address'];
        }
        if (array_key_exists('industry', $data)) {
            $customer->industry = $data['industry'];
        }
        if (array_key_exists('source', $data)) {
            $customer->source = $data['source'];
        }
        if (isset($data['potential_level'])) {
            $customer->potential_level = PotentialLevel::from($data['potential_level']);
        }

        $cancelledIds = [];
        $isArchiving = isset($data['status']) && $data['status'] === CustomerStatus::Archived->value;

        if ($isArchiving) {
            DB::transaction(function () use ($customer, &$cancelledIds) {
                $pendingFollowUps = FollowUp::where('customer_id', $customer->id)
                    ->where('status', FollowUpStatus::Pending)
                    ->lockForUpdate()
                    ->get();

                $cancelledIds = $pendingFollowUps->pluck('id')->all();

                if (! empty($cancelledIds)) {
                    FollowUp::whereIn('id', $cancelledIds)->update([
                        'status' => FollowUpStatus::Cancelled,
                    ]);
                }

                $customer->status = CustomerStatus::Archived;
                $customer->save();
            });
        } else {
            if (isset($data['status'])) {
                $customer->status = CustomerStatus::from($data['status']);
            }
            $customer->save();
        }

        // Nạp lại các trường tổng hợp
        $customer = $this->find($customer->id);

        $result = ['customer' => $customer];
        if ($isArchiving) {
            $result['cancelled_follow_up_ids'] = $cancelledIds;
        }

        return $result;
    }
}
