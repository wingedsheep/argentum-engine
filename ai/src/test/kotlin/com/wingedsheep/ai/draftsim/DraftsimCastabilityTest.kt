package com.wingedsheep.ai.draftsim

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.shouldBe

private data class CCard(
    override val name: String,
    override val manaCost: String,
    override val typeLine: String,
    override val colorIdentity: List<String> = emptyList(),
    override val fixing: List<String> = emptyList(),
    override val fetchesBasic: Boolean = false,
    override val colors: List<String> = emptyList(),
    override val cmc: Double = DraftsimMana.cmc(manaCost),
    override val rarity: String? = "common",
) : ScorerCard

/**
 * [DraftsimCastability]: a spell's cast chance on curve from its deck's coloured sources, against the
 * same spell on a main colour's — what the final score charges a deck for each colour past two.
 */
class DraftsimCastabilityTest : FunSpec({

    val castability = DraftsimCastability(DraftsimCardOps(DraftsimSetTables(emptyMap(), emptySet(), emptyMap())))
    fun basic(color: String) = CCard("Basic $color", "", "Basic Land", colorIdentity = listOf(color))
    fun basics(vararg split: Pair<String, Int>) = split.flatMap { (c, n) -> List(n) { basic(c) } }
    fun weight(card: ScorerCard, deck: List<ScorerCard>) = castability.weight(card, castability.sources(deck), 40, 17)
    fun sourcesOn(deck: List<ScorerCard>, turn: Int) = castability.sources(deck).at(turn)

    val twoDrop = CCard("Bear", "{1}{G}", "Creature")
    val bomb = CCard("Dragon", "{4}{U}", "Creature")
    val doublePip = CCard("Knight", "{1}{W}{W}", "Creature")

    test("a main colour on nine sources casts on curve as well as the reference") {
        weight(twoDrop, basics("G" to 9, "R" to 8)) shouldBe 1.0
    }

    test("a splash on three sources keeps well under its full value; four sources keep more") {
        val three = weight(bomb, basics("G" to 7, "R" to 7, "U" to 3))
        val four = weight(bomb, basics("G" to 7, "R" to 6, "U" to 4))
        three shouldBeLessThan 0.8
        four shouldBeGreaterThan three
    }

    test("an even three-colour split costs every spell; a double pip more than a single one") {
        val deck = basics("W" to 6, "U" to 6, "G" to 5)
        val single = weight(CCard("Soldier", "{1}{W}", "Creature"), deck)
        single shouldBeLessThan 0.95
        weight(doublePip, deck) shouldBeLessThan single
    }

    test("dual lands lift a third colour's spells back toward the reference") {
        val bare = basics("W" to 6, "U" to 6, "G" to 5)
        val fixed = basics("W" to 5, "U" to 5, "G" to 4) +
            listOf(CCard("WG dual", "", "Land", fixing = listOf("W", "G")),
                CCard("UG dual", "", "Land", fixing = listOf("U", "G")),
                CCard("WU dual", "", "Land", fixing = listOf("W", "U")))
        weight(doublePip, fixed) shouldBeGreaterThan weight(doublePip, bare)
    }

    test("a fetch land counts toward a colour only up to the basics of it the deck runs") {
        val fetcher = CCard("Overlook", "", "Land", fixing = listOf("B", "R", "G"), fetchesBasic = true)
        val deck = basics("B" to 8, "R" to 7, "G" to 1) + List(4) { fetcher }
        sourcesOn(deck, 1)["G"] shouldBe 2.0
        sourcesOn(deck, 1)["B"] shouldBe 12.0
    }

    test("a Phyrexian pip or a {2/G} never strands a spell") {
        weight(CCard("Mite", "{1}{G/P}", "Creature"), basics("W" to 9, "U" to 8)) shouldBe 1.0
        weight(CCard("Reaper", "{2/G}{2/G}", "Creature"), basics("W" to 9, "U" to 8)) shouldBe 1.0
    }

    test("a mana rock fixes the spells cast after it, not the ones it competes with for mana") {
        val prism = CCard("Prism", "{2}", "Artifact", fixing = listOf("W", "U", "B", "R", "G"))
        val petal = CCard("Petal", "{0}", "Artifact", fixing = listOf("W", "U", "B", "R", "G"))
        val deck = basics("G" to 9, "R" to 8) + prism + petal
        // The free Petal counts a whole land from turn 1; the Prism half a land, from turn 3.
        sourcesOn(deck, 2)["U"] shouldBe 1.0
        sourcesOn(deck, 3)["U"] shouldBe 1.5
        weight(CCard("Drake", "{3}{U}", "Creature"), deck) shouldBeGreaterThan
            weight(CCard("Drake", "{3}{U}", "Creature"), basics("G" to 9, "R" to 8) + petal)
    }

    // The splash and four/five-colour bars count fixing in lands' worth: a rock or dork half a land,
    // a free fixer (mana value 0) a whole one, a land that makes the colour one.
    test("fixing toward a colour counts a rock half a land and a free fixer a whole one") {
        val ops = DraftsimCardOps(DraftsimSetTables(emptyMap(), emptySet(), emptyMap()))
        val all = listOf("W", "U", "B", "R", "G")
        val dual = CCard("UR dual", "", "Land", fixing = listOf("U", "R"))
        val prism = CCard("Prism", "{2}", "Artifact", fixing = all)
        val petal = CCard("Petal", "{0}", "Artifact", fixing = all)
        val elf = CCard("Elf", "{G}", "Creature", fixing = listOf("G"))
        ops.fixingFor("U", listOf(dual, prism, prism)) shouldBe 2.0
        ops.fixingFor("U", listOf(dual, petal)) shouldBe 2.0
        ops.fixingFor("U", listOf(elf, basic("U"))) shouldBe 0.0
    }
})
