package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.mechanics.mana.LandManaColorInspector
import com.wingedsheep.engine.mechanics.mana.ManaColorSetResolver
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.values.LandControllerScope
import com.wingedsheep.sdk.scripting.values.ManaColorSet
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Reflection in "could produce" (CR 106.7): a land whose own mana ability reflects other lands
 * (Reflecting Pool's "any type a land you control could produce", Exotic Orchard's "any color a land
 * an opponent controls could produce") reflects only the *other* lands on the chain. Rulings pinned:
 * - "Multiple Reflecting Pools won't help each other produce mana" — Pools with no other lands
 *   produce nothing, of any color or {C}.
 * - A Pool never reflects itself: its own {C} half doesn't satisfy its own "a land you control
 *   could produce colorless" gate.
 * - Costs and legality are ignored: a land whose any-color ability is restricted still counts.
 * - Cross-controller reflection (two Exotic Orchards facing each other) terminates and yields
 *   only what a real land behind it produces.
 */
class ReflectedLandManaTest : ScenarioTestBase() {

    private val pool = card("Test Pool") {
        typeLine = "Land"
        activatedAbility {
            cost = Costs.Tap
            manaAbility = true
            effect = Effects.AddManaOfColorLandsCouldProduce(LandControllerScope.YOU)
        }
        activatedAbility {
            cost = Costs.Tap
            manaAbility = true
            effect = Effects.AddColorlessMana(1)
            restrictions = listOf(
                ActivationRestriction.OnlyIfCondition(
                    Conditions.YouControl(GameObjectFilter.Land.couldProduceColorlessMana())
                ),
            )
        }
    }

    private val orchard = card("Test Orchard") {
        typeLine = "Land"
        activatedAbility {
            cost = Costs.Tap
            manaAbility = true
            effect = Effects.AddManaOfColorLandsCouldProduce(LandControllerScope.OPPONENTS)
        }
    }

    // Fellwar Stone shape: a nonland reflecting opponents' lands.
    private val stone = card("Test Stone") {
        manaCost = "{2}"
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Tap
            manaAbility = true
            effect = Effects.AddManaOfColorLandsCouldProduce(LandControllerScope.OPPONENTS)
        }
    }

    private val colorlessLand = card("Test Wastes") {
        typeLine = "Land"
        activatedAbility {
            cost = Costs.Tap
            manaAbility = true
            effect = Effects.AddColorlessMana(1)
        }
    }

    // Spire of Industry shape: any color, but only if you control an artifact.
    private val restrictedRainbow = card("Test Spire") {
        typeLine = "Land"
        activatedAbility {
            cost = Costs.Tap
            manaAbility = true
            effect = Effects.AddAnyColorMana()
            restrictions = listOf(
                ActivationRestriction.OnlyIfCondition(Conditions.YouControl(GameObjectFilter.Artifact))
            )
        }
    }

    // Plaza of Harmony shape: a Gate land, reflected by a Gate-filtered color set.
    private val gate = card("Test Gate") {
        typeLine = "Land — Gate"
        activatedAbility {
            cost = Costs.Tap
            manaAbility = true
            effect = Effects.AddMana(Color.WHITE)
        }
    }

    private val gates = GameObjectFilter.Land.withSubtype("Gate")

    private val plaza = card("Test Plaza") {
        typeLine = "Land"
        activatedAbility {
            cost = Costs.Tap
            manaAbility = true
            effect = Effects.AddManaOfColorLandsCouldProduce(LandControllerScope.YOU, landFilter = gates)
        }
    }

    private fun TestGame.colorsFor(
        sourceId: EntityId,
        scope: LandControllerScope,
        landFilter: GameObjectFilter? = null,
    ): Set<Color> =
        ManaColorSetResolver.resolve(
            ManaColorSet.LandsCouldProduce(scope, landFilter),
            state,
            state.projectedState,
            sourceId,
            player1Id,
            cardRegistry,
            services.predicateEvaluator,
        )

    private fun TestGame.couldProduceColorless(entityId: EntityId): Boolean =
        LandManaColorInspector.landCouldProduceColorless(state, state.projectedState, entityId, cardRegistry, services.predicateEvaluator)

    /** The Pool's {C} gate exactly as the activation check evaluates it. */
    private fun TestGame.colorlessGateHolds(): Boolean =
        state.getBattlefield().any { id ->
            services.predicateEvaluator.matches(
                state,
                state.projectedState,
                id,
                GameObjectFilter.Land.youControl().couldProduceColorlessMana(),
                PredicateContext(controllerId = player1Id),
            )
        }

    init {
        cardRegistry.register(pool)
        cardRegistry.register(orchard)
        cardRegistry.register(stone)
        cardRegistry.register(colorlessLand)
        cardRegistry.register(restrictedRainbow)
        cardRegistry.register(gate)
        cardRegistry.register(plaza)

        fun base() = scenario()
            .withPlayers("Player", "Opponent")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        test("a Pool reflects the other lands you control, not itself") {
            val game = base()
                .withCardOnBattlefield(1, "Test Pool")
                .withCardOnBattlefield(1, "Forest", tapped = true)
                .withCardOnBattlefield(2, "Island")
                .build()
            val pool = game.findPermanent("Test Pool")!!

            withClue("a tapped Forest still counts; the opponent's Island doesn't") {
                game.colorsFor(pool, LandControllerScope.YOU) shouldBe setOf(Color.GREEN)
            }
            withClue("no land could produce {C}") {
                game.couldProduceColorless(pool) shouldBe false
                game.colorlessGateHolds() shouldBe false
            }
        }

        test("a lone Pool produces nothing") {
            val game = base().withCardOnBattlefield(1, "Test Pool").build()
            val pool = game.findPermanent("Test Pool")!!

            game.colorsFor(pool, LandControllerScope.YOU) shouldBe emptySet()
            withClue("its own {C} half doesn't satisfy its own gate") {
                game.couldProduceColorless(pool) shouldBe false
                game.colorlessGateHolds() shouldBe false
            }
        }

        test("two Pools and no other lands don't help each other") {
            val game = base()
                .withCardOnBattlefield(1, "Test Pool")
                .withCardOnBattlefield(1, "Test Pool")
                .build()
            val pool = game.findPermanent("Test Pool")!!

            game.colorsFor(pool, LandControllerScope.YOU) shouldBe emptySet()
            game.colorlessGateHolds() shouldBe false
        }

        test("two Pools plus a real land both reflect that land, colors and {C}") {
            val game = base()
                .withCardOnBattlefield(1, "Test Pool")
                .withCardOnBattlefield(1, "Test Pool")
                .withCardOnBattlefield(1, "Swamp")
                .withCardOnBattlefield(1, "Test Wastes")
                .build()
            val pool = game.findPermanent("Test Pool")!!

            game.colorsFor(pool, LandControllerScope.YOU) shouldBe setOf(Color.BLACK)
            game.couldProduceColorless(pool) shouldBe true
            game.colorlessGateHolds() shouldBe true
        }

        test("a land's costs and activation restrictions are ignored") {
            val game = base()
                .withCardOnBattlefield(1, "Test Pool")
                .withCardOnBattlefield(1, "Test Spire")
                .build()
            val pool = game.findPermanent("Test Pool")!!

            withClue("no artifact, yet the Spire's any-color ability counts") {
                game.colorsFor(pool, LandControllerScope.YOU) shouldBe Color.entries.toSet()
            }
        }

        test("a Stone facing an opponent's Pool and Island reflects only blue") {
            val game = base()
                .withCardOnBattlefield(1, "Test Stone")
                .withCardOnBattlefield(2, "Test Pool")
                .withCardOnBattlefield(2, "Island")
                .build()

            withClue("the opponent's Pool reflects their Island, not all five colors") {
                game.colorsFor(game.findPermanent("Test Stone")!!, LandControllerScope.OPPONENTS) shouldBe setOf(Color.BLUE)
            }
        }

        test("two Orchards facing each other reflect only the real land behind them") {
            val game = base()
                .withCardOnBattlefield(1, "Test Orchard")
                .withCardOnBattlefield(1, "Mountain")
                .withCardOnBattlefield(2, "Test Orchard")
                .build()
            val mine = game.findPermanents("Test Orchard").first {
                game.state.projectedState.getController(it) == game.player1Id
            }

            withClue("the opponent's Orchard reflects my Mountain") {
                game.colorsFor(mine, LandControllerScope.OPPONENTS) shouldBe setOf(Color.RED)
            }
        }

        test("a land filter narrows the lands read to those it matches") {
            val game = base()
                .withCardOnBattlefield(1, "Test Pool")
                .withCardOnBattlefield(1, "Test Gate")
                .withCardOnBattlefield(1, "Forest")
                .withCardOnBattlefield(2, "Test Gate")
                .withCardOnBattlefield(2, "Island")
                .build()
            val pool = game.findPermanent("Test Pool")!!

            withClue("only my Gate counts — not my Forest, nor the opponent's lands") {
                game.colorsFor(pool, LandControllerScope.YOU, gates) shouldBe setOf(Color.WHITE)
            }
            withClue("an opponent-scoped Gate filter reads only the opponent's Gate") {
                game.colorsFor(pool, LandControllerScope.OPPONENTS, gates) shouldBe setOf(Color.WHITE)
            }
            withClue("unfiltered, every land in scope counts") {
                game.colorsFor(pool, LandControllerScope.YOU) shouldBe setOf(Color.WHITE, Color.GREEN)
            }
        }

        test("a land filter matching nothing produces nothing") {
            val game = base()
                .withCardOnBattlefield(1, "Test Pool")
                .withCardOnBattlefield(1, "Forest")
                .build()

            game.colorsFor(game.findPermanent("Test Pool")!!, LandControllerScope.YOU, gates) shouldBe emptySet()
        }

        test("a land reflected through a filtered ability reports only the filtered lands' colors") {
            val game = base()
                .withCardOnBattlefield(1, "Test Plaza")
                .withCardOnBattlefield(1, "Test Gate")
                .withCardOnBattlefield(1, "Forest")
                .build()
            val plazaId = game.findPermanent("Test Plaza")!!

            withClue("what the Plaza could produce, as a Pool or Stone reflecting it would read it") {
                LandManaColorInspector.colorsLandCouldProduce(
                    game.state, game.state.projectedState, plazaId, cardRegistry, services.predicateEvaluator,
                ) shouldBe setOf(Color.WHITE)
            }
        }
    }
}
