package com.mocharealm.accompanist.lyrics.core.parser

import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.exporter.EnhancedLrcExporter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class EnhancedLrcParserTest {

    @Test
    fun testParseBgWithTranslation() {
        val lrc = """
            [00:10.00]<00:10.00>Main <00:10.50>Lyrics<00:10.70>
            [00:10.00]主歌词翻译
            [bg: <00:10.00>Back<00:10.50>ground<00:11.00>]
            [bg: <00:10.00>背景音翻译<00:11.00>]
        """.trimIndent().split("\n")
        
        val data = EnhancedLrcParser.parse(lrc)
        
        assertEquals(1, data.lines.size)
        val line = data.lines[0] as KaraokeLine.MainKaraokeLine
        assertEquals("主歌词翻译", line.translation)
        
        val bg = line.accompanimentLines?.first()
        assertNotNull(bg)
        assertEquals("Background", bg.syllables.joinToString("") { it.content }.trim())
        assertEquals("背景音翻译", bg.translation)
    }

    @Test
    fun testELRCRoundTrip() {
        val lrc = """
            [ti:Test Title]
            [00:01.00]<00:01.00>Hello <00:02.00>World<00:02.50>
            [00:01.00]你好世界
            [bg: <00:01.50>Chorus<00:02.00>]
            [bg: <00:01.50>合唱<00:02.00>]
        """.trimIndent().split("\n")
        
        val parsed = EnhancedLrcParser.parse(lrc)
        val exported = EnhancedLrcExporter.export(parsed)
        
        val reParsed = EnhancedLrcParser.parse(exported.split("\n"))
        
        assertEquals(parsed.lines.size, reParsed.lines.size)
        val p1 = parsed.lines[0] as KaraokeLine.MainKaraokeLine
        val p2 = reParsed.lines[0] as KaraokeLine.MainKaraokeLine
        
        assertEquals(p1.translation, p2.translation)
        assertEquals(p1.accompanimentLines?.size, p2.accompanimentLines?.size)
        assertEquals(p1.accompanimentLines?.first()?.translation, p2.accompanimentLines?.first()?.translation)
    }

    @Test
    fun testVoiceAlignmentRoundTrip() {
        val lrc = """
            [00:01.00]v1: <00:01.00>Singer <00:01.50>One<00:02.00>
            [00:03.00]v2: <00:03.00>Singer <00:03.50>Two<00:04.00>
        """.trimIndent().split("\n")

        val parsed = EnhancedLrcParser.parse(lrc)
        assertEquals(2, parsed.lines.size)
        assertEquals(com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeAlignment.Start, (parsed.lines[0] as KaraokeLine).alignment)
        assertEquals(com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeAlignment.End, (parsed.lines[1] as KaraokeLine).alignment)

        val exported = EnhancedLrcExporter.export(parsed)
        kotlin.test.assertTrue(exported.contains("v1: <00:01.000>Singer"))
        kotlin.test.assertTrue(exported.contains("v2: <00:03.000>Singer"))

        val reParsed = EnhancedLrcParser.parse(exported.split("\n"))
        assertEquals(2, reParsed.lines.size)
        assertEquals(com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeAlignment.Start, (reParsed.lines[0] as KaraokeLine).alignment)
        assertEquals(com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeAlignment.End, (reParsed.lines[1] as KaraokeLine).alignment)
    }

    @Test
    fun testInvertedSyllableTimestampsNeverCrash() {
        val lrc = """
            [00:10.00]<00:10.00>First <00:08.50>Second <00:07.00>Third<00:06.00>
        """.trimIndent().split("\n")

        val parsed = EnhancedLrcParser.parse(lrc)
        assertEquals(1, parsed.lines.size)
        val line = parsed.lines[0] as KaraokeLine.MainKaraokeLine
        kotlin.test.assertTrue(line.end >= line.start)
        line.syllables.forEach { s ->
            kotlin.test.assertTrue(s.end >= s.start)
        }
    }

    @Test
    fun testInvertedLineTimestampsNeverCrash() {
        val lrc = """
            [00:20.00]Later line
            [00:10.00]Earlier line
        """.trimIndent().split("\n")

        val parsed = EnhancedLrcParser.parse(lrc)
        assertEquals(2, parsed.lines.size)
        parsed.lines.forEach { line ->
            kotlin.test.assertTrue(line.end >= line.start)
        }
    }

    @Test
    fun testInvertedBgSyllablesNeverCrash() {
        val lrc = """
            [00:10.00]<00:10.00>Main<00:12.00>
            [bg: <00:15.00>Back<00:11.00>ground<00:09.00>]
        """.trimIndent().split("\n")

        val parsed = EnhancedLrcParser.parse(lrc)
        assertEquals(1, parsed.lines.size)
        val line = parsed.lines[0] as KaraokeLine.MainKaraokeLine
        val bg = line.accompanimentLines?.first()
        assertNotNull(bg)
        kotlin.test.assertTrue(bg.end >= bg.start)
        bg.syllables.forEach { s ->
            kotlin.test.assertTrue(s.end >= s.start)
        }
    }
}
