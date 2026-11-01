package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.`5dn`.cards.CrucibleOfWorlds
import com.wingedsheep.mtg.sets.definitions.war.cards.TomikDistinguishedAdvokist
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tomik, Distinguished Advokist (WAR #34).
 *
 * "Lands on the battlefield and land cards in graveyards can't be the targets of spells or abilities
 * your opponents control. Your opponents can't play land cards from graveyards."
 */
class TomikDistinguishedAdvokistScenarioTest : FunSpec({

    val stoneRain = card("Test Tomik Stone Rain") {
        manaCost = "{1}"
        typeLine = "Sorcery"
        spell {
            val t = target(TargetFilter.Land)
            effect = Effects.Destroy(t)
        }
    }
    val raise = card("Test Tomik Raise") {
        manaCost = "{1}"
        typeLine = "Sorcery"
        spell {
            val t = target(TargetFilter.CardInGraveyard)
            effect = Effects.ReturnToHandFromGraveyard(t)
        }
    }

    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(TomikDistinguishedAdvokist, CrucibleOfWorlds, stoneRain, raise))
        it.initMirrorMatch(Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("an opponent can't target lands on the battlefield, even their own") {
        val d = driver()
        d.putCreatureOnBattlefield(d.player2, TomikDistinguishedAdvokist.name)
        val tomiksLand = d.putLandOnBattlefield(d.player2, "Plains")
        val ownLand = d.putLandOnBattlefield(d.player1, "Plains")
        val spell = d.putCardInHand(d.player1, stoneRain.name)
        d.giveColorlessMana(d.player1, 1)
        d.legalActions(d.player1).none { (it.action as? CastSpell)?.cardId == spell } shouldBe true
        d.castSpell(d.player1, spell, listOf(tomiksLand)).error shouldNotBe null
        d.castSpell(d.player1, spell, listOf(ownLand)).error shouldNotBe null
    }

    test("an opponent can't target a land card in a graveyard, but can target other cards there") {
        val d = driver()
        d.putCreatureOnBattlefield(d.player2, TomikDistinguishedAdvokist.name)
        val plains = d.putCardInGraveyard(d.player1, "Plains")
        val bear = d.putCardInGraveyard(d.player1, "Grizzly Bears")
        val spell = d.putCardInHand(d.player1, raise.name)
        d.giveColorlessMana(d.player1, 1)
        d.castSpellWithTargets(d.player1, spell, listOf(ChosenTarget.Card(plains, d.player1, Zone.GRAVEYARD)))
            .error shouldNotBe null
        d.castSpellWithTargets(d.player1, spell, listOf(ChosenTarget.Card(bear, d.player1, Zone.GRAVEYARD)))
            .error shouldBe null
    }

    test("Tomik's controller still targets lands") {
        val d = driver()
        d.putCreatureOnBattlefield(d.player1, TomikDistinguishedAdvokist.name)
        val theirLand = d.putLandOnBattlefield(d.player2, "Plains")
        val spell = d.putCardInHand(d.player1, stoneRain.name)
        d.giveColorlessMana(d.player1, 1)
        d.castSpell(d.player1, spell, listOf(theirLand)).error shouldBe null
        d.bothPass()
        d.getGraveyard(d.player2).contains(theirLand) shouldBe true
    }

    test("an opponent with Crucible of Worlds can't play lands from their graveyard; Tomik's controller can") {
        val d = driver()
        d.putCreatureOnBattlefield(d.player2, TomikDistinguishedAdvokist.name)
        d.putPermanentOnBattlefield(d.player1, CrucibleOfWorlds.name)
        val graveLand = d.putCardInGraveyard(d.player1, "Plains")
        d.legalActions(d.player1).none { (it.action as? PlayLand)?.cardId == graveLand } shouldBe true
        d.playLand(d.player1, graveLand).error shouldNotBe null
        // A land from hand is still playable.
        val handLand = d.putCardInHand(d.player1, "Plains")
        d.playLand(d.player1, handLand).error shouldBe null
    }

    test("Tomik doesn't stop its own controller playing lands from their graveyard") {
        val d = driver()
        d.putCreatureOnBattlefield(d.player1, TomikDistinguishedAdvokist.name)
        d.putPermanentOnBattlefield(d.player1, CrucibleOfWorlds.name)
        val graveLand = d.putCardInGraveyard(d.player1, "Plains")
        d.playLand(d.player1, graveLand).error shouldBe null
        d.state.getBattlefield().contains(graveLand) shouldBe true
    }
})
