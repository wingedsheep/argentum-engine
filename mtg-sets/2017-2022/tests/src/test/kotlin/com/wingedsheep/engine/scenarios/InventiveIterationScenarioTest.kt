package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.player.CantCastSpellsComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Inventive Iteration // Living Breakthrough (NEO #57).
 *
 *   I — Return up to one target creature or planeswalker to its owner's hand.
 *   II — Return an artifact card from your graveyard to your hand. If you can't, draw a card.
 *   III — Exile this Saga, then return it to the battlefield transformed under your control.
 *   Back: Flying. Whenever you cast a spell, your opponents can't cast spells with the same mana
 *   value as that spell until your next turn.
 *
 * The ban's own rules (lock-in, expiry) live in `FilteredCastRestrictionTest`; this walks the card
 * through its chapters and pins the back face's ban on a real opponent turn.
 */
class InventiveIterationScenarioTest : ScenarioTestBase() {

    init {
        fun TestGame.toNextMain() {
            passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        }

        fun TestGame.toMyNextMain() {
            toNextMain()
            toNextMain()
            resolveStack()
        }

        fun TestGame.opponentBanned() =
            state.getEntity(player2Id)?.get<CantCastSpellsComponent>() != null

        fun setup(artifactInGraveyard: Boolean) = scenario()
            .withPlayers("Player", "Opponent")
            .withCardInHand(1, "Inventive Iteration")
            .withCardInHand(1, "Grizzly Bears")
            .withLandsOnBattlefield(1, "Island", 4)
            .withLandsOnBattlefield(1, "Forest", 2)
            .withCardOnBattlefield(2, "Grizzly Bears")
            .withCardInHand(2, "Shock")
            .withLandsOnBattlefield(2, "Mountain", 1)
            .withLandsOnBattlefield(2, "Forest", 2)
            .apply { if (artifactInGraveyard) withCardInGraveyard(1, "Ornithopter") }
            .apply { repeat(8) { withCardInLibrary(1, "Island"); withCardInLibrary(2, "Island") } }
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        context("Inventive Iteration") {

            test("chapter I bounces, chapter II returns an artifact, chapter III flips it; the ban locks the mana value until my next turn") {
                val game = setup(artifactInGraveyard = true)
                val theirBears = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Inventive Iteration").error shouldBe null
                game.resolveStack()
                if (game.hasPendingDecision()) game.selectTargets(listOf(theirBears))
                game.resolveStack()
                withClue("chapter I returned their creature to its owner's hand") {
                    game.isInHand(2, "Grizzly Bears") shouldBe true
                }

                game.toMyNextMain()
                if (game.hasPendingDecision()) game.selectCards(listOf(game.findCardsInGraveyard(1, "Ornithopter").first()))
                game.resolveStack()
                withClue("chapter II returned the artifact card instead of drawing") {
                    game.isInHand(1, "Ornithopter") shouldBe true
                }

                game.toMyNextMain()
                withClue("chapter III returned it transformed") {
                    game.findPermanent("Inventive Iteration") shouldBe null
                    game.isOnBattlefield("Living Breakthrough") shouldBe true
                }

                game.castSpell(1, "Grizzly Bears").error shouldBe null
                game.resolveStack()
                withClue("the ban outlives the triggering spell, which has resolved") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                    game.opponentBanned() shouldBe true
                }

                game.toNextMain()
                withClue("on their turn they can't cast a mana value 2 spell, but mana value 1 is fine") {
                    game.castSpell(2, "Grizzly Bears").error shouldNotBe null
                    val breakthrough = game.findPermanent("Living Breakthrough")!!
                    game.castSpell(2, "Shock", breakthrough).error shouldBe null
                }
                game.resolveStack()

                game.toNextMain()
                withClue("the ban lifts as my next turn begins") {
                    game.opponentBanned() shouldBe false
                }
            }

            test("chapter II draws a card when there is no artifact card to return") {
                val game = setup(artifactInGraveyard = false)

                game.castSpell(1, "Inventive Iteration").error shouldBe null
                game.resolveStack()
                if (game.hasPendingDecision()) game.selectTargets(emptyList())
                game.resolveStack()

                val before = game.handSize(1)
                game.toMyNextMain()
                withClue("draw step plus chapter II's fallback draw") {
                    game.handSize(1) shouldBe before + 2
                }
            }
        }
    }
}
