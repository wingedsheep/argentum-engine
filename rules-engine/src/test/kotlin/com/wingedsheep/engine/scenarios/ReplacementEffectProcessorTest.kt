package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ReplacementChoiceContinuation
import com.wingedsheep.engine.replacement.PendingGameEvent
import com.wingedsheep.engine.replacement.ProcessorResult
import com.wingedsheep.engine.replacement.ReplacementEffectProcessor
import com.wingedsheep.engine.replacement.ReplacementOutcome
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.ReplacementEffectSourceComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.ModifyDrawAmount
import com.wingedsheep.sdk.scripting.PreventDraw
import com.wingedsheep.sdk.scripting.ReplacementEffect
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class ReplacementEffectProcessorTest : ScenarioTestBase() {

    fun GameTestDriver.addPermanentWithReplacement(
        playerId: EntityId,
        name: String,
        effect: ReplacementEffect
    ): EntityId {
        val permanentId = EntityId.generate()
        val container = ComponentContainer.of(
            CardComponent(
                cardDefinitionId = name,
                name = name,
                manaCost = ManaCost.ZERO,
                typeLine = TypeLine.parse("Enchantment"),
                oracleText = effect.description,
                colors = emptySet(),
                ownerId = playerId,
            ),
            OwnerComponent(playerId),
            ControllerComponent(playerId),
            ReplacementEffectSourceComponent(listOf(effect))
        )
        var newState = state.withEntity(permanentId, container)
        newState = newState.addToZone(ZoneKey(playerId, Zone.BATTLEFIELD), permanentId)
        replaceState(newState)
        return permanentId
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    init {
        test("zero matches — processor returns pass for unrelated event") {
            val driver = createDriver()
            driver.initMirrorMatch(deck = Deck.of("Plains" to 20))
            val playerId = driver.activePlayer!!

            val processor = ReplacementEffectProcessor()
            val event = PendingGameEvent.DrawPending(playerId, 1)

            val result = processor.process(driver.state, event)
            withClue("No replacement effects on the battlefield") {
                (result is ProcessorResult.Pass) shouldBe true
            }
        }

        test("single ModifyDrawAmount increases draw count") {
            val driver = createDriver()
            driver.initMirrorMatch(deck = Deck.of("Plains" to 20))
            val playerId = driver.activePlayer!!

            driver.addPermanentWithReplacement(
                playerId, "Draw Booster",
                ModifyDrawAmount(modifier = 1, appliesTo = EventPattern.DrawCardsEvent(Player.You))
            )

            val processor = ReplacementEffectProcessor()
            val event = PendingGameEvent.DrawAmountPending(playerId, 1)

            val result = processor.process(driver.state, event)
            val resolved = result as ProcessorResult.Resolved
            resolved.outcome shouldBe ReplacementOutcome.Modified(
                PendingGameEvent.DrawAmountPending(playerId, totalCount = 2)
            )
        }

        test("single PreventDraw consumes draw") {
            val driver = createDriver()
            driver.initMirrorMatch(deck = Deck.of("Plains" to 20))
            val playerId = driver.activePlayer!!

            driver.addPermanentWithReplacement(
                playerId, "Draw Preventer",
                PreventDraw(appliesTo = EventPattern.DrawEvent(Player.You))
            )

            val processor = ReplacementEffectProcessor()
            val event = PendingGameEvent.DrawPending(playerId, 1)

            val result = processor.process(driver.state, event)
            val resolved = result as ProcessorResult.Resolved
            resolved.outcome shouldBe ReplacementOutcome.Consumed
        }

        test("ModifyDrawAmount adjusts total at announcement; PreventDraw catches via recursion") {
            // Both ModifyDrawAmount(+1) and PreventDraw use DrawCardsEvent, so they
            // both match DrawAmountPending at the announcement level (CR 121.2a).
            // They compete in the ANY group → the processor presents a choice.
            // Whichever is chosen, PreventDraw ultimately catches the draw:
            //  - Pick PreventDraw first → immediate Consumed
            //  - Pick ModifyDrawAmount first → Modified(totalCount=2), recursive check
            //    finds PreventDraw still fresh → Consumed
            //
            // This test verifies through the full processor pipeline (not applySingle):
            // 1. process() → Paused with correct choice structure
            // 2. Stamp the ModifyDrawAmount identity onto the state chain to simulate
            //    "ModifyDrawAmount was applied"; re-process → only PreventDraw is fresh
            //    → PreventDraw fires → Consumed
            val driver = createDriver()
            driver.initMirrorMatch(deck = Deck.of("Plains" to 20))
            val playerId = driver.activePlayer!!

            driver.addPermanentWithReplacement(
                playerId, "Draw Booster",
                ModifyDrawAmount(modifier = 1, appliesTo = EventPattern.DrawCardsEvent(Player.You))
            )
            driver.addPermanentWithReplacement(
                playerId, "Draw Preventer",
                PreventDraw(appliesTo = EventPattern.DrawCardsEvent(Player.You))
            )

            val processor = ReplacementEffectProcessor()
            val event = PendingGameEvent.DrawAmountPending(playerId, totalCount = 1)

            // Full process: both match → Paused with choice (CR 616.1e)
            val result = processor.process(driver.state, event)
            (result is ProcessorResult.Paused) shouldBe true
            val paused = result as ProcessorResult.Paused

            val continuation = paused.state.continuationStack
                .filterIsInstance<ReplacementChoiceContinuation>()
                .firstOrNull()
            continuation shouldNotBe null
            continuation!!.options.size shouldBe 2

            // Simulate "ModifyDrawAmount was applied first": stamp its identity on
            // the activeReplacementChain so re-processing filters it out, leaving
            // only PreventDraw fresh.
            val modifyIdentity = continuation.options[0].identity
            val stateAfterModify = driver.state.copy(activeReplacementChain = setOf(modifyIdentity))

            // Re-process: PreventDraw is the only fresh effect → fires → Consumed
            val recursiveResult = processor.process(stateAfterModify, event)
            val recursiveResolved = recursiveResult as ProcessorResult.Resolved
            withClue("ModifyDrawAmount made totalCount=2, then PreventDraw consumed") {
                recursiveResolved.outcome shouldBe ReplacementOutcome.Consumed
            }
        }

        test("ModifyDrawAmount alone adjusts totalCount at announcement") {
            // A single ModifyDrawAmount(+1) with DrawCardsEvent matches
            // DrawAmountPending and adjusts the total from 1 to 2.
            val driver = createDriver()
            driver.initMirrorMatch(deck = Deck.of("Plains" to 20))
            val playerId = driver.activePlayer!!

            driver.addPermanentWithReplacement(
                playerId, "Draw Booster",
                ModifyDrawAmount(modifier = 1, appliesTo = EventPattern.DrawCardsEvent(Player.You))
            )

            val processor = ReplacementEffectProcessor()
            val event = PendingGameEvent.DrawAmountPending(playerId, totalCount = 1)

            val result = processor.process(driver.state, event)
            val resolved = result as ProcessorResult.Resolved
            resolved.outcome shouldBe ReplacementOutcome.Modified(
                PendingGameEvent.DrawAmountPending(playerId, totalCount = 2)
            )
        }

        test("gatherReplacements, opponent's 'You' effects don't match active player") {
            val driver = createDriver()
            driver.initMirrorMatch(deck = Deck.of("Plains" to 20))
            val playerId = driver.activePlayer!!
            val opponentId = driver.getOpponent(playerId)

            driver.addPermanentWithReplacement(
                opponentId, "Opponent Booster",
                ModifyDrawAmount(modifier = 1, appliesTo = EventPattern.DrawCardsEvent(Player.You))
            )

            val processor = ReplacementEffectProcessor()
            val event = PendingGameEvent.DrawAmountPending(playerId, 1)

            val gathered = processor.gatherReplacements(driver.state, event)
            gathered.size shouldBe 0
        }

        test("gatherReplacements, EachOpponent filter matches opponent's draw") {
            val driver = createDriver()
            driver.initMirrorMatch(deck = Deck.of("Plains" to 20))
            val playerId = driver.activePlayer!!
            val opponentId = driver.getOpponent(playerId)

            driver.addPermanentWithReplacement(
                opponentId, "Opponent Modifier",
                ModifyDrawAmount(modifier = 1, appliesTo = EventPattern.DrawCardsEvent(Player.EachOpponent))
            )

            val processor = ReplacementEffectProcessor()
            // Active player draws → opponent's EachOpponent matches
            val gathered = processor.gatherReplacements(
                driver.state, PendingGameEvent.DrawAmountPending(playerId, 1)
            )
            gathered.size shouldBe 1

            // Opponent draws → opponent's EachOpponent does NOT match (source controller is drawing)
            val gathered2 = processor.gatherReplacements(
                driver.state, PendingGameEvent.DrawAmountPending(opponentId, 1)
            )
            gathered2.size shouldBe 0
        }

        test("Quantum Riddler ETB draws 2 with +1 modifier") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Quantum Riddler")
                .withLandsOnBattlefield(1, "Island", 5)
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            // Cast Riddler {3}{U}{U}.
            // Hand was 1 (Riddler) → 0 after casting. ETB draw 1 + ModifyDrawAmount(+1)
            // (condition: CardsInHandAtMost(1), 0 ≤ 1) → draws 2 cards.
            val cast = game.castSpell(1, "Quantum Riddler")
            cast.error shouldBe null
            game.resolveStack()

            game.handSize(1) shouldBe 2
        }

        test("PreventDraw blocks all draws via replacement loop") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Mornsong Aria")
                .withCardInHand(1, "Inspiration")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Hill Giant")
                .withLandsOnBattlefield(1, "Island", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            // Mornsong Aria has PreventDraw for all players. Cast Inspiration (draw 2) →
            // PreventDraw fires on each card → no cards drawn, hand ends at 0.
            val action = game.getLegalActions(1)
                .map { it.action }
                .filterIsInstance<CastSpell>()
                .firstOrNull()
            action shouldNotBe null
            game.execute(action!!.copy(targets = listOf(ChosenTarget.Player(game.player1Id))))
            game.resolveStack()

            game.handSize(1) shouldBe 0
        }

        test("activeReplacementChain prevents re-applying the same effect (CR 614.5)") {
            // A ModifyDrawAmount applied once should be tracked via activeReplacementChain
            // so that re-processing the same event with that state filters it out.
            val driver = createDriver()
            driver.initMirrorMatch(deck = Deck.of("Plains" to 20))
            val playerId = driver.activePlayer!!

            driver.addPermanentWithReplacement(
                playerId, "Draw Booster",
                ModifyDrawAmount(modifier = 1, appliesTo = EventPattern.DrawCardsEvent(Player.You))
            )

            val processor = ReplacementEffectProcessor()
            val event = PendingGameEvent.DrawAmountPending(playerId, totalCount = 1)

            // First pass: Modifier applies
            val firstResult = processor.process(driver.state, event)
            val firstResolved = firstResult as ProcessorResult.Resolved
            firstResolved.outcome shouldBe ReplacementOutcome.Modified(
                PendingGameEvent.DrawAmountPending(playerId, totalCount = 2)
            )

            // Verify activeReplacementChain was stamped on the returned state
            val stampedChain = firstResolved.state.activeReplacementChain
            stampedChain shouldNotBe null
            stampedChain!!.size shouldBe 1

            // Second pass with the stamped state: same effect identity is already in the chain
            val secondResult = processor.process(firstResolved.state, event)
            (secondResult is ProcessorResult.Pass) shouldBe true
        }

        test("Multiple draw effect replacements") {
            // Quantum Riddler (ModifyDrawAmount +1 on EventPattern.DrawCardsEvent)
            // and Phial of Galadriel (ReplaceDrawWithEffect on EventPattern.DrawEvent)
            // coexist: Quantum Riddler fires at announcement level (DrawAmountPending
            // matches DrawCardsEvent), and Phial fires per-card (DrawPending matches
            // DrawEvent). Both are mandatory, so no player choice is needed.
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Quantum Riddler")
                .withCardOnBattlefield(1, "Phial of Galadriel")
                .withCardInHand(1, "Inspiration")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Island", 4)
                .build()

            val action = game.getLegalActions(1)
                .map { it.action }
                .filterIsInstance<CastSpell>()
                .firstOrNull()
                ?: error("No action found")
            action shouldNotBe null
            game.execute(action.copy(
                targets = listOf(ChosenTarget.Player(game.player1Id))
            ))
            game.resolveStack()

            // No pausing — both replacements are mandatory.
            game.state.isPaused() shouldBe false
            game.state.stack shouldBe emptyList()
            game.state.getHand(game.player1Id).size shouldBe 4  // Inspiration draws 2, +1 from Riddler, +1 from Phial on first draw
        }

        test("sub-draw retains announcement-level chain — Riddler blocked on Phial replacement (CR 614.5)") {
            // Regression: when Phial of Galadriel replaces the first card draw with
            // DrawCardsEffect(2), the sub-draw goes through executeDraws() →
            // checkDrawAmount(). The activeReplacementChain (stamped by the parent
            // announcement) MUST still contain Riddler's identity so it doesn't fire
            // again on the sub-draw.
            //
            // With protection:
            //   Inspiration draws 2 + Riddler (+1) = 3 per-card iterations.
            //   Card 1 → Phial replaces with DrawCardsEffect(2) → draws 2 cards,
            //             Riddler blocked → exactly 2.
            //   Card 2 → normal draw → 1.
            //   Card 3 → normal draw → 1.
            //   Total: 2 + 1 + 1 + 1 = 4.
            // Without chain: sub-draw of 2 becomes 3 (Riddler re-fires) → total = 5.
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Quantum Riddler")
                .withCardOnBattlefield(1, "Phial of Galadriel")
                .withCardInHand(1, "Inspiration")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Island", 8)
                .build()

            game.state.activeReplacementChain shouldBe null

            val action = game.getLegalActions(1)
                .map { it.action }
                .filterIsInstance<CastSpell>()
                .firstOrNull()
                ?: error("No action found")
            game.execute(action.copy(targets = listOf(ChosenTarget.Player(game.player1Id))))
            game.resolveStack()

            game.state.activeReplacementChain shouldBe null
            game.state.stack shouldBe emptyList()
            game.state.isPaused() shouldBe false
            // If Riddler fired on the sub-draw, this would be 5.
            game.state.getHand(game.player1Id).size shouldBe 4

            // --- Second instruction: verify cross-instruction chain clearing ---
            // Reset hand so both Riddler (≤1 card) and Phial (empty hand)
            // restrictions are met, then fire a second Inspiration.
            val handCards = game.state.getHand(game.player1Id).toList()
            for (cardId in handCards) {
                game.state = game.state.removeFromZone(ZoneKey(game.player1Id, Zone.HAND), cardId)
                game.state = game.state.addToZone(ZoneKey(game.player1Id, Zone.LIBRARY), cardId)
            }
            val inspDef = cardRegistry.getCard("Inspiration")
                ?: error("Inspiration not found")
            val newInspId = EntityId.of("second-inspiration")
            game.state = game.state.withEntity(newInspId,
                ComponentContainer.of(
                    CardComponent(
                        cardDefinitionId = "Inspiration",
                        name = "Inspiration",
                        manaCost = inspDef.manaCost,
                        typeLine = inspDef.typeLine,
                        oracleText = inspDef.oracleText,
                        baseStats = inspDef.creatureStats,
                        baseKeywords = inspDef.keywords,
                        baseFlags = inspDef.flags,
                        colors = inspDef.colors,
                        ownerId = game.player1Id,
                        spellEffect = inspDef.spellEffect,
                        hasNonManaActivatedAbility = inspDef.hasNonManaActivatedAbility,
                    ),
                    OwnerComponent(game.player1Id),
                    ControllerComponent(game.player1Id)
                )
            )
            game.state = game.state.addToZone(ZoneKey(game.player1Id, Zone.HAND), newInspId)
            game.state.getHand(game.player1Id).size shouldBe 1

            val secondAction = game.getLegalActions(1)
                .map { it.action }
                .filterIsInstance<CastSpell>()
                .firstOrNull() ?: error("No castable spell")
            game.execute(secondAction.copy(targets = listOf(ChosenTarget.Player(game.player1Id))))
            game.resolveStack()

            // If chain from first instruction leaked, second Riddler is blocked:
            // draw 2 (blocked) → Card 1: Phial → DrawCardsEffect(2) → 2 cards,
            // Card 2: normal → 1, total = 3.
            // If chain was cleared: draw 3 (Riddler re-fires) → Card 1: Phial →
            // DrawCardsEffect(2) → 2, Card 2: normal → 1, Card 3: normal → 1,
            // total = 4.
            game.state.activeReplacementChain shouldBe null
            game.state.getHand(game.player1Id).size shouldBe 4
        }

        test("chain cleared between separate draw instructions — Riddler fires for each (CR 614.5)") {
            // Regression: announcement-level chain (stamped by checkDrawAmount) must be
            // cleared by DrawLoop.run() on exit so a subsequent separate draw instruction
            // gets a fresh replacement check.
            //
            // Game-level: Quantum Riddler has ModifyDrawAmount(+1) gated on
            // CardsInHandAtMost(1). After the first Inspiration (draws 3 instead of 2)
            // the hand has 3 cards — too many for Riddler to fire. We tuck the hand
            // into the library and add a new Inspiration to test that the chain from
            // the first instruction doesn't block the second.
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Quantum Riddler")
                .withCardInHand(1, "Inspiration")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Island", 8)
                .build()

            game.state.activeReplacementChain shouldBe null

            // --- First draw instruction ---
            val firstAction = game.getLegalActions(1)
                .map { it.action }
                .filterIsInstance<CastSpell>()
                .firstOrNull() ?: error("No castable spell")
            game.execute(firstAction.copy(targets = listOf(ChosenTarget.Player(game.player1Id))))
            game.resolveStack()

            // Riddler fired: 2 + 1 = 3 cards drawn
            game.state.activeReplacementChain shouldBe null
            game.state.getHand(game.player1Id).size shouldBe 3

            // Reset hand so Riddler restriction (≤1 card) is met for a second instruction.
            val handCards = game.state.getHand(game.player1Id).toList()
            for (cardId in handCards) {
                game.state = game.state.removeFromZone(ZoneKey(game.player1Id, Zone.HAND), cardId)
                game.state = game.state.addToZone(ZoneKey(game.player1Id, Zone.LIBRARY), cardId)
            }
            val inspDef = cardRegistry.getCard("Inspiration")
                ?: error("Inspiration not found")
            val newInspId = EntityId.of("second-inspiration")
            game.state = game.state.withEntity(newInspId,
                ComponentContainer.of(
                    CardComponent(
                        cardDefinitionId = "Inspiration",
                        name = "Inspiration",
                        manaCost = inspDef.manaCost,
                        typeLine = inspDef.typeLine,
                        oracleText = inspDef.oracleText,
                        baseStats = inspDef.creatureStats,
                        baseKeywords = inspDef.keywords,
                        baseFlags = inspDef.flags,
                        colors = inspDef.colors,
                        ownerId = game.player1Id,
                        spellEffect = inspDef.spellEffect,
                        hasNonManaActivatedAbility = inspDef.hasNonManaActivatedAbility,
                    ),
                    OwnerComponent(game.player1Id),
                    ControllerComponent(game.player1Id)
                )
            )
            game.state = game.state.addToZone(ZoneKey(game.player1Id, Zone.HAND), newInspId)
            game.state.getHand(game.player1Id).size shouldBe 1

            // --- Second draw instruction ---
            val secondAction = game.getLegalActions(1)
                .map { it.action }
                .filterIsInstance<CastSpell>()
                .firstOrNull() ?: error("No castable spell")
            game.execute(secondAction.copy(targets = listOf(ChosenTarget.Player(game.player1Id))))
            game.resolveStack()

            // Riddler fired again: chain from first instruction was properly cleared
            game.state.activeReplacementChain shouldBe null
            game.state.getHand(game.player1Id).size shouldBe 3
        }
    }
}
