package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.DeclareBlockers
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TransformPermanent
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Devoid (CR 702.114) in the engine.
 *
 * "Devoid" means "This object is colorless", and it is a *characteristic-defining* ability
 * (CR 604.3) — so it functions in every zone, and CR 613.3 applies it before any other layer-5
 * effect. The SDK therefore models it as a derived characteristic rather than a continuous effect:
 * `CardDefinition.colors` is empty for a card with the keyword, `CardComponent.colors` is built
 * from that, and everything downstream inherits it.
 *
 * This file is the engine's half of that claim. It walks the rule the CDA is supposed to obey —
 * all five zones, the layer-5 override, evasion, copies, and the *mana cost*, which devoid does
 * **not** touch — so that a regression in any of those read paths fails here rather than in a card.
 * The model-layer half (`colors` vs `colorIdentity`, both SDK spellings of the keyword) is
 * `DevoidColorsTest` in `mtg-sdk`.
 */
class DevoidKeywordScenarioTest : FunSpec({

    val projector = StateProjector()
    val predicateEvaluator = PredicateEvaluator()

    /** Ulamog's Nullifier's shape: coloured pips, devoid, colorless. */
    val DevoidEldrazi = CardDefinition.creature(
        name = "Devoid Eldrazi",
        manaCost = ManaCost.parse("{2}{U}{U}"),
        subtypes = setOf(Subtype("Eldrazi")),
        power = 2,
        toughness = 3,
        oracleText = "Devoid (This card has no color.)",
        keywords = setOf(Keyword.DEVOID),
    )

    /** The same card without the keyword — the control every colour assertion is read against. */
    val BlueEldrazi = CardDefinition.creature(
        name = "Blue Eldrazi",
        manaCost = ManaCost.parse("{2}{U}{U}"),
        subtypes = setOf(Subtype("Eldrazi")),
        power = 2,
        toughness = 3,
    )

    /** A devoid creature whose cost is black — the fear blocker that *isn't* black after all. */
    val DevoidDrone = CardDefinition.creature(
        name = "Devoid Drone",
        manaCost = ManaCost.parse("{1}{B}"),
        subtypes = setOf(Subtype("Eldrazi"), Subtype("Drone")),
        power = 2,
        toughness = 2,
        oracleText = "Devoid (This card has no color.)",
        keywords = setOf(Keyword.DEVOID),
    )

    /** "Creatures you control are blue." — a plain layer-5 colour-setting static. */
    val BlueWash = CardDefinition.enchantment(
        name = "Blue Wash",
        manaCost = ManaCost.parse("{2}{U}"),
        oracleText = "Creatures you control are blue.",
        script = CardScript(
            staticAbilities = listOf(
                TransformPermanent(
                    setColors = setOf(Color.BLUE),
                    filter = GroupFilter.AllCreaturesYouControl,
                )
            )
        ),
    )

    val extraCards = listOf(DevoidEldrazi, BlueEldrazi, DevoidDrone, BlueWash)

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + extraCards)
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameState.colorsOf(id: EntityId): Set<Color> =
        getEntity(id)?.get<CardComponent>()?.colors ?: error("no card component for $id")

    fun GameState.matches(id: EntityId, predicate: CardPredicate, controller: EntityId): Boolean =
        predicateEvaluator.matches(
            this,
            projectedState,
            id,
            GameObjectFilter(cardPredicates = listOf(predicate)),
            PredicateContext(controllerId = controller),
        )

    test("a devoid permanent is colorless on the battlefield, coloured pips notwithstanding") {
        val driver = createDriver()
        val me = driver.activePlayer!!

        val devoid = driver.putCreatureOnBattlefield(me, "Devoid Eldrazi")
        val blue = driver.putCreatureOnBattlefield(me, "Blue Eldrazi")

        val projected = projector.project(driver.state)
        projected.getColors(devoid) shouldBe emptySet()
        withClue("the control card proves the pips are still there to be read") {
            projected.getColors(blue) shouldBe setOf(Color.BLUE.name)
        }
    }

    // CR 604.3: characteristic-defining abilities function in all zones. Devoid is the reason a
    // BFZ Eldrazi is a colorless card in your graveyard as well as a colorless permanent in play,
    // which matters for every "target colorless card in a graveyard" and "colorless spell" payoff.
    test("devoid functions in every zone (CR 604.3)") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        val inHand = driver.putCardInHand(me, "Devoid Eldrazi")
        val inGraveyard = driver.putCardInGraveyard(me, "Devoid Eldrazi")
        val inExile = driver.putCardInExile(me, "Devoid Eldrazi")
        val inLibrary = driver.putCardOnTopOfLibrary(opponent, "Devoid Eldrazi")
        val onBattlefield = driver.putCreatureOnBattlefield(me, "Devoid Eldrazi")

        mapOf(
            "hand" to inHand,
            "graveyard" to inGraveyard,
            "exile" to inExile,
            "library" to inLibrary,
            "battlefield" to onBattlefield,
        ).forEach { (zone, id) ->
            withClue("colors in $zone") { driver.state.colorsOf(id) shouldBe emptySet() }
        }

        // …and on the stack, the zone the keyword's own reminder text is about.
        driver.giveMana(me, Color.BLUE, 2)
        driver.giveColorlessMana(me, 2)
        driver.castSpell(me, inHand).isSuccess shouldBe true
        val onStack = driver.getTopOfStack() ?: error("the spell should be on the stack")
        withClue("colors on the stack") { driver.state.colorsOf(onStack) shouldBe emptySet() }
    }

    test("the colour predicates read the CDA, not the mana cost") {
        val driver = createDriver()
        val me = driver.activePlayer!!

        val devoid = driver.putCreatureOnBattlefield(me, "Devoid Eldrazi")
        val blue = driver.putCreatureOnBattlefield(me, "Blue Eldrazi")
        val state = driver.state

        state.matches(devoid, CardPredicate.IsColorless, me) shouldBe true
        state.matches(devoid, CardPredicate.IsColored, me) shouldBe false
        state.matches(devoid, CardPredicate.HasColor(Color.BLUE), me) shouldBe false
        state.matches(devoid, CardPredicate.NotColor(Color.BLUE), me) shouldBe true

        state.matches(blue, CardPredicate.IsColorless, me) shouldBe false
        state.matches(blue, CardPredicate.HasColor(Color.BLUE), me) shouldBe true
    }

    // Devoid changes an object's colour and nothing else — the mana cost, and so the coloured mana
    // needed to pay it and the mana value, are untouched (CR 202.1 / 202.3).
    test("a devoid spell still costs its coloured mana") {
        val driver = createDriver()
        val me = driver.activePlayer!!

        val card = driver.putCardInHand(me, "Devoid Eldrazi")
        driver.state.getEntity(card)?.get<CardComponent>()?.manaValue shouldBe 4

        driver.giveColorlessMana(me, 4)
        withClue("four colorless mana can't pay {2}{U}{U} just because the card is colorless") {
            driver.castSpell(me, card).isSuccess shouldBe false
        }

        driver.giveMana(me, Color.BLUE, 2)
        driver.castSpell(me, card).isSuccess shouldBe true
    }

    // CR 613.3 applies CDAs first within a layer, then everything else in timestamp order. Devoid
    // is the base value here, so an ordinary layer-5 colour-setting effect paints straight over it.
    test("a later colour-changing effect paints over devoid (CR 613.3)") {
        val driver = createDriver()
        val me = driver.activePlayer!!

        val devoid = driver.putCreatureOnBattlefield(me, "Devoid Eldrazi")
        projector.project(driver.state).getColors(devoid) shouldBe emptySet()

        driver.putPermanentOnBattlefield(me, "Blue Wash")

        projector.project(driver.state).getColors(devoid) shouldBe setOf(Color.BLUE.name)
    }

    // Fear (CR 702.36b): "can't be blocked except by artifact creatures and/or black creatures."
    // A {1}{B} devoid Drone is neither, so it can't block — the sharpest proof that combat reads
    // the CDA rather than the printed pips.
    test("a devoid creature with a black mana cost can't block a creature with fear") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + extraCards)
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 20, "Grizzly Bears" to 20),
            skipMulligans = true,
        )

        val attacker = driver.putCreatureOnBattlefield(driver.player1, "Fear Creature")
        driver.removeSummoningSickness(attacker)
        val devoidBlocker = driver.putCreatureOnBattlefield(driver.player2, "Devoid Drone")
        driver.removeSummoningSickness(devoidBlocker)
        val blackBlocker = driver.putCreatureOnBattlefield(driver.player2, "Black Creature")
        driver.removeSummoningSickness(blackBlocker)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        var safety = 0
        while (driver.activePlayer != driver.player1 && safety < 50) {
            driver.bothPass()
            driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
            safety++
        }

        driver.declareAttackers(driver.player1, listOf(attacker), driver.player2).isSuccess shouldBe true
        driver.bothPass()
        driver.currentStep shouldBe Step.DECLARE_BLOCKERS

        withClue("devoid strips the black the {B} pip would otherwise give the blocker") {
            driver.submitExpectFailure(
                DeclareBlockers(driver.player2, mapOf(devoidBlocker to listOf(attacker)))
            ).isSuccess shouldBe false
        }
        withClue("the printed-black control still blocks, so the restriction itself is intact") {
            driver.submitSuccess(
                DeclareBlockers(driver.player2, mapOf(blackBlocker to listOf(attacker)))
            ).isSuccess shouldBe true
        }
    }

    // Devoid is printed on the card, so it is part of the copiable values (CR 707.2) — a token
    // copy of a devoid Eldrazi is colorless too, without the copy path knowing what devoid is.
    test("a token copy of a devoid permanent is colorless too (CR 707.2)") {
        val driver = createDriver()
        val me = driver.activePlayer!!

        val original = driver.putCreatureOnBattlefield(me, "Devoid Eldrazi")
        val spell = driver.putCardInHand(me, "Test Token Copy")
        driver.giveMana(me, Color.BLUE, 1)
        driver.giveColorlessMana(me, 1)
        driver.castSpell(me, spell, targets = listOf(original)).isSuccess shouldBe true
        driver.bothPass()

        val copy = driver.getCreatures(me).single { it != original }
        driver.state.colorsOf(copy) shouldBe emptySet()
        projector.project(driver.state).getColors(copy) shouldBe emptySet()
    }
})
