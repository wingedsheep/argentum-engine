package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActionProcessor
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.GameConfig
import com.wingedsheep.engine.core.GameInitializer
import com.wingedsheep.engine.core.KeepHand
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.PlayerConfig
import com.wingedsheep.engine.core.SubmitDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.core.YesNoResponse
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CastChoicesComponent
import com.wingedsheep.engine.state.components.battlefield.ChoiceValue
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dsk.cards.LeylineOfTransformation
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mayBeginGameOnBattlefield
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

private val projector = StateProjector()
private val predicateEvaluator = PredicateEvaluator()

/**
 * Scenarios for Leyline of Transformation (DSK) — the new [com.wingedsheep.sdk.scripting.GrantChosenSubtype]
 * static ability ("Creatures you control are the chosen type in addition to their other types").
 *
 * Only the battlefield clause is modeled (see the card definition for the documented limitation), so
 * these tests cover: creatures you control gain the chosen type *in addition to* their printed types,
 * opponents' creatures are unaffected, and the choice is made as the enchantment enters.
 */
class LeylineOfTransformationScenarioTest : FunSpec({

    val bear = CardDefinition.creature(
        name = "Test Bear",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype("Bear")),
        power = 2,
        toughness = 2
    )

    val goblin = CardDefinition.creature(
        name = "Test Goblin",
        manaCost = ManaCost.parse("{1}{R}"),
        subtypes = setOf(Subtype("Goblin")),
        power = 1,
        toughness = 1
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(LeylineOfTransformation, bear, goblin))
        return driver
    }

    test("creatures you control gain the chosen type in addition to their printed types") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20, "Mountain" to 20), skipMulligans = true)
        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Leyline of Transformation on the battlefield with the chosen type "Goblin".
        val leyline = driver.putPermanentOnBattlefield(activePlayer, "Leyline of Transformation")
        driver.replaceState(driver.state.updateEntity(leyline) { c ->
            c.with(CastChoicesComponent(chosen = mapOf(ChoiceSlot.CREATURE_TYPE to ChoiceValue.TextChoice("Goblin"))))
        })

        val bearId = driver.putCreatureOnBattlefield(activePlayer, "Test Bear")

        val projected = projector.project(driver.state)
        // The Bear is now a Goblin in addition to its other types.
        projected.hasSubtype(bearId, "Goblin") shouldBe true
        projected.hasSubtype(bearId, "Bear") shouldBe true
        // P/T untouched — this is a pure type-change.
        projected.getPower(bearId) shouldBe 2
        projected.getToughness(bearId) shouldBe 2
    }

    test("opponents' creatures are unaffected (Creatures YOU control)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20, "Mountain" to 20), skipMulligans = true)
        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val leyline = driver.putPermanentOnBattlefield(activePlayer, "Leyline of Transformation")
        driver.replaceState(driver.state.updateEntity(leyline) { c ->
            c.with(CastChoicesComponent(chosen = mapOf(ChoiceSlot.CREATURE_TYPE to ChoiceValue.TextChoice("Goblin"))))
        })

        val myBear = driver.putCreatureOnBattlefield(activePlayer, "Test Bear")
        val theirBear = driver.putCreatureOnBattlefield(opponent, "Test Bear")

        val projected = projector.project(driver.state)
        projected.hasSubtype(myBear, "Goblin") shouldBe true
        projected.hasSubtype(theirBear, "Goblin") shouldBe false
        projected.hasSubtype(theirBear, "Bear") shouldBe true
    }

    test("a creature already of the chosen type keeps its type and is unchanged") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20, "Mountain" to 20), skipMulligans = true)
        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val leyline = driver.putPermanentOnBattlefield(activePlayer, "Leyline of Transformation")
        driver.replaceState(driver.state.updateEntity(leyline) { c ->
            c.with(CastChoicesComponent(chosen = mapOf(ChoiceSlot.CREATURE_TYPE to ChoiceValue.TextChoice("Goblin"))))
        })

        val goblinId = driver.putCreatureOnBattlefield(activePlayer, "Test Goblin")

        val projected = projector.project(driver.state)
        projected.hasSubtype(goblinId, "Goblin") shouldBe true
    }

    test("choice is made as the enchantment enters (EntersWithChoice)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 20, "Forest" to 20), skipMulligans = true)
        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bearId = driver.putCreatureOnBattlefield(activePlayer, "Test Bear")

        // Cast Leyline of Transformation from hand — it should pause for the creature-type choice.
        val spell = driver.putCardInHand(activePlayer, "Leyline of Transformation")
        driver.giveMana(activePlayer, Color.BLUE, 4)
        driver.castSpell(activePlayer, spell)
        driver.bothPass()

        driver.isPaused shouldBe true
        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<ChooseOptionDecision>()
        val goblinIndex = decision.options.indexOf("Goblin")
        driver.submitDecision(activePlayer, OptionChosenResponse(decision.id, goblinIndex))

        val projected = projector.project(driver.state)
        projected.hasSubtype(bearId, "Goblin") shouldBe true
        projected.hasSubtype(bearId, "Bear") shouldBe true
    }

    // ---- Cross-zone clause: "creature spells you control and creature cards you own that
    //      aren't on the battlefield are the chosen type." ----

    test("a creature card you own in your graveyard is the chosen type") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20, "Mountain" to 20), skipMulligans = true)
        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val leyline = driver.putPermanentOnBattlefield(activePlayer, "Leyline of Transformation")
        driver.replaceState(driver.state.updateEntity(leyline) { c ->
            c.with(CastChoicesComponent(chosen = mapOf(ChoiceSlot.CREATURE_TYPE to ChoiceValue.TextChoice("Goblin"))))
        })

        val gyBear = driver.putCardInGraveyard(activePlayer, "Test Bear")
        val state = driver.state
        val projected = projector.project(state)

        // The graveyard Bear counts as a Goblin for type-matters checks (e.g. "Zombie card in graveyard").
        projected.crossZoneGrantedSubtypes(state, gyBear) shouldBe setOf("Goblin")
        predicateEvaluator.matchesCardPredicate(
            state, projected, gyBear, CardPredicate.HasSubtype(Subtype("Goblin"))
        ) shouldBe true
        // Still a Bear too.
        predicateEvaluator.matchesCardPredicate(
            state, projected, gyBear, CardPredicate.HasSubtype(Subtype("Bear"))
        ) shouldBe true
    }

    test("a creature card in an OPPONENT's graveyard is not the chosen type (you own)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20, "Mountain" to 20), skipMulligans = true)
        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val leyline = driver.putPermanentOnBattlefield(activePlayer, "Leyline of Transformation")
        driver.replaceState(driver.state.updateEntity(leyline) { c ->
            c.with(CastChoicesComponent(chosen = mapOf(ChoiceSlot.CREATURE_TYPE to ChoiceValue.TextChoice("Goblin"))))
        })

        val theirGyBear = driver.putCardInGraveyard(opponent, "Test Bear")
        val state = driver.state
        val projected = projector.project(state)

        projected.crossZoneGrantedSubtypes(state, theirGyBear) shouldBe emptySet()
        predicateEvaluator.matchesCardPredicate(
            state, projected, theirGyBear, CardPredicate.HasSubtype(Subtype("Goblin"))
        ) shouldBe false
    }

    test("a creature spell you control on the stack is the chosen type") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20, "Mountain" to 20), skipMulligans = true)
        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val leyline = driver.putPermanentOnBattlefield(activePlayer, "Leyline of Transformation")
        driver.replaceState(driver.state.updateEntity(leyline) { c ->
            c.with(CastChoicesComponent(chosen = mapOf(ChoiceSlot.CREATURE_TYPE to ChoiceValue.TextChoice("Goblin"))))
        })

        // Cast a Bear creature spell — it sits on the stack.
        val spell = driver.putCardInHand(activePlayer, "Test Bear")
        driver.giveMana(activePlayer, Color.GREEN, 2)
        driver.castSpell(activePlayer, spell)

        val state = driver.state
        val spellId = state.stack.last()
        val projected = projector.project(state)

        projected.crossZoneGrantedSubtypes(state, spellId) shouldBe setOf("Goblin")
        predicateEvaluator.matchesCardPredicate(
            state, projected, spellId, CardPredicate.HasSubtype(Subtype("Goblin"))
        ) shouldBe true
    }

    test("a non-creature card you own outside the battlefield is NOT granted the type") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20, "Mountain" to 20), skipMulligans = true)
        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val leyline = driver.putPermanentOnBattlefield(activePlayer, "Leyline of Transformation")
        driver.replaceState(driver.state.updateEntity(leyline) { c ->
            c.with(CastChoicesComponent(chosen = mapOf(ChoiceSlot.CREATURE_TYPE to ChoiceValue.TextChoice("Goblin"))))
        })

        // A land card in the graveyard is not a creature card, so the grant must not touch it.
        val gyLand = driver.putCardInGraveyard(activePlayer, "Forest")
        val state = driver.state
        val projected = projector.project(state)

        projected.crossZoneGrantedSubtypes(state, gyLand) shouldBe emptySet()
        predicateEvaluator.matchesCardPredicate(
            state, projected, gyLand, CardPredicate.HasSubtype(Subtype("Goblin"))
        ) shouldBe false
    }

    // ---- Opening-hand start (CR 103.6a): "you may begin the game with it on the battlefield"
    //      still has to make the as-enters creature-type choice. ----

    // A second "begin the game on the battlefield" card, with no as-enters choice of its own, so we
    // can prove the opening-hand walk keeps prompting for the *next* leyline after the creature-type
    // choice pauses in the middle of it.
    val plainLeyline = card("Test Plain Leyline") {
        manaCost = "{2}"
        typeLine = "Enchantment"
        mayBeginGameOnBattlefield()
    }

    /**
     * Start a real game through [GameInitializer] with all of [p1Deck] in P1's opening hand and both
     * players keeping, so the CR 103.6 leyline phase is the next thing that happens. Returns the
     * processor, the state paused on the first leyline yes/no, and the player ids in turn order.
     */
    fun startLeylinePhase(p1Deck: Deck): Triple<ActionProcessor, GameState, List<EntityId>> {
        val registry = CardRegistry().apply {
            register(TestCards.all)
            register(listOf(LeylineOfTransformation, bear, goblin, plainLeyline))
        }
        val init = GameInitializer(registry).initializeGame(
            GameConfig(
                players = listOf(
                    PlayerConfig("P1", p1Deck),
                    PlayerConfig("P2", Deck.of("Island" to 60))
                ),
                startingPlayerIndex = 0,
                // Draw the whole deck — the leyline scan only inspects the opening hand.
                startingHandSize = 60
            )
        )
        val processor = ActionProcessor(registry)
        var state = init.state
        for (playerId in init.playerIds) {
            val result = processor.process(state, KeepHand(playerId)).result
            withClue("KeepHand should succeed: ${result.error}") { result.error shouldBe null }
            state = result.state
        }
        return Triple(processor, state, init.playerIds)
    }

    fun battlefieldNames(state: GameState, playerId: EntityId): List<String> =
        state.getZone(ZoneKey(playerId, Zone.BATTLEFIELD)).mapNotNull {
            state.getEntity(it)?.get<CardComponent>()?.name
        }

    test("opening-hand start: saying yes prompts for the creature type") {
        val (processor, started, players) = startLeylinePhase(
            Deck.of("Leyline of Transformation" to 1, "Island" to 59)
        )
        var state = started
        val p1 = players[0]

        val leylinePrompt = state.pendingDecision
        leylinePrompt.shouldBeInstanceOf<YesNoDecision>()
        leylinePrompt.playerId shouldBe p1

        val afterYes = processor.process(
            state, SubmitDecision(p1, YesNoResponse(leylinePrompt.id, true))
        ).result
        withClue("Answering the leyline prompt should succeed: ${afterYes.error}") {
            afterYes.error shouldBe null
        }
        state = afterYes.state

        // The bug: the card went straight to the battlefield with no chosen creature type, so the
        // GrantChosenSubtype static ability had nothing to grant for the rest of the game.
        val choice = state.pendingDecision
        withClue("Leyline of Transformation must still make its as-enters creature-type choice") {
            choice shouldNotBe null
        }
        choice.shouldBeInstanceOf<ChooseOptionDecision>()
        choice.playerId shouldBe p1
        val goblinIndex = choice.options.indexOf("Goblin")
        withClue("The creature-type options should include Goblin") { goblinIndex shouldNotBe -1 }

        val afterChoice = processor.process(
            state, SubmitDecision(p1, OptionChosenResponse(choice.id, goblinIndex))
        ).result
        withClue("Submitting the creature type should succeed: ${afterChoice.error}") {
            afterChoice.error shouldBe null
        }
        state = afterChoice.state

        val leylineId = state.getZone(ZoneKey(p1, Zone.BATTLEFIELD)).firstOrNull {
            state.getEntity(it)?.get<CardComponent>()?.name == "Leyline of Transformation"
        }
        withClue("Leyline of Transformation should be on the battlefield") { leylineId shouldNotBe null }
        state.getEntity(leylineId!!)?.get<CastChoicesComponent>()?.chosen?.get(ChoiceSlot.CREATURE_TYPE) shouldBe
            ChoiceValue.TextChoice("Goblin")

        withClue("The leyline phase should be over once the choice is made") {
            state.pendingDecision shouldBe null
        }
    }

    test("opening-hand start: the leyline walk continues after the creature-type choice") {
        val (processor, started, players) = startLeylinePhase(
            Deck.of("Leyline of Transformation" to 1, "Test Plain Leyline" to 1, "Island" to 58)
        )
        var state = started
        val p1 = players[0]

        var sawCreatureTypeChoice = false
        var steps = 0
        while (state.pendingDecision != null && steps++ < 10) {
            val decision = state.pendingDecision
            val response = when (decision) {
                is YesNoDecision -> YesNoResponse(decision.id, true)
                is ChooseOptionDecision -> {
                    sawCreatureTypeChoice = true
                    OptionChosenResponse(decision.id, decision.options.indexOf("Goblin"))
                }
                else -> error("Unexpected decision during the leyline phase: $decision")
            }
            val result = processor.process(state, SubmitDecision(decision.playerId, response)).result
            withClue("Submitting ${decision.id} should succeed: ${result.error}") {
                result.error shouldBe null
            }
            state = result.state
        }

        withClue("The creature-type choice should have been asked") { sawCreatureTypeChoice shouldBe true }
        withClue("The leyline phase should finish, not stall on the paused choice") {
            state.pendingDecision shouldBe null
        }
        withClue("Both opening-hand leylines should have made it to the battlefield") {
            // Sorted: the order the two leylines are asked about follows the shuffled opening hand.
            battlefieldNames(state, p1).sorted() shouldBe listOf("Leyline of Transformation", "Test Plain Leyline")
        }
    }

    test("no Leyline on the battlefield means no cross-zone grant (overlay is empty)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20, "Mountain" to 20), skipMulligans = true)
        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val gyBear = driver.putCardInGraveyard(activePlayer, "Test Bear")
        val state = driver.state
        val projected = projector.project(state)

        projected.crossZoneSubtypeGrants shouldBe emptyList()
        projected.crossZoneGrantedSubtypes(state, gyBear) shouldBe emptySet()
    }
})
