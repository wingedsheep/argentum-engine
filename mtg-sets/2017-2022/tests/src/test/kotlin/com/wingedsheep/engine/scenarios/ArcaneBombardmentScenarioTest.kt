package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.snc.cards.ArcaneBombardment
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * Arcane Bombardment (SNC #101): on your first instant/sorcery cast each turn, exile a random
 * instant/sorcery card from your graveyard (linked to the enchantment), copy every card in that
 * pile, and cast any number of the copies for free during resolution.
 */
class ArcaneBombardmentScenarioTest : FunSpec({

    val bolt = card("Bombardment Test Bolt") {
        manaCost = "{R}"
        typeLine = "Instant"
        spell {
            effect = Effects.DealDamage(3, EffectTarget.PlayerRef(Player.EachOpponent))
        }
    }
    val blast = card("Bombardment Test Blast") {
        manaCost = "{3}{R}"
        typeLine = "Sorcery"
        spell {
            effect = Effects.DealDamage(2, EffectTarget.PlayerRef(Player.EachOpponent))
        }
    }
    val bear = card("Bombardment Test Bear") {
        manaCost = "{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ArcaneBombardment, bolt, blast, bear))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    // Accept the first offered copy, decline any further offers, and let everything resolve.
    fun GameTestDriver.resolveCastingCopies() {
        var pickedCopy = false
        var guard = 0
        while ((state.stack.isNotEmpty() || state.pendingDecision != null) && guard < 80) {
            val decision = state.pendingDecision
            when {
                decision is SelectCardsDecision && !pickedCopy && decision.options.isNotEmpty() -> {
                    pickedCopy = true
                    submitCardSelection(decision.playerId, listOf(decision.options.first()))
                }
                decision is SelectCardsDecision -> submitCardSelection(decision.playerId, emptyList())
                decision != null -> autoResolveDecision()
                else -> bothPass()
            }
            guard++
        }
    }

    test("first instant exiles a random instant or sorcery from the graveyard and casts a free copy") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.state.turnOrder.first { it != me }

        driver.putPermanentOnBattlefield(me, "Arcane Bombardment")
        driver.putCardInGraveyard(me, "Bombardment Test Blast")
        driver.putCardInGraveyard(me, "Mountain") // not an instant or sorcery — never exiled

        driver.giveMana(me, Color.RED, 2)
        driver.castSpell(me, driver.putCardInHand(me, "Bombardment Test Bolt"))
        driver.resolveCastingCopies()

        // Bolt (3) + the free Blast copy (2); the copy paid no {3}{R}.
        driver.getLifeTotal(opponent) shouldBe 15
        driver.getExileCardNames(me) shouldContainExactlyInAnyOrder listOf("Bombardment Test Blast")
        driver.getGraveyardCardNames(me) shouldContainExactlyInAnyOrder
            listOf("Mountain", "Bombardment Test Bolt")

        // A second instant the same turn doesn't trigger.
        driver.castSpell(me, driver.putCardInHand(me, "Bombardment Test Bolt"))
        driver.resolveCastingCopies()

        driver.getLifeTotal(opponent) shouldBe 12
        driver.getExileCardNames(me) shouldContainExactlyInAnyOrder listOf("Bombardment Test Blast")
    }

    test("a creature cast earlier in the turn doesn't use up the first instant-or-sorcery trigger") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.state.turnOrder.first { it != me }

        driver.putPermanentOnBattlefield(me, "Arcane Bombardment")
        driver.putCardInGraveyard(me, "Bombardment Test Blast")

        driver.giveMana(me, Color.GREEN, 1)
        driver.castSpell(me, driver.putCardInHand(me, "Bombardment Test Bear"))
        driver.resolveCastingCopies()

        driver.giveMana(me, Color.RED, 1)
        driver.castSpell(me, driver.putCardInHand(me, "Bombardment Test Bolt"))
        driver.resolveCastingCopies()

        driver.getLifeTotal(opponent) shouldBe 15
        driver.getExileCardNames(me) shouldContainExactlyInAnyOrder listOf("Bombardment Test Blast")
    }
})
