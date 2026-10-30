package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.m21.cards.BasriKet
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Basri Ket (M21 #7).
 *
 * −2: Whenever one or more nontoken creatures attack this turn, create that many 1/1 white
 * Soldier creature tokens that are tapped and attacking.
 *
 * Pins the −2's delayed trigger against its ruling: it counts only the nontoken attackers,
 * attacking tokens neither trigger it nor add to the count, and a counted attacker that leaves
 * combat before the trigger resolves still counts.
 */
class BasriKetScenarioTest : ScenarioTestBase() {

    private val minusTwo = BasriKet.activatedAbilities[1].id

    private fun TestGame.attackingSoldiers(): List<EntityId> =
        findAllPermanents("Soldier Token").filter { state.getEntity(it)?.get<AttackingComponent>() != null }

    private fun TestGame.activateMinusTwo() {
        val basri = findPermanent("Basri Ket")!!
        execute(ActivateAbility(playerId = player1Id, sourceId = basri, abilityId = minusTwo)).error shouldBe null
        resolveStack()
        state.getEntity(basri)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) shouldBe 1
    }

    init {
        test("−2: creates one tapped, attacking Soldier per nontoken attacker; attacking tokens aren't counted") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Basri Ket")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardOnBattlefield(1, "Savannah Lions", isToken = true)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.activateMinusTwo()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(
                mapOf("Grizzly Bears" to 2, "Hill Giant" to 2, "Savannah Lions" to 2)
            ).error shouldBe null
            game.resolveStack()

            val soldiers = game.attackingSoldiers()
            withClue("two nontoken attackers → two Soldiers; the attacking token adds none") {
                soldiers.size shouldBe 2
            }
            withClue("the Soldiers are tokens and enter tapped") {
                soldiers.forEach {
                    game.state.getEntity(it)!!.has<TokenComponent>() shouldBe true
                    game.state.getEntity(it)!!.has<TappedComponent>() shouldBe true
                }
            }
        }

        test("−2: only attacking tokens doesn't trigger") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Basri Ket")
                .withCardOnBattlefield(1, "Savannah Lions", isToken = true)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.activateMinusTwo()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Savannah Lions" to 2)).error shouldBe null
            game.resolveStack()

            game.findAllPermanents("Soldier Token").size shouldBe 0
        }

        test("−2: an attacker that leaves combat before the trigger resolves still counts") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Basri Ket")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardInHand(1, "Unsummon")
                .withLandsOnBattlefield(1, "Island", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.activateMinusTwo()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2, "Hill Giant" to 2)).error shouldBe null
            withClue("the trigger is on the stack; bounce one counted attacker in response") {
                game.castSpell(1, "Unsummon", game.findPermanent("Hill Giant")!!).error shouldBe null
            }
            game.resolveStack()

            game.findPermanent("Hill Giant") shouldBe null
            game.attackingSoldiers().size shouldBe 2
        }
    }
}
