package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Spellkeeper Weird (WAR #69) — {2}{U} Creature — Weird 1/4.
 *
 *   {2}, {T}, Sacrifice this creature: Return target instant or sorcery card from your graveyard
 *   to your hand.
 */
class SpellkeeperWeirdScenarioTest : ScenarioTestBase() {

    private fun board() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Spellkeeper Weird")
        .withLandsOnBattlefield(1, "Island", 2)
        .withCardInGraveyard(1, "Lightning Bolt")
        .withCardInGraveyard(1, "Grizzly Bears")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.activate(target: EntityId) = execute(
        ActivateAbility(
            playerId = player1Id,
            sourceId = findPermanent("Spellkeeper Weird")!!,
            abilityId = cardRegistry.getCard("Spellkeeper Weird")!!.script.activatedAbilities.first().id,
            targets = listOf(ChosenTarget.Card(target, player1Id, Zone.GRAVEYARD)),
        )
    )

    init {
        test("sacrificing it returns an instant from your graveyard to your hand") {
            val game = board()
            val bolt = game.findCardsInGraveyard(1, "Lightning Bolt").single()

            game.activate(bolt).error shouldBe null
            game.resolveStack()

            game.isInHand(1, "Lightning Bolt") shouldBe true
            game.isInGraveyard(1, "Spellkeeper Weird") shouldBe true
        }

        test("a creature card in your graveyard is not a legal target") {
            val game = board()
            val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()

            game.activate(bears).error shouldNotBe null
            game.isOnBattlefield("Spellkeeper Weird") shouldBe true
        }
    }
}
