package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.identity.DoubleFacedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mid.cards.CovetousCastaway
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Covetous Castaway // Ghostly Castigator (MID).
 *
 * Front: "When this creature dies, mill three cards." + Disturb {3}{U}{U}.
 * Back (Ghostly Castigator): 3/4 flier whose entry may shuffle up to three target cards from your
 * graveyard into your library, and which exiles itself instead of ever reaching a graveyard.
 */
class CovetousCastawayScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(CovetousCastaway))
        return driver
    }

    fun disturbCast(driver: GameTestDriver, player: EntityId, cardId: EntityId) =
        driver.submit(
            CastSpell(
                playerId = player,
                cardId = cardId,
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.DISTURB,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )

    /**
     * Resolve everything on the stack, answering the entry trigger's "you may" with [shuffle] and
     * feeding it [targets] when it asks which graveyard cards to shuffle back. Both prompts come at
     * resolution time — the may-decision first, then the target choice.
     */
    fun resolveEntryTrigger(
        driver: GameTestDriver,
        player: EntityId,
        shuffle: Boolean,
        targets: List<EntityId>
    ) {
        var guard = 0
        while (guard++ < 20 && (driver.state.stack.isNotEmpty() || driver.pendingDecision != null)) {
            when {
                driver.pendingDecision is YesNoDecision -> driver.submitYesNo(player, shuffle)
                driver.pendingDecision is ChooseTargetsDecision -> driver.submitTargetSelection(player, targets)
                driver.state.stack.isNotEmpty() -> driver.bothPass()
                else -> break
            }
        }
    }

    test("the front face mills three when it dies") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        val player = driver.activePlayer!!

        val castaway = driver.putCardInHand(player, "Covetous Castaway")
        val bolt = driver.putCardInHand(player, "Lightning Bolt")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(player, Color.BLUE, 2)
        driver.giveMana(player, Color.RED, 1)

        driver.submit(CastSpell(player, castaway, paymentStrategy = PaymentStrategy.FromPool)).isSuccess shouldBe true
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        val libraryBefore = driver.state.getLibrary(player).size
        val graveyardBefore = driver.getGraveyard(player).size

        // Bolt it: 3 damage kills the 1/3, and the dies trigger mills three.
        driver.submit(
            CastSpell(
                playerId = player, cardId = bolt,
                targets = listOf(ChosenTarget.Permanent(castaway)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe true
        while (driver.state.stack.isNotEmpty() || driver.pendingDecision != null) driver.bothPass()

        driver.state.getLibrary(player).size shouldBe libraryBefore - 3
        // The three milled cards plus the Castaway itself and the resolved Bolt.
        driver.getGraveyard(player).size shouldBe graveyardBefore + 5
        driver.getGraveyard(player) shouldContain castaway
    }

    test("disturb casts it as Ghostly Castigator, a 3/4 flier, and its entry shuffles the chosen cards back") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        val player = driver.activePlayer!!

        val castaway = driver.putCardInGraveyard(player, "Covetous Castaway")
        val fodder = listOf(
            driver.putCardInGraveyard(player, "Grizzly Bears"),
            driver.putCardInGraveyard(player, "Savannah Lions"),
        )
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(player, Color.BLUE, 5)

        val libraryBefore = driver.state.getLibrary(player).size
        val result = disturbCast(driver, player, castaway)
        io.kotest.assertions.withClue("error=${result.error}") { result.isSuccess shouldBe true }

        resolveEntryTrigger(driver, player, shuffle = true, targets = fodder)

        val castigator = driver.findPermanent(player, "Ghostly Castigator")
        castigator.shouldNotBeNull()
        driver.state.projectedState.hasKeyword(castigator, Keyword.FLYING).shouldBeTrue()
        driver.state.getEntity(castigator)?.get<DoubleFacedComponent>()?.isBack shouldBe true

        // Both chosen cards left the graveyard for the library.
        fodder.forEach { driver.getGraveyard(player) shouldNotContain it }
        driver.state.getLibrary(player).size shouldBe libraryBefore + fodder.size
    }

    test("declining the optional shuffle leaves the graveyard untouched") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        val player = driver.activePlayer!!

        val castaway = driver.putCardInGraveyard(player, "Covetous Castaway")
        val fodder = driver.putCardInGraveyard(player, "Grizzly Bears")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(player, Color.BLUE, 5)
        val libraryBefore = driver.state.getLibrary(player).size

        disturbCast(driver, player, castaway).isSuccess shouldBe true
        resolveEntryTrigger(driver, player, shuffle = false, targets = listOf(fodder))

        driver.findPermanent(player, "Ghostly Castigator").shouldNotBeNull()
        driver.getGraveyard(player) shouldContain fodder
        driver.state.getLibrary(player).size shouldBe libraryBefore
    }

    test("Ghostly Castigator is exiled when it dies, so it can never be disturbed twice") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        val player = driver.activePlayer!!

        val castaway = driver.putCardInGraveyard(player, "Covetous Castaway")
        val doomBlade = driver.putCardInHand(player, "Doom Blade")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(player, Color.BLUE, 5)
        driver.giveMana(player, Color.BLACK, 2)

        disturbCast(driver, player, castaway).isSuccess shouldBe true
        resolveEntryTrigger(driver, player, shuffle = false, targets = emptyList())

        driver.submit(
            CastSpell(
                playerId = player, cardId = doomBlade,
                targets = listOf(ChosenTarget.Permanent(castaway)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe true
        while (driver.state.stack.isNotEmpty() || driver.pendingDecision != null) driver.bothPass()

        // The back face's own replacement sends it to exile instead of the graveyard, and with the
        // front face restored (Rule 712.8a) it is a plain Covetous Castaway sitting in exile.
        driver.state.getExile(player).shouldContain(castaway)
        driver.getGraveyard(player) shouldNotContain castaway
    }
})
