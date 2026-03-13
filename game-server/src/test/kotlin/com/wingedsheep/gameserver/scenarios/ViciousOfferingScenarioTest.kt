package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Vicious Offering.
 *
 * Card reference:
 * - Vicious Offering ({1}{B}): Instant
 *   Kicker—Sacrifice a creature.
 *   Target creature gets -2/-2 until end of turn.
 *   If this spell was kicked, that creature gets -5/-5 until end of turn instead.
 */
class ViciousOfferingScenarioTest : ScenarioTestBase() {

    init {
        context("Vicious Offering") {

            test("unkicked gives -2/-2 - creature with 3 toughness survives") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Vicious Offering")
                    .withCardOnBattlefield(2, "Hill Giant") // 3/3
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpell(1, "Vicious Offering", game.findPermanent("Hill Giant")!!)
                withClue("Cast should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                // Hill Giant is 3/3, unkicked gives -2/-2 → 1/1, survives
                withClue("Hill Giant should survive -2/-2") {
                    game.isOnBattlefield("Hill Giant") shouldBe true
                }
            }

            test("unkicked gives -2/-2 - creature with 2 toughness dies") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Vicious Offering")
                    .withCardOnBattlefield(2, "Glory Seeker") // 2/2
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpell(1, "Vicious Offering", game.findPermanent("Glory Seeker")!!)
                withClue("Cast should succeed") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                withClue("Glory Seeker (2/2) should die from -2/-2") {
                    game.isOnBattlefield("Glory Seeker") shouldBe false
                }
            }

            test("kicked gives -5/-5 by sacrificing a creature") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Vicious Offering")
                    .withCardOnBattlefield(1, "Llanowar Elves") // sacrifice fodder
                    .withCardOnBattlefield(2, "Serra Angel") // 4/4
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val playerId = game.player1Id
                val hand = game.state.getHand(playerId)
                val cardId = hand.find { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Vicious Offering"
                }!!
                val angelId = game.findPermanent("Serra Angel")!!
                val elvesId = game.findPermanent("Llanowar Elves")!!

                val castResult = game.execute(
                    CastSpell(
                        playerId, cardId,
                        targets = listOf(ChosenTarget.Permanent(angelId)),
                        wasKicked = true,
                        additionalCostPayment = AdditionalCostPayment(sacrificedPermanents = listOf(elvesId))
                    )
                )
                withClue("Kicked cast should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                // Serra Angel is 4/4, kicked gives -5/-5 → dies
                withClue("Serra Angel (4/4) should die from -5/-5") {
                    game.isOnBattlefield("Serra Angel") shouldBe false
                }
                // Llanowar Elves was sacrificed
                withClue("Llanowar Elves should have been sacrificed") {
                    game.isOnBattlefield("Llanowar Elves") shouldBe false
                }
            }

            test("kicked fails without a creature to sacrifice") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Vicious Offering")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val playerId = game.player1Id
                val hand = game.state.getHand(playerId)
                val cardId = hand.find { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Vicious Offering"
                }!!
                val giantId = game.findPermanent("Hill Giant")!!

                // Try to kick without providing sacrifice
                val castResult = game.execute(
                    CastSpell(
                        playerId, cardId,
                        targets = listOf(ChosenTarget.Permanent(giantId)),
                        wasKicked = true
                    )
                )
                withClue("Kicked cast should fail without sacrifice") {
                    castResult.error shouldBe "You must sacrifice 1 creature to cast this spell"
                }
            }
        }
    }
}
