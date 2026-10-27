package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.znr.cards.JaceMirrorMage
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Jace, Mirror Mage (ZNR #63; {1}{U}{U}, Loyalty 4, Kicker {2}).
 *
 *   When Jace enters, if Jace was kicked, create a token that's a copy of Jace, except it's not
 *   legendary and its starting loyalty is 1.
 *   +1: Scry 2.
 *   0: Draw a card and reveal it. Remove a number of loyalty counters equal to that card's mana
 *      value from Jace.
 */
class JaceMirrorMageScenarioTest : ScenarioTestBase() {

    private val zero = JaceMirrorMage.activatedAbilities[1].id

    private fun loyalty(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    private fun isToken(game: TestGame, id: EntityId): Boolean =
        game.state.getEntity(id)?.has<TokenComponent>() == true

    init {
        test("kicked: a nonlegendary token copy enters with one loyalty, the original with four") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Jace, Mirror Mage")
                .withLandsOnBattlefield(1, "Island", 5)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val jace = game.findCardsInHand(1, "Jace, Mirror Mage").single()
            game.execute(CastSpell(game.player1Id, jace, declaredCostSlot = ChoiceSlot.KICKED))
                .error shouldBe null
            game.resolveStack()

            val jaces = game.findAllPermanents("Jace, Mirror Mage")
            withClue("the token isn't legendary, so both survive the legend rule") { jaces.size shouldBe 2 }
            val token = jaces.single { isToken(game, it) }
            val original = jaces.single { it != token }
            loyalty(game, token) shouldBe 1
            loyalty(game, original) shouldBe 4
        }

        test("unkicked: no token") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Jace, Mirror Mage")
                .withLandsOnBattlefield(1, "Island", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val jace = game.findCardsInHand(1, "Jace, Mirror Mage").single()
            game.execute(CastSpell(game.player1Id, jace)).error shouldBe null
            game.resolveStack()

            val jaces = game.findAllPermanents("Jace, Mirror Mage")
            jaces.size shouldBe 1
            loyalty(game, jaces.single()) shouldBe 4
        }

        test("0: draws a card and removes loyalty equal to its mana value") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Jace, Mirror Mage")
                .withCardInLibrary(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val jace = game.findPermanent("Jace, Mirror Mage")!!
            val handBefore = game.handSize(1)
            game.execute(ActivateAbility(playerId = game.player1Id, sourceId = jace, abilityId = zero))
                .error shouldBe null
            game.resolveStack()

            game.handSize(1) shouldBe handBefore + 1
            game.findCardsInHand(1, "Grizzly Bears").size shouldBe 1
            withClue("Grizzly Bears has mana value 2: four loyalty becomes two") { loyalty(game, jace) shouldBe 2 }
        }

        test("0: drawing a card with mana value at least Jace's loyalty kills him") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Jace, Mirror Mage")
                .withCardInLibrary(1, "Hill Giant")
                .withCardInLibrary(1, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val jace = game.findPermanent("Jace, Mirror Mage")!!
            game.execute(ActivateAbility(playerId = game.player1Id, sourceId = jace, abilityId = zero))
                .error shouldBe null
            game.resolveStack()

            withClue("Hill Giant has mana value 4: Jace drops to zero and is put into the graveyard") {
                game.findPermanent("Jace, Mirror Mage") shouldBe null
            }
        }
    }
}
