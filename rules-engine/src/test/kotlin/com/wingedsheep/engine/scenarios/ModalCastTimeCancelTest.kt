package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CancelDecisionResponse
import com.wingedsheep.engine.core.CastModalModeSelectionContinuation
import com.wingedsheep.engine.core.CastModalTargetSelectionContinuation
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards.BrigidsCommand
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests K1 / K2 from [`backlog/modal-cast-time-choices-plan.md`]: cancelling a
 * cast-time modal decision must roll back cleanly — no mode was chosen, no cost
 * was paid, the card is still in hand, and priority returns to the caster.
 *
 * The cast-time pause in [CastSpellHandler.execute] sits before cost payment
 * (rule 601.2f timing), so cancellation is a simple matter of popping the
 * continuation and restoring priority.
 *
 * - K1: cancel during mode selection (first decision).
 * - K2: cancel during target selection (after all modes are picked but before
 *   targets finalize).
 */
class ModalCastTimeCancelTest : FunSpec({

    // Simple modal card for K1 — two no-target modes so we don't trip on target
    // selection and can isolate the mode-selection cancel path.
    val NoTargetModal: CardDefinition = card("Test NoTarget Modal") {
        manaCost = "{G}"
        typeLine = "Sorcery"
        spell {
            modal(chooseCount = 2) {
                mode("Draw a card", Effects.DrawCards(1))
                mode("Gain 3 life", Effects.GainLife(3))
                mode("You lose 1 life", Effects.LoseLife(1))
            }
        }
    }

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(NoTargetModal))
        d.registerCard(BrigidsCommand)
        return d
    }

    test("K1 — cancel during mode selection returns spell to hand, drains no mana, restores priority") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Forest" to 40))
        val p1 = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        d.giveMana(p1, Color.GREEN, 1)

        // Baseline snapshots BEFORE the cast attempt — cancellation must restore these.
        val spellCard = d.putCardInHand(p1, "Test NoTarget Modal")
        val handBefore = d.state.getHand(p1).toList()
        val lifeBefore = d.state.getEntity(p1)!!.get<LifeTotalComponent>()!!.life
        val manaBefore = d.state.getEntity(p1)!!.get<ManaPoolComponent>()!!

        // Submit the CastSpell — engine pauses on mode selection. Use FromPool so we don't
        // get a detour through the mana-source selection decision path.
        d.submit(
            CastSpell(
                playerId = p1,
                cardId = spellCard,
                paymentStrategy = com.wingedsheep.engine.core.PaymentStrategy.FromPool
            )
        )

        val decision = d.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        // Sanity: continuation stack carries a CastModalModeSelectionContinuation.
        d.state.continuationStack.filterIsInstance<CastModalModeSelectionContinuation>()
            .shouldHaveSize(1)

        // Cancel without picking anything.
        d.submitDecision(p1, CancelDecisionResponse(decision.id))

        // Rollback asserts:
        //  1. No pending decision.
        d.state.pendingDecision.shouldBeNull()
        //  2. Continuation stack drained.
        d.state.continuationStack.filterIsInstance<CastModalModeSelectionContinuation>()
            .shouldHaveSize(0)
        //  3. Stack is empty (spell never reached it).
        d.state.stack shouldHaveSize 0
        //  4. Card is still in hand with the same entity id — nothing was moved/copied.
        val handAfter = d.state.getHand(p1).toList()
        handAfter shouldBe handBefore
        //  5. Mana pool unchanged (cost was never paid).
        val manaAfter = d.state.getEntity(p1)!!.get<ManaPoolComponent>()!!
        manaAfter shouldBe manaBefore
        //  6. Life unchanged, priority back with caster.
        d.state.getEntity(p1)!!.get<LifeTotalComponent>()!!.life shouldBe lifeBefore
        d.state.priorityPlayerId shouldBe p1
    }

    test("K2 — cancel during target selection also rolls back cleanly") {
        // Brigid's Command has targeted modes: after both modes are picked, engine
        // pushes a CastModalTargetSelectionContinuation and prompts for targets.
        // Cancelling at THAT stage must still restore hand / mana / priority.
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Forest" to 20, "Plains" to 20))
        val p1 = d.activePlayer!!
        val p2 = d.getOpponent(p1)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Mode 2 ("+3/+3") needs a creature you control; mode 3 ("fight") needs
        // a creature you control + an opponent creature. Set up legal targets so
        // the cast reaches target selection before we cancel it.
        d.putCreatureOnBattlefield(p1, "Centaur Courser")
        d.putCreatureOnBattlefield(p2, "Goblin Guide")

        d.giveMana(p1, Color.WHITE, 1)
        d.giveMana(p1, Color.GREEN, 1)
        d.giveColorlessMana(p1, 1)

        val commandCard = d.putCardInHand(p1, "Brigid's Command")
        val handBefore = d.state.getHand(p1).toList()
        val manaBefore = d.state.getEntity(p1)!!.get<ManaPoolComponent>()!!

        d.submit(
            CastSpell(
                playerId = p1,
                cardId = commandCard,
                paymentStrategy = com.wingedsheep.engine.core.PaymentStrategy.FromPool
            )
        )

        // First mode pick — pick mode 2 (+3/+3).
        val firstMode = d.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        val mode2Index = firstMode.options.indexOfFirst { it.startsWith("+3") || it.contains("+3/+3") }
        check(mode2Index >= 0) { "Couldn't locate mode 2 in ${firstMode.options}" }
        d.submitDecision(p1, OptionChosenResponse(firstMode.id, mode2Index))

        // Second mode pick — pick mode 3 (fight).
        val secondMode = d.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        val mode3Index = secondMode.options.indexOfFirst { it.contains("fight") }
        check(mode3Index >= 0) { "Couldn't locate mode 3 in ${secondMode.options}" }
        d.submitDecision(p1, OptionChosenResponse(secondMode.id, mode3Index))

        // Now we should be at target selection (the first chosen mode's target).
        val targetDecision = d.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        d.state.continuationStack.filterIsInstance<CastModalTargetSelectionContinuation>()
            .shouldHaveSize(1)

        // Cancel.
        d.submitDecision(p1, CancelDecisionResponse(targetDecision.id))

        // Rollback asserts:
        d.state.pendingDecision.shouldBeNull()
        d.state.continuationStack.filterIsInstance<CastModalModeSelectionContinuation>()
            .shouldHaveSize(0)
        d.state.continuationStack.filterIsInstance<CastModalTargetSelectionContinuation>()
            .shouldHaveSize(0)
        d.state.stack shouldHaveSize 0
        d.state.getHand(p1).toList() shouldBe handBefore
        d.state.getEntity(p1)!!.get<ManaPoolComponent>()!! shouldBe manaBefore
        d.state.priorityPlayerId shouldBe p1
    }
})
