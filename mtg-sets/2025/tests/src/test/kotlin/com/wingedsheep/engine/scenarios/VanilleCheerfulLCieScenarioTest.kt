package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.MeldedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fin.cards.VanilleCheerfulLCie
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import com.wingedsheep.engine.core.Outcome

/**
 * Vanille, Cheerful l'Cie (FIN).
 *
 * "When Vanille enters, mill two cards, then return a permanent card from your graveyard to your
 * hand."
 * "At the beginning of your first main phase, if you both own and control Vanille and a creature
 * named Fang, Fearless l'Cie, you may pay {3}{B}{G}. If you do, exile them, then meld them into
 * Ragnarok, Divine Deliverance." (CR 701.42)
 *
 * The ETB test casts Vanille for real so its enters-the-battlefield trigger fires, then resolves the
 * mill and the resolution-time choice: a permanent card already in the graveyard (Grizzly Bears)
 * is offered and returned to hand. Two cards are milled off the top of the library on the way.
 */
class VanilleCheerfulLCieScenarioTest : ScenarioTestBase() {
    init {
        fun createDriver(): GameTestDriver {
            val driver = GameTestDriver()
            driver.registerCards(TestCards.all + listOf(VanilleCheerfulLCie))
            return driver
        }

        test("enters: mill two, then return a permanent card from your graveyard to your hand") {
            val driver = createDriver()
            driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
            val you = driver.activePlayer!!

            // A permanent card sitting in your graveyard is the intended return target.
            val gyBear = driver.putCardInGraveyard(you, "Grizzly Bears")
            val libraryBefore = driver.state.getZone(ZoneKey(you, Zone.LIBRARY)).size
            val graveyardBefore = driver.getGraveyard(you).size

            // Cast Vanille so its ETB trigger fires.
            val vanille = driver.putCardInHand(you, "Vanille, Cheerful l'Cie")
            driver.giveMana(you, Color.GREEN, 4) // {3}{G}
            driver.castSpell(you, vanille).outcome shouldBe Outcome.Done

            // Resolve the creature spell and its ETB trigger (mill 2) until the return choice pauses.
            var guard = 0
            while (!driver.isPaused && driver.state.stack.isNotEmpty() && guard++ < 20) driver.bothPass()

            // Two cards were milled from the library into the graveyard.
            driver.state.getZone(ZoneKey(you, Zone.LIBRARY)).size shouldBe libraryBefore - 2

            // The ability offers a permanent card from your graveyard; Grizzly Bears is among the options.
            driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
            val choice = driver.pendingDecision as SelectCardsDecision
            withClue("Grizzly Bears is offered as a permanent card to return") {
                choice.playerId shouldBe you
                choice.options.contains(gyBear) shouldBe true
            }
            driver.submitCardSelection(you, listOf(gyBear))
            while (!driver.isPaused && driver.state.stack.isNotEmpty()) driver.bothPass()

            // Grizzly Bears moved from graveyard to hand; the two milled cards remain in the graveyard.
            withClue("Grizzly Bears is now in hand and no longer in the graveyard") {
                driver.getHand(you).contains(gyBear) shouldBe true
                driver.getGraveyard(you).contains(gyBear) shouldBe false
            }
            // Net graveyard: started with Grizzly Bears (graveyardBefore), +2 milled, −1 returned.
            driver.getGraveyard(you).size shouldBe graveyardBefore + 2 - 1
        }

        fun meldBoard() = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Vanille, Cheerful l'Cie")
            .withCardOnBattlefield(1, "Fang, Fearless l'Cie")
            .withLandsOnBattlefield(1, "Swamp", 3)
            .withLandsOnBattlefield(1, "Forest", 2)
            .withCardInLibrary(1, "Forest")
            .withCardInLibrary(1, "Forest")
            .withActivePlayer(1)
            .inPhase(Phase.BEGINNING, Step.UPKEEP)
            .build()

        test("first main phase with Fang: paying {3}{B}{G} melds the pair into Ragnarok, Divine Deliverance") {
            val game = meldBoard()
            val vanille = game.findPermanent("Vanille, Cheerful l'Cie")!!

            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.resolveStack()

            withClue("the trigger offers the may-pay {3}{B}{G}") {
                (game.getPendingDecision() is YesNoDecision) shouldBe true
            }
            game.answerYesNo(true).error shouldBe null
            if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay().error shouldBe null
            game.resolveStack()

            val ragnarok = game.findPermanent("Ragnarok, Divine Deliverance").shouldNotBeNull()
            withClue("Vanille's entity is the melded permanent") { ragnarok shouldBe vanille }
            game.findPermanent("Vanille, Cheerful l'Cie") shouldBe null
            game.findPermanent("Fang, Fearless l'Cie") shouldBe null
            game.isInExile(1, "Vanille, Cheerful l'Cie") shouldBe false
            game.isInExile(1, "Fang, Fearless l'Cie") shouldBe false
            game.state.getEntity(ragnarok)!!.has<MeldedComponent>() shouldBe true
            game.state.projectedState.getPower(ragnarok) shouldBe 7
            game.state.projectedState.getToughness(ragnarok) shouldBe 6
        }

        test("a Fang you control but don't own: the trigger never offers the payment") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Vanille, Cheerful l'Cie")
                .withCardOnBattlefield(2, "Fang, Fearless l'Cie")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.BEGINNING, Step.UPKEEP)
                .build()
            val fang = game.findPermanent("Fang, Fearless l'Cie")!!
            game.state = game.state.updateEntity(fang) { it.with(ControllerComponent(game.player1Id)) }

            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

            withClue("the intervening-if fails, so nothing goes on the stack and no payment is offered") {
                game.state.stack shouldBe emptyList()
                game.getPendingDecision() shouldBe null
            }
            game.findPermanent("Ragnarok, Divine Deliverance") shouldBe null
        }

        test("declining the payment leaves Vanille and Fang on the battlefield") {
            val game = meldBoard()

            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.resolveStack()

            (game.getPendingDecision() is YesNoDecision) shouldBe true
            game.answerYesNo(false).error shouldBe null
            game.resolveStack()

            game.findPermanent("Vanille, Cheerful l'Cie").shouldNotBeNull()
            game.findPermanent("Fang, Fearless l'Cie").shouldNotBeNull()
            game.findPermanent("Ragnarok, Divine Deliverance") shouldBe null
        }
    }
}
