package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Hunt the Hunter (THS #159) — {G} Sorcery.
 * "Target green creature you control gets +2/+2 until end of turn. It fights target green
 *  creature an opponent controls."
 */
class HuntTheHunterScenarioTest : ScenarioTestBase() {

    private fun TestGame.hunt(mine: EntityId, theirs: EntityId) = execute(
        CastSpell(
            player1Id,
            state.getHand(player1Id).first { state.getEntity(it)?.get<CardComponent>()?.name == "Hunt the Hunter" },
            listOf(ChosenTarget.Permanent(mine), ChosenTarget.Permanent(theirs)),
        )
    )

    private fun board() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Hunt the Hunter")
        .withLandsOnBattlefield(1, "Forest", 1)
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardOnBattlefield(2, "Centaur Courser")
        .withCardOnBattlefield(2, "Hill Giant")
        .withActivePlayer(1)
        .withPriorityPlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("pumped +2/+2, the green creature fights and kills the opposing green creature") {
            val game = board()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.hunt(bears, game.findPermanent("Centaur Courser")!!).error shouldBe null
            game.resolveStack()

            withClue("the 3/3 Courser took 4 and died") { game.isInGraveyard(2, "Centaur Courser") shouldBe true }
            withClue("the 4/4 Bears survived the Courser's 3 damage") {
                game.isOnBattlefield("Grizzly Bears") shouldBe true
                game.state.projectedState.getPower(bears) shouldBe 4
            }
        }

        test("the second target must be green") {
            val game = board()

            game.hunt(game.findPermanent("Grizzly Bears")!!, game.findPermanent("Hill Giant")!!).error shouldNotBe null
        }
    }
}
