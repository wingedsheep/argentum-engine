package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.mrd.cards.NightmareLash
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Nightmare Lash (MRD #219, {4}, Artifact — Equipment).
 *
 *   Equipped creature gets +1/+1 for each Swamp you control.
 *   Equip—Pay 3 life.
 *
 * Two things are worth pinning here, and neither is visible from the card definition alone:
 *
 *  - the equip cost is **life, not mana**, so it can't go through `equipAbility(...)`; these tests
 *    prove the hand-rolled `isEquipAbility = true` ability still equips, and that it actually
 *    charges the 3 life rather than attaching for free;
 *  - "each Swamp you control" is the land *subtype* scoped to the Equipment's controller — a Bayou
 *    counts, an opponent's Swamp does not — and the bonus is a Layer 7c dynamic amount recomputed
 *    at projection, so playing a Swamp grows the equipped creature immediately.
 */
class NightmareLashScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()
    private val equipAbility = NightmareLash.activatedAbilities.single { it.isEquipAbility }

    init {
        context("Nightmare Lash") {

            test("equipping costs 3 life and grants +1/+1 for each Swamp you control") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Nightmare Lash")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val lash = game.findPermanent("Nightmare Lash")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                withClue("Grizzly Bears is a plain 2/2 before the Lash is attached") {
                    val before = stateProjector.project(game.state)
                    before.getPower(bears) shouldBe 2
                    before.getToughness(bears) shouldBe 2
                }

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = lash,
                        abilityId = equipAbility.id,
                        targets = listOf(ChosenTarget.Permanent(bears))
                    )
                )
                withClue("Activating equip should succeed: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                withClue("The Lash should be attached to Grizzly Bears") {
                    game.state.getEntity(lash)?.get<AttachedToComponent>()?.targetId shouldBe bears
                }
                withClue("Equip—Pay 3 life is a real cost, not a free attach") {
                    game.getLifeTotal(1) shouldBe 17
                }
                withClue("Three Swamps turns the 2/2 into a 5/5") {
                    val after = stateProjector.project(game.state)
                    after.getPower(bears) shouldBe 5
                    after.getToughness(bears) shouldBe 5
                }
            }

            test("the bonus is recomputed as Swamps arrive, and is +0/+0 with none") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardAttachedTo(1, "Nightmare Lash", "Grizzly Bears")
                    .withCardInHand(1, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!

                withClue("No Swamps means no bonus at all — the Equipment is inert") {
                    val before = stateProjector.project(game.state)
                    before.getPower(bears) shouldBe 2
                    before.getToughness(bears) shouldBe 2
                }

                val swamp = game.findCardsInHand(1, "Swamp").first()
                val played = game.execute(PlayLand(game.player1Id, swamp))
                withClue("Playing the Swamp should succeed: ${played.error}") { played.error shouldBe null }

                withClue("The dynamic bonus follows the board — one Swamp, +1/+1") {
                    val after = stateProjector.project(game.state)
                    after.getPower(bears) shouldBe 3
                    after.getToughness(bears) shouldBe 3
                }
            }

            test("a nonbasic land with the Swamp subtype counts") {
                // "Swamp" is the land subtype, not the card name — Bayou is a Swamp Forest.
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardAttachedTo(1, "Nightmare Lash", "Grizzly Bears")
                    .withCardOnBattlefield(1, "Bayou")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!

                withClue("Bayou and the basic Swamp are two Swamps, so +2/+2") {
                    val projected = stateProjector.project(game.state)
                    projected.getPower(bears) shouldBe 4
                    projected.getToughness(bears) shouldBe 4
                }
            }

            test("only Swamps you control count") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardAttachedTo(1, "Nightmare Lash", "Grizzly Bears")
                    .withLandsOnBattlefield(2, "Swamp", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!

                withClue("Four opposing Swamps are worth nothing — 'you control' scopes to the Lash") {
                    val projected = stateProjector.project(game.state)
                    projected.getPower(bears) shouldBe 2
                    projected.getToughness(bears) shouldBe 2
                }
            }
        }
    }
}
