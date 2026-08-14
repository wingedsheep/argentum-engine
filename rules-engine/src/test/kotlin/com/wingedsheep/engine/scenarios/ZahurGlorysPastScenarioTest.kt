package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.mechanics.speed.SpeedService
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Speed
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Zahur, Glory's Past.
 *
 * Oracle:
 * - Start your engines!
 * - "Sacrifice another creature: Surveil 1. Activate only once each turn."
 * - "Max speed — Whenever a nontoken creature you control dies, create a tapped 2/2 black Zombie
 *   creature token."
 *
 * The card is an engine only if its two halves feed each other, and both of the ways to get that
 * wrong are silent:
 *
 * - modelling the trigger with `excludeSacrifice` (or with `TriggerBinding.OTHER` and a sacrifice
 *   check) would leave "sacrifice a creature: surveil" producing no Zombie at max speed;
 * - modelling it as "another nontoken creature" would drop Zahur's own death, which the card does
 *   not say.
 *
 * Both get an explicit test, plus the nontoken clause (a Zombie the ability itself made must not
 * loop) and the max-speed gate (below speed 4 the trigger does not exist at all).
 */
class ZahurGlorysPastScenarioTest : ScenarioTestBase() {

    private val surveilAbilityId by lazy {
        cardRegistry.requireCard("Zahur, Glory's Past").activatedAbilities[0].id
    }

    init {
        context("Sacrifice another creature: Surveil 1") {

            test("sacrifices the chosen creature and surveils") {
                val game = zahurGame {
                    withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                }
                val zahur = game.findPermanent("Zahur, Glory's Past")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val libraryBefore = game.librarySize(1)

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = zahur,
                        abilityId = surveilAbilityId,
                        costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(bears)),
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("the Bears paid the cost") {
                    game.findPermanent("Grizzly Bears") shouldBe null
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                }

                // Surveil 1 pauses for the keep-or-bin choice over the top card.
                val surveil = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                surveil.options.size shouldBe 1
                game.selectCards(surveil.options).error shouldBe null

                withClue("binning the surveiled card moves it out of the library") {
                    game.librarySize(1) shouldBe libraryBefore - 1
                }
            }

            test("cannot be activated twice in a turn") {
                val game = zahurGame {
                    withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
                }
                val zahur = game.findPermanent("Zahur, Glory's Past")!!

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = zahur,
                        abilityId = surveilAbilityId,
                        costPayment = AdditionalCostPayment(
                            sacrificedPermanents = listOf(game.findPermanent("Grizzly Bears")!!)
                        ),
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("\"Activate only once each turn\" rejects the second activation") {
                    game.execute(
                        ActivateAbility(
                            playerId = game.player1Id,
                            sourceId = zahur,
                            abilityId = surveilAbilityId,
                            costPayment = AdditionalCostPayment(
                                sacrificedPermanents = listOf(game.findPermanent("Hill Giant")!!)
                            ),
                        )
                    ).error shouldNotBe null
                }
                withClue("the Hill Giant survived the rejected activation") {
                    game.findPermanent("Hill Giant") shouldNotBe null
                }
            }
        }

        context("Max speed — whenever a nontoken creature you control dies") {

            test("the ability's own sacrifice makes a Zombie at max speed") {
                // The whole point of the card: a sacrifice is a battlefield → graveyard move, so it
                // matches the death trigger. An `excludeSacrifice` trigger would produce nothing.
                val game = zahurGame {
                    withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                }
                game.state = SpeedService.set(game.state, game.player1Id, Speed.MAX, "test").first

                val zahur = game.findPermanent("Zahur, Glory's Past")!!
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = zahur,
                        abilityId = surveilAbilityId,
                        costPayment = AdditionalCostPayment(
                            sacrificedPermanents = listOf(game.findPermanent("Grizzly Bears")!!)
                        ),
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("the sacrificed Bears triggered the max-speed ability") {
                    game.findAllPermanents("Zombie Token").size shouldBe 1
                }
            }

            test("Zahur's own death triggers it — the clause is not \"another\"") {
                val game = zahurGame {
                    withCardInHand(1, "Murder")
                    withLandsOnBattlefield(1, "Swamp", 3)
                }
                game.state = SpeedService.set(game.state, game.player1Id, Speed.MAX, "test").first

                val zahur = game.findPermanent("Zahur, Glory's Past")!!
                game.castSpell(1, "Murder", targetId = zahur).error shouldBe null
                game.resolveStack()

                withClue("Zahur is dead") {
                    game.findPermanent("Zahur, Glory's Past") shouldBe null
                }
                withClue("it still saw its own death and left a Zombie behind") {
                    game.findAllPermanents("Zombie Token").size shouldBe 1
                }
            }

            test("a token creature dying does not trigger it") {
                // Guards against a runaway loop: the Zombies the ability makes are tokens.
                val game = zahurGame {
                    withCardOnBattlefield(1, "Grizzly Bears", isToken = true, summoningSickness = false)
                    withCardInHand(1, "Murder")
                    withLandsOnBattlefield(1, "Swamp", 3)
                }
                game.state = SpeedService.set(game.state, game.player1Id, Speed.MAX, "test").first

                game.castSpell(
                    1, "Murder", targetId = game.findPermanent("Grizzly Bears")!!
                ).error shouldBe null
                game.resolveStack()

                withClue("the token died but it is not a nontoken creature") {
                    game.findAllPermanents("Zombie Token").size shouldBe 0
                }
            }

            test("below max speed the trigger does not fire") {
                val game = zahurGame {
                    withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                }

                withClue("Start your engines! leaves you at speed 1, not 4") {
                    game.state.speed(game.player1Id) shouldBe Speed.STARTING
                }

                val zahur = game.findPermanent("Zahur, Glory's Past")!!
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = zahur,
                        abilityId = surveilAbilityId,
                        costPayment = AdditionalCostPayment(
                            sacrificedPermanents = listOf(game.findPermanent("Grizzly Bears")!!)
                        ),
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("the max-speed ability doesn't exist at speed 1") {
                    game.findAllPermanents("Zombie Token").size shouldBe 0
                }
            }
        }
    }

    /**
     * Zahur on player 1's battlefield, arriving at the main phase through a real step sequence so
     * the Start your engines! state-based action has actually been polled. Libraries are stocked so
     * surveil and the draw step can't deck anyone.
     */
    private fun zahurGame(extra: ScenarioBuilder.() -> Unit): TestGame {
        val builder = scenario().withPlayers("Player1", "Player2")
        repeat(12) {
            builder.withCardInLibrary(1, "Grizzly Bears")
            builder.withCardInLibrary(2, "Grizzly Bears")
        }
        builder.withCardOnBattlefield(1, "Zahur, Glory's Past", summoningSickness = false)
        builder.extra()
        val game = builder
            .withActivePlayer(1)
            .inPhase(Phase.BEGINNING, Step.UPKEEP)
            .build()
        game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        return game
    }
}
