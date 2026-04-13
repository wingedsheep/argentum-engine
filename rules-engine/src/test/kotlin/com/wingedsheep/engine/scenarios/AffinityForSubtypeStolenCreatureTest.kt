package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ControlEnchantedPermanent
import com.wingedsheep.sdk.scripting.KeywordAbility
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests that Affinity for subtypes correctly counts stolen creatures.
 *
 * Affinity for Frogs should count Frogs the player *controls*, not just ones they own.
 * Control-changing effects (e.g., Kitnap-style auras) should make the stolen Frog
 * reduce the cost.
 */
class AffinityForSubtypeStolenCreatureTest : FunSpec({

    val projector = StateProjector()

    val FrogCreature = CardDefinition.creature(
        name = "Test Frog",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype("Frog")),
        power = 2,
        toughness = 2
    )

    val AffinityForFrogsSpell = CardDefinition(
        name = "Affinity Frog Spell",
        manaCost = ManaCost.parse("{3}{G}"),
        typeLine = com.wingedsheep.sdk.core.TypeLine(
            cardTypes = setOf(com.wingedsheep.sdk.core.CardType.INSTANT)
        ),
        oracleText = "Affinity for Frogs",
        keywordAbilities = listOf(KeywordAbility.AffinityForSubtype(Subtype.FROG))
    )

    val ControlAura = CardDefinition.enchantment(
        name = "Control Aura",
        manaCost = ManaCost.parse("{3}{U}"),
        script = CardScript(
            staticAbilities = listOf(ControlEnchantedPermanent)
        )
    )

    fun GameTestDriver.attachAura(auraId: EntityId, targetId: EntityId) {
        replaceState(state.updateEntity(auraId) { container ->
            container.with(AttachedToComponent(targetId))
        })
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(FrogCreature)
        driver.registerCard(AffinityForFrogsSpell)
        driver.registerCard(ControlAura)
        return driver
    }

    test("Affinity for Frogs counts stolen Frog creature") {
        val registry = CardRegistry()
        registry.register(TestCards.all)
        registry.register(FrogCreature)
        registry.register(AffinityForFrogsSpell)
        registry.register(ControlAura)

        val calculator = CostCalculator(registry)

        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Opponent owns a Frog
        val frog = driver.putCreatureOnBattlefield(opponent, "Test Frog")

        // Without control: active player controls 0 Frogs → no reduction
        val costBefore = calculator.calculateEffectiveCost(
            driver.state,
            registry.requireCard("Affinity Frog Spell"),
            activePlayer
        )
        costBefore.genericAmount shouldBe 3

        // Steal the Frog with Control Aura
        val aura = driver.putPermanentOnBattlefield(activePlayer, "Control Aura")
        driver.attachAura(aura, frog)

        // Verify control actually changed
        val projected = projector.project(driver.state)
        projected.getController(frog) shouldBe activePlayer

        // With control: active player now controls 1 Frog → cost reduced by 1
        val costAfter = calculator.calculateEffectiveCost(
            driver.state,
            registry.requireCard("Affinity Frog Spell"),
            activePlayer
        )
        costAfter.genericAmount shouldBe 2
    }

    test("Affinity for Frogs counts own Frogs and stolen Frogs") {
        val registry = CardRegistry()
        registry.register(TestCards.all)
        registry.register(FrogCreature)
        registry.register(AffinityForFrogsSpell)
        registry.register(ControlAura)

        val calculator = CostCalculator(registry)

        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Active player owns 1 Frog
        driver.putCreatureOnBattlefield(activePlayer, "Test Frog")

        // Opponent owns another Frog
        val opponentFrog = driver.putCreatureOnBattlefield(opponent, "Test Frog")

        // Steal opponent's Frog
        val aura = driver.putPermanentOnBattlefield(activePlayer, "Control Aura")
        driver.attachAura(aura, opponentFrog)

        // Active player now controls 2 Frogs → cost reduced by 2
        val effectiveCost = calculator.calculateEffectiveCost(
            driver.state,
            registry.requireCard("Affinity Frog Spell"),
            activePlayer
        )
        // {3}{G} - 2 generic = {1}{G}
        effectiveCost.genericAmount shouldBe 1
    }
})
