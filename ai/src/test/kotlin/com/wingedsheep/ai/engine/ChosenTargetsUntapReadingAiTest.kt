package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AbilityCost
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * A pipeline that gathers its own chosen targets and taps or untaps the lot
 * ([com.wingedsheep.sdk.scripting.effects.TapUntapCollectionEffect] over `CardSource.ChosenTargets`)
 * — Nissa, Genesis Mage's "+2: Untap up to two target creatures and up to two target lands". Each
 * slot reads as the single-target untap does: a gift, so the AI aims it at its own tapped
 * permanents rather than handing the opponent a blocker or mana back. Her −10, which puts creature
 * and land cards off the library top onto the battlefield, reads as card advantage.
 */
class ChosenTargetsUntapReadingAiTest : ScenarioTestBase() {

    private val nissa by lazy { cardRegistry.getCard("Nissa, Genesis Mage")!! }
    private fun loyalty(change: Int) =
        nissa.script.activatedAbilities.single { (it.cost as? AbilityCost.Loyalty)?.change == change }

    init {
        test("the +2 untap helps every slot; the −10 dig is card advantage; Nissa reads whole") {
            val plusTwo = loyalty(2)
            TargetPolarityAnalyzer.forRequirements(plusTwo.effect, plusTwo.targetRequirements) shouldBe
                listOf(TargetPolarity.BENEFICIAL, TargetPolarity.BENEFICIAL)
            CardIntentAnalyzer.analyzeEffect(plusTwo.effect, plusTwo.targetRequirements).tags shouldContain IntentTag.RAMP
            CardIntentAnalyzer.analyzeEffect(loyalty(-10).effect).tags shouldContain IntentTag.DRAW
            AiReadingAudit.read(nissa).abilities.flatMap { it.findings }.filterNot { it.read }.shouldBeEmpty()
        }

        test("Nissa's +2 untaps the AI's own tapped creature and land, and leaves the spare slots empty") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Nissa, Genesis Mage")
                .withCardOnBattlefield(1, "Grizzly Bears", tapped = true)
                .withCardOnBattlefield(1, "Forest", tapped = true)
                .withCardOnBattlefield(2, "Hill Giant", tapped = true)
                .withCardOnBattlefield(2, "Mountain", tapped = true)
                .withCardOnBattlefield(2, "Mountain", tapped = true)
                .apply { repeat(8) { withCardInLibrary(1, "Forest") } }
                .apply { repeat(8) { withCardInLibrary(2, "Mountain") } }
                .build()
            val nissaId = game.findPermanent("Nissa, Genesis Mage")!!
            game.state = game.state.updateEntity(nissaId) { c ->
                c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.LOYALTY, 5))
            }
            val ready = game.advanceToPriority(1, Step.PRECOMBAT_MAIN)

            val action = AIPlayer.create(cardRegistry, ready.player1Id, AiProfile.LIVE).chooseAction(ready.state)
            withClue("chose $action — the +2 is the play: it grows Nissa and readies our Bears and Forests") {
                (action as? ActivateAbility)?.abilityId shouldBe loyalty(2).id
            }
            val targets = (action as ActivateAbility).targets.filterIsInstance<ChosenTarget.Permanent>().map { it.entityId }
            val controllers = targets.map { ready.state.getEntity(it)?.get<ControllerComponent>()?.playerId }
            // Two land slots and one land of ours: the second slot stays empty rather than handing
            // the opponent a Mountain (or their Hill Giant) back.
            withClue("untapped $targets controlled by $controllers") {
                targets.toSet() shouldBe setOf(ready.findPermanent("Grizzly Bears")!!, ready.findPermanent("Forest")!!)
            }

            // The heuristic fill (the AI's own fallback and its fast profiles) has no simulation to
            // lean on: only the slots' reading keeps it off the opponent's tapped permanents.
            val plusTwo = GameSimulator(cardRegistry).getLegalActions(ready.state, ready.player1Id)
                .first { (it.action as? ActivateAbility)?.abilityId == loyalty(2).id }
            val filled = TargetSelection.fillHeuristically(
                ready.state, plusTwo, ready.player1Id, fillPartialRequirements = true,
                intents = IntentCatalog.of(cardRegistry), polarityFromEffect = true,
            ) as ActivateAbility
            val picked = filled.targets.filterIsInstance<ChosenTarget.Permanent>().map { it.entityId }
            withClue("heuristic fill untapped $picked") {
                picked.toSet() shouldBe setOf(ready.findPermanent("Grizzly Bears")!!, ready.findPermanent("Forest")!!)
            }
        }
    }
}
