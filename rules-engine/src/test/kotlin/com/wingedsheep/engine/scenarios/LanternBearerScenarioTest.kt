package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.vow.cards.LanternBearer
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Lantern Bearer // Lanterns' Lift (VOW) — a disturb card whose back face is an Aura.
 *
 * Front: {U} 1/1 flier with Disturb {2}{U}.
 * Back (Lanterns' Lift): Enchant creature; enchanted creature gets +1/+1 and has flying; exiles
 * itself instead of ever reaching a graveyard.
 */
class LanternBearerScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(LanternBearer))
        return driver
    }

    test("front face is a 1/1 flier cast normally from hand") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        val player = driver.activePlayer!!

        val bearer = driver.putCardInHand(player, "Lantern Bearer")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(player, Color.BLUE, 1)

        driver.submit(CastSpell(player, bearer, paymentStrategy = PaymentStrategy.FromPool)).isSuccess shouldBe true
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        val perm = driver.findPermanent(player, "Lantern Bearer")
        perm.shouldNotBeNull()
        driver.state.projectedState.hasKeyword(perm, Keyword.FLYING).shouldBeTrue()
    }

    test("disturb casts Lanterns' Lift as an Aura that grants the enchanted creature +1/+1 and flying") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        val player = driver.activePlayer!!

        val bears = driver.putCreatureOnBattlefield(player, "Grizzly Bears") // 2/2 ground creature
        val bearer = driver.putCardInGraveyard(player, "Lantern Bearer")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(player, Color.BLUE, 3)

        val result = driver.submit(
            CastSpell(
                playerId = player, cardId = bearer,
                targets = listOf(ChosenTarget.Permanent(bears)),
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.DISTURB,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        io.kotest.assertions.withClue("error=${result.error}") { result.isSuccess shouldBe true }
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        val aura = driver.findPermanent(player, "Lanterns' Lift")
        aura.shouldNotBeNull()
        driver.state.getEntity(aura)?.get<AttachedToComponent>()?.targetId shouldBe bears
        driver.state.projectedState.hasKeyword(bears, Keyword.FLYING).shouldBeTrue()
        driver.state.projectedState.getPower(bears) shouldBe 3
        driver.state.projectedState.getToughness(bears) shouldBe 3
    }

    test("the Aura is exiled rather than put into the graveyard when the creature it enchants dies") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        val player = driver.activePlayer!!

        val bears = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        val bearer = driver.putCardInGraveyard(player, "Lantern Bearer")
        val bolt = driver.putCardInHand(player, "Lightning Bolt")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(player, Color.BLUE, 3)
        driver.giveMana(player, Color.RED, 1)

        driver.submit(
            CastSpell(
                playerId = player, cardId = bearer,
                targets = listOf(ChosenTarget.Permanent(bears)),
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.DISTURB,
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe true
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        // Kill the enchanted creature (3/3 with the Aura, so Bolt is exactly lethal); the Aura is
        // put into a graveyard as a state-based action — and exiles itself instead.
        driver.submit(
            CastSpell(
                playerId = player, cardId = bolt,
                targets = listOf(ChosenTarget.Permanent(bears)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe true
        while (driver.state.stack.isNotEmpty() || driver.pendingDecision != null) driver.bothPass()

        driver.findPermanent(player, "Lanterns' Lift") shouldBe null
        driver.state.getExile(player).shouldContain(bearer)
        driver.getGraveyard(player) shouldNotContain bearer
    }
})
