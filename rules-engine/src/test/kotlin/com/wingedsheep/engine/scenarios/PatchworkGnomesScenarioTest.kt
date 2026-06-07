package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.tmp.cards.PatchworkGnomes
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for Patchwork Gnomes.
 *
 * Patchwork Gnomes
 * {3}
 * Artifact Creature — Gnome
 * 2/1
 * Discard a card: Regenerate this creature.
 */
class PatchworkGnomesScenarioTest : FunSpec({

    val abilityId = PatchworkGnomes.activatedAbilities.first().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(PatchworkGnomes)
        return driver
    }

    fun hasRegenShield(driver: GameTestDriver, entityId: com.wingedsheep.sdk.model.EntityId): Boolean =
        driver.state.floatingEffects.any {
            it.effect.modification is SerializableModification.RegenerationShield &&
                entityId in it.effect.affectedEntities
        }

    test("discarding a card installs a regeneration shield") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val gnomes = driver.putCreatureOnBattlefield(activePlayer, "Patchwork Gnomes")
        val toDiscard = driver.putCardInHand(activePlayer, "Grizzly Bears")

        hasRegenShield(driver, gnomes) shouldBe false

        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = gnomes,
                abilityId = abilityId,
                costPayment = AdditionalCostPayment(discardedCards = listOf(toDiscard))
            )
        )
        result.isSuccess shouldBe true
        driver.bothPass()

        // Card was discarded.
        driver.getHand(activePlayer).contains(toDiscard) shouldBe false
        driver.state.getGraveyard(activePlayer).contains(toDiscard) shouldBe true
        // Regeneration shield now present.
        hasRegenShield(driver, gnomes) shouldBe true
    }

    test("regeneration shield saves the creature from a damage spell") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val gnomes = driver.putCreatureOnBattlefield(activePlayer, "Patchwork Gnomes")
        val toDiscard = driver.putCardInHand(activePlayer, "Grizzly Bears")

        // Regenerate (discard a card).
        driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = gnomes,
                abilityId = abilityId,
                costPayment = AdditionalCostPayment(discardedCards = listOf(toDiscard))
            )
        ).isSuccess shouldBe true
        driver.bothPass()

        // Lightning Bolt the 2/1 — lethal, but the shield replaces destruction. The active
        // player casts it (their own sorcery-speed priority) targeting their own creature.
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")
        driver.giveMana(activePlayer, Color.RED, 1)
        driver.castSpell(activePlayer, bolt, listOf(gnomes)).isSuccess shouldBe true
        driver.bothPass()

        // Gnomes survived (regenerated) and is tapped.
        driver.findPermanent(activePlayer, "Patchwork Gnomes") shouldNotBe null
        driver.isTapped(gnomes) shouldBe true
        // Shield was consumed.
        hasRegenShield(driver, gnomes) shouldBe false
    }

    test("cannot activate without a card to discard") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val gnomes = driver.putCreatureOnBattlefield(activePlayer, "Patchwork Gnomes")

        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = gnomes,
                abilityId = abilityId,
                costPayment = AdditionalCostPayment(discardedCards = emptyList())
            )
        )
        result.isSuccess shouldBe false
    }
})
