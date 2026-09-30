package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Cursed Mirror (C21 #50; reprinted in MH3 #279).
 *
 * "{T}: Add {R}. / As this artifact enters, you may have it become a copy of any creature on the
 * battlefield until end of turn, except it has haste."
 */
class CursedMirrorScenarioTest : FunSpec({

    fun setup(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all)
        it.initMirrorMatch(Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.castMirror(copy: EntityId?): EntityId {
        val mirror = putCardInHand(player1, "Cursed Mirror")
        giveMana(player1, Color.RED, 3)
        castSpell(player1, mirror).error shouldBe null
        bothPass()
        state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        submitCardSelection(player1, listOfNotNull(copy)).error shouldBe null
        return mirror
    }

    fun GameTestDriver.name(id: EntityId) = state.getEntity(id)!!.get<CardComponent>()!!.name

    test("copies a creature with haste, attacks the turn it enters, and is an artifact again next turn") {
        val d = setup()
        val giant = d.putPermanentOnBattlefield(d.player2, "Hill Giant")
        val mirror = d.castMirror(giant)

        d.name(mirror) shouldBe "Hill Giant"
        d.state.projectedState.isCreature(mirror) shouldBe true
        d.state.projectedState.getPower(mirror) shouldBe 3
        d.state.projectedState.hasKeyword(mirror, Keyword.HASTE) shouldBe true

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, listOf(mirror), d.player2).error shouldBe null
        d.passPriorityUntil(Step.END_COMBAT)
        d.getLifeTotal(d.player2) shouldBe 17

        d.passPriorityUntil(Step.UPKEEP)
        d.name(mirror) shouldBe "Cursed Mirror"
        d.state.projectedState.isCreature(mirror) shouldBe false
        d.state.projectedState.hasKeyword(mirror, Keyword.HASTE) shouldBe false
        d.state.getBattlefield().contains(mirror) shouldBe true
    }

    test("declining the copy leaves a mana rock that taps for red") {
        val d = setup()
        d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        val mirror = d.castMirror(null)

        d.name(mirror) shouldBe "Cursed Mirror"
        d.state.projectedState.isCreature(mirror) shouldBe false
        d.legalActions(d.player1).any {
            it.isManaAbility && (it.action as? ActivateAbility)?.sourceId == mirror
        } shouldBe true
    }
})
