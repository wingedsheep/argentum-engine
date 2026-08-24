package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.fem.cards.ThrullRetainer
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Thrull Retainer (Fallen Empires).
 *
 * Oracle: "Enchant creature / Enchanted creature gets +1/+1. / Sacrifice this Aura: Regenerate
 * enchanted creature."
 *
 * The interesting half is the ability paying for itself: the Aura is gone before the effect
 * resolves, so "enchanted creature" can only be read as last-known information (CR 608.2h) — the
 * host it was attached to when the cost was paid.
 */
class ThrullRetainerScenarioTest : ScenarioTestBase() {

    private val abilityId = ThrullRetainer.activatedAbilities.first().id

    init {
        context("Thrull Retainer — sacrifice to regenerate the enchanted creature") {

            test("the enchanted creature gets +1/+1 while the Aura is attached") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardAttachedTo(1, "Thrull Retainer", "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.state.projectedState.getPower(bears) shouldBe 3
                game.state.projectedState.getToughness(bears) shouldBe 3
            }

            test("sacrificing the Aura leaves a regeneration shield on its host") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardAttachedTo(1, "Thrull Retainer", "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val retainer = game.findPermanent("Thrull Retainer")!!
                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = retainer, abilityId = abilityId)
                ).error shouldBe null
                game.resolveStack()

                withClue("the Aura sacrificed itself to pay") {
                    game.isOnBattlefield("Thrull Retainer") shouldBe false
                }
                val card = game.getClientState(1).cards.values.first { it.name == "Grizzly Bears" }
                withClue("the host should carry a regeneration shield") {
                    card.activeEffects.any { it.name.startsWith("Regen") } shouldBe true
                }
            }

            test("the shield saves the host from lethal damage") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardAttachedTo(1, "Thrull Retainer", "Grizzly Bears")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val retainer = game.findPermanent("Thrull Retainer")!!
                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = retainer, abilityId = abilityId)
                ).error shouldBe null
                game.resolveStack()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Lightning Bolt", bears).error shouldBe null
                game.resolveStack()

                withClue("regeneration replaces the destruction — a 2/2 survives a Bolt") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }
                withClue("regenerating taps the creature and removes its damage") {
                    game.state.projectedState.getToughness(bears) shouldBe 2
                }
            }
        }
    }
}
