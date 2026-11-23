package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Mark for Death — "Target creature an opponent controls blocks this turn if able. Untap that
 * creature. Other creatures that player controls can't block this turn."
 */
class MarkForDeathScenarioTest : ScenarioTestBase() {

    init {
        test("the marked creature untaps and must block; that player's other creatures can't block") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Mark for Death")
                .withLandsOnBattlefield(1, "Mountain", 4)
                .withCardOnBattlefield(1, "Centaur Courser")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant", tapped = true)
                .withCardOnBattlefield(2, "Llanowar Elves")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val giant = game.findPermanent("Hill Giant")!!
            game.castSpell(1, "Mark for Death", giant).error shouldBe null
            game.resolveStack()

            val theirElves = game.findPermanent("Llanowar Elves")!!
            val myBears = game.findPermanent("Grizzly Bears")!!
            withClue("the marked creature untapped") {
                game.state.getEntity(giant)!!.has<TappedComponent>() shouldBe false
            }
            withClue("only that player's other creatures can't block") {
                game.state.projectedState.cantBlock(theirElves) shouldBe true
                game.state.projectedState.cantBlock(giant) shouldBe false
                game.state.projectedState.cantBlock(myBears) shouldBe false
            }

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Centaur Courser" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

            withClue("the other creature can't block") {
                game.declareBlockers(mapOf("Llanowar Elves" to listOf("Centaur Courser"))).error shouldNotBe null
            }
            withClue("the marked creature has to block") {
                game.declareNoBlockers().error shouldNotBe null
            }
            game.declareBlockers(mapOf("Hill Giant" to listOf("Centaur Courser"))).error shouldBe null
        }
    }
}
