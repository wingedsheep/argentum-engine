package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.GameConfig
import com.wingedsheep.engine.core.GameInitializer
import com.wingedsheep.engine.core.PlayerConfig
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.ObjectReferenceEnvironment
import com.wingedsheep.engine.handlers.effects.EffectExecutorRegistry
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.chosenOpponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.blb.cards.CrumbAndGetIt
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.ChooseOpponentForSourceEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Gift mechanic: "You may promise AN OPPONENT a gift as you cast this spell."
 *
 * Regression guards for the recipient choice:
 * - 2-player: the sole opponent is a forced choice — no extra prompt, the gift arrives.
 * - Multiplayer: the promising player chooses which opponent receives the gift
 *   (ChooseOpponentForSourceEffect); the choice is recorded on the source and read back
 *   through Player.ChosenOpponent. Previously the gift always went to the FIRST opponent.
 */
class GiftChosenOpponentTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + PredefinedTokens.allTokens + listOf(CrumbAndGetIt))
        return driver
    }

    test("2-player gift: sole opponent gets the Food with no extra prompt") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val lions = driver.putCreatureOnBattlefield(active, "Savannah Lions")

        driver.giveMana(active, Color.WHITE, 1)
        val crumb = driver.putCardInHand(active, "Crumb and Get It")
        // Mode 2 = "Promise a gift"; its target is a creature you control.
        driver.submitSuccess(
            CastSpell(
                playerId = active,
                cardId = crumb,
                chosenModes = listOf(1),
                modeTargetsOrdered = listOf(listOf(ChosenTarget.Permanent(lions))),
                targets = listOf(ChosenTarget.Permanent(lions))
            )
        )
        driver.bothPass()

        // The sole opponent is a forced choice: no ChooseOptionDecision may surface.
        (driver.pendingDecision is ChooseOptionDecision) shouldBe false
        driver.findPermanent(opponent, "Food") shouldNotBe null
        driver.findPermanent(active, "Food") shouldBe null
    }

    test("multiplayer gift: the caster chooses the recipient among three opponents") {
        val registry = CardRegistry()
        registry.register(CrumbAndGetIt)
        val deck = Deck(cards = List(40) { "Crumb and Get It" })
        val result = GameInitializer(registry).initializeGame(
            GameConfig(
                players = (1..4).map { PlayerConfig("Player $it", deck, 20) },
                skipMulligans = true,
                startingPlayerIndex = 0
            )
        )
        val state = result.state
        val players = result.playerIds

        // Materialize a source entity (stands in for the gift spell/permanent).
        val sourceId = EntityId.generate()
        val container = ComponentContainer.of(
            CardComponent(
                cardDefinitionId = CrumbAndGetIt.name,
                name = CrumbAndGetIt.name,
                manaCost = ManaCost.parse("{W}"),
                typeLine = TypeLine.parse("Instant"),
                ownerId = players[0]
            ),
            ControllerComponent(players[0])
        )
        val withSource = state
            .withEntity(sourceId, container)
            .addToZone(ZoneKey(players[0], Zone.BATTLEFIELD), sourceId)

        // Executing the choose-opponent effect pauses with one option per opponent.
        val registry2 = EffectExecutorRegistry(cardRegistry = registry)
        val sourceRef = withSource.objectRef(sourceId)!!
        val context = EffectContext(sourceId = sourceId, controllerId = players[0],
            objectReferences = ObjectReferenceEnvironment(captured = true,
                origin = sourceRef, source = sourceRef, resolutionKey = "gift-recipient-fixture"))
        val execResult = registry2.execute(withSource, ChooseOpponentForSourceEffect(), context)

        execResult.isPaused shouldBe true
        val decision = execResult.state.pendingDecision
        decision.shouldNotBeNull()
        decision.shouldBeInstanceOf<ChooseOptionDecision>()
        decision.playerId shouldBe players[0]
        decision.options.size shouldBe 3

        // Choosing option index 1 (the SECOND opponent, players[2]) must be honored —
        // the old Player.AnOpponent behavior always used players[1].
        val processor = com.wingedsheep.engine.core.ActionProcessor(registry)
        val afterChoice = processor.process(
            execResult.state,
            com.wingedsheep.engine.core.SubmitDecision(
                players[0],
                com.wingedsheep.engine.core.OptionChosenResponse(decision.id, optionIndex = 1)
            )
        ).result

        afterChoice.isSuccess shouldBe true
        fun chosen(s: GameState) = s.getEntity(sourceId)?.chosenOpponent()
        chosen(afterChoice.newState) shouldBe players[2]
    }
})
