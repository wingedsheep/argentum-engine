package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.drawing.DrawReplacementDispatcher
import com.wingedsheep.engine.replacement.ReplacementEffectProcessor
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
import com.wingedsheep.sdk.scripting.ReplacementEffect
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * `DrawLoop` must terminate, and what guarantees that is [ModifyDrawAmount] never reaching the
 * per-card event.
 *
 * A draw-count modification applied to
 * [com.wingedsheep.engine.replacement.PendingGameEvent.DrawPending] does not converge: the loop
 * responds to a `Modified` result with `remaining += delta; continue`, so no card is drawn and
 * nothing about the game state changes — the same effect matches again on the next iteration and
 * `remaining` only ever grows. The fix is structural rather than a guard: [ModifyDrawAmount]'s
 * `appliesTo` is typed as [EventPattern.DrawCardsEvent], the *announcement* event (CR 121.2a), so
 * pointing one at the per-card `DrawEvent` no longer compiles.
 *
 * These tests pin the runtime half of that guarantee: the announcement modification fires exactly
 * once, and the per-card dispatcher then reports `None` — the loop draws and terminates.
 *
 * Asserted at dispatcher level deliberately: were the invariant to break, driving the real
 * `DrawLoop` would spin forever rather than fail, and a kotest timeout cannot interrupt a
 * non-suspending loop.
 */
class ModifyDrawAmountTerminationTest : FunSpec({

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

    fun dispatcher() = DrawReplacementDispatcher(
        effectExecutor = { s, _, _ -> EffectResult.success(s, emptyList()) },
        processor = ReplacementEffectProcessor()
    )

    fun boardWith(effect: ReplacementEffect, name: String): Pair<GameState, EntityId> {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.initMirrorMatch(deck = Deck.of("Plains" to 40))
        val me = d.player1
        return d.state.withReplacementPermanent(me, name, effect) to me
    }

    test("the announcement-level ModifyDrawAmount applies exactly once and then stops") {
        // Quantum Riddler's shape. The announcement modification must fire, and must not fire
        // a second time against the same instruction.
        val (state, me) = boardWith(
            ModifyDrawAmount(modifier = 1, appliesTo = EventPattern.DrawCardsEvent()),
            "Announcement Riddler"
        )
        val ctx = EffectContext(EntityId.generate(), me)
        val disp = dispatcher()

        val announced = disp.checkDrawAmount(state, me, totalCount = 1, isDrawStep = false, context = ctx)
        announced.shouldBeInstanceOf<DrawReplacementDispatcher.DispatchResult.Modified>()
        withClue("Draw 1 becomes draw 2 at the announcement (CR 121.2a)") {
            announced.delta shouldBe 1
        }

        withClue(
            "CR 614.5 — a replacement gets one opportunity to affect an event or any modified " +
                "event replacing it. Re-announcing the same instruction must not add another +1."
        ) {
            // checkDrawAmount returns null when nothing modified the count.
            disp.checkDrawAmount(
                announced.state, me, totalCount = 2, isDrawStep = false, context = ctx
            ) shouldBe null
        }
    }

    test("a ModifyDrawAmount never reaches the per-card draw check, so the loop terminates") {
        // The default `appliesTo` is the announcement event, so even a ModifyDrawAmount written
        // with no explicit pattern is invisible to the per-card check. Were it visible, the
        // dispatcher would return Modified without drawing a card and DrawLoop would spin.
        val (state, me) = boardWith(ModifyDrawAmount(modifier = 1), "Default Riddler")
        val ctx = EffectContext(EntityId.generate(), me)
        val disp = dispatcher()

        withClue("A count modification must never be offered to the per-card draw event") {
            disp.checkBeforeDraw(state, me, 1, emptyList(), false, ctx)
                .shouldBeInstanceOf<DrawReplacementDispatcher.DispatchResult.None>()
        }

        // And the same holds after the announcement has already modified the count — the state
        // the loop actually starts from.
        val announced = disp.checkDrawAmount(state, me, totalCount = 1, isDrawStep = false, context = ctx)
        announced.shouldBeInstanceOf<DrawReplacementDispatcher.DispatchResult.Modified>()
        withClue(
            "The announcement effect matches DrawCardsEvent only, so the per-card loop never " +
                "sees it — which is what keeps the loop terminating."
        ) {
            disp.checkBeforeDraw(announced.state, me, 2, emptyList(), false, ctx)
                .shouldBeInstanceOf<DrawReplacementDispatcher.DispatchResult.None>()
        }
    }
})
