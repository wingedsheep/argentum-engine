package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.hou.cards.ResilientKhenra
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Resilient Khenra (HOU #131) — {1}{G} Creature — Jackal Wizard 2/2.
 *
 *   When this creature enters, you may have target creature get +X/+X until end of turn, where X
 *   is this creature's power.
 *   Eternalize {4}{G}{G}
 */
class ResilientKhenraScenarioTest : ScenarioTestBase() {

    private val name = "Resilient Khenra"
    private val eternalizeId = ResilientKhenra.activatedAbilities.single().id

    init {
        fun board() = scenario()
            .withPlayers("Player1", "Player2")
            .withActivePlayer(1)
            .withPriorityPlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .withCardOnBattlefield(1, "Grizzly Bears")

        /** Answers the trigger's target prompt with [target] and its "you may" prompt with [accept]. */
        fun TestGame.answerTrigger(target: EntityId, accept: Boolean) {
            repeat(4) {
                when (getPendingDecision()) {
                    is ChooseTargetsDecision -> selectTargets(listOf(target)).error shouldBe null
                    is YesNoDecision -> answerYesNo(accept).error shouldBe null
                    else -> if (state.stack.isNotEmpty()) resolveStack() else return
                }
            }
        }

        fun TestGame.statsOf(id: EntityId) =
            state.projectedState.getPower(id) to state.projectedState.getToughness(id)

        test("the 2/2 gives target creature +2/+2 until end of turn") {
            val game = board()
                .withCardInHand(1, name)
                .withLandsOnBattlefield(1, "Forest", 2)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, name).error shouldBe null
            game.resolveStack()
            game.answerTrigger(bears, accept = true)

            withClue("X = the Khenra's power 2") { game.statsOf(bears) shouldBe (4 to 4) }

            game.passUntilPhase(Phase.ENDING, Step.CLEANUP)
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            withClue("the bonus ends with the turn") { game.statsOf(bears) shouldBe (2 to 2) }
        }

        test("declining the optional trigger leaves the target unchanged") {
            val game = board()
                .withCardInHand(1, name)
                .withLandsOnBattlefield(1, "Forest", 2)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, name).error shouldBe null
            game.resolveStack()
            game.answerTrigger(bears, accept = false)

            game.statsOf(bears) shouldBe (2 to 2)
        }

        test("the eternalized 4/4 token gives +4/+4") {
            val game = board()
                .withCardInGraveyard(1, name)
                .withLandsOnBattlefield(1, "Forest", 6)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!
            val card = game.findCardsInGraveyard(1, name).single()

            game.execute(ActivateAbility(game.player1Id, card, eternalizeId)).error shouldBe null
            game.resolveStack()
            game.answerTrigger(bears, accept = true)

            withClue("X = the token's power 4") { game.statsOf(bears) shouldBe (6 to 6) }
        }

        test("if the Khenra has left the battlefield, X is its last-known power") {
            val game = board()
                .withCardInHand(1, name)
                .withCardInHand(1, "Murder")
                .withCardInHand(1, "Giant Growth")
                .withLandsOnBattlefield(1, "Forest", 4)
                .withLandsOnBattlefield(1, "Swamp", 4)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, name).error shouldBe null
            game.resolveStack()
            val khenra = game.findPermanent(name)!!
            if (game.getPendingDecision() is ChooseTargetsDecision) {
                game.selectTargets(listOf(bears)).error shouldBe null
            }

            withClue("in response to its trigger, Murder the Khenra, then pump it on top so it dies as a 5/5") {
                game.castSpell(1, "Murder", khenra).error shouldBe null
                game.castSpell(1, "Giant Growth", khenra).error shouldBe null
            }
            game.resolveStack()
            game.answerTrigger(bears, accept = true)

            game.findPermanent(name) shouldBe null
            withClue("X uses the Khenra's last-known power 5, not its printed 2") { game.statsOf(bears) shouldBe (7 to 7) }
        }
    }
}
