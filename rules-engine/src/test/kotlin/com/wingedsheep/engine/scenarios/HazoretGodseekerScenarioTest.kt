package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.speed.SpeedService
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Speed
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Hazoret, Godseeker.
 *
 * Oracle:
 * - Indestructible, haste
 * - Start your engines!
 * - "{1}, {T}: Target creature with power 2 or less can't be blocked this turn."
 * - "Hazoret can't attack or block unless you have max speed."
 *
 * The restriction is the card: a 5/3 haste indestructible body for {1}{R} that cannot attack until
 * speed 4. It is *not* a "Max speed — [ability]" line, so it must exist at every speed with only its
 * condition reading speed — and it must be checked at declare-*blockers* as well as
 * declare-attackers, which a single `CantAttackUnless` would silently half-implement.
 */
class HazoretGodseekerScenarioTest : ScenarioTestBase() {

    private val unblockableAbilityId by lazy {
        cardRegistry.requireCard("Hazoret, Godseeker").activatedAbilities[0].id
    }

    init {
        context("can't attack or block unless you have max speed") {

            test("cannot attack at the speed 1 that its own keyword grants") {
                val game = hazoretGame()

                withClue("Start your engines! leaves you at 1, well short of max") {
                    game.state.speed(game.player1Id) shouldBe Speed.STARTING
                }

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                withClue("the restriction blocks the attack below max speed") {
                    game.declareAttackers(mapOf("Hazoret, Godseeker" to 2)).error shouldNotBe null
                }
            }

            test("can attack at max speed") {
                val game = hazoretGame()
                game.state = SpeedService.set(game.state, game.player1Id, Speed.MAX, "test").first

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                withClue("speed 4 lifts the restriction; haste covers the summoning-sickness side") {
                    game.declareAttackers(mapOf("Hazoret, Godseeker" to 2)).error shouldBe null
                }
            }

            test("speed 3 is not enough — max speed is exactly 4") {
                val game = hazoretGame()
                game.state = SpeedService.set(game.state, game.player1Id, Speed.MAX - 1, "test").first

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                withClue("3 is not max speed") {
                    game.declareAttackers(mapOf("Hazoret, Godseeker" to 2)).error shouldNotBe null
                }
            }

            test("cannot block below max speed either") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .also { b -> repeat(12) { b.withCardInLibrary(1, "Mountain"); b.withCardInLibrary(2, "Mountain") } }
                    .withCardOnBattlefield(1, "Hazoret, Godseeker", summoningSickness = false)
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                    .withActivePlayer(2)
                    .inPhase(Phase.BEGINNING, Step.UPKEEP)
                    .build()
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 1)).error shouldBe null
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                withClue("player 1 is at speed 1, so Hazoret cannot block") {
                    game.declareBlockers(
                        mapOf("Hazoret, Godseeker" to listOf("Grizzly Bears"))
                    ).error shouldNotBe null
                }
            }
        }

        context("{1}, {T}: target creature with power 2 or less can't be blocked") {

            test("grants the flag to a 2/2 and works below max speed") {
                // The activated ability carries no speed gate at all — only the combat restriction
                // does — so it must work at speed 1.
                val game = hazoretGame(extraLands = 1) {
                    withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                }
                val hazoret = game.findPermanent("Hazoret, Godseeker")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = hazoret,
                        abilityId = unblockableAbilityId,
                        targets = listOf(ChosenTarget.Permanent(bears)),
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("the 2/2 Bears now can't be blocked") {
                    game.state.projectedState.hasKeyword(bears, AbilityFlag.CANT_BE_BLOCKED) shouldBe true
                }
            }

            test("cannot target a creature with power 3 or greater") {
                val game = hazoretGame(extraLands = 1) {
                    withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
                }
                val hazoret = game.findPermanent("Hazoret, Godseeker")!!
                val giant = game.findPermanent("Hill Giant")!!

                withClue("Hill Giant is a 3/3, outside \"power 2 or less\"") {
                    game.execute(
                        ActivateAbility(
                            playerId = game.player1Id,
                            sourceId = hazoret,
                            abilityId = unblockableAbilityId,
                            targets = listOf(ChosenTarget.Permanent(giant)),
                        )
                    ).error shouldNotBe null
                }
            }
        }
    }

    /**
     * Hazoret on player 1's battlefield, reaching the main phase through a real step sequence so the
     * Start your engines! state-based action has been polled. [extraLands] stocks Mountains for the
     * `{1}` in the activated ability.
     */
    private fun hazoretGame(extraLands: Int = 0, extra: ScenarioBuilder.() -> Unit = {}): TestGame {
        val builder = scenario().withPlayers("Player1", "Player2")
        repeat(12) {
            builder.withCardInLibrary(1, "Mountain")
            builder.withCardInLibrary(2, "Mountain")
        }
        builder.withCardOnBattlefield(1, "Hazoret, Godseeker", summoningSickness = false)
        if (extraLands > 0) builder.withLandsOnBattlefield(1, "Mountain", extraLands)
        builder.extra()
        val game = builder
            .withActivePlayer(1)
            .inPhase(Phase.BEGINNING, Step.UPKEEP)
            .build()
        game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        return game
    }
}
