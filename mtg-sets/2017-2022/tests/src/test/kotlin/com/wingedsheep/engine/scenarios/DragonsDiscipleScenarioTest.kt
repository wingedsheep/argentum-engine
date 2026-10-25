package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Dragon's Disciple — {1}{W} Creature — Human Monk 1/3 (AFR #13).
 *
 * "As this creature enters, you may reveal a Dragon card from your hand. If you do or if you
 * control a Dragon, this creature enters with a +1/+1 counter on it."
 */
class DragonsDiscipleScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusOnes(id: EntityId) =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun board(inHand: List<String> = emptyList(), onBattlefield: List<String> = emptyList()): TestGame {
        val builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardInHand(1, "Dragon's Disciple")
            .withLandsOnBattlefield(1, "Plains", 2)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        inHand.forEach { builder.withCardInHand(1, it) }
        onBattlefield.forEach { builder.withCardOnBattlefield(1, it) }
        return builder.build()
    }

    init {
        test("revealing a Dragon card from hand gives the +1/+1 counter and keeps the card in hand") {
            val game = board(inHand = listOf("Shivan Dragon"))
            game.castSpell(1, "Dragon's Disciple").error shouldBe null
            game.resolveStack()

            withClue("The reveal should be offered") { game.hasPendingDecision() shouldBe true }
            game.selectCards(listOf(game.findCardsInHand(1, "Shivan Dragon").single()))
            game.resolveStack()

            val disciple = game.findPermanent("Dragon's Disciple").shouldNotBeNull()
            game.plusOnes(disciple) shouldBe 1
            game.isInHand(1, "Shivan Dragon") shouldBe true
        }

        test("declining the reveal with no Dragon in play gives no counter") {
            val game = board(inHand = listOf("Shivan Dragon"))
            game.castSpell(1, "Dragon's Disciple").error shouldBe null
            game.resolveStack()

            withClue("The reveal should be offered") { game.hasPendingDecision() shouldBe true }
            game.skipSelection()
            game.resolveStack()

            game.plusOnes(game.findPermanent("Dragon's Disciple").shouldNotBeNull()) shouldBe 0
        }

        test("controlling a Dragon gives the counter without any reveal") {
            val game = board(onBattlefield = listOf("Dragon Whelp"))
            game.castSpell(1, "Dragon's Disciple").error shouldBe null
            game.resolveStack()

            game.hasPendingDecision() shouldBe false
            game.plusOnes(game.findPermanent("Dragon's Disciple").shouldNotBeNull()) shouldBe 1
        }

        test("no Dragon anywhere means no counter and no question") {
            val game = board()
            game.castSpell(1, "Dragon's Disciple").error shouldBe null
            game.resolveStack()

            game.hasPendingDecision() shouldBe false
            game.plusOnes(game.findPermanent("Dragon's Disciple").shouldNotBeNull()) shouldBe 0
        }
    }
}
