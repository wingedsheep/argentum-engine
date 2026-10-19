package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.PreventionSourceFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * `PreventionSourceFilter.TargetingRecipient` — "if a spell or ability that targets that creature
 * would cause a source to deal damage to that creature this turn, prevent that damage" (Silhouette).
 *
 * The shield keys on the damage's cause (the resolving spell or ability and its targets), not on
 * the source: damage the targeting spell deals to the recipient through untargeted wording is
 * still prevented, while the same spell's damage to other creatures isn't. Unpreventable damage
 * beats it, and a shape it can't lower installs nothing.
 */
class TargetingRecipientPreventionTest : FunSpec({

    val shield = card("Test Silhouette") {
        manaCost = "{U}"
        typeLine = "Instant"
        spell {
            val creature = target(TargetFilter.Creature)
            effect = Effects.PreventDamage(target = creature, sources = PreventionSourceFilter.TargetingRecipient)
        }
    }
    // A shape with no lowering: a TargetingRecipient shield capped at an amount.
    val cappedShield = card("Test Capped Silhouette") {
        manaCost = "{U}"
        typeLine = "Instant"
        spell {
            val creature = target(TargetFilter.Creature)
            effect = Effects.PreventDamage(
                target = creature,
                sources = PreventionSourceFilter.TargetingRecipient,
                amount = DynamicAmount.Fixed(2)
            )
        }
    }
    // Targets one creature, then hurts every creature without naming a target for the damage.
    val markedBlast = card("Test Marked Blast") {
        manaCost = "{R}"
        typeLine = "Instant"
        spell {
            target(TargetFilter.Creature)
            effect = Effects.ForEachInGroup(GroupFilter(GameObjectFilter.Creature), Effects.DealDamage(3, EffectTarget.IterationEntity))
        }
    }
    val searingShot = card("Test Searing Shot") {
        manaCost = "{R}"
        typeLine = "Instant"
        spell {
            val creature = target(TargetFilter.Creature)
            effect = Effects.DealDamage(3, creature, cantBePrevented = true)
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + shield + cappedShield + markedBlast + searingShot)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.cast(player: EntityId, name: String, color: Color, target: EntityId) {
        val id = putCardInHand(player, name)
        giveMana(player, color, 1)
        castSpell(player, id, listOf(target)).error shouldBe null
        bothPass()
    }

    fun GameTestDriver.damageOn(creature: EntityId): Int =
        state.getEntity(creature)?.get<DamageComponent>()?.amount ?: 0

    test("a targeting spell's untargeted damage to the recipient is prevented; to others it is dealt") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val chosen = driver.putCreatureOnBattlefield(player, "Centaur Courser")
        val other = driver.putCreatureOnBattlefield(player, "Centaur Courser")

        driver.cast(player, "Test Silhouette", Color.BLUE, chosen)
        driver.cast(player, "Test Marked Blast", Color.RED, chosen)

        withClue("the spell targets the chosen creature, so all its damage to it is prevented") {
            driver.damageOn(chosen) shouldBe 0
        }
        withClue("the other creature isn't the shield's recipient") {
            driver.findPermanent(player, "Centaur Courser") shouldBe chosen
            driver.state.getBattlefield().contains(other) shouldBe false
        }
    }

    test("a targeting spell aimed elsewhere doesn't cover the recipient") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val chosen = driver.putCreatureOnBattlefield(player, "Centaur Courser")
        val other = driver.putCreatureOnBattlefield(player, "Centaur Courser")

        driver.cast(player, "Test Silhouette", Color.BLUE, chosen)
        driver.cast(player, "Test Marked Blast", Color.RED, other)

        withClue("Marked Blast targets the other creature, so the chosen one takes its damage") {
            driver.state.getBattlefield().contains(chosen) shouldBe false
        }
    }

    test("damage that can't be prevented gets through") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val chosen = driver.putCreatureOnBattlefield(player, "Centaur Courser")

        driver.cast(player, "Test Silhouette", Color.BLUE, chosen)
        driver.cast(player, "Test Searing Shot", Color.RED, chosen)

        driver.state.getBattlefield().contains(chosen) shouldBe false
    }

    test("a shape with no lowering installs no shield") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val chosen = driver.putCreatureOnBattlefield(player, "Centaur Courser")

        driver.cast(player, "Test Capped Silhouette", Color.BLUE, chosen)

        driver.state.floatingEffects.none {
            it.effect.modification is SerializableModification.PreventAllDamageTo
        } shouldBe true
    }

    test("the shield is a prevent-all modification flagged for targeting causes") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val chosen = driver.putCreatureOnBattlefield(player, "Centaur Courser")

        driver.cast(player, "Test Silhouette", Color.BLUE, chosen)

        val mod = driver.state.floatingEffects.single { chosen in it.effect.affectedEntities }.effect.modification
        mod shouldBe SerializableModification.PreventAllDamageTo(onlyCausedByObjectTargetingIt = true)
    }
})
