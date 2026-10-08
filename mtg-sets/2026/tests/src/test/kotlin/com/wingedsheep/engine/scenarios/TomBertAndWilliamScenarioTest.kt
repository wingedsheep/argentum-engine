package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.hob.cards.TomBertAndWilliam
import com.wingedsheep.mtg.sets.definitions.lea.cards.Shatter
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import com.wingedsheep.engine.core.Outcome

/**
 * Tom, Bert, and William {3}{B}{G} — Legendary Creature — Troll 5/5.
 *
 * "{1}, Sacrifice another creature: Draw cards equal to the sacrificed creature's power, then
 *  discard a card."
 * "When Tom, Bert, and William die, if they were a creature, return them to the battlefield.
 *  They're an artifact."
 *
 * The interesting behaviour is the self-recursion guard. The first death returns Tom as an
 * *artifact*; the second death must not return him again, and the only thing telling the two deaths
 * apart is a card type that — by the time the trigger is considered — exists solely as last-known
 * information on a card sitting in the graveyard wearing its printed `Legendary Creature — Troll`
 * line again. That is what `Conditions.TriggeringEntityWas` reads.
 *
 * Deaths are staged with real removal rather than the blunt `moveToGraveyard` test helper, which by
 * design skips dies triggers entirely and would make the guard assertions vacuous. Two Lightning
 * Bolts (3 + 3) are lethal to the printed 5/5; the returned artifact is no longer a creature, so
 * bolts can't even target it — Shatter is what kills it, which is itself part of the point.
 */
class TomBertAndWilliamScenarioTest : FunSpec({

    val projector = StateProjector()
    val sacOutletId = TomBertAndWilliam.activatedAbilities.first().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(TomBertAndWilliam, Shatter))
        return driver
    }

    fun settleStack(driver: GameTestDriver) {
        var safety = 0
        while (driver.stackSize > 0 && safety < 20) {
            driver.bothPass()
            safety++
        }
    }

    /** Bolt [creature] for 3 and let anything that follows resolve. */
    fun bolt(driver: GameTestDriver, caster: EntityId, creature: EntityId) {
        driver.giveMana(caster, Color.RED, 1)
        val boltCard = driver.putCardInHand(caster, "Lightning Bolt")
        driver.castSpell(caster, boltCard, targets = listOf(creature)).error shouldBe null
        settleStack(driver)
    }

    fun tomOnBattlefield(driver: GameTestDriver, player: EntityId): EntityId? =
        driver.findPermanent(player, "Tom, Bert, and William")

    test("sacrifice outlet draws cards equal to the sacrificed creature's power, then discards one") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!

        val tom = driver.putCreatureOnBattlefield(you, "Tom, Bert, and William")
        driver.removeSummoningSickness(tom)
        // Centaur Courser is a 3/3, so this must draw exactly 3.
        val courser = driver.putCreatureOnBattlefield(you, "Centaur Courser")

        val handBefore = driver.getHandSize(you)
        driver.giveColorlessMana(you, 1)

        driver.submit(
            ActivateAbility(
                playerId = you,
                sourceId = tom,
                abilityId = sacOutletId,
                costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(courser))
            )
        ).outcome shouldBe Outcome.Done
        settleStack(driver)

        // The discard is a card selection; pitch the first card in hand.
        driver.submitCardSelection(you, listOf(driver.getHand(you).first())).error shouldBe null
        settleStack(driver)

        // Drew 3, discarded 1.
        driver.getHandSize(you) shouldBe handBefore + 2
        driver.getGraveyardCardNames(you).contains("Centaur Courser") shouldBe true
    }

    test("first death: returns to the battlefield as an artifact that is no longer a creature") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!

        val tom = driver.putCreatureOnBattlefield(you, "Tom, Bert, and William")
        bolt(driver, you, tom)   // 3 damage — not yet lethal to a 5/5
        tomOnBattlefield(driver, you) shouldBe tom
        bolt(driver, you, tom)   // 6 total — lethal

        val returned = tomOnBattlefield(driver, you)
        (returned != null) shouldBe true

        val projected = projector.project(driver.state)
        val cardTypes = projected.getTypes(returned!!)
        cardTypes.contains(CardType.ARTIFACT.name) shouldBe true
        // "They're no longer a creature." — and losing CREATURE drops the Troll type with it.
        cardTypes.contains(CardType.CREATURE.name) shouldBe false
        projected.isCreature(returned) shouldBe false
        projected.getSubtypes(returned).contains("Troll") shouldBe false

        driver.getGraveyardCardNames(you).contains("Tom, Bert, and William") shouldBe false
    }

    test("second death: the artifact they came back as does NOT return again") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!

        val tom = driver.putCreatureOnBattlefield(you, "Tom, Bert, and William")
        bolt(driver, you, tom)
        bolt(driver, you, tom)
        val returned = tomOnBattlefield(driver, you)!!

        // They are an artifact now, so this death must NOT trigger the return.
        driver.giveMana(you, Color.RED, 2)
        val shatter = driver.putCardInHand(you, "Shatter")
        driver.castSpell(you, shatter, targets = listOf(returned)).error shouldBe null
        settleStack(driver)

        tomOnBattlefield(driver, you) shouldBe null
        driver.getGraveyardCardNames(you).contains("Tom, Bert, and William") shouldBe true
    }

    test("the returned artifact keeps its abilities, so the sacrifice outlet still works") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!

        val tom = driver.putCreatureOnBattlefield(you, "Tom, Bert, and William")
        bolt(driver, you, tom)
        bolt(driver, you, tom)
        val returned = tomOnBattlefield(driver, you)!!

        val lion = driver.putCreatureOnBattlefield(you, "Savannah Lions")   // 1/1
        val handBefore = driver.getHandSize(you)
        driver.giveColorlessMana(you, 1)

        driver.submit(
            ActivateAbility(
                playerId = you,
                sourceId = returned,
                abilityId = sacOutletId,
                costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(lion))
            )
        ).outcome shouldBe Outcome.Done
        settleStack(driver)

        driver.submitCardSelection(you, listOf(driver.getHand(you).first())).error shouldBe null
        settleStack(driver)

        // Drew 1 (the Lions' power), discarded 1 — net zero, but the ability resolved.
        driver.getHandSize(you) shouldBe handBefore
        driver.getGraveyardCardNames(you).contains("Savannah Lions") shouldBe true
    }
})
