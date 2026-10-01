package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Red Sun's Twilight ({X}{R}{R}, Sorcery):
 * "Destroy up to X target artifacts. If X is 5 or more, for each artifact destroyed this way,
 *  create a token that's a copy of it. Those tokens gain haste. Exile them at the beginning of
 *  the next end step."
 */
class RedSunsTwilightScenarioTest : ScenarioTestBase() {

    private fun TestGame.castTwilight(targets: List<EntityId>, xValue: Int): ExecutionResult {
        val cardId = state.getHand(player1Id).find {
            state.getEntity(it)?.get<CardComponent>()?.name == "Red Sun's Twilight"
        } ?: error("Red Sun's Twilight not in hand")
        return execute(CastSpell(player1Id, cardId, targets.map { ChosenTarget.Permanent(it) }, xValue))
    }

    init {
        test("X=2 destroys two target artifacts and creates no tokens") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Red Sun's Twilight")
                .withLandsOnBattlefield(1, "Mountain", 4)
                .withCardOnBattlefield(2, "Ornithopter")
                .withCardOnBattlefield(2, "Millstone")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val thopter = game.findPermanent("Ornithopter")!!
            val millstone = game.findPermanent("Millstone")!!
            game.castTwilight(listOf(thopter, millstone), 2).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Ornithopter") shouldBe true
            game.isInGraveyard(2, "Millstone") shouldBe true
            withClue("X < 5: no token copies") {
                game.findPermanent("Ornithopter") shouldBe null
                game.findPermanent("Millstone") shouldBe null
            }
        }

        test("X=1 cannot target more than one artifact") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Red Sun's Twilight")
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withCardOnBattlefield(2, "Ornithopter")
                .withCardOnBattlefield(2, "Millstone")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val thopter = game.findPermanent("Ornithopter")!!
            val millstone = game.findPermanent("Millstone")!!
            (game.castTwilight(listOf(thopter, millstone), 1).error != null) shouldBe true
        }

        test("X=5 copies each artifact actually destroyed, with haste, exiled at the next end step") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Red Sun's Twilight")
                .withLandsOnBattlefield(1, "Mountain", 7)
                .withCardOnBattlefield(2, "Ornithopter")
                .withCardOnBattlefield(2, "Darksteel Myr") // indestructible — not destroyed
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val thopter = game.findPermanent("Ornithopter")!!
            val myr = game.findPermanent("Darksteel Myr")!!
            game.castTwilight(listOf(thopter, myr), 5).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Ornithopter") shouldBe true
            val token = game.findPermanent("Ornithopter").shouldNotBeNull()
            game.state.getEntity(token)!!.get<ControllerComponent>()!!.playerId shouldBe game.player1Id
            game.state.projectedState.hasKeyword(token, Keyword.HASTE) shouldBe true

            withClue("the indestructible artifact survives and is not copied") {
                game.findPermanent("Darksteel Myr") shouldBe myr
                game.state.getBattlefield().count {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Darksteel Myr"
                } shouldBe 1
            }

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()
            withClue("the token is exiled at the beginning of the next end step") {
                game.findPermanent("Ornithopter") shouldBe null
            }
        }
    }
}
