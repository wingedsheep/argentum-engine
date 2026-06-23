package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.core.NumberChosenResponse
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.CreatedByComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.atq.cards.Tetravus
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Tetravus (ATQ #71).
 *
 * {6} Artifact Creature — Construct 1/1, flying, enters with three +1/+1 counters.
 * Upkeep: may remove any number of +1/+1 counters → create that many 1/1 flying Tetravite tokens
 * (each "can't be enchanted"); may exile any number of tokens created with this creature → put that
 * many +1/+1 counters back on it. The Tetravite tokens are recognized by provenance, so only
 * Tetravus's own tokens can be reabsorbed.
 */
class TetravusScenarioTest : FunSpec({

    val projector = StateProjector()

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.registerCard(Tetravus)
        d.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.tetraviteTokens(creator: EntityId): List<EntityId> =
        state.getBattlefield().filter {
            state.getEntity(it)?.get<CardComponent>()?.name == "Tetravite" &&
                state.getEntity(it)?.get<CreatedByComponent>()?.creatorId == creator
        }

    fun plusOne(d: GameTestDriver, id: EntityId): Int =
        d.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    /**
     * Advance to player1's next upkeep, handling the two optional upkeep "may" prompts. For each
     * upkeep trigger: answer its YesNo with [accept]; if it then asks for a number, supply
     * [removeCount]; if it asks to select tokens, supply [selectTokens].
     */
    fun GameTestDriver.runMyUpkeep(
        tetravus: EntityId,
        acceptConvert: Boolean,
        removeCount: Int,
        acceptReabsorb: Boolean
    ) {
        var guard = 0
        // Walk to player1's upkeep, then drain its triggers.
        while (!(state.step == Step.UPKEEP && state.activePlayerId == player1) && guard++ < 80) {
            if (state.pendingDecision != null) autoResolveDecision() else bothPass()
        }
        guard = 0
        while (state.step == Step.UPKEEP && state.activePlayerId == player1 && guard++ < 40) {
            val d = state.pendingDecision
            when (d) {
                is YesNoDecision -> {
                    // First YesNo encountered is the convert trigger, second is reabsorb — but
                    // ordering is the controller's choice; decide by what's on the board.
                    val hasTokens = tetraviteTokens(tetravus).isNotEmpty()
                    val accept = if (hasTokens && plusOne(this, tetravus) == 0) acceptReabsorb
                        else if (!hasTokens) acceptConvert
                        else acceptConvert
                    submitYesNo(d.playerId, accept)
                }
                is ChooseNumberDecision -> submitDecision(d.playerId, NumberChosenResponse(d.id, removeCount))
                is SelectCardsDecision -> submitCardSelection(d.playerId, tetraviteTokens(tetravus))
                null -> {
                    if (state.stack.isNotEmpty()) bothPass() else bothPass()
                }
                else -> autoResolveDecision()
            }
        }
    }

    test("enters with three +1/+1 counters as a 4/4 flying") {
        val d = driver()
        val card = d.putCardInHand(d.player1, "Tetravus")
        d.giveColorlessMana(d.player1, 6)
        d.castSpell(d.player1, card)
        d.bothPass()
        val t = d.findPermanent(d.player1, "Tetravus")!!
        plusOne(d, t) shouldBe 3
        val p = projector.project(d.state)
        p.getPower(t) shouldBe 4
        p.getToughness(t) shouldBe 4
        p.hasKeyword(t, Keyword.FLYING) shouldBe true
    }

    test("removing counters in upkeep mints that many flying, can't-be-enchanted Tetravite tokens") {
        val d = driver()
        val card = d.putCardInHand(d.player1, "Tetravus")
        d.giveColorlessMana(d.player1, 6)
        d.castSpell(d.player1, card)
        d.bothPass()
        val t = d.findPermanent(d.player1, "Tetravus")!!
        plusOne(d, t) shouldBe 3

        // Next upkeep: accept convert, remove 2 counters -> 2 Tetravite tokens; decline reabsorb.
        d.runMyUpkeep(t, acceptConvert = true, removeCount = 2, acceptReabsorb = false)

        plusOne(d, t) shouldBe 1
        val tokens = d.tetraviteTokens(t)
        tokens.size shouldBe 2
        val p = projector.project(d.state)
        tokens.forEach { tok ->
            p.hasKeyword(tok, Keyword.FLYING) shouldBe true
            p.hasKeyword(tok, com.wingedsheep.sdk.core.AbilityFlag.CANT_BE_ENCHANTED) shouldBe true
        }
    }

    test("reabsorbing its own Tetravite tokens in upkeep puts that many +1/+1 counters back") {
        val d = driver()
        val card = d.putCardInHand(d.player1, "Tetravus")
        d.giveColorlessMana(d.player1, 6)
        d.castSpell(d.player1, card)
        d.bothPass()
        val t = d.findPermanent(d.player1, "Tetravus")!!

        // Upkeep 1: convert all 3 counters -> 3 tokens.
        d.runMyUpkeep(t, acceptConvert = true, removeCount = 3, acceptReabsorb = false)
        plusOne(d, t) shouldBe 0
        d.tetraviteTokens(t).size shouldBe 3

        // Upkeep 2 (next turn): decline convert (no counters anyway), accept reabsorb of all 3 tokens.
        d.runMyUpkeep(t, acceptConvert = false, removeCount = 0, acceptReabsorb = true)
        plusOne(d, t) shouldBe 3
        d.tetraviteTokens(t).size shouldBe 0
        val p = projector.project(d.state)
        p.getPower(t) shouldBe 4
        p.getToughness(t) shouldBe 4
    }
})
