package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Gorex, the Tombshell — "As an additional cost to cast this spell, you may exile any number of
 * creature cards from your graveyard. This spell costs {2} less to cast for each card exiled this
 * way. Deathtouch. Whenever Gorex attacks or dies, choose a card at random exiled with Gorex and put
 * that card into its owner's hand."
 *
 * Pinned: the per-card discount (generic only, never below {B}{B}, extra picks allowed per the
 * rulings), the creature-card filter, the zero-floor graveyard picker, and that the attack and dies
 * triggers return one of exactly the cards the cost exiled.
 */
class GorexTheTombshellScenarioTest : ScenarioTestBase() {

    private val graveyardCreatures = listOf("Grizzly Bears", "Hill Giant", "Raging Goblin")

    private fun gorexGame(
        swamps: Int,
        extraGraveyard: List<String> = emptyList(),
        extraHand: List<String> = emptyList(),
    ): TestGame {
        val builder = scenario()
            .withPlayers()
            .withCardInHand(1, "Gorex, the Tombshell")
            .withLandsOnBattlefield(1, "Swamp", swamps)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        (graveyardCreatures + extraGraveyard).forEach { builder.withCardInGraveyard(1, it) }
        extraHand.forEach { builder.withCardInHand(1, it) }
        return builder.build()
    }

    private fun TestGame.gorexInHand(): EntityId = state.getHand(player1Id).first { name(it) == "Gorex, the Tombshell" }

    private fun TestGame.graveyardCards(vararg names: String): List<EntityId> =
        names.map { wanted -> state.getGraveyard(player1Id).first { name(it) == wanted } }

    private fun TestGame.name(id: EntityId): String? = state.getEntity(id)?.get<CardComponent>()?.name

    private fun TestGame.castGorex(exiled: List<EntityId>): ExecutionResult =
        execute(CastSpell(player1Id, gorexInHand(), additionalCostPayment = AdditionalCostPayment(exiledCards = exiled)))

    init {
        test("each creature card exiled takes {2} off: three cards make it {B}{B}") {
            val game = gorexGame(swamps = 2)
            val exiled = game.graveyardCards(*graveyardCreatures.toTypedArray())

            game.castGorex(exiled).error shouldBe null
            game.resolveStack()

            game.findPermanent("Gorex, the Tombshell") shouldNotBe null
            game.state.getExile(game.player1Id) shouldContainExactlyInAnyOrder exiled
        }

        test("two cards leave {2}{B}{B} owed — two Swamps can't pay it") {
            val game = gorexGame(swamps = 2)
            val exiled = game.graveyardCards("Grizzly Bears", "Hill Giant")

            game.castGorex(exiled).error shouldNotBe null
            game.state.getExile(game.player1Id) shouldHaveSize 0
        }

        test("exiling more cards than the discount can use is legal; the {B}{B} is always owed") {
            val game = gorexGame(swamps = 2, extraGraveyard = listOf("Grizzly Bears", "Hill Giant"))
            val everyCreature = game.state.getGraveyard(game.player1Id)

            game.castGorex(everyCreature).error shouldBe null
            game.resolveStack()
            game.state.getExile(game.player1Id) shouldHaveSize 5
        }

        test("a noncreature card can't be exiled for the discount") {
            val game = gorexGame(swamps = 8, extraGraveyard = listOf("Giant Growth"))
            val growth = game.graveyardCards("Giant Growth")

            game.castGorex(growth).error shouldNotBe null
        }

        test("choosing none pays full price") {
            val game = gorexGame(swamps = 8)

            game.castGorex(emptyList()).error shouldBe null
            game.resolveStack()
            game.state.getExile(game.player1Id) shouldHaveSize 0
            game.state.getGraveyard(game.player1Id) shouldHaveSize 3
        }

        test("the cast is offered with a zero-floor graveyard picker of creature cards only") {
            val game = gorexGame(swamps = 2, extraGraveyard = listOf("Giant Growth"))
            val gorex = game.gorexInHand()

            val cast = game.getLegalActions(1).firstOrNull { (it.action as? CastSpell)?.cardId == gorex }
            withClue("three creature cards to exile bring {6}{B}{B} within two Swamps") {
                cast.shouldNotBeNull()
                cast.isAffordable shouldBe true
            }
            val info = cast!!.additionalCostInfo.shouldNotBeNull()
            info.costType shouldBe "ExileFromGraveyard"
            info.exileMinCount shouldBe 0
            info.validExileTargets shouldContainExactlyInAnyOrder game.graveyardCards(*graveyardCreatures.toTypedArray())
        }

        test("attacking returns one card exiled with Gorex to its owner's hand at random") {
            val game = gorexGame(swamps = 2, extraGraveyard = listOf("Giant Growth"))
            val exiled = game.graveyardCards(*graveyardCreatures.toTypedArray())
            game.castGorex(exiled).error shouldBe null
            game.resolveStack()
            val gorex = game.findPermanent("Gorex, the Tombshell")!!
            game.state = game.state.updateEntity(gorex) { it.without<SummoningSicknessComponent>() }

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Gorex, the Tombshell" to 2)).error shouldBe null
            game.resolveStack()

            val returned = game.state.getHand(game.player1Id).filter { it in exiled }
            returned shouldHaveSize 1
            game.state.getExile(game.player1Id) shouldContainExactlyInAnyOrder (exiled - returned.toSet())
            withClue("a graveyard card the cost didn't exile is never 'exiled with Gorex'") {
                game.isInGraveyard(1, "Giant Growth") shouldBe true
            }
        }

        test("dying returns one card exiled with Gorex, read from the departed Gorex") {
            val game = gorexGame(swamps = 5, extraHand = listOf("Murder"))
            val exiled = game.graveyardCards(*graveyardCreatures.toTypedArray())
            game.castGorex(exiled).error shouldBe null
            game.resolveStack()
            val gorex = game.findPermanent("Gorex, the Tombshell")!!

            game.castSpell(1, "Murder", gorex).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Gorex, the Tombshell") shouldBe true
            val returned = game.state.getHand(game.player1Id).filter { it in exiled }
            returned shouldHaveSize 1
            game.state.getExile(game.player1Id) shouldHaveSize 2
        }
    }
}
