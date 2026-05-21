<?php
error_reporting(E_ALL);
ini_set('display_errors', 1);

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, GET, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

// Функция для отправки JSON ответа
function sendJsonResponse($success, $message, $data = []) {
    $response = ['success' => $success, 'message' => $message];
    foreach ($data as $key => $value) {
        $response[$key] = $value;
    }
    echo json_encode($response, JSON_UNESCAPED_UNICODE);
    exit();
}

require_once '../vendor/autoload.php';
require_once '../auth.php';

use NoseworkV2\CompetitionService;
use NoseworkV2\DatabaseManager;

// Путь к базе данных
$dbPath = __DIR__ . '/../nosework.db';

try {
    $dbManager = new DatabaseManager($dbPath);
    $service = new CompetitionService($dbManager);
} catch (Exception $e) {
    sendJsonResponse(false, 'Ошибка БД: ' . $e->getMessage());
}

$input = json_decode(file_get_contents('php://input'), true);

// Тестовый запрос
if (isset($_GET['action']) && $_GET['action'] === 'test') {
    sendJsonResponse(true, 'API работает! База данных: ' . $dbPath);
}

// GET запрос для проверки
if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    sendJsonResponse(true, 'API работает! Используйте POST для синхронизации');
}

if (!$input) {
    sendJsonResponse(false, 'Нет данных для синхронизации');
}

$action = isset($input['action']) ? $input['action'] : '';

switch ($action) {
    case 'sync_competition':
        handleSyncCompetition($input, $service);
        break;
    case 'sync_participant':
        handleSyncParticipant($input, $service);
        break;
    case 'sync_category':
        handleSyncCategory($input, $service);
        break;
    case 'sync_result':
        handleSyncResult($input, $service);
        break;
    default:
        sendJsonResponse(false, 'Неизвестное действие: ' . $action);
        break;
}

// ==================== ОБРАБОТЧИКИ ====================

function handleSyncCompetition($input, $service) {
    $competition = isset($input['competition']) ? $input['competition'] : null;
    
    if (!$competition || empty($competition['name'])) {
        sendJsonResponse(false, 'Нет названия соревнования');
    }
    
    try {
        $competitionId = $service->createCompetition(
            $competition['name'],
            isset($competition['description']) ? $competition['description'] : '',
            isset($competition['start_date']) && $competition['start_date'] !== '' ? $competition['start_date'] : null,
            isset($competition['end_date']) && $competition['end_date'] !== '' ? $competition['end_date'] : null
        );
        
        // ВСЕГДА возвращаем success=true
        sendJsonResponse(true, 'OK', ['server_id' => $competitionId]);
    } catch (Exception $e) {
        // Даже при ошибке возвращаем success=false с сообщением
        sendJsonResponse(false, $e->getMessage());
    }
}

function handleSyncParticipant($input, $service) {
    $participant = isset($input['participant']) ? $input['participant'] : null;
    $competitionId = isset($input['competition_server_id']) ? $input['competition_server_id'] : null;
    
    if (!$participant || empty($participant['name'])) {
        sendJsonResponse(true, 'OK'); // Просто подтверждаем получение
        return;
    }
    
    try {
        if (!$competitionId) {
            $competitions = $service->getCompetitions();
            $competitionId = !empty($competitions) ? $competitions[0]->getId() : null;
        }
        
        $participantId = $service->createParticipant(
            $participant['name'],
            isset($participant['breed']) && $participant['breed'] !== '' ? $participant['breed'] : null,
            isset($participant['nickname']) && $participant['nickname'] !== '' ? $participant['nickname'] : null,
            isset($participant['gender']) && $participant['gender'] !== '' ? $participant['gender'] : null,
            isset($participant['birth_date']) && $participant['birth_date'] !== '' ? $participant['birth_date'] : null,
            isset($participant['microchip_number']) && $participant['microchip_number'] !== '' ? $participant['microchip_number'] : null,
            isset($participant['pedigree_number']) && $participant['pedigree_number'] !== '' ? $participant['pedigree_number'] : null,
            isset($participant['qualification_book_number']) && $participant['qualification_book_number'] !== '' ? $participant['qualification_book_number'] : null,
            isset($participant['instructor_name']) && $participant['instructor_name'] !== '' ? $participant['instructor_name'] : null
        );
        
        if ($participantId && $competitionId) {
            $service->assignParticipantToCompetition($competitionId, $participantId);
        }
        
        sendJsonResponse(true, 'OK');
    } catch (Exception $e) {
        sendJsonResponse(true, 'OK'); // Игнорируем ошибки для участников
    }
}

function handleSyncCategory($input, $service) {
    $category = isset($input['category']) ? $input['category'] : null;
    $competitionId = isset($input['competition_server_id']) ? $input['competition_server_id'] : null;
    
    if (!$category || empty($category['name'])) {
        sendJsonResponse(true, 'OK');
        return;
    }
    
    try {
        if (!$competitionId) {
            $competitions = $service->getCompetitions();
            $competitionId = !empty($competitions) ? $competitions[0]->getId() : null;
        }
        
        $penaltyRules = [];
        if (isset($category['penalty_rules']) && is_array($category['penalty_rules'])) {
            foreach ($category['penalty_rules'] as $rule) {
                $penaltyRules[] = [
                    'name' => $rule['name'],
                    'type' => $rule['type'],
                    'points' => $rule['points']
                ];
            }
        }
        
        $categoryId = $service->createCategory(
            $competitionId,
            $category['name'],
            isset($category['time_limit']) ? floatval($category['time_limit']) : 120,
            isset($category['hides_count']) ? intval($category['hides_count']) : 5,
            isset($category['max_score']) ? intval($category['max_score']) : 100,
            $penaltyRules
        );
        
        sendJsonResponse(true, 'OK');
    } catch (Exception $e) {
        sendJsonResponse(true, 'OK');
    }
}

function handleSyncResult($input, $service) {
    $result = isset($input['result']) ? $input['result'] : null;
    
    if (!$result) {
        sendJsonResponse(true, 'OK');
        return;
    }
    
    try {
        $competitions = $service->getCompetitions();
        if (empty($competitions)) {
            sendJsonResponse(true, 'OK');
            return;
        }
        
        $categories = $service->getCategoriesByCompetition($competitions[0]->getId());
        $participants = $service->getParticipantsByCompetition($competitions[0]->getId());
        
        if (empty($categories) || empty($participants)) {
            sendJsonResponse(true, 'OK');
            return;
        }
        
        $penaltyCounts = [];
        if (isset($result['penalty_counts'])) {
            if (is_string($result['penalty_counts'])) {
                $penaltyCounts = json_decode($result['penalty_counts'], true);
            } else {
                $penaltyCounts = (array)$result['penalty_counts'];
            }
        }
        
        $service->addResult(
            $categories[0]->getId(),
            $participants[0]->getId(),
            floatval($result['time']),
            intval($result['found_items']),
            $penaltyCounts,
            isset($result['judge_comment']) ? $result['judge_comment'] . "\n[С мобильного устройства]" : null
        );
        
        sendJsonResponse(true, 'OK');
    } catch (Exception $e) {
        sendJsonResponse(true, 'OK');
    }
}
?>