package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Seer's Lantern (OGW) — {3} Artifact.
 * "{T}: Add {C}. / {2}, {T}: Scry 1."
 */
class SeersLanternScenarioTest : ScenarioTestBase() {
    init {
        val abilities = cardRegistry.getCard("Seer's Lantern")!!.script.activatedAbilities
        val manaId = abilities[0].id
        val scryId = abilities[1].id

        fun board() = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Seer's Lantern")
            .withLandsOnBattlefield(1, "Plains", 2)
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(1, "Forest")
            .withCardInLibrary(2, "Plains")
            .withActivePlayer(1)
            .withPriorityPlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("{T}: adds {C} as a mana ability") {
            val game = board()
            val lantern = game.findPermanent("Seer's Lantern")!!

            game.execute(ActivateAbility(game.player1Id, lantern, manaId)).error shouldBe null
            game.state.stack.size shouldBe 0
            game.state.getEntity(game.player1Id)!!.get<ManaPoolComponent>()!!.colorless shouldBe 1
        }

        test("{2}, {T}: scry 1 can put the top card on the bottom") {
            val game = board()
            val lantern = game.findPermanent("Seer's Lantern")!!
            val island = game.findCardsInLibrary(1, "Island").single()
            val forest = game.findCardsInLibrary(1, "Forest").single()

            val result = game.execute(ActivateAbility(game.player1Id, lantern, scryId))
            withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            val scry = game.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
            scry.options shouldBe listOf(island)
            game.selectCards(listOf(island)).error shouldBe null

            game.state.getEntity(lantern)?.has<TappedComponent>() shouldBe true
            game.state.getLibrary(game.player1Id).first() shouldBe forest
            game.state.getLibrary(game.player1Id).last() shouldBe island
        }
    }
}
