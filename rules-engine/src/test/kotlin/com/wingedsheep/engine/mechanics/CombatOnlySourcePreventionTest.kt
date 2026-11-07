package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.PreventionDirection
import com.wingedsheep.sdk.scripting.effects.PreventionScope
import com.wingedsheep.sdk.scripting.effects.PreventionSourceFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Source-side damage prevention scoped to combat damage — "prevent all **combat** damage that
 * would be dealt by target creature this turn" (Restrain, Subdue, Safeguard) and "prevent all
 * combat damage a creature of your choice would deal this turn" (Ajani's Aid).
 *
 * Combat damage is the damage creatures deal in the combat damage step (CR 510.2); a shield scoped
 * to it leaves the same creature's noncombat damage alone. The all-damage version (Chain of
 * Silence, Burrenton Forge-Tender) still stops both.
 */
class CombatOnlySourcePreventionTest : FunSpec({

    // {0}: this creature deals 1 damage to target player. A free, untapped ping so the same
    // creature can then attack.
    val pinger = card("Test Combat Pinger") {
        manaCost = "{1}"
        typeLine = "Creature — Elemental"
        power = 2
        toughness = 2
        activatedAbility {
            cost = Costs.Free
            val player = target(Targets.Player)
            effect = Effects.DealDamage(1, player)
        }
    }
    // Targeted: Restrain's shape.
    fun targetedShield(name: String, scope: PreventionScope) = card(name) {
        manaCost = "{W}"
        typeLine = "Instant"
        spell {
            val creature = target(TargetFilter.Creature)
            effect = Effects.PreventAllDamageDealtBy(creature, scope = scope)
        }
    }
    val combatSilence = targetedShield("Test Combat Silence", PreventionScope.CombatOnly)
    val fullSilence = targetedShield("Test Full Silence", PreventionScope.AllDamage)

    // Chosen on resolution: Ajani's Aid's shape (combat only) and Burrenton Forge-Tender's (all damage).
    fun chosenShield(name: String, combatOnly: Boolean) = card(name) {
        manaCost = "{0}"
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.SacrificeSelf
            effect = Effects.PreventDamage(
                direction = PreventionDirection.FromTarget,
                sources = PreventionSourceFilter.Chosen(GameObjectFilter.Creature.onBattlefield()),
                combatOnly = combatOnly
            )
        }
    }
    val chosenCombat = chosenShield("Test Chosen Combat Shield", combatOnly = true)
    val chosenAll = chosenShield("Test Chosen Full Shield", combatOnly = false)

    fun driver(): Pair<GameTestDriver, EntityId> {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(pinger, combatSilence, fullSilence, chosenCombat, chosenAll))
        d.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val creature = d.putCreatureOnBattlefield(d.player1, "Test Combat Pinger")
        d.removeSummoningSickness(creature)
        return d to creature
    }

    fun GameTestDriver.resolveStack() {
        var guard = 0
        while (guard++ < 10 && state.pendingDecision == null && state.stack.isNotEmpty()) bothPass()
    }

    fun GameTestDriver.silence(name: String, creature: EntityId) {
        val spell = putCardInHand(player1, name)
        giveMana(player1, Color.WHITE, 1)
        castSpell(player1, spell, listOf(creature)).error shouldBe null
        resolveStack()
    }

    fun GameTestDriver.chooseShield(name: String, creature: EntityId) {
        // The pinger's own controller holds the shield: it's their main phase, so they have priority.
        val shield = putPermanentOnBattlefield(player1, name)
        val ability = (if (name == chosenCombat.name) chosenCombat else chosenAll).activatedAbilities.single().id
        submit(ActivateAbility(player1, shield, ability)).error shouldBe null
        resolveStack()
        (state.pendingDecision is SelectCardsDecision) shouldBe true
        submitCardSelection(player1, listOf(creature)).error shouldBe null
        resolveStack()
    }

    fun GameTestDriver.ping(creature: EntityId) {
        submit(
            ActivateAbility(player1, creature, pinger.activatedAbilities.single().id,
                targets = listOf(ChosenTarget.Player(player2)))
        ).error shouldBe null
        resolveStack()
    }

    fun GameTestDriver.attackUnblocked(creature: EntityId) {
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        declareAttackers(player1, listOf(creature), player2).error shouldBe null
        passPriorityUntil(Step.DECLARE_BLOCKERS)
        declareNoBlockers(player2).error shouldBe null
        passPriorityUntil(Step.END_COMBAT)
    }

    test("targeted combat-only shield: noncombat damage is dealt, combat damage is prevented") {
        val (d, creature) = driver()
        d.silence("Test Combat Silence", creature)

        d.ping(creature)
        withClue("the ping is noncombat damage, so the combat-only shield lets it through") {
            d.getLifeTotal(d.player2) shouldBe 19
        }

        d.attackUnblocked(creature)
        withClue("its 2 combat damage is prevented") {
            d.getLifeTotal(d.player2) shouldBe 19
        }
    }

    test("targeted all-damage shield still stops noncombat damage") {
        val (d, creature) = driver()
        d.silence("Test Full Silence", creature)

        d.ping(creature)
        d.attackUnblocked(creature)
        withClue("both the ping and the combat damage are prevented") {
            d.getLifeTotal(d.player2) shouldBe 20
        }
    }

    test("chosen-source combat-only shield: noncombat damage is dealt, combat damage is prevented") {
        val (d, creature) = driver()
        d.chooseShield("Test Chosen Combat Shield", creature)

        d.ping(creature)
        withClue("the chosen creature's noncombat damage still lands") {
            d.getLifeTotal(d.player2) shouldBe 19
        }

        d.attackUnblocked(creature)
        withClue("its combat damage is prevented") {
            d.getLifeTotal(d.player2) shouldBe 19
        }
    }

    test("chosen-source all-damage shield still stops noncombat damage") {
        val (d, creature) = driver()
        d.chooseShield("Test Chosen Full Shield", creature)

        d.ping(creature)
        d.attackUnblocked(creature)
        withClue("both the ping and the combat damage are prevented") {
            d.getLifeTotal(d.player2) shouldBe 20
        }
    }

    test("the source choice offers only creatures on the battlefield") {
        val (d, creature) = driver()
        val shield = d.putPermanentOnBattlefield(d.player1, "Test Chosen Combat Shield")
        d.submit(ActivateAbility(d.player1, shield, chosenCombat.activatedAbilities.single().id)).error shouldBe null
        d.resolveStack()

        val options = (d.state.pendingDecision as SelectCardsDecision).options
        withClue("the pinger is the only creature on the battlefield; the artifact shield is gone") {
            options shouldBe listOf(creature)
        }
    }
})
