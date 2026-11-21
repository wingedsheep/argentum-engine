package com.wingedsheep.engine.event

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * `DealsDamageEvent(perInstruction = true)` — "whenever an instant or sorcery spell you control
 * deals damage" fires once per damage instruction (Blaze Commando's 2013-04-15 ruling: once per
 * "deals" in the spell's text). One damage effect is one instruction however many recipients it
 * hits; an adjacent damage effect from the same source continues it ("deals 1 damage to each
 * opponent and 2 damage to each opponent" authored as two steps); any other step in between, or a
 * different source, opens a new one.
 */
class PerInstructionDamageTriggerTest : ScenarioTestBase() {

    private val opponents = EffectTarget.PlayerRef(Player.EachOpponent)

    init {
        // Each trigger gains its controller 1 life, so life gained counts the firings.
        cardRegistry.register(card("Instruction Counter") {
            typeLine = "Creature — Elemental"; power = 1; toughness = 1
            triggeredAbility {
                trigger = Triggers.a(GameObjectFilter.InstantOrSorcery.youControl()).dealsDamage(perInstruction = true)
                effect = Effects.GainLife(1)
            }
        })
        cardRegistry.register(card("Adjacent Singe") {
            manaCost = "{R}"; typeLine = "Sorcery"
            spell { effect = Effects.DealDamage(1, opponents) then Effects.DealDamage(2, opponents) }
        })
        cardRegistry.register(card("Interrupted Singe") {
            manaCost = "{R}"; typeLine = "Sorcery"
            spell { effect = Effects.DealDamage(1, opponents) then Effects.GainLife(10) then Effects.DealDamage(2, opponents) }
        })
        cardRegistry.register(card("Each Player Singe") {
            manaCost = "{R}"; typeLine = "Sorcery"
            spell { effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.Each)) }
        })

        fun cast(spell: String): TestGame {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Instruction Counter")
                .withCardInHand(1, spell)
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.castSpell(1, spell).error shouldBe null
            game.resolveStack()
            return game
        }

        context("per-instruction damage observer") {
            test("adjacent damage steps from the same source are one instruction") {
                val game = cast("Adjacent Singe")
                game.getLifeTotal(2) shouldBe 17
                withClue("one firing") { game.getLifeTotal(1) shouldBe 21 }
            }

            test("a step between two damage steps makes them two instructions") {
                val game = cast("Interrupted Singe")
                game.getLifeTotal(2) shouldBe 17
                withClue("10 from the spell, two firings") { game.getLifeTotal(1) shouldBe 32 }
            }

            test("one damage effect hitting several players is one instruction") {
                val game = cast("Each Player Singe")
                game.getLifeTotal(2) shouldBe 19
                withClue("dealt 1, one firing") { game.getLifeTotal(1) shouldBe 20 }
            }

            test("batch and perInstruction are exclusive") {
                shouldThrow<IllegalArgumentException> {
                    EventPattern.DealsDamageEvent(batch = true, perInstruction = true)
                }
            }
        }
    }
}
