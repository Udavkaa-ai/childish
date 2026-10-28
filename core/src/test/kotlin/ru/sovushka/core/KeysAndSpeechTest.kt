package ru.sovushka.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KeysAndSpeechTest {
    private val token = "eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiIxMjM0NSJ9.AbCdEf_GhIj-KlMn"

    @Test
    fun qrKeys() {
        assertEquals(KeyTransfer.Keys(mesh = token), KeyTransfer.parse("  $token\n"))
        val k = KeyTransfer.parse("NIKA1:mesh=${token}&yaKey=AQVN%2Babc%3D&yaFolder=b1g")!!
        assertEquals(token, k.mesh)
        assertEquals("AQVN+abc=", k.yaKey)
        assertEquals("b1g", k.yaFolder)
        assertEquals("", k.gigaKey)
        assertEquals("ключ МЭШ, ключ Yandex Cloud, каталог Yandex Cloud", k.describe())
        assertEquals("ZmFrZQ==", KeyTransfer.parse("NIKA1:gigaKey=ZmFrZQ%3D%3D")!!.gigaKey)
        assertNull(KeyTransfer.parse("https://example.com"))
        assertNull(KeyTransfer.parse("NIKA1:"))
        assertNull(KeyTransfer.parse("NIKA1:mesh=broken"))
    }

    @Test
    fun soundsAlike() {
        assertEquals(TextMatch.soundKey("dog"), TextMatch.soundKey("dock"))
        assertEquals(TextMatch.soundKey("three"), TextMatch.soundKey("tree"))
        assertEquals(TextMatch.soundKey("phone"), TextMatch.soundKey("fone"))
        assertEquals(TextMatch.soundKey("very"), TextMatch.soundKey("wery"))
        assertEquals(TextMatch.soundKey("box"), TextMatch.soundKey("bocks"))
        assertTrue(TextMatch.soundKey("fish") != TextMatch.soundKey("fix"))

        assertTrue(TextMatch.spokenMatch("dog", listOf("dock")))
        assertTrue(TextMatch.spokenMatch("bag", listOf("back")))
        assertTrue(TextMatch.spokenMatch("three", listOf("tree")))
        assertTrue(TextMatch.spokenMatch("rabbit", listOf("I said rabbet")))
        assertTrue(TextMatch.spokenMatch("ice cream", listOf("eyes cream")))
        assertTrue(TextMatch.spokenMatch("cat", listOf("hello", "cut")))
        assertFalse(TextMatch.spokenMatch("cat", listOf("dog")))
        assertFalse(TextMatch.spokenMatch("elephant", listOf("bicycle")))
        assertFalse(TextMatch.spokenMatch("apple", listOf("table")))
    }
}
