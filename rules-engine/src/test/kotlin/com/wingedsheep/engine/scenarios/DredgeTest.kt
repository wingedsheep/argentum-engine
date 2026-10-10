package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.core.engineSerializersModule
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.scripting.ReplaceDrawWith
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class DredgeTest : ScenarioTestBase() {
    init {
        for (amount in listOf(0, 1, 3)) {
            cardRegistry.register(card("Test Dredger $amount") {
                manaCost = "{G}"
                typeLine = "Creature — Plant"
                power = 1
                toughness = 1
                keywordAbility(KeywordAbility.dredge(amount))
            })
        }
        for (count in 1..2) {
            cardRegistry.register(card("Test Draw $count") {
                manaCost = "{U}"
                typeLine = "Instant"
                spell { effect = Effects.DrawCards(count) }
            })
        }
        cardRegistry.register(card("Test Opponent Draw") {
            manaCost = "{U}"
            typeLine = "Instant"
            spell { effect = Effects.DrawCards(1, EffectTarget.PlayerRef(com.wingedsheep.sdk.scripting.references.Player.AnOpponent)) }
        })

        cardRegistry.register(card("Test Scry Replacement") {
            manaCost = "{U}"
            typeLine = "Enchantment"
            replacementEffect(ReplaceDrawWith(
                replacementEffect = Patterns.Library.scry(1), optional = true
            ))
        })

        fun base() = scenario().withPlayers("P1", "P2")
            .withLandsOnBattlefield(1, "Island", 2)
            .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        test("accepting dredge mills the exact count and returns the source instead of drawing") {
            val game = base().withCardInHand(1, "Test Draw 1")
                .withCardInGraveyard(1, "Test Dredger 3")
                .withCardInLibrary(1, "Forest").withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Mountain").withCardInLibrary(1, "Swamp").build()
            game.castSpell(1, "Test Draw 1").error shouldBe null
            game.resolveStack()
            (game.state.pendingDecision as YesNoDecision).playerId shouldBe game.player1Id
            (game.state.pendingDecision as YesNoDecision).prompt shouldBe
                "Dredge 3 — Mill 3 cards and return Test Dredger 3 from your graveyard to your hand instead of drawing?"
            game.answerYesNo(true).error shouldBe null
            game.resolveStack()
            game.isInHand(1, "Test Dredger 3") shouldBe true
            game.state.getLibrary(game.player1Id).size shouldBe 1
            game.isInGraveyard(1, "Forest") shouldBe true
            game.isInGraveyard(1, "Island") shouldBe true
            game.isInGraveyard(1, "Mountain") shouldBe true
            game.state.getHand(game.player1Id).size shouldBe 1
        }

        test("declining dredge performs the original draw") {
            val game = base().withCardInHand(1, "Test Draw 1")
                .withCardInGraveyard(1, "Test Dredger 3")
                .withCardInLibrary(1, "Forest").withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Mountain").build()
            game.castSpell(1, "Test Draw 1").error shouldBe null
            game.resolveStack()
            game.answerYesNo(false).error shouldBe null
            game.resolveStack()
            game.isInGraveyard(1, "Test Dredger 3") shouldBe true
            game.isInHand(1, "Forest") shouldBe true
            game.state.getLibrary(game.player1Id).size shouldBe 2
        }

        test("fewer than N library cards never offers dredge") {
            val game = base().withCardInHand(1, "Test Draw 1")
                .withCardInGraveyard(1, "Test Dredger 3")
                .withCardInLibrary(1, "Forest").withCardInLibrary(1, "Island").build()
            game.castSpell(1, "Test Draw 1").error shouldBe null
            game.resolveStack()
            game.state.pendingDecision shouldBe null
            game.isInHand(1, "Forest") shouldBe true
            game.isInGraveyard(1, "Test Dredger 3") shouldBe true
        }

        test("a newly milled dredger is available for the next draw of a multi-card instruction") {
            val game = base().withCardInHand(1, "Test Draw 2")
                .withCardInGraveyard(1, "Test Dredger 3")
                .withCardInLibrary(1, "Test Dredger 1").withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Mountain").withCardInLibrary(1, "Swamp")
                .withCardInLibrary(1, "Forest").build()
            game.castSpell(1, "Test Draw 2").error shouldBe null
            game.resolveStack()
            game.answerYesNo(true).error shouldBe null
            (game.state.pendingDecision as YesNoDecision).context.sourceName shouldBe "Test Dredger 1"
            game.answerYesNo(true).error shouldBe null
            game.resolveStack()
            game.isInHand(1, "Test Dredger 3") shouldBe true
            game.isInHand(1, "Test Dredger 1") shouldBe true
            game.state.getLibrary(game.player1Id).size shouldBe 1
        }

        test("declining one dredger and choosing another preserves remaining draws") {
            val game = base().withCardInHand(1, "Test Draw 2")
                .withCardInGraveyard(1, "Test Dredger 3")
                .withCardInGraveyard(1, "Test Dredger 1")
                .withCardInLibrary(1, "Forest").withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Mountain").withCardInLibrary(1, "Swamp")
                .withCardInLibrary(1, "Plains").build()
            game.castSpell(1, "Test Draw 2").error shouldBe null
            game.resolveStack()
            game.answerYesNo(false).error shouldBe null
            (game.state.pendingDecision as YesNoDecision).context.sourceName shouldBe "Test Dredger 1"
            game.answerYesNo(true).error shouldBe null
            (game.state.pendingDecision as YesNoDecision).context.sourceName shouldBe "Test Dredger 3"
            game.answerYesNo(false).error shouldBe null
            game.resolveStack()
            game.state.getHand(game.player1Id).size shouldBe 2
            game.state.getLibrary(game.player1Id).size shouldBe 3
        }

        test("dredge only applies to its owner's draw, even when an opponent controls the spell") {
            val game = base().withCardInHand(1, "Test Opponent Draw")
                .withCardInGraveyard(1, "Test Dredger 1")
                .withCardInGraveyard(2, "Test Dredger 3")
                .withCardInLibrary(2, "Forest").withCardInLibrary(2, "Island")
                .withCardInLibrary(2, "Mountain").build()
            game.castSpell(1, "Test Opponent Draw").error shouldBe null
            game.resolveStack()
            (game.state.pendingDecision as YesNoDecision).playerId shouldBe game.player2Id
            game.answerYesNo(true).error shouldBe null
            game.resolveStack()
            game.isInHand(2, "Test Dredger 3") shouldBe true
            game.isInGraveyard(1, "Test Dredger 1") shouldBe true
        }

        test("the printed ability is inactive in hand and on the battlefield") {
            val game = base().withCardInHand(1, "Test Draw 1")
                .withCardInHand(1, "Test Dredger 1")
                .withCardOnBattlefield(1, "Test Dredger 3")
                .withCardInLibrary(1, "Forest").withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Mountain").build()
            game.castSpell(1, "Test Draw 1").error shouldBe null
            game.resolveStack()
            game.state.pendingDecision shouldBe null
            game.isInHand(1, "Forest") shouldBe true
        }

        test("a pending dredge choice round-trips through saved game serialization") {
            val game = base().withCardInHand(1, "Test Draw 1")
                .withCardInGraveyard(1, "Test Dredger 1")
                .withCardInLibrary(1, "Forest").build()
            game.castSpell(1, "Test Draw 1").error shouldBe null
            game.resolveStack()
            val json = Json { serializersModule = engineSerializersModule; allowStructuredMapKeys = true }
            game.state = json.decodeFromString<GameState>(json.encodeToString(game.state))
            game.answerYesNo(true).error shouldBe null
            game.resolveStack()
            game.isInHand(1, "Test Dredger 1") shouldBe true
            game.state.getLibrary(game.player1Id).size shouldBe 0
        }

        test("dredge zero can replace drawing from an empty library") {
            val game = base().withCardInHand(1, "Test Draw 1")
                .withCardInGraveyard(1, "Test Dredger 0").build()
            game.castSpell(1, "Test Draw 1").error shouldBe null
            game.resolveStack()
            game.answerYesNo(true).error shouldBe null
            game.resolveStack()
            game.isInHand(1, "Test Dredger 0") shouldBe true
        }

        test("dredge replaces the turn-based draw and restores priority") {
            val game = scenario().withPlayers("P1", "P2")
                .withActivePlayer(1).withTurnNumber(3)
                .inPhase(Phase.BEGINNING, Step.UPKEEP)
                .withCardInGraveyard(1, "Test Dredger 1")
                .withCardInLibrary(1, "Forest").build()
            game.passPriority().error shouldBe null
            game.passPriority().error shouldBe null
            game.state.step shouldBe Step.DRAW
            (game.state.pendingDecision as YesNoDecision).playerId shouldBe game.player1Id
            game.answerYesNo(true).error shouldBe null
            game.isInHand(1, "Test Dredger 1") shouldBe true
            game.state.priorityPlayerId shouldBe game.player1Id
        }

        test("replacing the milled cards' destination still completes dredge") {
            val game = base().withCardInHand(1, "Test Draw 1")
                .withCardOnBattlefield(2, "Leyline of the Void")
                .withCardInGraveyard(1, "Test Dredger 1")
                .withCardInLibrary(1, "Forest").build()
            val forest = game.findCardsInLibrary(1, "Forest").single()
            game.castSpell(1, "Test Draw 1").error shouldBe null
            game.resolveStack()
            game.answerYesNo(true).error shouldBe null
            game.resolveStack()
            game.isInHand(1, "Test Dredger 1") shouldBe true
            game.state.getExile(game.player1Id).contains(forest) shouldBe true
        }

        test("an accepted optional replacement can pause without losing the remaining draw") {
            val game = base().withCardInHand(1, "Test Draw 2")
                .withCardOnBattlefield(1, "Test Scry Replacement")
                .withCardInLibrary(1, "Forest").withCardInLibrary(1, "Island").build()
            game.castSpell(1, "Test Draw 2").error shouldBe null
            game.resolveStack()
            game.answerYesNo(true).error shouldBe null
            game.selectCards(emptyList()).error shouldBe null
            (game.state.pendingDecision as YesNoDecision).context.sourceName shouldBe "Test Scry Replacement"
            game.answerYesNo(false).error shouldBe null
            game.resolveStack()
            game.isInHand(1, "Forest") shouldBe true
            game.state.getLibrary(game.player1Id).size shouldBe 1
        }

        test("declining every eligible dredger offers each once and then draws normally") {
            val game = base().withCardInHand(1, "Test Draw 1")
                .withCardInGraveyard(1, "Test Dredger 3")
                .withCardInGraveyard(1, "Test Dredger 1")
                .withCardInGraveyard(1, "Test Dredger 0")
                .withCardInLibrary(1, "Forest").withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Mountain").build()
            game.castSpell(1, "Test Draw 1").error shouldBe null
            game.resolveStack()
            for (amount in listOf(3, 1, 0)) {
                (game.state.pendingDecision as YesNoDecision).context.sourceName shouldBe "Test Dredger $amount"
                game.answerYesNo(false).error shouldBe null
            }
            game.resolveStack()
            game.state.pendingDecision shouldBe null
            game.isInHand(1, "Forest") shouldBe true
            game.state.getLibrary(game.player1Id).size shouldBe 2
        }

        test("multiple declines survive saved-game serialization and clear for the next draw") {
            val game = base().withCardInHand(1, "Test Draw 2")
                .withCardInGraveyard(1, "Test Dredger 3")
                .withCardInGraveyard(1, "Test Dredger 1")
                .withCardInGraveyard(1, "Test Dredger 0")
                .withCardInLibrary(1, "Forest").withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Mountain").withCardInLibrary(1, "Swamp").build()
            game.castSpell(1, "Test Draw 2").error shouldBe null
            game.resolveStack()
            game.answerYesNo(false).error shouldBe null
            game.answerYesNo(false).error shouldBe null
            (game.state.pendingDecision as YesNoDecision).context.sourceName shouldBe "Test Dredger 0"
            val json = Json { serializersModule = engineSerializersModule; allowStructuredMapKeys = true }
            game.state = json.decodeFromString<GameState>(json.encodeToString(game.state))
            game.answerYesNo(true).error shouldBe null
            (game.state.pendingDecision as YesNoDecision).context.sourceName shouldBe "Test Dredger 3"
            game.answerYesNo(true).error shouldBe null
            game.resolveStack()
            game.isInHand(1, "Test Dredger 0") shouldBe true
            game.isInHand(1, "Test Dredger 3") shouldBe true
            game.isInGraveyard(1, "Test Dredger 1") shouldBe true
            game.state.getLibrary(game.player1Id).size shouldBe 1
        }
    }
}
