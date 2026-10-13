package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.dmu.cards.VoharVodalianDesecrator
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Vohar, Vodalian Desecrator (DMU #226).
 *
 * {T}: loot, draining 1 if the card *actually discarded* was an instant or sorcery. {2}, sacrifice
 * Vohar: you may cast target instant or sorcery card from your graveyard this turn, and it's exiled
 * instead of going back to the graveyard.
 */
class VoharVodalianDesecratorScenarioTest : ScenarioTestBase() {

    private val lootId = VoharVodalianDesecrator.activatedAbilities[0].id
    private val flashbackId = VoharVodalianDesecrator.activatedAbilities[1].id

    private fun lootBoard(): TestGame = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Vohar, Vodalian Desecrator")
        .withCardInHand(1, "Grizzly Bears")
        .withCardInLibrary(1, "Lightning Bolt") // drawn by the loot
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.lootDiscarding(name: String) {
        val vohar = findPermanent("Vohar, Vodalian Desecrator")!!
        execute(ActivateAbility(player1Id, vohar, lootId)).error shouldBe null
        resolveStack()
        selectCards(listOf(findCardsInHand(1, name).single())).error shouldBe null
        resolveStack()
        isInGraveyard(1, name) shouldBe true
    }

    init {
        test("discarding an instant drains each opponent for 1") {
            val game = lootBoard()
            game.lootDiscarding("Lightning Bolt")
            game.getLifeTotal(1) shouldBe 21
            game.getLifeTotal(2) shouldBe 19
        }

        test("discarding a creature card drains nothing") {
            val game = lootBoard()
            game.lootDiscarding("Grizzly Bears")
            game.getLifeTotal(1) shouldBe 20
            game.getLifeTotal(2) shouldBe 20
        }

        test("sacrifice: cast the targeted instant from the graveyard, and it's exiled after resolving") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Vohar, Vodalian Desecrator")
                .withCardInGraveyard(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val vohar = game.findPermanent("Vohar, Vodalian Desecrator")!!
            val bolt = game.findCardsInGraveyard(1, "Lightning Bolt").single()

            withClue("no permission yet: the Bolt can't be cast from the graveyard") {
                game.castSpellFromGraveyard(1, "Lightning Bolt", game.findPermanent("Grizzly Bears")!!)
                    .error shouldNotBe null
            }

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = vohar,
                    abilityId = flashbackId,
                    targets = listOf(entityIdToChosenTarget(game.state, bolt)),
                )
            ).error shouldBe null
            game.isInGraveyard(1, "Vohar, Vodalian Desecrator") shouldBe true
            game.resolveStack()

            game.castSpellFromGraveyard(1, "Lightning Bolt", game.findPermanent("Grizzly Bears")!!)
                .error shouldBe null
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
