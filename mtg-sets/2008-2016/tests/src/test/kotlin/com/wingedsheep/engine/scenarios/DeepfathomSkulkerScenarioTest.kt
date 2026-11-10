package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Deepfathom Skulker — {5}{U} 4/4 Eldrazi. "Devoid. Whenever a creature you control deals combat
 * damage to a player, you may draw a card. {3}{C}: Target creature can't be blocked this turn."
 */
class DeepfathomSkulkerScenarioTest : ScenarioTestBase() {
    init {
        test("another creature you control connecting lets you draw a card") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Deepfathom Skulker")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInLibrary(1, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.state.projectedState.getColors(game.findPermanent("Deepfathom Skulker")!!) shouldBe emptySet()
            val handBefore = game.handSize(1)

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareNoBlockers()
            game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)

            var guard = 0
            while (game.state.pendingDecision !is YesNoDecision && guard++ < 20) game.resolveStack()
            withClue("the draw is optional") { (game.state.pendingDecision is YesNoDecision) shouldBe true }
            game.answerYesNo(true).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 18
            game.handSize(1) shouldBe handBefore + 1
        }

        test("an opponent's creature connecting doesn't trigger it") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Deepfathom Skulker", tapped = true)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val handBefore = game.handSize(1)

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 1)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareNoBlockers()
            game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)

            game.getLifeTotal(1) shouldBe 18
            withClue("only creatures Skulker's controller controls make it trigger") {
                (game.state.pendingDecision is YesNoDecision) shouldBe false
                game.handSize(1) shouldBe handBefore
            }
        }

        test("{3}{C}: target creature can't be blocked this turn") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Deepfathom Skulker")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Wastes", 1)
                .withLandsOnBattlefield(1, "Island", 3)
                .withCardOnBattlefield(2, "Hill Giant")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val skulker = game.findPermanent("Deepfathom Skulker")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            val abilityId = cardRegistry.getCard("Deepfathom Skulker")!!.script.activatedAbilities.single().id

            val result = game.execute(
                ActivateAbility(game.player1Id, skulker, abilityId, listOf(ChosenTarget.Permanent(bears)))
            )
            withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
            game.resolveStack()
            game.state.projectedState.hasKeyword(bears, AbilityFlag.CANT_BE_BLOCKED) shouldBe true

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            withClue("Hill Giant can't block the unblockable Bears") {
                game.declareBlockers(mapOf("Hill Giant" to listOf("Grizzly Bears"))).error shouldNotBe null
            }
        }
    }
}
