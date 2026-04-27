package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Pyrrhic Strike.
 *
 * Card reference:
 * - Pyrrhic Strike ({2}{W}): Instant
 *   "As an additional cost to cast this spell, you may blight 2.
 *    Choose one. If this spell's additional cost was paid, choose both instead.
 *    • Destroy target artifact or enchantment.
 *    • Destroy target creature with mana value 3 or greater."
 */
class PyrrhicStrikeScenarioTest : ScenarioTestBase() {

    init {
        test("blight paid - destroys both artifact and creature with mana value 3+") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Pyrrhic Strike")
                .withCardOnBattlefield(1, "Grizzly Bears") // P1's creature for blight target
                .withCardOnBattlefield(2, "Icy Manipulator") // artifact target (mode 0)
                .withCardOnBattlefield(2, "Hill Giant") // MV 3 creature target (mode 1)
                .withLandsOnBattlefield(1, "Plains", 3)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val grizzlyBears = game.findPermanent("Grizzly Bears")!!
            val icyManipulator = game.findPermanent("Icy Manipulator")!!
            val hillGiant = game.findPermanent("Hill Giant")!!

            val pyrrhicStrike = game.state.getHand(game.player1Id).first { entityId ->
                game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Pyrrhic Strike"
            }

            val castResult = game.execute(
                CastSpell(
                    playerId = game.player1Id,
                    cardId = pyrrhicStrike,
                    targets = listOf(
                        ChosenTarget.Permanent(icyManipulator),
                        ChosenTarget.Permanent(hillGiant)
                    ),
                    chosenModes = listOf(0, 1),
                    modeTargetsOrdered = listOf(
                        listOf(ChosenTarget.Permanent(icyManipulator)),
                        listOf(ChosenTarget.Permanent(hillGiant))
                    ),
                    additionalCostPayment = AdditionalCostPayment(
                        blightTargets = listOf(grizzlyBears)
                    )
                )
            )
            withClue("Pyrrhic Strike (blight + both modes) should be cast: ${castResult.error}") {
                castResult.error shouldBe null
            }

            // Grizzly Bears should have two -1/-1 counters from blight 2
            val counters = game.state.getEntity(grizzlyBears)?.get<CountersComponent>()
            withClue("Grizzly Bears should have 2 -1/-1 counters") {
                counters.shouldNotBeNull()
                counters.getCount(CounterType.MINUS_ONE_MINUS_ONE) shouldBe 2
            }

            game.resolveStack()

            withClue("Icy Manipulator should be destroyed") {
                game.isOnBattlefield("Icy Manipulator") shouldBe false
                game.isInGraveyard(2, "Icy Manipulator") shouldBe true
            }
            withClue("Hill Giant should be destroyed") {
                game.isOnBattlefield("Hill Giant") shouldBe false
                game.isInGraveyard(2, "Hill Giant") shouldBe true
            }
        }

        test("blight not paid - choosing one mode destroys only that target") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Pyrrhic Strike")
                .withCardOnBattlefield(2, "Icy Manipulator") // artifact
                .withCardOnBattlefield(2, "Hill Giant") // creature MV 3
                .withLandsOnBattlefield(1, "Plains", 3)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val icyManipulator = game.findPermanent("Icy Manipulator")!!

            val pyrrhicStrike = game.state.getHand(game.player1Id).first { entityId ->
                game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Pyrrhic Strike"
            }

            val castResult = game.execute(
                CastSpell(
                    playerId = game.player1Id,
                    cardId = pyrrhicStrike,
                    targets = listOf(ChosenTarget.Permanent(icyManipulator)),
                    chosenModes = listOf(0),
                    modeTargetsOrdered = listOf(listOf(ChosenTarget.Permanent(icyManipulator)))
                )
            )
            withClue("Pyrrhic Strike (no blight, mode 0) should be cast: ${castResult.error}") {
                castResult.error shouldBe null
            }

            game.resolveStack()

            withClue("Icy Manipulator should be destroyed (mode 0)") {
                game.isOnBattlefield("Icy Manipulator") shouldBe false
                game.isInGraveyard(2, "Icy Manipulator") shouldBe true
            }
            withClue("Hill Giant should still be on the battlefield (mode 1 not chosen)") {
                game.isOnBattlefield("Hill Giant") shouldBe true
            }
        }

        test("rejects choosing both modes without paying blight") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Pyrrhic Strike")
                .withCardOnBattlefield(2, "Icy Manipulator")
                .withCardOnBattlefield(2, "Hill Giant")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val icyManipulator = game.findPermanent("Icy Manipulator")!!
            val hillGiant = game.findPermanent("Hill Giant")!!

            val pyrrhicStrike = game.state.getHand(game.player1Id).first { entityId ->
                game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Pyrrhic Strike"
            }

            val castResult = game.execute(
                CastSpell(
                    playerId = game.player1Id,
                    cardId = pyrrhicStrike,
                    targets = listOf(
                        ChosenTarget.Permanent(icyManipulator),
                        ChosenTarget.Permanent(hillGiant)
                    ),
                    chosenModes = listOf(0, 1),
                    modeTargetsOrdered = listOf(
                        listOf(ChosenTarget.Permanent(icyManipulator)),
                        listOf(ChosenTarget.Permanent(hillGiant))
                    )
                    // No blight paid → may only choose one mode
                )
            )
            withClue("Should reject choosing both modes when blight not paid") {
                castResult.error.shouldNotBeNull()
            }
        }

        test("rejects choosing only one mode when blight is paid") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Pyrrhic Strike")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Icy Manipulator")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val grizzlyBears = game.findPermanent("Grizzly Bears")!!
            val icyManipulator = game.findPermanent("Icy Manipulator")!!

            val pyrrhicStrike = game.state.getHand(game.player1Id).first { entityId ->
                game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Pyrrhic Strike"
            }

            val castResult = game.execute(
                CastSpell(
                    playerId = game.player1Id,
                    cardId = pyrrhicStrike,
                    targets = listOf(ChosenTarget.Permanent(icyManipulator)),
                    chosenModes = listOf(0),
                    modeTargetsOrdered = listOf(listOf(ChosenTarget.Permanent(icyManipulator))),
                    additionalCostPayment = AdditionalCostPayment(
                        blightTargets = listOf(grizzlyBears)
                    )
                )
            )
            withClue("Should reject choosing one mode when blight is paid (must choose both)") {
                castResult.error.shouldNotBeNull()
            }
        }
    }
}
