package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Nezumi Freewheeler // Hideous Fleshwheeler (MOM #119).
 *
 *   Front — Menace. "When this creature enters, each player mills three cards."
 *           "{5}{W/P}: Transform this creature. Activate only as a sorcery."
 *   Back  — Menace. "When this creature transforms into Hideous Fleshwheeler, put target permanent
 *           card with mana value 2 or less from a graveyard onto the battlefield under your control."
 */
class NezumiFreewheelerScenarioTest : ScenarioTestBase() {

    private val transformAbility
        get() = cardRegistry.getCard("Nezumi Freewheeler")!!.activatedAbilities[0].id

    init {
        test("entering makes each player mill three cards") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Nezumi Freewheeler")
                .withLandsOnBattlefield(1, "Swamp", 4)
                .apply { repeat(5) { withCardInLibrary(1, "Island") } }
                .apply { repeat(5) { withCardInLibrary(2, "Swamp") } }
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Nezumi Freewheeler").error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Nezumi Freewheeler") shouldBe true
            game.graveyardSize(1) shouldBe 3
            game.graveyardSize(2) shouldBe 3
            game.librarySize(1) shouldBe 2
            game.librarySize(2) shouldBe 2
        }

        test("transforming reanimates a cheap permanent card from an opponent's graveyard under your control") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Nezumi Freewheeler", summoningSickness = false)
                .withLandsOnBattlefield(1, "Swamp", 5)
                .withLandsOnBattlefield(1, "Plains", 1)
                .withCardInGraveyard(2, "Grizzly Bears")
                .withCardInGraveyard(2, "Hill Giant")
                .withCardInGraveyard(2, "Lightning Bolt")
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val nezumi = game.findPermanent("Nezumi Freewheeler")!!
            game.execute(
                ActivateAbility(playerId = game.player1Id, sourceId = nezumi, abilityId = transformAbility)
            ).error shouldBe null
            if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
            game.resolveStack()

            game.state.getEntity(nezumi)!!.get<CardComponent>()!!.name shouldBe "Hideous Fleshwheeler"

            var guard = 0
            while (game.state.pendingDecision !is ChooseTargetsDecision && guard++ < 10) game.resolveStack()
            val td = game.state.pendingDecision as ChooseTargetsDecision
            val graveyard = game.state.getGraveyard(game.player2Id)
            fun named(n: String) = graveyard.single {
                game.state.getEntity(it)?.get<CardComponent>()?.name == n
            }
            val bears = named("Grizzly Bears")
            val legal = td.legalTargets[0].orEmpty()
            withClue("only permanent cards with mana value 2 or less are legal") {
                legal shouldContain bears
                legal shouldNotContain named("Hill Giant")
                legal shouldNotContain named("Lightning Bolt")
            }
            game.submitDecision(TargetsResponse(td.id, mapOf(0 to listOf(bears)))).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Grizzly Bears") shouldBe true
            game.state.getEntity(bears)?.get<ControllerComponent>()?.playerId shouldBe game.player1Id
        }
    }
}
