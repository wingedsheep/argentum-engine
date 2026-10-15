package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Cass, Hand of Vengeance (PIP #97) — whenever Cass or another creature you control dies, if it was
 * enchanted or equipped, return any number of Aura cards that were attached to it from your graveyard
 * to the battlefield attached to target creature, then attach any number of Equipment that were
 * attached to it to that creature.
 *
 * The Auras reach the graveyard by state-based action after the creature dies, so they are found
 * through the dying creature's last-known attachments (CR 608.2h); the Equipment stays on the
 * battlefield unattached and is found the same way.
 */
class CassHandOfVengeanceScenarioTest : ScenarioTestBase() {

    private fun TestGame.attachedTo(name: String) =
        state.getEntity(findPermanent(name)!!)?.get<AttachedToComponent>()?.targetId

    private fun TestGame.graveyardCard(playerNumber: Int, name: String) =
        findCardsInGraveyard(playerNumber, name).single()

    /** Alice's Grizzly Bears carrying Holy Strength, Unholy Strength and Bonesplitter; Hill Giant waits. */
    private fun ScenarioBuilder.decoratedBears(): ScenarioBuilder = this
        .withPlayers("Alice", "Bob")
        .withCardOnBattlefield(1, "Cass, Hand of Vengeance")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardAttachedTo(1, "Holy Strength", "Grizzly Bears")
        .withCardAttachedTo(1, "Unholy Strength", "Grizzly Bears")
        .withCardAttachedTo(1, "Bonesplitter", "Grizzly Bears")
        .withCardOnBattlefield(1, "Hill Giant")
        .withCardInHand(1, "Murder")
        .withLandsOnBattlefield(1, "Swamp", 3)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    private fun TestGame.murder(name: String) {
        val cast = castSpell(1, "Murder", findPermanent(name)!!)
        withClue("Murder should cast: ${cast.error}") { cast.error shouldBe null }
        resolveStack()
    }

    init {
        context("Cass, Hand of Vengeance") {
            test("returns the Auras attached to the target and re-attaches the Equipment") {
                val game = scenario().decoratedBears().build()

                game.murder("Grizzly Bears")
                withClue("the Bears and both Auras are in the graveyard") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                    game.isInGraveyard(1, "Holy Strength") shouldBe true
                    game.isInGraveyard(1, "Unholy Strength") shouldBe true
                }
                withClue("Cass's trigger asks for its target") {
                    (game.getPendingDecision() is ChooseTargetsDecision) shouldBe true
                }
                val giant = game.findPermanent("Hill Giant")!!
                game.selectTargets(listOf(giant)).error shouldBe null
                game.resolveStack()

                game.selectCards(
                    listOf(game.graveyardCard(1, "Holy Strength"), game.graveyardCard(1, "Unholy Strength"))
                ).error shouldBe null
                game.selectCards(listOf(game.findPermanent("Bonesplitter")!!)).error shouldBe null
                game.resolveStack()

                withClue("both Auras and the Equipment are on the Hill Giant") {
                    game.attachedTo("Holy Strength") shouldBe giant
                    game.attachedTo("Unholy Strength") shouldBe giant
                    game.attachedTo("Bonesplitter") shouldBe giant
                }
                withClue("Hill Giant is 3/3 + 1/2 + 2/1 + 2/0 = 8/6") {
                    game.state.projectedState.getPower(giant) shouldBe 8
                    game.state.projectedState.getToughness(giant) shouldBe 6
                }
            }

            test("any number — a chosen subset returns, the rest stays put") {
                val game = scenario().decoratedBears().build()

                game.murder("Grizzly Bears")
                val giant = game.findPermanent("Hill Giant")!!
                game.selectTargets(listOf(giant)).error shouldBe null
                game.resolveStack()

                game.selectCards(listOf(game.graveyardCard(1, "Unholy Strength"))).error shouldBe null
                game.selectCards(emptyList()).error shouldBe null
                game.resolveStack()

                withClue("only Unholy Strength came back") {
                    game.attachedTo("Unholy Strength") shouldBe giant
                    game.isInGraveyard(1, "Holy Strength") shouldBe true
                }
                withClue("the unchosen Equipment stays unattached") {
                    game.attachedTo("Bonesplitter") shouldBe null
                }
            }

            test("Cass itself dying equipped triggers too") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Cass, Hand of Vengeance")
                    .withCardAttachedTo(1, "Bonesplitter", "Cass, Hand of Vengeance")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardInHand(1, "Murder")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.murder("Cass, Hand of Vengeance")
                withClue("Cass's own death triggers") {
                    (game.getPendingDecision() is ChooseTargetsDecision) shouldBe true
                }
                val giant = game.findPermanent("Hill Giant")!!
                game.selectTargets(listOf(giant)).error shouldBe null
                game.resolveStack()
                game.selectCards(listOf(game.findPermanent("Bonesplitter")!!)).error shouldBe null
                game.resolveStack()

                game.attachedTo("Bonesplitter") shouldBe giant
            }

            test("a creature that was neither enchanted nor equipped doesn't trigger") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Cass, Hand of Vengeance")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardInHand(1, "Murder")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.murder("Grizzly Bears")
                withClue("no trigger") {
                    game.hasPendingDecision() shouldBe false
                    game.state.stack.isEmpty() shouldBe true
                }
            }

            test("an opponent's enchanted creature dying doesn't trigger") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Cass, Hand of Vengeance")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardAttachedTo(2, "Holy Strength", "Grizzly Bears")
                    .withCardInHand(1, "Murder")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.murder("Grizzly Bears")
                withClue("the opponent's Bears and Aura are gone, but Cass doesn't trigger") {
                    game.isInGraveyard(2, "Holy Strength") shouldBe true
                    game.hasPendingDecision() shouldBe false
                    game.state.stack.isEmpty() shouldBe true
                }
            }
        }
    }
}
