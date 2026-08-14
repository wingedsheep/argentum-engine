package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.replacement.PendingGameEvent
import com.wingedsheep.engine.replacement.ProcessorResult
import com.wingedsheep.engine.replacement.ReplacementEffectProcessor
import com.wingedsheep.engine.replacement.ReplacementOutcome
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.ReplacementEffectSourceComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.ModifyDrawAmount
import com.wingedsheep.sdk.scripting.ReplaceDrawWithEffect
import com.wingedsheep.sdk.scripting.ReplacementEffect
import com.wingedsheep.sdk.scripting.ReplacementPriorityGroup
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * CR 616.1 hands the choice between competing replacement effects to the affected player.
 *
 * Two halves are pinned here. First the choice itself: when the order is observable the
 * processor must pause, and when it isn't it must not — a prompt whose options are
 * indistinguishable is one the player cannot answer. Second the text the choice is rendered
 * with, since `GatheredReplacement.description` is what a `ChooseOptionDecision` lists.
 *
 * The 616.1a–d groups have no coverage through `process` because no shipped effect classifies
 * itself above [ReplacementPriorityGroup.ANY] yet and only the draw domain is wired to a
 * [PendingGameEvent]. What *is* pinned is the enum's declaration order, which
 * `ReplacementEffectProcessor` walks with `enumEntries` to get 616.1a→e precedence — reordering
 * it silently changes which effect wins.
 */
class ReplacementChoiceTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.initMirrorMatch(deck = Deck.of("Plains" to 20))
        return d
    }

    fun GameState.withReplacementPermanent(
        controllerId: EntityId,
        name: String,
        effect: ReplacementEffect
    ): GameState {
        val permanentId = EntityId.generate()
        val container = ComponentContainer.of(
            CardComponent(
                cardDefinitionId = name,
                name = name,
                manaCost = ManaCost.ZERO,
                typeLine = TypeLine.parse("Enchantment"),
                oracleText = effect.description,
                colors = emptySet(),
                ownerId = controllerId,
            ),
            OwnerComponent(controllerId),
            ControllerComponent(controllerId),
            ReplacementEffectSourceComponent(listOf(effect))
        )
        return withEntity(permanentId, container)
            .addToZone(ZoneKey(controllerId, Zone.BATTLEFIELD), permanentId)
    }

    test("priority group declaration order is the CR 616.1a→e sequence") {
        // ReplacementEffectProcessor.processInternal iterates enumEntries and returns on the
        // first group with any members, so this list *is* the precedence rule. It has no other
        // enforcement — a reordered enum compiles and quietly resolves 616.1 wrong.
        ReplacementPriorityGroup.entries.map { it.name } shouldBe listOf(
            "SELF_REPLACEMENT",  // CR 616.1a
            "CONTROL_CHANGE",    // CR 616.1b
            "COPY",              // CR 616.1c
            "TRANSFORM",         // CR 616.1d
            "ANY",               // CR 616.1e
        )
    }

    test("two competing ANY-group effects from different sources present a choice (CR 616.1e)") {
        val d = driver()
        val me = d.player1

        var state = d.state
        state = state.withReplacementPermanent(
            me, "Replacer A", ReplaceDrawWithEffect(replacementEffect = DrawCardsEffect(1))
        )
        state = state.withReplacementPermanent(
            me, "Replacer B", ReplaceDrawWithEffect(replacementEffect = DrawCardsEffect(2))
        )

        val processor = ReplacementEffectProcessor()
        val result = processor.process(state, PendingGameEvent.DrawPending(me, 1), EffectContext(EntityId.generate(), me))

        result.shouldBeInstanceOf<ProcessorResult.Paused>()
        val decision = result.decision
        decision.shouldBeInstanceOf<ChooseOptionDecision>()
        decision.options.size shouldBe 2
    }

    test("two Quantum Riddlers do not prompt — the orderings are indistinguishable") {
        // Both announce the same modified total, and CR 616.1f applies the loser on the next
        // pass regardless, so every ordering ends at the same number. Prompting here asks the
        // player to pick between two options they cannot tell apart and cannot get wrong.
        val d = driver()
        val me = d.player1

        var state = d.state
        state = state.withReplacementPermanent(
            me, "Riddler A", ModifyDrawAmount(modifier = 1, appliesTo = EventPattern.DrawCardsEvent())
        )
        state = state.withReplacementPermanent(
            me, "Riddler B", ModifyDrawAmount(modifier = 1, appliesTo = EventPattern.DrawCardsEvent())
        )

        val result = ReplacementEffectProcessor().process(
            state, PendingGameEvent.DrawAmountPending(me, 1), EffectContext(EntityId.generate(), me)
        )

        result.shouldBeInstanceOf<ProcessorResult.Resolved>()
        val outcome = result.outcome
        outcome.shouldBeInstanceOf<ReplacementOutcome.Modified>()
        withClue("Both +1s still apply exactly once each (CR 614.5 + 616.1f)") {
            (outcome.modifiedEvent as PendingGameEvent.DrawAmountPending).totalCount shouldBe 3
        }
    }

    test("competing effects with different outcomes still prompt, with options told apart") {
        // Same card name on both sides, so naming the source is not enough on its own.
        val d = driver()
        val me = d.player1

        var state = d.state
        state = state.withReplacementPermanent(
            me, "Riddler", ModifyDrawAmount(modifier = 1, appliesTo = EventPattern.DrawCardsEvent())
        )
        state = state.withReplacementPermanent(
            me, "Riddler", ModifyDrawAmount(multiplier = 3, appliesTo = EventPattern.DrawCardsEvent())
        )

        val result = ReplacementEffectProcessor().process(
            state, PendingGameEvent.DrawAmountPending(me, 2), EffectContext(EntityId.generate(), me)
        )

        result.shouldBeInstanceOf<ProcessorResult.Paused>()
        val decision = result.decision
        decision.shouldBeInstanceOf<ChooseOptionDecision>()
        withClue("(2+1)*3 = 9 vs (2*3)+1 = 7 — the order is observable, so the player must pick") {
            decision.options.size shouldBe 2
        }
        withClue("Options the player cannot distinguish are options they cannot answer: ${decision.options}") {
            decision.options.toSet().size shouldBe 2
        }
    }

    test("the replacement choice a player reads names the card it came from") {
        val d = driver()
        val me = d.player1

        var state = d.state
        state = state.withReplacementPermanent(
            me, "Replacer A", ReplaceDrawWithEffect(replacementEffect = DrawCardsEffect(1))
        )
        state = state.withReplacementPermanent(
            me, "Replacer B", ReplaceDrawWithEffect(replacementEffect = DrawCardsEffect(2))
        )

        val result = ReplacementEffectProcessor().process(
            state, PendingGameEvent.DrawPending(me, 1), EffectContext(EntityId.generate(), me)
        )
        result.shouldBeInstanceOf<ProcessorResult.Paused>()
        val decision = result.decision
        decision.shouldBeInstanceOf<ChooseOptionDecision>()

        withClue("Rendered options: ${decision.options}") {
            decision.options.count { it.startsWith("Replacer A - ") } shouldBe 1
            decision.options.count { it.startsWith("Replacer B - ") } shouldBe 1
        }
    }

    test("the replacement choice a player reads has no dangling 'while' clause") {
        val d = driver()
        val me = d.player1

        var state = d.state
        state = state.withReplacementPermanent(
            me, "Replacer A", ReplaceDrawWithEffect(replacementEffect = DrawCardsEffect(1))
        )
        state = state.withReplacementPermanent(
            me, "Replacer B", ReplaceDrawWithEffect(replacementEffect = DrawCardsEffect(2))
        )

        val result = ReplacementEffectProcessor().process(state, PendingGameEvent.DrawPending(me, 1), EffectContext(EntityId.generate(), me))
        result.shouldBeInstanceOf<ProcessorResult.Paused>()
        val decision = result.decision
        decision.shouldBeInstanceOf<ChooseOptionDecision>()

        for (option in decision.options) {
            withClue("Player-facing choice option: \"$option\"") {
                option shouldNotContain " while ,"
            }
        }
    }
})
