package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dgm.cards.ProgenitorMimic
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Progenitor Mimic {4}{G}{U} — Creature — Shapeshifter 0/0 (DGM #92).
 * "You may have this creature enter as a copy of any creature on the battlefield, except it has
 *  'At the beginning of your upkeep, if this creature isn't a token, create a token that's a copy
 *  of this creature.'"
 *
 * Pins the copiable-value shape: the added trigger rides the copy, so each token copy carries it
 * as well but never fires (intervening "isn't a token"), and a second Mimic copying the first
 * ends up with two instances that both trigger.
 */
class ProgenitorMimicScenarioTest : FunSpec({

    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(ProgenitorMimic))
        it.initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun card(d: GameTestDriver, id: EntityId) = d.state.getEntity(id)!!.get<CardComponent>()!!

    fun castMimic(d: GameTestDriver, copyOf: EntityId?): EntityId {
        val id = d.putCardInHand(d.player1, "Progenitor Mimic")
        d.giveMana(d.player1, Color.GREEN, 1)
        d.giveMana(d.player1, Color.BLUE, 5)
        d.castSpell(d.player1, id).error shouldBe null
        d.bothPass().error shouldBe null
        d.submitCardSelection(d.player1, listOfNotNull(copyOf)).error shouldBe null
        return id
    }

    /** Advance to player 1's next upkeep, stopping with its upkeep triggers on the stack. */
    fun nextOwnUpkeep(d: GameTestDriver) {
        d.passPriorityUntil(Step.END)
        d.passPriorityUntil(Step.UPKEEP, d.player1)
    }

    fun resolveStack(d: GameTestDriver) {
        var guard = 0
        while (d.state.stack.isNotEmpty() && guard++ < 10) d.bothPass().error shouldBe null
    }

    fun tokens(d: GameTestDriver) = d.getCreatures(d.player1).filter {
        d.state.getEntity(it)!!.has<TokenComponent>()
    }

    test("enters as a copy of the chosen creature with the upkeep ability added") {
        val d = driver()
        val bears = d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        val mimic = castMimic(d, bears)

        val c = card(d, mimic)
        c.name shouldBe "Grizzly Bears"
        c.typeLine shouldBe card(d, bears).typeLine
        c.baseStats shouldBe card(d, bears).baseStats
        c.copyTriggeredAbilities.size shouldBe 1
        d.state.getEntity(mimic)!!.has<TokenComponent>() shouldBe false
    }

    test("each upkeep the nontoken Mimic makes one token copy; the tokens carry the ability but never trigger") {
        val d = driver()
        val bears = d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        castMimic(d, bears)

        nextOwnUpkeep(d)
        d.state.stack.size shouldBe 1
        resolveStack(d)
        val first = tokens(d)
        first.size shouldBe 1
        card(d, first.single()).name shouldBe "Grizzly Bears"
        // The added trigger is a copiable value, so the token copy has it too.
        card(d, first.single()).copyTriggeredAbilities.size shouldBe 1

        nextOwnUpkeep(d)
        // Only the original Mimic's instance triggers — the token's fails its intervening-if.
        d.state.stack.size shouldBe 1
        resolveStack(d)
        tokens(d).size shouldBe 2
    }

    test("a second Mimic copying the first gets two instances of the ability") {
        val d = driver()
        val bears = d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        val firstMimic = castMimic(d, bears)
        val secondMimic = castMimic(d, firstMimic)

        card(d, secondMimic).name shouldBe "Grizzly Bears"
        card(d, secondMimic).copyTriggeredAbilities.size shouldBe 2

        nextOwnUpkeep(d)
        // One trigger from the first Mimic, two from the second.
        d.state.stack.size shouldBe 3
        resolveStack(d)
        tokens(d).size shouldBe 3
    }

    test("declining the copy leaves a 0/0 Shapeshifter that dies") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        val mimic = castMimic(d, null)
        (mimic in d.state.getZone(d.player1, Zone.GRAVEYARD)) shouldBe true
    }
})
