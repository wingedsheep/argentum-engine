package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Brought Back (M20 #9) — {W}{W} Instant.
 *
 * "Choose up to two target permanent cards in your graveyard that were put there from the
 *  battlefield this turn. Return them to the battlefield tapped."
 *
 * The two returning cards enter together: a returned Aura picks a host already on the battlefield
 * (never the creature returning beside it, per the card's ruling) and enters tapped like the rest.
 */
class BroughtBackScenarioTest : FunSpec({

    fun driver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all)
        it.initMirrorMatch(deck = Deck.of("Plains" to 30), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.settle(onAuraHost: (ChooseTargetsDecision) -> EntityId = { it.legalTargets[0]!!.first() }) {
        var guard = 0
        while ((state.stack.isNotEmpty() || state.pendingDecision != null) && guard++ < 20) {
            val decision = state.pendingDecision
            when {
                decision is ChooseTargetsDecision ->
                    submitTargetSelection(decision.playerId, listOf(onAuraHost(decision))).error shouldBe null
                decision != null -> autoResolveDecision()
                else -> bothPass()
            }
        }
    }

    /** Pyroclasm: 2 damage to each creature — the 2/2s die, a 3/3 survives. */
    fun GameTestDriver.pyroclasm(caster: EntityId) {
        giveMana(caster, Color.RED, 2)
        castSpell(caster, putCardInHand(caster, "Pyroclasm")).outcome shouldBe Outcome.Done
        settle()
    }

    fun GameTestDriver.castBroughtBack(caster: EntityId, cards: List<EntityId>) =
        castSpellWithTargets(
            caster,
            putCardInHand(caster, "Brought Back").also { giveMana(caster, Color.WHITE, 2) },
            cards.map { ChosenTarget.Card(it, caster, Zone.GRAVEYARD) }
        )

    test("returns a creature and its Aura tapped; the Aura can't enchant the creature returning with it") {
        val d = driver()
        val me = d.player1
        val bears = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        val courser = d.putCreatureOnBattlefield(me, "Centaur Courser")
        val pacifism = d.putPermanentOnBattlefield(me, "Pacifism")
        d.addComponent(pacifism, AttachedToComponent(bears))
        d.addComponent(bears, AttachmentsComponent(listOf(pacifism)))

        d.pyroclasm(me)
        d.state.getGraveyard(me).containsAll(listOf(bears, pacifism)) shouldBe true

        d.castBroughtBack(me, listOf(bears, pacifism)).error shouldBe null
        val offered = mutableListOf<List<EntityId>>()
        d.settle { offered += it.legalTargets[0]!!; courser }

        withClue("only the surviving Courser is offered as Pacifism's host") {
            offered.flatten().contains(bears) shouldBe false
            offered.flatten().contains(courser) shouldBe true
        }
        d.state.getEntity(pacifism)?.get<AttachedToComponent>()?.targetId shouldBe courser
        withClue("both returned cards are tapped") {
            d.isTapped(bears) shouldBe true
            d.isTapped(pacifism) shouldBe true
        }
    }

    test("a card that went to the graveyard from anywhere but the battlefield can't be targeted") {
        val d = driver()
        val me = d.player1
        val stale = d.putCardInGraveyard(me, "Grizzly Bears")

        d.castBroughtBack(me, listOf(stale)).error shouldNotBe null
    }

    test("up to two: zero targets is a legal cast that does nothing") {
        val d = driver()
        val me = d.player1
        d.castBroughtBack(me, emptyList()).error shouldBe null
        d.settle()
        d.getGraveyardCardNames(me).contains("Brought Back") shouldBe true
    }

    test("a lone returned creature enters tapped under its owner's control") {
        val d = driver()
        val me = d.player1
        val bears = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        d.pyroclasm(me)

        d.castBroughtBack(me, listOf(bears)).error shouldBe null
        d.settle()

        d.findPermanent(me, "Grizzly Bears").shouldNotBeNull()
        d.isTapped(bears) shouldBe true
        d.getPermanents(me).filter { it == bears } shouldContainExactly listOf(bears)
    }
})
