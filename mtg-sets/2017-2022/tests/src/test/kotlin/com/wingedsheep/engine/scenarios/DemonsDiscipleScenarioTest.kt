package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Demon's Disciple — "When this creature enters, each player sacrifices a creature or
 * planeswalker of their choice."
 */
class DemonsDiscipleScenarioTest : ScenarioTestBase() {

    /** Cast the Disciple and resolve everything, never choosing a permanent named in [keep]. */
    private fun TestGame.castAndResolve(keep: Set<String> = emptySet()) {
        val cast = castSpell(1, "Demon's Disciple")
        withClue("cast should succeed: ${cast.error}") { cast.error shouldBe null }
        var guard = 0
        while ((state.stack.isNotEmpty() || hasPendingDecision()) && guard++ < 20) {
            val decision = getPendingDecision()
            if (decision is SelectCardsDecision) {
                val pick = decision.options
                    .filterNot { state.getEntity(it)?.get<CardComponent>()?.name in keep }
                    .take(decision.minSelections.coerceAtLeast(1))
                selectCards(pick)
            } else {
                resolveStack()
            }
        }
    }

    init {
        test("each player sacrifices their only creature, including the Disciple itself") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Demon's Disciple")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castAndResolve()

            game.isInGraveyard(1, "Demon's Disciple") shouldBe true
            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
        }

        test("each player chooses which of their creatures to sacrifice") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Demon's Disciple")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withCardOnBattlefield(1, "Llanowar Elves")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castAndResolve(keep = setOf("Demon's Disciple", "Hill Giant"))

            game.isOnBattlefield("Demon's Disciple") shouldBe true
            game.isInGraveyard(1, "Llanowar Elves") shouldBe true
            game.isOnBattlefield("Hill Giant") shouldBe true
            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
        }
    }
}
