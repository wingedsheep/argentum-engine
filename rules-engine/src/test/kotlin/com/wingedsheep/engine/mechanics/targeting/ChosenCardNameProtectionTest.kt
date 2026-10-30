package com.wingedsheep.engine.mechanics.targeting

import com.wingedsheep.engine.state.components.battlefield.ChoiceValue
import com.wingedsheep.engine.state.components.battlefield.withCastChoice
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.scripting.GrantProtectionFromToGroup
import com.wingedsheep.sdk.scripting.GrantProtectionToController
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ProtectionScope
import com.wingedsheep.sdk.scripting.effects.GrantPlayerProtectionEffect
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * `ProtectionScope.ChosenCardName` — "You have protection from the chosen card name" (Runed Halo;
 * CR 702.16: protection from a quality, here a name). Pins the reading against the granting
 * permanent's as-enters choice: only sources with exactly that name match — whoever controls them,
 * in any zone — a face-down source has no name and never matches, and with no name chosen (or a
 * choice under another slot) nothing is protected. The printed keyword and one-shot grant spellings
 * reject the scope, since only a permanent carries a choice to read.
 */
class ChosenCardNameProtectionTest : ScenarioTestBase() {

    private val warden = card("Chosen Name Warden") {
        manaCost = "{W}{W}"
        typeLine = "Enchantment"
        staticAbility { ability = GrantProtectionToController(ProtectionScope.ChosenCardName()) }
    }

    private fun board() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Chosen Name Warden")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withCardOnBattlefield(2, "Hill Giant")
        .withCardInHand(2, "Lightning Bolt")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.choose(name: String, slot: ChoiceSlot = ChoiceSlot.CARD_NAME) {
        val wardenId = findPermanent("Chosen Name Warden")!!
        state = state.updateEntity(wardenId) { it.withCastChoice(slot, ChoiceValue.TextChoice(name)) }
    }

    private fun TestGame.protectedFrom(sourceId: EntityId?, casterId: EntityId = player2Id): Boolean =
        PlayerProtectionRules.isProtectedFromSource(
            state, player1Id, sourceId, casterId, predicateEvaluator = services.predicateEvaluator
        )

    private fun TestGame.opponentsBears(): EntityId =
        findPermanents("Grizzly Bears").first { state.projectedState.getController(it) == player2Id }

    private fun TestGame.ownBears(): EntityId =
        findPermanents("Grizzly Bears").first { state.projectedState.getController(it) == player1Id }

    init {
        cardRegistry.register(warden)

        test("with no name chosen, nothing is protected") {
            val game = board()
            game.protectedFrom(game.opponentsBears()) shouldBe false
            game.protectedFrom(game.findPermanent("Hill Giant")) shouldBe false
        }

        test("the chosen name protects you from sources with that name, and only those") {
            val game = board()
            game.choose("Grizzly Bears")

            withClue("an opponent's source with the chosen name") {
                game.protectedFrom(game.opponentsBears()) shouldBe true
            }
            withClue("a source with another name") {
                game.protectedFrom(game.findPermanent("Hill Giant")) shouldBe false
            }
            withClue("the quality is the name, not the controller — your own Grizzly Bears matches too") {
                game.protectedFrom(game.ownBears(), casterId = game.player1Id) shouldBe true
            }
        }

        test("the name matches off the battlefield — a spell in hand or on the stack") {
            val game = board()
            game.choose("Lightning Bolt")
            game.protectedFrom(game.findCardsInHand(2, "Lightning Bolt").first()) shouldBe true
            game.protectedFrom(game.opponentsBears()) shouldBe false
        }

        test("a face-down source has no name, so it never matches") {
            val game = board()
            game.choose("Grizzly Bears")
            val bears = game.opponentsBears()
            game.state = game.state.updateEntity(bears) { it.with(FaceDownComponent) }
            game.protectedFrom(bears) shouldBe false
        }

        test("a name stored under another slot isn't read") {
            val game = board()
            game.choose("Grizzly Bears", slot = ChoiceSlot.MODE)
            game.protectedFrom(game.opponentsBears()) shouldBe false
        }

        test("only a permanent's static can carry the chosen name") {
            shouldThrow<IllegalArgumentException> { KeywordAbility.Protection(ProtectionScope.ChosenCardName()) }
            shouldThrow<IllegalArgumentException> { KeywordAbility.Hexproof(ProtectionScope.ChosenCardName()) }
            shouldThrow<IllegalArgumentException> { GrantPlayerProtectionEffect(scope = ProtectionScope.ChosenCardName()) }
            shouldThrow<IllegalArgumentException> { GrantProtectionFromToGroup(ProtectionScope.ChosenCardName()) }
        }
    }
}
