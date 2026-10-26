package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Draugr Necromancer (KHM #86).
 *
 *   If a nontoken creature an opponent controls would die, exile that card with an ice counter on
 *   it instead. You may cast spells from among cards in exile your opponents own with ice counters
 *   on them, and you may spend mana from snow sources as though it were mana of any color to cast
 *   those spells.
 *
 * The snow-mana rider is pinned in the engine's SnowManaAsAnyColorTest; here it is exercised end
 * to end: an opponent's Grizzly Bears ({1}{G}) dies to Disfigure ({B}, paid by the Swamp), is iced,
 * and is cast back with two snow Islands.
 */
class DraugrNecromancerScenarioTest : ScenarioTestBase() {

    private fun board(islands: String, bearsOwner: Int = 2): TestGame = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Draugr Necromancer")
        .withCardOnBattlefield(bearsOwner, "Grizzly Bears")
        .withCardInHand(1, "Disfigure")
        .withLandsOnBattlefield(1, "Swamp", 1)
        .withLandsOnBattlefield(1, islands, 2)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.killBears() {
        castSpell(1, "Disfigure", findPermanent("Grizzly Bears")!!).error shouldBe null
        resolveStack()
    }

    private fun TestGame.icedBears(owner: EntityId = player2Id): EntityId? = state.getExile(owner).firstOrNull {
        state.getEntity(it)?.get<CardComponent>()?.name == "Grizzly Bears"
    }

    init {
        context("Draugr Necromancer") {

            test("an opponent's dying creature is exiled with an ice counter instead") {
                val game = board("Snow-Covered Island")
                game.killBears()
                val bears = game.icedBears()
                withClue("Grizzly Bears is in its owner's exile, not the graveyard") { bears shouldNotBe null }
                game.isInGraveyard(2, "Grizzly Bears") shouldBe false
                game.state.getEntity(bears!!)?.get<CountersComponent>()?.getCount(CounterType.ICE) shouldBe 1
            }

            test("the iced card is cast with snow mana of another color and enters under your control") {
                val game = board("Snow-Covered Island")
                game.killBears()
                val bears = game.icedBears()!!
                game.execute(CastSpell(game.player1Id, bears)).error shouldBe null
                game.resolveStack()
                val onBattlefield = game.findPermanent("Grizzly Bears")
                onBattlefield shouldNotBe null
                game.state.getEntity(onBattlefield!!)?.get<ControllerComponent>()?.playerId shouldBe game.player1Id
            }

            test("non-snow mana of another color can't pay the iced spell's colored pip") {
                val game = board("Island")
                game.killBears()
                val bears = game.icedBears()!!
                game.execute(CastSpell(game.player1Id, bears)).error shouldNotBe null
            }

            test("your own card an opponent controls is iced but can't be cast (ruling)") {
                val game = board("Snow-Covered Island", bearsOwner = 1)
                val bears = game.findPermanent("Grizzly Bears")!!
                game.state = game.state.updateEntity(bears) { it.with(ControllerComponent(game.player2Id)) }
                game.killBears()
                val iced = game.icedBears(owner = game.player1Id)
                withClue("Grizzly Bears is in its owner's (your) exile") { iced shouldNotBe null }
                game.state.getEntity(iced!!)?.get<CountersComponent>()?.getCount(CounterType.ICE) shouldBe 1
                game.execute(CastSpell(game.player1Id, iced)).error shouldNotBe null
            }

            test("your own creature dies normally") {
                val game = board("Snow-Covered Island", bearsOwner = 1)
                game.killBears()
                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            }
        }
    }
}
