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
        Schema::create('interaction_needs', function (Blueprint $table) {
            $table->foreignId('interaction_id')->constrained('interactions')->restrictOnDelete();
            $table->string('need_type', 16);

            $table->primary(['interaction_id', 'need_type']);
        });
    }

    /**
     * Reverse the migrations.
     */
    public function down(): void
    {
        Schema::dropIfExists('interaction_needs');
    }
};
