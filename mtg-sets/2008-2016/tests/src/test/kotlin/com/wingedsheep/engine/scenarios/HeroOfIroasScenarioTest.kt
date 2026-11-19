package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Hero of Iroas: "Aura spells you cast cost {1} less to cast. Heroic — Whenever you cast a spell
 * that targets this creature, put a +1/+1 counter on this creature."
 */
class HeroOfIroasScenarioTest : ScenarioTestBase() {

    init {
        test("an Aura spell costs {1} less") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Hero of Iroas")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInHand(1, "Pacifism")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.execute(
                CastSpell(
                    playerId = game.player1Id,
                    cardId = game.findCardsInHand(1, "Pacifism").single(),
                    targets = listOf(ChosenTarget.Permanent(bears)),
                )
            ).error shouldBe null
            game.resolveStack()
            game.findPermanent("Pacifism") shouldNotBe null
        }

        test("a bestow cast is discounted, and targeting the Hero triggers heroic") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Hero of Iroas")
                .withCardInHand(1, "Ghostblade Eidolon")
                .withLandsOnBattlefield(1, "Plains", 5)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val hero = game.findPermanent("Hero of Iroas")!!
            // Bestow {5}{W} less {1}: five Plains are enough.
            game.execute(
                CastSpell(
                    playerId = game.player1Id,
                    cardId = game.findCardsInHand(1, "Ghostblade Eidolon").single(),
                    targets = listOf(ChosenTarget.Permanent(hero)),
                    useAlternativeCost = true,
                    alternativeCostType = AlternativeCostType.BESTOW,
                )
            ).error shouldBe null
            game.resolveStack()

            // 2/2 + heroic +1/+1 counter + Ghostblade's +1/+1.
            val projected = game.state.projectedState
            projected.getPower(hero) shouldBe 4
            projected.getToughness(hero) shouldBe 4
        }
    }
}
