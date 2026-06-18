package com.jimu.app.data.backup

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupStreamIoTest {

    @Test
    fun readAcceptsContentExactlyAtLimit() {
        val bytes = ByteArray(MAX_BACKUP_BYTES) { 'a'.code.toByte() }

        val result = BackupStreamIo.readUtf8Limited(
            input = ByteArrayInputStream(bytes),
            declaredSizeBytes = bytes.size.toLong()
        )

        assertTrue(result is BackupFileReadResult.Success)
        assertEquals(MAX_BACKUP_BYTES, (result as BackupFileReadResult.Success).content.length)
    }

    @Test
    fun readRejectsContentOneByteOverLimitEvenWhenDeclaredSizeIsUnknown() {
        val bytes = ByteArray(MAX_BACKUP_BYTES + 1) { 'a'.code.toByte() }

        val result = BackupStreamIo.readUtf8Limited(
            input = ByteArrayInputStream(bytes),
            declaredSizeBytes = null
        )

        assertTrue(result is BackupFileReadResult.FileTooLarge)
    }

    @Test
    fun readRejectsDeclaredSizeOverLimitWithoutReadingStream() {
        val input = object : ByteArrayInputStream(byteArrayOf()) {
            override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                throw AssertionError("stream should not be read")
            }
        }

        val result = BackupStreamIo.readUtf8Limited(
            input = input,
            declaredSizeBytes = MAX_BACKUP_BYTES.toLong() + 1
        )

        assertTrue(result is BackupFileReadResult.FileTooLarge)
    }

    @Test
    fun readRejectsInvalidUtf8() {
        val invalidUtf8 = byteArrayOf(0xC3.toByte(), 0x28)

        val result = BackupStreamIo.readUtf8Limited(
            input = ByteArrayInputStream(invalidUtf8),
            declaredSizeBytes = null
        )

        assertTrue(result is BackupFileReadResult.InvalidEncoding)
    }

    @Test
    fun writeUsesUtf8AndFlushesOutput() {
        val output = ByteArrayOutputStream()

        val result = BackupStreamIo.writeUtf8(output, "迹目")

        assertEquals(BackupFileWriteResult.Success, result)
        assertArrayEquals("迹目".toByteArray(Charsets.UTF_8), output.toByteArray())
    }

    @Test
    fun writeReportsOutputFailure() {
        val output = object : OutputStream() {
            override fun write(value: Int) {
                throw IOException("disk full")
            }
        }

        val result = BackupStreamIo.writeUtf8(output, "backup")

        assertTrue(result is BackupFileWriteResult.WriteFailed)
    }
}
