package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CastChoicesComponent
import com.wingedsheep.engine.state.components.battlefield.ChoiceValue
import com.wingedsheep.engine.state.components.battlefield.chosenColor
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TextChanges
import com.wingedsheep.engine.state.components.identity.TextReplacement
import com.wingedsheep.engine.state.components.identity.TextReplacementCategory
import com.wingedsheep.engine.state.components.identity.TextReplacementComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Swirl the Mists (CHK #94) — "As this enchantment enters, choose a color word. All instances of
 * color words in the text of spells and permanents are changed to the chosen color word."
 *
 * A global Layer 3 text change (CR 613.1c): protection colors, color-word filters on statics and
 * spell targets all read as the chosen color while Swirl is on the battlefield, and go back the
 * moment it leaves. Mana symbols and objects' own colors are untouched (CR 612.2).
 */
class SwirlTheMistsScenarioTest : ScenarioTestBase() {

    private fun TestGame.chooseSwirlColor(color: Color) {
        val swirl = findPermanent("Swirl the Mists")!!
        state = state.updateEntity(swirl) { c ->
            c.with(CastChoicesComponent(chosen = mapOf(ChoiceSlot.COLOR to ChoiceValue.ColorChoice(color))))
        }
    }

    private fun TestGame.keywords(id: EntityId): Set<String> = state.projectedState.getKeywords(id)

    private fun TestGame.doomBladeTargets(): List<EntityId> =
        getLegalActions(1)
            .filter { (it.action as? CastSpell)?.let { a -> state.getEntity(a.cardId)?.get<CardComponent>()?.name } == "Doom Blade" }
            .flatMap { it.validTargets.orEmpty() }

    init {
        context("Swirl the Mists — the entry color choice") {
            test("casting it asks for a color and records it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Swirl the Mists")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withCardOnBattlefield(2, "White Knight") // protection from black
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Swirl the Mists")
                withClue("casting Swirl the Mists should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()
                val decision = game.state.pendingDecision
                withClue("Swirl the Mists pauses for its color choice") { (decision is ChooseColorDecision) shouldBe true }
                game.submitDecision(ColorChosenResponse(decision!!.id, Color.GREEN))
                game.resolveStack()

                val swirl = game.findPermanent("Swirl the Mists")!!
                game.state.getEntity(swirl)!!.chosenColor() shouldBe Color.GREEN
                val knight = game.findPermanent("White Knight")!!
                withClue("protection from black now reads protection from green") {
                    game.keywords(knight) shouldContain "PROTECTION_FROM_GREEN"
                    game.keywords(knight) shouldNotContain "PROTECTION_FROM_BLACK"
                }
            }
        }

        context("Swirl the Mists — what the text change reaches") {
            test("protection colors change, but the object's own color does not") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Swirl the Mists")
                    .withCardOnBattlefield(2, "White Knight")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.chooseSwirlColor(Color.BLUE)

                val knight = game.findPermanent("White Knight")!!
                game.keywords(knight) shouldContain "PROTECTION_FROM_BLUE"
                game.keywords(knight) shouldNotContain "PROTECTION_FROM_BLACK"
                withClue("an object's color comes from its mana cost, not a color word (CR 612.2)") {
                    game.state.projectedState.getColors(knight) shouldBe setOf(Color.WHITE.name)
                }
            }

            test("a static ability's color-word filter reads the chosen color") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Swirl the Mists")
                    .withCardOnBattlefield(1, "Crusade") // white creatures get +1/+1
                    .withCardOnBattlefield(1, "Savannah Lions") // white 1/1 (test registry)
                    .withCardOnBattlefield(1, "Grizzly Bears") // green 2/2
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.chooseSwirlColor(Color.GREEN)

                val lions = game.findPermanent("Savannah Lions")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                withClue("Crusade now pumps green creatures") {
                    game.state.projectedState.getPower(bears) shouldBe 3
                    game.state.projectedState.getToughness(bears) shouldBe 3
                }
                withClue("and no longer the white one") {
                    game.state.projectedState.getPower(lions) shouldBe 1
                    game.state.projectedState.getToughness(lions) shouldBe 1
                }
            }

            test("a spell's targets are chosen against the changed text") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Swirl the Mists")
                    .withCardInHand(1, "Doom Blade") // destroy target nonblack creature
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardOnBattlefield(2, "Air Elemental") // blue
                    .withCardOnBattlefield(2, "Hill Giant") // red
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.chooseSwirlColor(Color.BLUE)

                val elemental = game.findPermanent("Air Elemental")!!
                val giant = game.findPermanent("Hill Giant")!!
                withClue("\"nonblack\" reads \"nonblue\": the blue creature is off-limits, the red one isn't") {
                    game.doomBladeTargets() shouldContain giant
                    game.doomBladeTargets() shouldNotContain elemental
                }
                withClue("the engine rejects a cast the enumerator didn't offer") {
                    game.castSpell(1, "Doom Blade", elemental).error shouldNotBe null
                }

                game.castSpell(1, "Doom Blade", giant).error shouldBe null
                game.resolveStack()
                game.isInGraveyard(2, "Hill Giant") shouldBe true
            }

            test("the change ends as soon as Swirl the Mists leaves the battlefield") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Swirl the Mists")
                    .withCardOnBattlefield(2, "White Knight")
                    .withCardInHand(2, "Disenchant")
                    .withLandsOnBattlefield(2, "Plains", 2)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.chooseSwirlColor(Color.RED)

                val knight = game.findPermanent("White Knight")!!
                game.keywords(knight) shouldContain "PROTECTION_FROM_RED"

                val swirl = game.findPermanent("Swirl the Mists")!!
                game.castSpell(2, "Disenchant", swirl).error shouldBe null
                game.resolveStack()
                game.isInGraveyard(1, "Swirl the Mists") shouldBe true

                game.keywords(knight) shouldContain "PROTECTION_FROM_BLACK"
                game.keywords(knight) shouldNotContain "PROTECTION_FROM_RED"
            }

            test("a later targeted text change reads the already-changed text") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Swirl the Mists")
                    .withCardOnBattlefield(2, "White Knight")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.chooseSwirlColor(Color.BLUE)

                // Crystal Spray-style "Blue → Green" on the Knight after Swirl made every color word blue.
                val knight = game.findPermanent("White Knight")!!
                game.state = game.state.updateEntity(knight) { c ->
                    c.with(TextReplacementComponent(listOf(TextReplacement("Blue", "Green", TextReplacementCategory.COLOR_WORD))))
                }
                game.keywords(knight) shouldContain "PROTECTION_FROM_GREEN"
                game.keywords(knight) shouldNotContain "PROTECTION_FROM_BLUE"
            }

            test("cards outside the battlefield and stack keep their text") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Swirl the Mists")
                    .withCardInGraveyard(2, "White Knight")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.chooseSwirlColor(Color.BLUE)

                val knight = game.state.getZone(ZoneKey(game.player2Id, Zone.GRAVEYARD)).single()
                TextChanges.of(game.state, knight) shouldBe null
            }
        }
    }
}
