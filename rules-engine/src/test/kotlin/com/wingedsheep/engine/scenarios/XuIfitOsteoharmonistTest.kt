package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Xu-Ifit, Osteoharmonist (EOE).
 *
 * "{T}: Return target creature card from your graveyard to the battlefield. It's a Skeleton
 *  in addition to its other types and has no abilities. Activate only as a sorcery."
 *
 * Pins two guarantees of the reanimate-with-ability-strip pattern:
 *  1. The reanimated permanent gains the Skeleton subtype (layer 4) and loses all abilities
 *     (layer 6), while keeping its other creature types.
 *  2. Its OWN dies / leaves-battlefield triggers do NOT fire when it later leaves the
 *     battlefield (CR 603.10a — leaves-the-battlefield triggers look back in time at the
 *     object's appearance immediately prior to the event; with abilities stripped at LTB,
 *     there are no triggered abilities to fire).
 *
 * Festering Goblin is the canonical case: a 1/1 with "When Festering Goblin dies, target
 * creature gets -1/-1 until end of turn."
 */
class XuIfitOsteoharmonistTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        context("Xu-Ifit, Osteoharmonist") {

            test("reanimates a creature card as a Skeleton with no abilities") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardOnBattlefield(1, "Xu-Ifit, Osteoharmonist")
                    .withCardInGraveyard(1, "Festering Goblin")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val xuIfit = game.findPermanent("Xu-Ifit, Osteoharmonist")!!
                val festeringGoblin = game.state.getGraveyard(game.player1Id).first { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Festering Goblin"
                }
                val abilityId = cardRegistry.getCard("Xu-Ifit, Osteoharmonist")!!
                    .script.activatedAbilities.first().id

                val activate = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = xuIfit,
                        abilityId = abilityId,
                        targets = listOf(
                            ChosenTarget.Card(festeringGoblin, game.player1Id, Zone.GRAVEYARD)
                        )
                    )
                )
                withClue("Activating Xu-Ifit's reanimate ability should succeed: ${activate.error}") {
                    activate.error shouldBe null
                }
                game.resolveStack()

                game.isOnBattlefield("Festering Goblin") shouldBe true

                val reanimated = game.findPermanent("Festering Goblin")!!
                val projected = stateProjector.project(game.state)

                withClue("reanimated creature is a Skeleton (layer 4 add-subtype)") {
                    projected.hasSubtype(reanimated, Subtype.SKELETON.value) shouldBe true
                }
                withClue("reanimated creature retains its other creature types") {
                    // Festering Goblin is printed as Zombie Goblin — both subtypes survive.
                    projected.hasSubtype(reanimated, "Zombie") shouldBe true
                    projected.hasSubtype(reanimated, "Goblin") shouldBe true
                }
                withClue("reanimated creature has no abilities (layer 6 strip)") {
                    projected.hasLostAllAbilities(reanimated) shouldBe true
                }
            }

            test("reanimated creature's own dies trigger does not fire (CR 603.10a)") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardOnBattlefield(1, "Xu-Ifit, Osteoharmonist")
                    .withCardInGraveyard(1, "Festering Goblin")
                    // Witness: Festering Goblin's dies trigger reads "target creature gets
                    // -1/-1 until end of turn". If it fired against this 2/2 the projected
                    // power would drop to 1.
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val xuIfit = game.findPermanent("Xu-Ifit, Osteoharmonist")!!
                val grizzly = game.findPermanent("Grizzly Bears")!!
                val festeringGoblin = game.state.getGraveyard(game.player1Id).first { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Festering Goblin"
                }
                val abilityId = cardRegistry.getCard("Xu-Ifit, Osteoharmonist")!!
                    .script.activatedAbilities.first().id

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = xuIfit,
                        abilityId = abilityId,
                        targets = listOf(
                            ChosenTarget.Card(festeringGoblin, game.player1Id, Zone.GRAVEYARD)
                        )
                    )
                ).error shouldBe null
                game.resolveStack()

                val reanimated = game.findPermanent("Festering Goblin")!!

                // Lightning Bolt sends the reanimated 1/1 to the graveyard.
                game.castSpell(1, "Lightning Bolt", reanimated).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(1, "Festering Goblin") shouldBe true

                withClue(
                    "Festering Goblin's 'When this dies' trigger must NOT fire — its abilities " +
                        "were stripped at LTB, and CR 603.10a evaluates leaves-the-battlefield " +
                        "triggers against the object's appearance immediately prior to the event."
                ) {
                    game.state.stack.isEmpty() shouldBe true
                }
                withClue("Grizzly Bears must not have been shrunk by a (suppressed) dies trigger") {
                    val projected = stateProjector.project(game.state)
                    projected.getPower(grizzly) shouldBe 2
                    projected.getToughness(grizzly) shouldBe 2
                }
            }
        }
    }
}
