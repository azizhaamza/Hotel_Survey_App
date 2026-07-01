<?php
/**
 * config.php — Configuration base de données (PHP 5.6)
 *
 * !! ADAPTER ces valeurs à votre hébergement !!
 */

// ─── Paramètres de connexion ─────────────────────────────────────────────────
define('DB_HOST', 'localhost');      // hôte MySQL/MariaDB
define('DB_NAME', 'hotel_survey');   // nom de la base (à créer si inexistante)
define('DB_USER', 'root');           // utilisateur MySQL
define('DB_PASS', '');               // mot de passe MySQL
define('DB_CHARSET', 'utf8mb4');

// ─── Mode debug (mettre false en production) ─────────────────────────────────
define('DEBUG_MODE', true);

// ─── En-têtes communs ────────────────────────────────────────────────────────
header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

// Afficher toutes les erreurs si DEBUG_MODE
if (DEBUG_MODE) {
    ini_set('display_errors', 1);
    ini_set('display_startup_errors', 1);
    error_reporting(E_ALL);
} else {
    ini_set('display_errors', 0);
    error_reporting(0);
}

/**
 * Retourne une connexion PDO (singleton).
 */
function getDb() {
    static $pdo = null;
    if ($pdo !== null) {
        return $pdo;
    }

    $dsn = 'mysql:host=' . DB_HOST
         . ';dbname=' . DB_NAME
         . ';charset=' . DB_CHARSET;

    $options = array(
        PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
        PDO::ATTR_EMULATE_PREPARES   => false,
    );

    try {
        $pdo = new PDO($dsn, DB_USER, DB_PASS, $options);
    } catch (PDOException $e) {
        http_response_code(500);
        $msg = DEBUG_MODE ? $e->getMessage() : 'Erreur de connexion a la base de donnees';
        echo json_encode(array('success' => false, 'error' => 'DB_CONNECT: ' . $msg));
        exit;
    }

    return $pdo;
}
