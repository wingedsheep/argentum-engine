package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

/**
 * Mosswood Dreadknight (WOE #231) — {1}{G} 3/2 Creature — Human Knight with trample and
 * "When this creature dies, you may cast it from your graveyard as an Adventure until the end of
 * your next turn." Adventure half: Dread Whispers, {1}{B} Sorcery — "You draw a card and you
 * lose 1 life."
 *
 * Exercises the new `GrantMayPlayFromExileEffect.castFaceIndex` / `MayPlayPermission.castFaceIndex`
 * face restriction:
 *  - the dies trigger grants a permission over the card *in the graveyard*, tagged to face 0;
 *  - the cast enumerator offers only Dread Whispers from the graveyard, never the creature half,
 *    and charges the Adventure's {1}{B} rather than the creature's {1}{G};
 *  - a hand-constructed `CastSpell` for the unauthorized creature face is rejected by the handler;
 *  - resolving the Adventure from the graveyard exiles the card and hands back the ordinary
 *    cast-the-creature-from-exile permission (CR 715.3d), so the chain completes.
 */
class MosswoodDreadknightScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    /** Bolt the 3/2 (3 damage is lethal), then let the dies trigger resolve. */
    fun GameTestDriver.killWithBolt(player: EntityId, creature: EntityId) {
        val bolt = putCardInHand(player, "Lightning Bolt")
        giveMana(player, Color.RED, 1)
        castSpell(player, bolt, listOf(creature)).isSuccess shouldBe true
        bothPass() // resolve the bolt; SBA kills the creature and queues the dies trigger
        bothPass() // resolve the dies trigger
        findPermanent(player, "Mosswood Dreadknight") shouldBe null
    }

    fun GameTestDriver.graveyardCastsOf(player: EntityId, cardId: EntityId) =
        legalActions(player).filter {
            it.sourceZone == "GRAVEYARD" && (it.action as? CastSpell)?.cardId == cardId
        }

    test("dying grants a graveyard permission for the Adventure face only") {
        val driver = newDriver()
        val you = driver.player1
        val knight = driver.putCreatureOnBattlefield(you, "Mosswood Dreadknight")
        driver.killWithBolt(you, knight)

        withClue("the card is in the graveyard, not exile") {
            driver.getGraveyardCardNames(you).contains("Mosswood Dreadknight") shouldBe true
        }
        val permission = driver.state.mayPlayPermissions.single { knight in it.cardIds }
        permission.castFaceIndex shouldBe 0
        permission.controllerId shouldBe you

        // Enough mana for either half, so the offer set is decided by the permission, not affordability.
        driver.giveMana(you, Color.BLACK, 1)
        driver.giveMana(you, Color.GREEN, 1)
        driver.giveColorlessMana(you, 2)

        val casts = driver.graveyardCastsOf(you, knight)
        withClue("only the Adventure half is offered from the graveyard: $casts") {
            casts.size shouldBe 1
            casts.single().description shouldContain "Dread Whispers"
            (casts.single().action as CastSpell).faceIndex shouldBe 0
            casts.single().manaCostString shouldBe "{1}{B}"
            casts.single().affordable shouldBe true
        }
    }

    test("the creature face stays uncastable from the graveyard even for a hand-constructed action") {
        val driver = newDriver()
        val you = driver.player1
        val knight = driver.putCreatureOnBattlefield(you, "Mosswood Dreadknight")
        driver.killWithBolt(you, knight)
        driver.giveMana(you, Color.GREEN, 1)
        driver.giveColorlessMana(you, 1)

        val result = driver.submit(CastSpell(playerId = you, cardId = knight, faceIndex = null))
        withClue("permission covers face 0 only — the creature half must be refused") {
            result.isSuccess shouldBe false
        }
        driver.findPermanent(you, "Mosswood Dreadknight") shouldBe null
    }

    test("casting Dread Whispers from the graveyard draws, loses 1 life, exiles it, and unlocks the creature") {
        val driver = newDriver()
        val you = driver.player1
        val knight = driver.putCreatureOnBattlefield(you, "Mosswood Dreadknight")
        driver.killWithBolt(you, knight)

        val handBefore = driver.getHandSize(you)
        val lifeBefore = driver.getLifeTotal(you)
        driver.giveMana(you, Color.BLACK, 1)
        driver.giveColorlessMana(you, 1)

        driver.submit(CastSpell(playerId = you, cardId = knight, faceIndex = 0)).isSuccess shouldBe true
        driver.bothPass()

        driver.getHandSize(you) shouldBe handBefore + 1
        driver.getLifeTotal(you) shouldBe lifeBefore - 1
        withClue("CR 715.3d — an Adventure exiles itself on resolution instead of going to the graveyard") {
            driver.getExileCardNames(you).contains("Mosswood Dreadknight") shouldBe true
            driver.getGraveyardCardNames(you).contains("Mosswood Dreadknight") shouldBe false
        }

        // The exile permission is the ordinary creature-from-exile one: no face restriction.
        val exilePermission = driver.state.mayPlayPermissions.single { knight in it.cardIds }
        exilePermission.castFaceIndex shouldBe null

        driver.giveMana(you, Color.GREEN, 1)
        driver.giveColorlessMana(you, 1)
        driver.submit(CastSpell(playerId = you, cardId = knight)).isSuccess shouldBe true
        driver.bothPass()
        driver.assertPermanentExists(you, "Mosswood Dreadknight")
    }

    test("the permission survives this turn's cleanup and expires after your next turn") {
        val driver = newDriver()
        val you = driver.player1
        val knight = driver.putCreatureOnBattlefield(you, "Mosswood Dreadknight")
        driver.killWithBolt(you, knight)

        fun granted() = driver.state.mayPlayPermissions.any { knight in it.cardIds }
        fun nextMainPhase() {
            driver.passPriorityUntil(Step.END)
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        }

        granted() shouldBe true

        nextMainPhase() // the opponent's turn — "your next turn" hasn't happened yet
        withClue("the grant must outlive the cleanup of the turn the knight died on") {
            driver.activePlayer shouldBe driver.player2
            granted() shouldBe true
        }

        nextMainPhase() // your next turn — still live for its whole duration
        withClue("still castable throughout your next turn") {
            driver.activePlayer shouldBe you
            granted() shouldBe true
        }

        nextMainPhase() // your next turn's cleanup has now run
        granted() shouldBe false
    }
})
