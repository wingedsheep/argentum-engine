package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Sheer Drop — destroys a tapped creature (an untapped one is not a legal target); awakened, it also
 * makes a land a 3/3 Elemental with haste.
 */
class SheerDropScenarioTest : ScenarioTestBase() {

    private fun board(plains: Int) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Sheer Drop")
        .withLandsOnBattlefield(1, "Plains", plains)
        .withCardOnBattlefield(2, "Grizzly Bears", tapped = true)
        .withCardOnBattlefield(2, "Hill Giant")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("cast for {2}{W}, it destroys the tapped creature and no land is awakened") {
            val game = board(plains = 3)
            game.castSpell(1, "Sheer Drop", game.findPermanent("Grizzly Bears")!!).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            game.findPermanent("Hill Giant") shouldNotBe null
            game.findPermanents("Plains").none { game.state.projectedState.isCreature(it) } shouldBe true
        }

        test("an untapped creature is not a legal target") {
            val game = board(plains = 3)
            game.castSpell(1, "Sheer Drop", game.findPermanent("Hill Giant")!!).error shouldNotBe null
            game.findPermanent("Hill Giant") shouldNotBe null
        }

        test("cast for its awaken cost, it destroys the tapped creature and the land becomes a 3/3 hasty Elemental") {
            val game = board(plains = 6)
            val land = game.findPermanents("Plains").first()
            game.castSpellWithAwaken(1, "Sheer Drop", listOf(game.findPermanent("Grizzly Bears")!!, land))
                .error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            val projected = game.state.projectedState
            projected.isCreature(land) shouldBe true
            projected.hasSubtype(land, "Elemental") shouldBe true
            projected.hasSubtype(land, "Plains") shouldBe true
            projected.hasKeyword(land, Keyword.HASTE) shouldBe true
            projected.getPower(land) shouldBe 3
            projected.getToughness(land) shouldBe 3
            game.state.getEntity(land)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 3
        }
    }
}
