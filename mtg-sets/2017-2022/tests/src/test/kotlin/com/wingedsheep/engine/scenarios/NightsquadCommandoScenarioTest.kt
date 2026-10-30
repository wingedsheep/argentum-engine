package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * Nightsquad Commando (IKO #98) — {2}{B} 2/3 Human Soldier.
 *
 * "When this creature enters, if you attacked this turn, create a 1/1 white Human Soldier
 *  creature token."
 */
class NightsquadCommandoScenarioTest : ScenarioTestBase() {

    private fun setup() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Nightsquad Commando")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withLandsOnBattlefield(1, "Swamp", 3)
        .withCardInLibrary(1, "Swamp")
        .withCardInLibrary(2, "Swamp")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.soldierTokens() = state.getBattlefield().filter { id ->
        val card = state.getEntity(id)?.get<CardComponent>()
        card != null && card.name != "Nightsquad Commando" && card.name != "Grizzly Bears" &&
            card.typeLine.isCreature
    }

    init {
        test("creates a Human Soldier token when you attacked this turn") {
            val game = setup()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            game.castSpell(1, "Nightsquad Commando").error shouldBe null
            game.resolveStack()

            val tokens = game.soldierTokens()
            withClue("exactly one token was created") { tokens.size shouldBe 1 }
            val token = game.state.getEntity(tokens.single())!!.get<CardComponent>()!!
            withClue("it is a 1/1 white Human Soldier") {
                token.typeLine.subtypes.map { it.value } shouldContainExactlyInAnyOrder listOf("Human", "Soldier")
                game.state.projectedState.getPower(tokens.single()) shouldBe 1
                game.state.projectedState.getToughness(tokens.single()) shouldBe 1
            }
        }

        test("creates nothing if you haven't attacked this turn") {
            val game = setup()

            game.castSpell(1, "Nightsquad Commando").error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Nightsquad Commando") shouldBe true
            game.soldierTokens().size shouldBe 0
        }
    }
}
