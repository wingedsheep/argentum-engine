package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.legalactions.EnumerationMode
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Oathsworn Vampire (RIX #80) — {1}{B} 2/2 Creature — Vampire Knight.
 *
 * "This creature enters tapped.
 *  You may cast this card from your graveyard if you gained life this turn."
 *
 * Exercises the conditional self cast-from-graveyard permission gated on the per-player
 * life-gained turn tracker: only *your* life gain enables it, and a real life-gain spell
 * (Natural Spring) sets the tracker.
 */
class OathswornVampireScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun canCastFromGraveyard(driver: GameTestDriver, player: EntityId, cardId: EntityId): Boolean {
        val enumerator = LegalActionEnumerator.create(driver.cardRegistry)
        val actions = enumerator.enumerate(driver.state, player, EnumerationMode.FULL)
        return actions.any { it.sourceZone == "GRAVEYARD" && (it.action as? CastSpell)?.cardId == cardId }
    }

    fun GameTestDriver.castNaturalSpring(caster: EntityId, target: EntityId) {
        val spring = putCardInHand(caster, "Natural Spring")
        giveMana(caster, Color.GREEN, 5)
        castSpell(caster, spring, targets = listOf(target))
        bothPass()
    }

    test("no life gained: not castable from graveyard") {
        val d = newDriver()
        val you = d.player1
        val vampire = d.putCardInGraveyard(you, "Oathsworn Vampire")
        d.giveMana(you, Color.BLACK, 2)

        canCastFromGraveyard(d, you, vampire) shouldBe false
    }

    test("after you gain life: castable from graveyard and enters tapped") {
        val d = newDriver()
        val you = d.player1
        val vampire = d.putCardInGraveyard(you, "Oathsworn Vampire")

        d.castNaturalSpring(you, you)

        d.giveMana(you, Color.BLACK, 2)
        canCastFromGraveyard(d, you, vampire) shouldBe true

        d.castSpell(you, vampire)
        d.bothPass()

        val perm = d.findPermanent(you, "Oathsworn Vampire")!!
        d.state.getEntity(perm)?.has<TappedComponent>() shouldBe true
    }

    test("only the opponent gained life: not castable from graveyard") {
        val d = newDriver()
        val you = d.player1
        val opponent = d.getOpponent(you)
        val vampire = d.putCardInGraveyard(you, "Oathsworn Vampire")

        d.castNaturalSpring(you, opponent)

        d.giveMana(you, Color.BLACK, 2)
        canCastFromGraveyard(d, you, vampire) shouldBe false
    }
})
