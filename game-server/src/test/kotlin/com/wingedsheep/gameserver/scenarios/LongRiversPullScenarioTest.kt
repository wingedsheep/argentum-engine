package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import io.kotest.matchers.shouldBe

/**
 * Tests for Long River's Pull — a Gift counterspell.
 *
 * Mode 0: Counter target creature spell (no gift)
 * Mode 1: Opponent draws a card, then counter target spell (gift promised)
 */
class LongRiversPullScenarioTest : ScenarioTestBase() {

    init {
        test("Mode 0 - counter target creature spell without gift") {
            val game = scenario()
                .withPlayers()
                .withActivePlayer(2) // P2's turn so they can cast creatures at sorcery speed
                .withCardInHand(1, "Long River's Pull")
                .withLandsOnBattlefield(1, "Island", 2)
                .withCardInHand(2, "Grizzly Bears")
                .withLandsOnBattlefield(2, "Forest", 2)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Forest")
                .build()

            // P2 has priority (active player), casts Grizzly Bears
            game.castSpell(2, "Grizzly Bears")

            // Grizzly Bears is on the stack. P2 passes, P1 gets priority.
            game.passPriority()

            // P1 casts Long River's Pull (mode 0 = counter creature spell)
            val spellOnStack = game.state.stack.find { entityId ->
                game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Grizzly Bears"
            }!!

            val counterSpellId = game.state.getHand(game.player1Id).find { entityId ->
                game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Long River's Pull"
            }!!

            val result = game.execute(CastSpell(
                playerId = game.player1Id,
                cardId = counterSpellId,
                targets = listOf(ChosenTarget.Spell(spellOnStack)),
                chosenModes = listOf(0),
                modeTargetsOrdered = listOf(listOf(ChosenTarget.Spell(spellOnStack)))
            ))
            result.isSuccess shouldBe true

            // Resolve stack
            game.resolveStack()

            // Grizzly Bears should be countered (in graveyard, not on battlefield)
            game.isOnBattlefield("Grizzly Bears") shouldBe false
            game.isInGraveyard(2, "Grizzly Bears") shouldBe true

            // Opponent should NOT have drawn a card (no gift)
            game.handSize(2) shouldBe 0
        }

        test("Mode 1 - gift a card and counter any spell") {
            val game = scenario()
                .withPlayers()
                .withActivePlayer(2)
                .withCardInHand(1, "Long River's Pull")
                .withLandsOnBattlefield(1, "Island", 2)
                .withCardInHand(2, "Blooming Blast")
                .withLandsOnBattlefield(2, "Mountain", 2)
                .withCardOnBattlefield(1, "Grizzly Bears") // target for Blooming Blast
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Mountain")
                .build()

            val p2HandSizeBefore = game.handSize(2) // 1 (Blooming Blast)

            // P2 casts Blooming Blast (mode 0 = no gift, 2 damage to creature) targeting P1's Grizzly Bears
            val bearsId = game.findPermanent("Grizzly Bears")!!
            game.castSpellWithMode(2, "Blooming Blast", 0, bearsId)

            // P2 passes, P1 gets priority
            game.passPriority()

            // P1 casts Long River's Pull (mode 1 = gift + counter any spell)
            val spellOnStack = game.state.stack.find { entityId ->
                game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Blooming Blast"
            }!!

            val counterSpellId = game.state.getHand(game.player1Id).find { entityId ->
                game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Long River's Pull"
            }!!

            val result = game.execute(CastSpell(
                playerId = game.player1Id,
                cardId = counterSpellId,
                targets = listOf(ChosenTarget.Spell(spellOnStack)),
                chosenModes = listOf(1),
                modeTargetsOrdered = listOf(listOf(ChosenTarget.Spell(spellOnStack)))
            ))
            result.isSuccess shouldBe true

            // Resolve stack
            game.resolveStack()

            // Blooming Blast should be countered (in graveyard)
            game.isInGraveyard(2, "Blooming Blast") shouldBe true

            // Opponent should have drawn a card (gift was promised)
            // P2 started with 1 card in hand, cast it (0), then drew 1 from gift = 1
            game.handSize(2) shouldBe 1

            // Grizzly Bears should still be on battlefield (Blooming Blast was countered)
            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }

        test("Mode 0 - cannot target non-creature spell") {
            val game = scenario()
                .withPlayers()
                .withActivePlayer(2)
                .withCardInHand(1, "Long River's Pull")
                .withLandsOnBattlefield(1, "Island", 2)
                .withCardInHand(2, "Blooming Blast")
                .withLandsOnBattlefield(2, "Mountain", 2)
                .withCardOnBattlefield(1, "Grizzly Bears") // target for Blooming Blast
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Mountain")
                .build()

            // P2 casts Blooming Blast targeting P1's Grizzly Bears
            val bearsId = game.findPermanent("Grizzly Bears")!!
            game.castSpellWithMode(2, "Blooming Blast", 0, bearsId)

            // P2 passes, P1 gets priority
            game.passPriority()

            // P1 tries to cast Long River's Pull mode 0 (creature spell only) targeting Blooming Blast
            val spellOnStack = game.state.stack.find { entityId ->
                game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Blooming Blast"
            }!!

            val counterSpellId = game.state.getHand(game.player1Id).find { entityId ->
                game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Long River's Pull"
            }!!

            val result = game.execute(CastSpell(
                playerId = game.player1Id,
                cardId = counterSpellId,
                targets = listOf(ChosenTarget.Spell(spellOnStack)),
                chosenModes = listOf(0),
                modeTargetsOrdered = listOf(listOf(ChosenTarget.Spell(spellOnStack)))
            ))
            // Should fail — Blooming Blast is not a creature spell
            result.isSuccess shouldBe false
        }
    }
}
