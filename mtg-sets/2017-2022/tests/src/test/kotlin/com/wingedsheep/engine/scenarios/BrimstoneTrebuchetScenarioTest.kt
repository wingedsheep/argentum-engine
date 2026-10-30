package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.eld.cards.BrimstoneTrebuchet
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Brimstone Trebuchet (ELD #116) — "Defender, reach. {T}: This creature deals 1 damage to each
 * opponent. Whenever a Knight you control enters, untap this creature."
 */
class BrimstoneTrebuchetScenarioTest : FunSpec({

    val testKnight = card("Test Knight") {
        manaCost = "{W}"
        typeLine = "Creature — Human Knight"
        power = 2
        toughness = 2
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(BrimstoneTrebuchet, testKnight))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        return driver
    }

    fun GameTestDriver.ping(trebuchet: com.wingedsheep.sdk.model.EntityId) = submit(
        ActivateAbility(
            playerId = activePlayer!!,
            sourceId = trebuchet,
            abilityId = BrimstoneTrebuchet.activatedAbilities.first().id,
        )
    )

    fun GameTestDriver.drainStack() {
        var guard = 0
        while (state.stack.isNotEmpty() && guard++ < 10) bothPass()
    }

    test("tap to ping each opponent, then a Knight entering untaps it for a second ping") {
        val driver = createDriver()
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!
        val foe = driver.getOpponent(me)
        val trebuchet = driver.putCreatureOnBattlefield(me, "Brimstone Trebuchet")
        driver.removeSummoningSickness(trebuchet)

        driver.ping(trebuchet).error shouldBe null
        driver.drainStack()
        driver.getLifeTotal(foe) shouldBe 19
        driver.isTapped(trebuchet) shouldBe true

        val knight = driver.putCardInHand(me, "Test Knight")
        driver.giveMana(me, Color.WHITE, 1)
        driver.castSpell(me, knight).error shouldBe null
        driver.drainStack()
        driver.isTapped(trebuchet) shouldBe false

        driver.ping(trebuchet).error shouldBe null
        driver.drainStack()
        driver.getLifeTotal(foe) shouldBe 18
        driver.getLifeTotal(me) shouldBe 20
    }

    test("a non-Knight entering doesn't untap it") {
        val driver = createDriver()
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!
        val trebuchet = driver.putCreatureOnBattlefield(me, "Brimstone Trebuchet")
        driver.removeSummoningSickness(trebuchet)
        driver.ping(trebuchet).error shouldBe null
        driver.drainStack()

        val lions = driver.putCardInHand(me, "Savannah Lions")
        driver.giveMana(me, Color.WHITE, 1)
        driver.castSpell(me, lions).error shouldBe null
        driver.drainStack()
        driver.isTapped(trebuchet) shouldBe true
    }
})
