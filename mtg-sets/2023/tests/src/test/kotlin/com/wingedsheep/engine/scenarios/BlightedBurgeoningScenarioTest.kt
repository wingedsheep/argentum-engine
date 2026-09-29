package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Blighted Burgeoning — Aura (enchant land): ETB incubate 2; the enchanted land adds an additional
 * one mana of any color when tapped for mana.
 */
class BlightedBurgeoningScenarioTest : ScenarioTestBase() {
    init {
        test("entering incubates 2 and the enchanted forest makes an extra mana of any color") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Blighted Burgeoning")
                .withLandsOnBattlefield(1, "Forest", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val forest = game.findPermanents("Forest").first()

            game.castSpell(1, "Blighted Burgeoning", forest).error shouldBe null
            game.resolveStack()

            game.state.getEntity(game.findPermanent("Blighted Burgeoning")!!)
                ?.get<AttachedToComponent>()?.targetId shouldBe forest

            val incubators = game.state.getBattlefield(game.player1Id)
                .filter { game.state.getEntity(it)?.get<CardComponent>()?.name == "Incubator" }
            incubators.size shouldBe 1
            game.state.getEntity(incubators.single())?.get<CountersComponent>()
                ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2

            // Untap every land, then leave only the enchanted forest: {G} from the forest plus the
            // any-color bonus pays {R}{G}, but never two off-color pips.
            val others = game.findPermanents("Forest").filter { it != forest }
            game.state = others.fold(game.state) { st, id -> st.updateEntity(id) { it.with(TappedComponent) } }
            game.state = game.state.updateEntity(forest) { it.without<TappedComponent>() }
            val solver = services.manaSolver
            solver.canPay(game.state, game.player1Id, ManaCost.parse("{R}{G}")) shouldBe true
            solver.canPay(game.state, game.player1Id, ManaCost.parse("{U}")) shouldBe true
            solver.canPay(game.state, game.player1Id, ManaCost.parse("{R}{R}")) shouldBe false
        }
    }
}
