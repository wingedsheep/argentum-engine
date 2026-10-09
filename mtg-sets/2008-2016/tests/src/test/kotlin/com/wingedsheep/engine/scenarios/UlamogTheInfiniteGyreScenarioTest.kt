package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.roe.cards.UlamogTheInfiniteGyre
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Ulamog, the Infinite Gyre (ROE #12).
 *
 *  - The cast trigger destroys its target before Ulamog itself resolves.
 *  - "When Ulamog is put into a graveyard from anywhere, its owner shuffles their graveyard into
 *    their library" fires from the graveyard (here via the legend rule — Ulamog is indestructible)
 *    and moves the owner's whole graveyard, Ulamog included, into the library.
 */
class UlamogTheInfiniteGyreScenarioTest : FunSpec({

    val name = "Ulamog, the Infinite Gyre"

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(UlamogTheInfiniteGyre)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun castUlamog(driver: GameTestDriver, caster: com.wingedsheep.sdk.model.EntityId, target: com.wingedsheep.sdk.model.EntityId) {
        val ulamog = driver.putCardInHand(caster, name)
        driver.giveMana(caster, Color.GREEN, 11)
        driver.submit(
            CastSpell(playerId = caster, cardId = ulamog, paymentStrategy = PaymentStrategy.FromPool)
        ).error shouldBe null
        driver.submitTargetSelection(caster, listOf(target)).error shouldBe null
    }

    test("cast trigger destroys target permanent before Ulamog resolves") {
        val driver = createDriver()
        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)
        val victim = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")

        castUlamog(driver, caster, victim)

        driver.bothPass()
        driver.getGraveyard(opponent).contains(victim) shouldBe true
        driver.findPermanent(caster, name) shouldBe null

        driver.bothPass()
        driver.findPermanent(caster, name) shouldNotBe null
    }

    test("put into a graveyard, its owner shuffles their graveyard into their library") {
        val driver = createDriver()
        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)

        val firstUlamog = driver.putCreatureOnBattlefield(caster, name)
        val oldCard = driver.putCardInGraveyard(caster, "Savannah Lions")
        val opponentGraveyardCard = driver.putCardInGraveyard(opponent, "Savannah Lions")
        val victim = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")

        castUlamog(driver, caster, victim)
        driver.bothPass() // cast trigger: destroy the Courser
        driver.bothPass() // second Ulamog resolves -> legend rule

        val secondUlamog = driver.getPermanents(caster).first {
            driver.getCardName(it) == name && it != firstUlamog
        }
        driver.submitCardSelection(caster, listOf(secondUlamog)).error shouldBe null
        driver.getGraveyard(caster).contains(firstUlamog) shouldBe true

        val libraryBefore = driver.state.getLibrary(caster).size
        val graveyardBefore = driver.getGraveyard(caster).size

        // Resolve the graveyard trigger.
        driver.bothPass()

        driver.getGraveyard(caster).size shouldBe 0
        driver.state.getLibrary(caster).size shouldBe libraryBefore + graveyardBefore
        driver.state.getLibrary(caster).contains(firstUlamog) shouldBe true
        driver.state.getLibrary(caster).contains(oldCard) shouldBe true
        // Only the owner's graveyard is shuffled.
        driver.getGraveyard(opponent).contains(opponentGraveyardCard) shouldBe true
        driver.getGraveyard(opponent).contains(victim) shouldBe true
    }
})
