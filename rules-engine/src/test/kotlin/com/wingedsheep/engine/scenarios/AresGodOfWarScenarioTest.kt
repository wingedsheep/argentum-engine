package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.msh.cards.AresGodOfWar
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Ares, God of War (MSH) — "Whenever an attacking creature you control dies, return that card to
 * its owner's hand."
 *
 * The "attacking" half is the whole test. Putting `.attacking()` in the zone-change trigger filter
 * looks right and compiles, but `TriggerMatcher` carries last-known info only for the
 * counter/power/attachment predicates; `StatePredicate.IsAttacking` falls through to the explicit
 * "don't gate" list and returns `true` unconditionally, so the filter matched every dying creature
 * you control. Both polarities are pinned here: an attacker that dies comes back, a creature that
 * dies without ever attacking does not.
 */
class AresGodOfWarScenarioTest : FunSpec({

    // 1/1, small enough to die to the 4/4 blocker and to one Zap.
    val Grunt = CardDefinition.creature(
        name = "Ares Test Grunt",
        manaCost = ManaCost.parse("{1}{R}"),
        subtypes = setOf(Subtype("Warrior")),
        power = 1,
        toughness = 1,
    )

    // 4/4 with vigilance: kills whatever it blocks and survives.
    val Bruiser = CardDefinition.creature(
        name = "Ares Test Bruiser",
        manaCost = ManaCost.parse("{2}{G}{G}"),
        subtypes = setOf(Subtype("Bear")),
        power = 4,
        toughness = 4,
        keywords = setOf(Keyword.VIGILANCE),
    )

    // Lets the second test kill a creature that never attacked, without crossing a turn boundary.
    val Zap = card("Ares Test Zap") {
        manaCost = "{R}"
        typeLine = "Instant"
        spell {
            val victim = target("target creature", Targets.Creature)
            effect = Effects.DealDamage(3, victim)
        }
    }

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(Grunt, Bruiser))
        registerCard(AresGodOfWar)
        registerCard(Zap)
        initMirrorMatch(Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
    }

    /** Resolve whatever is on the stack, answering decisions, without advancing the turn. */
    fun GameTestDriver.resolveStack() {
        repeat(16) {
            if (state.pendingDecision != null) autoResolveDecision()
            else if (stackSize > 0) bothPass()
            else return
        }
    }

    fun GameTestDriver.handCardNames(playerId: EntityId): List<String> =
        getHand(playerId).mapNotNull { id -> state.getEntity(id)?.get<CardComponent>()?.name }

    fun GameTestDriver.graveyardNames(playerId: EntityId): List<String> =
        state.getZone(ZoneKey(playerId, Zone.GRAVEYARD))
            .mapNotNull { id -> state.getEntity(id)?.get<CardComponent>()?.name }

    test("an attacking creature that dies returns to its owner's hand") {
        val d = driver()
        val p1 = d.activePlayer!!
        val p2 = d.getOpponent(p1)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val ares = d.putCreatureOnBattlefield(p1, "Ares, God of War")
        d.removeSummoningSickness(ares)
        val grunt = d.putCreatureOnBattlefield(p1, "Ares Test Grunt")
        d.removeSummoningSickness(grunt)
        val bruiser = d.putCreatureOnBattlefield(p2, "Ares Test Bruiser")
        d.removeSummoningSickness(bruiser)

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        // Ares attacks each combat if able, so he is declared alongside the Grunt.
        d.declareAttackers(p1, listOf(ares, grunt), p2)
        d.bothPass()
        d.declareBlockers(p2, mapOf(bruiser to listOf(grunt)))
        // Blockers -> combat damage. The stack is empty here, so this has to be an explicit pass:
        // resolveStack() alone would return immediately and never deal damage.
        d.bothPass()
        d.currentStep shouldBe Step.COMBAT_DAMAGE
        d.bothPass()
        d.resolveStack()

        withClue("hand=${d.handCardNames(p1)} graveyard=${d.graveyardNames(p1)}") {
            d.handCardNames(p1).count { it == "Ares Test Grunt" } shouldBe 1
        }
    }

    test("a creature you control that dies without attacking is NOT returned") {
        val d = driver()
        val p1 = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val ares = d.putCreatureOnBattlefield(p1, "Ares, God of War")
        d.removeSummoningSickness(ares)
        // The Grunt never attacks — it dies in the main phase, before combat exists at all.
        val grunt = d.putCreatureOnBattlefield(p1, "Ares Test Grunt")
        d.removeSummoningSickness(grunt)

        val zap = d.putCardInHand(p1, "Ares Test Zap")
        d.giveMana(p1, Color.RED, 1)
        d.castSpell(p1, zap, listOf(grunt)).isSuccess shouldBe true
        d.resolveStack()

        withClue("graveyard=${d.graveyardNames(p1)} — the Grunt must have died") {
            d.graveyardNames(p1).count { it == "Ares Test Grunt" } shouldBe 1
        }
        withClue("hand=${d.handCardNames(p1)} — a non-attacker died, so Ares must not fire") {
            d.handCardNames(p1).count { it == "Ares Test Grunt" } shouldBe 0
        }
    }
})
