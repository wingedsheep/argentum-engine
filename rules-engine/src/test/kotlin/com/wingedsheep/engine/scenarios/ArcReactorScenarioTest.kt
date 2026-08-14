package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.engine.view.LegalActionInfo
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.AlternativePaymentChoice
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Arc Reactor — {5} Artifact.
 *
 * Improvise (CR 702.126)
 * This artifact enters tapped.
 * {T}: Add {C}{C}{C}.
 *
 * What this pins:
 *  1. The whole {5} is generic, so five untapped artifacts cast it for no mana at all.
 *  2. It enters tapped, so it cannot immediately be tapped for mana or for another improvise.
 *  3. Its mana ability adds three colorless.
 */
class ArcReactorScenarioTest : ScenarioTestBase() {

    init {
        val scrap = card("Reactor Scrap") {
            manaCost = "{1}"
            colorIdentity = ""
            typeLine = "Artifact"
            oracleText = ""
        }
        cardRegistry.register(scrap)

        fun castAction(game: TestGame, name: String): LegalActionInfo? =
            game.getLegalActions(1).firstOrNull {
                it.actionType == "CastSpell" && it.action is CastSpell && it.description.contains(name)
            }

        test("five untapped artifacts improvise the whole {5} — no mana needed") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Arc Reactor")
                .withCardOnBattlefield(1, "Reactor Scrap")
                .withCardOnBattlefield(1, "Reactor Scrap")
                .withCardOnBattlefield(1, "Reactor Scrap")
                .withCardOnBattlefield(1, "Reactor Scrap")
                .withCardOnBattlefield(1, "Reactor Scrap")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val scraps = game.findAllPermanents("Reactor Scrap")
            scraps.size shouldBe 5

            val action = castAction(game, "Arc Reactor")
            withClue("an all-generic cost is fully improvisable, so this is castable with no lands") {
                action shouldNotBe null
                action!!.isAffordable shouldBe true
                action.hasTapForGeneric shouldBe true
                action.tapForGenericLabel shouldBe "improvise"
            }

            val cast = (action!!.action as CastSpell).copy(
                alternativePayment = AlternativePaymentChoice(tapForGenericPermanents = scraps.toSet())
            )
            val result = game.execute(cast)
            withClue("five artifacts should pay the whole {5}: ${result.error}") {
                result.error shouldBe null
            }
            game.resolveStack()
            game.isOnBattlefield("Arc Reactor") shouldBe true
            scraps.all { game.state.getEntity(it)!!.has<TappedComponent>() } shouldBe true
        }

        test("it enters tapped") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Arc Reactor")
                .withLandsOnBattlefield(1, "Island", 5)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val action = castAction(game, "Arc Reactor")!!
            game.execute(action.action).error shouldBe null
            game.resolveStack()

            val reactor = game.findPermanent("Arc Reactor")!!
            withClue("\"This artifact enters tapped\"") {
                game.state.getEntity(reactor)!!.has<TappedComponent>() shouldBe true
            }
        }

        test("{T}: Add {C}{C}{C}") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Arc Reactor")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val reactor = game.findPermanent("Arc Reactor")!!
            val abilityId = cardRegistry.getCard("Arc Reactor")!!.script.activatedAbilities.first().id
            val result = game.execute(ActivateAbility(game.player1Id, reactor, abilityId))
            withClue("the mana ability should resolve immediately: ${result.error}") {
                result.error shouldBe null
            }
            game.state.getEntity(game.player1Id)!!.get<ManaPoolComponent>()!!.colorless shouldBe 3
            game.state.getEntity(reactor)!!.has<TappedComponent>() shouldBe true
        }
    }
}
