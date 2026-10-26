package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.CommanderComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Format
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Hellkite Courser (CMR #183) — "When this creature enters, you may put a commander you own from
 * the command zone onto the battlefield. It gains haste. Return it to the command zone at the
 * beginning of the next end step."
 *
 * Commanders are set up the way the game initializer leaves them: the card in its owner's command
 * zone carrying [CommanderComponent], in a [Format.Commander] game (so the CR 903.9a zone choice
 * applies when the commander dies).
 */
class HellkiteCourserScenarioTest : ScenarioTestBase() {

    private fun TestGame.commandZone(player: Int): List<EntityId> =
        state.getZone(ZoneKey(if (player == 1) player1Id else player2Id, Zone.COMMAND))

    private fun TestGame.commandCard(player: Int, name: String): EntityId =
        commandZone(player).single { state.getEntity(it)?.get<CardComponent>()?.name == name }

    private fun TestGame.makeCommander(player: Int, name: String) {
        val id = commandCard(player, name)
        state = state.updateEntity(id) {
            it.with(CommanderComponent(ownerId = if (player == 1) player1Id else player2Id))
        }
    }

    private fun TestGame.castCourser() {
        castSpell(1, "Hellkite Courser").error shouldBe null
        resolveStack()
    }

    private fun base(): ScenarioBuilder = scenario()
        .withPlayers("Alice", "Bob")
        .withCardInHand(1, "Hellkite Courser")
        .withLandsOnBattlefield(1, "Mountain", 6)
        .withCardInLibrary(1, "Mountain")
        .withCardInLibrary(1, "Mountain")
        .withCardInLibrary(2, "Mountain")
        .withCardInLibrary(2, "Mountain")
        .withFormat(Format.Commander())
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    init {
        test("accept: the commander enters with haste and goes back to the command zone at the next end step") {
            val game = base()
                .withCardInCommandZone(1, "Hill Giant")
                // Not commanders: an opponent's commander and a Vanguard avatar share command zones.
                .withCardInCommandZone(2, "Grizzly Bears")
                .withCardInCommandZone(1, "Momir Vig, Simic Visionary")
                .build()
            game.makeCommander(1, "Hill Giant")
            game.makeCommander(2, "Grizzly Bears")
            val giant = game.commandCard(1, "Hill Giant")

            game.castCourser()

            val decision = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            withClue("only a commander Alice owns is offered — not the avatar, not Bob's commander") {
                decision.options shouldContainExactlyInAnyOrder listOf(giant)
            }
            decision.minSelections shouldBe 0
            game.selectCards(listOf(giant)).error shouldBe null
            game.resolveStack()

            withClue("the commander is on the battlefield with haste") {
                game.findPermanent("Hill Giant") shouldBe giant
                game.state.projectedState.hasKeyword(giant, Keyword.HASTE) shouldBe true
                (giant in game.commandZone(1)) shouldBe false
            }
            withClue("putting it onto the battlefield isn't casting it — no commander tax") {
                game.state.getEntity(giant)?.get<CommanderComponent>()?.castsFromCommandZone shouldBe 0
            }

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()

            withClue("returned to the command zone at the beginning of the end step") {
                game.isOnBattlefield("Hill Giant") shouldBe false
                (giant in game.commandZone(1)) shouldBe true
            }
            game.isOnBattlefield("Hellkite Courser") shouldBe true
        }

        test("decline: the commander stays in the command zone") {
            val game = base().withCardInCommandZone(1, "Hill Giant").build()
            game.makeCommander(1, "Hill Giant")
            val giant = game.commandCard(1, "Hill Giant")

            game.castCourser()
            game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            game.skipSelection().error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Hill Giant") shouldBe false
            (giant in game.commandZone(1)) shouldBe true
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()
            (giant in game.commandZone(1)) shouldBe true
        }

        test("partners: the player chooses one of their two commanders") {
            val game = base()
                .withCardInCommandZone(1, "Hill Giant")
                .withCardInCommandZone(1, "Grizzly Bears")
                .build()
            game.makeCommander(1, "Hill Giant")
            game.makeCommander(1, "Grizzly Bears")
            val giant = game.commandCard(1, "Hill Giant")
            val bears = game.commandCard(1, "Grizzly Bears")

            game.castCourser()
            val decision = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            decision.options shouldContainExactlyInAnyOrder listOf(giant, bears)
            decision.maxSelections shouldBe 1
            game.selectCards(listOf(bears)).error shouldBe null
            game.resolveStack()

            game.findPermanent("Grizzly Bears") shouldBe bears
            (giant in game.commandZone(1)) shouldBe true
        }

        test("ruling: a commander that left the battlefield before the end step stays where it is") {
            val game = base()
                .withCardInCommandZone(1, "Hill Giant")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .build()
            game.makeCommander(1, "Hill Giant")
            val giant = game.commandCard(1, "Hill Giant")

            game.castCourser()
            game.selectCards(listOf(giant)).error shouldBe null
            game.resolveStack()
            game.findPermanent("Hill Giant") shouldBe giant

            game.castSpell(1, "Lightning Bolt", giant).error shouldBe null
            game.resolveStack()
            withClue("the CR 903.9a choice: Alice leaves her commander in the graveyard") {
                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(false).error shouldBe null
            }
            game.isInGraveyard(1, "Hill Giant") shouldBe true

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()

            withClue("the delayed trigger doesn't fetch it out of the graveyard") {
                game.isInGraveyard(1, "Hill Giant") shouldBe true
                (giant in game.commandZone(1)) shouldBe false
            }
        }

        test("CR 603.7c: a commander that went home and was recast is a new object and stays put") {
            val game = base()
                .withCardInCommandZone(1, "Hill Giant")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 5)
                .build()
            game.makeCommander(1, "Hill Giant")
            val giant = game.commandCard(1, "Hill Giant")

            game.castCourser()
            game.selectCards(listOf(giant)).error shouldBe null
            game.resolveStack()

            game.castSpell(1, "Lightning Bolt", giant).error shouldBe null
            game.resolveStack()
            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(true).error shouldBe null
            (giant in game.commandZone(1)) shouldBe true

            // Recast from the command zone: {3}{R}, no tax yet — Courser's put wasn't a cast.
            game.execute(CastSpell(game.player1Id, giant, emptyList())).error shouldBe null
            game.resolveStack()
            game.findPermanent("Hill Giant") shouldBe giant

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()

            withClue("the recast Hill Giant is a new object the delayed trigger doesn't know") {
                game.isOnBattlefield("Hill Giant") shouldBe true
            }
        }

        test("no commander in the command zone: the trigger does nothing") {
            val game = base()
                // A non-commander card in the command zone (a Vanguard avatar) is never offered.
                .withCardInCommandZone(1, "Momir Vig, Simic Visionary")
                .withFormat(Format.Standard)
                .build()

            game.castCourser()

            game.hasPendingDecision() shouldBe false
            game.isOnBattlefield("Hellkite Courser") shouldBe true
            game.commandZone(1).size shouldBe 1
        }
    }
}
