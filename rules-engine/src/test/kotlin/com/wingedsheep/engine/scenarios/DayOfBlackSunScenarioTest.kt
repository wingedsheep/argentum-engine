package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SpellCastEvent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Day of Black Sun (TLA #94) — {X}{B}{B} Sorcery.
 *
 * "Each creature with mana value X or less loses all abilities until end of turn.
 *  Destroy those creatures."
 *
 * The interesting case is the *undeclared* X. A caster that announces nothing — the AI's
 * `CastSpell` carries no `xValue`, and a synthesized free cast never picks one — pays nothing for
 * X, so X is 0 (CR 601.2b). Left null all the way to resolution it hit
 * [com.wingedsheep.sdk.scripting.predicates.CardPredicate.ManaValueAtMostX], which fails *open* on
 * an unbound X — deliberately, so an X spell is still enumerated as a legal action before X is
 * chosen — and "mana value X or less" then matched every creature on the board.
 */
class DayOfBlackSunScenarioTest : FunSpec({

    fun GameTestDriver.resolveStack() {
        var safety = 0
        while (stackSize > 0 && !isPaused && safety < 20) {
            bothPass(); safety++
        }
    }

    fun setup(): Triple<GameTestDriver, EntityId, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return Triple(driver, player, opponent)
    }

    fun GameTestDriver.castEvent(name: String): SpellCastEvent =
        events.filterIsInstance<SpellCastEvent>().single { it.cardName == name }

    test("cast with no declared X resolves as X=0: only mana value 0 creatures die") {
        val (driver, player, opponent) = setup()

        driver.putCreatureOnBattlefield(opponent, "Ornithopter")     // MV 0 — dies
        driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")   // MV 2 — survives
        driver.putCreatureOnBattlefield(opponent, "Hill Giant")      // MV 4 — survives

        repeat(2) { driver.putLandOnBattlefield(player, "Swamp") }

        val wipe = driver.putCardInHand(player, "Day of Black Sun")
        driver.submitSuccess(
            CastSpell(
                playerId = player,
                cardId = wipe,
                paymentStrategy = PaymentStrategy.AutoPay
                // xValue deliberately omitted — this is the AI / synthesized-cast shape.
            )
        )
        driver.resolveStack()

        driver.findPermanent(opponent, "Ornithopter") shouldBe null
        (driver.findPermanent(opponent, "Grizzly Bears") != null) shouldBe true
        (driver.findPermanent(opponent, "Hill Giant") != null) shouldBe true
    }

    test("an undeclared X is reported as 0 on the cast event, so the log line can show it") {
        val (driver, player, _) = setup()

        repeat(2) { driver.putLandOnBattlefield(player, "Swamp") }

        val wipe = driver.putCardInHand(player, "Day of Black Sun")
        driver.submitSuccess(
            CastSpell(playerId = player, cardId = wipe, paymentStrategy = PaymentStrategy.AutoPay)
        )

        driver.castEvent("Day of Black Sun").xValue shouldBe 0
    }

    test("X=2 destroys creatures with mana value 2 or less and spares larger ones") {
        val (driver, player, opponent) = setup()

        driver.putCreatureOnBattlefield(opponent, "Ornithopter")     // MV 0 — dies
        driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")   // MV 2 — dies
        driver.putCreatureOnBattlefield(opponent, "Hill Giant")      // MV 4 — survives

        repeat(4) { driver.putLandOnBattlefield(player, "Swamp") }

        val wipe = driver.putCardInHand(player, "Day of Black Sun")
        driver.submitSuccess(
            CastSpell(
                playerId = player,
                cardId = wipe,
                paymentStrategy = PaymentStrategy.AutoPay,
                xValue = 2
            )
        )
        driver.resolveStack()

        driver.findPermanent(opponent, "Ornithopter") shouldBe null
        driver.findPermanent(opponent, "Grizzly Bears") shouldBe null
        (driver.findPermanent(opponent, "Hill Giant") != null) shouldBe true

        driver.castEvent("Day of Black Sun").xValue shouldBe 2
    }

    test("a spell with no {X} in its cost still reports no X, so its log line stays clean") {
        val (driver, player, opponent) = setup()

        driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        repeat(3) { driver.putLandOnBattlefield(player, "Swamp") }

        val bears = driver.findPermanent(opponent, "Grizzly Bears")!!
        val removal = driver.putCardInHand(player, "Doom Blade")
        driver.submitSuccess(
            CastSpell(
                playerId = player,
                cardId = removal,
                paymentStrategy = PaymentStrategy.AutoPay,
                targets = listOf(ChosenTarget.Permanent(bears))
            )
        )

        driver.castEvent("Doom Blade").xValue shouldBe null
    }
})
