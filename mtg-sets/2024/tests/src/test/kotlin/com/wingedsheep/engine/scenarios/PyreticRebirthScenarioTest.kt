package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh3.cards.PyreticRebirth
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Pyretic Rebirth returns an artifact or creature card from your graveyard to hand and deals
 * damage equal to its mana value to up to one creature or planeswalker. The mana value is
 * frozen before the card moves; an illegal graveyard target means no damage (ruling).
 */
class PyreticRebirthScenarioTest : FunSpec({
    val relic = card("Pyretic Test Relic") {
        manaCost = "{3}"
        typeLine = "Artifact"
        oracleText = ""
    }
    val ogre = card("Pyretic Test Ogre") {
        manaCost = "{2}{R}"
        typeLine = "Creature — Ogre"
        power = 3
        toughness = 3
        oracleText = ""
    }

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(PyreticRebirth, relic, ogre))
        initMirrorMatch(Deck.of("Swamp" to 40), startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.cast(graveCard: com.wingedsheep.sdk.model.EntityId, victim: com.wingedsheep.sdk.model.EntityId?) {
        val spell = putCardInHand(player1, "Pyretic Rebirth")
        giveMana(player1, Color.BLACK, 2)
        giveMana(player1, Color.RED, 2)
        val targets = listOfNotNull(
            ChosenTarget.Card(graveCard, player1, Zone.GRAVEYARD),
            victim?.let { ChosenTarget.Permanent(it) }
        )
        castSpellWithTargets(player1, spell, targets).error shouldBe null
    }

    test("returns the card and deals damage equal to its mana value") {
        val d = driver()
        val grave = d.putCardInGraveyard(d.player1, "Pyretic Test Relic")
        val victim = d.putCreatureOnBattlefield(d.player2, "Pyretic Test Ogre")
        d.cast(grave, victim)
        d.bothPass().error shouldBe null

        d.state.getZone(ZoneKey(d.player1, Zone.HAND)).contains(grave) shouldBe true
        d.state.getZone(ZoneKey(d.player2, Zone.GRAVEYARD)).contains(victim) shouldBe true
    }

    test("with no creature target it only returns the card") {
        val d = driver()
        val grave = d.putCardInGraveyard(d.player1, "Pyretic Test Ogre")
        d.cast(grave, null)
        d.bothPass().error shouldBe null

        d.state.getZone(ZoneKey(d.player1, Zone.HAND)).contains(grave) shouldBe true
    }

    test("an illegal graveyard target means no damage is dealt") {
        val d = driver()
        val grave = d.putCardInGraveyard(d.player1, "Pyretic Test Relic")
        val victim = d.putCreatureOnBattlefield(d.player2, "Pyretic Test Ogre")
        d.cast(grave, victim)
        d.replaceState(
            d.state.removeFromZone(ZoneKey(d.player1, Zone.GRAVEYARD), grave)
                .addToZone(ZoneKey(d.player1, Zone.EXILE), grave)
        )
        d.bothPass().error shouldBe null

        d.state.getZone(ZoneKey(d.player1, Zone.EXILE)).contains(grave) shouldBe true
        d.state.getZone(ZoneKey(d.player2, Zone.BATTLEFIELD)).contains(victim) shouldBe true
    }
})
