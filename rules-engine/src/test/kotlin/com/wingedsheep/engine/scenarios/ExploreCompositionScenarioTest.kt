package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsRevealedEvent
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for the Explore keyword action (CR 701.44) as a pure pipeline
 * composition — `Patterns.Mechanic.explore()`.
 *
 * CR 701.44a: "...that permanent's controller reveals the top card of their library.
 * If a land card is revealed this way, that player puts that card into their hand.
 * Otherwise, that player puts a +1/+1 counter on the exploring permanent and may put
 * the revealed card into their graveyard."
 *
 * The composition is the worked example for "branch on gathered properties"
 * (backlog §2.1): Gather (revealed) → FilterCollection partition by Land →
 * MoveCollection(land → hand) → GatedEffect(WhenCondition(no land revealed)) over
 * counter + optional graveyard. No bespoke Explore effect type exists.
 */
class ExploreCompositionScenarioTest : ScenarioTestBase() {

    private val explorer = card("Test Explorer") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Merfolk Scout"
        power = 1
        toughness = 1
        oracleText = "When this creature enters, it explores."

        triggeredAbility {
            trigger = Triggers.EntersBattlefield
            effect = Patterns.Mechanic.explore()
        }
    }

    init {
        cardRegistry.register(explorer)

        fun plusOneCounters(game: TestGame, name: String): Int {
            val id = game.findPermanent(name)!!
            return game.state.getEntity(id)?.get<CountersComponent>()
                ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
        }

        context("Explore as a pipeline composition (CR 701.44)") {

            test("revealed land goes to hand; no +1/+1 counter") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Test Explorer")
                    .withCardInLibrary(1, "Forest")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Test Explorer").error shouldBe null
                val results = game.resolveStack()

                withClue("revealed land should be put into hand") {
                    game.isInHand(1, "Forest") shouldBe true
                }
                withClue("library should now be empty (top card moved to hand)") {
                    game.state.getZone(ZoneKey(game.player1Id, Zone.LIBRARY)).size shouldBe 0
                }
                withClue("a land reveal must not put a +1/+1 counter on the explorer (CR 701.44a)") {
                    plusOneCounters(game, "Test Explorer") shouldBe 0
                }
                withClue("the top card must be revealed to all players") {
                    results.flatMap { it.events }.filterIsInstance<CardsRevealedEvent>()
                        .any { it.cardNames.contains("Forest") } shouldBe true
                }
            }

            test("revealed nonland: counter on explorer, card may go to graveyard") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Test Explorer")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Test Explorer").error shouldBe null
                game.resolveStack()

                withClue("nonland reveal should pause for the may-put-in-graveyard choice") {
                    (game.state.pendingDecision != null) shouldBe true
                }
                val bearsId = game.state.getZone(ZoneKey(game.player1Id, Zone.LIBRARY)).first()
                game.selectCards(listOf(bearsId))
                game.resolveStack()

                withClue("explorer should have the +1/+1 counter (nonland branch)") {
                    plusOneCounters(game, "Test Explorer") shouldBe 1
                }
                withClue("the revealed nonland should be in the graveyard") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                }
            }

            test("revealed nonland left on top stays on top of the library") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Test Explorer")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Forest")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val topBefore = game.state.getZone(ZoneKey(game.player1Id, Zone.LIBRARY)).first()

                game.castSpell(1, "Test Explorer").error shouldBe null
                game.resolveStack()

                withClue("nonland reveal should pause for the may-put-in-graveyard choice") {
                    (game.state.pendingDecision != null) shouldBe true
                }
                game.skipSelection()
                game.resolveStack()

                withClue("explorer still gets its +1/+1 counter when the card is kept on top") {
                    plusOneCounters(game, "Test Explorer") shouldBe 1
                }
                withClue("declined card must remain on top of the library") {
                    game.state.getZone(ZoneKey(game.player1Id, Zone.LIBRARY)).first() shouldBe topBefore
                }
                withClue("nothing went to the graveyard") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe false
                }
            }

            test("empty library: explorer still gets the +1/+1 counter (CR 701.44a/b)") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Test Explorer")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Test Explorer").error shouldBe null
                game.resolveStack()

                withClue("no card revealed ⇒ 'if a land card is revealed' is false ⇒ otherwise-branch still runs") {
                    plusOneCounters(game, "Test Explorer") shouldBe 1
                }
                withClue("no decision should be pending (nothing to put in the graveyard)") {
                    game.state.pendingDecision shouldBe null
                }
            }
        }
    }
}
