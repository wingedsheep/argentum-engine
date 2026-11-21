package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.CombatReach
import com.wingedsheep.ai.engine.knowledge.HoldPolicy
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.ai.engine.knowledge.TimingVerdict
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.ai.engine.knowledge.ExpiringGrantWindow
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * "Target creature blocks this turn if able" ([com.wingedsheep.sdk.scripting.effects.MarkMustBlockEffect]):
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
            for (name in listOf("Peema Aether-Seer", "Culvert Ambusher", "Shessra, Death's Whisper", "Boros Battleshaper")) {
                val reading = AiReadingAudit.read(card(name))
                withClue("$name: $reading") { reading.flags.none { "MarkMustBlockEffect" in it.key } shouldBe true }
            }
            // A lure *spell* beside a draw or Learn is judged by cantripLeverBuysNothing
            // (CantripLeverTimingAiTest); Hustle's, beside its forced attack, by castBuysNothing on
            // the half cast (SplitFaceCastTimingAiTest).
            for (name in listOf("Academic Dispute", "Timely Interference", "Hustle // Bustle")) {
                val reading = AiReadingAudit.read(card(name))
                withClue("$name: $reading") { reading.flags.none { "MarkMustBlockEffect" in it.key } shouldBe true }
            }
            withClue("Academic Dispute's reach lets the forced blocker meet our flier; it is no gift") {
                TargetPolarityAnalyzer.forSpell(card("Academic Dispute")) shouldBe listOf(TargetPolarity.HARMFUL)
            }
        }

        test("Peema's forced block buys something only on our turn, before blocks") {
            fun verdict(activePlayer: Int): TimingVerdict {
                val game = scenario()
                    .withPlayers()
                    .withActivePlayer(activePlayer)
                    .withCardOnBattlefield(1, "Peema Aether-Seer")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .build()
                    .advanceToPriority(1, Step.PRECOMBAT_MAIN)
                val peema = game.findPermanent("Peema Aether-Seer")!!
                val intents = IntentCatalog.of(cardRegistry)
                return HoldPolicy(intents, refuseWastedPayoffs = true).verdictFor(
                    game.state, game.player1Id, "Peema Aether-Seer",
                    activation = ActivateAbility(
                        game.player1Id, peema, intents.definition("Peema Aether-Seer")!!.script.activatedAbilities.single().id,
                        targets = listOf(ChosenTarget.Permanent(game.findPermanent("Grizzly Bears")!!)),
                    ),
                )
            }
            withClue("on their turn their creature has nothing of ours to block") { verdict(activePlayer = 2) shouldBe TimingVerdict.NoWindow }
            withClue("on our turn before blocks the lure is live") { verdict(activePlayer = 1) shouldNotBe TimingVerdict.NoWindow }
        }

        test("a lure spell, with reach for the forced blocker, buys nothing on their turn") {
            fun buysNothing(activePlayer: Int, step: Step): Boolean {
                val game = scenario()
                    .withPlayers()
                    .withActivePlayer(activePlayer)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .build()
                    .advanceToPriority(1, step)
                val theirs = game.state.getBattlefield().first { game.state.projectedState.getController(it) == game.player2Id }
                val lure = Effects.MarkMustBlockThisTurn(EffectTarget.ContextTarget(0)) then
                    Effects.GrantKeyword(Keyword.REACH, EffectTarget.ContextTarget(0))
                return ExpiringGrantWindow.castBuysNothing(
                    game.state, game.player1Id, listOf(lure), listOf(ChosenTarget.Permanent(theirs)),
                    IntentCatalog.of(cardRegistry),
                )
            }
            withClue("their turn: their creature has no attack of ours to block") { buysNothing(2, Step.PRECOMBAT_MAIN) shouldBe true }
            withClue("our turn before blocks: the lure is live") { buysNothing(1, Step.PRECOMBAT_MAIN) shouldBe false }
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
