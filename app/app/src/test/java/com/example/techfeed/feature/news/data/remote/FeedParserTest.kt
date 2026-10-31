package com.example.techfeed.feature.news.data.remote

import com.example.techfeed.core.feed.FeedSource
import org.junit.Assert.*
import org.junit.Test

class FeedParserTest {
    private val source = FeedSource("Test", "開発", "https://example.com/feed")

    private fun parse(xml: String) = FeedParser().parse(xml.byteInputStream(), source)

    @Test
    fun atomUsesAlternateLinkAndSkipsUnsafeLinks() {
        val result =
            parse(
                """<feed xmlns="http://www.w3.org/2005/Atom">
            <entry><title>Title</title><link rel="self" href="https://example.com/self"/>
            <link rel="alternate" href="https://example.com/article"/>
            <summary>&lt;p&gt;Hello&lt;/p&gt;</summary><updated>2026-10-28T10:00:00Z</updated></entry>
            <entry><title>Unsafe</title><link href="javascript:alert(1)"/></entry></feed>""",
            )
        assertEquals(1, result.size)
        assertEquals("https://example.com/article", result.single().url)
        assertEquals("Hello", result.single().summary)
    }

    @Test
    fun rssDeduplicatesAndIgnoresMissingTitle() {
        val result =
            parse(
                """<rss><channel>
            <item><title>News</title><link>https://example.com/a</link>
            <description><![CDATA[<b>Summary</b>]]></description></item>
            <item><title>Duplicate</title><link>https://example.com/a</link></item>
            <item><link>https://example.com/b</link></item></channel></rss>""",
            )
        assertEquals(1, result.size)
        assertEquals("Summary", result.single().summary)
    }

    @Test
    fun rssOneReadsNamespacedItemsAndDate() {
        val result =
            parse(
                """<rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
            xmlns="http://purl.org/rss/1.0/" xmlns:dc="http://purl.org/dc/elements/1.1/">
            <channel rdf:about="https://example.com/"><title>Feed</title></channel>
            <item rdf:about="https://example.com/a"><title>News</title>
            <link>https://example.com/a</link><dc:date>2026-10-28T10:00:00+09:00</dc:date></item>
            </rdf:RDF>""",
            )
        assertEquals(1, result.size)
        assertEquals("2026-10-28T10:00:00+09:00", result.single().time)
    }

    @Test
    fun doctypeIsRejectedBeforeReadingEntries() {
        assertThrows(Exception::class.java) {
            parse(
                """<!DOCTYPE rss [<!ENTITY title "Injected">]>
                <rss><channel><item><title>&title;</title>
                <link>https://example.com/a</link></item></channel></rss>""",
            )
        }
    }

    @Test
    fun malformedXmlFails() {
        assertThrows(Exception::class.java) { parse("<rss><item>") }
    }

    @Test
    fun documentReadsFeedTitleAndRejectsHtml() {
        val document =
            FeedParser()
                .parseDocument(
                    """<feed xmlns="http://www.w3.org/2005/Atom"><title>Example News</title></feed>"""
                        .byteInputStream(),
                    source,
                )
        assertEquals("Example News", document.title)
        assertTrue(document.articles.isEmpty())
        assertThrows(Exception::class.java) { parse("<html><title>Not a feed</title></html>") }
    }
}
