package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Restore the Peace — "Return each creature that dealt damage this turn to its owner's hand."
 *
 * First card to gather a mass bounce through the active-voice `hasDealtDamageThisTurn()` group
 * predicate: a creature that dealt combat damage and one that dealt noncombat damage both return,
 * while creatures that dealt none — on either side — stay put.
 */
class RestoreThePeaceScenarioTest : ScenarioTestBase() {

    init {
        test("returns creatures that dealt combat or noncombat damage, leaves the rest") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Restore the Peace")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withLandsOnBattlefield(1, "Island", 2)
                .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
                .withCardOnBattlefield(1, "Prodigal Sorcerer", summoningSickness = false)
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withCardOnBattlefield(2, "Craw Wurm")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            // Noncombat damage: the Sorcerer pings the opponent.
            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = game.findPermanent("Prodigal Sorcerer")!!,
                    abilityId = cardRegistry.getCard("Prodigal Sorcerer")!!.activatedAbilities[0].id,
                    targets = listOf(ChosenTarget.Player(game.player2Id))
                )
            ).error shouldBe null
            game.resolveStack()

            // Combat damage: Hill Giant attacks unblocked.
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Hill Giant" to 2)).error shouldBe null
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            game.castSpell(1, "Restore the Peace").error shouldBe null
            game.resolveStack()

            game.isInHand(1, "Hill Giant") shouldBe true
            game.isInHand(1, "Prodigal Sorcerer") shouldBe true
            game.isOnBattlefield("Grizzly Bears") shouldBe true
            game.isOnBattlefield("Craw Wurm") shouldBe true
        }
    }
}
