package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * A modal triggered ability's mode with an "up to X target" count (`dynamicMaxCount`): the count
 * is evaluated as that mode's targets are chosen, while the ability goes on the stack (CR 603.3d
 * applying 601.2c), exactly as a non-modal trigger's is — the target decision offers X slots, and
 * the stack object keeps the locked count. Orcus, Prince of Undeath is the card that needs it.
 */
class ModalTriggerDynamicTargetCountTest : ScenarioTestBase() {

    /** "When this enters, choose one — • Tap up to X target creatures, where X is the number of lands you control. • You gain 1 life." */
    private val tapper = card("Test Modal Dynamic Tapper") {
        manaCost = "{0}"
        typeLine = "Artifact Creature — Construct"
        power = 1
        toughness = 1
        oracleText = "When this creature enters, choose one —\n" +
            "• Tap up to X target creatures, where X is the number of lands you control.\n" +
            "• You gain 1 life."
        triggeredAbility {
            trigger = Triggers.self.enters()
            effect = ModalEffect.chooseOne(
                mode("Tap up to X target creatures, where X is the number of lands you control") {
                    targets(TargetFilter.Creature, optional = true, dynamicMaxCount = DynamicAmounts.landsYouControl())
                    effect = Effects.TapEachTarget()
                },
                Mode.noTarget(Effects.GainLife(1), "You gain 1 life"),
            )
        }
    }

    private fun castWithLands(lands: Int): TestGame {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Test Modal Dynamic Tapper")
            .withLandsOnBattlefield(1, "Forest", lands)
            .withCardOnBattlefield(2, "Grizzly Bears")
            .withCardOnBattlefield(2, "Grizzly Bears")
            .withCardOnBattlefield(2, "Grizzly Bears")
            .withCardInLibrary(1, "Forest")
            .withCardInLibrary(2, "Forest")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        game.castSpell(1, "Test Modal Dynamic Tapper").error shouldBe null
        if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
        game.resolveStack()
        val modeQuestion = game.getPendingDecision().shouldBeInstanceOf<ChooseOptionDecision>()
        val index = modeQuestion.options.indexOfFirst { it.startsWith("Tap up to X") }
        game.submitDecision(OptionChosenResponse(modeQuestion.id, index)).error shouldBe null
        return game
    }

    private fun TestGame.bears() = state.getBattlefield().filter {
        state.getEntity(it)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == "Grizzly Bears"
    }

    init {
        cardRegistry.register(tapper)

        test("the mode's target decision offers X slots, X read as the trigger goes on the stack") {
            val game = castWithLands(2)
            val decision = game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            decision.targetRequirements.single().maxTargets shouldBe 2
            decision.targetRequirements.single().minTargets shouldBe 0

            val (first, second, third) = game.bears()
            game.selectTargets(listOf(first, second)).error shouldBe null

            val onStack = game.state.stack.firstNotNullOf {
                game.state.getEntity(it)?.get<TriggeredAbilityOnStackComponent>()
            }
            onStack.modeTargetRequirements.values.single().single().count shouldBe 2

            game.resolveStack()
            game.state.getEntity(first)!!.get<TappedComponent>().shouldNotBeNull()
            game.state.getEntity(second)!!.get<TappedComponent>().shouldNotBeNull()
            game.state.getEntity(third)!!.get<TappedComponent>().shouldBeNull()
        }

        test("more targets than X are rejected") {
            val game = castWithLands(2)
            game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            game.selectTargets(game.bears()).error shouldNotBe null
        }

        test("an X above the legal-target count still allows every legal target") {
            val game = castWithLands(5)
            val decision = game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            decision.targetRequirements.single().maxTargets shouldBe 5
            game.selectTargets(game.bears()).error shouldBe null
            game.resolveStack()
            game.bears().forEach { game.state.getEntity(it)!!.get<TappedComponent>().shouldNotBeNull() }
        }
    }
}
