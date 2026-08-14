package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.vow.cards.TwinbladeGeist
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Twinblade Geist // Twinblade Invocation (VOW) — a disturb card whose back face is an Aura.
 *
 * Front: {1}{W} 1/1 with double strike and Disturb {2}{W}.
 * Back (Twinblade Invocation): Enchant creature; enchanted creature has double strike; exiles
 * itself instead of ever reaching a graveyard.
 */
class TwinbladeGeistScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(TwinbladeGeist))
        return driver
    }

    test("front face is a 1/1 with double strike") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        val player = driver.activePlayer!!

        val geist = driver.putCardInHand(player, "Twinblade Geist")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(player, Color.WHITE, 2)

        driver.submit(CastSpell(player, geist, paymentStrategy = PaymentStrategy.FromPool)).isSuccess shouldBe true
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        val perm = driver.findPermanent(player, "Twinblade Geist")
        perm.shouldNotBeNull()
        driver.state.projectedState.hasKeyword(perm, Keyword.DOUBLE_STRIKE).shouldBeTrue()
    }

    test("disturb casts Twinblade Invocation, granting the enchanted creature double strike") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        val player = driver.activePlayer!!

        val bears = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        driver.state.projectedState.hasKeyword(bears, Keyword.DOUBLE_STRIKE).shouldBeFalse()

        val geist = driver.putCardInGraveyard(player, "Twinblade Geist")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(player, Color.WHITE, 3)

        val result = driver.submit(
            CastSpell(
                playerId = player, cardId = geist,
                targets = listOf(ChosenTarget.Permanent(bears)),
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.DISTURB,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        io.kotest.assertions.withClue("error=${result.error}") { result.isSuccess shouldBe true }
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        val aura = driver.findPermanent(player, "Twinblade Invocation")
        aura.shouldNotBeNull()
        driver.state.getEntity(aura)?.get<AttachedToComponent>()?.targetId shouldBe bears
        driver.state.projectedState.hasKeyword(bears, Keyword.DOUBLE_STRIKE).shouldBeTrue()
    }

    test("the disturb offer is withheld when there is no creature for the Aura to enchant") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        val player = driver.activePlayer!!

        driver.putCardInGraveyard(player, "Twinblade Geist")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(player, Color.WHITE, 3)

        // An Aura spell needs a legal target as it is cast (CR 601.2c); with an empty board there
        // is nothing to enchant, so the offer is not surfaced at all.
        LegalActionEnumerator.create(driver.cardRegistry).enumerate(driver.state, player)
            .filter { (it.action as? CastSpell)?.alternativeCostType == AlternativeCostType.DISTURB }
            .shouldBeEmpty()
    }
})
