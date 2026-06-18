package com.jimu.app.data.backup

import org.json.JSONArray
import org.json.JSONObject

object BackupJsonCodec {

    fun encode(payload: AppBackupPayloadV1): String {
        return JSONObject()
            .put("backupVersion", payload.backupVersion)
            .put(
                "meta",
                JSONObject()
                    .put("exportedAt", payload.meta.exportedAt)
                    .put("appPackage", payload.meta.appPackage)
                    .put("appVersionName", payload.meta.appVersionName)
                    .put("appVersionCode", payload.meta.appVersionCode)
            )
            .put("tasks", payload.tasks.toJsonArray(::taskToJson))
            .put("habits", payload.habits.toJsonArray(::habitToJson))
            .put("habitRecords", payload.habitRecords.toJsonArray(::habitRecordToJson))
            .put("goals", payload.goals.toJsonArray(::goalToJson))
            .put("goalSteps", payload.goalSteps.toJsonArray(::goalStepToJson))
            .put("reviews", payload.reviews.toJsonArray(::reviewToJson))
            .toString()
    }

    fun decode(json: String): BackupDecodeResult {
        return try {
            val root = JSONObject(json)
            if (!root.has("backupVersion")) {
                return BackupDecodeResult.InvalidFormat("缺少 backupVersion")
            }

            val version = root.getInt("backupVersion")
            if (version != BACKUP_VERSION) {
                return BackupDecodeResult.UnsupportedVersion(version)
            }

            val metaJson = root.getJSONObject("meta")
            val payload = AppBackupPayloadV1(
                backupVersion = version,
                meta = BackupMetaV1(
                    exportedAt = metaJson.getLong("exportedAt"),
                    appPackage = metaJson.getString("appPackage"),
                    appVersionName = metaJson.getString("appVersionName"),
                    appVersionCode = metaJson.getLong("appVersionCode")
                ),
                tasks = root.getJSONArray("tasks").mapObjects(::taskFromJson),
                habits = root.getJSONArray("habits").mapObjects(::habitFromJson),
                habitRecords = root.getJSONArray("habitRecords").mapObjects(::habitRecordFromJson),
                goals = root.getJSONArray("goals").mapObjects(::goalFromJson),
                goalSteps = root.getJSONArray("goalSteps").mapObjects(::goalStepFromJson),
                reviews = root.getJSONArray("reviews").mapObjects(::reviewFromJson)
            )
            BackupDecodeResult.Success(payload)
        } catch (error: Exception) {
            BackupDecodeResult.InvalidFormat(error.message ?: "备份 JSON 格式错误")
        }
    }

    private fun taskToJson(item: TaskBackupV1): JSONObject = JSONObject()
        .put("id", item.id)
        .put("title", item.title)
        .putNullable("description", item.description)
        .putNullable("dueDate", item.dueDate)
        .put("isCompleted", item.isCompleted)
        .put("createdAt", item.createdAt)
        .put("updatedAt", item.updatedAt)

    private fun habitToJson(item: HabitBackupV1): JSONObject = JSONObject()
        .put("id", item.id)
        .put("name", item.name)
        .putNullable("description", item.description)
        .put("createdAt", item.createdAt)

    private fun habitRecordToJson(item: HabitRecordBackupV1): JSONObject = JSONObject()
        .put("id", item.id)
        .put("habitId", item.habitId)
        .put("recordDate", item.recordDate)
        .put("createdAt", item.createdAt)

    private fun goalToJson(item: GoalBackupV1): JSONObject = JSONObject()
        .put("id", item.id)
        .put("title", item.title)
        .putNullable("description", item.description)
        .put("createdAt", item.createdAt)
        .put("updatedAt", item.updatedAt)

    private fun goalStepToJson(item: GoalStepBackupV1): JSONObject = JSONObject()
        .put("id", item.id)
        .put("goalId", item.goalId)
        .put("title", item.title)
        .put("isCompleted", item.isCompleted)
        .put("createdAt", item.createdAt)
        .put("updatedAt", item.updatedAt)

    private fun reviewToJson(item: ReviewBackupV1): JSONObject = JSONObject()
        .put("id", item.id)
        .put("reviewDate", item.reviewDate)
        .put("type", item.type)
        .put("summary", item.summary)
        .put("problems", item.problems)
        .put("tomorrowFocus", item.tomorrowFocus)
        .putNullable("mood", item.mood)
        .put("completedTaskSnapshot", item.completedTaskSnapshot)
        .put("checkedHabitSnapshot", item.checkedHabitSnapshot)
        .put("createdAt", item.createdAt)
        .put("updatedAt", item.updatedAt)

    private fun taskFromJson(json: JSONObject): TaskBackupV1 = TaskBackupV1(
        id = json.getLong("id"),
        title = json.getString("title"),
        description = json.nullableString("description"),
        dueDate = json.nullableLong("dueDate"),
        isCompleted = json.getBoolean("isCompleted"),
        createdAt = json.getLong("createdAt"),
        updatedAt = json.getLong("updatedAt")
    )

    private fun habitFromJson(json: JSONObject): HabitBackupV1 = HabitBackupV1(
        id = json.getLong("id"),
        name = json.getString("name"),
        description = json.nullableString("description"),
        createdAt = json.getLong("createdAt")
    )

    private fun habitRecordFromJson(json: JSONObject): HabitRecordBackupV1 =
        HabitRecordBackupV1(
            id = json.getLong("id"),
            habitId = json.getLong("habitId"),
            recordDate = json.getString("recordDate"),
            createdAt = json.getLong("createdAt")
        )

    private fun goalFromJson(json: JSONObject): GoalBackupV1 = GoalBackupV1(
        id = json.getLong("id"),
        title = json.getString("title"),
        description = json.nullableString("description"),
        createdAt = json.getLong("createdAt"),
        updatedAt = json.getLong("updatedAt")
    )

    private fun goalStepFromJson(json: JSONObject): GoalStepBackupV1 = GoalStepBackupV1(
        id = json.getLong("id"),
        goalId = json.getLong("goalId"),
        title = json.getString("title"),
        isCompleted = json.getBoolean("isCompleted"),
        createdAt = json.getLong("createdAt"),
        updatedAt = json.getLong("updatedAt")
    )

    private fun reviewFromJson(json: JSONObject): ReviewBackupV1 = ReviewBackupV1(
        id = json.getLong("id"),
        reviewDate = json.getString("reviewDate"),
        type = json.getString("type"),
        summary = json.getString("summary"),
        problems = json.getString("problems"),
        tomorrowFocus = json.getString("tomorrowFocus"),
        mood = json.nullableInt("mood"),
        completedTaskSnapshot = json.getInt("completedTaskSnapshot"),
        checkedHabitSnapshot = json.getInt("checkedHabitSnapshot"),
        createdAt = json.getLong("createdAt"),
        updatedAt = json.getLong("updatedAt")
    )
}

private fun <T> List<T>.toJsonArray(mapper: (T) -> JSONObject): JSONArray {
    return JSONArray().also { array ->
        forEach { item -> array.put(mapper(item)) }
    }
}

private fun <T> JSONArray.mapObjects(mapper: (JSONObject) -> T): List<T> {
    return buildList(length()) {
        for (index in 0 until length()) {
            add(mapper(getJSONObject(index)))
        }
    }
}

private fun JSONObject.putNullable(
    key: String,
    value: Any?
): JSONObject = put(key, value ?: JSONObject.NULL)

private fun JSONObject.nullableString(key: String): String? {
    return if (isNull(key)) null else getString(key)
}

private fun JSONObject.nullableLong(key: String): Long? {
    return if (isNull(key)) null else getLong(key)
}

private fun JSONObject.nullableInt(key: String): Int? {
    return if (isNull(key)) null else getInt(key)
}
