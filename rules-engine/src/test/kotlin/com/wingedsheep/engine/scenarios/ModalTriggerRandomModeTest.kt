package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * [ModalEffect.chooseAtRandom] — "choose one at random" on a triggered ability (Summon: Magus
 * Sisters). The engine draws the mode from the game's seeded RNG as the ability goes onto the
 * stack, so the controller is never asked; only modes with a legal target are in the draw, and an
 * ability with no choosable mode is removed from the stack (CR 603.3c).
 */
class ModalTriggerRandomModeTest : ScenarioTestBase() {

    private fun gainMode(amount: Int) = mode("You gain $amount life") { effect = Effects.GainLife(amount) }

    private fun destroyMode(label: String, filter: TargetFilter) = mode("Destroy target $label") {
        val permanent = target(filter)
        effect = Effects.Destroy(permanent)
    }

    private fun etbRandom(name: String, vararg modes: Mode) = card(name) {
        manaCost = "{0}"
        typeLine = "Artifact Creature — Construct"
        power = 1
        toughness = 1
        oracleText = "When this creature enters, choose one at random."
        triggeredAbility {
            trigger = Triggers.self.enters()
            effect = ModalEffect.chooseOneAtRandom(*modes)
        }
    }

    private val threeGains = etbRandom("Test Random Gains", gainMode(1), gainMode(2), gainMode(3))
    private val deadFirstMode = etbRandom(
        "Test Random Dead Mode",
        destroyMode("enchantment", TargetFilter.Enchantment),
        gainMode(2)
    )
    private val allDead = etbRandom(
        "Test Random All Dead",
        destroyMode("enchantment", TargetFilter.Enchantment),
        destroyMode("planeswalker", TargetFilter.Planeswalker)
    )

    private fun TestGame.triggersOnStack(): List<TriggeredAbilityOnStackComponent> =
        state.stack.mapNotNull { state.getEntity(it)?.get<TriggeredAbilityOnStackComponent>() }

    /** Cast [cardName] and resolve the creature spell, leaving its ETB trigger announced. */
    private fun castInto(cardName: String, seed: Long): TestGame {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, cardName)
            .withCardInLibrary(1, "Swamp")
            .withCardInLibrary(2, "Forest")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .withRngSeed(seed)
            .build()
        game.castSpell(1, cardName).error shouldBe null
        if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
        // Both players pass: the creature spell resolves and its ETB trigger is announced.
        repeat(2) { game.passPriority().error shouldBe null }
        return game
    }

    init {
        cardRegistry.register(threeGains)
        cardRegistry.register(deadFirstMode)
        cardRegistry.register(allDead)

        test("the mode is drawn without asking the controller, and every mode can come up") {
            val seen = mutableSetOf<Int>()
            for (seed in 1L..40L) {
                val game = castInto("Test Random Gains", seed)
                withClue("seed $seed: no mode question — the engine chooses") {
                    game.getPendingDecision() shouldBe null
                }
                val chosen = game.triggersOnStack().single().chosenModes
                chosen.size shouldBe 1
                seen += chosen.single()

                val lifeBefore = game.getLifeTotal(1)
                game.resolveStack()
                withClue("seed $seed: the announced mode is the one that resolves") {
                    game.getLifeTotal(1) shouldBe lifeBefore + chosen.single() + 1
                }
            }
            seen shouldContainExactlyInAnyOrder listOf(0, 1, 2)
        }

        test("the same seed draws the same mode") {
            val first = castInto("Test Random Gains", 7L).triggersOnStack().single().chosenModes
            val second = castInto("Test Random Gains", 7L).triggersOnStack().single().chosenModes
            first shouldBe second
        }

        test("a mode with no legal target is never drawn") {
            for (seed in 1L..20L) {
                val game = castInto("Test Random Dead Mode", seed)
                withClue("seed $seed: only the life-gain mode is choosable") {
                    game.triggersOnStack().single().chosenModes shouldBe listOf(1)
                }
            }
        }

        test("with no choosable mode the ability is removed from the stack") {
            val game = castInto("Test Random All Dead", 3L)
            game.triggersOnStack().shouldBeEmpty()
            game.getPendingDecision() shouldBe null
        }

        test("a random modal must choose exactly one mode") {
            shouldThrow<IllegalArgumentException> {
                ModalEffect(listOf(gainMode(1), gainMode(2)), chooseCount = 2, chooseAtRandom = true)
            }
        }
    }
}
