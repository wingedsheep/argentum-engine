package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.RedirectZoneChangeWith
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * A [RedirectZoneChangeWith] rider with the default WITH_REPLACEMENT timing is part of the
 * replacement (CR 614.1): no stack, no priority between the move and the rest. Riders the zone
 * mover can run inline (extra turn, counters on the moved card, a fixed life gain, hand locks) do
 * so; any other effect — creating a token, drawing a card — is queued as a pending replacement
 * rider and runs with a full effect context before anything else happens.
 *
 * Pins down:
 *  - a token rider runs when the death is a state-based action (lethal damage), and the token
 *    belongs to the replacement's controller;
 *  - a token rider runs when the death is a destroy instruction;
 *  - a composite rider mixing an inline part (gain 2 life) and a queued one (draw a card) runs both;
 *  - a non-matching death runs no rider.
 */
class ZoneChangeReplacementRiderTest : FunSpec({

    fun opponentDeathShield(rider: com.wingedsheep.sdk.scripting.effects.Effect) = RedirectZoneChangeWith(
        newDestination = Zone.EXILE,
        additionalEffect = rider,
        appliesTo = EventPattern.ZoneChangeEvent(
            filter = GameObjectFilter.Creature.nontoken().opponentControls(),
            from = Zone.BATTLEFIELD,
            to = Zone.GRAVEYARD,
        ),
    )

    val tokenShield = card("Test Token Shield") {
        manaCost = "{0}"
        typeLine = "Enchantment"
        replacementEffect(
            opponentDeathShield(
                Effects.CreateToken(power = 2, toughness = 2, colors = setOf(Color.BLACK), creatureTypes = setOf("Zombie"))
            )
        )
    }
    val drawShield = card("Test Draw Shield") {
        manaCost = "{0}"
        typeLine = "Enchantment"
        replacementEffect(opponentDeathShield(Effects.GainLife(2) then Effects.DrawCards(1)))
    }
    val doom = card("Test Rider Doom") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.Destroy(t)
        }
    }
    val zap = card("Test Rider Zap") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.DealDamage(3, t)
        }
    }

    val cards = TestCards.all + listOf(tokenShield, drawShield, doom, zap)

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(cards)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, driver.player1)
        return driver
    }

    fun GameTestDriver.castAndResolve(player: EntityId, name: String, target: EntityId) {
        castSpell(player, putCardInHand(player, name), listOf(target)).error shouldBe null
        var guard = 0
        while (state.stack.isNotEmpty()) {
            check(guard++ < 10) { "stack didn't resolve: pending ${state.pendingDecision}" }
            bothPass()
        }
    }

    fun GameTestDriver.zombiesOf(player: EntityId) = getCreatures(player).filter {
        state.projectedState.hasSubtype(it, "Zombie")
    }

    test("a token rider runs when the creature dies to lethal damage (state-based action)") {
        val d = createDriver()
        d.putPermanentOnBattlefield(d.player1, tokenShield.name)
        val bears = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")

        d.castAndResolve(d.player1, zap.name, bears)

        d.getExile(d.player2) shouldContain bears
        d.getGraveyard(d.player2) shouldNotContain bears
        withClue("the replacement's controller creates the token") {
            d.zombiesOf(d.player1) shouldHaveSize 1
            d.zombiesOf(d.player2) shouldHaveSize 0
        }
    }

    test("a token rider runs when the creature is destroyed by an instruction") {
        val d = createDriver()
        d.putPermanentOnBattlefield(d.player1, tokenShield.name)
        val bears = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")

        d.castAndResolve(d.player1, doom.name, bears)

        d.getExile(d.player2) shouldContain bears
        d.zombiesOf(d.player1) shouldHaveSize 1
    }

    test("a composite rider runs its inline part and its queued part") {
        val d = createDriver()
        d.putPermanentOnBattlefield(d.player1, drawShield.name)
        val bears = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        val life = d.getLifeTotal(d.player1)
        val doomCard = d.putCardInHand(d.player1, doom.name)
        val handBefore = d.getHand(d.player1).size

        d.castSpell(d.player1, doomCard, listOf(bears)).error shouldBe null
        while (d.state.stack.isNotEmpty()) d.bothPass()

        d.getExile(d.player2) shouldContain bears
        withClue("gain 2 life (inline)") { d.getLifeTotal(d.player1) shouldBe life + 2 }
        withClue("draw a card (queued); the cast Doom left the hand") {
            d.getHand(d.player1).size shouldBe handBefore - 1 + 1
        }
    }

    test("a death the shield doesn't match runs no rider") {
        val d = createDriver()
        d.putPermanentOnBattlefield(d.player1, tokenShield.name)
        val mine = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")

        d.castAndResolve(d.player1, zap.name, mine)

        d.getGraveyard(d.player1) shouldContain mine
        d.zombiesOf(d.player1) shouldHaveSize 0
    }
})
