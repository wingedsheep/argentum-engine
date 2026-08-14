package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/** Scenario tests for Wildwood Mentor. */
class WildwoodMentorScenarioTest : ScenarioTestBase() {

    private fun power(game: TestGame, id: EntityId): Int? = game.state.projectedState.getPower(id)
    private fun toughness(game: TestGame, id: EntityId): Int? = game.state.projectedState.getToughness(id)

    private fun plusOneCounters(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun auraOn(game: TestGame, auraName: String, host: EntityId): EntityId? =
        game.findPermanents(auraName).firstOrNull { aura ->
            game.state.getEntity(aura)?.get<AttachedToComponent>()?.targetId == host
        }

    /**
     * Twisted Fealty has two independent targets, which the base fixture's single-target
     * [ScenarioTestBase.TestGame.castSpell] can't express — cast it directly so the "up to one"
     * second target can be present or absent.
     */
    private fun TestGame.castTwistedFealty(targets: List<EntityId>) = execute(
        CastSpell(
            player1Id,
            findCardsInHand(1, "Twisted Fealty").first(),
            targets.map { ChosenTarget.Permanent(it) }
        )
    )

    init {
        context("Wildwood Mentor — counters from tokens, +X/+X on attack") {
            test("a token entering puts a +1/+1 counter on the Mentor") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Wildwood Mentor", summoningSickness = false)
                    // Voracious Vermin's enters trigger creates one Rat *token*.
                    .withCardInHand(1, "Voracious Vermin")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val mentor = game.findPermanent("Wildwood Mentor").shouldNotBeNull()
                plusOneCounters(game, mentor) shouldBe 0

                game.castSpell(1, "Voracious Vermin").error shouldBe null
                game.resolveStack()

                withClue("the Rat token entering is one trigger; the nontoken Vermin itself is not") {
                    plusOneCounters(game, mentor) shouldBe 1
                }
                withClue("the 1/1 Mentor is now 2/2") {
                    power(game, mentor) shouldBe 2
                    toughness(game, mentor) shouldBe 2
                }
            }

            test("a nontoken creature entering does nothing") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Wildwood Mentor", summoningSickness = false)
                    .withCardInHand(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val mentor = game.findPermanent("Wildwood Mentor").shouldNotBeNull()

                game.castSpell(1, "Grizzly Bears").error shouldBe null
                game.resolveStack()

                withClue("Grizzly Bears is a real card, not a token") {
                    plusOneCounters(game, mentor) shouldBe 0
                }
            }

            test("the attack trigger pumps another attacker by the Mentor's power") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Wildwood Mentor", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(
                    mapOf("Wildwood Mentor" to 2, "Grizzly Bears" to 2)
                ).error shouldBe null

                // Only the Bears is a legal target — "another" excludes the Mentor.
                game.selectTargets(listOf(bears)).error shouldBe null
                game.resolveStack()

                withClue("X = the Mentor's power (1), so the 2/2 Bears becomes 3/3") {
                    power(game, bears) shouldBe 3
                    toughness(game, bears) shouldBe 3
                }
            }
        }
    }
}
