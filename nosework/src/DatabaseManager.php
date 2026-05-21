<?php

namespace NoseworkV2;

use PDO;
use PDOException;

class DatabaseManager
{
    private PDO $pdo;

    public function __construct(string $dbPath = null)
    {
	// Если путь не указан, используем абсолютный путь к корню сайта
   	 if ($dbPath === null) {
     	    $dbPath = __DIR__ . '/../nosework.db';
        }
        try {
            $this->pdo = new PDO("sqlite:$dbPath");
            $this->pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
            $this->createSchema();
        } catch (PDOException $e) {
            throw new PDOException("Database connection failed: " . $e->getMessage());
        }
    }

    private function createSchema(): void
    {
        // Таблица соревнований
        $this->pdo->exec(
            "CREATE TABLE IF NOT EXISTS competitions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                description TEXT NOT NULL,
                is_published INTEGER DEFAULT 0,
                start_date TEXT,
                end_date TEXT
            );"
        );

        $this->pdo->exec(
            "CREATE TABLE IF NOT EXISTS categories (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                competition_id INTEGER NOT NULL,
                name TEXT NOT NULL,
                time_limit REAL NOT NULL,
                hides_count INTEGER NOT NULL,
                max_score INTEGER NOT NULL,
                sort_order INTEGER DEFAULT 0,
                FOREIGN KEY (competition_id) REFERENCES competitions(id)
            );"
        );

        $this->pdo->exec(
            "CREATE TABLE IF NOT EXISTS penalty_rules (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                category_id INTEGER NOT NULL,
                name TEXT NOT NULL,
                type TEXT NOT NULL,
                points TEXT NOT NULL,
                sequence_index INTEGER NOT NULL,
                FOREIGN KEY (category_id) REFERENCES categories(id)
            );"
        );

        $this->pdo->exec(
            "CREATE TABLE IF NOT EXISTS standard_rule_templates (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                time_limit REAL NOT NULL,
                hides_count INTEGER NOT NULL,
                max_score INTEGER NOT NULL
            );"
        );

        $this->pdo->exec(
            "CREATE TABLE IF NOT EXISTS standard_template_penalty_rules (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                template_id INTEGER NOT NULL,
                name TEXT NOT NULL,
                type TEXT NOT NULL,
                points TEXT NOT NULL,
                sequence_index INTEGER NOT NULL,
                FOREIGN KEY (template_id) REFERENCES standard_rule_templates(id)
            );"
        );

        $this->pdo->exec(
            "CREATE TABLE IF NOT EXISTS results (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                category_id INTEGER NOT NULL,
                participant_id INTEGER,
                participant_name TEXT NOT NULL,
                time REAL NOT NULL,
                found_items INTEGER NOT NULL,
                penalty_counts TEXT NOT NULL,
                penalty_score INTEGER NOT NULL,
                total_score INTEGER NOT NULL,
                judge_comment TEXT,
                created_at TEXT NOT NULL,
                FOREIGN KEY (category_id) REFERENCES categories(id),
                FOREIGN KEY (participant_id) REFERENCES participants(id)
            );"
        );

        $this->pdo->exec(
            "CREATE TABLE IF NOT EXISTS deleted_results (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                result_id INTEGER NOT NULL,
                category_id INTEGER NOT NULL,
                participant_id INTEGER,
                participant_name TEXT NOT NULL,
                time REAL NOT NULL,
                found_items INTEGER NOT NULL,
                penalty_counts TEXT NOT NULL,
                penalty_score INTEGER NOT NULL,
                total_score INTEGER NOT NULL,
                judge_comment TEXT,
                deleted_at TEXT NOT NULL,
                deleted_by INTEGER NOT NULL,
                FOREIGN KEY (result_id) REFERENCES results(id),
                FOREIGN KEY (category_id) REFERENCES categories(id),
                FOREIGN KEY (participant_id) REFERENCES participants(id),
                FOREIGN KEY (deleted_by) REFERENCES users(id)
            );"
        );

        // Проверка и создание таблицы participants
        $expectedParticipantColumns = [
            'id', 'name', 'breed', 'nickname', 'gender', 'birth_date',
            'microchip_number', 'pedigree_number', 'qualification_book_number',
            'instructor_name', 'created_by'
        ];

        if ($this->hasTable('participants')) {
            $existingColumns = $this->getTableColumns('participants');
            $missingColumns = array_diff($expectedParticipantColumns, $existingColumns);
            if (!empty($missingColumns)) {
                $backupName = 'participants_old_' . time();
                $this->pdo->exec("ALTER TABLE participants RENAME TO $backupName;");
            }
        }

        $this->pdo->exec(
            "CREATE TABLE IF NOT EXISTS participants (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                breed TEXT,
                nickname TEXT,
                gender TEXT,
                birth_date TEXT,
                microchip_number TEXT,
                pedigree_number TEXT,
                qualification_book_number TEXT,
                instructor_name TEXT,
                created_by INTEGER,
                FOREIGN KEY (created_by) REFERENCES users(id)
            );"
        );

        $participantColumns = [
            'breed' => 'TEXT', 'nickname' => 'TEXT', 'gender' => 'TEXT',
            'birth_date' => 'TEXT', 'microchip_number' => 'TEXT',
            'pedigree_number' => 'TEXT', 'qualification_book_number' => 'TEXT',
            'instructor_name' => 'TEXT', 'created_by' => 'INTEGER'
        ];

        foreach ($participantColumns as $column => $type) {
            if (!$this->hasColumn('participants', $column)) {
                $this->pdo->exec("ALTER TABLE participants ADD COLUMN $column $type;");
            }
        }

        $this->pdo->exec(
            "CREATE TABLE IF NOT EXISTS competition_participants (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                competition_id INTEGER NOT NULL,
                participant_id INTEGER NOT NULL,
                sort_order INTEGER DEFAULT 0,
                FOREIGN KEY (competition_id) REFERENCES competitions(id),
                FOREIGN KEY (participant_id) REFERENCES participants(id),
                UNIQUE (competition_id, participant_id)
            );"
        );

        if (!$this->hasColumn('competition_participants', 'sort_order')) {
            $this->pdo->exec('ALTER TABLE competition_participants ADD COLUMN sort_order INTEGER DEFAULT 0;');
        }

        if (!$this->hasColumn('categories', 'sort_order')) {
            $this->pdo->exec('ALTER TABLE categories ADD COLUMN sort_order INTEGER DEFAULT 0;');
        }

        if (!$this->hasColumn('results', 'participant_id')) {
            $this->pdo->exec('ALTER TABLE results ADD COLUMN participant_id INTEGER;');
        }

        // Таблица пользователей
        $this->pdo->exec(
            "CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT NOT NULL UNIQUE,
                password_hash TEXT NOT NULL,
                role TEXT NOT NULL,
                competition_id INTEGER,
                display_name TEXT,
                FOREIGN KEY (competition_id) REFERENCES competitions(id)
            );"
        );

        // Таблица синхронизации
        $this->pdo->exec(
            "CREATE TABLE IF NOT EXISTS sync_mappings (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                entity_type TEXT NOT NULL,
                mobile_id INTEGER NOT NULL,
                server_id INTEGER NOT NULL,
                created_at TEXT NOT NULL,
                UNIQUE(entity_type, mobile_id)
            );"
        );

        if (!$this->hasColumn('users', 'display_name')) {
            $this->pdo->exec('ALTER TABLE users ADD COLUMN display_name TEXT;');
        }

        $this->pdo->exec(
            "CREATE TABLE IF NOT EXISTS user_competitions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL,
                competition_id INTEGER NOT NULL,
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                FOREIGN KEY (competition_id) REFERENCES competitions(id) ON DELETE CASCADE,
                UNIQUE (user_id, competition_id)
            );"
        );

        if ($this->hasColumn('users', 'competition_id')) {
            $this->pdo->exec(
                "INSERT OR IGNORE INTO user_competitions (user_id, competition_id)
                 SELECT id, competition_id FROM users WHERE competition_id IS NOT NULL AND competition_id > 0"
            );
        }

        // Создаём администратора по умолчанию
        $stmt = $this->pdo->query('SELECT COUNT(*) FROM users');
        if ((int)$stmt->fetchColumn() === 0) {
            $defaultAdminPassword = 'admin';
            $passwordHash = password_hash($defaultAdminPassword, PASSWORD_DEFAULT);
            $this->pdo->prepare('INSERT INTO users (username, password_hash, role) VALUES (?, ?, ?)')
                ->execute(['admin', $passwordHash, User::ROLE_ADMIN]);
        }
    }

    private function hasColumn(string $table, string $column): bool
    {
        $stmt = $this->pdo->query("PRAGMA table_info($table)");
        $columns = $stmt->fetchAll(PDO::FETCH_ASSOC);
        foreach ($columns as $columnInfo) {
            if ($columnInfo['name'] === $column) {
                return true;
            }
        }
        return false;
    }

    private function hasTable(string $table): bool
    {
        $stmt = $this->pdo->prepare("SELECT name FROM sqlite_master WHERE type='table' AND name = ?");
        $stmt->execute([$table]);
        return (bool)$stmt->fetchColumn();
    }

    private function getTableColumns(string $table): array
    {
        $stmt = $this->pdo->query("PRAGMA table_info($table)");
        $columns = $stmt->fetchAll(PDO::FETCH_ASSOC);
        return array_column($columns, 'name');
    }

    // ==================== ОСНОВНЫЕ МЕТОДЫ ====================

    public function insertCompetition(Competition $competition): int
    {
        $sql = "INSERT INTO competitions (name, description, start_date, end_date) VALUES (?, ?, ?, ?)";
        $stmt = $this->pdo->prepare($sql);
        $stmt->execute([
            $competition->getName(),
            $competition->getDescription(),
            $competition->getStartDate(),
            $competition->getEndDate()
        ]);
        return (int)$this->pdo->lastInsertId();
    }

    public function insertCategory(Category $category): int
    {
        $sql = "INSERT INTO categories (competition_id, name, time_limit, hides_count, max_score) VALUES (?, ?, ?, ?, ?)";
        $stmt = $this->pdo->prepare($sql);
        $stmt->execute([
            $category->getCompetitionId(),
            $category->getName(),
            $category->getTimeLimit(),
            $category->getHidesCount(),
            $category->getMaxScore(),
        ]);
        return (int)$this->pdo->lastInsertId();
    }

    public function insertPenaltyRule(PenaltyRule $rule): int
    {
        $sql = "INSERT INTO penalty_rules (category_id, name, type, points, sequence_index) VALUES (?, ?, ?, ?, ?)";
        $stmt = $this->pdo->prepare($sql);
        $stmt->execute([
            $rule->getCategoryId(),
            $rule->getName(),
            $rule->getType(),
            json_encode($rule->getPoints(), JSON_THROW_ON_ERROR),
            $rule->getSequence(),
        ]);
        $id = (int)$this->pdo->lastInsertId();
        $rule->setId($id);
        return $id;
    }

    public function insertResult(Result $result): int
    {
        $sql = "INSERT INTO results (category_id, participant_id, participant_name, time, found_items, penalty_counts, penalty_score, total_score, judge_comment, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        $stmt = $this->pdo->prepare($sql);
        $stmt->execute([
            $result->getCategoryId(),
            $result->getParticipantId(),
            $result->getParticipantName(),
            $result->getTime(),
            $result->getFoundItems(),
            json_encode($result->getPenaltyCounts(), JSON_THROW_ON_ERROR),
            $result->getPenaltyScore(),
            $result->getTotalScore(),
            $result->getJudgeComment(),
            (new \DateTimeImmutable())->format('Y-m-d H:i:s'),
        ]);
        return (int)$this->pdo->lastInsertId();
    }

    public function getCompetitions(): array
    {
        $stmt = $this->pdo->query('SELECT * FROM competitions ORDER BY id ASC');
        $rows = $stmt->fetchAll(PDO::FETCH_ASSOC);
        $competitions = [];
        foreach ($rows as $row) {
            $competitions[] = new Competition(
                $row['name'],
                $row['description'],
                (int)$row['id'],
                isset($row['is_published']) ? (bool)$row['is_published'] : false,
                $row['start_date'] ?? null,
                $row['end_date'] ?? null
            );
        }
        return $competitions;
    }

    public function getCompetitionById(int $competitionId): ?Competition
    {
        $stmt = $this->pdo->prepare('SELECT * FROM competitions WHERE id = ?');
        $stmt->execute([$competitionId]);
        $row = $stmt->fetch(PDO::FETCH_ASSOC);
        if (!$row) {
            return null;
        }
        return new Competition(
            $row['name'],
            $row['description'],
            (int)$row['id'],
            isset($row['is_published']) ? (bool)$row['is_published'] : false,
            $row['start_date'] ?? null,
            $row['end_date'] ?? null
        );
    }

    public function updateCompetition(Competition $competition): void
    {
        $sql = 'UPDATE competitions SET name = ?, description = ?, is_published = ?, start_date = ?, end_date = ? WHERE id = ?';
        $stmt = $this->pdo->prepare($sql);
        $stmt->execute([
            $competition->getName(),
            $competition->getDescription(),
            $competition->isPublished() ? 1 : 0,
            $competition->getStartDate(),
            $competition->getEndDate(),
            $competition->getId(),
        ]);
    }

    public function getCategoriesByCompetition(int $competitionId): array
    {
        $sql = 'SELECT * FROM categories WHERE competition_id = ? ORDER BY sort_order ASC, id ASC';
        $stmt = $this->pdo->prepare($sql);
        $stmt->execute([$competitionId]);
        $categories = [];
        foreach ($stmt->fetchAll(PDO::FETCH_ASSOC) as $row) {
            $category = new Category(
                (int)$row['competition_id'],
                $row['name'],
                (float)$row['time_limit'],
                (int)$row['hides_count'],
                (int)$row['max_score'],
                [],
                (int)$row['id']
            );
            $category->setPenaltyRules($this->getPenaltyRulesByCategory($category->getId()));
            $categories[] = $category;
        }
        return $categories;
    }

    public function getCategoryById(int $categoryId): ?Category
    {
        $sql = 'SELECT * FROM categories WHERE id = ?';
        $stmt = $this->pdo->prepare($sql);
        $stmt->execute([$categoryId]);
        $row = $stmt->fetch(PDO::FETCH_ASSOC);
        if (!$row) {
            return null;
        }
        $category = new Category(
            (int)$row['competition_id'],
            $row['name'],
            (float)$row['time_limit'],
            (int)$row['hides_count'],
            (int)$row['max_score'],
            [],
            (int)$row['id']
        );
        $category->setPenaltyRules($this->getPenaltyRulesByCategory($category->getId()));
        return $category;
    }

    public function getPenaltyRulesByCategory(int $categoryId): array
    {
        $sql = 'SELECT * FROM penalty_rules WHERE category_id = ? ORDER BY sequence_index ASC';
        $stmt = $this->pdo->prepare($sql);
        $stmt->execute([$categoryId]);
        $rules = [];
        foreach ($stmt->fetchAll(PDO::FETCH_ASSOC) as $row) {
            $rules[] = new PenaltyRule(
                (int)$row['category_id'],
                $row['name'],
                $row['type'],
                json_decode($row['points'], true, 512, JSON_THROW_ON_ERROR),
                (int)$row['sequence_index'],
                (int)$row['id']
            );
        }
        return $rules;
    }

    public function deletePenaltyRulesByCategory(int $categoryId): void
    {
        $sql = 'DELETE FROM penalty_rules WHERE category_id = ?';
        $stmt = $this->pdo->prepare($sql);
        $stmt->execute([$categoryId]);
    }

    public function insertParticipant(Participant $participant, ?int $createdBy = null): int
    {
        $sql = 'INSERT INTO participants (name, breed, nickname, gender, birth_date, microchip_number, pedigree_number, qualification_book_number, instructor_name, created_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)';
        $stmt = $this->pdo->prepare($sql);
        $stmt->execute([
            $participant->getName(),
            $participant->getBreed(),
            $participant->getNickname(),
            $participant->getGender(),
            $participant->getBirthDate(),
            $participant->getMicrochipNumber(),
            $participant->getPedigreeNumber(),
            $participant->getQualificationBookNumber(),
            $participant->getInstructorName(),
            $createdBy,
        ]);
        $id = (int)$this->pdo->lastInsertId();
        $participant->setId($id);
        return $id;
    }

    public function getParticipantById(int $participantId): ?Participant
    {
        $stmt = $this->pdo->prepare('SELECT * FROM participants WHERE id = ?');
        $stmt->execute([$participantId]);
        $row = $stmt->fetch(PDO::FETCH_ASSOC);
        if (!$row) {
            return null;
        }
        return new Participant(
            $row['name'],
            $row['breed'] ?? null,
            $row['nickname'] ?? null,
            $row['gender'] ?? null,
            $row['birth_date'] ?? null,
            $row['microchip_number'] ?? null,
            $row['pedigree_number'] ?? null,
            $row['qualification_book_number'] ?? null,
            $row['instructor_name'] ?? null,
            (int)$row['id']
        );
    }

    public function getParticipantsByCompetition(int $competitionId): array
    {
        $sql = 'SELECT p.*, cp.sort_order FROM participants p JOIN competition_participants cp ON cp.participant_id = p.id WHERE cp.competition_id = ? ORDER BY cp.sort_order ASC, p.name ASC';
        $stmt = $this->pdo->prepare($sql);
        $stmt->execute([$competitionId]);
        $participants = [];
        foreach ($stmt->fetchAll(PDO::FETCH_ASSOC) as $row) {
            $participant = new Participant(
                $row['name'],
                $row['breed'] ?? null,
                $row['nickname'] ?? null,
                $row['gender'] ?? null,
                $row['birth_date'] ?? null,
                $row['microchip_number'] ?? null,
                $row['pedigree_number'] ?? null,
                $row['qualification_book_number'] ?? null,
                $row['instructor_name'] ?? null,
                (int)$row['id']
            );
            $participant->setSortOrder((int)($row['sort_order'] ?? 0));
            $participants[] = $participant;
        }
        return $participants;
    }

    public function insertCompetitionParticipant(int $competitionId, int $participantId): void
    {
        $sql = 'SELECT COALESCE(MAX(sort_order), 0) as max_order FROM competition_participants WHERE competition_id = ?';
        $stmt = $this->pdo->prepare($sql);
        $stmt->execute([$competitionId]);
        $maxOrder = (int)$stmt->fetchColumn();
        $sql = 'INSERT OR IGNORE INTO competition_participants (competition_id, participant_id, sort_order) VALUES (?, ?, ?)';
        $stmt = $this->pdo->prepare($sql);
        $stmt->execute([$competitionId, $participantId, $maxOrder + 1]);
    }

    public function getResultByParticipantAndCategory(int $categoryId, int $participantId): ?Result
    {
        $sql = 'SELECT * FROM results WHERE category_id = ? AND participant_id = ? LIMIT 1';
        $stmt = $this->pdo->prepare($sql);
        $stmt->execute([$categoryId, $participantId]);
        $row = $stmt->fetch(PDO::FETCH_ASSOC);
        if (!$row) {
            return null;
        }
        return new Result(
            (int)$row['category_id'],
            $row['participant_name'],
            (int)$row['participant_id'] ?: null,
            (float)$row['time'],
            (int)$row['found_items'],
            json_decode($row['penalty_counts'], true, 512, JSON_THROW_ON_ERROR),
            (int)$row['penalty_score'],
            (int)$row['total_score'],
            (int)$row['id'],
            $row['judge_comment'] ?? null
        );
    }

    public function getResultsByCategory(int $categoryId): array
    {
        $sql = 'SELECT * FROM results WHERE category_id = ? ORDER BY total_score DESC, time ASC';
        $stmt = $this->pdo->prepare($sql);
        $stmt->execute([$categoryId]);
        $results = [];
        foreach ($stmt->fetchAll(PDO::FETCH_ASSOC) as $row) {
            $participantId = $row['participant_id'] !== null && $row['participant_id'] !== '' ? (int)$row['participant_id'] : null;
            $results[] = new Result(
                (int)$row['category_id'],
                $row['participant_name'],
                $participantId,
                (float)$row['time'],
                (int)$row['found_items'],
                json_decode($row['penalty_counts'], true, 512, JSON_THROW_ON_ERROR),
                (int)$row['penalty_score'],
                (int)$row['total_score'],
                (int)$row['id'],
                $row['judge_comment'] ?? null
            );
        }
        return $results;
    }

    public function addResult(Result $result): int
    {
        return $this->insertResult($result);
    }

    public function clearResultsByCategory(int $categoryId): void
    {
        $sql = 'DELETE FROM results WHERE category_id = ?';
        $stmt = $this->pdo->prepare($sql);
        $stmt->execute([$categoryId]);
    }

    public function deleteCategoryById(int $categoryId): void
    {
        $this->clearResultsByCategory($categoryId);
        $this->deletePenaltyRulesByCategory($categoryId);
        $sql = 'DELETE FROM categories WHERE id = ?';
        $stmt = $this->pdo->prepare($sql);
        $stmt->execute([$categoryId]);
    }

    // ==================== МЕТОДЫ ДЛЯ ПОЛЬЗОВАТЕЛЕЙ И СИНХРОНИЗАЦИИ ====================

    public function getUserByUsername(string $username): ?User
    {
        $sql = 'SELECT * FROM users WHERE username = ? LIMIT 1';
        $stmt = $this->pdo->prepare($sql);
        $stmt->execute([$username]);
        $row = $stmt->fetch(PDO::FETCH_ASSOC);
        if (!$row) {
            return null;
        }
        return new User(
            $row['username'],
            $row['password_hash'],
            $row['role'],
            $row['competition_id'] ?? null,
            (int)$row['id'],
            $row['display_name'] ?? null
        );
    }

    public function getUserById(int $userId): ?User
    {
        $sql = 'SELECT * FROM users WHERE id = ? LIMIT 1';
        $stmt = $this->pdo->prepare($sql);
        $stmt->execute([$userId]);
        $row = $stmt->fetch(PDO::FETCH_ASSOC);
        if (!$row) {
            return null;
        }
        return new User(
            $row['username'],
            $row['password_hash'],
            $row['role'],
            $row['competition_id'] ?? null,
            (int)$row['id'],
            $row['display_name'] ?? null
        );
    }

    public function getAllUsers(): array
    {
        $sql = 'SELECT * FROM users ORDER BY id ASC';
        $stmt = $this->pdo->query($sql);
        $users = [];
        foreach ($stmt->fetchAll(PDO::FETCH_ASSOC) as $row) {
            $users[] = new User(
                $row['username'],
                $row['password_hash'],
                $row['role'],
                $row['competition_id'] ?? null,
                (int)$row['id'],
                $row['display_name'] ?? null
            );
        }
        return $users;
    }

    public function updateUser(User $user): void
    {
        $sql = 'UPDATE users SET username = ?, password_hash = ?, role = ?, competition_id = ?, display_name = ? WHERE id = ?';
        $stmt = $this->pdo->prepare($sql);
        $stmt->execute([
            $user->getUsername(),
            $user->getPasswordHash(),
            $user->getRole(),
            $user->getCompetitionId(),
            $user->getDisplayName(),
            $user->getId(),
        ]);
    }

    public function insertUser(User $user): int
    {
        $sql = "INSERT INTO users (username, password_hash, role, competition_id, display_name) VALUES (?, ?, ?, ?, ?)";
        $stmt = $this->pdo->prepare($sql);
        $stmt->execute([
            $user->getUsername(),
            $user->getPasswordHash(),
            $user->getRole(),
            $user->getCompetitionId(),
            $user->getDisplayName(),
        ]);
        $id = (int)$this->pdo->lastInsertId();
        $user->setId($id);
        return $id;
    }

    public function deleteUser(int $userId): void
    {
        $sql = 'DELETE FROM users WHERE id = ?';
        $stmt = $this->pdo->prepare($sql);
        $stmt->execute([$userId]);
    }

    public function getOverallResults(int $competitionId): array
    {
        $sql = '
            SELECT 
                r.participant_id,
                r.participant_name,
                SUM(r.time) AS total_time,
                SUM(r.total_score) AS total_score
            FROM results r
            INNER JOIN categories c ON r.category_id = c.id
            WHERE c.competition_id = ?
            GROUP BY r.participant_id, r.participant_name
            ORDER BY total_score DESC, total_time ASC
        ';
        $stmt = $this->pdo->prepare($sql);
        $stmt->execute([$competitionId]);
        $results = [];
        foreach ($stmt->fetchAll(PDO::FETCH_ASSOC) as $row) {
            $results[] = [
                'participant_id' => (int)$row['participant_id'],
                'participant_name' => $row['participant_name'],
                'total_time' => (float)$row['total_time'],
                'total_score' => (int)$row['total_score'],
            ];
        }
        return $results;
    }

    public function getQualificationResults(int $competitionId): array
    {
        return [];
    }

    public function getParticipants(?int $userId = null, string $role = 'admin'): array
    {
        if ($role === User::ROLE_ADMIN) {
            $stmt = $this->pdo->query('SELECT * FROM participants ORDER BY name ASC');
        } else {
            $stmt = $this->pdo->prepare('SELECT * FROM participants WHERE created_by = ? ORDER BY name ASC');
            $stmt->execute([$userId]);
        }
        $participants = [];
        foreach ($stmt->fetchAll(PDO::FETCH_ASSOC) as $row) {
            $participants[] = new Participant(
                $row['name'],
                $row['breed'] ?? null,
                $row['nickname'] ?? null,
                $row['gender'] ?? null,
                $row['birth_date'] ?? null,
                $row['microchip_number'] ?? null,
                $row['pedigree_number'] ?? null,
                $row['qualification_book_number'] ?? null,
                $row['instructor_name'] ?? null,
                (int)$row['id']
            );
        }
        return $participants;
    }

    public function setCompetitionPublished(int $competitionId, bool $isPublished): void
    {
        $sql = 'UPDATE competitions SET is_published = ? WHERE id = ?';
        $stmt = $this->pdo->prepare($sql);
        $stmt->execute([$isPublished ? 1 : 0, $competitionId]);
    }

    public function getDeletedResults(int $competitionId): array
    {
        return [];
    }

    public function getCategoriesByIds(array $categoryIds): array
    {
        if (empty($categoryIds)) {
            return [];
        }
        $placeholders = implode(',', array_fill(0, count($categoryIds), '?'));
        $sql = "SELECT * FROM categories WHERE id IN ($placeholders)";
        $stmt = $this->pdo->prepare($sql);
        $stmt->execute($categoryIds);
        $categories = [];
        foreach ($stmt->fetchAll(PDO::FETCH_ASSOC) as $row) {
            $categories[] = new Category(
                (int)$row['competition_id'],
                $row['name'],
                (float)$row['time_limit'],
                (int)$row['hides_count'],
                (int)$row['max_score'],
                [],
                (int)$row['id']
            );
        }
        return $categories;
    }

    public function updateCategory(Category $category): void
    {
        $sql = 'UPDATE categories SET name = ?, time_limit = ?, hides_count = ?, max_score = ? WHERE id = ?';
        $stmt = $this->pdo->prepare($sql);
        $stmt->execute([
            $category->getName(),
            $category->getTimeLimit(),
            $category->getHidesCount(),
            $category->getMaxScore(),
            $category->getId(),
        ]);
    }

    public function getStandardRuleTemplates(): array
    {
        return [];
    }

    public function getStandardRuleTemplateById(int $templateId): ?StandardRuleTemplate
    {
        return null;
    }

    public function getUsersByCompetition(int $competitionId, ?string $role = null): array
    {
        return [];
    }

    public function deleteCompetitionParticipant(int $competitionId, int $participantId): void
    {
        $sql = 'DELETE FROM competition_participants WHERE competition_id = ? AND participant_id = ?';
        $stmt = $this->pdo->prepare($sql);
        $stmt->execute([$competitionId, $participantId]);
    }

    public function updateParticipantSortOrder(int $competitionId, array $participantIds): void
    {
        $sql = 'UPDATE competition_participants SET sort_order = ? WHERE competition_id = ? AND participant_id = ?';
        $stmt = $this->pdo->prepare($sql);
        foreach ($participantIds as $order => $participantId) {
            $stmt->execute([$order + 1, $competitionId, $participantId]);
        }
    }

    public function updateCategorySortOrder(int $competitionId, array $categoryIds): void
    {
        $sql = 'UPDATE categories SET sort_order = ? WHERE competition_id = ? AND id = ?';
        $stmt = $this->pdo->prepare($sql);
        foreach ($categoryIds as $order => $categoryId) {
            $stmt->execute([$order + 1, $competitionId, $categoryId]);
        }
    }

    public function saveUserCompetitions(int $userId, array $competitionIds): void
    {
        $this->pdo->prepare('DELETE FROM user_competitions WHERE user_id = ?')->execute([$userId]);
        if (!empty($competitionIds)) {
            $stmt = $this->pdo->prepare('INSERT INTO user_competitions (user_id, competition_id) VALUES (?, ?)');
            foreach ($competitionIds as $competitionId) {
                $stmt->execute([$userId, $competitionId]);
            }
        }
    }

    public function getJudgesByCompetition(int $competitionId): array
    {
        return [];
    }

    public function getSecretaryByCompetition(int $competitionId): ?User
    {
        return null;
    }

    public function getAllJudges(): array
    {
        return [];
    }

    public function getAllSecretaries(): array
    {
        return [];
    }

    public function updateCompetitionStaff(int $competitionId, array $judgeIds, ?int $secretaryId): void
    {
        // Заглушка
    }

    public function deleteCompetition(int $competitionId): bool
    {
        try {
            $this->pdo->beginTransaction();
            $sql = 'DELETE FROM competitions WHERE id = ?';
            $stmt = $this->pdo->prepare($sql);
            $stmt->execute([$competitionId]);
            $this->pdo->commit();
            return true;
        } catch (\Exception $e) {
            $this->pdo->rollBack();
            throw $e;
        }
    }
}