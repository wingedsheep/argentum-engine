package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Polygraph Orb (MKM #99) — {4}{B} Artifact.
 *
 * "When this artifact enters, look at the top four cards of your library. Put two of them into your
 *  hand and the rest into your graveyard. You lose 2 life.
 *  {2}, {T}, Collect evidence 3: Each opponent loses 3 life unless they discard a card or sacrifice
 *  a creature."
 *
 * The activated ability is a **punisher**, not an edict, and that is the distinction these tests are
 * built around: the "unless" is a choice the *opponent* makes, so "lose 3 life" carries no
 * feasibility gate and stays on the menu even for an opponent holding a hand full of cards and a
 * board full of creatures. The official ruling is explicit about it. Gating that option — the
 * natural mistake — would silently turn the card into an edict.
 *
 * Collect evidence 3 is a real cost atom alongside `{2}` and `{T}`, so CR 701.59b makes the ability
 * simply unactivatable under the threshold, and the exile happens during activation where no
 * opponent can respond to strand it.
 */
class PolygraphOrbScenarioTest : ScenarioTestBase() {

    /** Activate the Orb's only activated ability, letting the engine auto-pay. */
    private fun TestGame.activateOrb() = execute(
        ActivateAbility(
            playerId = player1Id,
            sourceId = findPermanent("Polygraph Orb") ?: error("Polygraph Orb is not on the battlefield"),
            abilityId = cardRegistry.getCard("Polygraph Orb")!!.activatedAbilities[0].id,
            paymentStrategy = PaymentStrategy.AutoPay,
        )
    )

    /** A board where the Orb is online: two lands for {2} and a mana-value-3 card to exile. */
    private fun orbReady() = scenario()
        .withPlayers("Caster", "Opponent")
        .withCardOnBattlefield(1, "Polygraph Orb")
        .withCardInGraveyard(1, "Centaur Courser")
        .withLandsOnBattlefield(1, "Swamp", 2)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    init {
        test("entering keeps two cards, bins the rest, and costs 2 life") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Polygraph Orb")
                .withLandsOnBattlefield(1, "Swamp", 5)
                // The builder seeds no library, so the top four have to be put there explicitly.
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Centaur Courser")
                .withCardInLibrary(1, "Lightning Bolt")
                .withCardInLibrary(1, "Savannah Lions")
                .withCardInLibrary(1, "Doom Blade")
                .withCardInLibrary(1, "Giant Growth")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val life = game.getLifeTotal(1)
            val hand = game.handSize(1)
            val graveyard = game.graveyardSize(1)
            val library = game.librarySize(1)

            game.castSpell(1, "Polygraph Orb").error shouldBe null
            game.resolveStack()

            val look = game.getPendingDecision() as? SelectCardsDecision
            look.shouldNotBeNull()
            withClue("look at the top four, keep exactly two") {
                look.options.size shouldBe 4
                look.minSelections shouldBe 2
                look.maxSelections shouldBe 2
            }
            game.selectCards(look.options.take(2)).error shouldBe null

            // The Orb left hand and the two kept cards arrived, so the net hand change is +1.
            game.handSize(1) shouldBe hand + 1
            game.graveyardSize(1) shouldBe graveyard + 2
            game.librarySize(1) shouldBe library - 4
            withClue("the life loss is unconditional") {
                game.getLifeTotal(1) shouldBe life - 2
            }
        }

        test("the opponent may take the 3 life even holding a card and a creature") {
            val game = orbReady()
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInHand(2, "Centaur Courser")
                .build()

            val life = game.getLifeTotal(2)
            val hand = game.handSize(2)

            game.activateOrb().error shouldBe null
            game.resolveStack()

            val choice = game.getPendingDecision() as? ChooseOptionDecision
            choice.shouldNotBeNull()
            choice.playerId shouldBe game.player2Id
            withClue("all three options are live — 'lose 3 life' carries no feasibility gate") {
                choice.options.size shouldBe 3
            }
            // Option 2 = "Lose 3 life".
            game.submitDecision(OptionChosenResponse(choice.id, 2)).error shouldBe null

            withClue("a punisher, not an edict: nothing was taken from them") {
                game.getLifeTotal(2) shouldBe life - 3
                game.handSize(2) shouldBe hand
                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }
        }

        test("an opponent who discards takes no damage") {
            val game = orbReady()
                .withCardOnBattlefield(2, "Grizzly Bears")
                // Two cards, so the discard is a genuine choice rather than an auto-pick.
                .withCardInHand(2, "Centaur Courser")
                .withCardInHand(2, "Lightning Bolt")
                .build()

            val life = game.getLifeTotal(2)
            val hand = game.handSize(2)

            game.activateOrb().error shouldBe null
            game.resolveStack()

            val choice = game.getPendingDecision() as? ChooseOptionDecision
            choice.shouldNotBeNull()
            // Option 0 = "Discard a card".
            game.submitDecision(OptionChosenResponse(choice.id, 0)).error shouldBe null
            // The discard pauses to let the opponent pick which card.
            game.getPendingDecision().shouldNotBeNull()
            game.selectCards(game.findCardsInHand(2, "Centaur Courser")).error shouldBe null

            game.handSize(2) shouldBe hand - 1
            game.isInGraveyard(2, "Centaur Courser") shouldBe true
            game.getLifeTotal(2) shouldBe life
            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }

        test("an opponent who sacrifices a creature takes no damage") {
            val game = orbReady()
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInHand(2, "Centaur Courser")
                .build()

            val life = game.getLifeTotal(2)
            val hand = game.handSize(2)

            game.activateOrb().error shouldBe null
            game.resolveStack()

            val choice = game.getPendingDecision() as? ChooseOptionDecision
            choice.shouldNotBeNull()
            // Option 1 = "Sacrifice a creature".
            game.submitDecision(OptionChosenResponse(choice.id, 1)).error shouldBe null
            if (game.getPendingDecision() is SelectCardsDecision) {
                game.selectCards(listOfNotNull(game.findPermanent("Grizzly Bears")))
            }

            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            game.getLifeTotal(2) shouldBe life
            game.handSize(2) shouldBe hand
        }

        test("with an empty hand and no creatures, losing 3 life is the only option left") {
            val game = orbReady().build()

            val life = game.getLifeTotal(2)

            game.activateOrb().error shouldBe null
            game.resolveStack()

            val choice = game.getPendingDecision()
            if (choice is ChooseOptionDecision) {
                withClue("the two infeasible options hide themselves") {
                    choice.options.size shouldBe 1
                }
                game.submitDecision(OptionChosenResponse(choice.id, 0)).error shouldBe null
            }
            withClue("either way the opponent ends up 3 life down") {
                game.getLifeTotal(2) shouldBe life - 3
            }
        }

        test("CR 701.59b — the ability is unactivatable below the evidence threshold") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardOnBattlefield(1, "Polygraph Orb")
                // Mana value 1, short of 3.
                .withCardInGraveyard(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val life = game.getLifeTotal(2)

            game.getLegalActions(1)
                .filter { it.description.contains("Each opponent loses 3 life") }
                .none { it.isAffordable } shouldBe true

            game.activateOrb().error.shouldNotBeNull()
            withClue("nothing was spent and nothing happened") {
                game.isInGraveyard(1, "Lightning Bolt") shouldBe true
                game.getLifeTotal(2) shouldBe life
            }
        }

        test("activating exiles the evidence as a cost") {
            val game = orbReady()
                .withCardOnBattlefield(2, "Grizzly Bears")
                .build()

            game.activateOrb().error shouldBe null

            withClue("costs are paid during activation, before anyone can respond") {
                game.isInExile(1, "Centaur Courser") shouldBe true
                game.isInGraveyard(1, "Centaur Courser") shouldBe false
            }
        }
    }
}
