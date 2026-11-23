package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * Dimir Charm (GTC #154) — third mode: the caster looks at the top three cards of the *targeted*
 * player's library, puts exactly one back on top, and the other two go to that player's graveyard.
 */
class DimirCharmScenarioTest : ScenarioTestBase() {
    init {
        test("third mode keeps the caster's chosen card on top of the opponent's library and mills the rest") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Dimir Charm")
                .withLandsOnBattlefield(1, "Island", 1)
                .withLandsOnBattlefield(1, "Swamp", 1)
                .withCardInLibrary(2, "Grizzly Bears")
                .withCardInLibrary(2, "Hill Giant")
                .withCardInLibrary(2, "Elvish Warrior")
                .withCardInLibrary(2, "Darksteel Myr")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val before = game.state.getLibrary(game.player2Id)
            val spell = game.findCardsInHand(1, "Dimir Charm").single()

            val result = game.execute(
                CastSpell(
                    game.player1Id,
                    spell,
                    chosenModes = listOf(2),
                    modeTargetsOrdered = listOf(listOf(ChosenTarget.Player(game.player2Id))),
                )
            )
            withClue("cast should succeed: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            // The caster keeps Hill Giant (second from the top) on top.
            game.selectCards(listOf(before[1])).error shouldBe null
            game.resolveStack()

            withClue("the chosen card is back on top; the untouched fourth card sits under it") {
                game.state.getLibrary(game.player2Id) shouldBe listOf(before[1], before[3])
            }
            game.state.getGraveyard(game.player2Id)
                .map { game.state.getEntity(it)?.get<CardComponent>()?.name } shouldContainExactlyInAnyOrder
                listOf("Grizzly Bears", "Elvish Warrior")
            game.graveyardSize(1) shouldBe 1 // only Dimir Charm itself
        }
    }
}
