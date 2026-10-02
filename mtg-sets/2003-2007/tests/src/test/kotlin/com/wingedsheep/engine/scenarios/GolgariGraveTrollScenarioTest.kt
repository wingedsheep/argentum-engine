package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.rav.cards.GolgariGraveTroll
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Golgari Grave-Troll (RAV #167) — a 0/0 that is only as big as the graveyard it came from, buys
 * regeneration with its own counters, and dredges itself back.
 *
 * The three things worth proving are the three that could each silently no-op: the as-enters
 * count reads *creature* cards only, the regeneration shield actually replaces a destruction (and
 * costs a counter to raise), and dredge 6 mills its printed number rather than the default one.
 */
class GolgariGraveTrollScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()
    private val regenerateAbility = GolgariGraveTroll.activatedAbilities.single().id

    private fun plusCounters(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        context("Golgari Grave-Troll") {

            test("enters with one +1/+1 counter per creature card in your graveyard, ignoring the rest") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Golgari Grave-Troll")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Centaur Courser")
                    .withCardInGraveyard(1, "Votary of the Conclave")
                    .withCardInGraveyard(1, "Murder") // an instant — must not be counted
                    .withLandsOnBattlefield(1, "Forest", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Golgari Grave-Troll").error shouldBe null
                game.resolveStack()

                val troll = game.findPermanent("Golgari Grave-Troll")
                troll shouldNotBe null
                withClue("three creature cards in the graveyard, the instant doesn't count") {
                    plusCounters(game, troll!!) shouldBe 3
                }
                withClue("a 0/0 body plus three counters is a 3/3") {
                    projector.project(game.state).getPower(troll!!) shouldBe 3
                    projector.project(game.state).getToughness(troll) shouldBe 3
                }
            }

            test("returned from the graveyard, it counts itself among the creature cards there") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInGraveyard(1, "Golgari Grave-Troll")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInHand(1, "Zombify")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val troll = game.findCardsInGraveyard(1, "Golgari Grave-Troll").single()
                game.castSpellTargetingGraveyardCard(1, "Zombify", listOf(troll)).error shouldBe null
                game.resolveStack()

                withClue("the Bears and the Troll itself were in the graveyard as it entered") {
                    plusCounters(game, game.findPermanent("Golgari Grave-Troll")!!) shouldBe 2
                }
            }

            test("an empty graveyard leaves a 0/0 that dies to state-based actions") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Golgari Grave-Troll")
                    .withLandsOnBattlefield(1, "Forest", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Golgari Grave-Troll").error shouldBe null
                game.resolveStack()
                game.checkStateBasedActions()

                withClue("no creature cards to count means no counters, and a 0/0 can't stay") {
                    game.findPermanent("Golgari Grave-Troll") shouldBe null
                    game.isInGraveyard(1, "Golgari Grave-Troll") shouldBe true
                }
            }

            test("regeneration costs a +1/+1 counter and replaces a destroy, leaving it tapped") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Golgari Grave-Troll")
                    .withCardInHand(1, "Murder")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Centaur Courser")
                    .withLandsOnBattlefield(1, "Forest", 6)
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Golgari Grave-Troll").error shouldBe null
                game.resolveStack()
                val troll = game.findPermanent("Golgari Grave-Troll")!!
                plusCounters(game, troll) shouldBe 2

                val activation = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = troll,
                        abilityId = regenerateAbility
                    )
                )
                withClue("activation should succeed: ${activation.error}") { activation.error shouldBe null }
                game.resolveStack()

                withClue("the counter is spent paying for the shield") {
                    plusCounters(game, troll) shouldBe 1
                }

                game.castSpell(1, "Murder", troll).error shouldBe null
                game.resolveStack()

                withClue("the shield replaces the destruction — the Troll survives, tapped") {
                    game.findPermanent("Golgari Grave-Troll") shouldNotBe null
                    game.state.getEntity(troll)?.has<TappedComponent>() shouldBe true
                    plusCounters(game, troll) shouldBe 1
                }
            }

            test("without a shield the same destroy effect kills it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Golgari Grave-Troll")
                    .withCardInHand(1, "Murder")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Centaur Courser")
                    .withLandsOnBattlefield(1, "Forest", 6)
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Golgari Grave-Troll").error shouldBe null
                game.resolveStack()
                val troll = game.findPermanent("Golgari Grave-Troll")!!

                game.castSpell(1, "Murder", troll).error shouldBe null
                game.resolveStack()

                game.findPermanent("Golgari Grave-Troll") shouldBe null
                game.isInGraveyard(1, "Golgari Grave-Troll") shouldBe true
            }

            test("dredge 6 replaces a draw by milling six and returning the Troll") {
                var builder = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInGraveyard(1, "Golgari Grave-Troll")
                    .withCardInHand(1, "Inspiration")
                    .withLandsOnBattlefield(1, "Island", 4)
                repeat(8) { builder = builder.withCardInLibrary(1, "Forest") }
                val game = builder
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(1, "Inspiration", 1).error shouldBe null
                game.resolveStack()

                val offer = game.state.pendingDecision
                (offer as YesNoDecision).context.sourceName shouldBe "Golgari Grave-Troll"
                game.answerYesNo(true).error shouldBe null
                game.resolveStack()

                withClue("dredge 6 mills six and returns the Troll; the second draw still happens") {
                    game.isInHand(1, "Golgari Grave-Troll") shouldBe true
                    game.findCardsInGraveyard(1, "Forest").size shouldBe 6
                    game.librarySize(1) shouldBe 1
                }
            }

            test("dredge is not offered when the library is too small to mill six") {
                var builder = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInGraveyard(1, "Golgari Grave-Troll")
                    .withCardInHand(1, "Inspiration")
                    .withLandsOnBattlefield(1, "Island", 4)
                repeat(5) { builder = builder.withCardInLibrary(1, "Forest") }
                val game = builder
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(1, "Inspiration", 1).error shouldBe null
                game.resolveStack()

                withClue("five cards can't pay a six-card mill, so the draw is never replaced") {
                    game.isInHand(1, "Golgari Grave-Troll") shouldBe false
                    game.isInGraveyard(1, "Golgari Grave-Troll") shouldBe true
                    game.librarySize(1) shouldBe 3
                }
            }
        }
    }
}
