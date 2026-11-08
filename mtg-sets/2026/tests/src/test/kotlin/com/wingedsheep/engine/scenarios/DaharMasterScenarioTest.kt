package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lea.cards.SolRing
import com.wingedsheep.mtg.sets.definitions.trk.cards.DaharMaster
import com.wingedsheep.mtg.sets.definitions.trk.cards.QuarkHumbleProprietor
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Dahar Master (TRK #146) — {3}{R} 4/3 Klingon Warrior.
 *
 * "When this creature enters, you may sacrifice another nonland permanent. When you do, this
 *  creature deals 2 damage to any target."
 *
 * The damage is a reflexive trigger: it exists only if a permanent was actually sacrificed, and its
 * target is chosen after the sacrifice. Any nonland permanent qualifies, not just a creature — but
 * not one that can't be sacrificed (Quark, Humble Proprietor's Treasures): it is never offered, so
 * picking it can't no-op the sacrifice and still arm the "when you do".
 */
class DaharMasterScenarioTest : FunSpec({

    fun setup(): Pair<GameTestDriver, EntityId> {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(DaharMaster, SolRing))
        d.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d to d.activePlayer!!
    }

    /** Cast Dahar Master and resolve it, leaving its enters trigger to resolve. */
    fun GameTestDriver.castMaster(me: EntityId) {
        val spell = putCardInHand(me, "Dahar Master")
        giveMana(me, Color.RED, 1)
        giveColorlessMana(me, 3)
        castSpell(me, spell).error shouldBe null
        bothPass() // the creature resolves and enters
        bothPass() // its enters trigger resolves
    }

    test("sacrificing a noncreature artifact deals 2 damage to any target") {
        val (d, me) = setup()
        val opp = d.getOpponent(me)
        val ring = d.putPermanentOnBattlefield(me, "Sol Ring")

        d.castMaster(me)

        d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        d.submitYesNo(me, true)
        d.submitTargetSelection(me, listOf(ring))
        d.submitTargetSelection(me, listOf(opp)) // the "when you do" trigger's target
        d.bothPass()

        d.getGraveyard(me).contains(ring) shouldBe true
        d.getLifeTotal(opp) shouldBe 18
    }

    test("declining the sacrifice deals no damage") {
        val (d, me) = setup()
        val opp = d.getOpponent(me)
        val ring = d.putPermanentOnBattlefield(me, "Sol Ring")

        d.castMaster(me)

        d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        d.submitYesNo(me, false)

        d.getPermanents(me).contains(ring) shouldBe true
        d.getLifeTotal(opp) shouldBe 20
    }

    test("with nothing else to sacrifice, no damage is dealt and lands can't be sacrificed") {
        val (d, me) = setup()
        val opp = d.getOpponent(me)
        val land = d.putLandOnBattlefield(me, "Mountain")

        d.castMaster(me)
        if (d.pendingDecision is YesNoDecision) d.submitYesNo(me, true)

        withClue("nothing was sacrificed, so the reflexive trigger never exists") {
            d.pendingDecision shouldBe null
            d.state.stack.isEmpty() shouldBe true
        }
        d.getPermanents(me).contains(land) shouldBe true
        d.getLifeTotal(opp) shouldBe 20
    }

    test("under Quark, the Treasure is left out of the sacrifice choice") {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + PredefinedTokens.allTokens + listOf(DaharMaster, SolRing, QuarkHumbleProprietor))
        d.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val quark = d.putCreatureOnBattlefield(me, "Quark, Humble Proprietor")
        // The Treasure token Quark's upkeep trigger mints.
        val treasure = d.putPermanentOnBattlefield(me, "Treasure").also { d.addComponent(it, TokenComponent) }
        val ring = d.putPermanentOnBattlefield(me, "Sol Ring")

        d.castMaster(me)

        d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        d.submitYesNo(me, true)
        val choice = d.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        choice.legalTargets[0]!!.toSet() shouldBe setOf(quark, ring)
        d.submitTargetSelection(me, listOf(ring))
        d.submitTargetSelection(me, listOf(opp)) // the "when you do" trigger's target
        d.bothPass()

        d.getPermanents(me).contains(treasure) shouldBe true
        d.getGraveyard(me).contains(ring) shouldBe true
        d.getLifeTotal(opp) shouldBe 18
    }
})
