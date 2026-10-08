package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Titania, Voice of Gaea (BRO #193) — {1}{G}{G} Legendary Creature — Elemental, 3/4.
 *
 *   Reach
 *   Whenever one or more land cards are put into your graveyard from anywhere, you gain 2 life.
 *   At the beginning of your upkeep, if there are four or more land cards in your graveyard and you
 *   both own and control Titania, Voice of Gaea and a land named Argoth, Sanctum of Nature, exile
 *   them, then meld them into Titania, Gaea Incarnate.
 *
 * Pins the real meld through the card pair: the upkeep trigger exiles both and puts Titania, Gaea
 * Incarnate onto the battlefield, whose own enters trigger returns the graveyard lands tapped. The
 * intervening if keeps the trigger off the stack with only three lands in the graveyard or without
 * Argoth. The land-to-graveyard trigger is a batch trigger: three lands milled at once gain 2 life,
 * not 6.
 */
class TitaniaVoiceOfGaeaScenarioTest : ScenarioTestBase() {

    private fun upkeepBoard(landsInGraveyard: Int, withArgoth: Boolean = true, otherLands: Int = 2): TestGame {
        var b = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Titania, Voice of Gaea")
            .withCardInLibrary(1, "Forest")
            .withCardInLibrary(1, "Forest")
            .withCardInLibrary(2, "Forest")
            .withCardInLibrary(2, "Forest")
            .withActivePlayer(2)
            .inPhase(Phase.ENDING, Step.END)
        if (withArgoth) b = b.withCardOnBattlefield(1, "Argoth, Sanctum of Nature")
        if (otherLands > 0) b = b.withLandsOnBattlefield(1, "Plains", otherLands)
        repeat(landsInGraveyard) { b = b.withCardInGraveyard(1, "Forest") }
        return b.build()
    }

    init {
        context("upkeep meld") {
            test("four lands in the graveyard: Titania and Argoth meld into Titania, Gaea Incarnate") {
                val game = upkeepBoard(landsInGraveyard = 4)

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.state.activePlayerId shouldBe game.player1Id
                withClue("the meld trigger is on the stack") { game.state.stack.size shouldBe 1 }
                game.resolveStack()

                val titania = game.findPermanent("Titania, Gaea Incarnate").shouldNotBeNull()
                game.findPermanent("Titania, Voice of Gaea") shouldBe null
                game.findPermanent("Argoth, Sanctum of Nature") shouldBe null
                game.isInExile(1, "Titania, Voice of Gaea") shouldBe false
                game.isInExile(1, "Argoth, Sanctum of Nature") shouldBe false

                withClue("its enters trigger returned all four graveyard lands, tapped") {
                    val forests = game.findPermanents("Forest")
                    forests.size shouldBe 4
                    forests.all { game.state.getEntity(it)!!.has<TappedComponent>() } shouldBe true
                    game.graveyardSize(1) shouldBe 0
                }
                withClue("power and toughness equal the six lands you control (two Plains + four Forests)") {
                    game.state.projectedState.getPower(titania) shouldBe 6
                    game.state.projectedState.getToughness(titania) shouldBe 6
                }
            }

            test("with no other lands the melded Titania enters as 0/0 and dies before its enters trigger resolves") {
                // Argoth is part of the melded creature, not a land you control, so Titania's CDA
                // reads 0 as it enters; SBAs put both cards into the graveyard. Its enters trigger
                // still resolves and returns every land card there — Argoth included.
                val game = upkeepBoard(landsInGraveyard = 4, otherLands = 0)

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()

                game.findPermanent("Titania, Gaea Incarnate") shouldBe null
                game.isInGraveyard(1, "Titania, Voice of Gaea") shouldBe true
                val argoth = game.findPermanent("Argoth, Sanctum of Nature").shouldNotBeNull()
                game.state.getEntity(argoth)!!.has<TappedComponent>() shouldBe true
                game.findPermanents("Forest").size shouldBe 4
            }

            test("three lands in the graveyard: the intervening if keeps the trigger off the stack") {
                val game = upkeepBoard(landsInGraveyard = 3)

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.state.stack.size shouldBe 0
                game.resolveStack()

                game.findPermanent("Titania, Voice of Gaea").shouldNotBeNull()
                game.findPermanent("Argoth, Sanctum of Nature").shouldNotBeNull()
                game.findPermanent("Titania, Gaea Incarnate") shouldBe null
                game.graveyardSize(1) shouldBe 3
            }

            test("no Argoth: nothing triggers and Titania stays") {
                val game = upkeepBoard(landsInGraveyard = 5, withArgoth = false)

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.state.stack.size shouldBe 0
                game.findPermanent("Titania, Voice of Gaea").shouldNotBeNull()
                game.isInExile(1, "Titania, Voice of Gaea") shouldBe false
            }
        }

        context("land cards to your graveyard") {
            fun millBoard(libraryCard: String): TestGame {
                var b = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Titania, Voice of Gaea")
                    .withCardOnBattlefield(1, "Argoth, Sanctum of Nature")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(4) { b = b.withCardInLibrary(1, libraryCard) }
                return b.build()
            }

            fun TestGame.activateArgoth() {
                val argoth = findPermanent("Argoth, Sanctum of Nature")!!
                val abilityId = cardRegistry.getCard("Argoth, Sanctum of Nature")!!.script.activatedAbilities[1].id
                execute(ActivateAbility(player1Id, argoth, abilityId)).error shouldBe null
                resolveStack()
            }

            test("three lands milled at once gain 2 life, once") {
                val game = millBoard("Forest")
                game.activateArgoth()

                game.graveyardSize(1) shouldBe 3
                game.getLifeTotal(1) shouldBe 22
            }

            test("milling nonland cards gains nothing") {
                val game = millBoard("Grizzly Bears")
                game.activateArgoth()

                game.graveyardSize(1) shouldBe 3
                game.getLifeTotal(1) shouldBe 20
            }
        }
    }
}
