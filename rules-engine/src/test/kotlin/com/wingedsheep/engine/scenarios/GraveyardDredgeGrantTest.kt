package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.core.engineSerializersModule
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GraveyardCardsHaveDredge
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.LoseAllAbilities
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.matchers.shouldBe
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** `GraveyardCardsHaveDredge`: a battlefield static granting dredge to its controller's graveyard cards. */
class GraveyardDredgeGrantTest : ScenarioTestBase() {
    init {
        cardRegistry.register(card("Test Land Dredge Granter") {
            manaCost = "{G}"
            typeLine = "Enchantment"
            staticAbility { ability = GraveyardCardsHaveDredge(GameObjectFilter.Land, amount = 2) }
        })
        cardRegistry.register(card("Test Creature Dredge Granter") {
            manaCost = "{G}"
            typeLine = "Enchantment"
            staticAbility { ability = GraveyardCardsHaveDredge(GameObjectFilter.Creature, amount = 3) }
        })
        cardRegistry.register(card("Test Creature Land Dredge Granter") {
            manaCost = "{G}"
            typeLine = "Creature — Plant"
            power = 1
            toughness = 1
            staticAbility { ability = GraveyardCardsHaveDredge(GameObjectFilter.Land, amount = 2) }
        })
        cardRegistry.register(card("Test Ability Wipe") {
            manaCost = "{W}"
            typeLine = "Enchantment"
            staticAbility { ability = LoseAllAbilities(GroupFilter(GameObjectFilter.Creature)) }
        })
        cardRegistry.register(card("Test Printed Dredger") {
            manaCost = "{G}"
            typeLine = "Creature — Plant"
            power = 1
            toughness = 1
            keywordAbility(KeywordAbility.dredge(1))
        })
        cardRegistry.register(card("Test Draw One") {
            manaCost = "{U}"
            typeLine = "Instant"
            spell { effect = Effects.DrawCards(1) }
        })
        cardRegistry.register(card("Test Opponent Draws") {
            manaCost = "{U}"
            typeLine = "Instant"
            spell { effect = Effects.DrawCards(1, EffectTarget.PlayerRef(Player.AnOpponent)) }
        })

        fun base() = scenario().withPlayers("P1", "P2")
            .withLandsOnBattlefield(1, "Island", 2)
            .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        test("a matching graveyard card gains dredge and replaces a draw") {
            val game = base().withCardOnBattlefield(1, "Test Land Dredge Granter")
                .withCardInHand(1, "Test Draw One")
                .withCardInGraveyard(1, "Forest")
                .withCardInLibrary(1, "Plains").withCardInLibrary(1, "Swamp")
                .withCardInLibrary(1, "Mountain").build()
            game.castSpell(1, "Test Draw One").error shouldBe null
            game.resolveStack()
            val decision = game.state.pendingDecision as YesNoDecision
            decision.context.sourceName shouldBe "Forest"
            decision.prompt shouldBe
                "Dredge 2 — Mill 2 cards and return Forest from your graveyard to your hand instead of drawing?"
            game.answerYesNo(true).error shouldBe null
            game.resolveStack()
            game.isInHand(1, "Forest") shouldBe true
            game.isInGraveyard(1, "Plains") shouldBe true
            game.isInGraveyard(1, "Swamp") shouldBe true
            game.state.getLibrary(game.player1Id).size shouldBe 1
        }

        test("non-matching cards and too-small libraries are never offered") {
            val game = base().withCardOnBattlefield(1, "Test Land Dredge Granter")
                .withCardInHand(1, "Test Draw One")
                .withCardInGraveyard(1, "Grizzly Bears")
                .withCardInLibrary(1, "Plains").build()
            game.castSpell(1, "Test Draw One").error shouldBe null
            game.resolveStack()
            game.state.pendingDecision shouldBe null
            game.isInHand(1, "Plains") shouldBe true
        }

        test("the grant reaches only its controller's graveyard") {
            val game = base().withCardOnBattlefield(1, "Test Land Dredge Granter")
                .withCardInHand(1, "Test Opponent Draws")
                .withCardInGraveyard(2, "Forest")
                .withCardInLibrary(2, "Plains").withCardInLibrary(2, "Swamp")
                .withCardInLibrary(2, "Mountain").build()
            game.castSpell(1, "Test Opponent Draws").error shouldBe null
            game.resolveStack()
            game.state.pendingDecision shouldBe null
            game.isInGraveyard(2, "Forest") shouldBe true
            game.state.getLibrary(game.player2Id).size shouldBe 2
        }

        test("the grant functions only while the granter is on the battlefield") {
            val game = base().withCardInGraveyard(1, "Test Land Dredge Granter")
                .withCardInHand(1, "Test Draw One")
                .withCardInGraveyard(1, "Forest")
                .withCardInLibrary(1, "Plains").withCardInLibrary(1, "Swamp")
                .withCardInLibrary(1, "Mountain").build()
            game.castSpell(1, "Test Draw One").error shouldBe null
            game.resolveStack()
            game.state.pendingDecision shouldBe null
            game.isInGraveyard(1, "Forest") shouldBe true
        }

        test("a granter that has lost all its abilities grants nothing") {
            val game = base().withCardOnBattlefield(1, "Test Creature Land Dredge Granter")
                .withCardOnBattlefield(2, "Test Ability Wipe")
                .withCardInHand(1, "Test Draw One")
                .withCardInGraveyard(1, "Forest")
                .withCardInLibrary(1, "Plains").withCardInLibrary(1, "Swamp")
                .withCardInLibrary(1, "Mountain").build()
            game.castSpell(1, "Test Draw One").error shouldBe null
            game.resolveStack()
            game.state.pendingDecision shouldBe null
            game.isInGraveyard(1, "Forest") shouldBe true
        }

        test("a card with printed and granted dredge offers each ability separately") {
            val game = base().withCardOnBattlefield(1, "Test Creature Dredge Granter")
                .withCardInHand(1, "Test Draw One")
                .withCardInGraveyard(1, "Test Printed Dredger")
                .withCardInLibrary(1, "Plains").withCardInLibrary(1, "Swamp")
                .withCardInLibrary(1, "Mountain").withCardInLibrary(1, "Forest").build()
            game.castSpell(1, "Test Draw One").error shouldBe null
            game.resolveStack()
            (game.state.pendingDecision as YesNoDecision).prompt.startsWith("Dredge 1") shouldBe true
            game.answerYesNo(false).error shouldBe null
            (game.state.pendingDecision as YesNoDecision).prompt.startsWith("Dredge 3") shouldBe true
            game.answerYesNo(true).error shouldBe null
            game.resolveStack()
            game.isInHand(1, "Test Printed Dredger") shouldBe true
            game.state.getLibrary(game.player1Id).size shouldBe 1
        }

        test("a granted dredge choice round-trips through saved-game serialization") {
            val game = base().withCardOnBattlefield(1, "Test Land Dredge Granter")
                .withCardInHand(1, "Test Draw One")
                .withCardInGraveyard(1, "Forest")
                .withCardInLibrary(1, "Plains").withCardInLibrary(1, "Swamp").build()
            game.castSpell(1, "Test Draw One").error shouldBe null
            game.resolveStack()
            val json = Json { serializersModule = engineSerializersModule; allowStructuredMapKeys = true }
            game.state = json.decodeFromString<GameState>(json.encodeToString(game.state))
            game.answerYesNo(true).error shouldBe null
            game.resolveStack()
            game.isInHand(1, "Forest") shouldBe true
            game.state.getLibrary(game.player1Id).size shouldBe 0
        }
    }
}
