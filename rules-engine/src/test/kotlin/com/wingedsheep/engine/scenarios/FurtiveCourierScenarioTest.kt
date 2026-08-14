package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.combat.BlockedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.mrd.cards.WeldingJar
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Furtive Courier (MKM) — {2}{U} 3/2, "can't be blocked as long as you've sacrificed an artifact
 * this turn", plus an attack-trigger loot.
 *
 * The evasion half is the `TurnTracker.ARTIFACT_SACRIFICED` consumer that reads through the
 * *projection* rather than the cost rail, so it's the other side of the tracker from
 * [SuspiciousDetonationScenarioTest]: here the condition has to reach `ConditionalStaticAbility`
 * and land in `ProjectedState` before `BlockPhaseManager` validates a declaration.
 *
 * "As long as" is continuous, which is why the third test matters — sacrificing the artifact
 * *after* blockers are declared changes nothing, because CR 509 has already locked the block in.
 * That's the card's printed ruling, and it falls out of modelling this as a static rather than an
 * attack trigger.
 */
class FurtiveCourierScenarioTest : ScenarioTestBase() {

    private val weldingJarAbility = WeldingJar.activatedAbilities.first().id

    /**
     * Sacrifice one Welding Jar, pointing its regenerate at the other so the target is real.
     *
     * Declaring blockers hands priority to the defending player, so take it back before activating
     * — the third test needs to sacrifice inside the declare-blockers step.
     */
    private fun sacrificeAWeldingJar(game: TestGame) {
        if (game.state.priorityPlayerId != game.player1Id) game.passPriority()
        val jars = game.findPermanents("Welding Jar")
        withClue("the scenario must set up two Welding Jars") { jars.size shouldBe 2 }
        game.execute(
            ActivateAbility(
                playerId = game.player1Id,
                sourceId = jars[0],
                abilityId = weldingJarAbility,
                targets = listOf(ChosenTarget.Permanent(jars[1])),
            )
        ).error shouldBe null
        game.resolveStack()
        withClue("the sacrifice must actually have happened") {
            game.findPermanents("Welding Jar").size shouldBe 1
        }
    }

    /**
     * A scenario starts with empty libraries, and the Courier's attack trigger draws — seed both
     * players so nobody decks out mid-test.
     */
    private fun board(): ScenarioBuilder {
        var builder = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Furtive Courier")
            .withCardOnBattlefield(1, "Welding Jar")
            .withCardOnBattlefield(1, "Welding Jar")
            .withCardOnBattlefield(2, "Grizzly Bears")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        repeat(10) {
            builder = builder.withCardInLibrary(1, "Island").withCardInLibrary(2, "Island")
        }
        return builder
    }

    init {
        test("blockable when no artifact has been sacrificed") {
            val game = board().build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Furtive Courier" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

            withClue("without a sacrifice this turn the Courier has no evasion") {
                game.declareBlockers(mapOf("Grizzly Bears" to listOf("Furtive Courier")))
                    .error shouldBe null
            }
        }

        test("sacrificing an artifact before blockers makes it unblockable") {
            val game = board().build()

            sacrificeAWeldingJar(game)

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Furtive Courier" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

            withClue("the block must be rejected — the Courier can't be blocked") {
                game.declareBlockers(mapOf("Grizzly Bears" to listOf("Furtive Courier")))
                    .error shouldNotBe null
            }
            withClue("declaring no blockers is the only legal declaration") {
                game.declareNoBlockers().error shouldBe null
            }
        }

        test("sacrificing after blockers are declared doesn't unblock it") {
            val game = board().build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Furtive Courier" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Grizzly Bears" to listOf("Furtive Courier")))
                .error shouldBe null

            sacrificeAWeldingJar(game)

            withClue("CR 509 already locked the block in — the Bears is still blocking") {
                val courier = game.findPermanent("Furtive Courier").shouldNotBeNull()
                val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()
                game.state.getEntity(courier)?.get<BlockedComponent>()?.blockerIds shouldBe listOf(bears)
            }
        }
    }
}
