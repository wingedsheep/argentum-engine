package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Wrath of the Skies (MH3) — "You get X {E}, then you may pay any amount of {E}. Destroy each
 * artifact, creature, and enchantment with mana value less than or equal to the amount of {E}
 * paid this way."
 */
class WrathOfTheSkiesScenarioTest : ScenarioTestBase() {

    private fun energy(game: TestGame, playerId: EntityId): Int =
        game.state.getEntity(playerId)?.get<CountersComponent>()?.getCount(CounterType.ENERGY) ?: 0

    private fun castWrath(game: TestGame, x: Int) {
        val card = game.state.getHand(game.player1Id).first { id ->
            game.state.getEntity(id)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == "Wrath of the Skies"
        }
        game.execute(CastSpell(game.player1Id, card, xValue = x)).error shouldBe null
        game.resolveStack()
    }

    private fun board() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Wrath of the Skies")
        .withLandsOnBattlefield(1, "Plains", 5)
        .withCardOnBattlefield(1, "Memnite") // artifact creature, MV 0
        .withCardOnBattlefield(2, "Grizzly Bears") // creature, MV 2
        .withCardOnBattlefield(2, "Craw Wurm") // creature, MV 6
        .withCardOnBattlefield(2, "Ornithopter") // artifact creature, MV 0
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("X=3, pay 2: destroys mana value 2 or less, keeps the rest and the unpaid energy") {
            val game = board()
            castWrath(game, 3)

            val decision = game.getPendingDecision()
            withClue("prompt to pay 0..3 energy") {
                (decision is ChooseNumberDecision) shouldBe true
                (decision as ChooseNumberDecision).maxValue shouldBe 3
            }
            game.chooseNumber(2).error shouldBe null

            game.findPermanent("Memnite") shouldBe null
            game.findPermanent("Ornithopter") shouldBe null
            game.findPermanent("Grizzly Bears") shouldBe null
            game.findPermanent("Craw Wurm") shouldNotBe null
            energy(game, game.player1Id) shouldBe 1
        }

        test("paying zero still destroys every mana-value-0 permanent") {
            val game = board()
            castWrath(game, 3)
            game.chooseNumber(0).error shouldBe null

            game.findPermanent("Memnite") shouldBe null
            game.findPermanent("Ornithopter") shouldBe null
            game.findPermanent("Grizzly Bears") shouldNotBe null
            game.findPermanent("Craw Wurm") shouldNotBe null
            energy(game, game.player1Id) shouldBe 3
        }

        test("X=0 with no energy: no prompt, mana-value-0 permanents destroyed") {
            val game = board()
            castWrath(game, 0)

            (game.getPendingDecision() is ChooseNumberDecision) shouldBe false
            game.findPermanent("Memnite") shouldBe null
            game.findPermanent("Grizzly Bears") shouldNotBe null
            energy(game, game.player1Id) shouldBe 0
        }
    }
}
