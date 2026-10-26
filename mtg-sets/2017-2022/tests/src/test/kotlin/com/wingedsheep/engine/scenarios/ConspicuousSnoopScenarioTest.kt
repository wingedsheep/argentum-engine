package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.m21.cards.ConspicuousSnoop
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Conspicuous Snoop (M21 #139) — plays with the top card of your library revealed, lets you cast
 * Goblin spells from there, and has every activated ability of a Goblin card on top
 * (`DonorCards.TOP_OF_YOUR_LIBRARY`), re-read live as the top card changes.
 */
class ConspicuousSnoopScenarioTest : FunSpec({

    val goblinPinger = card("Snoop Test Goblin") {
        manaCost = "{1}{R}"
        typeLine = "Creature — Goblin"
        power = 1
        toughness = 1
        oracleText = "{T}: You gain 2 life."
        activatedAbility {
            cost = Costs.Tap
            effect = Effects.GainLife(2)
        }
    }
    val elfHealer = card("Snoop Test Elf") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Elf"
        power = 1
        toughness = 1
        oracleText = "{T}: You gain 3 life."
        activatedAbility {
            cost = Costs.Tap
            effect = Effects.GainLife(3)
        }
    }
    val goblinAbility = goblinPinger.activatedAbilities[0].id
    val elfAbility = elfHealer.activatedAbilities[0].id

    fun driver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(ConspicuousSnoop, goblinPinger, elfHealer))
        it.initMirrorMatch(Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("Snoop has the activated abilities of a Goblin card on top, and loses them when it's cast") {
        val d = driver()
        val snoop = d.putPermanentOnBattlefield(d.player1, "Conspicuous Snoop")
        val goblin = d.putCardOnTopOfLibrary(d.player1, "Snoop Test Goblin")
        d.giveMana(d.player1, Color.RED, 2)

        withClue("the Goblin on top is offered as a cast from the library") {
            d.legalActions(d.player1).any {
                it.actionType == "CastSpell" && it.description.contains("Snoop Test Goblin")
            } shouldBe true
        }

        val life = d.getLifeTotal(d.player1)
        d.submit(ActivateAbility(d.player1, snoop, goblinAbility)).error shouldBe null
        d.bothPass()
        d.getLifeTotal(d.player1) shouldBe life + 2
        d.isTapped(snoop) shouldBe true

        d.castSpell(d.player1, goblin).error shouldBe null
        d.bothPass()
        d.findPermanent(d.player1, "Snoop Test Goblin") shouldNotBe null

        withClue("with the Goblin gone from the top, Snoop no longer has its ability") {
            d.untapPermanent(snoop)
            d.submit(ActivateAbility(d.player1, snoop, goblinAbility)).error shouldNotBe null
        }
    }

    test("a non-Goblin card on top lends nothing and can't be cast from there") {
        val d = driver()
        val snoop = d.putPermanentOnBattlefield(d.player1, "Conspicuous Snoop")
        d.putCardOnTopOfLibrary(d.player1, "Snoop Test Elf")
        d.giveMana(d.player1, Color.GREEN, 2)

        d.submit(ActivateAbility(d.player1, snoop, elfAbility)).error shouldNotBe null
        d.legalActions(d.player1).any {
            it.actionType == "CastSpell" && it.description.contains("Snoop Test Elf")
        } shouldBe false
    }
})
