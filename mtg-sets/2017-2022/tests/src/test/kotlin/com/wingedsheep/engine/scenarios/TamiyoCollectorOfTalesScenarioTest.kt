package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.war.cards.TamiyoCollectorOfTales
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tamiyo, Collector of Tales (WAR #220).
 *
 *   Spells and abilities your opponents control can't cause you to discard cards or sacrifice
 *   permanents.
 *   +1: Choose a nonland card name, then reveal the top four cards of your library. Put all cards
 *   with the chosen name from among them into your hand and the rest into your graveyard.
 *   −3: Return target card from your graveyard to your hand.
 */
class TamiyoCollectorOfTalesScenarioTest : ScenarioTestBase() {

    init {
        test("+1 puts every revealed card with the chosen name into hand and the rest into the graveyard") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Tamiyo, Collector of Tales")
                .withCardInLibrary(1, "Lightning Bolt")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Lightning Bolt")
                .withCardInLibrary(1, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val tamiyo = game.findPermanent("Tamiyo, Collector of Tales")!!
            activate(game, tamiyo, index = 0)
            game.resolveStack()

            val naming = game.getPendingDecision().shouldBeInstanceOf<ChooseOptionDecision>()
            withClue("only nonland names are offered") { naming.options.contains("Forest") shouldBe false }
            game.submitDecision(OptionChosenResponse(naming.id, naming.options.indexOf("Lightning Bolt"))).error shouldBe null
            game.resolveStack()

            game.findCardsInHand(1, "Lightning Bolt").size shouldBe 2
            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            game.isInGraveyard(1, "Forest") shouldBe true
            game.librarySize(1) shouldBe 0
        }

        test("−3 returns target card from your graveyard to your hand") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Tamiyo, Collector of Tales")
                .withCardInGraveyard(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val tamiyo = game.findPermanent("Tamiyo, Collector of Tales")!!
            val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
            activate(game, tamiyo, index = 1, targets = listOf(ChosenTarget.Card(bears, game.player1Id, Zone.GRAVEYARD)))
            game.resolveStack()

            game.isInHand(1, "Grizzly Bears") shouldBe true
            game.isInGraveyard(1, "Grizzly Bears") shouldBe false
        }

        test("an opponent's Mind Rot makes Tamiyo's controller discard nothing, unasked") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Tamiyo, Collector of Tales")
                .withCardsInHand(1, "Grizzly Bears", 3)
                .withCardInHand(2, "Mind Rot")
                .withLandsOnBattlefield(2, "Swamp", 3)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpellTargetingPlayer(2, "Mind Rot", 1).error shouldBe null
            game.resolveStack()

            game.hasPendingDecision() shouldBe false
            game.handSize(1) shouldBe 3
            game.graveyardSize(1) shouldBe 0
        }

        test("an opponent's Diabolic Edict makes Tamiyo's controller sacrifice nothing") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Tamiyo, Collector of Tales")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(2, "Diabolic Edict")
                .withLandsOnBattlefield(2, "Swamp", 2)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpellTargetingPlayer(2, "Diabolic Edict", 1).error shouldBe null
            game.resolveStack()

            game.hasPendingDecision() shouldBe false
            game.findPermanent("Grizzly Bears") shouldNotBe null
        }
    }

    private fun activate(game: TestGame, source: EntityId, index: Int, targets: List<ChosenTarget> = emptyList()) {
        game.execute(
            ActivateAbility(
                playerId = game.player1Id,
                sourceId = source,
                abilityId = TamiyoCollectorOfTales.activatedAbilities[index].id,
                targets = targets,
            )
        ).error shouldBe null
    }
}
