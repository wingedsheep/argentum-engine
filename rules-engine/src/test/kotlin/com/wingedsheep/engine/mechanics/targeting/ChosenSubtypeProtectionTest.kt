package com.wingedsheep.engine.mechanics.targeting

import com.wingedsheep.engine.state.components.battlefield.ChoiceValue
import com.wingedsheep.engine.state.components.battlefield.withCastChoice
import com.wingedsheep.engine.state.components.player.PlayerProtectionComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
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
import io.kotest.matchers.shouldNotBe

/**
 * `ProtectionScope.ChosenSubtype` — protection bound to the subtype the granting permanent chose
 * as it entered, narrowed to a card type ("protection from creatures of the chosen type", CR
 * 702.16a: a subtype is a quality protection can name). Pins: the group grant projects the same
 * `PROTECTION_FROM_SUBTYPE_<X>@CREATURE` keyword a fixed `Subtype` scope does; the card-type
 * narrowing excludes a noncreature of the type; a changeling creature has every creature type and
 * matches; the controller grant protects the player; and with no choice made (or no permanent to
 * read it from) nothing is protected.
 */
class ChosenSubtypeProtectionTest : ScenarioTestBase() {

    private val keyword = TypeQualityProtection.subtypeKeyword("Goblin", "Creature")

    private val captain = card("Chosen Subtype Captain") {
        manaCost = "{2}{W}{W}"
        typeLine = "Creature — Human Knight"
        power = 3
        toughness = 3
        staticAbility {
            ability = GrantProtectionFromToGroup(
                scope = ProtectionScope.ChosenSubtype(),
                filter = GroupFilter(GameObjectFilter.Creature.withSubtype("Human").youControl())
            )
        }
        staticAbility { ability = GrantProtectionToController(ProtectionScope.ChosenSubtype()) }
    }

    private val human = card("Test Human Squire") {
        manaCost = "{W}"
        typeLine = "Creature — Human Soldier"
        power = 1
        toughness = 1
    }

    private val goblin = card("Test Goblin Raider") {
        manaCost = "{R}"
        typeLine = "Creature — Goblin Warrior"
        power = 2
        toughness = 1
    }

    private val shapeshifter = card("Test Mimic") {
        manaCost = "{1}"
        typeLine = "Creature — Shapeshifter"
        power = 1
        toughness = 1
        keywords(Keyword.CHANGELING)
    }

    private val goblinShrine = card("Test Goblin Totem") {
        manaCost = "{1}"
        typeLine = "Kindred Artifact — Goblin"
    }

    private fun board() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Chosen Subtype Captain")
        .withCardOnBattlefield(1, "Test Human Squire", summoningSickness = false)
        .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
        .withCardOnBattlefield(2, "Test Goblin Raider")
        .withCardOnBattlefield(2, "Test Mimic")
        .withCardOnBattlefield(2, "Test Goblin Totem")
        .withCardOnBattlefield(2, "Hill Giant")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.choose(type: String, slot: ChoiceSlot = ChoiceSlot.CREATURE_TYPE) {
        val captainId = findPermanent("Chosen Subtype Captain")!!
        state = state.updateEntity(captainId) { it.withCastChoice(slot, ChoiceValue.TextChoice(type)) }
    }

    private fun TestGame.playerProtectedFrom(name: String): Boolean =
        PlayerProtectionRules.isProtectedFromSource(
            state, player1Id, findPermanent(name), player2Id, predicateEvaluator = services.predicateEvaluator
        )

    init {
        cardRegistry.register(captain)
        cardRegistry.register(human)
        cardRegistry.register(goblin)
        cardRegistry.register(shapeshifter)
        cardRegistry.register(goblinShrine)

        test("with no type chosen, nothing is protected") {
            val game = board()
            game.state.projectedState.hasKeyword(game.findPermanent("Test Human Squire")!!, keyword) shouldBe false
            game.playerProtectedFrom("Test Goblin Raider") shouldBe false
        }

        test("the chosen type protects your Humans, the source included, and only them") {
            val game = board()
            game.choose("Goblin")
            val projected = game.state.projectedState

            withClue("Humans you control gain protection from Goblin creatures") {
                projected.hasKeyword(game.findPermanent("Test Human Squire")!!, keyword) shouldBe true
                projected.hasKeyword(game.findPermanent("Chosen Subtype Captain")!!, keyword) shouldBe true
            }
            withClue("a non-Human you control doesn't") {
                projected.hasKeyword(game.findPermanent("Grizzly Bears")!!, keyword) shouldBe false
            }
        }

        test("a Goblin creature and a changeling can't block a protected Human; others can") {
            val game = board()
            game.choose("Goblin")
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Test Human Squire" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            withClue("Goblin creature can't block") {
                game.declareBlockers(mapOf("Test Goblin Raider" to listOf("Test Human Squire"))).error shouldNotBe null
            }
            withClue("a changeling has every creature type, so it's a Goblin creature too") {
                game.declareBlockers(mapOf("Test Mimic" to listOf("Test Human Squire"))).error shouldNotBe null
            }
            withClue("a non-Goblin can block") {
                game.declareBlockers(mapOf("Hill Giant" to listOf("Test Human Squire"))).error shouldBe null
            }
        }

        test("the controller grant covers Goblin creatures but not a noncreature Goblin") {
            val game = board()
            game.choose("Goblin")
            game.playerProtectedFrom("Test Goblin Raider") shouldBe true
            game.playerProtectedFrom("Test Mimic") shouldBe true
            game.playerProtectedFrom("Test Goblin Totem") shouldBe false
            game.playerProtectedFrom("Hill Giant") shouldBe false
        }

        test("a choice made under another slot isn't read") {
            val game = board()
            game.choose("Goblin", ChoiceSlot.CARD_TYPE)
            game.state.projectedState.hasKeyword(game.findPermanent("Test Human Squire")!!, keyword) shouldBe false
            game.playerProtectedFrom("Test Goblin Raider") shouldBe false
        }

        test("an unbound chosen subtype on a one-shot player grant protects from nothing") {
            val game = board()
            game.state = game.state.updateEntity(game.player2Id) {
                it.with(PlayerProtectionComponent.of(listOf(ProtectionScope.ChosenSubtype())))
            }
            PlayerProtectionRules.isProtectedFromSource(
                game.state, game.player2Id, game.findPermanent("Test Human Squire"), game.player1Id,
                predicateEvaluator = services.predicateEvaluator
            ) shouldBe false
        }
    }
}
