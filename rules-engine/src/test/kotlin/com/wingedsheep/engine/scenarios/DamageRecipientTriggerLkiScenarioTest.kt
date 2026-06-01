package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.RecipientFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Regression for the recipient-based damage-trigger last-known-information gap: a creature that
 * dies to the same combat-damage event has its `ControllerComponent` stripped (it's in the
 * graveyard) by the time triggers are detected — combat-damage state-based actions run before
 * trigger detection. Before the fix, `RecipientFilter.CreatureOpponentControls` /
 * `CreatureYouControl` resolved the recipient's controller from *live* state, so a trigger
 * watching "a creature an opponent controls / you control is dealt combat damage" silently
 * missed the killing blow. The fix captures the recipient's controller + creature-ness at
 * damage time onto `DamageDealtEvent` and falls back to it in `TriggerMatcher` (CR 603.10).
 *
 * Noncombat damage already worked (its trigger detection sees the recipient before the kill
 * SBA); the noncombat cases here guard that LKI population didn't regress that path and that
 * control discrimination still holds.
 */
class DamageRecipientTriggerLkiScenarioTest : FunSpec({

    // 0/8 sentinels (survive everything) that draw a card whenever the watched recipient is
    // dealt combat damage. binding = ANY so they watch the whole table.
    val OpponentCreatureWatcher = card("Opp Creature Watcher") {
        manaCost = "{0}"; typeLine = "Creature — Spirit"; power = 0; toughness = 8
        triggeredAbility {
            trigger = Triggers.dealsDamage(
                damageType = DamageType.Combat,
                recipient = RecipientFilter.CreatureOpponentControls,
                binding = TriggerBinding.ANY,
            )
            effect = Effects.DrawCards(1)
        }
    }
    val YourCreatureWatcher = card("Your Creature Watcher") {
        manaCost = "{0}"; typeLine = "Creature — Spirit"; power = 0; toughness = 8
        triggeredAbility {
            trigger = Triggers.dealsDamage(
                damageType = DamageType.Combat,
                recipient = RecipientFilter.CreatureYouControl,
                binding = TriggerBinding.ANY,
            )
            effect = Effects.DrawCards(1)
        }
    }
    // Noncombat version, to guard the already-working path + control discrimination.
    val OpponentCreatureWatcherNoncombat = card("Opp Creature Watcher NC") {
        manaCost = "{0}"; typeLine = "Creature — Spirit"; power = 0; toughness = 8
        triggeredAbility {
            trigger = Triggers.dealsDamage(
                damageType = DamageType.NonCombat,
                recipient = RecipientFilter.CreatureOpponentControls,
                binding = TriggerBinding.ANY,
            )
            effect = Effects.DrawCards(1)
        }
    }

    fun vanilla(n: String, p: Int, t: Int) = card(n) {
        manaCost = "{0}"; typeLine = "Creature — Bear"; power = p; toughness = t
    }
    val Bear = vanilla("LKI Bear", 2, 2)          // dies to >=2
    val Ogre = vanilla("LKI Ogre", 3, 3)          // 3 power attacker/blocker
    val Smasher = vanilla("LKI Smasher", 5, 5)
    val Wall = vanilla("LKI Wall", 0, 6)          // survives 5 damage

    val Bolt = card("LKI Bolt") {
        manaCost = "{0}"; typeLine = "Sorcery"; oracleText = "Deal 5 damage to target creature."
        spell {
            val c = target("target creature", Targets.Creature)
            effect = Effects.DealDamage(5, c)
        }
    }

    fun driver() = GameTestDriver().apply {
        registerCards(
            TestCards.all + listOf(
                OpponentCreatureWatcher, YourCreatureWatcher, OpponentCreatureWatcherNoncombat,
                Bear, Ogre, Smasher, Wall, Bolt
            )
        )
    }

    test("combat: an opponent's creature that DIES to combat damage still fires CreatureOpponentControls") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Forest" to 40))
        val active = d.activePlayer!!
        val opp = d.getOpponent(active)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        d.putCreatureOnBattlefield(active, "Opp Creature Watcher")
        val attacker = d.putCreatureOnBattlefield(active, "LKI Smasher") // 5/5
        val blocker = d.putCreatureOnBattlefield(opp, "LKI Bear")        // 2/2, dies to 5
        d.removeSummoningSickness(attacker)

        val handBefore = d.getHandSize(active)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(active, listOf(attacker), opp)
        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        d.declareBlockers(opp, mapOf(blocker to listOf(attacker)))
        d.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        (blocker !in d.state.getBattlefield()) shouldBe true       // it really died
        d.getHandSize(active) shouldBe handBefore + 1              // trigger fired -> drew
    }

    test("combat: YOUR creature that DIES to combat damage still fires CreatureYouControl") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Forest" to 40))
        val active = d.activePlayer!!
        val opp = d.getOpponent(active)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        d.putCreatureOnBattlefield(active, "Your Creature Watcher")
        val attacker = d.putCreatureOnBattlefield(active, "LKI Bear")  // 2/2 attacker, will die
        val blocker = d.putCreatureOnBattlefield(opp, "LKI Ogre")      // 3/3 blocker, survives the 2
        d.removeSummoningSickness(attacker)

        val handBefore = d.getHandSize(active)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(active, listOf(attacker), opp)
        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        d.declareBlockers(opp, mapOf(blocker to listOf(attacker)))
        d.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        (attacker !in d.state.getBattlefield()) shouldBe true      // active's creature died
        (blocker in d.state.getBattlefield()) shouldBe true        // opp's survived (only took 2)
        d.getHandSize(active) shouldBe handBefore + 1
    }

    test("combat: an opponent's creature that SURVIVES still fires (live-state path intact, no regression)") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Forest" to 40))
        val active = d.activePlayer!!
        val opp = d.getOpponent(active)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        d.putCreatureOnBattlefield(active, "Opp Creature Watcher")
        val attacker = d.putCreatureOnBattlefield(active, "LKI Smasher") // 5/5
        val blocker = d.putCreatureOnBattlefield(opp, "LKI Wall")        // 0/6, survives 5
        d.removeSummoningSickness(attacker)

        val handBefore = d.getHandSize(active)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(active, listOf(attacker), opp)
        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        d.declareBlockers(opp, mapOf(blocker to listOf(attacker)))
        d.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        (blocker in d.state.getBattlefield()) shouldBe true
        d.getHandSize(active) shouldBe handBefore + 1
    }

    test("noncombat: your own dying creature does NOT fire a CreatureOpponentControls trigger (control discrimination via LKI)") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Forest" to 40))
        val active = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        d.putCreatureOnBattlefield(active, "Opp Creature Watcher NC")
        val ownBear = d.putCreatureOnBattlefield(active, "LKI Bear")  // active's own 2/2

        val handBefore = d.getHandSize(active)
        val bolt = d.putCardInHand(active, "LKI Bolt")
        d.castSpell(active, bolt, listOf(ownBear))                    // 5 noncombat dmg, ownBear dies
        d.bothPass()

        (ownBear !in d.state.getBattlefield()) shouldBe true          // it died
        d.getHandSize(active) shouldBe handBefore                     // own creature -> no fire
    }
})
