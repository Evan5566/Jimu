package com.jimu.app.data.backup

import java.time.LocalDate
import java.time.format.DateTimeParseException

object BackupValidator {

    fun validate(payload: AppBackupPayloadV1): BackupValidationResult {
        if (payload.backupVersion != BACKUP_VERSION) {
            return invalid("不支持的备份版本：${payload.backupVersion}")
        }
        if (payload.meta.appPackage != BACKUP_APP_PACKAGE) {
            return invalid("备份文件不属于迹目")
        }

        validateIds("待办", payload.tasks.map(TaskBackupV1::id))?.let { return it }
        validateIds("习惯", payload.habits.map(HabitBackupV1::id))?.let { return it }
        validateIds("习惯记录", payload.habitRecords.map(HabitRecordBackupV1::id))?.let { return it }
        validateIds("目标", payload.goals.map(GoalBackupV1::id))?.let { return it }
        validateIds("目标步骤", payload.goalSteps.map(GoalStepBackupV1::id))?.let { return it }
        validateIds("复盘", payload.reviews.map(ReviewBackupV1::id))?.let { return it }

        if (payload.tasks.any { it.title.isBlank() }) return invalid("待办标题不能为空")
        if (payload.habits.any { it.name.isBlank() }) return invalid("习惯名称不能为空")
        if (payload.goals.any { it.title.isBlank() }) return invalid("目标标题不能为空")
        if (payload.goalSteps.any { it.title.isBlank() }) return invalid("目标步骤标题不能为空")

        val habitIds = payload.habits.mapTo(mutableSetOf(), HabitBackupV1::id)
        if (payload.habitRecords.any { it.habitId !in habitIds }) {
            return invalid("习惯记录引用了不存在的习惯")
        }

        val goalIds = payload.goals.mapTo(mutableSetOf(), GoalBackupV1::id)
        if (payload.goalSteps.any { it.goalId !in goalIds }) {
            return invalid("目标步骤引用了不存在的目标")
        }

        if (payload.habitRecords.any { !isIsoDate(it.recordDate) }) {
            return invalid("习惯记录日期格式错误")
        }
        if (payload.reviews.any { !isIsoDate(it.reviewDate) }) {
            return invalid("复盘日期格式错误")
        }
        if (payload.reviews.any { it.type != "daily" }) {
            return invalid("当前版本只支持 daily 复盘")
        }

        return BackupValidationResult.Valid(ValidatedBackupPayload(payload))
    }

    private fun validateIds(
        label: String,
        ids: List<Long>
    ): BackupValidationResult.Invalid? {
        if (ids.any { it <= 0L }) return invalid("$label ID 必须大于 0")
        if (ids.toSet().size != ids.size) return invalid("${label}存在重复 ID")
        return null
    }

    private fun isIsoDate(value: String): Boolean {
        return try {
            LocalDate.parse(value)
            true
        } catch (_: DateTimeParseException) {
            false
        }
    }

    private fun invalid(message: String): BackupValidationResult.Invalid {
        return BackupValidationResult.Invalid(message)
    }
}
