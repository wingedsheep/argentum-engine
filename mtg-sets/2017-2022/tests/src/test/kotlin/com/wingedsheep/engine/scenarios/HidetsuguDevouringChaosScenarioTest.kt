package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Hidetsugu, Devouring Chaos (NEO #99).
 *
 *   {2}{R}, {T}: Exile the top card of your library. You may play that card this turn. When you
 *   exile a nonland card this way, Hidetsugu deals damage equal to the exiled card's mana value to
 *   any target.
 *
 * Pins the composition: the reflexive trigger is gated on the exiled card being nonland, its
 * target is chosen only once the card is already exiled, and the damage reads the exiled card's
 * mana value off the pipeline collection the reflexive trigger carries forward.
 */
class HidetsuguDevouringChaosScenarioTest : ScenarioTestBase() {

    private fun setup(topCard: String): TestGame = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Hidetsugu, Devouring Chaos", summoningSickness = false)
        .withLandsOnBattlefield(1, "Mountain", 3)
        .withCardInLibrary(1, topCard)
        .withCardInLibrary(2, "Mountain")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun activateImpulse(game: TestGame) {
        val hidetsugu = game.findPermanent("Hidetsugu, Devouring Chaos")!!
        val ability = cardRegistry.getCard("Hidetsugu, Devouring Chaos")!!.script.activatedAbilities[1]
        game.execute(
            ActivateAbility(playerId = game.player1Id, sourceId = hidetsugu, abilityId = ability.id)
        ).error shouldBe null
    }

    init {
        test("exiling a nonland card deals its mana value to any target") {
            val game = setup("Hill Giant")
            activateImpulse(game)
            game.resolveStack()

            withClue("the reflexive trigger asks for its target after the exile") {
                game.isInExile(1, "Hill Giant") shouldBe true
                game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            }
            game.selectTargets(listOf(game.player2Id)).error shouldBe null
            game.resolveStack()

            withClue("Hill Giant has mana value 4") {
                game.getLifeTotal(2) shouldBe 16
            }
        }

        test("exiling a land deals no damage and asks for no target") {
            val game = setup("Forest")
            activateImpulse(game)
            game.resolveStack()

            game.isInExile(1, "Forest") shouldBe true
            game.hasPendingDecision() shouldBe false
            game.state.stack.isEmpty() shouldBe true
            game.getLifeTotal(2) shouldBe 20
        }
    }
}
