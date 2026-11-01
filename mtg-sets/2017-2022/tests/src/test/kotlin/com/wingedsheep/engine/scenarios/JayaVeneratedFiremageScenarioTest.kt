package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.war.cards.JayaVeneratedFiremage
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Jaya, Venerated Firemage — "If another red source you control would deal damage to a permanent
 * or player, it deals that much damage plus 1 to that permanent or player instead. / −2: Jaya deals
 * 2 damage to any target."
 *
 * Pins the two scopes the snapshot can't see: the bonus reaches every recipient (a permanent, an
 * opponent, and you yourself), and "another" keeps Jaya's own −2 at 2.
 */
class JayaVeneratedFiremageScenarioTest : ScenarioTestBase() {

    private val minusTwo = JayaVeneratedFiremage.activatedAbilities[0].id

    private fun board(vararg extra: String) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Jaya, Venerated Firemage")
        .withCardInHand(1, "Shock")
        .withLandsOnBattlefield(1, "Mountain", 1)
        .apply { extra.forEach { withCardOnBattlefield(2, it) } }
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("a red spell you control deals 1 extra damage to an opponent") {
            val game = board()
            game.castSpellTargetingPlayer(1, "Shock", 2).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(2) shouldBe 17
        }

        test("the bonus applies to damage to you as well — any player") {
            val game = board()
            game.castSpellTargetingPlayer(1, "Shock", 1).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 17
        }

        test("the bonus applies to damage to a permanent") {
            val game = board("Hill Giant")
            val giant = game.findPermanent("Hill Giant")!!
            game.castSpell(1, "Shock", targetId = giant).error shouldBe null
            game.resolveStack()
            withClue("Shock deals 3 to the 3/3 Hill Giant") {
                game.isInGraveyard(2, "Hill Giant") shouldBe true
            }
        }

        test("Jaya's own −2 is not boosted — she isn't another red source") {
            val game = board()
            val jaya = game.findPermanent("Jaya, Venerated Firemage")!!
            game.execute(
                ActivateAbility(game.player1Id, jaya, minusTwo, targets = listOf(ChosenTarget.Player(game.player2Id)))
            ).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 18
            game.state.getEntity(jaya)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) shouldBe 3
        }
    }
}
