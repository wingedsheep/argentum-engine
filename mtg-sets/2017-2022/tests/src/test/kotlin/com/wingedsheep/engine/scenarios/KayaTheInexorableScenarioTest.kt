package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.ForetoldComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.khm.cards.KayaTheInexorable
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Kaya the Inexorable (KHM #218): the +1's ghostform rider returns the creature to hand and makes
 * a Spirit whether it dies or is exiled, and the −7 emblem casts a legendary card you own from
 * hand, graveyard or exile for free at your upkeep.
 */
class KayaTheInexorableScenarioTest : ScenarioTestBase() {

    private val kaya = "Kaya the Inexorable"
    private val plusOne = KayaTheInexorable.activatedAbilities[0].id
    private val minusSeven = KayaTheInexorable.activatedAbilities[2].id

    private fun ghostformBears(removal: String, removalLand: String): TestGame {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, kaya)
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardInHand(1, removal)
            .withLandsOnBattlefield(1, removalLand, 1)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        val kayaId = game.findPermanent(kaya)!!
        val bears = game.findPermanent("Grizzly Bears")!!

        game.execute(
            ActivateAbility(game.player1Id, kayaId, plusOne, targets = listOf(ChosenTarget.Permanent(bears)))
        ).error shouldBe null
        game.resolveStack()
        game.state.getEntity(bears)?.get<CountersComponent>()?.getCount(CounterType.GHOSTFORM) shouldBe 1

        game.castSpell(1, removal, bears).error shouldBe null
        game.resolveStack()
        return game
    }

    init {
        test("+1: a ghostformed creature that dies returns to hand and makes a Spirit") {
            val game = ghostformBears("Lightning Bolt", "Mountain")

            withClue("Grizzly Bears is back in its owner's hand") {
                game.isInGraveyard(1, "Grizzly Bears") shouldBe false
                game.isInHand(1, "Grizzly Bears") shouldBe true
            }
            game.findPermanents("Spirit Token").size shouldBe 1
        }

        test("+1: a ghostformed creature that is exiled returns to hand and makes a Spirit") {
            val game = ghostformBears("Swords to Plowshares", "Plains")

            withClue("Grizzly Bears is back in its owner's hand, not in exile") {
                game.isInExile(1, "Grizzly Bears") shouldBe false
                game.isInHand(1, "Grizzly Bears") shouldBe true
            }
            game.findPermanents("Spirit Token").size shouldBe 1
        }

        test("−7 emblem: at your upkeep, cast a legendary card you own from hand, graveyard or exile for free") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, kaya)
                .withCardInHand(1, "Grizzly Bears")
                .withCardInGraveyard(1, "Squee, Goblin Nabob")
                .withCardInExile(1, "Isamaru, Hound of Konda")
                .withCardInExile(2, "Squee, Goblin Nabob")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .apply { repeat(6) { withCardInLibrary(1, "Island"); withCardInLibrary(2, "Island") } }
                .build()
            val kayaId = game.findPermanent(kaya)!!
            game.state = game.state.updateEntity(kayaId) { c ->
                c.with(CountersComponent().withAdded(CounterType.LOYALTY, 7))
            }

            game.execute(ActivateAbility(game.player1Id, kayaId, minusSeven)).error shouldBe null
            game.resolveStack()
            withClue("Kaya used all her loyalty and is gone; the emblem stays") {
                game.isOnBattlefield(kaya) shouldBe false
            }

            // The opponent's upkeep doesn't trigger "your upkeep".
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.activePlayerId shouldBe game.player2Id
            game.state.stack.isEmpty() shouldBe true
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.activePlayerId shouldBe game.player1Id

            game.resolveStack()
            val decision = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            val isamaru = game.state.getExile(game.player1Id).single()
            val squee = game.findCardsInGraveyard(1, "Squee, Goblin Nabob").single()
            withClue("your own legendary cards are offered (Kaya, now in your graveyard, too) — not the nonlegendary Bears, not the opponent's exile") {
                decision.options shouldContainExactlyInAnyOrder listOf(isamaru, squee, kayaId)
            }

            game.selectCards(listOf(isamaru)).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Isamaru, Hound of Konda") shouldBe true
            game.isInExile(1, "Isamaru, Hound of Konda") shouldBe false
        }

        test("+1 twice: the card returns once, but each granted trigger makes a Spirit") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, kaya)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .apply { repeat(6) { withCardInLibrary(1, "Island"); withCardInLibrary(2, "Island") } }
                .build()
            val kayaId = game.findPermanent(kaya)!!
            val bears = game.findPermanent("Grizzly Bears")!!
            val plusOneOnBears = ActivateAbility(
                game.player1Id, kayaId, plusOne, targets = listOf(ChosenTarget.Permanent(bears))
            )

            game.execute(plusOneOnBears).error shouldBe null
            game.resolveStack()
            // One loyalty ability per turn: come back on your next turn for the second.
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.state.activePlayerId shouldBe game.player1Id
            game.execute(plusOneOnBears).error shouldBe null
            game.resolveStack()
            game.state.getEntity(bears)?.get<CountersComponent>()?.getCount(CounterType.GHOSTFORM) shouldBe 2

            game.castSpell(1, "Lightning Bolt", bears).error shouldBe null
            game.resolveStack()

            game.isInHand(1, "Grizzly Bears") shouldBe true
            game.findPermanents("Spirit Token").size shouldBe 2
        }

        test("−7 emblem: a face-down legendary you own in exile is offered only if you may look at it") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, kaya)
                .withCardInExile(1, "Isamaru, Hound of Konda")
                .withCardInExile(1, "Squee, Goblin Nabob")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .apply { repeat(6) { withCardInLibrary(1, "Island"); withCardInLibrary(2, "Island") } }
                .build()
            val kayaId = game.findPermanent(kaya)!!
            game.state = game.state.updateEntity(kayaId) { c ->
                c.with(CountersComponent().withAdded(CounterType.LOYALTY, 7))
            }
            val (hidden, foretold) = game.state.getExile(game.player1Id)
            // Isamaru: exiled face down by a plain rider — nobody may look. Squee: foretold by you.
            game.state = game.state
                .updateEntity(hidden) { it.with(FaceDownComponent) }
                .updateEntity(foretold) { it.with(FaceDownComponent).with(ForetoldComponent(game.player1Id, 1)) }

            game.execute(ActivateAbility(game.player1Id, kayaId, minusSeven)).error shouldBe null
            game.resolveStack()
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.resolveStack()

            val decision = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            withClue("the foretold card is offered; the hidden face-down card is not") {
                decision.options shouldContainExactlyInAnyOrder listOf(foretold, kayaId)
            }
        }
    }
}
