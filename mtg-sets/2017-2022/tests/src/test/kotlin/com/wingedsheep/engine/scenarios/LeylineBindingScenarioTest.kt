package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Leyline Binding (DMU #24) — {5}{W} Enchantment, Flash.
 * "Domain — This spell costs {1} less to cast for each basic land type among lands you control.
 * When this enchantment enters, exile target nonland permanent an opponent controls until this
 * enchantment leaves the battlefield."
 *
 * First card to lower a domain count into a self-cast cost reduction, so this pins that the
 * discount counts the caster's basic land *types* (not lands, not the opponent's), and that the
 * enter trigger exiles the chosen permanent.
 */
class LeylineBindingScenarioTest : ScenarioTestBase() {

    private fun TestGame.untappedLands(): Int = state.getBattlefield(player1Id).count { id ->
        val entity = state.getEntity(id)
        entity?.get<CardComponent>()?.typeLine?.isLand == true && !entity.has<TappedComponent>()
    }

    init {
        test("four basic land types make it cost {1}{W}; the trigger exiles the target") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Leyline Binding")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withLandsOnBattlefield(1, "Island", 1)
                .withLandsOnBattlefield(1, "Swamp", 1)
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!

            withClue("four lands pay only the domain-discounted {1}{W}") {
                game.castSpell(1, "Leyline Binding").error shouldBe null
            }
            withClue("exactly two of the four lands were tapped for {1}{W}") {
                game.untappedLands() shouldBe 2
            }
            game.resolveStack()
            game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Leyline Binding") shouldBe true
            game.findPermanent("Grizzly Bears") shouldBe null
            game.isInExile(2, "Grizzly Bears") shouldBe true
        }

        test("counts types, not lands, and ignores the opponent's lands") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Leyline Binding")
                .withLandsOnBattlefield(1, "Plains", 4)
                .withLandsOnBattlefield(2, "Island", 1)
                .withLandsOnBattlefield(2, "Swamp", 1)
                .withLandsOnBattlefield(2, "Mountain", 1)
                .withLandsOnBattlefield(2, "Forest", 1)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            withClue("one basic land type → {4}{W}, which four Plains can't pay") {
                game.castSpell(1, "Leyline Binding").error shouldNotBe null
            }
        }
    }
}
