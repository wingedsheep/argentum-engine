package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.soi.cards.StartledAwake
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Startled Awake // Persistent Nightmare (SOI #88).
 *
 * Front ({2}{U}{U} Sorcery): "Target opponent mills thirteen cards. {3}{U}{U}: Put this card from
 * your graveyard onto the battlefield transformed. Activate only as a sorcery."
 * Back (1/1 Nightmare): "Skulk. When this creature deals combat damage to a player, return it to
 * its owner's hand."
 *
 * A sorcery-front transforming DFC: the front resolves to the graveyard, the graveyard ability
 * puts the same card onto the battlefield back face up, and the bounce trigger returns it to hand
 * as the castable sorcery again.
 */
class StartledAwakeScenarioTest : ScenarioTestBase() {

    private val returnAbilityId = StartledAwake.activatedAbilities.first().id

    init {
        test("cast from hand: target opponent mills thirteen and the sorcery goes to the graveyard") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Startled Awake")
                .withLandsOnBattlefield(1, "Island", 4)
                .withCardInLibrary(1, "Island")
                .apply { repeat(15) { withCardInLibrary(2, "Island") } }
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpellTargetingPlayer(1, "Startled Awake", 2).error shouldBe null
            game.resolveStack()

            withClue("the opponent milled exactly thirteen cards") {
                game.librarySize(2) shouldBe 2
                game.graveyardSize(2) shouldBe 13
            }
            withClue("the sorcery front resolved to its owner's graveyard, not the battlefield") {
                game.isInGraveyard(1, "Startled Awake") shouldBe true
                game.isOnBattlefield("Persistent Nightmare") shouldBe false
            }
        }

        test("from the graveyard it returns as Persistent Nightmare, which bounces itself to hand as Startled Awake") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInGraveyard(1, "Startled Awake")
                .withLandsOnBattlefield(1, "Island", 5)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val card = game.findCardsInGraveyard(1, "Startled Awake").single()
            game.execute(
                ActivateAbility(playerId = game.player1Id, sourceId = card, abilityId = returnAbilityId)
            ).error shouldBe null
            game.resolveStack()

            withClue("the same card is on the battlefield back face up") {
                game.findPermanent("Persistent Nightmare") shouldBe card
                game.isInGraveyard(1, "Startled Awake") shouldBe false
                val projected = game.state.projectedState
                projected.getPower(card) shouldBe 1
                projected.getToughness(card) shouldBe 1
                projected.hasKeyword(card, Keyword.SKULK) shouldBe true
            }

            // Let it attack this turn.
            game.state = game.state.updateEntity(card) { it.without<SummoningSicknessComponent>() }
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Persistent Nightmare" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
            game.resolveStack()

            withClue("it dealt 1 combat damage to the opponent") {
                game.getLifeTotal(2) shouldBe 19
            }
            withClue("its trigger returned it to its owner's hand, front face up") {
                game.isOnBattlefield("Persistent Nightmare") shouldBe false
                game.isInHand(1, "Startled Awake") shouldBe true
            }
        }
    }
}
