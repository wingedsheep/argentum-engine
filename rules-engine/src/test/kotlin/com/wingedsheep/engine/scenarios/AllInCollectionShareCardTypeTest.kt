package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.effects.CardSource
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * `CollectionSharesCardType(all = true)` — "if all of those cards share a card type": true when one
 * card type is common to every card in the collection, true for a single card, false when empty.
 * A probe sorcery gathers its caster's graveyard and gains 10 life when the condition holds.
 */
class AllInCollectionShareCardTypeTest : FunSpec({

    val probe = card("All-Share Probe") {
        manaCost = "{G}"
        typeLine = "Sorcery"
        spell {
            effect = Effects.Pipeline {
                val yard = gather(CardSource.FromZone(Zone.GRAVEYARD))
                run(Effects.If(Conditions.AllInCollectionShareCardType(yard), Effects.GainLife(10)))
            }
        }
    }
    val bear = card("All-Share Bear") {
        manaCost = "{1}{G}"; typeLine = "Creature — Bear"; power = 2; toughness = 2
    }
    val walker = card("All-Share Walker") {
        manaCost = "{3}"; typeLine = "Artifact Creature — Golem"; power = 3; toughness = 3
    }
    val relic = card("All-Share Relic") {
        manaCost = "{1}"; typeLine = "Artifact"
    }

    fun lifeAfterCasting(vararg graveyard: String): Int {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(probe, bear, walker, relic))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        graveyard.forEach { driver.putCardInGraveyard(me, it) }
        val spell = driver.putCardInHand(me, "All-Share Probe")
        driver.giveMana(me, Color.GREEN, 1)
        driver.castSpell(me, spell)
        driver.bothPass()
        return driver.getLifeTotal(me)
    }

    test("an empty collection shares nothing") {
        lifeAfterCasting() shouldBe 20
    }

    test("a single card shares its own card type") {
        lifeAfterCasting("All-Share Relic") shouldBe 30
    }

    test("a card type common to every card qualifies; pairwise overlap alone does not") {
        lifeAfterCasting("All-Share Bear", "All-Share Walker", "All-Share Relic") shouldBe 20
        lifeAfterCasting("All-Share Walker", "All-Share Relic") shouldBe 30
        lifeAfterCasting("All-Share Bear", "All-Share Walker") shouldBe 30
    }

    test("two cards sharing a type are not enough when a third shares none with them") {
        lifeAfterCasting("All-Share Bear", "All-Share Bear", "All-Share Relic") shouldBe 20
    }
})
