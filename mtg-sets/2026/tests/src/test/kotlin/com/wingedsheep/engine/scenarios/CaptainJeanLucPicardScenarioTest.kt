package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Captain Jean-Luc Picard (TRK #7) — {2}{W} Legendary Creature — Human Officer 2/3.
 *
 *   Other creatures you control get +1/+1.
 *   Whenever you cast a creature or Spacecraft spell, create a 1/1 red Officer creature token.
 *   Tap seven untapped Officers you control: Search your library for a Spacecraft card, put it onto
 *   the battlefield with seven charge counters on it, then shuffle.
 *
 * The activated ability is the composition worth pinning: a tap-seven-Officers cost Picard himself
 * can help pay, a library search restricted to Spacecraft, and the seven charge counters arriving
 * *with* the Spacecraft so it is stationed (here: an artifact creature at 6+) as it enters.
 */
class CaptainJeanLucPicardScenarioTest : ScenarioTestBase() {

    init {
        context("Captain Jean-Luc Picard") {

            test("other creatures you control get +1/+1, Picard doesn't") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Captain Jean-Luc Picard")
                    .withCardOnBattlefield(1, "Starfleet Crew")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val projected = game.state.projectedState
                val crew = game.findPermanent("Starfleet Crew")!!
                val picard = game.findPermanent("Captain Jean-Luc Picard")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                projected.getPower(crew) shouldBe 3
                projected.getToughness(crew) shouldBe 4
                projected.getPower(picard) shouldBe 2
                projected.getToughness(picard) shouldBe 3
                withClue("an opponent's creature isn't pumped") {
                    projected.getPower(bears) shouldBe 2
                }
            }

            test("casting a creature spell creates a 1/1 red Officer token") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Captain Jean-Luc Picard")
                    .withCardInHand(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Grizzly Bears").error shouldBe null
                game.resolveStack()

                val tokens = game.state.getBattlefield(game.player1Id).filter {
                    game.state.projectedState.hasSubtype(it, "Officer") &&
                        game.state.getEntity(it)?.get<CardComponent>()?.name != "Captain Jean-Luc Picard"
                }
                tokens.size shouldBe 1
                withClue("the 1/1 token gets Picard's +1/+1") {
                    game.state.projectedState.getPower(tokens.single()) shouldBe 2
                }
                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }

            test("tapping seven Officers puts a Spacecraft onto the battlefield with seven charge counters") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Captain Jean-Luc Picard")
                    .apply { repeat(6) { withCardOnBattlefield(1, "Starfleet Crew", summoningSickness = true) } }
                    .withCardInLibrary(1, "Oberth-Class Starship")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val picard = game.findPermanent("Captain Jean-Luc Picard")!!
                val officers = listOf(picard) + game.findPermanents("Starfleet Crew")
                officers.size shouldBe 7
                val ability = cardRegistry.getCard("Captain Jean-Luc Picard")!!.script.activatedAbilities.single().id

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = picard,
                        abilityId = ability,
                        costPayment = AdditionalCostPayment(tappedPermanents = officers)
                    )
                ).error shouldBe null
                withClue("all seven Officers, Picard included, were tapped to pay") {
                    officers.all { game.state.getEntity(it)?.has<TappedComponent>() == true } shouldBe true
                }
                game.resolveStack()

                val decision = game.getPendingDecision()
                withClue("the search pauses for a Spacecraft choice") {
                    (decision is SelectCardsDecision) shouldBe true
                }
                decision as SelectCardsDecision
                val offered = decision.options.mapNotNull { game.state.getEntity(it)?.get<CardComponent>()?.name }
                withClue("only Spacecraft cards are offered") {
                    offered shouldBe listOf("Oberth-Class Starship")
                }
                game.selectCards(decision.options)
                game.resolveStack()

                val ship = game.findPermanent("Oberth-Class Starship")!!
                game.state.getEntity(ship)?.get<CountersComponent>()?.getCount(CounterType.CHARGE) shouldBe 7
                withClue("stationed past 6+, the Spacecraft is an artifact creature") {
                    game.state.projectedState.isCreature(ship) shouldBe true
                }
            }

            test("six Officers can't pay the cost") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Captain Jean-Luc Picard")
                    .apply { repeat(5) { withCardOnBattlefield(1, "Starfleet Crew") } }
                    .withCardInLibrary(1, "Oberth-Class Starship")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val picard = game.findPermanent("Captain Jean-Luc Picard")!!
                val officers = listOf(picard) + game.findPermanents("Starfleet Crew")
                val ability = cardRegistry.getCard("Captain Jean-Luc Picard")!!.script.activatedAbilities.single().id

                (game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = picard,
                        abilityId = ability,
                        costPayment = AdditionalCostPayment(tappedPermanents = officers)
                    )
                ).error != null) shouldBe true
            }
        }
    }
}
