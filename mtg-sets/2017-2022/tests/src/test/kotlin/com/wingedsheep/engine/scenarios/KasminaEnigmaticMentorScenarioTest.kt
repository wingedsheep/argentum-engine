package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.war.cards.KasminaEnigmaticMentor
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Kasmina, Enigmatic Mentor (WAR #56; {3}{U}, Loyalty 5).
 *
 *   Spells your opponents cast that target a creature or planeswalker you control cost {2} more to cast.
 *   −2: Create a 2/2 blue Wizard creature token. Draw a card, then discard a card.
 *
 * The tax tests put Kasmina on the non-active player's side (player 2) so the active player can cast
 * at their permanents with an exact land count: one land short of the tax is rejected, the taxed total
 * is accepted, and untaxed shapes (targeting a player, targeting the caster's own creature, Kasmina's
 * controller targeting their own creature) cost only the printed cost.
 */
class KasminaEnigmaticMentorScenarioTest : ScenarioTestBase() {

    private val minusTwo = KasminaEnigmaticMentor.activatedAbilities[0].id

    init {
        context("Kasmina, Enigmatic Mentor") {

            test("an opponent's spell targeting a creature you control costs {2} more") {
                val short = taxBoard(mountains = 2)
                val bears = short.findPermanent("Grizzly Bears")!!
                withClue("Lightning Bolt at Kasmina's controller's creature can't be cast for {R}{1}") {
                    short.castSpell(1, "Lightning Bolt", bears).error shouldNotBe null
                }

                val paid = taxBoard(mountains = 3)
                val bears2 = paid.findPermanent("Grizzly Bears")!!
                withClue("{2}{R} pays for it") {
                    paid.castSpell(1, "Lightning Bolt", bears2).error shouldBe null
                }
                paid.resolveStack()
                paid.isInGraveyard(2, "Grizzly Bears") shouldBe true
            }

            test("an opponent's spell targeting a planeswalker you control is taxed too") {
                val short = taxBoard(mountains = 2)
                val kasmina = short.findPermanent("Kasmina, Enigmatic Mentor")!!
                short.castSpell(1, "Lightning Bolt", kasmina).error shouldNotBe null

                val paid = taxBoard(mountains = 3)
                val kasmina2 = paid.findPermanent("Kasmina, Enigmatic Mentor")!!
                paid.castSpell(1, "Lightning Bolt", kasmina2).error shouldBe null
                paid.resolveStack()
                loyalty(paid, kasmina2) shouldBe 2
            }

            test("targeting Kasmina's controller (a player) is not taxed") {
                val game = taxBoard(mountains = 1)
                val result = game.execute(
                    com.wingedsheep.engine.core.CastSpell(
                        game.player1Id,
                        game.findCardsInHand(1, "Lightning Bolt").single(),
                        listOf(ChosenTarget.Player(game.player2Id))
                    )
                )
                result.error shouldBe null
                game.resolveStack()
                game.getLifeTotal(2) shouldBe 17
            }

            test("an opponent targeting their own creature, and you targeting yours, pay no tax") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(2, "Kasmina, Enigmatic Mentor")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(1, "Giant Growth")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                seedLoyalty(game, game.findPermanent("Kasmina, Enigmatic Mentor")!!, 5)
                withClue("the caster's own creature isn't 'a creature you control' for Kasmina") {
                    game.castSpell(1, "Giant Growth", game.findPermanent("Grizzly Bears")!!).error shouldBe null
                }

                val own = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Kasmina, Enigmatic Mentor")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(1, "Giant Growth")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                seedLoyalty(own, own.findPermanent("Kasmina, Enigmatic Mentor")!!, 5)
                withClue("Kasmina only taxes opponents' spells") {
                    own.castSpell(1, "Giant Growth", own.findPermanent("Grizzly Bears")!!).error shouldBe null
                }
            }

            test("−2 creates a 2/2 blue Wizard, then draws and discards") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Kasmina, Enigmatic Mentor")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val kasmina = game.findPermanent("Kasmina, Enigmatic Mentor")!!
                seedLoyalty(game, kasmina, 5)

                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = kasmina, abilityId = minusTwo)
                ).error shouldBe null
                loyalty(game, kasmina) shouldBe 3
                game.resolveStack()

                val discard = game.getPendingDecision() as? SelectCardsDecision
                    ?: error("expected a discard selection; got ${game.getPendingDecision()}")
                withClue("the discard is chosen from a hand that includes the drawn card") {
                    discard.options.size shouldBe 2
                }
                game.selectCards(game.findCardsInHand(1, "Grizzly Bears")).error shouldBe null
                game.resolveStack()

                val token = game.findPermanent("Wizard Token")
                withClue("a Wizard token was created") { token shouldNotBe null }
                val projected = game.state.projectedState
                projected.getProjectedValues(token!!)?.power shouldBe 2
                projected.getProjectedValues(token)?.toughness shouldBe 2
                projected.hasColor(token, Color.BLUE) shouldBe true
                projected.hasSubtype(token, "Wizard") shouldBe true

                game.isInHand(1, "Hill Giant") shouldBe true
                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                game.handSize(1) shouldBe 1
            }
        }
    }

    private fun taxBoard(mountains: Int): TestGame {
        val game = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(2, "Kasmina, Enigmatic Mentor")
            .withCardOnBattlefield(2, "Grizzly Bears")
            .withCardInHand(1, "Lightning Bolt")
            .withLandsOnBattlefield(1, "Mountain", mountains)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        seedLoyalty(game, game.findPermanent("Kasmina, Enigmatic Mentor")!!, 5)
        return game
    }

    /** The scenario builder doesn't run "enters with starting loyalty", so seed the counters. */
    private fun seedLoyalty(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with(CountersComponent().withAdded(CounterType.LOYALTY, amount))
        }
    }

    private fun loyalty(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0
}
