package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.engine.view.ClientStateTransformer
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.PreventionSourceFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

/**
 * A `PreventionSourceFilter.Matching` prevent-all shield on one target — "prevent all damage that
 * would be dealt to that creature this turn by colorless sources" (Lithomancer's Focus). Only the
 * target is protected and only from matching sources; `combatOnly` narrows it to combat damage; a
 * player recipient works too; a capped shape still has no lowering. Both a protected creature and a
 * protected player show what the shield stops.
 */
class MatchingTargetPreventionTest : FunSpec({

    val red = GameObjectFilter.Any.withCardPredicate(CardPredicate.HasColor(Color.RED))

    val redWard = card("Test Red Ward") {
        manaCost = "{W}"
        typeLine = "Instant"
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.PreventDamage(target = t, sources = PreventionSourceFilter.Matching(red))
        }
    }
    val redCombatWard = card("Test Red Combat Ward") {
        manaCost = "{W}"
        typeLine = "Instant"
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.PreventDamage(target = t, sources = PreventionSourceFilter.Matching(red), combatOnly = true)
        }
    }
    val redWardYou = card("Test Red Ward You") {
        manaCost = "{W}"
        typeLine = "Instant"
        spell {
            effect = Effects.PreventDamage(target = EffectTarget.Controller, sources = PreventionSourceFilter.Matching(red))
        }
    }
    val cappedWard = card("Test Capped Red Ward") {
        manaCost = "{W}"
        typeLine = "Instant"
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.PreventDamage(target = t, sources = PreventionSourceFilter.Matching(red), amount = DynamicAmount.Fixed(2))
        }
    }
    val redBlast = card("Test Red Blast") {
        manaCost = "{R}"
        typeLine = "Instant"
        spell {
            val t = target(Targets.Any)
            effect = Effects.DealDamage(2, t)
        }
    }
    val blackBlast = card("Test Black Blast") {
        manaCost = "{B}"
        typeLine = "Instant"
        spell {
            val t = target(Targets.Any)
            effect = Effects.DealDamage(2, t)
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + redWard + redCombatWard + redWardYou + cappedWard + redBlast + blackBlast)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.cast(player: EntityId, name: String, color: Color, target: EntityId? = null) {
        val id = putCardInHand(player, name)
        giveMana(player, color, 1)
        castSpell(player, id, listOfNotNull(target)).error shouldBe null
        bothPass()
    }

    fun GameTestDriver.damageOn(creature: EntityId): Int =
        state.getEntity(creature)?.get<DamageComponent>()?.amount ?: 0

    fun GameTestDriver.view(player: EntityId) =
        ClientStateTransformer(cardRegistry = cardRegistry, predicateEvaluator = PredicateEvaluator(cardRegistry = null))
            .transform(state, viewingPlayerId = player)

    test("only matching sources' damage to the target is prevented") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val warded = driver.putCreatureOnBattlefield(player, "Centaur Courser")
        val other = driver.putCreatureOnBattlefield(player, "Centaur Courser")

        driver.cast(player, "Test Red Ward", Color.WHITE, warded)
        driver.cast(player, "Test Red Blast", Color.RED, warded)
        withClue("a red source's damage to the warded creature is prevented") { driver.damageOn(warded) shouldBe 0 }

        driver.cast(player, "Test Black Blast", Color.BLACK, warded)
        withClue("a black source isn't covered") { driver.damageOn(warded) shouldBe 2 }

        driver.cast(player, "Test Red Blast", Color.RED, other)
        withClue("another creature isn't the recipient") { driver.damageOn(other) shouldBe 2 }
    }

    test("the warded creature's badge names the sources it is shielded from") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val warded = driver.putCreatureOnBattlefield(player, "Centaur Courser")

        driver.cast(player, "Test Red Ward", Color.WHITE, warded)

        val descriptions = driver.view(player).cards[warded]!!.activeEffects.mapNotNull { it.description }
        withClue("$descriptions") {
            descriptions.joinToString() shouldContain "damage to it from red sources is prevented"
        }
    }

    test("a combat-only shield lets matching noncombat damage through") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val warded = driver.putCreatureOnBattlefield(player, "Centaur Courser")

        driver.cast(player, "Test Red Combat Ward", Color.WHITE, warded)
        driver.cast(player, "Test Red Blast", Color.RED, warded)

        driver.damageOn(warded) shouldBe 2
    }

    test("a player recipient is shielded from matching sources and shows it") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        driver.cast(player, "Test Red Ward You", Color.WHITE)
        val life = driver.getLifeTotal(player)
        driver.cast(player, "Test Red Blast", Color.RED, player)
        withClue("red damage to the shielded player is prevented") { driver.getLifeTotal(player) shouldBe life }
        driver.cast(player, "Test Black Blast", Color.BLACK, player)
        withClue("black damage isn't") { driver.getLifeTotal(player) shouldBe life - 2 }

        val badges = driver.view(player).players.single { it.playerId == player }.activeEffects
        withClue("a bare \"Prevent All Damage\" would overstate the shield: ${badges.map { it.effectId }}") {
            badges.any { it.effectId == "prevent_all_damage" } shouldBe false
            badges.single { it.effectId.startsWith("prevent_all_damage_from_") }.description!! shouldContain "by red sources"
        }
    }

    test("a capped matching shield on one target still has no lowering") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val warded = driver.putCreatureOnBattlefield(player, "Centaur Courser")

        driver.cast(player, "Test Capped Red Ward", Color.WHITE, warded)
        driver.cast(player, "Test Red Blast", Color.RED, warded)

        driver.damageOn(warded) shouldBe 2
    }
})
