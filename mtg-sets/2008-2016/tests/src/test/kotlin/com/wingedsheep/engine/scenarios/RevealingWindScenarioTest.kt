package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.RevealedToComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Revealing Wind — "Prevent all combat damage that would be dealt this turn. You may look at each
 * face-down creature that's attacking or blocking."
 *
 * Pins that the look reaches every face-down creature in combat (attacker and blocker), privately
 * to the caster, and skips a face-down creature that stayed out of combat — alongside the fog.
 */
class RevealingWindScenarioTest : ScenarioTestBase() {

    init {
        test("prevents combat damage and lets the caster look at only the face-down creatures in combat") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Revealing Wind")
                .withLandsOnBattlefield(1, "Forest", 3)
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardOnBattlefield(2, "Centaur Courser")
                .withCardOnBattlefield(2, "Glory Seeker")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(2)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            val courser = game.findPermanent("Centaur Courser")!!
            val seeker = game.findPermanent("Glory Seeker")!!
            val giant = game.findPermanent("Hill Giant")!!
            listOf(courser, seeker, giant).forEach { id ->
                game.state = game.state.updateEntity(id) { it.with(FaceDownComponent) }
            }

            // The face-down Courser and the Bears attack; the face-down Seeker stays home.
            game.declareAttackers(mapOf("Centaur Courser" to 1, "Grizzly Bears" to 1)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Hill Giant" to listOf("Grizzly Bears"))).error shouldBe null

            // The attacking player passes; the defender casts Revealing Wind.
            game.execute(PassPriority(game.player2Id))
            game.castSpell(1, "Revealing Wind").error shouldBe null
            game.resolveStack()

            fun revealedToCaster(id: com.wingedsheep.sdk.model.EntityId) =
                game.state.getEntity(id)?.get<RevealedToComponent>()?.isRevealedTo(game.player1Id) == true
            fun revealedToOpponent(id: com.wingedsheep.sdk.model.EntityId) =
                game.state.getEntity(id)?.get<RevealedToComponent>()?.isRevealedTo(game.player2Id) == true

            withClue("the face-down attacker is revealed to the caster") { revealedToCaster(courser) shouldBe true }
            withClue("the face-down creature outside combat stays hidden") { revealedToCaster(seeker) shouldBe false }
            withClue("the look is private: the opponent learns nothing about the caster's face-down blocker") {
                revealedToOpponent(giant) shouldBe false
            }

            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            withClue("the unblocked face-down Courser's combat damage is prevented") {
                game.getLifeTotal(1) shouldBe 20
            }
            withClue("the blocked Bears and the face-down blocker deal each other no damage") {
                game.isOnBattlefield("Grizzly Bears") shouldBe true
                game.isOnBattlefield("Hill Giant") shouldBe true
            }
        }
    }
}
