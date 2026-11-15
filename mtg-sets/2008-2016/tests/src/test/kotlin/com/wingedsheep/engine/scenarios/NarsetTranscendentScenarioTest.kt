package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.dtk.cards.NarsetTranscendent
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Narset Transcendent (DTK #225; {2}{W}{U}, Loyalty 6).
 *
 *   +1: Look at the top card of your library. If it's a noncreature, nonland card, you may reveal
 *       it and put it into your hand.
 *   −2: When you next cast an instant or sorcery spell from your hand this turn, it gains rebound.
 *   −9: You get an emblem with "Your opponents can't cast noncreature spells."
 *
 * The emblem's rules matrix lives in the engine's `EmblemCastProhibitionTest`; this pins the card.
 */
class NarsetTranscendentScenarioTest : ScenarioTestBase() {

    private val plusOne = NarsetTranscendent.activatedAbilities[0].id
    private val minusTwo = NarsetTranscendent.activatedAbilities[1].id
    private val minusNine = NarsetTranscendent.activatedAbilities[2].id

    init {
        fun TestGame.zoneHas(playerNumber: Int, zone: Zone, name: String): Boolean {
            val player = if (playerNumber == 1) player1Id else player2Id
            return state.getZone(player, zone).any { state.getEntity(it)?.get<CardComponent>()?.name == name }
        }

        fun narsetGame(loyalty: Int, configure: ScenarioBuilder.() -> ScenarioBuilder = { this }): TestGame {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Narset Transcendent")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .configure()
                .build()
            val narset = game.findPermanent("Narset Transcendent")!!
            game.state = game.state.updateEntity(narset) { c ->
                c.with(CountersComponent().withAdded(CounterType.LOYALTY, loyalty))
            }
            return game
        }

        fun TestGame.activate(abilityId: com.wingedsheep.sdk.scripting.AbilityId) {
            val narset = findPermanent("Narset Transcendent")!!
            execute(ActivateAbility(playerId = player1Id, sourceId = narset, abilityId = abilityId)).error shouldBe null
            resolveStack()
        }

        context("Narset Transcendent — +1") {
            test("a noncreature, nonland top card may be revealed and put into hand") {
                val game = narsetGame(6) {
                    withCardInLibrary(1, "Lightning Bolt").withCardInLibrary(1, "Island")
                }
                game.activate(plusOne)

                val choice = game.getPendingDecision() as? SelectCardsDecision
                    ?: error("expected the reveal choice; got ${game.getPendingDecision()}")
                val bolt = game.findCardsInLibrary(1, "Lightning Bolt").single()
                choice.options shouldBe listOf(bolt)
                game.selectCards(listOf(bolt)).error shouldBe null
                game.resolveStack()

                game.isInHand(1, "Lightning Bolt") shouldBe true
                game.librarySize(1) shouldBe 1
                val narset = game.findPermanent("Narset Transcendent")!!
                game.state.getEntity(narset)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) shouldBe 7
            }

            test("a creature on top stays there") {
                val game = narsetGame(6) {
                    withCardInLibrary(1, "Grizzly Bears").withCardInLibrary(1, "Island")
                }
                game.activate(plusOne)
                withClue("nothing eligible, so no reveal choice is offered") {
                    (game.getPendingDecision() as? SelectCardsDecision)?.options.orEmpty() shouldBe emptyList()
                }
                if (game.getPendingDecision() != null) {
                    game.selectCards(emptyList()).error shouldBe null
                    game.resolveStack()
                }

                game.isInHand(1, "Grizzly Bears") shouldBe false
                game.state.getZone(game.player1Id, Zone.LIBRARY).first() shouldBe
                    game.findCardsInLibrary(1, "Grizzly Bears").single()
            }
        }

        context("Narset Transcendent — −2") {
            test("the next instant cast from hand this turn gains rebound; the one after doesn't") {
                val game = narsetGame(6) {
                    withCardInHand(1, "Lightning Bolt")
                        .withCardInHand(1, "Lightning Bolt")
                        .withLandsOnBattlefield(1, "Mountain", 2)
                }
                game.activate(minusTwo)

                repeat(2) {
                    game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
                    if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                    game.resolveStack()
                }

                game.getLifeTotal(2) shouldBe 14
                withClue("the first Bolt rebounded into exile") { game.zoneHas(1, Zone.EXILE, "Lightning Bolt") shouldBe true }
                withClue("the second Bolt went to the graveyard") { game.zoneHas(1, Zone.GRAVEYARD, "Lightning Bolt") shouldBe true }
            }
        }

        context("Narset Transcendent — −9") {
            test("the emblem stops opponents casting noncreature spells but not creature spells") {
                val game = narsetGame(9) {
                    withCardInHand(2, "Lightning Bolt")
                        .withCardInHand(2, "Grizzly Bears")
                        .withLandsOnBattlefield(2, "Mountain", 1)
                        .withLandsOnBattlefield(2, "Forest", 2)
                        .apply { repeat(4) { withCardInLibrary(1, "Island"); withCardInLibrary(2, "Forest") } }
                }
                game.activate(minusNine)
                withClue("Narset went to the graveyard at 0 loyalty") {
                    game.findPermanent("Narset Transcendent") shouldBe null
                }

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                game.state.activePlayerId shouldBe game.player2Id

                val bolt = game.findCardsInHand(2, "Lightning Bolt").single()
                withClue("Lightning Bolt isn't offered") {
                    game.getLegalActions(2).none { (it.action as? CastSpell)?.cardId == bolt } shouldBe true
                }
                game.castSpellTargetingPlayer(2, "Lightning Bolt", 1).error shouldNotBe null

                game.castSpell(2, "Grizzly Bears").error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()
                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }
        }
    }
}
