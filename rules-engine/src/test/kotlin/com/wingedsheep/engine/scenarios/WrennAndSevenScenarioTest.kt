package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.mid.cards.WrennAndSeven
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Wrenn and Seven (MID #208, {3}{G}{G}, Loyalty 5).
 *
 *   +1: Reveal the top four cards of your library. Put all land cards revealed this way into your
 *       hand and the rest into your graveyard.
 *   0: Put any number of land cards from your hand onto the battlefield tapped.
 *   −3: Create a green Treefolk creature token with reach and "This token's power and toughness are
 *       each equal to the number of lands you control."
 *   −8: Return all permanent cards from your graveyard to your hand. You get an emblem with "You
 *       have no maximum hand size."
 *
 * The load-bearing test here is the −3. `Effects.CreateDynamicToken` evaluates its amount once, in
 * `CreateTokenExecutor`, which would freeze the Treefolk at whatever the land count was on the turn
 * it was made. The printed ability is characteristic-defining and recalculates continuously, so the
 * token instead carries a `SetBasePowerToughnessDynamicStatic`. "Grows when you play a land" is
 * exactly the assertion that tells those two implementations apart.
 */
class WrennAndSevenScenarioTest : ScenarioTestBase() {

    init {
        cardRegistry.register(listOf(WrennAndSeven))

        context("the −3 Treefolk token") {

            test("enters with power and toughness equal to your land count") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Wrenn and Seven")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wrenn = game.findPermanent("Wrenn and Seven")!!
                setLoyalty(game, wrenn, 5)

                activate(game, wrenn, index = 2)
                game.resolveStack()

                val maybeToken = game.findPermanent("Treefolk Token")
                withClue("the token was created") { maybeToken shouldNotBe null }
                val token = maybeToken!!

                val projected = game.state.projectedState
                withClue("3 lands -> 3/3") {
                    projected.getPower(token) shouldBe 3
                    projected.getToughness(token) shouldBe 3
                }
                withClue("and it has reach") {
                    projected.hasKeyword(token, Keyword.REACH) shouldBe true
                }
                withClue("−3 took Wrenn from 5 to 2") { loyalty(game, wrenn) shouldBe 2 }
            }

            test("grows as you add lands — the P/T is characteristic-defining, not a snapshot") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Wrenn and Seven")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wrenn = game.findPermanent("Wrenn and Seven")!!
                setLoyalty(game, wrenn, 5)

                activate(game, wrenn, index = 2)
                game.resolveStack()

                val token = game.findPermanent("Treefolk Token")!!
                withClue("2 lands at creation time") {
                    game.state.projectedState.getPower(token) shouldBe 2
                }

                // Two more lands arrive after the token already exists.
                val grown = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Wrenn and Seven")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val grownWrenn = grown.findPermanent("Wrenn and Seven")!!
                setLoyalty(grown, grownWrenn, 5)
                activate(grown, grownWrenn, index = 2)
                grown.resolveStack()

                withClue("4 lands -> 4/4, so the count is read from the board, not baked in") {
                    grown.state.projectedState.getPower(grown.findPermanent("Treefolk Token")!!) shouldBe 4
                }
            }
        }

        context("the +1") {

            test("lands go to hand, everything else to the graveyard") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Wrenn and Seven")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Savannah Lions")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Lightning Bolt")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wrenn = game.findPermanent("Wrenn and Seven")!!
                setLoyalty(game, wrenn, 5)

                activate(game, wrenn, index = 0)
                game.resolveStack()

                withClue("both lands were put into hand") {
                    game.findCardsInHand(1, "Forest").size shouldBe 2
                }
                withClue("the nonlands went to the graveyard") {
                    game.isInGraveyard(1, "Savannah Lions") shouldBe true
                    game.isInGraveyard(1, "Lightning Bolt") shouldBe true
                }
                withClue("+1 took Wrenn from 5 to 6") { loyalty(game, wrenn) shouldBe 6 }
            }
        }

        context("the 0") {

            test("puts chosen lands from hand onto the battlefield tapped") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Wrenn and Seven")
                    .withCardInHand(1, "Forest")
                    .withCardInHand(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wrenn = game.findPermanent("Wrenn and Seven")!!
                setLoyalty(game, wrenn, 5)

                activate(game, wrenn, index = 1)
                game.resolveStack()
                game.selectCards(game.findCardsInHand(1, "Forest"))
                game.resolveStack()

                withClue("both lands are on the battlefield") {
                    game.findAllPermanents("Forest").size shouldBe 2
                }
                withClue("and they entered tapped") {
                    game.findAllPermanents("Forest").all {
                        game.state.getEntity(it)?.get<TappedComponent>() != null
                    } shouldBe true
                }
                withClue("0 leaves loyalty untouched") { loyalty(game, wrenn) shouldBe 5 }
            }
        }

        context("the −8") {

            test("returns only permanent cards from the graveyard and removes maximum hand size") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Wrenn and Seven")
                    .withCardInGraveyard(1, "Savannah Lions")
                    .withCardInGraveyard(1, "Forest")
                    .withCardInGraveyard(1, "Lightning Bolt")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wrenn = game.findPermanent("Wrenn and Seven")!!
                setLoyalty(game, wrenn, 8)

                activate(game, wrenn, index = 3)
                game.resolveStack()

                withClue("the creature and the land came back") {
                    game.isInHand(1, "Savannah Lions") shouldBe true
                    game.isInHand(1, "Forest") shouldBe true
                }
                withClue("the instant is not a permanent card and stayed put") {
                    game.isInGraveyard(1, "Lightning Bolt") shouldBe true
                    game.isInHand(1, "Lightning Bolt") shouldBe false
                }
            }
        }
    }

    private fun activate(game: TestGame, source: EntityId, index: Int) {
        val ability = cardRegistry.getCard("Wrenn and Seven")!!.script.activatedAbilities[index]
        game.execute(
            ActivateAbility(
                playerId = game.player1Id,
                sourceId = source,
                abilityId = ability.id
            )
        ).error shouldBe null
    }

    private fun loyalty(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    private fun setLoyalty(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with(CountersComponent().withAdded(CounterType.LOYALTY, amount))
        }
    }
}
