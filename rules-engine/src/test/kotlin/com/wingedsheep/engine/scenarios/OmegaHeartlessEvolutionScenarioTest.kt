package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.fin.cards.OmegaHeartlessEvolution
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Omega, Heartless Evolution (FIN) — {5}{G}{U} Legendary Artifact Creature — Robot 8/8.
 *
 * "Wave Cannon — When Omega enters, for each opponent, tap up to one target nonland permanent that
 * opponent controls. Put X stun counters on each of those permanents and you gain X life, where X
 * is the number of nonbasic lands you control."
 *
 * Verifies the enter trigger: the chosen opponent permanent is tapped and loaded with X stun
 * counters, the controller gains X life, and X counts only *nonbasic* lands (basics don't count).
 */
class OmegaHeartlessEvolutionScenarioTest : ScenarioTestBase() {

    // A nonbasic land that taps for any color — lets one setup both pay Omega's {5}{G}{U} and hold
    // a deterministic number of *nonbasic* lands for X.
    private val prismaticNexus = card("Prismatic Nexus") {
        typeLine = "Land"
        activatedAbility {
            cost = Costs.Tap
            effect = Effects.AddAnyColorMana(1)
            manaAbility = true
        }
    }

    private fun TestGame.stunCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.STUN) ?: 0

    private fun TestGame.isTapped(id: EntityId): Boolean =
        state.getEntity(id)?.has<TappedComponent>() == true

    init {
        cardRegistry.register(OmegaHeartlessEvolution)
        cardRegistry.register(prismaticNexus)

        context("Omega — Wave Cannon enter trigger") {

            test("taps the opponent's permanent, loads X stun counters, and you gain X life (X = your nonbasic lands)") {
                var builder = scenario()
                    .withPlayers("You", "Opponent")
                    .withCardInHand(1, "Omega, Heartless Evolution")
                    .withLifeTotal(1, 20)
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                // 7 nonbasic lands pay {5}{G}{U} and set X = 7 ...
                repeat(7) { builder = builder.withCardOnBattlefield(1, "Prismatic Nexus", summoningSickness = false) }
                // ... plus 2 basic Forests that must NOT count toward X (proving the nonbasic filter).
                builder = builder.withLandsOnBattlefield(1, "Forest", 2)
                val game = builder.build()

                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Omega, Heartless Evolution")
                game.resolveStack()
                // Wave Cannon's optional target: choose the opponent's Grizzly Bears.
                game.selectTargets(listOf(bears))
                game.resolveStack()

                withClue("the chosen permanent is tapped") {
                    game.isTapped(bears) shouldBe true
                }
                withClue("X = 7 nonbasic lands (the 2 Forests don't count) → 7 stun counters") {
                    game.stunCounters(bears) shouldBe 7
                }
                withClue("you gain X = 7 life (20 → 27)") {
                    game.getLifeTotal(1) shouldBe 27
                }
            }
        }
    }
}
