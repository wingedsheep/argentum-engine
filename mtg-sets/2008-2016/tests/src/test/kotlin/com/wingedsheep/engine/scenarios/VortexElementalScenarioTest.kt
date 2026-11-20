package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.bng.cards.VortexElemental
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Vortex Elemental — "{U}: Put this creature and each creature blocking or blocked by it on top of
 * their owners' libraries, then those players shuffle."
 *
 * The pairing is read live while the Elemental is on the battlefield, and as last-known
 * information once it has left before the ability resolves (the card's ruling).
 */
class VortexElementalScenarioTest : ScenarioTestBase() {

    private val vortexAbilityId = VortexElemental.activatedAbilities.first().id

    private fun blockedVortex() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Vortex Elemental")
        .withLandsOnBattlefield(1, "Island", 2)
        .withCardInHand(1, "Unsummon")
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withCardOnBattlefield(2, "Savannah Lions")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        .build()
        .also { game ->
            game.declareAttackers(mapOf("Vortex Elemental" to 2))
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Grizzly Bears" to listOf("Vortex Elemental"))).error shouldBe null
            if (game.state.priorityPlayerId == game.player2Id) game.passPriority().error shouldBe null
            withClue("still in declare blockers with the attacker holding priority") {
                game.state.step shouldBe Step.DECLARE_BLOCKERS
                game.state.priorityPlayerId shouldBe game.player1Id
            }
        }

    private fun ScenarioTestBase.TestGame.activateVortex() {
        val vortex = findPermanent("Vortex Elemental")!!
        val result = execute(ActivateAbility(playerId = player1Id, sourceId = vortex, abilityId = vortexAbilityId))
        withClue("${result.error}") { result.error shouldBe null }
        if (getPendingDecision() is SelectManaSourcesDecision) submitManaSourcesAutoPay()
    }

    init {
        test("puts itself and the creature blocking it into their owners' libraries, sparing bystanders") {
            val game = blockedVortex()
            game.activateVortex()
            game.resolveStack()

            game.findPermanent("Vortex Elemental") shouldBe null
            game.findPermanent("Grizzly Bears") shouldBe null
            game.findCardsInLibrary(1, "Vortex Elemental") shouldHaveSize 1
            game.findCardsInLibrary(2, "Grizzly Bears") shouldHaveSize 1
            game.findPermanent("Savannah Lions") shouldNotBe null
        }

        test("if the Elemental leaves before the ability resolves, its blocker still goes to the library") {
            val game = blockedVortex()
            game.activateVortex()
            // In response, bounce the Elemental itself: the pairing is read as last-known information.
            val vortex = game.findPermanent("Vortex Elemental")!!
            game.castSpell(1, "Unsummon", vortex).error shouldBe null
            if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
            game.resolveStack()

            game.findCardsInHand(1, "Vortex Elemental") shouldHaveSize 1
            game.findCardsInLibrary(1, "Vortex Elemental") shouldHaveSize 0
            game.findPermanent("Grizzly Bears") shouldBe null
            game.findCardsInLibrary(2, "Grizzly Bears") shouldHaveSize 1
            game.findPermanent("Savannah Lions") shouldNotBe null
        }

        test("outside combat only the Elemental is put into its library") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Vortex Elemental")
                .withLandsOnBattlefield(1, "Island", 1)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.activateVortex()
            game.resolveStack()

            game.findCardsInLibrary(1, "Vortex Elemental") shouldHaveSize 1
            game.findPermanent("Grizzly Bears") shouldNotBe null
        }
    }
}
