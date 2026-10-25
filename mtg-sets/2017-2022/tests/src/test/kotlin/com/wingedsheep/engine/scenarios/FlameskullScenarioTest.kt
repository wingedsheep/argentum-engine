package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.afr.cards.Flameskull
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Flameskull — "Rejuvenation — When this creature dies, exile it. If you do, exile the top card of
 * your library. Until the end of your next turn, you may play one of those cards."
 *
 * The pair (Flameskull + the top card) shares one play: casting either locks the other, and a card
 * that was already in exile is never swept into the grant.
 */
class FlameskullScenarioTest : FunSpec({

    val bear = card("Test Bear") {
        manaCost = "{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }

    fun setup(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(Flameskull, bear))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 20))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.canCast(playerId: EntityId, cardId: EntityId): Boolean =
        legalActions(playerId).any { (it.action as? CastSpell)?.cardId == cardId }

    /** Kill Flameskull with a Bolt and resolve the Rejuvenation trigger; returns the exiled top card. */
    fun GameTestDriver.killFlameskull(me: EntityId, skull: EntityId): EntityId {
        val top = putCardOnTopOfLibrary(me, "Test Bear")
        val bolt = putCardInHand(me, "Lightning Bolt")
        giveMana(me, Color.RED, 1)
        castSpellWithTargets(me, bolt, listOf(ChosenTarget.Permanent(skull))).error shouldBe null
        bothPass() // Bolt resolves, Flameskull dies, Rejuvenation triggers
        bothPass() // Rejuvenation resolves
        withClue("Flameskull and the top card are both exiled") {
            getExile(me).contains(skull) shouldBe true
            getExile(me).contains(top) shouldBe true
        }
        return top
    }

    test("casting the exiled top card forfeits Flameskull") {
        val d = setup()
        val me = d.activePlayer!!
        val oldExile = d.putCardInExile(me, "Test Bear")
        val skull = d.putCreatureOnBattlefield(me, "Flameskull")
        val top = d.killFlameskull(me, skull)

        d.giveMana(me, Color.RED, 3)
        d.giveMana(me, Color.GREEN, 2)
        withClue("Either card of the pair is playable; a card already in exile is not") {
            d.canCast(me, skull) shouldBe true
            d.canCast(me, top) shouldBe true
            d.canCast(me, oldExile) shouldBe false
        }

        d.submit(CastSpell(me, top, paymentStrategy = PaymentStrategy.FromPool)).outcome shouldBe Outcome.Done
        d.bothPass()

        withClue("The one play is spent: Flameskull stays in exile, unplayable") {
            d.findPermanent(me, "Test Bear") shouldBe top
            d.canCast(me, skull) shouldBe false
            d.getExile(me).contains(skull) shouldBe true
        }
    }

    test("casting Flameskull from exile forfeits the other card") {
        val d = setup()
        val me = d.activePlayer!!
        val skull = d.putCreatureOnBattlefield(me, "Flameskull")
        val top = d.killFlameskull(me, skull)

        d.giveMana(me, Color.RED, 3)
        d.giveMana(me, Color.GREEN, 1)
        d.submit(CastSpell(me, skull, paymentStrategy = PaymentStrategy.FromPool)).outcome shouldBe Outcome.Done
        d.bothPass()

        withClue("Flameskull is back; the top card stays in exile, unplayable") {
            d.findPermanent(me, "Flameskull") shouldBe skull
            d.canCast(me, top) shouldBe false
            d.getExile(me).contains(top) shouldBe true
        }
    }
})
