package com.wingedsheep.ai.engine

import com.wingedsheep.mtg.sets.MtgSetCatalog
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe

/**
 * The limited heuristic prices what the intent readers read. When a reader learns an effect —
 * a dynamic -X/-X as removal, "destroy all" as a wrath, "look at the top, put one into your hand"
 * as card draw, scry and surveil as selection — the heuristic draft and deckbuilding rating of a
 * card no table rates has to move with it, or the AI aims the card correctly and still drafts it
 * like filler. These pin each reading's term in [LimitedCardRater.explain]'s heuristic breakdown,
 * which is computed even for table-rated cards.
 */
class LimitedRatingFollowsReadingTest : FunSpec({

    val byName by lazy { MtgSetCatalog.all.flatMap { it.cards }.distinctBy { it.name }.associateBy { it.name } }

    fun term(card: String, part: String): Double {
        val def = byName[card] ?: error("'$card' is not in the corpus")
        return LimitedCardRater.explain(def).heuristic.singleOrNull { it.part == part }?.value ?: 0.0
    }

    test("a -N/-N on another creature is priced like N damage, not as negative stats") {
        withClue("Grasp of Darkness's -4/-4 used to sum to -1.2") {
            term("Grasp of Darkness", "Spell") shouldBe (1.2 plusOrMinus 1e-9)
        }
    }

    test("a dynamic -X/-X on another creature is priced like X damage") {
        term("Defile", "Spell") shouldBe (1.2 plusOrMinus 1e-9)
        term("Death Wind", "Spell") shouldBe (1.2 plusOrMinus 1e-9)
    }

    test("a destroy-all pipeline is priced like a destroy") {
        term("Planar Cleansing", "Spell") shouldBe (1.5 plusOrMinus 1e-9)
        term("Day of Judgment", "Spell") shouldBe (1.5 plusOrMinus 1e-9)
    }

    test("a for-each wrath is priced like the pipeline spelling") {
        term("Olivia's Wrath", "Spell") shouldBe (1.5 plusOrMinus 1e-9)
    }

    test("looking at the top and putting one into hand is priced like one card drawn") {
        term("Impulse", "Spell") shouldBe (0.6 plusOrMinus 1e-9)
        term("Ancient Stirrings", "Spell") shouldBe (0.6 plusOrMinus 1e-9)
    }

    test("scry and surveil are priced as a fraction of a draw") {
        withClue("Opt: scry 1 (0.1) and draw a card (0.6)") {
            term("Opt", "Spell") shouldBe (0.7 plusOrMinus 1e-9)
        }
        withClue("Crystal Ball: scry 2 on an activated ability (x0.6)") {
            term("Crystal Ball", "Activated ability 1") shouldBe (0.12 plusOrMinus 1e-9)
        }
    }
})
