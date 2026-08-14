package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Atlantis Attacks (MSH #46) — {5}{U}{U} Sorcery.
 *
 *   Teamwork 4
 *   Choose one. If this spell was cast using teamwork, choose both instead.
 *   • Target player creates a 6/5 blue Leviathan creature token with hexproof.
 *   • Return one or two target nonland permanents to their owners' hands.
 *
 * The bounce mode is a single "one or two target" requirement, so the teamwork case takes both an
 * opposing creature and an opposing artifact in one mode.
 */
class AtlantisAttacksScenarioTest : ScenarioTestBase() {

    init {
        context("Atlantis Attacks") {

            test("cast without teamwork resolves only the one chosen mode") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Atlantis Attacks")
                    .withLandsOnBattlefield(1, "Island", 7)
                    .withCardOnBattlefield(1, "Craw Wurm")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Sol Ring")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wurm = game.findPermanent("Craw Wurm").shouldNotBeNull()
                val cardId = game.findCardsInHand(1, "Atlantis Attacks").first()

                game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = cardId,
                        targets = listOf(ChosenTarget.Player(game.player1Id)),
                        chosenModes = listOf(0),
                        modeTargetsOrdered = listOf(listOf(ChosenTarget.Player(game.player1Id))),
                    ),
                ).error shouldBe null
                game.resolveStack()

                val token = game.findPermanent("Leviathan Token").shouldNotBeNull()
                game.state.projectedState.getPower(token) shouldBe 6
                game.state.projectedState.getToughness(token) shouldBe 5
                game.state.projectedState.hasKeyword(token, Keyword.HEXPROOF) shouldBe true

                withClue("the bounce mode was not chosen, so the board is untouched") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                    game.isOnBattlefield("Sol Ring") shouldBe true
                }
                withClue("no teamwork was declared, so nothing tapped") {
                    game.state.getEntity(wurm)?.has<TappedComponent>() shouldBe false
                }
            }

            test("cast using teamwork resolves both modes and bounces two permanents") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Atlantis Attacks")
                    .withLandsOnBattlefield(1, "Island", 7)
                    .withCardOnBattlefield(1, "Craw Wurm")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Sol Ring")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wurm = game.findPermanent("Craw Wurm").shouldNotBeNull()
                val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()
                val solRing = game.findPermanent("Sol Ring").shouldNotBeNull()
                val cardId = game.findCardsInHand(1, "Atlantis Attacks").first()

                // Teamwork 4 — the 6/4 Craw Wurm clears the threshold on its own.
                game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = cardId,
                        targets = listOf(
                            ChosenTarget.Player(game.player1Id),
                            ChosenTarget.Permanent(bears),
                            ChosenTarget.Permanent(solRing),
                        ),
                        chosenModes = listOf(0, 1),
                        modeTargetsOrdered = listOf(
                            listOf(ChosenTarget.Player(game.player1Id)),
                            listOf(ChosenTarget.Permanent(bears), ChosenTarget.Permanent(solRing)),
                        ),
                        declaredCostSlot = ChoiceSlot.TEAMWORK,
                        additionalCostPayment = AdditionalCostPayment(
                            variableCostPermanents = listOf(wurm),
                        ),
                    ),
                ).error shouldBe null
                game.state.getEntity(wurm)?.has<TappedComponent>() shouldBe true

                game.resolveStack()

                game.findPermanent("Leviathan Token").shouldNotBeNull()
                game.isOnBattlefield("Grizzly Bears") shouldBe false
                game.isOnBattlefield("Sol Ring") shouldBe false
                game.isInHand(2, "Grizzly Bears") shouldBe true
                game.isInHand(2, "Sol Ring") shouldBe true
            }

            test("the bounce mode accepts a single target") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Atlantis Attacks")
                    .withLandsOnBattlefield(1, "Island", 7)
                    .withCardOnBattlefield(1, "Craw Wurm")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Sol Ring")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wurm = game.findPermanent("Craw Wurm").shouldNotBeNull()
                val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()
                val cardId = game.findCardsInHand(1, "Atlantis Attacks").first()

                // "One or two target nonland permanents" — `minCount = 1`, so one is enough.
                game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = cardId,
                        targets = listOf(
                            ChosenTarget.Player(game.player1Id),
                            ChosenTarget.Permanent(bears),
                        ),
                        chosenModes = listOf(0, 1),
                        modeTargetsOrdered = listOf(
                            listOf(ChosenTarget.Player(game.player1Id)),
                            listOf(ChosenTarget.Permanent(bears)),
                        ),
                        declaredCostSlot = ChoiceSlot.TEAMWORK,
                        additionalCostPayment = AdditionalCostPayment(
                            variableCostPermanents = listOf(wurm),
                        ),
                    ),
                ).error shouldBe null

                game.resolveStack()

                game.isInHand(2, "Grizzly Bears") shouldBe true
                withClue("only one target was chosen, so the second permanent stays put") {
                    game.isOnBattlefield("Sol Ring") shouldBe true
                }
            }

            test("choosing one mode with teamwork declared is rejected") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Atlantis Attacks")
                    .withLandsOnBattlefield(1, "Island", 7)
                    .withCardOnBattlefield(1, "Craw Wurm")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Sol Ring")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wurm = game.findPermanent("Craw Wurm").shouldNotBeNull()
                val cardId = game.findCardsInHand(1, "Atlantis Attacks").first()

                // "Choose both instead" is not an allowance — a cast that declared teamwork owes
                // both modes, so a one-mode submission is illegal (CR 601.2, 700.2a).
                game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = cardId,
                        targets = listOf(ChosenTarget.Player(game.player1Id)),
                        chosenModes = listOf(0),
                        modeTargetsOrdered = listOf(listOf(ChosenTarget.Player(game.player1Id))),
                        declaredCostSlot = ChoiceSlot.TEAMWORK,
                        additionalCostPayment = AdditionalCostPayment(
                            variableCostPermanents = listOf(wurm),
                        ),
                    ),
                ).error.shouldNotBeNull()

                withClue("the rejected cast is rewound whole — no token was created") {
                    game.isInHand(1, "Atlantis Attacks") shouldBe true
                    game.state.getEntity(wurm)?.has<TappedComponent>() shouldBe false
                    game.findPermanent("Leviathan Token") shouldBe null
                }
            }

            test("choosing both modes without teamwork is rejected") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Atlantis Attacks")
                    .withLandsOnBattlefield(1, "Island", 7)
                    .withCardOnBattlefield(1, "Craw Wurm")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Sol Ring")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()
                val solRing = game.findPermanent("Sol Ring").shouldNotBeNull()
                val cardId = game.findCardsInHand(1, "Atlantis Attacks").first()

                game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = cardId,
                        targets = listOf(
                            ChosenTarget.Player(game.player1Id),
                            ChosenTarget.Permanent(bears),
                            ChosenTarget.Permanent(solRing),
                        ),
                        chosenModes = listOf(0, 1),
                        modeTargetsOrdered = listOf(
                            listOf(ChosenTarget.Player(game.player1Id)),
                            listOf(ChosenTarget.Permanent(bears), ChosenTarget.Permanent(solRing)),
                        ),
                    ),
                ).error.shouldNotBeNull()
                game.isInHand(1, "Atlantis Attacks") shouldBe true
            }
        }
    }
}
