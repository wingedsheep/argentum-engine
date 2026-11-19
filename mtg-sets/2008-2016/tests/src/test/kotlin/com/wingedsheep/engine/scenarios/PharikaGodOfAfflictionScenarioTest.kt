package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.matchers.shouldBe

/**
 * Pharika, God of Affliction: the Snake goes to the exiled card's *owner*, not to Pharika's
 * controller — exiling an opponent's creature card gives that opponent the deathtouch token.
 */
class PharikaGodOfAfflictionScenarioTest : ScenarioTestBase() {
    init {
        val abilityId = cardRegistry.getCard("Pharika, God of Affliction")!!.activatedAbilities.single().id

        test("exiling an opponent's creature card gives that opponent a 1/1 black-green deathtouch Snake enchantment creature") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Pharika, God of Affliction")
                .withLandsOnBattlefield(1, "Swamp", 1)
                .withLandsOnBattlefield(1, "Forest", 1)
                .withCardInGraveyard(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val pharika = game.findPermanent("Pharika, God of Affliction")!!
            game.state.projectedState.isCreature(pharika) shouldBe false

            val bears = game.findCardsInGraveyard(2, "Grizzly Bears").single()
            game.execute(ActivateAbility(
                playerId = game.player1Id,
                sourceId = pharika,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Card(bears, game.player2Id, Zone.GRAVEYARD))
            )).error shouldBe null
            game.resolveStack()

            game.isInExile(2, "Grizzly Bears") shouldBe true
            val snake = game.findPermanent("Snake Token")!!
            val projected = game.state.projectedState
            projected.getController(snake) shouldBe game.player2Id
            projected.getPower(snake) shouldBe 1
            projected.getToughness(snake) shouldBe 1
            projected.hasKeyword(snake, Keyword.DEATHTOUCH) shouldBe true
            projected.hasSubtype(snake, "Snake") shouldBe true
            projected.hasType(snake, "ENCHANTMENT") shouldBe true
            projected.isCreature(snake) shouldBe true
            projected.getColors(snake) shouldBe setOf("BLACK", "GREEN")
        }
    }
}
