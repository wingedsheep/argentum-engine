package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.fin.cards.ZidaneTantalusThief
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Zidane, Tantalus Thief (FIN uncommon, {3}{R}{W}, 3/3 Legendary Creature —
 * Human Mutant Scout).
 *
 *  1. ETB: "gain control of target creature an opponent controls until end of turn. Untap it. It
 *     gains lifelink and haste until end of turn." — composed from existing primitives
 *     (GainControl + Untap + GrantKeyword), same shape as Zealous Conscripts.
 *  2. "Whenever an opponent gains control of a permanent from you, you create a Treasure token." —
 *     the new resident control-change watcher
 *     ([com.wingedsheep.sdk.dsl.Triggers.OpponentGainsControlOfYourPermanent]). Per the official
 *     ruling it fires once for each permanent stolen — including Zidane itself, whose old controller
 *     still gets the Treasure (look-back-in-time, CR 603.10).
 */
class ZidaneTantalusThiefScenarioTest : ScenarioTestBase() {

    // A simple "Threaten"-style steal used to drive control changes in either direction.
    private val steal = card("Test Steal") {
        manaCost = "{1}{U}"
        colorIdentity = "U"
        typeLine = "Sorcery"
        oracleText = "Gain control of target creature."
        spell {
            val t = target("target creature", Targets.Creature)
            effect = Effects.GainControl(t, Duration.Permanent)
        }
        metadata { rarity = Rarity.COMMON; collectorNumber = "1" }
    }

    init {
        cardRegistry.register(ZidaneTantalusThief)
        cardRegistry.register(steal)

        context("Zidane, Tantalus Thief") {

            test("ETB steals an opponent's creature until end of turn, untaps it, grants lifelink and haste") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Zidane, Tantalus Thief")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    // Opponent's creature starts tapped to prove the "Untap it" clause.
                    .withCardOnBattlefield(2, "Grizzly Bears", tapped = true)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")
                bears.shouldNotBeNull()
                game.state.projectedState.getController(bears) shouldBe game.player2Id

                game.castSpell(1, "Zidane, Tantalus Thief")
                game.resolveStack()

                withClue("ETB trigger should pause to choose the creature to steal") {
                    (game.state.pendingDecision as? ChooseTargetsDecision).shouldNotBeNull()
                }
                game.selectTargets(listOf(bears))
                game.resolveStack()

                withClue("Player 1 gains control of the Bears") {
                    game.state.projectedState.getController(bears) shouldBe game.player1Id
                }
                withClue("The Bears is untapped") {
                    game.state.getEntity(bears)!!.has<TappedComponent>() shouldBe false
                }
                withClue("The Bears gains lifelink") {
                    game.state.projectedState.hasKeyword(bears, Keyword.LIFELINK) shouldBe true
                }
                withClue("The Bears gains haste") {
                    game.state.projectedState.hasKeyword(bears, Keyword.HASTE) shouldBe true
                }
            }

            test("an opponent gaining control of another of your permanents creates a Treasure for you") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Zidane, Tantalus Thief")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(2, "Test Steal")
                    .withLandsOnBattlefield(2, "Island", 2)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(2, "Test Steal", bears).error shouldBe null
                game.resolveStack()

                withClue("Player 2 now controls the stolen Bears") {
                    game.state.projectedState.getController(bears) shouldBe game.player2Id
                }
                val p1Treasures = game.findPermanents("Treasure")
                    .filter { game.state.projectedState.getController(it) == game.player1Id }
                withClue("Player 1 (the player who lost the permanent) gets exactly one Treasure") {
                    p1Treasures.size shouldBe 1
                }
            }

            test("an opponent stealing Zidane itself still creates a Treasure for its old controller") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Zidane, Tantalus Thief")
                    .withCardInHand(2, "Test Steal")
                    .withLandsOnBattlefield(2, "Island", 2)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val zidane = game.findPermanent("Zidane, Tantalus Thief")!!
                game.castSpell(2, "Test Steal", zidane).error shouldBe null
                game.resolveStack()

                withClue("Player 2 now controls Zidane") {
                    game.state.projectedState.getController(zidane) shouldBe game.player2Id
                }
                val p1Treasures = game.findPermanents("Treasure")
                    .filter { game.state.projectedState.getController(it) == game.player1Id }
                withClue("Per the ruling, the old controller still creates a Treasure when Zidane is stolen") {
                    p1Treasures.size shouldBe 1
                }
            }

            test("YOU gaining control of an opponent's creature does not create a Treasure") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Zidane, Tantalus Thief")
                    .withCardInHand(1, "Test Steal")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Test Steal", bears).error shouldBe null
                game.resolveStack()

                game.state.projectedState.getController(bears) shouldBe game.player1Id
                withClue("No permanent left your control to an opponent, so no Treasure is made") {
                    game.findPermanents("Treasure").shouldBeEmpty()
                }
            }
        }
    }
}
