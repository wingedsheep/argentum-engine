package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.HoldPolicy
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.TimingVerdict
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Brutal Hordechief's "{3}{R/W}{R/W}: Creatures your opponents control block this turn if able,
 * and you choose how those creatures block" bends only a block still to come: our turn, before
 * blocks, with an opposing creature untapped. Activated on the opponent's turn it buys nothing —
 * the [AiProfile.refuseWastedPayoffs] restriction window reads the group lure and the take-over of
 * the block declaration as one block lever.
 */
class BlockTakeOverWindowTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    private fun TestGame.hordechiefVerdict(): TimingVerdict {
        val chief = findPermanent("Brutal Hordechief")!!
        return HoldPolicy(intents, refuseWastedPayoffs = true).verdictFor(
            state, player1Id, "Brutal Hordechief",
            activation = ActivateAbility(
                player1Id, chief, intents.definition("Brutal Hordechief")!!.script.activatedAbilities.single().id,
            ),
        )
    }

    private fun position(activePlayer: Int, opposingTapped: Boolean = false) = scenario()
        .withPlayers()
        .withTurnNumber(20)
        .withActivePlayer(activePlayer)
        .withLandsOnBattlefield(1, "Mountain", 5)
        .withCardOnBattlefield(1, "Brutal Hordechief")
        .withCardOnBattlefield(2, "Grizzly Bears", tapped = opposingTapped)
        .withLandsOnBattlefield(2, "Forest", 2)
        .build()
        .advanceToPriority(1, Step.PRECOMBAT_MAIN)

    init {
        test("on their turn, taking over their blocks buys nothing") {
            position(activePlayer = 2).hordechiefVerdict() shouldBe TimingVerdict.NoWindow
        }

        test("on our turn before blocks, with an untapped opposing creature, is the window") {
            position(activePlayer = 1).hordechiefVerdict() shouldNotBe TimingVerdict.NoWindow
        }

        test("on our turn with every opposing creature tapped, nothing is left to block") {
            position(activePlayer = 1, opposingTapped = true).hordechiefVerdict() shouldBe TimingVerdict.NoWindow
        }
    }
}
