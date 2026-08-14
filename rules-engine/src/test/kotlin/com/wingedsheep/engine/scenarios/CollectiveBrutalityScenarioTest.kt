package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain

/**
 * Scenario test for Collective Brutality (EMN #85) — {1}{B} sorcery, "Escalate—Discard a card",
 * three modes.
 *
 * It is the first card whose escalate cost isn't mana (CR 702.120a), so these pin the cost scaling
 * the engine does for it: one mode owes nothing, two modes owe one discard, three modes owe two.
 * Mode 1's "-2/-2" and mode 2's "loses 2 life and you gain 2 life" ride along.
 */
class CollectiveBrutalityScenarioTest : ScenarioTestBase() {

    init {
        context("Collective Brutality") {

            /** Cast Collective Brutality with an explicit mode subset, per-mode targets, and discards. */
            fun ScenarioTestBase.TestGame.castBrutality(
                modes: List<Int>,
                modeTargets: List<List<ChosenTarget>>,
                discards: List<EntityId> = emptyList(),
            ) = execute(
                CastSpell(
                    playerId = player1Id,
                    cardId = state.getHand(player1Id).first {
                        state.getEntity(it)?.get<CardComponent>()?.name == "Collective Brutality"
                    },
                    targets = modeTargets.flatten(),
                    chosenModes = modes,
                    modeTargetsOrdered = modeTargets,
                    additionalCostPayment = AdditionalCostPayment(discardedCards = discards),
                )
            )

            fun ScenarioTestBase.TestGame.cardInHand(playerNumber: Int, name: String): EntityId =
                findCardsInHand(playerNumber, name).first()

            test("one mode owes no escalate cost — the drain resolves and nothing is discarded") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Collective Brutality")
                    .withCardInHand(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val lifeBefore = game.getLifeTotal(1)
                val opponentLifeBefore = game.getLifeTotal(2)

                val cast = game.castBrutality(
                    modes = listOf(2),
                    modeTargets = listOf(listOf(ChosenTarget.Player(game.player2Id))),
                )
                withClue("a single-mode cast pays no escalate cost: ${cast.error}") {
                    cast.error shouldBe null
                }
                withClue("nothing was discarded") { game.isInHand(1, "Grizzly Bears") shouldBe true }

                game.resolveStack()
                game.getLifeTotal(2) shouldBe (opponentLifeBefore - 2)
                game.getLifeTotal(1) shouldBe (lifeBefore + 2)
            }

            test("two modes discard one card and both modes resolve") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Collective Brutality")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Centaur Courser") // 3/3 — survives -2/-2
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val courser = game.findPermanent("Centaur Courser")!!
                val opponentLifeBefore = game.getLifeTotal(2)

                val cast = game.castBrutality(
                    modes = listOf(1, 2),
                    modeTargets = listOf(
                        listOf(ChosenTarget.Permanent(courser)),
                        listOf(ChosenTarget.Player(game.player2Id)),
                    ),
                    discards = listOf(game.cardInHand(1, "Grizzly Bears")),
                )
                withClue("two modes owe exactly one discard: ${cast.error}") { cast.error shouldBe null }

                withClue("the escalate cost is paid as the spell is cast, before it goes on the stack") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                }

                game.resolveStack()
                withClue("-2/-2 shrinks the 3/3 to a 1/1 rather than killing it") {
                    game.findPermanent("Centaur Courser") shouldNotBe null
                }
                game.getLifeTotal(2) shouldBe (opponentLifeBefore - 2)
            }

            test("three modes discard two cards and every mode resolves") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Collective Brutality")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInHand(1, "Centaur Courser")
                    .withCardInHand(2, "Lightning Bolt") // the only instant/sorcery to take
                    .withCardInHand(2, "Forest")
                    .withCardOnBattlefield(2, "Savannah Lions") // 1/1 — dies to -2/-2
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val lions = game.findPermanent("Savannah Lions")!!
                val opponentLifeBefore = game.getLifeTotal(2)
                val lifeBefore = game.getLifeTotal(1)

                val cast = game.castBrutality(
                    modes = listOf(0, 1, 2),
                    modeTargets = listOf(
                        listOf(ChosenTarget.Player(game.player2Id)),
                        listOf(ChosenTarget.Permanent(lions)),
                        listOf(ChosenTarget.Player(game.player2Id)),
                    ),
                    discards = listOf(
                        game.cardInHand(1, "Grizzly Bears"),
                        game.cardInHand(1, "Centaur Courser"),
                    ),
                )
                withClue("three modes owe two discards: ${cast.error}") { cast.error shouldBe null }
                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                game.isInGraveyard(1, "Centaur Courser") shouldBe true

                game.resolveStack()

                // Mode 0 pauses for the caster to pick a card out of the revealed hand.
                game.selectCards(listOf(game.cardInHand(2, "Lightning Bolt")))
                game.resolveStack()

                withClue("mode 0 took the opponent's only instant") {
                    game.isInGraveyard(2, "Lightning Bolt") shouldBe true
                }
                withClue("the land was never an eligible choice") {
                    game.isInHand(2, "Forest") shouldBe true
                }
                withClue("-2/-2 killed the 1/1") { game.findPermanent("Savannah Lions") shouldBe null }
                game.getLifeTotal(2) shouldBe (opponentLifeBefore - 2)
                game.getLifeTotal(1) shouldBe (lifeBefore + 2)
            }

            test("a second mode without its discard is rejected") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Collective Brutality")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Centaur Courser")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val courser = game.findPermanent("Centaur Courser")!!
                val cast = game.castBrutality(
                    modes = listOf(1, 2),
                    modeTargets = listOf(
                        listOf(ChosenTarget.Permanent(courser)),
                        listOf(ChosenTarget.Player(game.player2Id)),
                    ),
                )
                withClue("the escalate cost is mandatory once a second mode is chosen") {
                    cast.error shouldNotBe null
                }
                cast.error!!.lowercase() shouldContain "discard"
            }

            test("three modes are unpayable with only one spare card in hand") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Collective Brutality")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Centaur Courser")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val courser = game.findPermanent("Centaur Courser")!!
                val cast = game.castBrutality(
                    modes = listOf(0, 1, 2),
                    modeTargets = listOf(
                        listOf(ChosenTarget.Player(game.player2Id)),
                        listOf(ChosenTarget.Permanent(courser)),
                        listOf(ChosenTarget.Player(game.player2Id)),
                    ),
                    discards = listOf(game.cardInHand(1, "Grizzly Bears")),
                )
                withClue("one card can't pay for two extra modes") { cast.error shouldNotBe null }
            }

            test("the offered mode count is capped by the cards available to escalate with") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Collective Brutality")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Centaur Courser")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val modal = game.getLegalActions(1)
                    .first { it.description.contains("Collective Brutality") }
                    .modalEnumeration
                withClue("one spare card pays for exactly one extra mode") {
                    modal?.chooseCount shouldBe 2
                }
                withClue("the client is told what each extra mode costs") {
                    modal?.additionalCostPerExtraMode?.costType shouldBe "DiscardCard"
                }
            }
        }
    }
}
