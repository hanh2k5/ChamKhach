<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::table('interactions', function (Blueprint $table) {
            $table->dropForeign(['customer_id']);
            $table->foreign('customer_id')->references('id')->on('customers')->cascadeOnDelete();
        });

        Schema::table('interaction_needs', function (Blueprint $table) {
            $table->dropForeign(['interaction_id']);
            $table->foreign('interaction_id')->references('id')->on('interactions')->cascadeOnDelete();
        });

        Schema::table('follow_ups', function (Blueprint $table) {
            $table->dropForeign(['customer_id']);
            $table->dropForeign(['interaction_id']);
            $table->foreign('customer_id')->references('id')->on('customers')->cascadeOnDelete();
            $table->foreign('interaction_id')->references('id')->on('interactions')->cascadeOnDelete();
        });
    }

    public function down(): void
    {
        Schema::table('follow_ups', function (Blueprint $table) {
            $table->dropForeign(['customer_id']);
            $table->dropForeign(['interaction_id']);
            $table->foreign('customer_id')->references('id')->on('customers')->restrictOnDelete();
            $table->foreign('interaction_id')->references('id')->on('interactions')->restrictOnDelete();
        });

        Schema::table('interaction_needs', function (Blueprint $table) {
            $table->dropForeign(['interaction_id']);
            $table->foreign('interaction_id')->references('id')->on('interactions')->restrictOnDelete();
        });

        Schema::table('interactions', function (Blueprint $table) {
            $table->dropForeign(['customer_id']);
            $table->foreign('customer_id')->references('id')->on('customers')->restrictOnDelete();
        });
    }
};
