package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.handlers.effects.ZoneMovementUtils
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Kotose, the Silent Spider (NEO #228).
 *
 * "When Kotose enters, exile target card other than a basic land card from an opponent's
 *  graveyard. Search that player's graveyard, hand, and library for any number of cards with the
 *  same name as that card and exile them. Then that player shuffles. For as long as you control
 *  Kotose, you may play one of the exiled cards, and you may spend mana as though it were mana of
 *  any color to cast it."
 *
 * Pins the parts that depend on how the pipeline executes: the target and the searched-out copies
 * form one permission group capped at a single play, the search may leave copies behind, a basic
 * land can't be targeted, and the off-color cast works on Kotose's colors alone.
 */
class KotoseTheSilentSpiderScenarioTest : ScenarioTestBase() {
    init {
        fun base() = scenario()
            .withPlayers("Alice", "Bob")
            .withCardInHand(1, "Kotose, the Silent Spider")
            .withLandsOnBattlefield(1, "Island", 3)
            .withLandsOnBattlefield(1, "Swamp", 3)
            .withCardInGraveyard(2, "Lightning Bolt")
            .withCardInGraveyard(2, "Mountain")
            .withCardInHand(2, "Lightning Bolt")
            .withCardInLibrary(2, "Lightning Bolt")
            .withCardInLibrary(2, "Mountain")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        fun TestGame.bobExile(name: String): List<EntityId> = state.getExile(player2Id).filter {
            state.getEntity(it)?.get<CardComponent>()?.name == name
        }

        fun TestGame.canCast(cardId: EntityId): Boolean =
            getLegalActions(1).any { (it.action as? CastSpell)?.cardId == cardId }

        /** Cast Kotose and answer its enter trigger's target with the Bolt in Bob's graveyard. */
        fun TestGame.castKotoseTargetingBolt(): EntityId {
            val graveyardBolt = findCardsInGraveyard(2, "Lightning Bolt").single()
            val mountain = findCardsInGraveyard(2, "Mountain").single()
            castSpell(1, "Kotose, the Silent Spider").error shouldBe null
            resolveStack()
            val decision = getPendingDecision()
            withClue("Kotose's enter trigger asks for its target") {
                (decision is ChooseTargetsDecision) shouldBe true
            }
            val legal = (decision as ChooseTargetsDecision).legalTargets[0].orEmpty()
            withClue("A basic land card isn't a legal target") { legal shouldNotContain mountain }
            selectTargets(listOf(graveyardBolt)).error shouldBe null
            resolveStack()
            return graveyardBolt
        }

        test("exiles the target and the chosen copies; exactly one of them may be cast, off-color") {
            val game = base().build()
            val handBolt = game.findCardsInHand(2, "Lightning Bolt").single()
            val libraryBolt = game.findCardsInLibrary(2, "Lightning Bolt").single()
            val graveyardBolt = game.castKotoseTargetingBolt()

            val select = game.getPendingDecision()
            withClue("You choose among the same-named cards in Bob's hand and library") {
                (select is SelectCardsDecision) shouldBe true
                (select as SelectCardsDecision).options shouldContainExactlyInAnyOrder listOf(handBolt, libraryBolt)
                select.minSelections shouldBe 0
            }
            game.selectCards(listOf(handBolt, libraryBolt)).error shouldBe null
            game.resolveStack()

            withClue("All three Bolts are in Bob's exile") {
                game.bobExile("Lightning Bolt") shouldContainExactlyInAnyOrder
                    listOf(graveyardBolt, handBolt, libraryBolt)
            }
            game.handSize(2) shouldBe 0
            game.findCardsInLibrary(2, "Mountain").size shouldBe 1

            withClue("Alice (Islands and Swamps only) may cast any of the exiled Bolts") {
                game.canCast(graveyardBolt) shouldBe true
                game.canCast(handBolt) shouldBe true
                game.canCast(libraryBolt) shouldBe true
            }

            game.execute(
                CastSpell(game.player1Id, handBolt, targets = listOf(ChosenTarget.Player(game.player2Id)))
            ).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(2) shouldBe 17

            withClue("Having played one, the other exiled cards stay in exile, unplayable") {
                game.canCast(graveyardBolt) shouldBe false
                game.canCast(libraryBolt) shouldBe false
                game.bobExile("Lightning Bolt") shouldContainExactlyInAnyOrder listOf(graveyardBolt, libraryBolt)
            }
        }

        test("choosing no copies exiles only the target and leaves the rest where they are") {
            val game = base().build()
            val handBolt = game.findCardsInHand(2, "Lightning Bolt").single()
            val libraryBolt = game.findCardsInLibrary(2, "Lightning Bolt").single()
            val graveyardBolt = game.castKotoseTargetingBolt()

            game.selectCards(emptyList()).error shouldBe null
            game.resolveStack()

            game.bobExile("Lightning Bolt") shouldBe listOf(graveyardBolt)
            game.findCardsInHand(2, "Lightning Bolt") shouldBe listOf(handBolt)
            game.findCardsInLibrary(2, "Lightning Bolt") shouldBe listOf(libraryBolt)
            withClue("The target alone is still castable") { game.canCast(graveyardBolt) shouldBe true }
        }

        test("the permission ends when Kotose leaves the battlefield") {
            val game = base().build()
            val graveyardBolt = game.castKotoseTargetingBolt()
            game.selectCards(emptyList()).error shouldBe null
            game.resolveStack()
            game.canCast(graveyardBolt) shouldBe true

            val kotose = game.findPermanent("Kotose, the Silent Spider")
            kotose shouldNotBe null
            game.state = ZoneMovementUtils.moveCardToZone(zones, game.state, kotose!!, Zone.GRAVEYARD).state
            game.checkStateBasedActions()

            withClue("Without Kotose the exiled Bolt can't be cast") { game.canCast(graveyardBolt) shouldBe false }
        }
    }
}
