package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.chk.cards.SenseiGoldenTail
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Sensei Golden-Tail (CHK #44) — "Bushido 1. {1}{W}, {T}: Put a training counter on target
 * creature. That creature gains bushido 1 and becomes a Samurai in addition to its other creature
 * types. Activate only as a sorcery."
 */
class SenseiGoldenTailScenarioTest : ScenarioTestBase() {

    private val train = SenseiGoldenTail.activatedAbilities.single()

    private fun TestGame.activateTrain(target: EntityId) = execute(
        ActivateAbility(
            playerId = player1Id,
            sourceId = findPermanent("Sensei Golden-Tail")!!,
            abilityId = train.id,
            targets = listOf(ChosenTarget.Permanent(target))
        )
    )

    init {
        context("Sensei Golden-Tail") {

            test("the trained creature gets a training counter, bushido 1, and becomes a Samurai") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Sensei Golden-Tail")
                    .withCardOnBattlefield(1, "Takeno, Samurai General")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanents("Grizzly Bears")
                    .single { game.state.projectedState.getController(it) == game.player1Id }
                game.state.projectedState.getPower(bears) shouldBe 2

                val result = game.activateTrain(bears)
                withClue("activation: ${result.error}") { result.error shouldBe null }
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                val projected = game.state.projectedState
                game.state.getEntity(bears)!!.get<CountersComponent>()!!
                    .getCount(CounterType.TRAINING) shouldBe 1
                projected.getSubtypes(bears) shouldContain Subtype.SAMURAI.value
                withClue("Takeno now sees a Samurai with one point of bushido → +1/+1") {
                    projected.getPower(bears) shouldBe 3
                    projected.getToughness(bears) shouldBe 3
                }
            }

            test("the granted bushido triggers when the trained creature becomes blocked") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Sensei Golden-Tail")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.activateTrain(bears).error shouldBe null
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Hill Giant" to listOf("Grizzly Bears"))).error shouldBe null
                game.resolveStack()

                game.state.projectedState.getPower(bears) shouldBe 3
                game.state.projectedState.getToughness(bears) shouldBe 3
            }

            test("activate only as a sorcery") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Sensei Golden-Tail")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UPKEEP)
                    .build()

                game.activateTrain(game.findPermanent("Grizzly Bears")!!).error shouldNotBe null
            }
        }
    }
}
