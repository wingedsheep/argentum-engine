package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.soi.cards.BoundByMoonsilver
import com.wingedsheep.mtg.sets.definitions.spm.cards.MilesMorales
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Bound by Moonsilver (SOI #7) — {2}{W} Enchantment — Aura.
 *
 * "Enchant creature
 *  Enchanted creature can't attack, block, or transform.
 *  Sacrifice another permanent: Attach this Aura to target creature. Activate only as a sorcery
 *  and only once each turn."
 *
 * The can't-attack / can't-block halves are the same statics Pacifism uses; what's new here is the
 * `AbilityFlag.CANT_TRANSFORM` prohibition, enforced in the engine's shared transform-in-place
 * implementation. These tests pin:
 *  1. a transform ability of the enchanted creature still *activates and resolves* (per the ruling),
 *     but the flip itself does nothing;
 *  2. the same creature does flip when it isn't enchanted (control, so the test can't pass vacuously);
 *  3. the move ability re-attaches the Aura and is limited to once each turn.
 */
class BoundByMoonsilverScenarioTest : ScenarioTestBase() {

    init {
        val transformAbilityId = MilesMorales.activatedAbilities.first { !it.isManaAbility }.id
        val moveAbilityId = BoundByMoonsilver.activatedAbilities.first { !it.isManaAbility }.id

        // Miles Morales must be cast from hand for the engine to attach the DoubleFacedComponent his
        // Transform ability needs — the scenario builder only wires that for back faces.
        fun gameWithMilesOnBattlefield(): Pair<TestGame, com.wingedsheep.sdk.model.EntityId> {
            val game = scenario()
                .withPlayers("You", "Opponent")
                .withCardInHand(1, "Miles Morales")
                .withCardInHand(1, "Bound by Moonsilver")
                .withLandsOnBattlefield(1, "Mountain", 5)
                .withLandsOnBattlefield(1, "Forest", 5)
                .withLandsOnBattlefield(1, "Plains", 5)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Miles Morales").error shouldBe null
            if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
            game.resolveStack()
            // ETB targets are optional ("up to two"); take none.
            if (game.getPendingDecision() is ChooseTargetsDecision) game.selectTargets(emptyList())
            game.resolveStack()

            return game to game.findPermanent("Miles Morales")!!
        }

        context("Bound by Moonsilver") {

            test("an enchanted creature's transform ability resolves but the flip doesn't happen") {
                val (game, miles) = gameWithMilesOnBattlefield()

                game.castSpell(1, "Bound by Moonsilver", miles).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                val aura = game.findPermanent("Bound by Moonsilver")!!
                withClue("the Aura is attached to Miles") {
                    game.state.getEntity(aura)!!.get<AttachedToComponent>()!!.targetId shouldBe miles
                }

                // Per the ruling, the transform ability itself is still activatable.
                val activation = game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = miles, abilityId = transformAbilityId)
                )
                withClue("the transform ability can still be activated: ${activation.error}") {
                    activation.error shouldBe null
                }
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("but Miles does not flip — he's still on his front face") {
                    game.state.getEntity(miles)!!.get<CardComponent>()!!.name shouldBe "Miles Morales"
                }
            }

            test("the same creature does flip when it isn't enchanted") {
                val (game, miles) = gameWithMilesOnBattlefield()

                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = miles, abilityId = transformAbilityId)
                ).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("without the Aura the transform goes through") {
                    game.state.getEntity(miles)!!.get<CardComponent>()!!.name shouldBe "Ultimate Spider-Man"
                }
            }

            test("sacrificing another permanent moves the Aura, and only once each turn") {
                val game = scenario()
                    .withPlayers("You", "Opponent")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardAttachedTo(1, "Bound by Moonsilver", "Grizzly Bears")
                    .withCardOnBattlefield(1, "Ornithopter")  // fodder for the sacrifice cost
                    .withLandsOnBattlefield(1, "Plains", 1)   // spare fodder, so the second attempt
                    .withActivePlayer(1)                      // fails on the once-per-turn limit only
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val aura = game.findPermanent("Bound by Moonsilver")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!
                val thopter = game.findPermanent("Ornithopter")!!

                game.state.getEntity(aura)!!.get<AttachedToComponent>()!!.targetId shouldBe bears

                val activation = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = aura,
                        abilityId = moveAbilityId,
                        targets = listOf(ChosenTarget.Permanent(giant))
                    )
                )
                withClue("the move ability should activate: ${activation.error}") {
                    activation.error shouldBe null
                }
                // The sacrifice cost asks which permanent to give up.
                if (game.getPendingDecision() != null) game.selectCards(listOf(thopter))
                game.resolveStack()

                withClue("the Aura moved onto Hill Giant") {
                    game.state.getEntity(aura)!!.get<AttachedToComponent>()!!.targetId shouldBe giant
                }
                withClue("the sacrifice cost was actually paid") {
                    game.isOnBattlefield("Ornithopter") shouldBe false
                }

                // "only once each turn" — a second activation this turn is illegal.
                val second = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = aura,
                        abilityId = moveAbilityId,
                        targets = listOf(ChosenTarget.Permanent(bears))
                    )
                )
                withClue("a second activation in the same turn must be rejected") {
                    second.error shouldNotBe null
                }
            }
        }
    }
}
