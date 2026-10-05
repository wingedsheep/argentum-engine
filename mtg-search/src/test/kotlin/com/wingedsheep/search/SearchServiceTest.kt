package com.wingedsheep.search

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * End-to-end tests for the search language. The same scenarios are covered by
 * the frontend Vitest suite — the two implementations must stay in lockstep.
 */
class SearchServiceTest : StringSpec({

    fun run(q: String): List<String> = SearchService.search(Fixtures.CARDS, q).map { it.name }.sorted()
    fun errs(q: String): List<String> = SearchService.parse(q).errors.map { it.message }

    "empty query matches everything" {
        SearchService.search(Fixtures.CARDS, "").shouldHaveSize(Fixtures.CARDS.size)
    }

    "bareword does substring name match" {
        run("lightning") shouldContain "Lightning Bolt"
    }

    "exact-name shortcut" {
        run("!\"lightning bolt\"") shouldContainExactly listOf("Lightning Bolt")
        run("!Lightning") shouldContainExactly emptyList()
    }

    "regex name match" {
        run("name:/llano/") shouldContainExactly listOf("Llanowar Elves")
    }

    "type matches union of cardTypes/supertypes/subtypes" {
        run("t:elf") shouldContainExactly listOf("Llanowar Elves")
    }

    "type AND-words across the type line (quoted multi-word value)" {
        run("t:\"legendary creature\"") shouldContainExactly listOf("Niv-Mizzet, Parun")
    }

    "cmc comparison" {
        run("cmc:1") shouldContain "Lightning Bolt"
        run("cmc:1") shouldContain "Llanowar Elves"
        run("cmc:1") shouldContain "Delver of Secrets"
        run("cmc<=2 t:creature") shouldContain "Llanowar Elves"
        run("cmc<=2 t:creature") shouldContain "Tarmogoyf"
        run("cmc>=5") shouldContain "Niv-Mizzet, Parun"
        run("cmc>=5") shouldContain "Serra Angel"
    }

    "color identity" {
        run("c:r") shouldContain "Lightning Bolt"
        run("c:r") shouldContain "Niv-Mizzet, Parun"
        run("c=ur") shouldContainExactly listOf("Niv-Mizzet, Parun")
        run("c=izzet") shouldContainExactly listOf("Niv-Mizzet, Parun")
        // Niv-Mizzet (UR) and Lightning Helix (RW) both have 2 colors in identity.
        run("c>=2") shouldContain "Niv-Mizzet, Parun"
        run("c>=2") shouldContain "Lightning Helix"
        // Forest has GREEN identity even though printed cost is empty.
        run("c:colorless") shouldContainExactly emptyList()
    }

    "cost queries printed mana-cost colors" {
        run("cost:colorless") shouldContain "Forest"
    }

    "mana cost multiset" {
        run("mana:{u}{u}") shouldContainExactly listOf("Counterspell")
        run("mana>={u}{u}{u}") shouldContainExactly listOf("Niv-Mizzet, Parun")
        run("mana:uu") shouldContainExactly listOf("Counterspell")
    }

    "cross-field numeric (pow vs tou)" {
        run("pow>=tou t:creature") shouldContain "Niv-Mizzet, Parun"
    }

    "rarity / set / format" {
        run("r:mythic") shouldContain "Tarmogoyf"
        run("s:grn") shouldContainExactly listOf("Niv-Mizzet, Parun")
        run("f:commander") shouldContain "Forest"
        run("f:commander") shouldContain "Niv-Mizzet, Parun"
    }

    "set: matches reprints, not just the canonical printing" {
        // Banishing Light's canonical setCode is BLB, but it's reprinted in EOE.
        // `s:EOE` must surface it via the reprint set; `s:BLB` still works for the
        // canonical printing.
        run("s:eoe") shouldContain "Banishing Light"
        run("s:blb") shouldContain "Banishing Light"
        // Cards without an EOE printing must not leak in.
        run("s:eoe") shouldNotContain "Lightning Bolt"
    }

    "name matching also checks printed names" {
        val cards = Fixtures.CARDS + Fixtures.SPECTACULAR_SPIDER_MAN
        fun run(q: String) = SearchService.search(cards, q).map { it.name }
        run("ademi") shouldContainExactly listOf("Spectacular Spider-Man")
        run("name:silkchutes") shouldContainExactly listOf("Spectacular Spider-Man")
        run("!\"Ademi of the Silkchutes\"") shouldContainExactly listOf("Spectacular Spider-Man")
        run("name:/^ademi/") shouldContainExactly listOf("Spectacular Spider-Man")
        run("spectacular") shouldContainExactly listOf("Spectacular Spider-Man")
    }

    "set: filter combines with name match for 's:EOE Banishing Light'" {
        run("s:eoe banishing") shouldContainExactly listOf("Banishing Light")
    }

    "keywords and is: shortcuts" {
        run("kw:flying") shouldContain "Niv-Mizzet, Parun"
        run("kw:flying") shouldContain "Serra Angel"
        run("is:flying") shouldContain "Serra Angel"
        run("is:legendary") shouldContain "Niv-Mizzet, Parun"
        run("is:basic") shouldContainExactly listOf("Forest")
        run("is:land") shouldContainExactly listOf("Forest")
        run("is:vanilla") shouldContainExactly emptyList()
    }

    "negation forms" {
        run("t:creature -c:r") shouldContain "Llanowar Elves"
        run("t:creature -c:r") shouldNotContain "Niv-Mizzet, Parun"
        run("t:creature not c:r") shouldNotContain "Niv-Mizzet, Parun"
    }

    "boolean OR alternation" {
        run("t:instant or is:legendary") shouldContain "Counterspell"
        run("t:instant or is:legendary") shouldContain "Lightning Bolt"
        run("t:instant or is:legendary") shouldContain "Niv-Mizzet, Parun"
    }

    "parens override default precedence" {
        // Niv-Mizzet (UR), Counterspell (U), Lightning Bolt (R), Lightning Helix (RW),
        // Jace (U), Delver of Secrets (U) → keeping only creatures: Niv-Mizzet, Delver.
        val results = run("(c:u or c:r) t:creature")
        results shouldContain "Niv-Mizzet, Parun"
        results shouldContain "Delver of Secrets"
    }

    "unknown filter produces error with suggestion" {
        val errors = SearchService.parse("clor:r").errors
        errors[0].suggestion shouldBe "Did you mean \"color:\"?"
    }

    "invalid color value reports error and matches nothing" {
        run("c:k") shouldContainExactly emptyList()
        errs("c:k").any { it.contains("Unknown color") } shouldBe true
    }

    "non-numeric cmc reports error" {
        errs("cmc:abc").size shouldBe 1
    }

    "unsupported operator on a key" {
        errs("name<=foo").any { it.contains("Operator") } shouldBe true
    }

    "isAdvancedQuery flags or / parens / not keyword" {
        Parser.isAdvancedQuery("t:creature c:r cmc<=3") shouldBe false
        Parser.isAdvancedQuery("-t:creature") shouldBe false
        Parser.isAdvancedQuery("t:creature or t:planeswalker") shouldBe true
        Parser.isAdvancedQuery("(c:r or c:b) t:creature") shouldBe true
        Parser.isAdvancedQuery("not t:creature") shouldBe true
    }

    "unmatched paren reported with span" {
        val errors = SearchService.parse("(t:creature").errors
        errors.any { it.message.contains("Unmatched") } shouldBe true
    }

    "top-level OR and parens parse" {
        // sanity: more complex composite query
        val results = run("(c:r or c:u) and t:creature")
        results shouldContain "Niv-Mizzet, Parun"
    }
})
