<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, GET, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

require_once '../vendor/autoload.php';
use NoseworkV2\CompetitionService;
use NoseworkV2\DatabaseManager;

function sendResponse($success, $data = [], $message = '') {
    echo json_encode([
        'success' => $success,
        'data' => $data,
        'message' => $message
    ], JSON_UNESCAPED_UNICODE);
    exit();
}

$dbPath = __DIR__ . '/../nosework.db';

try {
    $dbManager = new DatabaseManager($dbPath);
    $service = new CompetitionService($dbManager);
} catch (Exception $e) {
    sendResponse(false, [], 'Ошибка БД: ' . $e->getMessage());
}

$action = isset($_GET['action']) ? $_GET['action'] : '';

switch ($action) {
    case 'get_competitions':
        getCompetitions($service);
        break;
    case 'get_categories':
        getCategories($service);
        break;
    case 'get_participants':
        getParticipants($service);
        break;
    case 'get_all':
        getAllData($service);
        break;
    default:
        sendResponse(false, [], 'Неизвестное действие');
        break;
}

function getCompetitions($service) {
    $competitions = $service->getCompetitions();
    $result = [];
    
    foreach ($competitions as $comp) {
        $result[] = [
            'id' => $comp->getId(),
            'name' => $comp->getName(),
            'description' => $comp->getDescription(),
            'start_date' => $comp->getStartDate(),
            'end_date' => $comp->getEndDate(),
            'is_published' => $comp->isPublished()
        ];
    }
    
    sendResponse(true, $result);
}

function getCategories($service) {
    $competitionId = isset($_GET['competition_id']) ? intval($_GET['competition_id']) : null;
    
    if (!$competitionId) {
        sendResponse(false, [], 'Не указан ID соревнования');
    }
    
    $categories = $service->getCategoriesByCompetition($competitionId);
    $result = [];
    
    foreach ($categories as $cat) {
        $rules = [];
        foreach ($cat->getPenaltyRules() as $rule) {
            $rules[] = [
                'id' => $rule->getId(),
                'name' => $rule->getName(),
                'type' => $rule->getType(),
                'points' => $rule->getPoints()
            ];
        }
        
        $result[] = [
            'id' => $cat->getId(),
            'name' => $cat->getName(),
            'time_limit' => $cat->getTimeLimit(),
            'hides_count' => $cat->getHidesCount(),
            'max_score' => $cat->getMaxScore(),
            'penalty_rules' => $rules
        ];
    }
    
    sendResponse(true, $result);
}

function getParticipants($service) {
    $competitionId = isset($_GET['competition_id']) ? intval($_GET['competition_id']) : null;
    
    if (!$competitionId) {
        sendResponse(false, [], 'Не указан ID соревнования');
    }
    
    $participants = $service->getParticipantsByCompetition($competitionId);
    $result = [];
    
    foreach ($participants as $part) {
        $result[] = [
            'id' => $part->getId(),
            'name' => $part->getName(),
            'nickname' => $part->getNickname(),
            'breed' => $part->getBreed(),
            'gender' => $part->getGender(),
            'birth_date' => $part->getBirthDate(),
            'microchip_number' => $part->getMicrochipNumber(),
            'pedigree_number' => $part->getPedigreeNumber(),
            'qualification_book_number' => $part->getQualificationBookNumber(),
            'instructor_name' => $part->getInstructorName()
        ];
    }
    
    sendResponse(true, $result);
}

function getAllData($service) {
    $competitions = $service->getCompetitions();
    $result = [
        'competitions' => [],
        'categories' => [],
        'participants' => []
    ];
    
    foreach ($competitions as $comp) {
        $result['competitions'][] = [
            'id' => $comp->getId(),
            'name' => $comp->getName(),
            'description' => $comp->getDescription(),
            'start_date' => $comp->getStartDate(),
            'end_date' => $comp->getEndDate()
        ];
        
        $categories = $service->getCategoriesByCompetition($comp->getId());
        foreach ($categories as $cat) {
            $rules = [];
            foreach ($cat->getPenaltyRules() as $rule) {
                $rules[] = [
                    'name' => $rule->getName(),
                    'type' => $rule->getType(),
                    'points' => $rule->getPoints()
                ];
            }
            
            $result['categories'][] = [
                'competition_id' => $comp->getId(),
                'name' => $cat->getName(),
                'time_limit' => $cat->getTimeLimit(),
                'hides_count' => $cat->getHidesCount(),
                'max_score' => $cat->getMaxScore(),
                'penalty_rules' => $rules
            ];
        }
        
        $participants = $service->getParticipantsByCompetition($comp->getId());
        foreach ($participants as $part) {
            $result['participants'][] = [
                'competition_id' => $comp->getId(),
                'name' => $part->getName(),
                'nickname' => $part->getNickname(),
                'breed' => $part->getBreed()
            ];
        }
    }
    
    sendResponse(true, $result);
}
?>