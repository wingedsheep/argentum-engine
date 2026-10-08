package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.event.GrantedStaticAbility
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CantBeBlockedByMoreThan
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.MustBeBlocked
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * "Must be blocked" (and its "can't be blocked by more than N" sibling) are static abilities, so a
 * permanent that has lost all its abilities no longer imposes them — whether the requirement is
 * the attacker's own printed static (Goblin Fire Fiend) or a battlefield permanent's filtered
 * static projected onto an attacker (The Masamune). A *granted* "must be blocked" static on the
 * attacker is honoured the same way as a printed one.
 */
class MustBeBlockedAbilityLossTest : FunSpec({
    val lureBeast = card("Must-Block Beast") {
        manaCost = "{1}"; typeLine = "Creature — Beast"; power = 2; toughness = 2
        staticAbility { ability = MustBeBlocked() }
    }
    val lureBanner = card("Must-Block Banner") {
        manaCost = "{1}"; typeLine = "Artifact"
        staticAbility { ability = MustBeBlocked(filter = GroupFilter.AllCreaturesYouControl) }
    }
    val loneBeast = card("Lone Beast") {
        manaCost = "{1}"; typeLine = "Creature — Beast"; power = 2; toughness = 2
        staticAbility { ability = CantBeBlockedByMoreThan(1) }
    }
    val mute = card("Mute Ray") {
        manaCost = "{0}"; typeLine = "Instant"
        spell { val t = target(TargetFilter.Permanent); effect = Effects.RemoveAllAbilities(t) }
    }

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(lureBeast, lureBanner, loneBeast, mute))
        initMirrorMatch(Deck.of("Forest" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.muteAndAttack(muted: EntityId, attacker: EntityId): EntityId {
        val me = activePlayer!!
        val opponent = getOpponent(me)
        castSpell(me, putCardInHand(me, mute.name), listOf(muted)).error shouldBe null
        bothPass()
        state.projectedState.hasLostAllAbilities(muted) shouldBe true
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        declareAttackers(me, listOf(attacker), opponent).outcome shouldBe Outcome.Done
        passPriorityUntil(Step.DECLARE_BLOCKERS)
        return opponent
    }

    test("printed must-be-blocked is enforced while the attacker has its abilities") {
        val d = driver(); val me = d.activePlayer!!
        val beast = d.putCreatureOnBattlefield(me, lureBeast.name).also(d::removeSummoningSickness)
        d.putCreatureOnBattlefield(d.getOpponent(me), "Grizzly Bears")
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(me, listOf(beast), d.getOpponent(me)).outcome shouldBe Outcome.Done
        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        d.declareNoBlockers(d.getOpponent(me)).outcome shouldNotBe Outcome.Done
    }

    test("an attacker that lost all abilities no longer must be blocked") {
        val d = driver(); val me = d.activePlayer!!
        val beast = d.putCreatureOnBattlefield(me, lureBeast.name).also(d::removeSummoningSickness)
        d.putCreatureOnBattlefield(d.getOpponent(me), "Grizzly Bears")
        val opponent = d.muteAndAttack(beast, beast)
        d.declareNoBlockers(opponent).outcome shouldBe Outcome.Done
    }

    test("a source that lost all abilities no longer projects must-be-blocked onto attackers") {
        val d = driver(); val me = d.activePlayer!!
        val banner = d.putPermanentOnBattlefield(me, lureBanner.name)
        val bears = d.putCreatureOnBattlefield(me, "Grizzly Bears").also(d::removeSummoningSickness)
        d.putCreatureOnBattlefield(d.getOpponent(me), "Grizzly Bears")
        val opponent = d.muteAndAttack(banner, bears)
        d.declareNoBlockers(opponent).outcome shouldBe Outcome.Done
    }

    test("a granted must-be-blocked static is enforced") {
        val d = driver(); val me = d.activePlayer!!
        val bears = d.putCreatureOnBattlefield(me, "Grizzly Bears").also(d::removeSummoningSickness)
        d.putCreatureOnBattlefield(d.getOpponent(me), "Grizzly Bears")
        d.replaceState(d.state.copy(grantedStaticAbilities = d.state.grantedStaticAbilities +
            GrantedStaticAbility(bears, MustBeBlocked(), Duration.EndOfTurn)))
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(me, listOf(bears), d.getOpponent(me)).outcome shouldBe Outcome.Done
        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        d.declareNoBlockers(d.getOpponent(me)).outcome shouldNotBe Outcome.Done
    }

    test("an attacker that lost all abilities can be blocked by more than one creature") {
        val d = driver(); val me = d.activePlayer!!
        val beast = d.putCreatureOnBattlefield(me, loneBeast.name).also(d::removeSummoningSickness)
        val b1 = d.putCreatureOnBattlefield(d.getOpponent(me), "Grizzly Bears")
        val b2 = d.putCreatureOnBattlefield(d.getOpponent(me), "Grizzly Bears")
        val opponent = d.muteAndAttack(beast, beast)
        d.declareBlockers(opponent, mapOf(b1 to listOf(beast), b2 to listOf(beast))).outcome shouldBe Outcome.Done
    }
})
