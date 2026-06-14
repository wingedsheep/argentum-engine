package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.PhantomInterference
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Phantom Interference — {U} Instant, Spree
 *
 * + {3} — Create a 2/2 white Spirit creature token with flying.
 * + {1} — Counter target spell unless its controller pays {2}.
 */
class PhantomInterferenceScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(PhantomInterference)
        return driver
    }

    test("mode + {3}: create a 2/2 white flying Spirit token") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!

        val spell = driver.putCardInHand(me, "Phantom Interference")
        driver.giveMana(me, Color.BLUE, 1)
        driver.giveColorlessMana(me, 3) // {U} base + {3} for the token mode
        driver.submit(
            CastSpell(
                playerId = me,
                cardId = spell,
                chosenModes = listOf(0),
                modeTargetsOrdered = listOf(emptyList()),
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe true
        driver.bothPass()
        driver.isPaused shouldBe false

        val token = driver.findPermanent(me, "Spirit Token")!!
        val card = driver.state.getEntity(token)!!.get<CardComponent>()!!
        card.typeLine.subtypes.map { it.value } shouldContain "Spirit"
        card.colors shouldContain Color.WHITE
        val projected = driver.state.projectedState
        projected.getPower(token) shouldBe 2
        projected.getToughness(token) shouldBe 2
        projected.hasKeyword(token, Keyword.FLYING) shouldBe true
    }

    test("mode + {1}: counter target spell when its controller declines to pay {2}") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 20, "Mountain" to 20), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        // Opponent casts Lightning Bolt; I respond with Phantom Interference's counter mode.
        val bolt = driver.putCardInHand(opponent, "Lightning Bolt")
        driver.giveMana(opponent, Color.RED, 1)
        driver.passPriority(me) // pass to opponent
        driver.castSpell(opponent, bolt, listOf(me))
        val boltOnStack = driver.getTopOfStack()!!
        driver.passPriority(opponent) // priority back to me

        val spell = driver.putCardInHand(me, "Phantom Interference")
        driver.giveMana(me, Color.BLUE, 1)
        driver.giveColorlessMana(me, 1) // {U} base + {1} for the counter mode
        driver.submit(
            CastSpell(
                playerId = me,
                cardId = spell,
                targets = listOf(ChosenTarget.Spell(boltOnStack)),
                chosenModes = listOf(1),
                modeTargetsOrdered = listOf(listOf(ChosenTarget.Spell(boltOnStack))),
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe true

        // Give the opponent enough floating mana that they *could* pay {2}, so the
        // counter prompts a real pay-or-counter decision (rather than auto-countering).
        driver.giveColorlessMana(opponent, 2)

        // Resolve Phantom Interference → opponent is asked whether to pay {2}.
        driver.bothPass()
        driver.isPaused shouldBe true
        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        (driver.pendingDecision as YesNoDecision).playerId shouldBe opponent

        // Opponent declines → Lightning Bolt is countered.
        driver.submitYesNo(opponent, false)
        driver.getGraveyardCardNames(opponent) shouldContain "Lightning Bolt"
    }

    test("both modes: counter the spell and create a Spirit token") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 20, "Mountain" to 20), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        val bolt = driver.putCardInHand(opponent, "Lightning Bolt")
        driver.giveMana(opponent, Color.RED, 1)
        driver.passPriority(me)
        driver.castSpell(opponent, bolt, listOf(me))
        val boltOnStack = driver.getTopOfStack()!!
        driver.passPriority(opponent)

        val spell = driver.putCardInHand(me, "Phantom Interference")
        driver.giveMana(me, Color.BLUE, 1)
        driver.giveColorlessMana(me, 4) // {U} base + {3} (token) + {1} (counter)
        driver.submit(
            CastSpell(
                playerId = me,
                cardId = spell,
                targets = listOf(ChosenTarget.Spell(boltOnStack)),
                chosenModes = listOf(0, 1),
                modeTargetsOrdered = listOf(emptyList(), listOf(ChosenTarget.Spell(boltOnStack))),
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe true

        // Opponent could pay {2} but will decline.
        driver.giveColorlessMana(opponent, 2)

        driver.bothPass()
        driver.isPaused shouldBe true
        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(opponent, false)

        // Token mode resolved first → Spirit on the battlefield; counter mode → bolt countered.
        driver.findPermanent(me, "Spirit Token").shouldBeInstanceOf<Any>()
        driver.getGraveyardCardNames(opponent) shouldContain "Lightning Bolt"
    }

    test("Spree requires at least one mode: casting with no modes fails") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!

        val spell = driver.putCardInHand(me, "Phantom Interference")
        driver.giveMana(me, Color.BLUE, 1)
        val result = driver.submit(
            CastSpell(
                playerId = me,
                cardId = spell,
                chosenModes = emptyList(),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        result.isSuccess shouldBe false
    }
})
