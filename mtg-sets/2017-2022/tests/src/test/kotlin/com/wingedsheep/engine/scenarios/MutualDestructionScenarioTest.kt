package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Mutual Destruction (IKO #96) — {B} Sorcery.
 *
 * "This spell has flash as long as you control a permanent with flash. As an additional cost to
 *  cast this spell, sacrifice a creature. Destroy target creature."
 */
class MutualDestructionScenarioTest : ScenarioTestBase() {

    private fun TestGame.cast(sacrifice: EntityId, target: EntityId) = execute(
        CastSpell(
            playerId = player1Id,
            cardId = findCardsInHand(1, "Mutual Destruction").first(),
            targets = listOf(ChosenTarget.Permanent(target)),
            additionalCostPayment = AdditionalCostPayment(sacrificedPermanents = listOf(sacrifice))
        )
    )

    private fun TestGame.canCast(): Boolean {
        val spell = findCardsInHand(1, "Mutual Destruction").first()
        return getLegalActions(1).any { (it.action as? CastSpell)?.cardId == spell }
    }

    init {
        test("sacrifices a creature as a cost and destroys the target creature") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Mutual Destruction")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withLandsOnBattlefield(1, "Swamp", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!

            game.cast(bears, giant).error shouldBe null
            withClue("the sacrifice is paid on casting") {
                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            }
            game.resolveStack()

            withClue("Hill Giant is destroyed") {
                game.isOnBattlefield("Hill Giant") shouldBe false
                game.isInGraveyard(2, "Hill Giant") shouldBe true
            }
        }

        test("can't be cast on an opponent's turn without a permanent with flash") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Mutual Destruction")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withLandsOnBattlefield(1, "Swamp", 1)
                .withActivePlayer(2)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            withClue("no permanent with flash, so it keeps sorcery timing") {
                game.canCast() shouldBe false
                game.cast(game.findPermanent("Grizzly Bears")!!, game.findPermanent("Hill Giant")!!).error shouldNotBe null
            }
        }

        test("has flash while you control a permanent with flash") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Mutual Destruction")
                .withCardOnBattlefield(1, "Ambush Viper")       // Flash, Deathtouch
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withLandsOnBattlefield(1, "Swamp", 1)
                .withActivePlayer(2)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            withClue("Ambush Viper has flash, so Mutual Destruction is castable at instant speed") {
                game.canCast() shouldBe true
            }
            game.cast(game.findPermanent("Grizzly Bears")!!, game.findPermanent("Hill Giant")!!).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            game.isInGraveyard(2, "Hill Giant") shouldBe true
            game.isOnBattlefield("Ambush Viper") shouldBe true
        }
    }
}
