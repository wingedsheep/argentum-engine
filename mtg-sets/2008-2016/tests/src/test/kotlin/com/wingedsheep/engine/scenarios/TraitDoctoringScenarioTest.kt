package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseReplacementDecision
import com.wingedsheep.engine.core.ReplacementChosenResponse
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Trait Doctoring (DGM #18) — {U} Sorcery.
 *
 *   Change the text of target permanent by replacing all instances of one color word with another
 *   or one basic land type with another until end of turn.
 *   Cipher
 *
 * The text change lands on the target permanent, expires at end of turn, and the card is then
 * encoded on a creature its caster controls.
 */
class TraitDoctoringScenarioTest : ScenarioTestBase() {
    init {
        fun board(): TestGame = scenario()
            .withPlayers("Caster", "Opponent")
            .withCardInHand(1, "Trait Doctoring")
            .withLandsOnBattlefield(1, "Island", 1)
            .withCardOnBattlefield(1, "Hill Giant")
            .withLandsOnBattlefield(2, "Forest", 1)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        fun TestGame.cast(target: EntityId) = execute(
            CastSpell(
                player1Id,
                state.getHand(player1Id).first { state.getEntity(it)?.get<CardComponent>()?.name == "Trait Doctoring" },
                listOf(ChosenTarget.Permanent(target)),
            )
        )

        fun TestGame.replace(from: String, to: String) {
            val decision = getPendingDecision().shouldBeInstanceOf<ChooseReplacementDecision>()
            submitDecision(
                ReplacementChosenResponse(decision.id, decision.fromOptions.indexOf(from), decision.toOptions.indexOf(to))
            ).error shouldBe null
        }

        test("turns the opponent's Forest into an Island, then encodes on a creature") {
            val game = board()
            val forest = game.findPermanent("Forest")!!
            val giant = game.findPermanent("Hill Giant")!!

            game.cast(forest).error shouldBe null
            game.resolveStack()
            game.replace("Forest", "Island")

            withClue("the Forest's basic land type is now Island") {
                game.state.projectedState.getSubtypes(forest) shouldBe setOf("Island")
            }

            game.selectCards(listOf(giant)).error shouldBe null
            game.isInExile(1, "Trait Doctoring") shouldBe true
        }

        test("the text change ends at end of turn") {
            val game = board()
            val forest = game.findPermanent("Forest")!!
            val giant = game.findPermanent("Hill Giant")!!

            game.cast(forest).error shouldBe null
            game.resolveStack()
            game.replace("Forest", "Island")
            game.selectCards(listOf(giant)).error shouldBe null

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            withClue("cleanup strips the until-end-of-turn change") {
                game.state.projectedState.getSubtypes(forest) shouldBe setOf("Forest")
            }
        }
    }
}
