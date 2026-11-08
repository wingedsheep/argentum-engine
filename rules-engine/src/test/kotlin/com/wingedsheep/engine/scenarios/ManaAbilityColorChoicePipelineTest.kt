package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.core.PermanentTappedForManaEvent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.AdditionalManaOnSourceTap
import com.wingedsheep.sdk.scripting.AdditionalManaOnTap
import com.wingedsheep.sdk.scripting.DampLandManaProduction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * A mana ability that must ask which colour it produced finishes in a different place from one that
 * doesn't: `ActivateAbilityHandler` returns at the decision, and
 * [com.wingedsheep.engine.handlers.continuations.ColorChoiceContinuationResumer] picks the
 * resolution back up once the colour is known. Everything downstream of the produced mana therefore
 * has two entry points, and this pins them to the same behaviour — the shared
 * [com.wingedsheep.engine.handlers.effects.mana.ManaAbilityResolutionPipeline].
 *
 * The gap this covers was real: the resume path used to apply exactly one of these payoffs (the
 * mirror-colour bonus) and silently drop the rest, so "{T}: Add one mana of any color" — the shape
 * Cryptolith Rite grants, and the one the AI always takes because it never supplies a
 * `manaColorChoice` — skipped Damping Sphere, its aura bonuses, its riders and its land-tapped
 * event entirely.
 *
 * Every test here activates **without** a `manaColorChoice`, which is what makes the ability pause.
 */
class ManaAbilityColorChoicePipelineTest : ScenarioTestBase() {

    init {
        cardRegistry.register(
            listOf(PrismCavern, TwinPrismCavern, DampingOrb, AbundanceEngine, VerdantVine)
        )

        context("a mana ability that pauses for its colour still runs the whole tap pipeline") {

            test("Damping Sphere replaces the two mana with one {C}") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Twin Prism Cavern")
                    .withCardOnBattlefield(1, "Damping Orb")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.execute(tapForMana(game, "Twin Prism Cavern", TwinPrismCavern)).error shouldBe null
                chooseColor(game, Color.BLUE)

                val pool = game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()!!
                withClue("a land tapped for two mana produces {C} instead, pool was $pool") {
                    pool.colorless shouldBe 1
                    pool.blue shouldBe 0
                    pool.total shouldBe 1
                }
            }

            test("a mirror bonus and its rider both resolve") {
                // Overabundance's shape: "that player adds one mana of any type that land produced,
                // and this deals 1 damage to the player". The mirror mana used to arrive without
                // the damage, because the rider only ran on the synchronous path.
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Prism Cavern")
                    .withCardOnBattlefield(1, "Abundance Engine")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.execute(tapForMana(game, "Prism Cavern", PrismCavern)).error shouldBe null
                chooseColor(game, Color.RED)

                val pool = game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()!!
                withClue("the chosen red plus one mirrored red, pool was $pool") {
                    pool.red shouldBe 2
                }
                withClue("the rider deals 1 damage to the tapper") {
                    game.getLifeTotal(1) shouldBe 19
                }
            }

            test("an attached 'additional mana on tap' aura fires") {
                // Fertile Ground's shape, in its fixed-colour form.
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Prism Cavern")
                    .withCardOnBattlefield(1, "Verdant Vine")
                    .withCardAttachedTo(1, "Verdant Vine", "Prism Cavern")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.execute(tapForMana(game, "Prism Cavern", PrismCavern)).error shouldBe null
                chooseColor(game, Color.BLUE)

                val pool = game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()!!
                withClue("the chosen blue plus the aura's {G}, pool was $pool") {
                    pool.blue shouldBe 1
                    pool.green shouldBe 1
                }
            }

            test("the land-tapped-for-mana event is still emitted") {
                // Only the event: the generic `landTappedForMana` *trigger* has never been wired to
                // off-stack mana resolution on either path (see Groundchuck and Dirtbag, which uses
                // AdditionalManaOnSourceTap for exactly that reason). What this pins is that the
                // pausing path emits the same event the synchronous one does, so whatever consumes
                // it later sees both.
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Prism Cavern")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.execute(tapForMana(game, "Prism Cavern", PrismCavern)).error shouldBe null

                val decision = game.getPendingDecision() as? ChooseColorDecision
                decision shouldNotBe null
                val resumed = game.submitDecision(ColorChosenResponse(decision!!.id, Color.WHITE))

                withClue("events were ${resumed.events.map { it::class.simpleName }}") {
                    resumed.events.filterIsInstance<PermanentTappedForManaEvent>()
                        .any { it.permanentId == game.findPermanent("Prism Cavern") } shouldBe true
                }
            }
        }
    }

    companion object {

        /** Activate [card]'s mana ability with no colour supplied, so it has to ask. */
        private fun tapForMana(
            game: ScenarioTestBase.TestGame,
            permanentName: String,
            card: com.wingedsheep.sdk.model.CardDefinition
        ) = ActivateAbility(
            playerId = game.player1Id,
            sourceId = game.findPermanent(permanentName)!!,
            abilityId = card.activatedAbilities.first { it.isManaAbility }.id
        )

        private fun chooseColor(game: ScenarioTestBase.TestGame, color: Color) {
            val decision = game.getPendingDecision() as? ChooseColorDecision
            withClue("the any-colour mana ability should have paused for a colour choice") {
                decision shouldNotBe null
            }
            game.submitDecision(ColorChosenResponse(decision!!.id, color)).error shouldBe null
        }

        /** "{T}: Add one mana of any color." */
        private val PrismCavern = card("Prism Cavern") {
            typeLine = "Land"
            activatedAbility {
                manaAbility = true
                cost = Costs.Tap
                effect = Effects.AddAnyColorMana(1)
            }
        }

        /** "{T}: Add two mana of any one color." — enough to trip Damping Sphere. */
        private val TwinPrismCavern = card("Twin Prism Cavern") {
            typeLine = "Land"
            activatedAbility {
                manaAbility = true
                cost = Costs.Tap
                effect = Effects.AddAnyColorMana(2)
            }
        }

        /** Damping Sphere's mana half. */
        private val DampingOrb = card("Damping Orb") {
            typeLine = "Artifact"
            staticAbility { ability = DampLandManaProduction }
        }

        /** Overabundance's shape: mirror the produced colour, then damage the tapper. */
        private val AbundanceEngine = card("Abundance Engine") {
            typeLine = "Enchantment"
            staticAbility {
                ability = AdditionalManaOnSourceTap(
                    sourceFilter = GameObjectFilter.Land,
                    color = null,
                    rider = DealDamageEffect(1, EffectTarget.Controller, damageSource = EffectTarget.Self)
                )
            }
        }

        /** Fertile Ground's shape, fixed to {G}. */
        private val VerdantVine = card("Verdant Vine") {
            typeLine = "Enchantment — Aura"
            staticAbility {
                ability = AdditionalManaOnTap(color = Color.GREEN, amount = DynamicAmount.Fixed(1))
            }
        }
    }
}
