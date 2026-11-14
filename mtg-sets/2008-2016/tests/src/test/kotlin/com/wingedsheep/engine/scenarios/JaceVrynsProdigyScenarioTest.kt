package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.ori.cards.JaceVrynsProdigy
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Jace, Vryn's Prodigy // Jace, Telepath Unbound (ORI #60).
 *
 * Front {T}: loot, then flip if there are five or more cards in your graveyard — counted *after*
 * the discard (ruling 2015-06-22). Back −3: you may cast target instant or sorcery card from your
 * graveyard this turn, and it's exiled instead of going back to the graveyard.
 */
class JaceVrynsProdigyScenarioTest : ScenarioTestBase() {

    private val lootId = JaceVrynsProdigy.activatedAbilities[0].id
    private val minusThreeId = JaceVrynsProdigy.backFace!!.script.activatedAbilities[1].id

    /** Activates Jace's loot, drawing the library's Island and discarding the Grizzly Bears in hand. */
    private fun TestGame.lootDiscardingBears() {
        val jace = findPermanent("Jace, Vryn's Prodigy")!!
        execute(ActivateAbility(player1Id, jace, lootId)).error shouldBe null
        resolveStack()
        val bears = findCardsInHand(1, "Grizzly Bears").single()
        selectCards(listOf(bears)).error shouldBe null
        resolveStack()
    }

    init {
        test("the discarded card counts: four in the graveyard plus the discard flips Jace into a 5-loyalty walker") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Jace, Vryn's Prodigy")
                .withCardInHand(1, "Grizzly Bears")
                .withCardInLibrary(1, "Island")
                .withCardInGraveyard(1, "Lightning Bolt")
                .withCardInGraveyard(1, "Lightning Bolt")
                .withCardInGraveyard(1, "Lightning Bolt")
                .withCardInGraveyard(1, "Lightning Bolt")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.lootDiscardingBears()

            game.graveyardSize(1) shouldBe 5
            game.findPermanent("Jace, Vryn's Prodigy") shouldBe null
            val telepath = game.findPermanent("Jace, Telepath Unbound").shouldNotBeNull()
            game.state.getEntity(telepath)!!.get<CountersComponent>()!!
                .getCount(CounterType.LOYALTY) shouldBe 5
            game.state.projectedState.isCreature(telepath) shouldBe false
        }

        test("four cards in the graveyard after the discard leaves Jace unflipped") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Jace, Vryn's Prodigy")
                .withCardInHand(1, "Grizzly Bears")
                .withCardInLibrary(1, "Island")
                .withCardInGraveyard(1, "Lightning Bolt")
                .withCardInGraveyard(1, "Lightning Bolt")
                .withCardInGraveyard(1, "Lightning Bolt")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.lootDiscardingBears()

            game.graveyardSize(1) shouldBe 4
            game.isInHand(1, "Island") shouldBe true
            game.findPermanent("Jace, Vryn's Prodigy") shouldNotBe null
            game.findPermanent("Jace, Telepath Unbound") shouldBe null
        }

        test("−3: cast the targeted instant from the graveyard this turn, and it's exiled after resolving") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Jace, Vryn's Prodigy")
                .withCardInHand(1, "Grizzly Bears")
                .withCardInLibrary(1, "Island")
                .withCardInGraveyard(1, "Lightning Bolt")
                .withCardInGraveyard(1, "Grizzly Bears")
                .withCardInGraveyard(1, "Grizzly Bears")
                .withCardInGraveyard(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.lootDiscardingBears()
            val telepath = game.findPermanent("Jace, Telepath Unbound").shouldNotBeNull()
            val opposingBears = game.findPermanent("Grizzly Bears").shouldNotBeNull()
            val bolt = game.findCardsInGraveyard(1, "Lightning Bolt").single()

            withClue("no permission yet: the Bolt can't be cast from the graveyard") {
                game.castSpellFromGraveyard(1, "Lightning Bolt", opposingBears).error shouldNotBe null
            }

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = telepath,
                    abilityId = minusThreeId,
                    targets = listOf(entityIdToChosenTarget(game.state, bolt)),
                )
            ).error shouldBe null
            game.resolveStack()
            game.state.getEntity(telepath)!!.get<CountersComponent>()!!
                .getCount(CounterType.LOYALTY) shouldBe 2

            withClue("the permission leaves the card where it is until it's cast") {
                game.isInGraveyard(1, "Lightning Bolt") shouldBe true
            }

            game.castSpellFromGraveyard(1, "Lightning Bolt", opposingBears).error shouldBe null
            if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
            game.resolveStack()

            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            withClue("the cast Bolt is exiled instead of returning to the graveyard") {
                game.isInGraveyard(1, "Lightning Bolt") shouldBe false
                game.isInExile(1, "Lightning Bolt") shouldBe true
            }
        }
    }
}
