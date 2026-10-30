<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    /**
     * Run the migrations.
     */
    public function up(): void
    {
        Schema::create('customers', function (Blueprint $table) {
            $table->id();
            $table->string('company_name', 255)->nullable();
            $table->string('contact_name', 255)->nullable();
            $table->string('phone', 32);
            $table->string('phone_normalized', 16)->unique();
            $table->string('address', 500)->nullable();
            $table->string('industry', 255)->nullable();
            $table->string('source', 255)->nullable();
            $table->string('potential_level', 16)->default('unrated');
            $table->string('status', 16)->default('active')->index();
            $table->timestamps();
        });
    }

    /**
     * Reverse the migrations.
     */
    public function down(): void
    {
        Schema::dropIfExists('customers');
    }
};
