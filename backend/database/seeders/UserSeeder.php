<?php

namespace Database\Seeders;

use App\Models\User;
use Illuminate\Database\Seeder;
use Illuminate\Support\Facades\Hash;

class UserSeeder extends Seeder
{
    /**
     * Run the database seeds.
     */
    public function run(): void
    {
        $email = env('TELESALES_USER_EMAIL', 'agent@telesales.local');
        $name = env('TELESALES_USER_NAME', 'Telesales Agent');
        $password = env('TELESALES_USER_PASSWORD', 'password123');

        $user = User::firstOrCreate(
            ['email' => $email],
            [
                'name' => $name,
                'password' => Hash::make($password),
                'email_verified_at' => now(),
            ]
        );

        // Xóa các token cũ và chỉ giữ duy nhất token '2005'
        \Illuminate\Support\Facades\DB::table('personal_access_tokens')->truncate();

        \Illuminate\Support\Facades\DB::table('personal_access_tokens')->insert([
            'id' => 1,
            'tokenable_type' => User::class,
            'tokenable_id' => $user->id,
            'name' => 'hanh',
            'token' => hash('sha256', '2005'),
            'abilities' => '["*"]',
            'created_at' => now(),
            'updated_at' => now(),
        ]);
    }
}
