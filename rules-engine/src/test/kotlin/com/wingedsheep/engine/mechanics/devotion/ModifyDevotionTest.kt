package com.wingedsheep.engine.mechanics.devotion

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ths.cards.NyleaGodOfTheHunt
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ModifyDevotion
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * [ModifyDevotion] — "your devotion to each color and each combination of colors is increased by
 * one" (Altar of the Pantheon, CR 700.5). Every devotion count of the holder's controller rises by
 * the amount, a combination by one rather than once per color (Altar ruling), and the raise reaches
 * a Theros god's devotion threshold, which is read mid-projection.
 */
class ModifyDevotionTest : FunSpec({

    val altar = card("Test Devotion Altar") {
        manaCost = "{3}"
        typeLine = "Artifact"
        staticAbility { ability = ModifyDevotion(1) }
    }

    /** Three green pips — with Nylea's own {G}, devotion to green is four. */
    val devotee = card("Test Green Devotee") {
        manaCost = "{G}{G}{G}"
        typeLine = "Creature — Elf"
        power = 1
        toughness = 1
    }

    fun gauge(name: String, vararg colors: Color) = card(name) {
        manaCost = "{0}"
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Tap
            effect = Effects.GainLife(DynamicAmounts.devotionTo(*colors))
        }
    }

    val greenGauge = gauge("Green Gauge", Color.GREEN)
    val pairGauge = gauge("Green-White Gauge", Color.GREEN, Color.WHITE)
    val blackGauge = gauge("Black Gauge", Color.BLACK)

    fun setup(): Pair<GameTestDriver, EntityId> {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(altar, devotee, greenGauge, pairGauge, blackGauge, NyleaGodOfTheHunt))
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val you = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d to you
    }

    fun GameTestDriver.readGauge(you: EntityId, name: String): Int {
        val before = getLifeTotal(you)
        val gauge = putPermanentOnBattlefield(you, name)
        val def = cardRegistry.getCard(name)!!
        submit(ActivateAbility(you, gauge, def.activatedAbilities.first().id)).error shouldBe null
        bothPass()
        return getLifeTotal(you) - before
    }

    test("devotion to a color rises by one") {
        val (d, you) = setup()
        d.putCreatureOnBattlefield(you, "Test Green Devotee")
        d.putPermanentOnBattlefield(you, "Test Devotion Altar")
        d.readGauge(you, "Green Gauge") shouldBe 4
    }

    test("devotion to a combination of colors rises by one, not once per color") {
        val (d, you) = setup()
        d.putCreatureOnBattlefield(you, "Test Green Devotee")
        d.putCreatureOnBattlefield(you, "Savannah Lions")
        d.putPermanentOnBattlefield(you, "Test Devotion Altar")
        d.readGauge(you, "Green-White Gauge") shouldBe 5
    }

    test("a color with no pips still reads the bonus") {
        val (d, you) = setup()
        d.putPermanentOnBattlefield(you, "Test Devotion Altar")
        d.readGauge(you, "Black Gauge") shouldBe 1
    }

    test("two altars stack") {
        val (d, you) = setup()
        d.putPermanentOnBattlefield(you, "Test Devotion Altar")
        d.putPermanentOnBattlefield(you, "Test Devotion Altar")
        d.readGauge(you, "Black Gauge") shouldBe 2
    }

    test("an opponent's altar doesn't raise your devotion") {
        val (d, you) = setup()
        d.putCreatureOnBattlefield(you, "Test Green Devotee")
        d.putPermanentOnBattlefield(d.getOpponent(you), "Test Devotion Altar")
        d.readGauge(you, "Green Gauge") shouldBe 3
    }

    test("the bonus lifts a god over its devotion threshold, and leaves with the altar") {
        val (d, you) = setup()
        val nylea = d.putPermanentOnBattlefield(you, "Nylea, God of the Hunt")
        d.putCreatureOnBattlefield(you, "Test Green Devotee")
        d.state.projectedState.isCreature(nylea) shouldBe false // devotion to green is four

        val altarId = d.putPermanentOnBattlefield(you, "Test Devotion Altar")
        d.state.projectedState.isCreature(nylea) shouldBe true // five with the altar

        d.moveToGraveyard(altarId)
        d.state.projectedState.isCreature(nylea) shouldBe false
    }
})
