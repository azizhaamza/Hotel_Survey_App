<?php
/**
 * api/debug.php — Diagnostic complet de l'installation (PHP 5.6)
 *
 * GET /api_survey/api/debug.php
 *
 * !! SUPPRIMER CE FICHIER EN PRODUCTION !!
 */

ini_set('display_errors', 1);
error_reporting(E_ALL);
header('Content-Type: application/json; charset=utf-8');

$report = array();

// 1. Version PHP
$report['php_version']  = PHP_VERSION;
$report['php_ok']       = version_compare(PHP_VERSION, '5.6.0', '>=');

// 2. Extensions requises
$extensions = array('pdo', 'pdo_mysql', 'json', 'mbstring');
$report['extensions'] = array();
foreach ($extensions as $ext) {
    $report['extensions'][$ext] = extension_loaded($ext);
}

// 3. Connexion base de données
require_once dirname(__FILE__) . '/../config.php';

try {
    $pdo = getDb();
    $report['db_connect'] = 'OK';

    // 4. Tables existantes
    $stmt = $pdo->query("SHOW TABLES");
    $tables = array();
    while ($row = $stmt->fetch(PDO::FETCH_NUM)) {
        $tables[] = $row[0];
    }
    $report['tables_found'] = $tables;

    // 5. Vérification des tables requises
    $required = array('t_chambre', 'survey_results', 'survey_guest_tracking');
    $report['tables_check'] = array();
    foreach ($required as $table) {
        $report['tables_check'][$table] = in_array($table, $tables) ? 'OK' : 'MANQUANTE';
    }

    // 6. Colonnes de t_chambre
    if (in_array('t_chambre', $tables)) {
        $stmt = $pdo->query("DESCRIBE t_chambre");
        $cols = array();
        while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
            $cols[] = $row['Field'];
        }
        $report['t_chambre_columns'] = $cols;

        // Colonnes critiques
        $needed = array('IP_LAN', 'ip_chambre', 'nom_client', 'prenom_client', 'num_chambre', 'langue', 'survey');
        $report['t_chambre_columns_check'] = array();
        foreach ($needed as $col) {
            $report['t_chambre_columns_check'][$col] = in_array($col, $cols) ? 'OK' : 'MANQUANTE';
        }

        // Nombre de chambres
        $count = $pdo->query("SELECT COUNT(*) FROM t_chambre")->fetchColumn();
        $report['t_chambre_rows'] = (int) $count;

        // Aperçu des 3 premières chambres (sans données sensibles)
        $sample = $pdo->query(
            "SELECT IP_LAN, ip_chambre, num_chambre, langue, survey FROM t_chambre LIMIT 3"
        )->fetchAll(PDO::FETCH_ASSOC);
        $report['t_chambre_sample'] = $sample;
    }

    // 7. Colonnes de survey_results
    if (in_array('survey_results', $tables)) {
        $stmt = $pdo->query("DESCRIBE survey_results");
        $cols = array();
        while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
            $cols[] = $row['Field'] . ' (' . $row['Type'] . ')';
        }
        $report['survey_results_columns'] = $cols;
        $count = $pdo->query("SELECT COUNT(*) FROM survey_results")->fetchColumn();
        $report['survey_results_rows'] = (int) $count;
    }

    // 8. Colonnes de survey_guest_tracking
    if (in_array('survey_guest_tracking', $tables)) {
        $stmt = $pdo->query("DESCRIBE survey_guest_tracking");
        $cols = array();
        while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
            $cols[] = $row['Field'] . ' (' . $row['Type'] . ')';
        }
        $report['survey_guest_tracking_columns'] = $cols;
        $count = $pdo->query("SELECT COUNT(*) FROM survey_guest_tracking")->fetchColumn();
        $report['survey_guest_tracking_rows'] = (int) $count;
    }

    // 9. Test guest.php avec l'IP fournie en paramètre
    if (isset($_GET['ip'])) {
        $ip = trim($_GET['ip']);
        $stmt = $pdo->prepare(
            "SELECT IP_LAN, ip_chambre, nom_client, prenom_client, num_chambre, langue, survey
             FROM t_chambre
             WHERE IP_LAN = :ip OR ip_chambre = :ip2
             LIMIT 1"
        );
        $stmt->execute(array(':ip' => $ip, ':ip2' => $ip));
        $row = $stmt->fetch(PDO::FETCH_ASSOC);
        $report['test_ip'] = $ip;
        $report['test_ip_result'] = $row ? $row : 'NON TROUVE';
    }

} catch (PDOException $e) {
    $report['db_connect'] = 'ERREUR: ' . $e->getMessage();
}

// 10. Infos serveur
$report['server_software'] = isset($_SERVER['SERVER_SOFTWARE']) ? $_SERVER['SERVER_SOFTWARE'] : 'inconnu';
$report['document_root']   = isset($_SERVER['DOCUMENT_ROOT'])   ? $_SERVER['DOCUMENT_ROOT']   : 'inconnu';
$report['script_path']     = __FILE__;

echo json_encode($report, JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE);
