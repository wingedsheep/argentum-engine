package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * Lukka, Coppercoat Outcast (IKO #125) — {3}{R}{R} planeswalker, loyalty 5.
 *
 *   +1: Exile the top three cards of your library. Creature cards exiled this way gain "You may
 *       cast this card from exile as long as you control a Lukka planeswalker."
 *   −2: Exile target creature you control, then reveal cards from the top of your library until
 *       you reveal a creature card with greater mana value. Put that card onto the battlefield and
 *       the rest on the bottom of your library in a random order.
 *   −7: Each creature you control deals damage equal to its power to each opponent.
 *
 * Pins: the +1 permission covers only the creature cards and only while a Lukka is controlled;
 * the −2 skips creatures of lesser *and equal* mana value; the −7 sums every creature's power.
 */
class LukkaCoppercoatOutcastScenarioTest : ScenarioTestBase() {

    private fun loyalty(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    /** Seed a planeswalker's loyalty (withCardOnBattlefield doesn't stamp it). */
    private fun seedLoyalty(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with(CountersComponent().withAdded(CounterType.LOYALTY, amount))
        }
    }

    private fun ability(index: Int) =
        cardRegistry.getCard("Lukka, Coppercoat Outcast")!!.script.activatedAbilities[index]

    private fun canCast(game: TestGame, cardId: EntityId): Boolean =
        game.getLegalActions(1).any { (it.action as? CastSpell)?.cardId == cardId }

    init {
        fun builder() = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Lukka, Coppercoat Outcast")
            .withLandsOnBattlefield(1, "Mountain", 4)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        fun TestGame.lukka(): EntityId = findPermanent("Lukka, Coppercoat Outcast")!!

        test("+1 exiles the top three; only the creature cards among them become castable") {
            val game = builder()
                .withCardInLibrary(1, "Hill Giant")
                .withCardInLibrary(1, "Lightning Bolt")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(1, "Forest")
                .build()
            val lukka = game.lukka()
            seedLoyalty(game, lukka, 5)
            val giant = game.findCardsInLibrary(1, "Hill Giant").single()
            val bolt = game.findCardsInLibrary(1, "Lightning Bolt").single()

            game.execute(ActivateAbility(game.player1Id, lukka, ability(0).id)).error shouldBe null
            game.resolveStack()

            loyalty(game, lukka) shouldBe 6
            withClue("the top three cards were exiled") {
                game.librarySize(1) shouldBe 1
                game.isInExile(1, "Hill Giant") shouldBe true
                game.isInExile(1, "Lightning Bolt") shouldBe true
                game.isInExile(1, "Mountain") shouldBe true
            }
            withClue("the creature card is castable from exile while you control Lukka") {
                canCast(game, giant) shouldBe true
            }
            withClue("a noncreature card exiled this way gains nothing") {
                canCast(game, bolt) shouldBe false
            }

            game.castSpellFromExile(1, "Hill Giant").error shouldBe null
            game.resolveStack()
            game.isOnBattlefield("Hill Giant") shouldBe true
        }

        test("the exiled creature can't be cast once you no longer control a Lukka planeswalker") {
            val game = builder()
                .withCardInLibrary(1, "Hill Giant")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .withCardInHand(2, "Lightning Bolt")
                .withLandsOnBattlefield(2, "Mountain", 1)
                .build()
            val lukka = game.lukka()
            seedLoyalty(game, lukka, 2)
            val giant = game.findCardsInLibrary(1, "Hill Giant").single()

            game.execute(ActivateAbility(game.player1Id, lukka, ability(0).id)).error shouldBe null
            game.passPriority()
            withClue("the opponent bolts Lukka (3 loyalty) in response") {
                game.castSpell(2, "Lightning Bolt", lukka).error shouldBe null
            }
            game.resolveStack()

            game.isOnBattlefield("Lukka, Coppercoat Outcast") shouldBe false
            game.isInExile(1, "Hill Giant") shouldBe true
            withClue("no Lukka is controlled, so the permission is closed") {
                canCast(game, giant) shouldBe false
            }
        }

        test("−2 exiles the creature and puts the first creature card of greater mana value onto the battlefield") {
            val game = builder()
                .withCardOnBattlefield(1, "Grizzly Bears")              // MV 2 — exiled
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(1, "Raging Goblin")                  // MV 1 — lesser
                .withCardInLibrary(1, "Grizzly Bears")                  // MV 2 — equal, not greater
                .withCardInLibrary(1, "Hill Giant")                     // MV 4 — the match
                .withCardInLibrary(1, "Gray Ogre")                      // never revealed
                .build()
            val lukka = game.lukka()
            seedLoyalty(game, lukka, 5)
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findCardsInLibrary(1, "Hill Giant").single()
            val ogre = game.findCardsInLibrary(1, "Gray Ogre").single()
            val bottomed = game.state.getLibrary(game.player1Id).take(3)

            game.execute(
                ActivateAbility(game.player1Id, lukka, ability(1).id, listOf(ChosenTarget.Permanent(bears)))
            ).error shouldBe null
            game.resolveStack()

            loyalty(game, lukka) shouldBe 3
            withClue("the targeted creature was exiled") { game.isInExile(1, "Grizzly Bears") shouldBe true }
            withClue("the first creature card with greater mana value entered") {
                game.findPermanent("Hill Giant") shouldBe giant
            }
            val library = game.state.getLibrary(game.player1Id)
            withClue("the unrevealed card stays on top; the other revealed cards went to the bottom") {
                library.size shouldBe 4
                library.first() shouldBe ogre
                library.drop(1) shouldContainExactlyInAnyOrder bottomed
            }
        }

        test("−7 has each creature you control deal damage equal to its power to each opponent") {
            val game = builder()
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardOnBattlefield(2, "Gray Ogre")
                .build()
            val lukka = game.lukka()
            seedLoyalty(game, lukka, 7)

            game.execute(ActivateAbility(game.player1Id, lukka, ability(2).id)).error shouldBe null
            game.resolveStack()

            withClue("2 + 3 damage to the opponent; the opponent's creature deals none") {
                game.getLifeTotal(2) shouldBe 15
            }
            game.getLifeTotal(1) shouldBe 20
        }
    }
}
