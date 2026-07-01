<?php
/**
 * api/guest.php — Identification du client par IP LAN de la TV (PHP 5.6)
 *
 * GET /api_survey/api/guest.php?ip=192.168.0.189
 *
 * Reponse (found) :
 * {
 *   "found":        true,
 *   "ip_chambre":   "192.168.0.189",
 *   "nom_client":   "DUPONT",
 *   "prenom_client":"Jean",
 *   "num_chambre":  "204",
 *   "langue":       "FR",
 *   "survey":       0,
 *   "later_active": false
 * }
 *
 * Reponse (not found) : { "found": false }
 */

require_once dirname(__FILE__) . '/../config.php';

// Validation
$ip = isset($_GET['ip']) ? trim($_GET['ip']) : '';

if ($ip === '') {
    http_response_code(400);
    echo json_encode(array('error' => 'Parametre ip manquant'));
    exit;
}

// Requete principale
$pdo  = getDb();
$stmt = $pdo->prepare(
    "SELECT nom_client, prenom_client, num_chambre, langue, survey
     FROM   t_chambre
     WHERE  IP_LAN = :ip OR ip_chambre = :ip2
     LIMIT  1"
);
$stmt->execute(array(':ip' => $ip, ':ip2' => $ip));
$row = $stmt->fetch(PDO::FETCH_ASSOC);

if (!$row) {
    echo json_encode(array('found' => false));
    exit;
}

$langue       = (!empty($row['langue'])) ? strtoupper(trim($row['langue'])) : 'FR';
$survey       = isset($row['survey']) ? (int) $row['survey'] : 0;
$nomClient    = isset($row['nom_client'])    ? $row['nom_client']    : '';
$prenomClient = isset($row['prenom_client']) ? $row['prenom_client'] : '';
$laterActive  = false;

// Cooldown "Plus tard" cote serveur
// Si le client a clique "Plus tard" il y a moins de 2h -> later_active = true
// L'app fermera silencieusement sans reproposer le sondage.
if ($survey === 0) {
    $stmtLater = $pdo->prepare(
        "SELECT later_until FROM survey_guest_tracking
         WHERE ip_chambre = :ip AND later_until > NOW()"
    );
    $stmtLater->execute(array(':ip' => $ip));
    if ($stmtLater->fetch()) {
        $laterActive = true;
    }
}

// Detection de changement de client (reset "Jamais")
// Si survey=-1 mais que le nom du client a change depuis le refus,
// c'est un nouveau client : on remet survey=0 automatiquement.
if ($survey === -1) {
    $stmtTrack = $pdo->prepare(
        "SELECT never_client_nom, never_client_prenom
         FROM   survey_guest_tracking
         WHERE  ip_chambre = :ip"
    );
    $stmtTrack->execute(array(':ip' => $ip));
    $track = $stmtTrack->fetch(PDO::FETCH_ASSOC);

    if ($track) {
        $nomChange    = (trim($track['never_client_nom'])    !== trim($nomClient));
        $prenomChange = (trim($track['never_client_prenom']) !== trim($prenomClient));

        if ($nomChange || $prenomChange) {
            // Nouveau client -> reset survey=0 + supprimer le tracking
            $pdo->prepare(
                "UPDATE t_chambre SET survey = 0
                 WHERE IP_LAN = :ip OR ip_chambre = :ip2"
            )->execute(array(':ip' => $ip, ':ip2' => $ip));

            $pdo->prepare(
                "DELETE FROM survey_guest_tracking WHERE ip_chambre = :ip"
            )->execute(array(':ip' => $ip));

            $survey = 0;
        }
    }
}

echo json_encode(array(
    'found'         => true,
    'ip_chambre'    => $ip,
    'nom_client'    => $nomClient,
    'prenom_client' => $prenomClient,
    'num_chambre'   => isset($row['num_chambre']) ? $row['num_chambre'] : '',
    'langue'        => $langue,
    'survey'        => $survey,
    'later_active'  => $laterActive,
));
