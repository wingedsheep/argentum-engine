package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.one.cards.LukkaBoundToRuin
import com.wingedsheep.mtg.sets.definitions.rna.cards.BiogenicUpgrade
import com.wingedsheep.mtg.sets.definitions.xln.cards.HuatliWarriorPoet
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * "N damage divided as you choose among …" names no target of its own — the targets are the
 * ability's — so its slot was read as neither help nor harm and the AI aimed it blind. It is damage
 * to every target it divides among (Huatli, Warrior Poet's −X; Lukka, Bound to Ruin's −4). Its
 * counter twin, "distribute N +1/+1 counters among …", is a gift to each target and a pump, and so
 * is doubling the +1/+1 counters a target holds (Biogenic Upgrade).
 */
class AnnouncedDivisionReadingAiTest : ScenarioTestBase() {
    init {
        test("a divided-damage slot is harmful, inside a pipeline or alone") {
            TargetPolarityAnalyzer.forAbility(HuatliWarriorPoet, HuatliWarriorPoet.activatedAbilities[2]) shouldBe
                listOf(TargetPolarity.HARMFUL)
            TargetPolarityAnalyzer.forAbility(LukkaBoundToRuin, LukkaBoundToRuin.activatedAbilities[2]) shouldBe
                listOf(TargetPolarity.HARMFUL)
        }

        test("distributing and doubling +1/+1 counters is a beneficial pump") {
            TargetPolarityAnalyzer.forSpell(BiogenicUpgrade) shouldBe listOf(TargetPolarity.BENEFICIAL)
            CardIntentAnalyzer.analyzeEffect(BiogenicUpgrade.script.spellEffect!!).tags shouldContain IntentTag.PUMP
        }

        test("Lukka's −4 divides its damage onto their creature, not ours") {
            val game = scenario().withPlayers().withActivePlayer(1)
                .withCardOnBattlefield(1, LukkaBoundToRuin.name)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Savannah Lions")
                .withCardOnBattlefield(2, "Centaur Courser")
                .build().advanceToPriority(1, Step.PRECOMBAT_MAIN)
            val lukka = game.findPermanent(LukkaBoundToRuin.name)!!
            game.state = game.state.updateEntity(lukka) { it.with(CountersComponent().withAdded(CounterType.LOYALTY, 5)) }
            val minusFour = LukkaBoundToRuin.activatedAbilities[2].id
            val activate = GameSimulator(cardRegistry).getLegalActions(game.state, game.player1Id)
                .first { (it.action as? ActivateAbility)?.abilityId == minusFour }

            val filled = TargetSelection.fillHeuristically(
                game.state, activate, game.player1Id, fillPartialRequirements = true,
                intents = IntentCatalog.of(cardRegistry), polarityFromEffect = true,
            ) as ActivateAbility

            val chosen = filled.targets.map { (it as ChosenTarget.Permanent).entityId }
            chosen.isNotEmpty() shouldBe true
            chosen.all { game.state.projectedState.getController(it) == game.player2Id } shouldBe true
        }
    }
}
