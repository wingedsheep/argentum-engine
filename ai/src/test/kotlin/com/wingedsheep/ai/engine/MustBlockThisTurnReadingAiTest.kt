package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.CombatReach
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * "Target creature blocks this turn if able" ([com.wingedsheep.sdk.scripting.effects.MarkMustBlockThisTurnEffect]):
 * the attacker's half of a lure, dragging an opposing creature into a block it would not choose.
 * Its slot reads [TargetPolarity.HARMFUL], it is a [IntentTag.COMBAT_ENHANCEMENT], and an
 * activation of it is a combat response ([CombatReach.FIGHT]) the combat simulation tries.
 *
 * The decision it changes: with the opponent's only creature hexproof, Peema Aether-Seer's
 * "Pay {E}{E}{E}" can only aim at our own creatures, so the AI no longer spends its energy on it.
 */
class MustBlockThisTurnReadingAiTest : ScenarioTestBase() {

    private fun card(name: String) = cardRegistry.requireCard(name)

    init {
        test("a forced block reads as harmful to its target, a combat enhancement and a combat response") {
            val peema = card("Peema Aether-Seer")
            val ability = peema.script.activatedAbilities.single()
            TargetPolarityAnalyzer.forAbility(peema, ability) shouldBe listOf(TargetPolarity.HARMFUL)
            CardIntentAnalyzer.analyzeEffect(ability.effect, ability.targetRequirements).tags shouldContain IntentTag.COMBAT_ENHANCEMENT
            CombatReach.of(ability) shouldContain CombatReach.FIGHT
            for (name in listOf("Peema Aether-Seer", "Culvert Ambusher", "Shessra, Death's Whisper", "Academic Dispute")) {
                val reading = AiReadingAudit.read(card(name))
                withClue("$name: $reading") { reading.flags.none { "MarkMustBlockThisTurnEffect" in it.key } shouldBe true }
            }
            withClue("Academic Dispute's reach lets the forced blocker meet our flier; it is no gift") {
                TargetPolarityAnalyzer.forSpell(card("Academic Dispute")) shouldBe listOf(TargetPolarity.HARMFUL)
            }
        }

        test("Peema's forced block is not aimed at our own creature when theirs can't be targeted") {
            val game = scenario()
                .withPlayers()
                .withActivePlayer(1)
                .withCardOnBattlefield(1, "Peema Aether-Seer")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Gladecover Scout")
                .build()
                .advanceToPriority(1, Step.PRECOMBAT_MAIN)
            game.state = game.state.updateEntity(game.player1Id) { container ->
                val current = container.get<CountersComponent>() ?: CountersComponent()
                container.with(current.withAdded(CounterType.ENERGY, 3))
            }
            val peemaId = game.findPermanent("Peema Aether-Seer")!!
            val action = GameSimulator(cardRegistry).getLegalActions(game.state, game.player1Id).first {
                (it.action as? ActivateAbility)?.sourceId == peemaId
            }

            withClue("the hexproof Scout leaves only our own creatures to force into a block") {
                TargetSelection.aimsOnlyAtTheWrongSide(game.state, action, game.player1Id, IntentCatalog.of(cardRegistry)) shouldBe true
            }
        }
    }
}
