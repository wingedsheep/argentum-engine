package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.tdm.cards.MoltenExhale
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Molten Exhale: "You may cast this spell as though it had flash if you behold a Dragon
 * as an additional cost to cast it. Molten Exhale deals 4 damage to target creature or
 * planeswalker."
 *
 * Exercises the non-mana flash-timing unlock: an [com.wingedsheep.sdk.scripting.KeywordAbility.OptionalAdditionalCost]
 * whose `additionalCost` is a Behold and whose `grantsFlashTiming` is `true`. Paying the
 * behold (kicked path) must let the sorcery be cast at instant speed; without it, the
 * sorcery is restricted to its main phase.
 */
class MoltenExhaleBeholdFlashTest : FunSpec({

    val testDragon = CardDefinition.creature(
        name = "Test Dragon",
        manaCost = com.wingedsheep.sdk.core.ManaCost.parse("{4}{R}"),
        subtypes = setOf(Subtype.DRAGON),
        power = 4,
        toughness = 4
    )
    val testOgre = CardDefinition.creature(
        name = "Test Ogre",
        manaCost = com.wingedsheep.sdk.core.ManaCost.parse("{3}{R}"),
        subtypes = setOf(Subtype("Ogre")),
        power = 4,
        toughness = 4
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + MoltenExhale + testDragon + testOgre)
        return driver
    }

    test("beholding a Dragon lets Molten Exhale be cast at instant speed and deals 4 damage") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)

        val p1 = driver.activePlayer!!
        val p2 = driver.player2
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // P1 controls a Dragon (the behold subject) and has Molten Exhale + mana.
        val dragon = driver.putCreatureOnBattlefield(p1, "Test Dragon")
        val ogre = driver.putCreatureOnBattlefield(p2, "Test Ogre")
        val moltenExhale = driver.putCardInHand(p1, "Molten Exhale")
        driver.giveMana(p1, Color.RED, 2)

        // Past the main phase — sorcery speed no longer applies.
        driver.passPriorityUntil(Step.END)

        val result = driver.submit(
            CastSpell(
                playerId = p1,
                cardId = moltenExhale,
                targets = listOf(ChosenTarget.Permanent(ogre)),
                wasKicked = true,
                additionalCostPayment = AdditionalCostPayment(beheldCards = listOf(dragon)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        result.isSuccess shouldBe true

        // Resolve the spell — the 4/4 Ogre takes 4 damage and dies; the beheld Dragon stays.
        driver.bothPass()
        driver.findPermanent(p2, "Test Ogre") shouldBe null
        driver.findPermanent(p1, "Test Dragon") shouldBe dragon
    }

    test("Molten Exhale cannot be cast at instant speed without beholding a Dragon") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)

        val p1 = driver.activePlayer!!
        val p2 = driver.player2
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val ogre = driver.putCreatureOnBattlefield(p2, "Test Ogre")
        val moltenExhale = driver.putCardInHand(p1, "Molten Exhale")
        driver.giveMana(p1, Color.RED, 2)

        driver.passPriorityUntil(Step.END)

        // Plain cast (no behold) at instant speed is illegal — it's a sorcery.
        val result = driver.submit(
            CastSpell(
                playerId = p1,
                cardId = moltenExhale,
                targets = listOf(ChosenTarget.Permanent(ogre)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        result.isSuccess shouldBe false
    }

    test("Molten Exhale can still be cast at sorcery speed without beholding") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)

        val p1 = driver.activePlayer!!
        val p2 = driver.player2
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val ogre = driver.putCreatureOnBattlefield(p2, "Test Ogre")
        val moltenExhale = driver.putCardInHand(p1, "Molten Exhale")
        driver.giveMana(p1, Color.RED, 2)

        val result = driver.submit(
            CastSpell(
                playerId = p1,
                cardId = moltenExhale,
                targets = listOf(ChosenTarget.Permanent(ogre)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        result.isSuccess shouldBe true

        driver.bothPass()
        driver.findPermanent(p2, "Test Ogre") shouldBe null
    }
})
