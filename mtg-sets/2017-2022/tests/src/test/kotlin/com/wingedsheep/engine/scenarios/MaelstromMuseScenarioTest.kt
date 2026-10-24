package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Maelstrom Muse (STX #202) — "Whenever this creature attacks, the next instant or sorcery spell
 * you cast this turn costs {X} less to cast, where X is this creature's power as this ability
 * resolves."
 *
 * X is read as the trigger resolves, so a pump cast in response raises it; a creature spell cast
 * first leaves the rider waiting for the instant or sorcery that spends it.
 */
class MaelstromMuseScenarioTest : ScenarioTestBase() {

    init {
        context("Maelstrom Muse") {

            test("X is the Muse's power as the trigger resolves, and only an instant or sorcery spends it") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Maelstrom Muse")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardInHand(1, "Giant Growth")
                    .withCardInHand(1, "Llanowar Elves")
                    .withCardInHand(1, "Divination")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Maelstrom Muse" to 2)).error shouldBe null

                withClue("Giant Growth in response to the attack trigger makes the Muse 5/7") {
                    game.castSpell(1, "Giant Growth", game.findPermanent("Maelstrom Muse")).error shouldBe null
                }
                game.resolveStack()

                val rider = game.state.spellCostModifiers.single()
                rider.nextSpellOnly shouldBe true
                rider.amount shouldBe 5

                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("a creature spell doesn't match, so it pays full price and leaves the rider") {
                    game.castSpell(1, "Llanowar Elves").error shouldBe null
                    game.resolveStack()
                    game.state.spellCostModifiers.size shouldBe 1
                }

                withClue("{2}{U} Divination costs {U} with the {5} discount") {
                    game.castSpell(1, "Divination").error shouldBe null
                }
                game.resolveStack()
                game.state.spellCostModifiers shouldBe emptyList()
            }
        }
    }
}
