<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Http\Requests\StoreCustomerRequest;
use App\Http\Requests\UpdateCustomerRequest;
use App\Http\Resources\CustomerListResource;
use App\Http\Resources\CustomerResource;
use App\Models\Customer;
use App\Services\CustomerService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\AnonymousResourceCollection;

class CustomerController extends Controller
{
    public function __construct(
        protected CustomerService $customerService
    ) {}

    /**
     * Danh sách khách hàng có phân trang.
     */
    public function index(Request $request): AnonymousResourceCollection
    {
        $customers = $this->customerService->list($request->all());

        return CustomerListResource::collection($customers);
    }

    /**
     * Tạo khách hàng mới.
     */
    public function store(StoreCustomerRequest $request): JsonResponse
    {
        $customer = $this->customerService->create($request->validated());

        return (new CustomerResource($customer))
            ->response()
            ->setStatusCode(201);
    }

    /**
     * Chi tiết khách hàng.
     */
    public function show(int $id): CustomerResource
    {
        $customer = $this->customerService->find($id);

        return new CustomerResource($customer);
    }

    /**
     * Cập nhật thông tin khách hàng.
     */
    public function update(UpdateCustomerRequest $request, int $id): CustomerResource
    {
        $customer = Customer::findOrFail($id);
        $result = $this->customerService->update($customer, $request->validated());

        return new CustomerResource($result['customer']);
    }

    /** Permanently delete a customer and all dependent records. */
    public function destroy(int $id): JsonResponse
    {
        $followUpIds = $this->customerService->delete($id);

        return response()->json(['data' => ['deleted_follow_up_ids' => $followUpIds]]);
    }
}
