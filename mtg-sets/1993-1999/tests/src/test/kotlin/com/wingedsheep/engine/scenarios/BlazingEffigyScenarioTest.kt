package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Blazing Effigy — {1}{R} 0/3 Creature — Elemental.
 *  - When this creature dies, it deals X damage to target creature, where X is 3 plus the amount
 *    of damage dealt to this creature this turn by other sources named Blazing Effigy.
 *
 * Pinned: damage from a source with another name adds nothing (X = 3), and an Effigy killed by
 * another Effigy's death trigger adds that damage — read after both Effigies have left the
 * battlefield.
 */
class BlazingEffigyScenarioTest : ScenarioTestBase() {

    private val dummy = card("Effigy Target Dummy") {
        manaCost = "{5}"
        typeLine = "Creature — Wall"
        power = 0
        toughness = 20
    }

    init {
        cardRegistry.register(dummy)

        context("Blazing Effigy") {
            test("killed by a Lightning Bolt it deals 3 — the Bolt isn't named Blazing Effigy") {
                val game = scenario()
                    .withPlayers("Caster", "Effigy Owner")
                    .withCardOnBattlefield(2, "Blazing Effigy")
                    .withCardOnBattlefield(1, "Effigy Target Dummy")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Lightning Bolt", game.findPermanent("Blazing Effigy")!!).error shouldBe null
                game.resolveStack()

                val dummyId = game.findPermanent("Effigy Target Dummy")!!
                game.selectTargets(listOf(dummyId)).error shouldBe null
                game.resolveStack()

                game.state.getEntity(dummyId)?.get<DamageComponent>()?.amount shouldBe 3
            }

            test("an Effigy killed by another Effigy's trigger deals 3 plus that damage") {
                val game = scenario()
                    .withPlayers("Caster", "Effigy Owner")
                    .withCardOnBattlefield(2, "Blazing Effigy")
                    .withCardOnBattlefield(2, "Blazing Effigy")
                    .withCardOnBattlefield(1, "Effigy Target Dummy")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val (first, second) = game.findPermanents("Blazing Effigy")
                game.castSpell(1, "Lightning Bolt", first).error shouldBe null
                game.resolveStack()

                withClue("the first Effigy's trigger (X = 3) kills the 0/3 second Effigy") {
                    game.selectTargets(listOf(second)).error shouldBe null
                    game.resolveStack()
                    game.findPermanents("Blazing Effigy").isEmpty() shouldBe true
                }

                val dummyId = game.findPermanent("Effigy Target Dummy")!!
                game.selectTargets(listOf(dummyId)).error shouldBe null
                game.resolveStack()

                withClue("X = 3 + 3 damage from the other Effigy") {
                    game.state.getEntity(dummyId)?.get<DamageComponent>()?.amount shouldBe 6
                }
            }
        }
    }
}
