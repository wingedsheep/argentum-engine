package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Karametra's Favor (BNG #125) — "When this Aura enters, draw a card. Enchanted creature has
 * '{T}: Add one mana of any color.'"
 */
class KarametrasFavorScenarioTest : ScenarioTestBase() {
    init {
        val grantedId = cardRegistry.getCard("Karametra's Favor")!!.staticAbilities
            .filterIsInstance<GrantActivatedAbility>().single().ability.id

        test("entering draws a card, and the enchanted creature taps for mana of any color") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Karametra's Favor")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Karametra's Favor", bears).error shouldBe null
            game.resolveStack()
            withClue("the Aura's entry drew a card") { game.isInHand(1, "Island") shouldBe true }

            val result = game.execute(ActivateAbility(game.player1Id, bears, grantedId, manaColorChoice = Color.BLUE))
            withClue("activation should succeed: ${result.error}") { result.error shouldBe null }

            game.state.getEntity(bears)!!.has<TappedComponent>() shouldBe true
            game.state.getEntity(game.player1Id)!!.get<ManaPoolComponent>()!!.getAmount(Color.BLUE) shouldBe 1
        }
    }
}
