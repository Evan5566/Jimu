package com.jimu.app.data.backup

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

object BackupStreamIo {

    fun readUtf8Limited(
        input: InputStream,
        declaredSizeBytes: Long?
    ): BackupFileReadResult {
        if (declaredSizeBytes != null && declaredSizeBytes > MAX_BACKUP_BYTES) {
            return BackupFileReadResult.FileTooLarge()
        }

        return try {
            val initialSize = declaredSizeBytes
                ?.coerceAtMost(MAX_BACKUP_BYTES.toLong())
                ?.toInt()
                ?: 8 * 1024
            val output = ByteArrayOutputStream(initialSize)
            val buffer = ByteArray(8 * 1024)
            var total = 0

            while (true) {
                val remaining = MAX_BACKUP_BYTES + 1 - total
                if (remaining <= 0) {
                    return BackupFileReadResult.FileTooLarge()
                }

                val count = input.read(buffer, 0, minOf(buffer.size, remaining))
                if (count < 0) break
                if (count == 0) continue

                output.write(buffer, 0, count)
                total += count
                if (total > MAX_BACKUP_BYTES) {
                    return BackupFileReadResult.FileTooLarge()
                }
            }

            val decoder = Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
            val content = decoder.decode(ByteBuffer.wrap(output.toByteArray())).toString()
            BackupFileReadResult.Success(content)
        } catch (_: java.nio.charset.CharacterCodingException) {
            BackupFileReadResult.InvalidEncoding
        } catch (error: Exception) {
            BackupFileReadResult.ReadFailed(error.message ?: "读取备份文件失败")
        }
    }

    fun writeUtf8(
        output: OutputStream,
        content: String
    ): BackupFileWriteResult {
        val bytes = content.toByteArray(Charsets.UTF_8)
        if (bytes.size > MAX_BACKUP_BYTES) {
            return BackupFileWriteResult.FileTooLarge()
        }

        return try {
            output.write(bytes)
            output.flush()
            BackupFileWriteResult.Success
        } catch (error: Exception) {
            BackupFileWriteResult.WriteFailed(error.message ?: "写入备份文件失败")
        }
    }
}
