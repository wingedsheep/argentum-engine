package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Fires of Invention (ELD #125) — "You can cast spells only during your turn and you can cast no
 * more than two spells each turn. You may cast spells with mana value less than or equal to the
 * number of lands you control without paying their mana costs."
 *
 * Proves the free-cast filter's mana-value cap tracks the lands its controller has, and that the
 * two-spell cap applies to free casts too.
 */
class FiresOfInventionScenarioTest : ScenarioTestBase() {

    private fun TestGame.handCard(name: String): EntityId =
        state.getHand(player1Id).first { state.getEntity(it)?.get<CardComponent>()?.name == name }

    private fun board() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Fires of Invention")
        .withLandsOnBattlefield(1, "Mountain", 2)
        .withCardsInHand(1, "Grizzly Bears", 3)
        .withCardInHand(1, "Hill Giant")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("a spell with mana value at most the lands you control casts for free; a bigger one doesn't") {
            val game = board()

            // Hill Giant has mana value 4 against two lands.
            game.execute(
                CastSpell(game.player1Id, game.handCard("Hill Giant"), useWithoutPayingManaCost = true)
            ).error shouldNotBe null

            // Grizzly Bears has mana value 2 — free, leaving both Mountains untapped.
            game.execute(
                CastSpell(game.player1Id, game.handCard("Grizzly Bears"), useWithoutPayingManaCost = true)
            ).error shouldBe null
            game.resolveStack()
            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }

        test("no more than two spells each turn, free or not") {
            val game = board()
            repeat(2) {
                game.execute(
                    CastSpell(game.player1Id, game.handCard("Grizzly Bears"), useWithoutPayingManaCost = true)
                ).error shouldBe null
                game.resolveStack()
            }

            game.execute(
                CastSpell(game.player1Id, game.handCard("Grizzly Bears"), useWithoutPayingManaCost = true)
            ).error shouldNotBe null
        }
    }
}
