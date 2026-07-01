<?php
/**
 * api/survey_action.php — Enregistre l'action du client (PHP 5.6)
 *
 * GET /api_survey/api/survey_action.php?ip=192.168.0.189&action=never
 *
 * Actions supportees :
 *   never  -> survey = -1, memorise le client dans survey_guest_tracking
 *   later  -> stocke later_until = NOW() + 2h dans survey_guest_tracking
 *   done   -> survey =  1, nettoie le tracking (fallback si submit.php a echoue)
 *
 * Reponse : { "success": true }  ou  { "success": false, "error": "..." }
 */

require_once dirname(__FILE__) . '/../config.php';

// Parametres
$ip     = isset($_GET['ip'])     ? trim($_GET['ip'])                 : '';
$action = isset($_GET['action']) ? strtolower(trim($_GET['action'])) : '';

if ($ip === '' || $action === '') {
    http_response_code(400);
    echo json_encode(array('success' => false, 'error' => 'Parametres ip et action requis'));
    exit;
}

// Traitement
switch ($action) {

    case 'never':
        $surveyValue = -1;
        break;

    case 'done':
        $surveyValue = 1;
        break;

    case 'later':
        // Stocker le cooldown 2h cote serveur (resiste aux redemarrages de la box)
        try {
            $pdoL = getDb();
            $pdoL->prepare(
                "INSERT INTO survey_guest_tracking (ip_chambre, later_until)
                 VALUES (:ip, DATE_ADD(NOW(), INTERVAL 2 HOUR))
                 ON DUPLICATE KEY UPDATE later_until = DATE_ADD(NOW(), INTERVAL 2 HOUR)"
            )->execute(array(':ip' => $ip));
        } catch (PDOException $e) { /* non bloquant */ }
        echo json_encode(array('success' => true));
        exit;

    default:
        http_response_code(400);
        echo json_encode(array('success' => false, 'error' => 'Action inconnue : ' . $action));
        exit;
}

// Mise a jour
try {
    $pdo = getDb();
    $pdo->beginTransaction();

    // 1. Mettre a jour le statut survey dans t_chambre
    $stmt = $pdo->prepare(
        "UPDATE t_chambre
         SET    survey = :val
         WHERE  IP_LAN = :ip OR ip_chambre = :ip2"
    );
    $stmt->execute(array(':val' => $surveyValue, ':ip' => $ip, ':ip2' => $ip));
    $rows = $stmt->rowCount();

    // 2. Pour "never" : memoriser quel client a refuse
    //    -> detection automatique du changement de client au prochain check-in
    if ($surveyValue === -1) {
        $stmtClient = $pdo->prepare(
            "SELECT nom_client, prenom_client FROM t_chambre
             WHERE IP_LAN = :ip OR ip_chambre = :ip2
             LIMIT 1"
        );
        $stmtClient->execute(array(':ip' => $ip, ':ip2' => $ip));
        $client = $stmtClient->fetch(PDO::FETCH_ASSOC);

        $nom    = ($client && isset($client['nom_client']))    ? $client['nom_client']    : '';
        $prenom = ($client && isset($client['prenom_client'])) ? $client['prenom_client'] : '';

        $stmtTrack = $pdo->prepare(
            "INSERT INTO survey_guest_tracking
                 (ip_chambre, never_client_nom, never_client_prenom, set_at)
             VALUES (:ip, :nom, :prenom, NOW())
             ON DUPLICATE KEY UPDATE
                 never_client_nom    = VALUES(never_client_nom),
                 never_client_prenom = VALUES(never_client_prenom),
                 set_at              = NOW()"
        );
        $stmtTrack->execute(array(':ip' => $ip, ':nom' => $nom, ':prenom' => $prenom));

        // Effacer le cooldown "Plus tard" s'il existait
        $pdo->prepare(
            "UPDATE survey_guest_tracking SET later_until = NULL WHERE ip_chambre = :ip"
        )->execute(array(':ip' => $ip));
    }

    // 3. Pour "done" : nettoyer tout le tracking (survey complete normalement)
    if ($surveyValue === 1) {
        $pdo->prepare(
            "DELETE FROM survey_guest_tracking WHERE ip_chambre = :ip"
        )->execute(array(':ip' => $ip));
    }

    $pdo->commit();
    echo json_encode(array('success' => true, 'rows_updated' => $rows));

} catch (PDOException $e) {
    if ($pdo->inTransaction()) {
        $pdo->rollBack();
    }
    http_response_code(500);
    echo json_encode(array('success' => false, 'error' => $e->getMessage()));
}
