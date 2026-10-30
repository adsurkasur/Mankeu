package com.mankeu.presentation.guide

import com.mankeu.domain.BookMode
import com.mankeu.presentation.Destination
import com.mankeu.presentation.Overlay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** The guide's invariants: help that can always show its place. */
class GuidesTest {
    @Test
    fun `the page help lists the articles of that page and the plan page is not the ledger`() {
        val plan = articlesFor(Destination.ENTRY, BookMode.PROJECTION, Overlay.NONE)
        assertTrue(plan.isNotEmpty() && HelpArticle.PLAN_CREATE in plan && HelpArticle.RECORD_FIX !in plan)
        assertTrue(HelpArticle.RECORD_FIX in articlesFor(Destination.ENTRY, BookMode.REALIZATION, Overlay.NONE))
        assertTrue(HelpArticle.DATA_IMPORT in articlesFor(Destination.HOME, BookMode.REALIZATION, Overlay.DATA))
        assertTrue(articlesFor(Destination.HOME, BookMode.REALIZATION, Overlay.HELP).isEmpty())
    }

    /**
     * "Tunjukkan di aplikasi" needs a screenshot of every place it shows. A new article or target
     * without one means `scripts/capture_help_screenshots.ps1` was not run.
     */
    @Test
    fun `every help article has its screenshot in the app`() {
        val images = File("src/main/res/drawable-nodpi").listFiles().orEmpty().map { it.nameWithoutExtension }.toSet()
        val missing = HelpArticle.entries.map { it.show.imageName }.filterNot { it in images }.distinct()
        assertTrue("Missing screenshots: $missing", missing.isEmpty())
    }

    /**
     * HelpUi splits steps on the `\n` escape; a raw newline in the XML collapses to a space, so all
     * steps would render as one (U2 review I-1). Edit help in docs/guide/help_strings.py, never by hand.
     */
    @Test
    fun `every help steps string has escaped line breaks and no raw newline`() {
        val step = Regex("""<string name="(help_\w+_steps)">(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)
        listOf("values", "values-en").forEach { dir ->
            val found = step.findAll(File("src/main/res/$dir/strings.xml").readText()).toList()
            assertTrue("$dir: no steps strings", found.size > 10)
            found.forEach { m ->
                val (name, text) = m.destructured
                assertTrue("$dir/$name has a raw newline", '\n' !in text && '\r' !in text)
                assertTrue("$dir/$name has fewer than 2 steps", text.split("\\n").size > 1)
            }
        }
    }

    @Test
    fun `plan articles open the plan page and overlay articles keep the page`() {
        HelpArticle.entries.forEach { article ->
            val spec = article.show
            if (spec.bookMode == BookMode.PROJECTION) assertEquals(article.name, Destination.ENTRY, spec.destination)
            if (spec.overlay != Overlay.NONE) assertEquals(article.name, null, spec.bookMode)
        }
        assertTrue(HelpGroup.entries.all { group -> HelpArticle.entries.any { it.group == group } })
    }
}
