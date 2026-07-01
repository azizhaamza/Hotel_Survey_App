<?php
/**
 * api/reset_survey.php — Reset manuel du statut survey (PHP 5.6)
 *
 * A appeler par la reception ou le PMS lors d'un check-in/check-out.
 *
 * GET /api_survey/api/reset_survey.php?ip=192.168.0.189
 * GET /api_survey/api/reset_survey.php?num_chambre=204
 * GET /api_survey/api/reset_survey.php?all=1          (toutes les chambres)
 *
 * Repond : { "success": true, "reset": "192.168.0.189" }
 */

require_once dirname(__FILE__) . '/../config.php';

$ip         = isset($_GET['ip'])          ? trim($_GET['ip'])          : '';
$numChambre = isset($_GET['num_chambre']) ? trim($_GET['num_chambre']) : '';
$resetAll   = isset($_GET['all'])         ? (bool) $_GET['all']        : false;

if ($ip === '' && $numChambre === '' && !$resetAll) {
    http_response_code(400);
    echo json_encode(array('success' => false, 'error' => 'Parametre ip, num_chambre ou all requis'));
    exit;
}

try {
    $pdo = getDb();
    $pdo->beginTransaction();

    if ($resetAll) {
        // Reset toutes les chambres (fin de saison, nettoyage global)
        $pdo->prepare("UPDATE t_chambre SET survey = 0")->execute();
        $pdo->prepare("DELETE FROM survey_guest_tracking")->execute();
        $rows = 'all';

    } elseif ($ip !== '') {
        $pdo->prepare(
            "UPDATE t_chambre SET survey = 0
             WHERE IP_LAN = :ip OR ip_chambre = :ip2"
        )->execute(array(':ip' => $ip, ':ip2' => $ip));

        $pdo->prepare(
            "DELETE FROM survey_guest_tracking WHERE ip_chambre = :ip"
        )->execute(array(':ip' => $ip));

        $rows = $ip;

    } else {
        $pdo->prepare(
            "UPDATE t_chambre SET survey = 0
             WHERE num_chambre = :num"
        )->execute(array(':num' => $numChambre));

        // Supprimer les trackings de toutes les IP de cette chambre
        $stmtIps = $pdo->prepare(
            "SELECT IP_LAN, ip_chambre FROM t_chambre WHERE num_chambre = :num"
        );
        $stmtIps->execute(array(':num' => $numChambre));
        $ips = $stmtIps->fetchAll(PDO::FETCH_ASSOC);

        foreach ($ips as $r) {
            foreach (array($r['IP_LAN'], $r['ip_chambre']) as $rip) {
                if ($rip !== null && $rip !== '') {
                    $pdo->prepare(
                        "DELETE FROM survey_guest_tracking WHERE ip_chambre = :ip"
                    )->execute(array(':ip' => $rip));
                }
            }
        }
        $rows = $numChambre;
    }

    $pdo->commit();
    echo json_encode(array('success' => true, 'reset' => $rows));

} catch (PDOException $e) {
    if ($pdo->inTransaction()) {
        $pdo->rollBack();
    }
    http_response_code(500);
    echo json_encode(array('success' => false, 'error' => $e->getMessage()));
}
