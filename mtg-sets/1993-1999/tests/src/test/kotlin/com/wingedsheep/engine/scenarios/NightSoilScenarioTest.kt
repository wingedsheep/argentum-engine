package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fem.cards.NightSoil
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * Tests for Night Soil (Fallen Empires).
 *
 * Night Soil: {G}{G}
 * Enchantment
 * {1}, Exile two creature cards from a single graveyard: Create a 1/1 green Saproling creature token.
 *
 * The cost is the point. "A single graveyard" is not "your graveyard" — an opponent's is fair game —
 * and it is not "two creature cards" either: both have to come out of the *same* graveyard, so a
 * board with one creature card in each pays nothing. Each card is exiled by its own owner.
 */
class NightSoilScenarioTest : FunSpec({

    val abilityId = NightSoil.activatedAbilities.first().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(NightSoil)
        return driver
    }

    test("two creature cards in an opponent's graveyard pay the cost") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)

        val alice = driver.activePlayer!!
        val bob = driver.getOpponent(alice)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(alice, "Night Soil")
        val first = driver.putCardInGraveyard(bob, "Elvish Warrior")
        val second = driver.putCardInGraveyard(bob, "Centaur Courser")
        driver.giveColorlessMana(alice, 1)

        driver.submitSuccess(
            ActivateAbility(
                playerId = alice,
                sourceId = driver.findPermanent(alice, "Night Soil")!!,
                abilityId = abilityId,
                costPayment = AdditionalCostPayment(exiledCards = listOf(first, second))
            )
        )
        driver.bothPass()

        withClue("both cards left Bob's graveyard for Bob's exile") {
            driver.state.getZone(com.wingedsheep.engine.state.ZoneKey(bob, Zone.EXILE)).containsAll(listOf(first, second)) shouldBe true
        }
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        withClue("a Saproling arrived: " + driver.getCreatures(alice).map { driver.getCardName(it) }) {
            driver.getCreatures(alice).size shouldBe 1
        }
    }

    test("the offered cost lists every graveyard that can pay it, not just the activator's") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)

        val alice = driver.activePlayer!!
        val bob = driver.getOpponent(alice)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val nightSoil = driver.putPermanentOnBattlefield(alice, "Night Soil")
        val mine = listOf(
            driver.putCardInGraveyard(alice, "Elvish Warrior"),
            driver.putCardInGraveyard(alice, "Centaur Courser"),
        )
        val theirs = listOf(
            driver.putCardInGraveyard(bob, "Elvish Warrior"),
            driver.putCardInGraveyard(bob, "Centaur Courser"),
        )
        driver.putCardInGraveyard(bob, "Lightning Bolt")
        driver.giveColorlessMana(alice, 1)

        val info = driver.legalActions(alice)
            .first {
                it.actionType == "ActivateAbility" &&
                    (it.action as? ActivateAbility)?.sourceId == nightSoil
            }
            .additionalCostInfo!!

        withClue("the picker is offered both graveyards' creature cards, and only creature cards") {
            info.validExileTargets shouldContainExactlyInAnyOrder (mine + theirs)
        }
    }

    test("a graveyard too shallow to pay the whole cost is not offered at all") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)

        val alice = driver.activePlayer!!
        val bob = driver.getOpponent(alice)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val nightSoil = driver.putPermanentOnBattlefield(alice, "Night Soil")
        driver.putCardInGraveyard(alice, "Elvish Warrior")
        val theirs = listOf(
            driver.putCardInGraveyard(bob, "Elvish Warrior"),
            driver.putCardInGraveyard(bob, "Centaur Courser"),
        )
        driver.giveColorlessMana(alice, 1)

        val info = driver.legalActions(alice)
            .first {
                it.actionType == "ActivateAbility" &&
                    (it.action as? ActivateAbility)?.sourceId == nightSoil
            }
            .additionalCostInfo!!

        withClue("Alice's lone creature card can never be half of a legal payment, so it is dropped") {
            info.validExileTargets shouldContainExactlyInAnyOrder theirs
        }
    }

    test("one creature card in each graveyard is not a single graveyard") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)

        val alice = driver.activePlayer!!
        val bob = driver.getOpponent(alice)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(alice, "Night Soil")
        val mine = driver.putCardInGraveyard(alice, "Elvish Warrior")
        val theirs = driver.putCardInGraveyard(bob, "Centaur Courser")
        driver.giveColorlessMana(alice, 1)

        driver.submit(
            ActivateAbility(
                playerId = alice,
                sourceId = driver.findPermanent(alice, "Night Soil")!!,
                abilityId = abilityId,
                costPayment = AdditionalCostPayment(exiledCards = listOf(mine, theirs))
            )
        ).isSuccess shouldBe false
    }
})
