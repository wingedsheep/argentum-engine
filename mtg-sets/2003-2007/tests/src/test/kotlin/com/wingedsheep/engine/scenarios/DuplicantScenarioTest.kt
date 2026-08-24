package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mrd.cards.Bonesplitter
import com.wingedsheep.mtg.sets.definitions.mrd.cards.Duplicant
import com.wingedsheep.mtg.sets.definitions.mrd.cards.MarchOfTheMachines
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Duplicant (MRD #165) — "Imprint — When this creature enters, you may exile target nontoken
 * creature. As long as a card exiled with this creature is a creature card, this creature has the
 * power, toughness, and creature types of the last creature card exiled with it. It's still a
 * Shapeshifter."
 *
 * Both halves of the second sentence are dynamic reads of the linked-exile pile, so what these
 * tests pin is that the gate and the two layers agree:
 *  - an imprinted creature card replaces Duplicant's P/T (Layer 7b) *and* its creature types
 *    (Layer 4), while Shapeshifter survives;
 *  - the P/T is a *base* set, so counters still apply on top (the printed ruling);
 *  - declining the imprint leaves the printed 2/4 Shapeshifter — the dynamic read fails closed;
 *  - the gate reads the exiled **card**, not the permanent that was exiled: a noncreature artifact
 *    animated by March of the Machines is a legal target, but the card in exile is not a creature
 *    card, so neither half applies.
 */
class DuplicantScenarioTest : FunSpec({

    val allTestCards = TestCards.all

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(allTestCards + Duplicant + MarchOfTheMachines + Bonesplitter)
        d.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    /**
     * Cast Duplicant from hand so the imprint trigger fires, choose [victim] as its target (when
     * given the choice) and answer the "you may" with [accept]. Returns Duplicant on the
     * battlefield. Both decisions are handled by whichever arrives first — an optional trigger that
     * also targets asks for consent and for the target in an order the harness shouldn't assume.
     */
    fun GameTestDriver.castDuplicant(victim: EntityId, accept: Boolean): EntityId {
        val inHand = putCardInHand(player1, "Duplicant")
        giveColorlessMana(player1, 6)
        castSpell(player1, inHand).error shouldBe null

        repeat(12) {
            when (state.pendingDecision) {
                null -> bothPass()
                is ChooseTargetsDecision ->
                    submitTargetSelection(player1, listOf(victim)).error shouldBe null
                is YesNoDecision -> submitYesNo(player1, accept).error shouldBe null
                else -> error("unexpected decision ${state.pendingDecision}")
            }
        }
        return findPermanent(player1, "Duplicant")!!
    }

    test("an imprinted creature card lends Duplicant its power, toughness, and creature types") {
        val d = driver()
        val courser = d.putCreatureOnBattlefield(d.player2, "Centaur Courser") // 3/3 Centaur Warrior
        val duplicant = d.castDuplicant(courser, accept = true)

        withClue("the Courser was exiled with Duplicant") {
            d.getExileCardNames(d.player2) shouldBe listOf("Centaur Courser")
        }
        withClue("Layer 7b: base P/T comes from the exiled card, not the printed 2/4") {
            d.state.projectedState.getPower(duplicant) shouldBe 3
            d.state.projectedState.getToughness(duplicant) shouldBe 3
        }
        withClue("Layer 4: the exiled card's creature types replace Duplicant's, and it's still a Shapeshifter") {
            d.state.projectedState.getSubtypes(duplicant) shouldBe setOf("Centaur", "Warrior", "Shapeshifter")
        }
    }

    test("the copied P/T is a base set, so counters still apply on top of it") {
        val d = driver()
        val courser = d.putCreatureOnBattlefield(d.player2, "Centaur Courser")
        val duplicant = d.castDuplicant(courser, accept = true)
        d.addComponent(duplicant, CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 1)))

        withClue("3/3 from the exiled card in Layer 7b, +1/+1 from the counter in Layer 7d") {
            d.state.projectedState.getPower(duplicant) shouldBe 4
            d.state.projectedState.getToughness(duplicant) shouldBe 4
        }
    }

    test("declining the imprint leaves the printed 2/4 Shapeshifter") {
        val d = driver()
        val courser = d.putCreatureOnBattlefield(d.player2, "Centaur Courser")
        val duplicant = d.castDuplicant(courser, accept = false)

        withClue("nothing was exiled") {
            d.getExileCardNames(d.player2) shouldBe emptyList()
        }
        withClue("an empty pile means the gate is false and neither half applies") {
            d.state.projectedState.getPower(duplicant) shouldBe 2
            d.state.projectedState.getToughness(duplicant) shouldBe 4
            d.state.projectedState.getSubtypes(duplicant) shouldBe setOf("Shapeshifter")
        }
    }

    test("an animated noncreature artifact is a legal target, but its card isn't a creature card") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player2, "March of the Machines")
        val bonesplitter = d.putPermanentOnBattlefield(d.player2, "Bonesplitter")

        withClue("March makes the Equipment a creature, so Duplicant can target it") {
            d.state.projectedState.isCreature(bonesplitter) shouldBe true
        }

        val duplicant = d.castDuplicant(bonesplitter, accept = true)

        withClue("it really was exiled") {
            d.getExileCardNames(d.player2) shouldBe listOf("Bonesplitter")
        }
        withClue(
            "\"as long as a card exiled with this creature is a creature card\" reads the card in " +
                "exile, where March's animation no longer reaches — so Duplicant stays a 2/4 Shapeshifter"
        ) {
            d.state.projectedState.getPower(duplicant) shouldBe 2
            d.state.projectedState.getToughness(duplicant) shouldBe 4
            d.state.projectedState.getSubtypes(duplicant) shouldBe setOf("Shapeshifter")
        }
    }
})
