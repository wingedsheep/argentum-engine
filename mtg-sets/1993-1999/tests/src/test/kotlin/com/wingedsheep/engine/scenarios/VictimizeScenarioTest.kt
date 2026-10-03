package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.usg.cards.Victimize
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Victimize (USG #166) — {2}{B} Sorcery.
 *
 * "Choose two target creature cards in your graveyard. Sacrifice a creature. If you do, return the
 *  chosen cards to the battlefield tapped."
 */
class VictimizeScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(Victimize))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.cast(you: EntityId, a: EntityId, b: EntityId) {
        val spell = putCardInHand(you, "Victimize")
        giveMana(you, Color.BLACK, 1)
        giveColorlessMana(you, 2)
        castSpellWithTargets(
            you, spell,
            listOf(
                ChosenTarget.Card(a, you, Zone.GRAVEYARD),
                ChosenTarget.Card(b, you, Zone.GRAVEYARD),
            )
        )
    }

    test("sacrifice a chosen creature, then both targeted cards return tapped") {
        val d = createDriver()
        val you = d.activePlayer!!
        val bears = d.putCardInGraveyard(you, "Grizzly Bears")
        val courser = d.putCardInGraveyard(you, "Centaur Courser")
        val lions = d.putCreatureOnBattlefield(you, "Savannah Lions")
        val other = d.putCreatureOnBattlefield(you, "Grizzly Bears")

        d.cast(you, bears, courser)
        d.bothPass()

        withClue("the sacrifice is chosen at resolution") {
            (d.pendingDecision is SelectCardsDecision) shouldBe true
        }
        d.submitCardSelection(you, listOf(lions))

        withClue("the sacrificed creature is in the graveyard, the other one stays") {
            d.getGraveyard(you).contains(lions) shouldBe true
            d.getCreatures(you).contains(other) shouldBe true
        }
        withClue("both targeted cards are on the battlefield, tapped") {
            d.getCreatures(you).contains(bears) shouldBe true
            d.getCreatures(you).contains(courser) shouldBe true
            d.isTapped(bears) shouldBe true
            d.isTapped(courser) shouldBe true
        }
    }

    test("no creature to sacrifice: nothing returns") {
        val d = createDriver()
        val you = d.activePlayer!!
        val bears = d.putCardInGraveyard(you, "Grizzly Bears")
        val courser = d.putCardInGraveyard(you, "Centaur Courser")

        d.cast(you, bears, courser)
        d.bothPass()

        d.getCreatures(you).size shouldBe 0
        d.getGraveyard(you).contains(bears) shouldBe true
        d.getGraveyard(you).contains(courser) shouldBe true
    }

    test("one target left the graveyard: still sacrifice, the other card returns") {
        val d = createDriver()
        val you = d.activePlayer!!
        val bears = d.putCardInGraveyard(you, "Grizzly Bears")
        val courser = d.putCardInGraveyard(you, "Centaur Courser")
        val lions = d.putCreatureOnBattlefield(you, "Savannah Lions")

        d.cast(you, bears, courser)
        d.replaceState(d.state.moveToZone(bears, ZoneKey(you, Zone.GRAVEYARD), ZoneKey(you, Zone.EXILE)))
        d.bothPass()

        withClue("the single creature is sacrificed") {
            d.getGraveyard(you).contains(lions) shouldBe true
        }
        withClue("the still-legal target returns tapped; the exiled one stays in exile") {
            d.getCreatures(you) shouldBe listOf(courser)
            d.isTapped(courser) shouldBe true
            d.getExile(you).contains(bears) shouldBe true
        }
    }
})
