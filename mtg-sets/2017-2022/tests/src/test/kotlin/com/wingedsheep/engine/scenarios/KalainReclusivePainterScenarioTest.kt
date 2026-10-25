package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.afr.cards.KalainReclusivePainter
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Kalain, Reclusive Painter (AFR #225) — {B}{R} Legendary Creature — Human Elf Bard 1/2.
 *
 *   When Kalain enters, create a Treasure token.
 *   Other creatures you control enter with an additional +1/+1 counter on them for each mana from
 *   a Treasure spent to cast them.
 *
 * The count reads the ENTERING creature's own cast payment, not Kalain's: Treasure mana spent on
 * the creature adds one counter per mana; ordinary mana adds none.
 */
class KalainReclusivePainterScenarioTest : FunSpec({

    val treasureManaAbilityId = PredefinedTokens.Treasure.activatedAbilities.single().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(KalainReclusivePainter))
        driver.registerCard(PredefinedTokens.Treasure)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.plusOneCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    test("entering creates a Treasure token") {
        val driver = createDriver()
        val me = driver.player1

        val kalain = driver.putCardInHand(me, "Kalain, Reclusive Painter")
        driver.giveMana(me, Color.BLACK, 1)
        driver.giveMana(me, Color.RED, 1)
        driver.submitSuccess(CastSpell(playerId = me, cardId = kalain, paymentStrategy = PaymentStrategy.FromPool))
        driver.bothPass() // resolve Kalain
        driver.bothPass() // resolve its enters trigger

        driver.findPermanent(me, "Treasure") shouldNotBe null
    }

    test("a creature cast with two Treasure mana enters with two +1/+1 counters") {
        val driver = createDriver()
        val me = driver.player1

        driver.putPermanentOnBattlefield(me, "Kalain, Reclusive Painter")
        repeat(2) {
            val treasure = driver.putPermanentOnBattlefield(me, "Treasure")
            driver.submitSuccess(
                ActivateAbility(
                    playerId = me,
                    sourceId = treasure,
                    abilityId = treasureManaAbilityId,
                    manaColorChoice = Color.GREEN,
                )
            )
        }

        val bears = driver.putCardInHand(me, "Grizzly Bears")
        driver.submitSuccess(CastSpell(playerId = me, cardId = bears, paymentStrategy = PaymentStrategy.FromPool))
        driver.bothPass()

        withClue("both mana came from Treasures") {
            driver.plusOneCounters(bears) shouldBe 2
        }
    }

    test("a creature cast without Treasure mana gets no counters") {
        val driver = createDriver()
        val me = driver.player1

        driver.putPermanentOnBattlefield(me, "Kalain, Reclusive Painter")
        // An untapped Treasure that isn't spent doesn't count.
        driver.putPermanentOnBattlefield(me, "Treasure")

        val bears = driver.putCardInHand(me, "Grizzly Bears")
        driver.giveMana(me, Color.GREEN, 2)
        driver.submitSuccess(CastSpell(playerId = me, cardId = bears, paymentStrategy = PaymentStrategy.FromPool))
        driver.bothPass()

        driver.plusOneCounters(bears) shouldBe 0
    }
})
