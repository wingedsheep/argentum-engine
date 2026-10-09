package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.AbilityId
import io.kotest.matchers.shouldBe

class MishraTamerOfMakFawaScenarioTest : ScenarioTestBase() {
    private val mishra = "Mishra, Tamer of Mak Fawa"
    private val grantedUnearth = AbilityId("unearth_{1}{B}{R}")

    private fun board(artifact: String = "Ornithopter") = scenario().withPlayers("A", "B")
        .withCardOnBattlefield(1, mishra)
        .withCardInGraveyard(1, artifact)
        .withCardInHand(1, "Unsummon")
        .withCardInHand(1, "Cloudshift")
        .withLandsOnBattlefield(1, "Swamp", 1)
        .withLandsOnBattlefield(1, "Mountain", 1)
        .withLandsOnBattlefield(1, "Island", 2)
        .withLandsOnBattlefield(1, "Plains", 1)
        .withCardInLibrary(1, "Swamp").withCardInLibrary(2, "Island")
        .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()

    private fun TestGame.unearthAction(artifact: String = "Ornithopter") = getLegalActions(1)
        .mapNotNull { it.action as? ActivateAbility }
        .single { it.sourceId == findCardsInGraveyard(1, artifact).single() && it.abilityId == grantedUnearth }

    init {
        test("a graveyard artifact gains its own unearth action with haste and end-step exile") {
            val game = board()
            val action = game.unearthAction()
            game.execute(action).error shouldBe null
            game.resolveStack()
            val artifact = game.findPermanent("Ornithopter")!!
            artifact shouldBe action.sourceId
            game.state.projectedState.hasKeyword(artifact, Keyword.HASTE) shouldBe true
            game.state.projectedState.hasKeyword(game.findPermanent(mishra)!!, Keyword.HASTE) shouldBe false
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()
            game.isInExile(1, "Ornithopter") shouldBe true
            game.isOnBattlefield(mishra) shouldBe true
        }
        test("noncreature artifacts also unearth and are exiled at end step") {
            val game = board("Sol Ring")
            game.execute(game.unearthAction("Sol Ring")).error shouldBe null
            game.resolveStack()
            game.isOnBattlefield("Sol Ring") shouldBe true
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()
            game.isInExile(1, "Sol Ring") shouldBe true
        }
        test("bounce after unearth exiles the artifact") {
            val game = board()
            game.execute(game.unearthAction()).error shouldBe null
            game.resolveStack()
            game.castSpell(1, "Unsummon", game.findPermanent("Ornithopter")!!).error shouldBe null
            game.resolveStack()
            game.isInExile(1, "Ornithopter") shouldBe true
            game.isInHand(1, "Ornithopter") shouldBe false
        }
        test("blinking an unearthed artifact makes a new object that the old delayed exile ignores") {
            val game = board()
            game.execute(game.unearthAction()).error shouldBe null
            game.resolveStack()
            game.castSpell(1, "Cloudshift", game.findPermanent("Ornithopter")!!).error shouldBe null
            game.resolveStack()
            game.state.projectedState.hasKeyword(game.findPermanent("Ornithopter")!!, Keyword.HASTE) shouldBe false
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()
            game.isOnBattlefield("Ornithopter") shouldBe true
        }
        test("removing Mishra in response does not stop the already activated unearth") {
            val game = board()
            game.execute(game.unearthAction()).error shouldBe null
            game.castSpell(1, "Unsummon", game.findPermanent(mishra)!!).error shouldBe null
            game.resolveStack()
            game.isInHand(1, mishra) shouldBe true
            game.isOnBattlefield("Ornithopter") shouldBe true
        }
        test("removing the graveyard card in response makes unearth do nothing") {
            val game = board()
            val action = game.unearthAction()
            game.execute(action).error shouldBe null
            val oldZone = game.state.logicalZone(action.sourceId)!!
            game.state = game.state.moveToZone(action.sourceId, oldZone, oldZone.copy(zoneType = Zone.EXILE))
            game.resolveStack()
            game.isInExile(1, "Ornithopter") shouldBe true
            game.isOnBattlefield("Ornithopter") shouldBe false
        }
        test("printed unearth coexists with Mishra's different cost") {
            val game = board("Yotian Frontliner")
            val ids = game.getLegalActions(1).mapNotNull { it.action as? ActivateAbility }
                .filter { it.sourceId == game.findCardsInGraveyard(1, "Yotian Frontliner").single() }
                .map { it.abilityId }.toSet()
            ids shouldBe setOf(AbilityId("unearth_{W}"), grantedUnearth)
        }
        test("Mishra grants ward to itself and a noncreature permanent but not opposing permanents") {
            val game = scenario().withPlayers("A", "B")
                .withCardOnBattlefield(1, mishra)
                .withLandsOnBattlefield(1, "Island", 1)
                .withLandsOnBattlefield(2, "Island", 1)
                .withCardInHand(2, "Unsummon")
                .withActivePlayer(2).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            game.state.projectedState.hasKeyword(game.findPermanent(mishra)!!, Keyword.WARD) shouldBe true
            val islands = game.findPermanents("Island")
            for (island in islands) {
                game.state.projectedState.hasKeyword(island, Keyword.WARD) shouldBe
                    (game.state.projectedState.getController(island) == game.player1Id)
            }
            game.castSpell(2, "Unsummon", game.findPermanent(mishra)!!).error shouldBe null
            game.resolveStack()
            game.hasPendingDecision() shouldBe true
            // Decline to sacrifice: ward counters the opponent's bounce spell.
            game.skipSelection().error shouldBe null
            game.resolveStack()
            game.isOnBattlefield(mishra) shouldBe true
            game.isInGraveyard(2, "Unsummon") shouldBe true
        }
    }
}
