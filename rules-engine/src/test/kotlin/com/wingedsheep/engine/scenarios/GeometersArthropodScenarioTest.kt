package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Geometer's Arthropod {G}{U} 1/4 Fractal Crab.
 *
 * "Whenever you cast a spell with {X} in its mana cost, look at the top X cards of your library.
 * Put one of them into your hand and the rest on the bottom of your library in a random order."
 *
 * Exercises the new `SpellCastPredicate.HasXInCost` trigger gate and
 * `DynamicAmounts.xValueOfTriggeringSpell()` dig count: the dig only fires for spells with {X} in
 * their cost, looks at exactly the announced X, keeps one in hand, and bottoms the rest. X=0 is a
 * harmless no-op (look at zero), and a spell without {X} never triggers.
 *
 * The triggering spells here are deliberately effect-less ({X}{U} / {U} "do nothing") so the only
 * thing that touches the library is the Arthropod's dig — keeping the assertions about library and
 * hand contents unambiguous.
 */
class GeometersArthropodScenarioTest : ScenarioTestBase() {

    // A targetless {X}{U} spell that does nothing on resolution, so the only library/hand change is
    // the Arthropod's dig.
    private val xNoop = card("X Noop Test") {
        manaCost = "{X}{U}"
        typeLine = "Sorcery"
        spell { effect = Effects.GainLife(0) }
    }

    // A vanilla {U} no-op spell with NO {X} — must not fire the Arthropod's trigger.
    private val plainNoop = card("Plain Noop Test") {
        manaCost = "{U}"
        typeLine = "Sorcery"
        spell { effect = Effects.GainLife(0) }
    }

    init {
        cardRegistry.register(xNoop)
        cardRegistry.register(plainNoop)

        context("Geometer's Arthropod") {

            test("casting an {X} spell with X=2: look at top 2, keep one, bottom the rest") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Geometer's Arthropod")
                    .withCardInHand(1, "X Noop Test") // {X}{U}
                    .withLandsOnBattlefield(1, "Island", 3) // X=2 + {U}
                    .withCardInLibrary(1, "Plains")    // top
                    .withCardInLibrary(1, "Swamp")     // 2nd — both looked at with X=2
                    .withCardInLibrary(1, "Mountain")  // 3rd — beyond X=2, untouched
                    .withCardInLibrary(1, "Forest")    // 4th
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                fun libraryNames(): List<String> =
                    game.state.getZone(ZoneKey(game.player1Id, Zone.LIBRARY))
                        .mapNotNull { game.state.getEntity(it)?.get<CardComponent>()?.name }

                fun libraryIdOf(name: String): EntityId =
                    game.state.getZone(ZoneKey(game.player1Id, Zone.LIBRARY))
                        .first { game.state.getEntity(it)?.get<CardComponent>()?.name == name }

                val plains = libraryIdOf("Plains")

                // Cast X Noop Test with X=2. The "you cast a spell with {X}" trigger fires and asks
                // which of the top 2 to keep.
                game.castXSpell(1, "X Noop Test", xValue = 2).error shouldBe null
                game.resolveStack()

                withClue("X=2 → the dig pauses for a keep-one decision") {
                    (game.state.pendingDecision != null) shouldBe true
                }
                game.selectCards(listOf(plains)).error shouldBe null
                game.resolveStack()

                withClue("The chosen card (Plains) went to hand") {
                    game.isInHand(1, "Plains") shouldBe true
                }
                withClue("Swamp (2nd, rejected) is on the bottom of the library") {
                    libraryNames().last() shouldBe "Swamp"
                }
                withClue("Mountain (3rd) and Forest (4th) were never looked at — still on top, in order") {
                    libraryNames() shouldBe listOf("Mountain", "Forest", "Swamp")
                }
            }

            test("X=0 is a harmless no-op: nothing looked at, library order preserved") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Geometer's Arthropod")
                    .withCardInHand(1, "X Noop Test") // {X}{U}
                    .withLandsOnBattlefield(1, "Island", 1) // just {U}, X=0
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(1, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                fun libraryNames(): List<String> =
                    game.state.getZone(ZoneKey(game.player1Id, Zone.LIBRARY))
                        .mapNotNull { game.state.getEntity(it)?.get<CardComponent>()?.name }

                game.castXSpell(1, "X Noop Test", xValue = 0).error shouldBe null
                game.resolveStack()

                withClue("X=0 → the dig looks at zero cards, so no keep-one decision pends") {
                    (game.state.pendingDecision == null) shouldBe true
                }
                withClue("Nothing was drawn or moved — library keeps its full order") {
                    game.isInHand(1, "Plains") shouldBe false
                    libraryNames() shouldBe listOf("Plains", "Swamp", "Mountain")
                }
            }

            test("a spell WITHOUT {X} in its cost does not trigger the dig") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Geometer's Arthropod")
                    .withCardInHand(1, "Plain Noop Test") // {U}, no {X}
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(1, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                fun libraryNames(): List<String> =
                    game.state.getZone(ZoneKey(game.player1Id, Zone.LIBRARY))
                        .mapNotNull { game.state.getEntity(it)?.get<CardComponent>()?.name }

                game.castSpell(1, "Plain Noop Test").error shouldBe null
                game.resolveStack()

                withClue("No {X} in cost → no dig decision") {
                    (game.state.pendingDecision == null) shouldBe true
                }
                withClue("Nothing happened — library untouched") {
                    game.isInHand(1, "Plains") shouldBe false
                    libraryNames() shouldBe listOf("Plains", "Swamp")
                }
            }
        }
    }
}
