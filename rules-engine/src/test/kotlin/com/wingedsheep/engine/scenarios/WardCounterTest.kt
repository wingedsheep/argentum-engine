package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.KeywordAbility
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for WardCounterEffectExecutor.
 *
 * Ward triggers when the warded creature becomes the target of a spell/ability an
 * opponent controls. On resolution, the caster may pay the ward cost or have their
 * spell countered.
 */
class WardCounterTest : FunSpec({

    val wardedBear = card("Warded Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
        keywordAbility(KeywordAbility.ward("{2}"))
    }

    val bigWardedBear = card("Costly Warded Bear") {
        manaCost = "{2}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 3
        keywordAbility(KeywordAbility.ward("{4}"))
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(wardedBear, bigWardedBear))
        return driver
    }

    test("ward counters the targeting spell when the caster cannot pay") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            startingLife = 20
        )
        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Opponent controls a creature with Ward {4}.
        val bear = driver.putCreatureOnBattlefield(opponent, "Costly Warded Bear")

        // Active player has exactly {R} for Bolt — nothing left to pay the {4} ward cost.
        driver.giveMana(activePlayer, Color.RED, 1)
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")
        val cast = driver.castSpellWithTargets(
            activePlayer, bolt, listOf(ChosenTarget.Permanent(bear))
        )
        cast.isSuccess shouldBe true

        // Ward trigger goes on the stack; resolve it — caller cannot pay ⇒ Bolt is countered.
        driver.bothPass()

        // No pending decision — the executor goes straight to counter.
        driver.pendingDecision shouldBe null

        // Resolve everything. Bolt was countered before it could deal damage, so the
        // bear is still alive.
        driver.bothPass()
        driver.findPermanent(opponent, "Costly Warded Bear") shouldNotBe null
        // Bolt itself ends up in the graveyard (countered spells go to their owner's graveyard).
        driver.getGraveyardCardNames(activePlayer).contains("Lightning Bolt") shouldBe true
    }

    test("ward prompts caster with mana source selection when they can pay") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 40),
            startingLife = 20
        )
        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bear = driver.putCreatureOnBattlefield(opponent, "Warded Bear")

        // Enough lands to pay {R} for Bolt and {2} for Ward.
        driver.putLandOnBattlefield(activePlayer, "Mountain")
        driver.putLandOnBattlefield(activePlayer, "Mountain")
        driver.putLandOnBattlefield(activePlayer, "Mountain")

        driver.giveMana(activePlayer, Color.RED, 1)
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")
        driver.castSpellWithTargets(activePlayer, bolt, listOf(ChosenTarget.Permanent(bear)))

        // Resolve ward trigger — caster must decide whether to pay.
        driver.bothPass()

        val decision = driver.pendingDecision
        decision.shouldNotBeNull()
        decision.shouldBeInstanceOf<SelectManaSourcesDecision>()
        decision.playerId shouldBe activePlayer
        decision.canDecline shouldBe true
        decision.requiredCost shouldBe "{2}"
    }

    test("declining ward payment counters the targeting spell") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 40),
            startingLife = 20
        )
        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bear = driver.putCreatureOnBattlefield(opponent, "Warded Bear")

        driver.putLandOnBattlefield(activePlayer, "Mountain")
        driver.putLandOnBattlefield(activePlayer, "Mountain")
        driver.putLandOnBattlefield(activePlayer, "Mountain")

        driver.giveMana(activePlayer, Color.RED, 1)
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")
        driver.castSpellWithTargets(activePlayer, bolt, listOf(ChosenTarget.Permanent(bear)))

        driver.bothPass()
        driver.pendingDecision.shouldBeInstanceOf<SelectManaSourcesDecision>()

        // Decline the ward payment — Bolt is countered.
        driver.submitManaAutoPayOrDecline(activePlayer, autoPay = false)

        // Let everything settle.
        repeat(2) { if (driver.state.priorityPlayerId != null) driver.bothPass() }

        // Bear survives since Bolt was countered.
        driver.findPermanent(opponent, "Warded Bear") shouldNotBe null
    }

    test("paying ward cost lets the targeting spell resolve") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 40),
            startingLife = 20
        )
        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bear = driver.putCreatureOnBattlefield(opponent, "Warded Bear")

        // Three untapped Mountains covers {R} + {2}.
        driver.putLandOnBattlefield(activePlayer, "Mountain")
        driver.putLandOnBattlefield(activePlayer, "Mountain")
        driver.putLandOnBattlefield(activePlayer, "Mountain")

        driver.giveMana(activePlayer, Color.RED, 1)
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")
        driver.castSpellWithTargets(activePlayer, bolt, listOf(ChosenTarget.Permanent(bear)))

        driver.bothPass()
        driver.pendingDecision.shouldBeInstanceOf<SelectManaSourcesDecision>()

        // Pay the ward cost — Bolt will resolve and kill the 2/2 bear.
        driver.submitManaAutoPayOrDecline(activePlayer, autoPay = true)

        // Resolve remaining stack.
        repeat(3) { if (driver.state.priorityPlayerId != null) driver.bothPass() }

        driver.findPermanent(opponent, "Warded Bear") shouldBe null
    }
})
