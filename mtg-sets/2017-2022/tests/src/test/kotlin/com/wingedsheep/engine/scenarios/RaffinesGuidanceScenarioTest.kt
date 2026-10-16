package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Raffine's Guidance — "You may cast this card from your graveyard by paying {2}{W} rather than
 * paying its mana cost." A keywordless alternative cost on the self-zone permission
 * (`MayCastSelfFromZones.alternativeManaCost`): {2}{W} replaces {W} from the graveyard only, and
 * cost increases still apply on top of it (CR 601.2f).
 */
class RaffinesGuidanceScenarioTest : ScenarioTestBase() {

    private fun game(plains: Int, inGraveyard: Boolean = true, extraBattlefield: String? = null) = scenario()
        .withPlayers()
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withLandsOnBattlefield(1, "Plains", plains)
        .let { if (inGraveyard) it.withCardInGraveyard(1, "Raffine's Guidance") else it.withCardInHand(1, "Raffine's Guidance") }
        .let { b -> extraBattlefield?.let { b.withCardOnBattlefield(2, it) } ?: b }
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(2, "Plains")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.graveyardCast(guidance: EntityId) =
        getLegalActions(1).single { (it.action as? CastSpell)?.cardId == guidance }

    /** The graveyard cast as the engine offers it — announced as the card's own alternative cost. */
    private fun TestGame.castFromGraveyard(guidance: EntityId, bears: EntityId) =
        execute((graveyardCast(guidance).action as CastSpell).copy(targets = listOf(ChosenTarget.Permanent(bears))))

    private fun TestGame.tappedPlains() =
        state.getBattlefield().count { id ->
            state.getEntity(id)?.let { it.has<TappedComponent>() && it.get<CardComponent>()?.name == "Plains" } == true
        }

    init {
        test("cast from the graveyard for {2}{W} instead of {W}") {
            val game = game(plains = 3)
            val bears = game.findPermanent("Grizzly Bears")!!
            val guidance = game.findCardsInGraveyard(1, "Raffine's Guidance").single()

            val offered = game.graveyardCast(guidance)
            offered.manaCostString shouldBe "{2}{W}"
            offered.isAffordable shouldBe true
            (offered.action as CastSpell).alternativeCostType shouldBe AlternativeCostType.SELF_ZONE

            game.castFromGraveyard(guidance, bears).error shouldBe null
            game.resolveStack()

            game.findPermanent("Raffine's Guidance") shouldNotBe null
            game.tappedPlains() shouldBe 3
            game.state.projectedState.getPower(bears) shouldBe 3
            game.state.projectedState.getToughness(bears) shouldBe 3
        }

        test("the graveyard cast can't be paid with only its printed {W}") {
            val game = game(plains = 2)
            val bears = game.findPermanent("Grizzly Bears")!!
            val guidance = game.findCardsInGraveyard(1, "Raffine's Guidance").single()

            game.graveyardCast(guidance).isAffordable shouldBe false
            game.castFromGraveyard(guidance, bears).error shouldNotBe null
            game.isInGraveyard(1, "Raffine's Guidance") shouldBe true
        }

        test("a plain cast from the graveyard isn't authorized — only the {2}{W} alternative cost is") {
            val game = game(plains = 3)
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpellFromGraveyard(1, "Raffine's Guidance", bears).error shouldNotBe null
            game.isInGraveyard(1, "Raffine's Guidance") shouldBe true
        }

        test("cast from hand it still costs {W}") {
            val game = game(plains = 1, inGraveyard = false)
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Raffine's Guidance", bears).error shouldBe null
            game.resolveStack()

            game.findPermanent("Raffine's Guidance") shouldNotBe null
        }

        test("a cost increase applies on top of the alternative cost") {
            // Thalia: noncreature spells cost {1} more — {2}{W} becomes {3}{W}.
            val game = game(plains = 3, extraBattlefield = "Thalia, Guardian of Thraben")
            val bears = game.findPermanent("Grizzly Bears")!!
            val guidance = game.findCardsInGraveyard(1, "Raffine's Guidance").single()

            game.graveyardCast(guidance).isAffordable shouldBe false
            game.castFromGraveyard(guidance, bears).error shouldNotBe null
        }

        test("with the cost increase paid, the graveyard cast taps four lands") {
            val game = game(plains = 4, extraBattlefield = "Thalia, Guardian of Thraben")
            val bears = game.findPermanent("Grizzly Bears")!!
            val guidance = game.findCardsInGraveyard(1, "Raffine's Guidance").single()

            game.graveyardCast(guidance).isAffordable shouldBe true
            game.castFromGraveyard(guidance, bears).error shouldBe null
            game.tappedPlains() shouldBe 4
        }
    }
}
