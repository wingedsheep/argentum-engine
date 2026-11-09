package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Daring Sleuth // Bearer of Overwhelming Truths (SOI #54).
 *
 *   Front: When you sacrifice a Clue, transform this creature.
 *   Back:  Prowess. Whenever this creature deals combat damage to a player, investigate.
 *
 * The interaction worth proving is the 2016-04-08 ruling: with several of the front face's triggers
 * on the stack at once, only the first to resolve transforms it (CR 701.28f) — the rest must not flip
 * it back.
 */
class DaringSleuthScenarioTest : ScenarioTestBase() {

    private val clueAbilityId get() = cardRegistry.getCard("Clue")!!.activatedAbilities.single().id

    private fun sleuthBoard(clues: Int) = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Daring Sleuth", summoningSickness = false)
        .apply { repeat(clues) { withCardOnBattlefield(1, "Clue", isToken = true) } }
        .withLandsOnBattlefield(1, "Island", 2 * clues)
        .apply { repeat(clues + 2) { withCardInLibrary(1, "Island") } }
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.faceName(entity: EntityId) =
        state.getEntity(entity)!!.get<CardComponent>()!!.name

    init {
        test("sacrificing a Clue transforms it into Bearer of Overwhelming Truths") {
            val game = sleuthBoard(clues = 1)
            val sleuth = game.findPermanent("Daring Sleuth")!!
            val clue = game.findPermanents("Clue").single()

            game.execute(ActivateAbility(playerId = game.player1Id, sourceId = clue, abilityId = clueAbilityId))
                .error shouldBe null
            game.resolveStack()

            game.faceName(sleuth) shouldBe "Bearer of Overwhelming Truths"
            game.state.projectedState.getPower(sleuth) shouldBe 3
            game.state.projectedState.getToughness(sleuth) shouldBe 2
        }

        test("two Clue triggers on the stack transform it only once") {
            val game = sleuthBoard(clues = 2)
            val sleuth = game.findPermanent("Daring Sleuth")!!
            val (first, second) = game.findPermanents("Clue")

            // Crack both Clues before anything resolves, so both transform triggers wait on the stack.
            game.execute(ActivateAbility(playerId = game.player1Id, sourceId = first, abilityId = clueAbilityId))
                .error shouldBe null
            game.execute(ActivateAbility(playerId = game.player1Id, sourceId = second, abilityId = clueAbilityId))
                .error shouldBe null
            game.resolveStack()

            withClue("the second trigger to resolve finds it already transformed and does nothing") {
                game.faceName(sleuth) shouldBe "Bearer of Overwhelming Truths"
            }
        }

        test("the back face investigates when it deals combat damage to a player") {
            val game = sleuthBoard(clues = 1)
            val sleuth = game.findPermanent("Daring Sleuth")!!
            val clue = game.findPermanents("Clue").single()
            game.execute(ActivateAbility(playerId = game.player1Id, sourceId = clue, abilityId = clueAbilityId))
                .error shouldBe null
            game.resolveStack()
            game.findPermanents("Clue") shouldHaveSize 0

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Bearer of Overwhelming Truths" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)

            game.getLifeTotal(2) shouldBe 17
            game.findPermanents("Clue") shouldHaveSize 1
            game.faceName(sleuth) shouldBe "Bearer of Overwhelming Truths"
        }
    }
}
