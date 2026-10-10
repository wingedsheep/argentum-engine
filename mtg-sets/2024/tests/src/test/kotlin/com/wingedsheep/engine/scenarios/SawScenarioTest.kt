package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Saw (DSK #254) — {2} Artifact — Equipment.
 *
 * "Equipped creature gets +2/+0.
 *  Whenever equipped creature attacks, you may sacrifice a permanent other than that creature or
 *  this Equipment. If you do, draw a card.
 *  Equip {2}"
 */
class SawScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    private val sawEquipId by lazy {
        cardRegistry.requireCard("Saw").activatedAbilities[0].id
    }

    init {
        test("equipped creature gets +2/+0") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Grizzly Bears") // 2/2 base
                .withCardOnBattlefield(1, "Saw")
                .withLandsOnBattlefield(1, "Forest", 2)    // pay Equip {2}
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            val saw = game.findPermanent("Saw")!!

            game.execute(
                com.wingedsheep.engine.core.ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = saw,
                    abilityId = sawEquipId,
                    targets = listOf(ChosenTarget.Permanent(bears))
                )
            ).error shouldBe null
            game.resolveStack()

            val projected = stateProjector.project(game.state)
            withClue("Equipped Bears should be 4/2 (2/2 + 2/+0)") {
                projected.getPower(bears) shouldBe 4
                projected.getToughness(bears) shouldBe 2
            }
        }

        test("attacking and sacrificing another permanent draws a card") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Grizzly Bears")  // becomes the equipped attacker
                .withCardOnBattlefield(1, "Saw")
                .withCardOnBattlefield(1, "Food", isToken = true) // the sacrifice fodder
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardInLibrary(1, "Centaur Courser")    // the card drawn by the trigger
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            val saw = game.findPermanent("Saw")!!

            game.execute(
                com.wingedsheep.engine.core.ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = saw,
                    abilityId = sawEquipId,
                    targets = listOf(ChosenTarget.Permanent(bears))
                )
            ).error shouldBe null
            game.resolveStack()

            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2))
            game.resolveStack() // attack trigger goes on the stack; resolving pauses at the "may"

            withClue("attack asks which permanent to sacrifice, if any") {
                (game.getPendingDecision() is SelectCardsDecision) shouldBe true
            }
            val food = game.findPermanents("Food")
            game.selectCards(food)
            game.resolveStack()

            withClue("the Food was sacrificed (no longer in play)") {
                game.findPermanents("Food").isEmpty() shouldBe true
            }
            withClue("the trigger drew a card") {
                game.isInHand(1, "Centaur Courser") shouldBe true
            }
        }

        test("declining the attack 'may' draws nothing and sacrifices nothing") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Saw")
                .withCardOnBattlefield(1, "Food", isToken = true)
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardInLibrary(1, "Centaur Courser")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            val saw = game.findPermanent("Saw")!!

            game.execute(
                com.wingedsheep.engine.core.ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = saw,
                    abilityId = sawEquipId,
                    targets = listOf(ChosenTarget.Permanent(bears))
                )
            ).error shouldBe null
            game.resolveStack()

            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2))
            game.resolveStack()

            (game.getPendingDecision() is SelectCardsDecision) shouldBe true
            game.selectCards(emptyList())
            game.resolveStack()

            withClue("Food remains; nothing drawn") {
                game.findPermanents("Food").isEmpty() shouldBe false
                game.isInHand(1, "Centaur Courser") shouldBe false
            }
        }
    }
}
