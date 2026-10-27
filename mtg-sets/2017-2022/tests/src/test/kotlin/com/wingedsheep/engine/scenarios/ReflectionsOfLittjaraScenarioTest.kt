package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CastChoicesComponent
import com.wingedsheep.engine.state.components.battlefield.ChoiceValue
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.khm.cards.ReflectionsOfLittjara
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.matchers.shouldBe

/**
 * Reflections of Littjara — "Whenever you cast a spell of the chosen type, copy that spell."
 *
 * The chosen type is pinned on the enchantment's [CastChoicesComponent], as its entry choice
 * leaves it. A creature spell of that type is copied into a token; a kindred spell of that type is
 * copied too (ruling) and, with no "you may choose new targets", its copy keeps the original's
 * target without a prompt; a spell of another type is not copied.
 */
class ReflectionsOfLittjaraScenarioTest : ScenarioTestBase() {

    private val elf = card("Littjara Test Elf") {
        manaCost = "{1}"
        typeLine = "Creature — Elf"
        power = 1
        toughness = 1
    }
    private val goblin = card("Littjara Test Goblin") {
        manaCost = "{1}"
        typeLine = "Creature — Goblin"
        power = 1
        toughness = 1
    }
    private val elfBolt = card("Littjara Test Elf Bolt") {
        manaCost = "{1}"
        typeLine = "Kindred Instant — Elf"
        spell {
            val t = target(Targets.Any)
            effect = Effects.DealDamage(DynamicAmounts.fixed(2), t)
        }
    }

    private fun board(): TestGame {
        val game = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Reflections of Littjara")
            .withLandsOnBattlefield(1, "Island", 2)
            .withCardInHand(1, elf.name)
            .withCardInHand(1, goblin.name)
            .withCardInHand(1, elfBolt.name)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        val reflections = game.findPermanent("Reflections of Littjara")!!
        game.state = game.state.updateEntity(reflections) {
            it.with(CastChoicesComponent(chosen = mapOf(ChoiceSlot.CREATURE_TYPE to ChoiceValue.TextChoice("Elf"))))
        }
        return game
    }

    init {
        cardRegistry.register(ReflectionsOfLittjara)
        cardRegistry.register(listOf(elf, goblin, elfBolt))

        test("a creature spell of the chosen type is copied into a token") {
            val game = board()
            game.castSpell(1, elf.name).error shouldBe null
            game.resolveStack()
            game.findAllPermanents(elf.name).size shouldBe 2
        }

        test("a creature spell of another type is not copied") {
            val game = board()
            game.castSpell(1, goblin.name).error shouldBe null
            game.resolveStack()
            game.findAllPermanents(goblin.name).size shouldBe 1
        }

        test("a kindred spell of the chosen type is copied, keeping its target without a prompt") {
            val game = board()
            game.castSpellTargetingPlayer(1, elfBolt.name, 2).error shouldBe null
            game.resolveStack()
            game.hasPendingDecision() shouldBe false
            game.state.stack.isEmpty() shouldBe true
            game.getLifeTotal(2) shouldBe 16
            game.getLifeTotal(1) shouldBe 20
        }
    }
}
