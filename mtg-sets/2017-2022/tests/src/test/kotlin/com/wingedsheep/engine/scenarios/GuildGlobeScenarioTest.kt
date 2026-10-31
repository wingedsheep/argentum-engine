package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Guild Globe (WAR #239) — {2} Artifact.
 *
 *   When this artifact enters, draw a card.
 *   {2}, {T}, Sacrifice this artifact: Add two mana of different colors.
 */
class GuildGlobeScenarioTest : ScenarioTestBase() {

    init {
        test("entering draws a card") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Guild Globe")
                .withCardInLibrary(1, "Plains")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Guild Globe").error shouldBe null
            game.resolveStack() // the Globe
            game.resolveStack() // its enters trigger

            game.isInHand(1, "Plains") shouldBe true
        }

        test("sacrificing it adds two mana of different colors") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Guild Globe")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val globe = game.findPermanent("Guild Globe").shouldNotBeNull()
            val abilityId = cardRegistry.getCard("Guild Globe")!!.activatedAbilities.single().id
            game.execute(ActivateAbility(playerId = game.player1Id, sourceId = globe, abilityId = abilityId))
                .error shouldBe null
            game.isInGraveyard(1, "Guild Globe") shouldBe true

            val first = game.getPendingDecision().shouldBeInstanceOf<ChooseColorDecision>()
            game.submitDecision(ColorChosenResponse(first.id, Color.RED)).error shouldBe null
            val second = game.getPendingDecision().shouldBeInstanceOf<ChooseColorDecision>()
            second.availableColors shouldBe Color.entries.toSet() - Color.RED
            game.submitDecision(ColorChosenResponse(second.id, Color.GREEN)).error shouldBe null

            val pool = game.state.getEntity(game.player1Id)!!.get<ManaPoolComponent>()!!
            pool.red shouldBe 1
            pool.green shouldBe 1
            pool.white shouldBe 0
        }
    }
}
