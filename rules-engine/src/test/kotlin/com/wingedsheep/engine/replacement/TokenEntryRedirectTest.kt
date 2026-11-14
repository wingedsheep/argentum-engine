package com.wingedsheep.engine.replacement

import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.RedirectZoneChange
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * A battlefield-entry `RedirectZoneChange` applies to **tokens** as they are created
 * (`TokenEntryRedirect`): creating a token puts it onto the battlefield, so "if a creature would
 * enter and it wasn't cast, exile it instead" (Hallowed Moonlight) sends a creature token to exile,
 * where it ceases to exist, without it ever entering — no entry triggers, nothing left behind.
 * Tokens are never cast. The redirect's filter still decides: a `nontoken()` redirect (Containment
 * Priest) lets tokens through, and a creature-only one lets a Treasure through.
 */
class TokenEntryRedirectTest : FunSpec({

    val moonlightLamp = card("Moonlight Lamp Test") {
        manaCost = "{2}"
        typeLine = "Artifact"
        replacementEffect(
            RedirectZoneChange(
                newDestination = Zone.EXILE,
                appliesTo = EventPattern.ZoneChangeEvent(
                    filter = GameObjectFilter.Creature.notCast(),
                    to = Zone.BATTLEFIELD,
                ),
            )
        )
    }
    val nontokenLamp = card("Nontoken Lamp Test") {
        manaCost = "{2}"
        typeLine = "Artifact"
        replacementEffect(
            RedirectZoneChange(
                newDestination = Zone.EXILE,
                appliesTo = EventPattern.ZoneChangeEvent(
                    filter = GameObjectFilter.Creature.nontoken().notCast(),
                    to = Zone.BATTLEFIELD,
                ),
            )
        )
    }
    val makeSoldiers = card("Make Soldiers Test") {
        manaCost = "{W}"
        typeLine = "Sorcery"
        spell { effect = Effects.CreateToken(1, 1, setOf(Color.WHITE), setOf("Soldier"), count = 2) }
    }
    val makeTreasure = card("Make Treasure Test") {
        manaCost = "{W}"
        typeLine = "Sorcery"
        spell { effect = Effects.CreateTreasure(1) }
    }
    val copyCreature = card("Copy Creature Test") {
        manaCost = "{W}"
        typeLine = "Sorcery"
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.CreateTokenCopyOfTarget(t)
        }
    }
    val entryWatcher = card("Entry Watcher Test") {
        manaCost = "{W}"
        typeLine = "Enchantment"
        triggeredAbility {
            trigger = Triggers.another(GameObjectFilter.Creature).enters()
            effect = Effects.GainLife(1)
        }
    }

    fun newDriver(vararg permanents: String): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + PredefinedTokens.allTokens)
        listOf(moonlightLamp, nontokenLamp, makeSoldiers, makeTreasure, copyCreature, entryWatcher)
            .forEach(d::registerCard)
        d.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        permanents.forEach { d.putPermanentOnBattlefield(d.player1, it) }
        return d
    }

    fun GameTestDriver.resolveAll() {
        var guard = 0
        while (state.stack.isNotEmpty() && guard++ < 20) bothPass()
    }

    fun GameTestDriver.battlefieldNames(player: EntityId): List<String> =
        getPermanents(player).mapNotNull { state.getEntity(it)?.get<CardComponent>()?.name }

    test("creature tokens are exiled instead of entering, cease to exist, and trigger nothing") {
        val d = newDriver(moonlightLamp.name, entryWatcher.name)
        val spell = d.putCardInHand(d.player1, makeSoldiers.name)
        d.giveMana(d.player1, Color.WHITE, 1)
        d.castSpell(d.player1, spell).error shouldBe null
        d.bothPass()
        val creations = d.events.filterIsInstance<ZoneChangeEvent>().filter { it.fromZone == null }
        creations.count { it.toZone == Zone.EXILE } shouldBe 2
        creations.none { it.toZone == Zone.BATTLEFIELD } shouldBe true
        d.resolveAll()

        d.getCreatures(d.player1).shouldBeEmpty()
        // The tokens-in-other-zones state-based action removed them from exile.
        d.getExile(d.player1).shouldBeEmpty()
        d.getLifeTotal(d.player1) shouldBe 20
    }

    test("a noncreature token isn't a creature entering — the Treasure is created") {
        val d = newDriver(moonlightLamp.name)
        val spell = d.putCardInHand(d.player1, makeTreasure.name)
        d.giveMana(d.player1, Color.WHITE, 1)
        d.castSpell(d.player1, spell).error shouldBe null
        d.resolveAll()

        d.battlefieldNames(d.player1) shouldContain "Treasure"
    }

    test("a token copy of a creature is exiled instead") {
        val d = newDriver(moonlightLamp.name)
        val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val spell = d.putCardInHand(d.player1, copyCreature.name)
        d.giveMana(d.player1, Color.WHITE, 1)
        d.castSpell(d.player1, spell, listOf(bears)).error shouldBe null
        d.resolveAll()

        d.getCreatures(d.player1) shouldBe listOf(bears)
    }

    test("a nontoken-only redirect (Containment Priest) lets creature tokens enter") {
        val d = newDriver(nontokenLamp.name, entryWatcher.name)
        val spell = d.putCardInHand(d.player1, makeSoldiers.name)
        d.giveMana(d.player1, Color.WHITE, 1)
        d.castSpell(d.player1, spell).error shouldBe null
        d.resolveAll()

        d.getCreatures(d.player1).size shouldBe 2
        d.getLifeTotal(d.player1) shouldBe 22
    }

    test("without a redirect in force the tokens enter as normal") {
        val d = newDriver(entryWatcher.name)
        val spell = d.putCardInHand(d.player1, makeSoldiers.name)
        d.giveMana(d.player1, Color.WHITE, 1)
        d.castSpell(d.player1, spell).error shouldBe null
        d.resolveAll()

        d.getCreatures(d.player1).size shouldBe 2
        d.getLifeTotal(d.player1) shouldBe 22
    }
})
