<?php
/**
 * api/submit.php — Enregistrement des réponses du sondage (PHP 5.6)
 *
 * POST /api_survey/api/submit.php
 * Content-Type: application/json
 *
 * Body attendu :
 * {
 *   "device_id":     "abc123",
 *   "ip_chambre":    "192.168.0.189",
 *   "num_chambre":   "204",
 *   "nom_client":    "DUPONT",
 *   "prenom_client": "Jean",
 *   "langue":        "FR",
 *   "reponses": [
 *     { "question_id": 1, "category_id": 1, "rating": 4, "skipped": 0 },
 *     { "question_id": 2, "category_id": 2, "rating": 0, "skipped": 1 },
 *     ...
 *   ]
 * }
 *
 * Les réponses sont stockées en JSON dans survey_results.reponses.
 * N questions → N objets dans le tableau, sans modifier la structure de la table.
 *
 * Réponse succès : { "success": true, "id": 42 }
 * Réponse erreur : { "success": false, "error": "..." }
 */

require_once dirname(__FILE__) . '/../config.php';

// ─── Méthode ────────────────────────────────────────────────────────────────
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo json_encode(array('success' => false, 'error' => 'Methode non autorisee'));
    exit;
}

// ─── Lecture du body JSON ────────────────────────────────────────────────────
$body = file_get_contents('php://input');
$data = json_decode($body, true);

if (!$data || !isset($data['reponses']) || !is_array($data['reponses'])) {
    http_response_code(400);
    echo json_encode(array('success' => false, 'error' => 'Body JSON invalide ou reponses manquantes'));
    exit;
}

// Helper PHP 5.6 (pas de ??)
function val($arr, $key, $default) {
    return isset($arr[$key]) ? $arr[$key] : $default;
}

// ─── Préparer les réponses (nettoyage des types) ─────────────────────────────
$reponses = array();
foreach ($data['reponses'] as $rep) {
    $reponses[] = array(
        'question_id' => (int) val($rep, 'question_id', 0),
        'category_id' => (int) val($rep, 'category_id', 0),
        'rating'      => (int) val($rep, 'rating',      0),
        'skipped'     => (int) val($rep, 'skipped',     0),
    );
}
$reponsesJson = json_encode($reponses);

// ─── Insertion en base (une seule ligne) ─────────────────────────────────────
$pdo = getDb();
$ip  = val($data, 'ip_chambre', '');

try {
    $pdo->beginTransaction();

    // 1. Insérer le sondage complet (entête + réponses JSON en une ligne)
    $stmt = $pdo->prepare(
        "INSERT INTO survey_results
             (ip_chambre, num_chambre, nom_client, prenom_client, device_id, langue, reponses, submitted_at)
         VALUES
             (:ip, :num, :nom, :prenom, :device, :langue, :reponses, NOW())"
    );
    $stmt->execute(array(
        ':ip'       => $ip,
        ':num'      => val($data, 'num_chambre',   ''),
        ':nom'      => val($data, 'nom_client',    ''),
        ':prenom'   => val($data, 'prenom_client', ''),
        ':device'   => val($data, 'device_id',     ''),
        ':langue'   => val($data, 'langue',         'FR'),
        ':reponses' => $reponsesJson,
    ));
    $resultId = (int) $pdo->lastInsertId();

    // 2. Marquer le sondage comme complété dans t_chambre
    if ($ip !== '') {
        $pdo->prepare(
            "UPDATE t_chambre SET survey = 1
             WHERE IP_LAN = :ip OR ip_chambre = :ip2"
        )->execute(array(':ip' => $ip, ':ip2' => $ip));

        // Nettoyer le tracking "Plus tard" et "Jamais" (sondage complété)
        $pdo->prepare(
            "DELETE FROM survey_guest_tracking WHERE ip_chambre = :ip"
        )->execute(array(':ip' => $ip));
    }

    $pdo->commit();
    echo json_encode(array('success' => true, 'id' => $resultId));

} catch (PDOException $e) {
    $pdo->rollBack();
    http_response_code(500);
    echo json_encode(array('success' => false, 'error' => $e->getMessage()));
}
