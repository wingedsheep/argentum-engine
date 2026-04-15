package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SpellFizzledEvent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Regression test for Rule 608.2b as it applies to hexproof granted by an Aura's
 * enters-the-battlefield trigger (e.g. Aquitect's Defenses).
 *
 * Flow under test:
 *   1. P2 casts a "destroy target creature" spell on P1's creature.
 *   2. P1 flashes in an Aura on the same creature.
 *   3. Aura resolves → ETB trigger grants the creature hexproof until end of turn.
 *   4. Destroy spell resolves → target is now illegal (hexproof vs. opponent) →
 *      spell is countered on resolution and produces no effects.
 */
class AuraEntersGrantsHexproofTest : FunSpec({

    // Minimal Aura: flash, enchant creature, ETB gives enchanted creature hexproof EOT.
    val HexproofAura = card("Test Hexproof Aura") {
        manaCost = "{1}{U}"
        typeLine = "Enchantment — Aura"

        keywords(Keyword.FLASH)
        auraTarget = Targets.Creature

        triggeredAbility {
            trigger = Triggers.EntersBattlefield
            effect = Effects.GrantHexproof(EffectTarget.EnchantedCreature)
        }
    }

    // Minimal "destroy target creature" instant.
    val DestroyTargetCreature = card("Test Doom Blade") {
        manaCost = "{1}{B}"
        typeLine = "Instant"

        spell {
            val t = target("target creature", TargetCreature())
            effect = MoveToZoneEffect(t, Zone.GRAVEYARD, byDestruction = true)
        }
    }

    test("destroy fizzles when Aura's ETB grants the targeted creature hexproof (Rule 608.2b)") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(HexproofAura, DestroyTargetCreature))
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Swamp" to 20),
            startingLife = 20
        )

        val p1 = driver.activePlayer!!
        val p2 = driver.getOpponent(p1)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // P1 controls a creature.
        val creature = driver.putCreatureOnBattlefield(p1, "Grizzly Bears")
        driver.removeSummoningSickness(creature)

        // P1 passes priority so P2 can cast at instant speed.
        driver.passPriority(p1)

        // P2 casts "destroy target creature" on P1's creature.
        val removal = driver.putCardInHand(p2, "Test Doom Blade")
        driver.giveMana(p2, Color.BLACK, 2)
        driver.castSpellWithTargets(p2, removal, listOf(ChosenTarget.Permanent(creature)))
            .isSuccess shouldBe true

        // P2 passes → P1 gets priority, responds with the flash Aura on own creature.
        driver.passPriority(p2)
        val aura = driver.putCardInHand(p1, "Test Hexproof Aura")
        driver.giveMana(p1, Color.BLUE, 2)
        driver.castSpellWithTargets(p1, aura, listOf(ChosenTarget.Permanent(creature)))
            .isSuccess shouldBe true

        // Stack: [destroy (bottom), aura (top)]. Both pass → aura resolves first.
        driver.stackSize shouldBe 2
        driver.bothPass()

        // Aura ETB trigger is now on the stack. Both pass → it resolves, granting hexproof.
        driver.bothPass()

        // Both pass → destroy tries to resolve, should fizzle because target has hexproof
        // relative to its controller (P2).
        driver.bothPass()

        // The creature should still be on the battlefield.
        (creature in driver.state.getBattlefield()) shouldBe true

        // A SpellFizzledEvent should have been emitted for the destroy spell.
        driver.events.any { it is SpellFizzledEvent } shouldBe true
    }
})
