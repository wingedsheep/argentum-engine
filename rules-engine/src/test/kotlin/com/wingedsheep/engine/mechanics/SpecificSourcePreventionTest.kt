package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.PreventionSourceFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * `PreventionSourceFilter.Specific` — a shield on one recipient covering only the one object an
 * `EffectTarget` names, bound at resolution: "prevent all damage that would be dealt to this
 * creature by target creature this turn" (Stonewise Fortifier). The next-instance reading
 * (`Specific(Self)`, Power Leak) is pinned by PowerLeakScenarioTest.
 *
 * Covered: only that source's damage, only to that recipient, for the whole turn (several
 * instances), gone after cleanup, and not following its source through a zone change;
 * unpreventable damage beats it; a shape with no lowering installs nothing.
 */
class SpecificSourcePreventionTest : FunSpec({

    // "Prevent all damage that would be dealt to target creature by target creature this turn."
    val ward = card("Test Ward") {
        manaCost = "{W}"
        typeLine = "Instant"
        spell {
            val recipient = target(TargetFilter.Creature)
            val source = target(TargetFilter.Creature)
            effect = Effects.PreventDamage(target = recipient, sources = PreventionSourceFilter.Specific(source))
        }
    }
    // No lowering: an amount without nextInstanceOnly.
    val cappedWard = card("Test Capped Ward") {
        manaCost = "{W}"
        typeLine = "Instant"
        spell {
            val recipient = target(TargetFilter.Creature)
            val source = target(TargetFilter.Creature)
            effect = Effects.PreventDamage(
                target = recipient,
                sources = PreventionSourceFilter.Specific(source),
                amount = DynamicAmount.Fixed(1)
            )
        }
    }
    // "Target creature deals 1 damage to target creature."
    val shove = card("Test Shove") {
        manaCost = "{R}"
        typeLine = "Instant"
        spell {
            val source = target(TargetFilter.Creature)
            val victim = target(TargetFilter.Creature)
            effect = Effects.DealDamage(1, victim, damageSource = source)
        }
    }
    val searingShove = card("Test Searing Shove") {
        manaCost = "{R}"
        typeLine = "Instant"
        spell {
            val source = target(TargetFilter.Creature)
            val victim = target(TargetFilter.Creature)
            effect = Effects.DealDamage(1, victim, damageSource = source, cantBePrevented = true)
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + ward + cappedWard + shove + searingShove)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.cast(player: EntityId, name: String, color: Color, targets: List<EntityId>) {
        val id = putCardInHand(player, name)
        giveMana(player, color, 1)
        castSpell(player, id, targets).error shouldBe null
        bothPass()
    }

    fun GameTestDriver.damageOn(creature: EntityId): Int =
        state.getEntity(creature)?.get<DamageComponent>()?.amount ?: 0

    test("the named source's damage to the recipient is prevented, every instance this turn") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val wall = driver.putCreatureOnBattlefield(player, "Centaur Courser")
        val brute = driver.putCreatureOnBattlefield(player, "Centaur Courser")

        driver.cast(player, "Test Ward", Color.WHITE, listOf(wall, brute))
        driver.cast(player, "Test Shove", Color.RED, listOf(brute, wall))
        driver.cast(player, "Test Shove", Color.RED, listOf(brute, wall))

        withClue("the shield is not spent by the first instance") { driver.damageOn(wall) shouldBe 0 }
    }

    test("another source still damages the recipient") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val wall = driver.putCreatureOnBattlefield(player, "Centaur Courser")
        val brute = driver.putCreatureOnBattlefield(player, "Centaur Courser")
        val bystander = driver.putCreatureOnBattlefield(player, "Grizzly Bears")

        driver.cast(player, "Test Ward", Color.WHITE, listOf(wall, brute))
        driver.cast(player, "Test Shove", Color.RED, listOf(bystander, wall))

        driver.damageOn(wall) shouldBe 1
    }

    test("the named source's damage to another recipient is dealt") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val wall = driver.putCreatureOnBattlefield(player, "Centaur Courser")
        val brute = driver.putCreatureOnBattlefield(player, "Centaur Courser")
        val bystander = driver.putCreatureOnBattlefield(player, "Grizzly Bears")

        driver.cast(player, "Test Ward", Color.WHITE, listOf(wall, brute))
        driver.cast(player, "Test Shove", Color.RED, listOf(brute, bystander))

        driver.damageOn(bystander) shouldBe 1
    }

    test("the shield ends with the turn") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        val wall = driver.putCreatureOnBattlefield(player, "Centaur Courser")
        val brute = driver.putCreatureOnBattlefield(player, "Centaur Courser")

        driver.cast(player, "Test Ward", Color.WHITE, listOf(wall, brute))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, activePlayer = opponent)
        driver.cast(opponent, "Test Shove", Color.RED, listOf(brute, wall))

        driver.damageOn(wall) shouldBe 1
    }

    test("a source that leaves and returns is a new object the shield doesn't cover (CR 400.7)") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val wall = driver.putCreatureOnBattlefield(player, "Centaur Courser")
        val brute = driver.putCreatureOnBattlefield(player, "Centaur Courser")

        driver.cast(player, "Test Ward", Color.WHITE, listOf(wall, brute))
        val exiled = driver.zones.moveToZone(driver.state, brute, Zone.EXILE).state
        driver.replaceState(driver.zones.moveToZone(exiled, brute, Zone.BATTLEFIELD).state)
        withClue("the flickered creature is back") { driver.state.getBattlefield().contains(brute) shouldBe true }
        driver.cast(player, "Test Shove", Color.RED, listOf(brute, wall))

        driver.damageOn(wall) shouldBe 1
    }

    test("damage that can't be prevented gets through") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val wall = driver.putCreatureOnBattlefield(player, "Centaur Courser")
        val brute = driver.putCreatureOnBattlefield(player, "Centaur Courser")

        driver.cast(player, "Test Ward", Color.WHITE, listOf(wall, brute))
        driver.cast(player, "Test Searing Shove", Color.RED, listOf(brute, wall))

        driver.damageOn(wall) shouldBe 1
    }

    test("a shape with no lowering installs no shield") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val wall = driver.putCreatureOnBattlefield(player, "Centaur Courser")
        val brute = driver.putCreatureOnBattlefield(player, "Centaur Courser")

        val id = driver.putCardInHand(player, "Test Capped Ward")
        driver.giveMana(player, Color.WHITE, 1)
        driver.castSpell(player, id, listOf(wall, brute)).error shouldBe null
        driver.bothPass()
        driver.cast(player, "Test Shove", Color.RED, listOf(brute, wall))

        driver.damageOn(wall) shouldBe 1
    }
})
