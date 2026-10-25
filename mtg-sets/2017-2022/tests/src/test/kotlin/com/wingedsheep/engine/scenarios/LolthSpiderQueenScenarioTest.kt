package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Lolth, Spider Queen (AFR #112).
 *
 *   Whenever a creature you control dies, put a loyalty counter on Lolth.
 *   −8: You get an emblem with "Whenever an opponent is dealt combat damage by one or more
 *   creatures you control, if that player lost less than 8 life this turn, they lose life equal
 *   to the difference."
 *
 * The emblem's batch trigger lives on no permanent, so these prove the combat-damage batch
 * detector reads global abilities: one trigger per damaged opponent, topping their life lost this
 * turn up to 8, and none once they already lost 8.
 */
class LolthSpiderQueenScenarioTest : ScenarioTestBase() {

    private val lolthName = "Lolth, Spider Queen"

    private fun ScenarioTestBase.TestGame.loyalty(): Int =
        state.getEntity(findPermanent(lolthName)!!)!!.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    private fun ScenarioTestBase.TestGame.ultimate() {
        val lolth = findPermanent(lolthName)!!
        state = state.updateEntity(lolth) { c -> c.with(CountersComponent().withAdded(CounterType.LOYALTY, 8)) }
        val minusEight = cardRegistry.getCard(lolthName)!!.script.activatedAbilities[2]
        execute(ActivateAbility(playerId = player1Id, sourceId = lolth, abilityId = minusEight.id)).error shouldBe null
        resolveStack()
        withClue("Lolth went to 0 loyalty; the emblem stays") { findPermanent(lolthName) shouldBe null }
    }

    init {
        fun board(vararg attackers: String) = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, lolthName)
            .apply { attackers.forEach { withCardOnBattlefield(1, it) } }
            .withCardInLibrary(1, "Swamp")
            .withCardInLibrary(2, "Swamp")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        test("a creature you control dying puts a loyalty counter on Lolth") {
            val game = board("Grizzly Bears")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .build()
            val before = game.loyalty()
            game.castSpell(1, "Lightning Bolt", game.findPermanent("Grizzly Bears")).error shouldBe null
            game.resolveStack()
            game.findPermanent("Grizzly Bears") shouldBe null
            game.loyalty() shouldBe before + 1
        }

        test("emblem triggers once for the batch and tops the opponent's life lost this turn up to 8") {
            val game = board("Centaur Courser", "Grizzly Bears").build()
            game.ultimate()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Centaur Courser" to 2, "Grizzly Bears" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)

            withClue("5 combat damage, then one trigger for the difference of 3") {
                game.getLifeTotal(2) shouldBe 12
            }
        }

        test("emblem does nothing once the opponent already lost 8 or more life this turn") {
            val game = board("Craw Wurm", "Centaur Courser").build()
            game.ultimate()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Craw Wurm" to 2, "Centaur Courser" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)

            game.getLifeTotal(2) shouldBe 11
        }
    }
}
