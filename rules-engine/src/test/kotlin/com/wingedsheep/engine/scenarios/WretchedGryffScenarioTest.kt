package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SpellCastEvent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Wretched Gryff — {7} 3/4 Eldrazi Hippogriff with flying, emerge {5}{U}, and
 * "When you cast this spell, draw a card."
 *
 * The draw is a cast trigger, so it resolves before the Gryff itself.
 */
class WretchedGryffScenarioTest : ScenarioTestBase() {

    init {
        context("Wretched Gryff") {

            test("emerge pays {5}{U} minus the sacrificed creature's mana value and draws a card") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Wretched Gryff")
                    .withCardOnBattlefield(1, "Centaur Courser") // {2}{G} → mana value 3
                    .withCardInLibrary(1, "Grizzly Bears")
                    // Emerge {5}{U} reduced by 3 → {2}{U}: three Islands is exactly enough.
                    .withLandsOnBattlefield(1, "Island", 3)
                    .build()

                val cast = game.castSpellWithEmerge(1, "Wretched Gryff", "Centaur Courser")
                withClue("the emerge cast should succeed: ${cast.error}") { cast.error shouldBe null }

                withClue("the creature is sacrificed as the cost is paid (CR 702.119c)") {
                    game.isInGraveyard(1, "Centaur Courser") shouldBe true
                    game.isOnBattlefield("Centaur Courser") shouldBe false
                }

                game.resolveStack()

                withClue("the cast trigger drew a card and the Gryff resolved") {
                    game.isInHand(1, "Grizzly Bears") shouldBe true
                    game.isOnBattlefield("Wretched Gryff") shouldBe true
                }
                val gryff = game.findPermanent("Wretched Gryff")!!
                game.state.projectedState.hasKeyword(gryff, Keyword.FLYING) shouldBe true
            }

            test("a four-mana emerge cast explains itself: the sacrifice and the mana paid are visible") {
                // The board from a real game that read as a bug: four lands, a mana-value-2 Spirit,
                // and a {7} Eldrazi hitting the stack. Emerge {5}{U} − 2 = {3}{U} is exactly four
                // mana (CR 702.119a), so the arithmetic is right — it was only invisible. Both halves
                // of it now ride on the cast, for the caster and the opponent alike.
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Wretched Gryff")
                    .withCardOnBattlefield(1, "Niblis of the Urn") // {1}{W} → mana value 2
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withLandsOnBattlefield(1, "Island", 1)
                    .build()

                val cast = game.castSpellWithEmerge(1, "Wretched Gryff", "Niblis of the Urn")
                withClue("four lands is enough for emerge {5}{U} minus a 2-drop: ${cast.error}") {
                    cast.error shouldBe null
                }

                val castEvent = cast.events.filterIsInstance<SpellCastEvent>().single { it.cardName == "Wretched Gryff" }
                withClue("the cast event names what paid for it") {
                    castEvent.sacrificedAsCostNames shouldBe listOf("Niblis of the Urn")
                    castEvent.totalManaSpent shouldBe 4
                }

                // Both seats read the stack off the same client view; assert the opponent's.
                val gryffOnStack = game.getClientState(2).cards.values.single { it.name == "Wretched Gryff" }
                withClue("stack badges: ${gryffOnStack.castProvenanceLabel} / ${gryffOnStack.costSacrificeLabel} / ${gryffOnStack.manaPaidCost}") {
                    gryffOnStack.castProvenanceLabel shouldBe "Emerge"
                    gryffOnStack.costSacrificeLabel shouldBe "Sacrificed Niblis of the Urn"
                    // Three Plains and an Island paid {3}{U}.
                    gryffOnStack.manaPaidCost shouldBe "{W}{W}{W}{U}"
                }
            }

            test("a hard cast keeps the stack card free of alternative-cost badges") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Wretched Gryff")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Island", 7)
                    .build()

                game.castSpell(1, "Wretched Gryff").error shouldBe null

                val gryffOnStack = game.getClientState(2).cards.values.single { it.name == "Wretched Gryff" }
                withClue("nothing about a normal cast needs explaining") {
                    gryffOnStack.castProvenanceLabel shouldBe null
                    gryffOnStack.costSacrificeLabel shouldBe null
                    gryffOnStack.manaPaidCost shouldBe null
                }
            }

            test("hard cast for {7} draws a card and sacrifices nothing") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Wretched Gryff")
                    .withCardOnBattlefield(1, "Centaur Courser")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Island", 7)
                    .build()

                val cast = game.castSpell(1, "Wretched Gryff")
                withClue("the hard cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                game.isInHand(1, "Grizzly Bears") shouldBe true
                game.isOnBattlefield("Wretched Gryff") shouldBe true
                withClue("no emerge cost was chosen, so nothing was sacrificed") {
                    game.isOnBattlefield("Centaur Courser") shouldBe true
                }
            }
        }
    }
}
