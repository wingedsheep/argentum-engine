package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Historian's Wisdom (NEO #192) — {2}{G} Enchantment — Aura
 *
 *   Enchant artifact or creature
 *   When this Aura enters, if enchanted permanent is a creature with the greatest power among
 *   creatures on the battlefield, draw a card.
 *   As long as enchanted permanent is a creature, it gets +2/+1.
 *
 * Proves the intervening-if reads the enchanted permanent's projected power (including the Aura's
 * own +2/+1) against every creature on the battlefield, ties included, and that a noncreature host
 * neither draws nor gets the bonus.
 */
class HistoriansWisdomScenarioTest : ScenarioTestBase() {
    init {
        fun setup(vararg opposing: String) = scenario()
            .withPlayers("P1", "P2")
            .withCardInHand(1, "Historian's Wisdom")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardOnBattlefield(1, "Mind Stone")
            .withLandsOnBattlefield(1, "Forest", 3)
            .withCardInLibrary(1, "Forest")
            .withCardInLibrary(1, "Forest")
            .apply { opposing.forEach { withCardOnBattlefield(2, it) } }
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("boosted creature now has the greatest power: draws a card") {
            val game = setup("Hill Giant") // 3/3 vs Bears 2/2 -> 4/3
            val bears = game.findPermanent("Grizzly Bears")!!
            val handBefore = game.handSize(1)

            val r = game.castSpell(1, "Historian's Wisdom", bears)
            withClue("${r.error}") { r.error shouldBe null }
            game.resolveStack()

            game.state.projectedState.getPower(bears) shouldBe 4
            game.state.projectedState.getToughness(bears) shouldBe 3
            withClue("Aura left hand, one card drawn") { game.handSize(1) shouldBe handBefore }
        }

        test("tied for greatest power still draws") {
            val game = setup("Rumbling Baloth") // 4/4 ties the boosted 4/3 Bears
            val bears = game.findPermanent("Grizzly Bears")!!
            val handBefore = game.handSize(1)

            game.castSpell(1, "Historian's Wisdom", bears).error shouldBe null
            game.resolveStack()

            game.handSize(1) shouldBe handBefore
        }

        test("a bigger creature elsewhere: no card") {
            val game = setup("Craw Wurm") // 6/4 beats Bears 4/3
            val bears = game.findPermanent("Grizzly Bears")!!
            val handBefore = game.handSize(1)

            game.castSpell(1, "Historian's Wisdom", bears).error shouldBe null
            game.resolveStack()

            game.state.projectedState.getPower(bears) shouldBe 4
            game.handSize(1) shouldBe handBefore - 1
        }

        test("on a noncreature artifact: no card and no creature") {
            val game = setup()
            val stone = game.findPermanent("Mind Stone")!!
            val handBefore = game.handSize(1)

            game.castSpell(1, "Historian's Wisdom", stone).error shouldBe null
            game.resolveStack()

            game.state.projectedState.isCreature(stone) shouldBe false
            game.handSize(1) shouldBe handBefore - 1
        }
    }
}
