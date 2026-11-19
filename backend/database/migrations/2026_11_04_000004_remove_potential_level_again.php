<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        if (Schema::hasColumn('customers', 'potential_level')) {
            Schema::table('customers', function (Blueprint $table): void {
                $table->dropColumn('potential_level');
            });
        }
    }

    public function down(): void
    {
        if (! Schema::hasColumn('customers', 'potential_level')) {
            Schema::table('customers', function (Blueprint $table): void {
                $table->string('potential_level', 16)->default('unrated')->after('source');
            });
        }
    }
};
