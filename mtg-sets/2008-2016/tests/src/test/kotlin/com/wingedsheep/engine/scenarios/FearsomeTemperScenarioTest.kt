package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.bng.cards.FearsomeTemper
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Fearsome Temper (BNG #95) — Aura: enchanted creature gets +2/+2 and has
 * "{2}{R}: Target creature can't block this creature this turn."
 *
 * The load-bearing part is that `EffectTarget.Self` inside the *granted* ability resolves to the
 * enchanted creature (the ability's source), not the Aura: the named blocker is barred from blocking
 * the enchanted creature only, and may still block another attacker.
 */
class FearsomeTemperScenarioTest : ScenarioTestBase() {

    private val grantedAbilityId = FearsomeTemper.staticAbilities
        .filterIsInstance<GrantActivatedAbility>().single().ability.id

    private fun TestGame.drain() {
        var guard = 0
        while (guard++ < 15) {
            when (val decision = getPendingDecision()) {
                is SelectManaSourcesDecision -> submitManaSourcesAutoPay()
                null -> if (state.stack.isNotEmpty()) resolveStack() else break
                else -> error("unexpected decision $decision")
            }
        }
    }

    private fun board(): ScenarioBuilder = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
        .withCardAttachedTo(1, "Fearsome Temper", "Grizzly Bears")
        .withCardOnBattlefield(1, "Raging Goblin", summoningSickness = false)
        .withLandsOnBattlefield(1, "Mountain", 3)
        .withCardOnBattlefield(2, "Hill Giant")
        .withActivePlayer(1)
        .withPriorityPlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    init {
        test("enchanted creature gets +2/+2") {
            val game = board().build()
            val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()
            game.state.projectedState.getPower(bears) shouldBe 4
            game.state.projectedState.getToughness(bears) shouldBe 4
        }

        test("the named creature can't block the enchanted creature, but can block another attacker") {
            val game = board().build()
            val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()
            val giant = game.findPermanent("Hill Giant").shouldNotBeNull()

            val result = game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = bears,
                    abilityId = grantedAbilityId,
                    targets = listOf(ChosenTarget.Permanent(giant)),
                )
            )
            withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
            game.drain()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2, "Raging Goblin" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

            withClue("the Giant was named, so blocking the enchanted Bears must be rejected") {
                game.declareBlockers(mapOf("Hill Giant" to listOf("Grizzly Bears"))).error shouldNotBe null
            }
            withClue("the restriction is pairwise — the Goblin is still blockable") {
                game.declareBlockers(mapOf("Hill Giant" to listOf("Raging Goblin"))).error shouldBe null
            }
        }
    }
}
