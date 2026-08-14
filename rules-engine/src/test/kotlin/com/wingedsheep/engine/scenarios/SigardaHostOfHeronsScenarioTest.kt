package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.avr.cards.KillingWave
import com.wingedsheep.mtg.sets.definitions.avr.cards.SigardaHostOfHerons
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Sigarda, Host of Herons — {2}{G}{W}{W} Legendary Creature — Angel 5/5
 * "Flying, hexproof. Spells and abilities your opponents control can't cause you to sacrifice
 * permanents."
 *
 * The grant has to hold across the shapes an opponent can reach for: a plain edict, a
 * pay-or-sacrifice sweeper whose per-creature sacrifice runs under a per-player iteration
 * (Killing Wave), and — per the 2018-12-07 ruling — it must leave the controller's *own*
 * sacrifice effects alone.
 */
class SigardaHostOfHeronsScenarioTest : FunSpec({

    // Untargeted ("each opponent sacrifices a creature") so the test exercises the sacrifice, not
    // the targeting layer.
    val edict = card("Grim Decree") {
        manaCost = "{1}{B}"
        colorIdentity = "B"
        typeLine = "Sorcery"
        spell {
            effect = Effects.Sacrifice(
                GameObjectFilter.Creature,
                target = EffectTarget.PlayerRef(Player.EachOpponent),
            )
        }
    }
    val selfEdict = card("Grim Devotion") {
        manaCost = "{1}{B}"
        colorIdentity = "B"
        typeLine = "Sorcery"
        spell {
            effect = Effects.Sacrifice(GameObjectFilter.Creature, target = EffectTarget.Controller)
        }
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(SigardaHostOfHerons, KillingWave, edict, selfEdict))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("an opponent's edict can't make Sigarda's controller sacrifice anything") {
        val driver = newDriver()
        val caster = driver.activePlayer!!
        val defender = driver.getOpponent(caster)

        driver.putCreatureOnBattlefield(defender, "Sigarda, Host of Herons")
        driver.putCreatureOnBattlefield(defender, "Grizzly Bears")

        val decree = driver.putCardInHand(caster, "Grim Decree")
        driver.giveMana(caster, Color.BLACK, 1)
        driver.giveColorlessMana(caster, 1)
        driver.castSpell(caster, decree).isSuccess shouldBe true
        driver.bothPass()

        withClue("no prompt at all — the protected player is never offered the choice") {
            driver.pendingDecision shouldBe null
        }
        driver.findPermanent(defender, "Grizzly Bears") shouldNotBe null
        driver.findPermanent(defender, "Sigarda, Host of Herons") shouldNotBe null
        driver.getGraveyardCardNames(defender) shouldBe emptyList()
    }

    test("the same edict still works on a player without Sigarda") {
        val driver = newDriver()
        val caster = driver.activePlayer!!
        val defender = driver.getOpponent(caster)

        driver.putCreatureOnBattlefield(defender, "Grizzly Bears")

        val decree = driver.putCardInHand(caster, "Grim Decree")
        driver.giveMana(caster, Color.BLACK, 1)
        driver.giveColorlessMana(caster, 1)
        driver.castSpell(caster, decree).isSuccess shouldBe true
        driver.bothPass()

        driver.findPermanent(defender, "Grizzly Bears") shouldBe null
        driver.getGraveyardCardNames(defender) shouldBe listOf("Grizzly Bears")
    }

    test("Killing Wave sweeps the caster's board but leaves Sigarda's controller untouched") {
        val driver = newDriver()
        val caster = driver.activePlayer!!
        val defender = driver.getOpponent(caster)

        driver.putCreatureOnBattlefield(caster, "Grizzly Bears")
        driver.putCreatureOnBattlefield(defender, "Sigarda, Host of Herons")
        driver.putCreatureOnBattlefield(defender, "Grizzly Bears")

        val wave = driver.putCardInHand(caster, "Killing Wave")
        driver.giveMana(caster, Color.BLACK, 3)
        driver.castXSpell(caster, wave, xValue = 2).isSuccess shouldBe true
        driver.bothPass()

        // The caster is still asked about their own creature — Sigarda protects only her controller.
        driver.pendingDecision shouldNotBe null
        driver.submitYesNo(caster, false).error shouldBe null

        var guard = 0
        while (driver.isPaused && guard++ < 10) driver.autoResolveDecision()

        driver.findPermanent(caster, "Grizzly Bears") shouldBe null
        withClue("the protected player pays nothing and sacrifices nothing") {
            driver.getLifeTotal(defender) shouldBe 20
            driver.findPermanent(defender, "Grizzly Bears") shouldNotBe null
            driver.findPermanent(defender, "Sigarda, Host of Herons") shouldNotBe null
        }
    }

    test("Sigarda's controller can still sacrifice to their own spell") {
        val driver = newDriver()
        val player = driver.activePlayer!!

        driver.putCreatureOnBattlefield(player, "Sigarda, Host of Herons")
        driver.putCreatureOnBattlefield(player, "Grizzly Bears")

        val devotion = driver.putCardInHand(player, "Grim Devotion")
        driver.giveMana(player, Color.BLACK, 1)
        driver.giveColorlessMana(player, 1)
        driver.castSpell(player, devotion).isSuccess shouldBe true
        driver.bothPass()

        // Two creatures, one to sacrifice — the controller picks.
        driver.pendingDecision shouldNotBe null
        val bears = driver.findPermanent(player, "Grizzly Bears")!!
        driver.submitCardSelection(player, listOf(bears)).error shouldBe null

        driver.findPermanent(player, "Grizzly Bears") shouldBe null
        driver.getGraveyardCardNames(player) shouldContain "Grizzly Bears"
    }
})
