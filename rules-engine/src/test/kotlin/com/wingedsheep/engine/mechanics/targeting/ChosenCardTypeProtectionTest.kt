package com.wingedsheep.engine.mechanics.targeting

import com.wingedsheep.engine.state.components.battlefield.ChoiceValue
import com.wingedsheep.engine.state.components.battlefield.withCastChoice
import com.wingedsheep.engine.state.components.player.PlayerProtectionComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantProtectionFromToGroup
import com.wingedsheep.sdk.scripting.GrantProtectionToController
import com.wingedsheep.sdk.scripting.ProtectionScope
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * `ProtectionScope.ChosenCardType` — protection bound to the card type the granting permanent
 * chose as it entered (CR 702.16: protection from a quality; a card type is such a quality). Pins
 * the three readings: the group grant projects `PROTECTION_FROM_CARDTYPE_<TYPE>` from the source's
 * choice, the controller grant protects the player from sources of that type, and with no choice
 * made (or no permanent to read it from) neither protects from anything.
 */
class ChosenCardTypeProtectionTest : ScenarioTestBase() {

    private val warden = card("Chosen Type Warden") {
        manaCost = "{3}{W}"
        typeLine = "Creature — Angel"
        power = 3
        toughness = 3
        staticAbility {
            ability = GrantProtectionFromToGroup(
                scope = ProtectionScope.ChosenCardType(),
                filter = GroupFilter(GameObjectFilter.Creature.youControl())
            )
        }
        staticAbility { ability = GrantProtectionToController(ProtectionScope.ChosenCardType()) }
    }

    private val construct = card("Test Construct") {
        manaCost = "{2}"
        typeLine = "Artifact Creature — Construct"
        power = 2
        toughness = 2
    }

    private fun board() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Chosen Type Warden")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardOnBattlefield(2, "Test Construct")
        .withCardOnBattlefield(2, "Hill Giant")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.choose(type: String) {
        val wardenId = findPermanent("Chosen Type Warden")!!
        state = state.updateEntity(wardenId) { it.withCastChoice(ChoiceSlot.CARD_TYPE, ChoiceValue.TextChoice(type)) }
    }

    private fun TestGame.playerProtectedFrom(name: String): Boolean =
        PlayerProtectionRules.isProtectedFromSource(
            state, player1Id, findPermanent(name), player2Id, predicateEvaluator = services.predicateEvaluator
        )

    init {
        cardRegistry.register(warden)
        cardRegistry.register(construct)

        test("with no type chosen, nothing is protected") {
            val game = board()
            val projected = game.state.projectedState
            projected.hasKeyword(game.findPermanent("Grizzly Bears")!!, "PROTECTION_FROM_CARDTYPE_ARTIFACT") shouldBe false
            game.playerProtectedFrom("Test Construct") shouldBe false
        }

        test("the chosen type protects your creatures and you, and only from that type") {
            val game = board()
            game.choose("Artifact")
            val projected = game.state.projectedState

            withClue("your creatures, the source included, gain protection from artifacts") {
                projected.hasKeyword(game.findPermanent("Grizzly Bears")!!, "PROTECTION_FROM_CARDTYPE_ARTIFACT") shouldBe true
                projected.hasKeyword(game.findPermanent("Chosen Type Warden")!!, "PROTECTION_FROM_CARDTYPE_ARTIFACT") shouldBe true
            }
            withClue("an opponent's creature doesn't") {
                projected.hasKeyword(game.findPermanent("Hill Giant")!!, "PROTECTION_FROM_CARDTYPE_ARTIFACT") shouldBe false
            }
            withClue("you're protected from an artifact source but not a non-artifact one") {
                game.playerProtectedFrom("Test Construct") shouldBe true
                game.playerProtectedFrom("Hill Giant") shouldBe false
            }
        }

        test("a choice made under another slot isn't read") {
            val game = board()
            val wardenId = game.findPermanent("Chosen Type Warden")!!
            game.state = game.state.updateEntity(wardenId) {
                it.withCastChoice(ChoiceSlot.MODE, ChoiceValue.TextChoice("Artifact"))
            }
            game.playerProtectedFrom("Test Construct") shouldBe false
            game.state.projectedState.hasKeyword(game.findPermanent("Grizzly Bears")!!, "PROTECTION_FROM_CARDTYPE_ARTIFACT") shouldBe false
        }

        test("an unbound chosen scope on a one-shot player grant protects from nothing") {
            val game = board()
            game.state = game.state.updateEntity(game.player2Id) {
                it.with(PlayerProtectionComponent(scopes = listOf(ProtectionScope.ChosenCardType())))
            }
            PlayerProtectionRules.isProtectedFromSource(
                game.state, game.player2Id, game.findPermanent("Grizzly Bears"), game.player1Id,
                predicateEvaluator = services.predicateEvaluator
            ) shouldBe false
        }
    }
}
