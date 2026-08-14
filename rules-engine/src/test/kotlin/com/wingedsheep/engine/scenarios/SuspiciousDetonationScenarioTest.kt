package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.mrd.cards.WeldingJar
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Suspicious Detonation (MKM) — {4}{R} sorcery, "costs {3} less to cast if you've sacrificed an
 * artifact this turn", can't be countered, 4 damage to target creature.
 *
 * Covers the new `TurnTracker.ARTIFACT_SACRIFICED` end to end. The tracker is only worth anything
 * if it survives the artifact leaving the battlefield *and* the round trip through the cost
 * reduction rail, so these tests pin the mana actually required rather than reading the component:
 * five lands cast it cold, two lands cast it after a sacrifice, and two lands alone can't.
 *
 * Welding Jar is the sacrifice outlet — a {0} artifact whose whole ability is
 * `Costs.SacrificeSelf`, so activating it routes through `ZoneTransitionService`'s central
 * sacrifice hook the same way cracking a Clue does. Two copies are on the battlefield so the
 * sacrificed one has another artifact to target.
 */
class SuspiciousDetonationScenarioTest : ScenarioTestBase() {

    private val weldingJarAbility = WeldingJar.activatedAbilities.first().id

    /** Sacrifice one Welding Jar, pointing its regenerate at the other so the target is real. */
    private fun sacrificeAWeldingJar(game: TestGame) {
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

    private fun toughnessDamageVictim(game: TestGame): EntityId =
        game.findPermanent("Grizzly Bears").shouldNotBeNull()

    /**
     * Shared board: the Detonation in hand, a 2/2 to point it at, and stocked libraries so the
     * turn-scoping test can round the table through two draw steps without decking anyone.
     */
    private fun board(mountains: Int, withJars: Boolean): ScenarioBuilder {
        var builder = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Suspicious Detonation")
            .withLandsOnBattlefield(1, "Mountain", mountains)
            .withCardOnBattlefield(2, "Grizzly Bears")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        if (withJars) {
            builder = builder
                .withCardOnBattlefield(1, "Welding Jar")
                .withCardOnBattlefield(1, "Welding Jar")
        }
        repeat(10) {
            builder = builder.withCardInLibrary(1, "Island").withCardInLibrary(2, "Island")
        }
        return builder
    }

    init {
        test("costs its full {4}{R} when no artifact was sacrificed") {
            val game = board(mountains = 2, withJars = false).build()

            withClue("{1}{R} can't pay {4}{R} without the reduction") {
                game.castSpell(1, "Suspicious Detonation", toughnessDamageVictim(game))
                    .error shouldNotBe null
            }
        }

        test("sacrificing an artifact reduces it to {1}{R} and it deals 4 damage") {
            val game = board(mountains = 2, withJars = true).build()

            sacrificeAWeldingJar(game)

            val victim = toughnessDamageVictim(game)
            withClue("the {3} reduction must bring {4}{R} within reach of two Mountains") {
                game.castSpell(1, "Suspicious Detonation", victim).error shouldBe null
            }
            game.resolveStack()

            withClue("4 damage kills a 2/2") {
                game.findPermanent("Grizzly Bears") shouldBe null
            }
        }

        test("the reduction is gone on a later turn — the tracker is turn-scoped") {
            val game = board(mountains = 2, withJars = true).build()

            sacrificeAWeldingJar(game)

            // Round the table back to player 1's next main phase. Each stop has to be a distinct
            // (phase, step) or passUntilPhase returns immediately without advancing, so walk
            // upkeep → main → upkeep → main across the turn boundary; cleanup clears the marker
            // on the way.
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP) // player 2's upkeep
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN) // player 2's main
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP) // player 1's next upkeep
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN) // player 1's next main
            withClue("we must actually be back on player 1's turn") {
                game.state.activePlayerId shouldBe game.player1Id
            }

            withClue("a sacrifice on a previous turn must not discount this spell") {
                game.castSpell(1, "Suspicious Detonation", toughnessDamageVictim(game))
                    .error shouldNotBe null
            }
        }

        test("it resolves through a counterspell") {
            val game = board(mountains = 5, withJars = false)
                .withCardInHand(2, "Cancel")
                .withLandsOnBattlefield(2, "Island", 3)
                .build()

            game.castSpell(1, "Suspicious Detonation", toughnessDamageVictim(game))
                .error shouldBe null
            game.passPriority()

            withClue("Cancel may still target it — it just won't counter it") {
                game.castSpellTargetingStackSpell(2, "Cancel", "Suspicious Detonation")
                    .error shouldBe null
            }
            game.resolveStack()

            withClue("can't be countered: the damage still happens") {
                game.findPermanent("Grizzly Bears") shouldBe null
            }
        }
    }
}
