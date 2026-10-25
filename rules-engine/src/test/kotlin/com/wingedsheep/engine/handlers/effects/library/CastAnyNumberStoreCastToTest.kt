package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * `CastAnyNumberFromCollectionWithoutPayingCost(storeCastTo = …)` records every card the loop
 * actually cast, for a step after the loop — "then each player who owns a spell you cast this way
 * loses life equal to its mana value" (Kefka, Dancing Mad). `Player.OwnerOfIterationEntity` names
 * that spell's owner, who differs from its controller (the caster).
 *
 * Pins the loop's three exits: no candidate left, the controller declining after a cast, and a
 * decline before any cast (an empty record, so the follow-up loop does nothing).
 */
class CastAnyNumberStoreCastToTest : FunSpec({

    val raid = card("Test Graveyard Raid") {
        manaCost = "{B}"
        typeLine = "Sorcery"
        spell {
            effect = Effects.Pipeline {
                val pool = filter(gather(CardSource.FromZone(Zone.GRAVEYARD, Player.EachOpponent)), GameObjectFilter.Nonland)
                val cast = runStoringCollection { Effects.CastAnyNumberFromCollectionWithoutPayingCost(pool, storeCastTo = it) }
                run(
                    Effects.ForEachInCollection(
                        cast,
                        Effects.LoseLife(
                            DynamicAmounts.manaValueOf(EffectTarget.IterationEntity),
                            EffectTarget.PlayerRef(Player.OwnerOfIterationEntity),
                        ),
                    )
                )
            }
        }
    }

    /** A modal spell: casting it pauses inside the cast for the mode choice (CR 601.2b). */
    val modal = card("Test Modal Probe") {
        manaCost = "{2}{G}"
        typeLine = "Sorcery"
        spell { effect = Effects.Modal(listOf(Mode.noTarget(Effects.GainLife(2)), Mode.noTarget(Effects.GainLife(4)))) }
    }

    fun setup(): Triple<GameTestDriver, com.wingedsheep.sdk.model.EntityId, com.wingedsheep.sdk.model.EntityId> {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + raid + modal)
        d.initMirrorMatch(Deck.of("Swamp" to 40))
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = d.activePlayer!!
        return Triple(d, me, d.getOpponent(me))
    }

    fun GameTestDriver.castRaid(me: com.wingedsheep.sdk.model.EntityId) {
        val spell = putCardInHand(me, "Test Graveyard Raid")
        giveMana(me, com.wingedsheep.sdk.core.Color.BLACK)
        castSpell(me, spell).error shouldBe null
        var guard = 0
        while (pendingDecision == null && stackSize > 0 && guard++ < 10) passPriority(state.priorityPlayerId!!)
    }

    test("declining after one cast records only the spell that was cast") {
        val (d, me, opp) = setup()
        val bears = d.putCardInGraveyard(opp, "Grizzly Bears")
        val courser = d.putCardInGraveyard(opp, "Centaur Courser")

        d.castRaid(me)
        d.submitCardSelection(me, listOf(bears)).error shouldBe null
        (d.pendingDecision as SelectCardsDecision).options shouldBe listOf(courser)
        d.submitCardSelection(me, emptyList()).error shouldBe null

        withClue("only Grizzly Bears (mana value 2) was cast; its owner, not its caster, loses 2") {
            d.getLifeTotal(opp) shouldBe 18
            d.getLifeTotal(me) shouldBe 20
        }
        d.getGraveyard(opp) shouldContain courser
    }

    test("casting every candidate records all of them") {
        val (d, me, opp) = setup()
        val bears = d.putCardInGraveyard(opp, "Grizzly Bears")
        val courser = d.putCardInGraveyard(opp, "Centaur Courser")

        d.castRaid(me)
        d.submitCardSelection(me, listOf(courser)).error shouldBe null
        d.submitCardSelection(me, listOf(bears)).error shouldBe null

        withClue("2 + 3 mana value, one life loss each") { d.getLifeTotal(opp) shouldBe 15 }
    }

    test("a cast that pauses inside the cast for a mode is still recorded") {
        val (d, me, opp) = setup()
        val probe = d.putCardInGraveyard(opp, "Test Modal Probe")

        d.castRaid(me)
        d.submitCardSelection(me, listOf(probe)).error shouldBe null
        (d.pendingDecision is ChooseOptionDecision) shouldBe true
        d.submitDecision(me, OptionChosenResponse(d.pendingDecision!!.id, 0)).error shouldBe null

        withClue("the modal spell (mana value 3) was cast; its owner loses 3") { d.getLifeTotal(opp) shouldBe 17 }
    }

    test("declining before any cast records nothing") {
        val (d, me, opp) = setup()
        d.putCardInGraveyard(opp, "Grizzly Bears")

        d.castRaid(me)
        d.submitCardSelection(me, emptyList()).error shouldBe null

        d.getLifeTotal(opp) shouldBe 20
        d.getLifeTotal(me) shouldBe 20
    }
})
