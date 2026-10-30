<?php

namespace Tests\Feature;

use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class AuthTest extends TestCase
{
    use RefreshDatabase;

    public function test_ping_without_token_returns_401_json(): void
    {
        $response = $this->get('/api/v1/ping');

        $response->assertStatus(401)
            ->assertHeader('Content-Type', 'application/json')
            ->assertExactJson([
                'message' => 'Unauthenticated.',
            ]);
    }

    public function test_ping_with_invalid_token_returns_401_json(): void
    {
        $response = $this->withHeaders([
            'Authorization' => 'Bearer invalid-token-sample',
        ])->get('/api/v1/ping');

        $response->assertStatus(401)
            ->assertExactJson([
                'message' => 'Unauthenticated.',
            ]);
    }

    public function test_ping_with_valid_sanctum_token_returns_200(): void
    {
        $user = User::factory()->create([
            'name' => 'Agent Test',
            'email' => 'agent@test.local',
        ]);

        $token = $user->createToken('test-token')->plainTextToken;

        $response = $this->withHeaders([
            'Authorization' => 'Bearer '.$token,
        ])->get('/api/v1/ping');

        $response->assertStatus(200)
            ->assertJsonPath('status', 'ok')
            ->assertJsonPath('message', 'pong')
            ->assertJsonPath('user.email', 'agent@test.local');
    }

    public function test_telesales_issue_token_artisan_command_generates_token(): void
    {
        $this->artisan('telesales:issue-token', [
            'email' => 'cli-agent@test.local',
            '--name' => 'cli-token',
        ])
            ->expectsOutputToContain('Cap token thanh cong cho: cli-agent@test.local')
            ->assertSuccessful();

        $this->assertDatabaseHas('users', [
            'email' => 'cli-agent@test.local',
        ]);

        $this->assertDatabaseHas('personal_access_tokens', [
            'name' => 'cli-token',
        ]);
    }

    public function test_not_found_endpoint_returns_json_404_even_without_accept_header(): void
    {
        $response = $this->get('/api/v1/non-existent-endpoint');

        $response->assertStatus(404)
            ->assertExactJson([
                'message' => 'Not found.',
            ]);
    }
}
