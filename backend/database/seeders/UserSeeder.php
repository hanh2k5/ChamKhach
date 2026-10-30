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

        User::firstOrCreate(
            ['email' => $email],
            [
                'name' => $name,
                'password' => Hash::make($password),
                'email_verified_at' => now(),
            ]
        );
    }
}
