package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.AlternativePaymentChoice
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Battle at the Bridge — {X}{B} Sorcery. Improvise. Target creature gets -X/-X until end of turn.
 * You gain X life.
 *
 * Pins the cost-linked amount: artifacts tapped for improvise pay part of the announced X, and the
 * resolved effect still reads the announced X, not the mana actually spent from lands.
 */
class BattleAtTheBridgeScenarioTest : ScenarioTestBase() {

    init {
        val scrap = card("Bridge Scrap") {
            manaCost = "{1}"
            colorIdentity = ""
            typeLine = "Artifact"
            oracleText = ""
        }
        cardRegistry.register(scrap)

        test("X=3 paid partly by two improvised artifacts gives -3/-3 and 3 life") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Battle at the Bridge")
                .withCardOnBattlefield(1, "Bridge Scrap")
                .withCardOnBattlefield(1, "Bridge Scrap")
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(2, "Swamp")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val action = game.getLegalActions(1).firstOrNull {
                it.actionType == "CastSpell" && it.action is CastSpell && it.description.contains("Battle at the Bridge")
            }
            withClue("two Swamps plus two artifacts reach X=3") {
                action shouldNotBe null
                action!!.hasTapForGeneric shouldBe true
                action.maxAffordableX shouldBe 3
            }

            val giant = game.findPermanent("Hill Giant")!!
            val scraps = game.findAllPermanents("Bridge Scrap")
            val before = game.getLifeTotal(1)
            val cast = (action!!.action as CastSpell).copy(
                targets = listOf(ChosenTarget.Permanent(giant)),
                xValue = 3,
                alternativePayment = AlternativePaymentChoice(tapForGenericPermanents = scraps.toSet())
            )
            game.execute(cast).error shouldBe null
            game.resolveStack()

            scraps.all { game.state.getEntity(it)!!.has<TappedComponent>() } shouldBe true
            withClue("the 3/3 gets -3/-3 and dies") {
                game.findPermanent("Hill Giant") shouldBe null
                game.isInGraveyard(2, "Hill Giant") shouldBe true
            }
            withClue("life gained is the announced X") { game.getLifeTotal(1) shouldBe before + 3 }
        }
    }
}
