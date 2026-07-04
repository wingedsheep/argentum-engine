package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Feature test for [com.wingedsheep.sdk.scripting.effects.AddCountersUpToEffect] — "Put up to N
 * counters on target", the additive/player-chosen mirror of `RemoveAnyNumberOfCounters`. Driven
 * through an inline instant that puts up to three +1/+1 counters on target creature.
 *
 * Proves: the single `ChooseNumberDecision` is capped at the effect's max; the chosen count is
 * placed; choosing 0 is a no-op; and the count clamps to the effect's ceiling regardless of board
 * state. (The Saga-lore composition — gate on `CollectionContainsMatch(SAGA)` then
 * `AddCountersUpTo(LORE, 3, …)` advancing a copied Saga's chapters — is proven end-to-end by
 * `TerraMagicalAdeptScenarioTest`.)
 */
class AddCountersUpToScenarioTest : ScenarioTestBase() {

    private val empowerment = card("Test Empowerment") {
        manaCost = "{1}{G}"
        colorIdentity = "G"
        typeLine = "Instant"
        oracleText = "Put up to three +1/+1 counters on target creature."
        spell {
            target = Targets.Creature
            effect = Effects.AddCountersUpTo(Counters.PLUS_ONE_PLUS_ONE, 3, EffectTarget.ContextTarget(0))
        }
    }

    private fun count(game: TestGame, id: EntityId, type: CounterType): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(type) ?: 0

    init {
        cardRegistry.register(empowerment)

        context("AddCountersUpTo — put up to N counters (player chooses)") {

            test("prompt is capped at the effect's max, and the chosen count is placed") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Test Empowerment")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Test Empowerment", targetId = bears).error shouldBe null
                game.resolveStack()

                val decision = game.getPendingDecision()
                withClue("A single ChooseNumber prompt, capped at the effect's max of 3") {
                    (decision is ChooseNumberDecision) shouldBe true
                    (decision as ChooseNumberDecision).minValue shouldBe 0
                    decision.maxValue shouldBe 3
                }
                game.chooseNumber(2).error shouldBe null

                withClue("Two +1/+1 counters were placed") {
                    count(game, bears, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
                }
            }

            test("choosing 0 places no counters") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Test Empowerment")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Test Empowerment", targetId = bears).error shouldBe null
                game.resolveStack()

                (game.getPendingDecision() as ChooseNumberDecision).maxValue shouldBe 3
                game.chooseNumber(0).error shouldBe null

                withClue("No counters placed when the controller chooses 0") {
                    count(game, bears, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 0
                }
            }

            test("the full max may be chosen") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Test Empowerment")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Test Empowerment", targetId = bears).error shouldBe null
                game.resolveStack()

                game.chooseNumber(3).error shouldBe null

                withClue("All three counters placed") {
                    count(game, bears, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 3
                }
            }
        }
    }
}
