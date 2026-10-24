package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Mavinda, Students' Advocate — "{0}: You may cast target instant or sorcery card from your
 * graveyard this turn. If that spell doesn't target a creature you control, it costs {8} more to
 * cast this way. If that spell would be put into your graveyard, exile it instead."
 */
class MavindaStudentsAdvocateScenarioTest : ScenarioTestBase() {

    private fun TestGame.activateOn(cardName: String) {
        val card = findCardsInGraveyard(1, cardName).single()
        execute(ActivateAbility(
            playerId = player1Id,
            sourceId = findPermanent("Mavinda, Students' Advocate")!!,
            abilityId = cardRegistry.getCard("Mavinda, Students' Advocate")!!.activatedAbilities.single().id,
            targets = listOf(ChosenTarget.Card(card, player1Id, Zone.GRAVEYARD)),
        )).error shouldBe null
        resolveStack()
    }

    private fun board(forests: Int, islands: Int = 0, inGraveyard: String = "Giant Growth") = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Mavinda, Students' Advocate")
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withCardInGraveyard(1, inGraveyard)
        .withLandsOnBattlefield(1, "Forest", forests)
        .withLandsOnBattlefield(1, "Island", islands)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("targeting a creature you control costs only the spell's mana cost, then it is exiled") {
            val game = board(forests = 1)
            game.activateOn("Giant Growth")
            val mavinda = game.findPermanent("Mavinda, Students' Advocate")!!
            game.castSpellFromGraveyard(1, "Giant Growth", mavinda).error shouldBe null
            game.resolveStack()
            game.isInExile(1, "Giant Growth") shouldBe true
            game.isInGraveyard(1, "Giant Growth") shouldBe false
        }

        test("targeting a creature you don't control costs {8} more") {
            val poor = board(forests = 8)
            poor.activateOn("Giant Growth")
            val bears = poor.findPermanent("Grizzly Bears")!!
            poor.castSpellFromGraveyard(1, "Giant Growth", bears).error shouldNotBe null

            val rich = board(forests = 9)
            rich.activateOn("Giant Growth")
            rich.castSpellFromGraveyard(1, "Giant Growth", rich.findPermanent("Grizzly Bears")!!).error shouldBe null
        }

        test("the offered cast is affordable only when a waiver target exists") {
            val game = board(forests = 1)
            game.activateOn("Giant Growth")
            val offer = game.getLegalActions(1).single {
                (it.action as? CastSpell)?.cardId == game.findCardsInGraveyard(1, "Giant Growth").single()
            }
            offer.isAffordable shouldBe true
        }

        test("a spell with no targets always pays the tax") {
            val poor = board(forests = 0, islands = 9, inGraveyard = "Think Twice")
            poor.activateOn("Think Twice")
            poor.castSpellFromGraveyard(1, "Think Twice").error shouldNotBe null

            val rich = board(forests = 0, islands = 10, inGraveyard = "Think Twice")
            rich.activateOn("Think Twice")
            rich.castSpellFromGraveyard(1, "Think Twice").error shouldBe null
        }

        test("casting the card with flashback instead is not this way and owes no tax") {
            val game = board(forests = 0, islands = 3, inGraveyard = "Think Twice")
            game.activateOn("Think Twice")
            game.execute(CastSpell(
                playerId = game.player1Id,
                cardId = game.findCardsInGraveyard(1, "Think Twice").single(),
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.FLASHBACK,
            )).error shouldBe null
        }

        test("activates only once each turn") {
            val game = board(forests = 1)
            game.activateOn("Giant Growth")
            game.execute(ActivateAbility(
                playerId = game.player1Id,
                sourceId = game.findPermanent("Mavinda, Students' Advocate")!!,
                abilityId = cardRegistry.getCard("Mavinda, Students' Advocate")!!.activatedAbilities.single().id,
                targets = listOf(ChosenTarget.Card(
                    game.findCardsInGraveyard(1, "Giant Growth").single(), game.player1Id, Zone.GRAVEYARD,
                )),
            )).error shouldNotBe null
        }
    }
}
