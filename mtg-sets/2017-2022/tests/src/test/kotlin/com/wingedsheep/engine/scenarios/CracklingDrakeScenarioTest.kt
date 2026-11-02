package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Crackling Drake (GRN #163) — {U}{U}{R}{R} Drake, star/4, Flying.
 *
 * "Crackling Drake's power is equal to the total number of instant and sorcery cards you own in
 *  exile and in your graveyard. / When this creature enters, draw a card."
 *
 * Pinned: the power sums your graveyard and your exile, ignores other card types and the
 * opponent's cards, and toughness stays 4.
 */
class CracklingDrakeScenarioTest : ScenarioTestBase() {

    init {
        test("power counts your instants and sorceries in graveyard and exile, and nobody else's") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Crackling Drake")
                .withCardInGraveyard(1, "Lightning Bolt")
                .withCardInGraveyard(1, "Divination")
                .withCardInGraveyard(1, "Grizzly Bears")
                .withCardInExile(1, "Shock")
                .withCardInGraveyard(2, "Lava Spike")
                .withCardInExile(2, "Lightning Bolt")
                .withActivePlayer(1)
                .build()

            val drake = game.findPermanent("Crackling Drake")!!

            withClue("Lightning Bolt + Divination in the graveyard, Shock in exile") {
                game.state.projectedState.getPower(drake) shouldBe 3
            }
            game.state.projectedState.getToughness(drake) shouldBe 4
        }
    }
}
