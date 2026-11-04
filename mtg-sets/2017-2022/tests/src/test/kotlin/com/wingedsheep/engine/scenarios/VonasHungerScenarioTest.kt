package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.player.PlayerCitysBlessingComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Vona's Hunger (RIX #90) — {2}{B} Instant.
 *   "Ascend. Each opponent sacrifices a creature of their choice. If you have the city's blessing,
 *    instead each opponent sacrifices half the creatures they control of their choice, rounded up."
 *
 * The blessed mode reads the *opponent's* creature count (`countPerPlayer`), not the caster's.
 */
class VonasHungerScenarioTest : ScenarioTestBase() {

    init {
        test("with the city's blessing the opponent sacrifices half their creatures, rounded up") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Vona's Hunger")
                .withLandsOnBattlefield(1, "Swamp", 10) // ten permanents: the spell's ascend blesses you
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Vona's Hunger").error shouldBe null
            game.resolveStack()

            withClue("the spell's ascend granted the city's blessing") {
                game.state.getEntity(game.player1Id)?.has<PlayerCitysBlessingComponent>() shouldBe true
            }
            val decision = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            withClue("the opponent chooses half of their five creatures, rounded up") {
                decision.playerId shouldBe game.player2Id
                decision.minSelections shouldBe 3
                decision.maxSelections shouldBe 3
            }
            game.selectCards(game.findAllPermanents("Grizzly Bears").take(3)).error shouldBe null

            game.findAllPermanents("Grizzly Bears").size shouldBe 2
            game.findCardsInGraveyard(2, "Grizzly Bears").size shouldBe 3
        }

        test("without the city's blessing the opponent sacrifices one creature") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Vona's Hunger")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Vona's Hunger").error shouldBe null
            game.resolveStack()

            game.state.getEntity(game.player1Id)?.has<PlayerCitysBlessingComponent>() shouldBe false
            val decision = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            decision.minSelections shouldBe 1
            game.selectCards(listOf(game.findAllPermanents("Grizzly Bears").first())).error shouldBe null

            game.findAllPermanents("Grizzly Bears").size shouldBe 2
        }
    }
}
