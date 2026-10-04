package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * [com.wingedsheep.sdk.scripting.predicates.CardPredicate.CouldProduceColorlessMana] — "a land that
 * could produce {C}". It reads the land's mana abilities the way `ManaColorSet.LandsCouldProduce`
 * does: the effect counts, the cost and the tapped state don't (Reflecting Pool / Fellwar Stone
 * rulings), and a nonland never matches even if it taps for {C}.
 */
class CouldProduceColorlessManaPredicateTest : ScenarioTestBase() {

    private val colorlessLand = card("Test Colorless Land") {
        typeLine = "Land"
        activatedAbility {
            cost = Costs.Tap
            manaAbility = true
            effect = Effects.AddColorlessMana(1)
        }
    }

    // Eldrazi Temple shape: a restricted {C}{C} still produces the colorless type.
    private val restrictedColorlessLand = card("Test Temple") {
        typeLine = "Land"
        activatedAbility {
            cost = Costs.Tap
            manaAbility = true
            effect = Effects.AddColorlessMana(2, ManaRestriction.ColorlessSpellsOnly)
        }
    }

    private val colorlessRock = card("Test Colorless Rock") {
        manaCost = "{1}"
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Tap
            manaAbility = true
            effect = Effects.AddColorlessMana(2)
        }
    }

    private fun TestGame.couldProduceColorless(entityId: EntityId): Boolean =
        services.predicateEvaluator.matches(
            state,
            state.projectedState,
            entityId,
            GameObjectFilter.Any.couldProduceColorlessMana(),
            PredicateContext(controllerId = player1Id)
        )

    init {
        cardRegistry.register(colorlessLand)
        cardRegistry.register(restrictedColorlessLand)
        cardRegistry.register(colorlessRock)

        test("a land with a {C} ability matches — tapped or not, restricted or not") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Test Colorless Land", tapped = true)
                .withCardOnBattlefield(1, "Test Temple")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            withClue("tapped colorless land") {
                game.couldProduceColorless(game.findPermanent("Test Colorless Land")!!) shouldBe true
            }
            withClue("restricted {C}{C}") {
                game.couldProduceColorless(game.findPermanent("Test Temple")!!) shouldBe true
            }
        }

        test("a basic that taps for a color, and a nonland that taps for {C}, don't match") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Forest")
                .withCardOnBattlefield(1, "Test Colorless Rock")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            withClue("Forest only makes green") {
                game.couldProduceColorless(game.findPermanent("Forest")!!) shouldBe false
            }
            withClue("an artifact isn't a land") {
                game.couldProduceColorless(game.findPermanent("Test Colorless Rock")!!) shouldBe false
            }
        }
    }
}
