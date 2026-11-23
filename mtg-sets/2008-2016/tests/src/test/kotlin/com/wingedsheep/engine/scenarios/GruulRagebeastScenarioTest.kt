package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Gruul Ragebeast (GTC #170) — "Whenever this creature or another creature you control enters,
 * that creature fights target creature an opponent controls."
 *
 * Pins that the fighter is the *entering* creature, not the Ragebeast, and that the Ragebeast's
 * own entry triggers it too.
 */
class GruulRagebeastScenarioTest : ScenarioTestBase() {

    private fun TestGame.damageOn(id: EntityId): Int =
        state.getEntity(id)?.get<DamageComponent>()?.amount ?: 0

    private fun TestGame.aimTriggerAt(target: EntityId) {
        resolveStack()
        withClue("the enters trigger asks for a creature an opponent controls") {
            getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
        }
        selectTargets(listOf(target)).error shouldBe null
        resolveStack()
    }

    init {
        test("the Ragebeast entering fights the chosen opposing creature") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Gruul Ragebeast")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withLandsOnBattlefield(1, "Forest", 6)
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val giant = game.findPermanent("Hill Giant")!!

            game.castSpell(1, "Gruul Ragebeast").error shouldBe null
            game.aimTriggerAt(giant)

            val beast = game.findPermanent("Gruul Ragebeast")!!
            withClue("the Ragebeast deals 6 to the Hill Giant") {
                game.isInGraveyard(2, "Hill Giant") shouldBe true
            }
            withClue("the Hill Giant deals 3 back to the Ragebeast") { game.damageOn(beast) shouldBe 3 }
            withClue("the untargeted Bears are untouched") { game.isOnBattlefield("Grizzly Bears") shouldBe true }
        }

        test("another creature entering is the one that fights, not the Ragebeast") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Gruul Ragebeast")
                .withCardInHand(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardOnBattlefield(2, "Savannah Lions")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val beast = game.findPermanent("Gruul Ragebeast")!!
            val giant = game.findPermanent("Hill Giant")!!

            game.castSpell(1, "Grizzly Bears").error shouldBe null
            game.aimTriggerAt(giant)

            withClue("the Bears fight the 3/3 Giant and die") { game.isInGraveyard(1, "Grizzly Bears") shouldBe true }
            withClue("the Giant took the Bears' 2, not the Ragebeast's 6") {
                game.isOnBattlefield("Hill Giant") shouldBe true
                game.damageOn(giant) shouldBe 2
            }
            withClue("the Ragebeast was not part of the fight") { game.damageOn(beast) shouldBe 0 }
        }
    }
}
