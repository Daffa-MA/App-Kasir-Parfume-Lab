<?php

use App\Services\LegacyPosImporter;
use Illuminate\Foundation\Inspiring;
use Illuminate\Support\Facades\Artisan;

Artisan::command('inspire', function () {
    $this->comment(Inspiring::quote());
})->purpose('Display an inspiring quote');

Artisan::command('pos:import-legacy {--path=../backend/pos.db}', function (LegacyPosImporter $importer) {
    $pathOption = (string) $this->option('path');
    $legacyPath = str_starts_with($pathOption, DIRECTORY_SEPARATOR)
        ? $pathOption
        : base_path($pathOption);

    $this->info('Importing legacy DB: '.$legacyPath);
    $summary = $importer->import($legacyPath);

    $this->table(['Metric', 'Value'], [
        ['Products imported', $summary['products_imported'] ?? 0],
        ['Transactions imported', $summary['transactions_imported'] ?? 0],
        ['Transactions skipped', $summary['transactions_skipped'] ?? 0],
        ['Items imported', $summary['items_imported'] ?? 0],
        ['Items skipped', $summary['items_skipped'] ?? 0],
    ]);

    $this->info('Legacy import complete.');
})->purpose('Import historical POS data from legacy SQLite db');
