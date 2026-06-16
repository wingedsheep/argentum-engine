package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.otj.cards.ErietteTheBeguiler
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Eriette, the Beguiler (OTJ rare Human Warlock), {1}{W}{U}{B}, 4/4.
 *
 * Lifelink
 * Whenever an Aura you control becomes attached to a nonland permanent an opponent controls with
 * mana value less than or equal to that Aura's mana value, gain control of that permanent for as
 * long as that Aura is attached to it.
 *
 * Exercises the new "becomes attached" trigger ([com.wingedsheep.sdk.scripting.EventPattern.BecomesAttachedEvent]),
 * the relative mana-value gate (permanent MV ≤ Aura MV), and the
 * [com.wingedsheep.sdk.scripting.Duration.WhileSourceAttachedToAffected] control duration (control
 * reverts when the Aura leaves).
 */
class ErietteTheBeguilerScenarioTest : ScenarioTestBase() {

    // MV-2 Aura with no rider — used to enchant an opponent's creature. ({1}{W} = MV 2.)
    private val cheapAura = card("Test Cheap Aura") {
        manaCost = "{1}{W}"
        colorIdentity = "W"
        typeLine = "Enchantment — Aura"
        oracleText = "Enchant creature"
        auraTarget = Targets.Creature
        metadata { rarity = Rarity.COMMON; collectorNumber = "1" }
    }

    // MV-4 Aura — confirms an MV-4 Aura can steal an MV-2 creature (2 <= 4).
    private val expensiveAura = card("Test Expensive Aura") {
        manaCost = "{2}{W}{W}"
        colorIdentity = "W"
        typeLine = "Enchantment — Aura"
        oracleText = "Enchant creature"
        auraTarget = Targets.Creature
        metadata { rarity = Rarity.COMMON; collectorNumber = "2" }
    }

    // Disenchant-style removal used to prove control reverts when the Aura leaves play.
    private val disenchant = card("Test Disenchant") {
        manaCost = "{1}{W}"
        colorIdentity = "W"
        typeLine = "Instant"
        oracleText = "Destroy target enchantment."
        spell {
            val t = target("target enchantment", TargetPermanent(filter = TargetFilter.Enchantment))
            effect = Effects.Destroy(t)
        }
        metadata { rarity = Rarity.COMMON; collectorNumber = "3" }
    }

    init {
        cardRegistry.register(ErietteTheBeguiler)
        cardRegistry.register(cheapAura)
        cardRegistry.register(expensiveAura)
        cardRegistry.register(disenchant)

        context("Eriette, the Beguiler") {
            test("aura attaches to an opponent's creature of equal mana value: gain control") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Eriette, the Beguiler")
                    .withCardInHand(1, "Test Cheap Aura")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    // Opponent's Grizzly Bears (MV 2) ≤ the Aura's MV 2 → control switches.
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")
                bears.shouldNotBeNull()
                withClue("Bears starts under opponent's control") {
                    game.state.getEntity(bears!!)?.get<ControllerComponent>()?.playerId shouldBe game.player2Id
                }

                game.castSpell(1, "Test Cheap Aura", bears!!).error shouldBe null
                game.resolveStack()

                withClue("Player 1 gained control of the enchanted Bears via Eriette") {
                    game.state.projectedState.getController(bears) shouldBe game.player1Id
                }
            }

            test("aura on a HIGHER mana value creature still steals it (MV 2 <= MV 4)") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Eriette, the Beguiler")
                    .withCardInHand(1, "Test Expensive Aura")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withCardOnBattlefield(2, "Grizzly Bears") // MV 2 <= Aura MV 4
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Test Expensive Aura", bears).error shouldBe null
                game.resolveStack()

                withClue("Player 1 stole the MV-2 Bears with the MV-4 Aura") {
                    game.state.projectedState.getController(bears) shouldBe game.player1Id
                }
            }

            test("aura on a creature with mana value GREATER than the Aura's does NOT steal it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Eriette, the Beguiler")
                    .withCardInHand(1, "Test Cheap Aura") // MV 2
                    .withLandsOnBattlefield(1, "Plains", 2)
                    // Gurmag Angler is MV 7 (> Aura MV 2) → the trigger's MV gate fails, no steal.
                    .withCardOnBattlefield(2, "Gurmag Angler")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val angler = game.findPermanent("Gurmag Angler")!!
                game.castSpell(1, "Test Cheap Aura", angler).error shouldBe null
                game.resolveStack()

                withClue("Angler (MV 7) is NOT stolen by an MV-2 Aura") {
                    game.state.projectedState.getController(angler) shouldBe game.player2Id
                }
            }

            test("control reverts when the Aura leaves the battlefield (CR 611.2b)") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Eriette, the Beguiler")
                    .withCardInHand(1, "Test Cheap Aura")
                    .withCardInHand(1, "Test Disenchant")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Test Cheap Aura", bears).error shouldBe null
                game.resolveStack()
                game.state.projectedState.getController(bears) shouldBe game.player1Id

                val aura = game.findPermanent("Test Cheap Aura")!!
                game.castSpell(1, "Test Disenchant", aura).error shouldBe null
                game.resolveStack()

                withClue("Control reverts to the opponent once the Aura is gone") {
                    game.state.projectedState.getController(bears) shouldBe game.player2Id
                }
            }
        }
    }
}
