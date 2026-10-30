<?php

namespace App\Console\Commands;

use App\Models\User;
use Illuminate\Console\Command;
use Illuminate\Support\Facades\Hash;

class IssueTokenCommand extends Command
{
    /**
     * The name and signature of the console command.
     *
     * @var string
     */
    protected $signature = 'telesales:issue-token {email? : Email cua nguoi dung} {--name=telesales-token : Ten token}';

    /**
     * The console command description.
     *
     * @var string
     */
    protected $description = 'Cap Bearer token Sanctum cho user telesales';

    /**
     * Execute the console command.
     */
    public function handle(): int
    {
        $email = $this->argument('email') ?? env('TELESALES_USER_EMAIL', 'agent@telesales.local');
        $tokenName = (string) $this->option('name');

        $user = User::where('email', $email)->first();

        if (! $user) {
            $this->warn("User [{$email}] chua ton tai. Dang tao user moi...");
            $user = User::create([
                'name' => env('TELESALES_USER_NAME', 'Telesales Agent'),
                'email' => $email,
                'password' => Hash::make(env('TELESALES_USER_PASSWORD', 'password123')),
                'email_verified_at' => now(),
            ]);
        }

        $token = $user->createToken($tokenName)->plainTextToken;

        $this->info("Cap token thanh cong cho: {$user->email}");
        $this->line("Token: {$token}");

        return self::SUCCESS;
    }
}
