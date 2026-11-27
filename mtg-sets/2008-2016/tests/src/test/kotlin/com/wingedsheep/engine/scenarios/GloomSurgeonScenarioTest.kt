package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Gloom Surgeon (AVR #104) — {1}{B} 2/1 Spirit.
 *
 *   If combat damage would be dealt to this creature, prevent that damage and exile that many
 *   cards from the top of your library.
 *
 * "That many" is the damage that would be dealt (`DynamicAmounts.wouldBeDamage()`), so combat damage
 * that can't be prevented (Malignus) is dealt and still exiles that many cards, per the ruling.
 * Noncombat damage isn't touched.
 */
class GloomSurgeonScenarioTest : ScenarioTestBase() {

    private fun ScenarioBuilder.library(cards: Int): ScenarioBuilder {
        repeat(cards) { withCardInLibrary(1, "Swamp") }
        return this
    }

    init {
        test("combat damage to it is prevented and that many cards are exiled from the top of your library") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Gloom Surgeon")
                .withCardOnBattlefield(2, "Hill Giant")
                .library(6)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Hill Giant" to 1)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Gloom Surgeon" to listOf("Hill Giant"))).error shouldBe null
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            withClue("Hill Giant's 3 damage was prevented") {
                game.isOnBattlefield("Gloom Surgeon") shouldBe true
            }
            withClue("three cards were exiled from the top of the library") {
                game.librarySize(1) shouldBe 3
                game.state.getExile(game.player1Id).size shouldBe 3
            }
        }

        test("combat damage that can't be prevented is dealt, and that many cards are still exiled") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withLifeTotal(1, 8)
                .withCardOnBattlefield(1, "Gloom Surgeon")
                .withCardOnBattlefield(2, "Malignus") // 4/4: half of 8
                .library(6)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Malignus" to 1)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Gloom Surgeon" to listOf("Malignus"))).error shouldBe null
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            withClue("Malignus's damage can't be prevented, so Gloom Surgeon died") {
                game.isOnBattlefield("Gloom Surgeon") shouldBe false
            }
            withClue("four cards (the damage that would be dealt) were still exiled") {
                game.librarySize(1) shouldBe 2
                game.state.getExile(game.player1Id).size shouldBe 4
            }
        }

        test("noncombat damage is dealt normally and exiles nothing") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Gloom Surgeon")
                .withCardInHand(2, "Lightning Bolt")
                .withLandsOnBattlefield(2, "Mountain", 1)
                .library(6)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val surgeon = game.findPermanent("Gloom Surgeon")!!
            game.castSpell(2, "Lightning Bolt", surgeon).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Gloom Surgeon") shouldBe false
            game.librarySize(1) shouldBe 6
        }
    }
}
