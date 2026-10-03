package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh3.cards.Gravedig
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Gravedig (MH3 #96) — {1}{B} Sorcery.
 *
 * "Choose one — • Target player creates a 2/2 black Zombie creature token.
 *  • Return target creature card from your graveyard to your hand. Entwine {2}"
 */
class GravedigScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(Gravedig))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("mode 0 targeting the opponent: the opponent gets the 2/2 Zombie") {
        val d = createDriver()
        val you = d.activePlayer!!
        val opp = d.getOpponent(you)
        val spell = d.putCardInHand(you, "Gravedig")
        d.giveMana(you, Color.BLACK, 1)
        d.giveColorlessMana(you, 1)
        d.submit(
            CastSpell(
                playerId = you, cardId = spell,
                chosenModes = listOf(0),
                targets = listOf(ChosenTarget.Player(opp)),
                modeTargetsOrdered = listOf(listOf(ChosenTarget.Player(opp))),
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).outcome shouldBe Outcome.Done
        d.bothPass()

        withClue("the targeted player controls the Zombie") {
            d.findPermanent(opp, "Zombie Token") shouldNotBe null
            d.getCreatures(opp).size shouldBe 1
            d.getCreatures(you).size shouldBe 0
        }
    }

    test("mode 1: returns a creature card from your graveyard to hand") {
        val d = createDriver()
        val you = d.activePlayer!!
        val bears = d.putCardInGraveyard(you, "Grizzly Bears")
        val spell = d.putCardInHand(you, "Gravedig")
        d.giveMana(you, Color.BLACK, 1)
        d.giveColorlessMana(you, 1)
        d.submit(
            CastSpell(
                playerId = you, cardId = spell,
                chosenModes = listOf(1),
                targets = listOf(ChosenTarget.Card(bears, you, Zone.GRAVEYARD)),
                modeTargetsOrdered = listOf(listOf(ChosenTarget.Card(bears, you, Zone.GRAVEYARD))),
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).outcome shouldBe Outcome.Done
        d.bothPass()

        d.getHand(you).contains(bears) shouldBe true
        d.getCreatures(you).size shouldBe 0
    }

    test("entwined: both modes for {3}{B}, and not castable entwined for only {1}{B}") {
        val d = createDriver()
        val you = d.activePlayer!!
        val bears = d.putCardInGraveyard(you, "Grizzly Bears")
        val spell = d.putCardInHand(you, "Gravedig")

        d.giveMana(you, Color.BLACK, 1)
        d.giveColorlessMana(you, 1)
        val cheap = d.submit(
            CastSpell(
                playerId = you, cardId = spell,
                chosenModes = listOf(0, 1),
                targets = listOf(ChosenTarget.Player(you), ChosenTarget.Card(bears, you, Zone.GRAVEYARD)),
                modeTargetsOrdered = listOf(
                    listOf(ChosenTarget.Player(you)),
                    listOf(ChosenTarget.Card(bears, you, Zone.GRAVEYARD))
                ),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        withClue("entwine needs the extra {2}") { cheap.outcome shouldNotBe Outcome.Done }

        d.giveColorlessMana(you, 2)
        d.submit(
            CastSpell(
                playerId = you, cardId = spell,
                chosenModes = listOf(0, 1),
                targets = listOf(ChosenTarget.Player(you), ChosenTarget.Card(bears, you, Zone.GRAVEYARD)),
                modeTargetsOrdered = listOf(
                    listOf(ChosenTarget.Player(you)),
                    listOf(ChosenTarget.Card(bears, you, Zone.GRAVEYARD))
                ),
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).outcome shouldBe Outcome.Done
        d.bothPass()

        d.findPermanent(you, "Zombie Token") shouldNotBe null
        d.getHand(you).contains(bears) shouldBe true
    }
})
