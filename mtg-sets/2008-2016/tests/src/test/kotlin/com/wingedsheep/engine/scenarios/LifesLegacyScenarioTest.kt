package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Life's Legacy (M15 #183) — "As an additional cost to cast this spell, sacrifice a creature.
 * Draw cards equal to the sacrificed creature's power."
 *
 * The draw count is cost-linked: the creature is already in the graveyard when the spell
 * resolves, so the amount must come from its last-known power (pumps included), not its
 * printed power or the card in the graveyard.
 */
class LifesLegacyScenarioTest : ScenarioTestBase() {

    private fun TestGame.castLegacy(sacrificed: EntityId?) = execute(
        CastSpell(
            playerId = player1Id,
            cardId = findCardsInHand(1, "Life's Legacy").single(),
            targets = emptyList(),
            additionalCostPayment = sacrificed?.let { AdditionalCostPayment(sacrificedPermanents = listOf(it)) },
        )
    )

    private fun board(creature: String? = "Grizzly Bears") = scenario()
        .withPlayers("Alice", "Bob")
        .withCardInHand(1, "Life's Legacy")
        .withCardInHand(1, "Giant Growth")
        .withLandsOnBattlefield(1, "Forest", 3)
        .apply { if (creature != null) withCardOnBattlefield(1, creature) }
        .apply { repeat(8) { withCardInLibrary(1, "Forest") } }
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Life's Legacy") {

            test("sacrificing a 2/2 draws two cards") {
                val game = board()
                val bears = game.findPermanent("Grizzly Bears")!!
                game.castLegacy(bears).error shouldBe null
                game.isInGraveyard(1, "Grizzly Bears") shouldBe true

                val handBefore = game.handSize(1)
                game.resolveStack()

                game.handSize(1) shouldBe handBefore + 2
                game.isInGraveyard(1, "Life's Legacy") shouldBe true
            }

            test("draws equal to the sacrificed creature's last-known power, pump included") {
                val game = board()
                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Giant Growth", bears).error shouldBe null
                game.resolveStack()
                game.state.projectedState.getPower(bears) shouldBe 5

                game.castLegacy(bears).error shouldBe null
                val handBefore = game.handSize(1)
                game.resolveStack()

                game.handSize(1) shouldBe handBefore + 5
            }

            test("a 0-power creature draws nothing") {
                val game = board(creature = "Ornithopter")
                game.castLegacy(game.findPermanent("Ornithopter")!!).error shouldBe null
                val handBefore = game.handSize(1)
                game.resolveStack()

                game.handSize(1) shouldBe handBefore
                game.isInGraveyard(1, "Ornithopter") shouldBe true
            }

            test("can't be cast without sacrificing a creature") {
                val game = board(creature = null)
                game.castLegacy(null).error shouldNotBe null
                game.isInHand(1, "Life's Legacy") shouldBe true
            }
        }
    }
}
