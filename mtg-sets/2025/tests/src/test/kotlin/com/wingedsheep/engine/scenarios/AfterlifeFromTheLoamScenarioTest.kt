package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.tdc.cards.AfterlifeFromTheLoam
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Afterlife from the Loam (TDC #25) — Delve. For each player, choose up to one target creature card
 * in that player's graveyard. Put those cards onto the battlefield under your control. They're
 * Zombies in addition to their other types.
 *
 * Pins the per-owner "up to one" target distribution (one card per graveyard at most, none at all
 * is legal), the control change for an opponent's card, and the Zombie type on what returned.
 */
class AfterlifeFromTheLoamScenarioTest : FunSpec({

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + AfterlifeFromTheLoam)
        initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun cast(d: GameTestDriver, targets: List<Pair<EntityId, EntityId>>) =
        d.putCardInHand(d.player1, AfterlifeFromTheLoam.name).let { spell ->
            d.giveMana(d.player1, Color.BLACK, 8)
            d.castSpellWithTargets(
                d.player1,
                spell,
                targets.map { (card, owner) -> ChosenTarget.Card(card, owner, Zone.GRAVEYARD) },
            )
        }

    fun resolve(d: GameTestDriver) {
        var guard = 0
        while (d.state.stack.isNotEmpty() && d.pendingDecision == null && guard++ < 16) d.bothPass()
    }

    test("one creature card from each graveyard returns under your control as a Zombie") {
        val d = driver()
        val mine = d.putCardInGraveyard(d.player1, "Grizzly Bears")
        val theirs = d.putCardInGraveyard(d.player2, "Centaur Courser")

        cast(d, listOf(mine to d.player1, theirs to d.player2)).error.shouldBeNull()
        resolve(d)

        val battlefield = d.state.getZone(ZoneKey(d.player1, Zone.BATTLEFIELD))
        withClue("both cards entered on your side, including the opponent's") {
            (mine in battlefield) shouldBe true
            (theirs in battlefield) shouldBe true
            d.state.projectedState.getController(theirs) shouldBe d.player1
        }
        withClue("each is a Zombie in addition to its other types") {
            d.state.projectedState.hasSubtype(mine, "Zombie") shouldBe true
            d.state.projectedState.hasSubtype(mine, "Bear") shouldBe true
            d.state.projectedState.hasSubtype(theirs, "Zombie") shouldBe true
            d.state.projectedState.hasSubtype(theirs, "Centaur") shouldBe true
        }
    }

    test("two targets from the same graveyard are rejected") {
        val d = driver()
        val a = d.putCardInGraveyard(d.player2, "Grizzly Bears")
        val b = d.putCardInGraveyard(d.player2, "Centaur Courser")

        cast(d, listOf(a to d.player2, b to d.player2)).error.shouldNotBeNull()
    }

    test("choosing no targets is legal and returns nothing") {
        val d = driver()
        val bear = d.putCardInGraveyard(d.player2, "Grizzly Bears")

        cast(d, emptyList()).error.shouldBeNull()
        resolve(d)

        (bear in d.state.getZone(ZoneKey(d.player2, Zone.GRAVEYARD))) shouldBe true
        d.findPermanent(d.player1, "Grizzly Bears").shouldBeNull()
    }
})
