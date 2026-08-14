package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.player.PlayerNoMaximumHandSizeComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Scenario tests for Wisdom of Ages. */
class WisdomOfAgesScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    init {
        context("Wisdom of Ages") {
            test("returns all instants/sorceries from graveyard, removes max hand size, and exiles itself") {
                var builder = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Wisdom of Ages")
                    .withCardInGraveyard(1, "Lightning Bolt")   // instant
                    .withCardInGraveyard(1, "Divination")       // sorcery
                    .withCardInGraveyard(1, "Grizzly Bears")    // creature — must stay
                    .withLandsOnBattlefield(1, "Island", 7)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                val game = builder.build()

                game.castSpell(1, "Wisdom of Ages").error shouldBe null
                game.resolveStack()

                withClue("both the instant and the sorcery return to hand") {
                    game.findCardsInHand(1, "Lightning Bolt").size shouldBe 1
                    game.findCardsInHand(1, "Divination").size shouldBe 1
                }
                withClue("the creature stays in the graveyard") {
                    game.findCardsInGraveyard(1, "Grizzly Bears").size shouldBe 1
                }
                withClue("Wisdom of Ages exiles itself (not in hand or graveyard)") {
                    game.findCardsInHand(1, "Wisdom of Ages").size shouldBe 0
                    game.findCardsInGraveyard(1, "Wisdom of Ages").size shouldBe 0
                    game.state.getExile(game.player1Id).any { id ->
                        game.state.getEntity(id)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == "Wisdom of Ages"
                    } shouldBe true
                }
                withClue("the caster has no maximum hand size for the rest of the game") {
                    game.state.getEntity(game.player1Id)?.has<PlayerNoMaximumHandSizeComponent>() shouldBe true
                }
            }
        }
    }
}
