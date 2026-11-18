package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.isd.cards.PurifyTheGrave
import com.wingedsheep.mtg.sets.definitions.m15.cards.Phytotitan
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Phytotitan (M15 #191) — {4}{G}{G} 7/2 Creature — Plant Elemental.
 *
 * "When this creature dies, return it to the battlefield tapped under its owner's control at the
 *  beginning of their next upkeep."
 *
 * Covers the owner-gated delayed upkeep return (not the next upkeep of any player, not the
 * controller's), the tapped entry, and the ruling that a card no longer in the graveyard stays put.
 */
class PhytotitanScenarioTest : FunSpec({

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + Phytotitan + PurifyTheGrave)
        initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    /** [caster] bolts [victim]; the death and the dies trigger (scheduling the return) both resolve. */
    fun GameTestDriver.bolt(caster: EntityId, victim: EntityId) {
        val bolt = putCardInHand(caster, "Lightning Bolt")
        giveMana(caster, Color.RED, 1)
        castSpell(caster, bolt, listOf(victim)).outcome shouldBe Outcome.Done
        bothPass() // Bolt resolves; Phytotitan dies and its trigger goes on the stack
        bothPass() // the dies trigger resolves, creating the delayed trigger
    }

    test("returns tapped at the beginning of its owner's next upkeep, not the opponent's") {
        val d = driver()
        val you = d.player1
        val titan = d.putCreatureOnBattlefield(you, "Phytotitan")

        d.bolt(you, titan)
        d.getGraveyard(you) shouldContain titan

        // The opponent's upkeep comes first — "their next upkeep" means the owner's.
        d.passPriorityUntil(Step.UPKEEP)
        d.activePlayer shouldBe d.player2
        d.getGraveyard(you) shouldContain titan

        d.passPriorityUntil(Step.UPKEEP, you)
        d.bothPass() // the delayed trigger resolves

        d.getCreatures(you) shouldContain titan
        d.getGraveyard(you) shouldNotContain titan
        d.isTapped(titan) shouldBe true
        d.state.projectedState.getController(titan) shouldBe you
    }

    test("a Phytotitan exiled from the graveyard before the upkeep stays in exile") {
        val d = driver()
        val you = d.player1
        val titan = d.putCreatureOnBattlefield(you, "Phytotitan")

        d.bolt(you, titan)

        val purify = d.putCardInHand(you, "Purify the Grave")
        d.giveMana(you, Color.WHITE, 1)
        d.castSpellWithTargets(you, purify, listOf(ChosenTarget.Card(titan, you, Zone.GRAVEYARD)))
            .outcome shouldBe Outcome.Done
        d.bothPass()
        d.getExile(you) shouldContain titan

        d.passPriorityUntil(Step.UPKEEP, you)
        d.bothPass()

        d.getExile(you) shouldContain titan
        d.getCreatures(you) shouldNotContain titan
    }

    test("dying while stolen returns it under its owner's control on the owner's upkeep") {
        val d = driver()
        val owner = d.player1
        val thief = d.player2
        val titan = d.putCreatureOnBattlefield(owner, "Phytotitan")

        // Simulate a control-changing effect: the thief controls it, the owner still owns it.
        d.replaceState(d.state.updateEntity(titan) { it.with(ControllerComponent(thief)) })
        d.state.projectedState.getController(titan) shouldBe thief

        d.bolt(owner, titan)
        d.getGraveyard(owner) shouldContain titan

        // The thief's upkeep is the next upkeep, and it was the thief's trigger — still nothing.
        d.passPriorityUntil(Step.UPKEEP)
        d.activePlayer shouldBe thief
        d.getGraveyard(owner) shouldContain titan

        d.passPriorityUntil(Step.UPKEEP, owner)
        d.bothPass()

        d.getCreatures(owner) shouldContain titan
        d.isTapped(titan) shouldBe true
        d.state.projectedState.getController(titan) shouldBe owner
    }
})
