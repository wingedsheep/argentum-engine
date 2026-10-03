package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.mh3.cards.SnappingVoidcraw
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Snapping Voidcraw (MH3) — Devoid; {T}: Add {C}{C}; {3}{C}, {T}: Draw a card.
 */
class SnappingVoidcrawScenarioTest : ScenarioTestBase() {

    private val manaAbility = SnappingVoidcraw.activatedAbilities[0].id
    private val drawAbility = SnappingVoidcraw.activatedAbilities[1].id

    private fun TestGame.pool(): ManaPoolComponent =
        state.getEntity(player1Id)?.get<ManaPoolComponent>() ?: ManaPoolComponent()

    init {
        context("Snapping Voidcraw") {

            test("{T}: Add {C}{C}") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Snapping Voidcraw")
                    .withCardInLibrary(1, "Wastes")
                    .withCardInLibrary(2, "Wastes")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val craw = game.findPermanent("Snapping Voidcraw")!!
                game.execute(ActivateAbility(game.player1Id, craw, manaAbility)).error shouldBe null
                game.pool().colorless shouldBe 2
                game.state.getEntity(craw)?.get<TappedComponent>() shouldNotBe null
            }

            test("{3}{C}, {T}: draws a card") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Snapping Voidcraw")
                    .withLandsOnBattlefield(1, "Wastes", 4)
                    .withCardInLibrary(1, "Wastes")
                    .withCardInLibrary(1, "Wastes")
                    .withCardInLibrary(2, "Wastes")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val craw = game.findPermanent("Snapping Voidcraw")!!
                val before = game.handSize(1)
                game.execute(ActivateAbility(game.player1Id, craw, drawAbility)).error shouldBe null
                game.resolveStack()
                game.handSize(1) shouldBe before + 1
                game.state.getEntity(craw)?.get<TappedComponent>() shouldNotBe null
            }

            test("the draw ability needs {C} — four Forests can't pay it") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Snapping Voidcraw")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withCardInLibrary(1, "Wastes")
                    .withCardInLibrary(2, "Wastes")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val craw = game.findPermanent("Snapping Voidcraw")!!
                val before = game.handSize(1)
                game.execute(ActivateAbility(game.player1Id, craw, drawAbility)).error shouldNotBe null
                game.handSize(1) shouldBe before
            }
        }
    }
}
