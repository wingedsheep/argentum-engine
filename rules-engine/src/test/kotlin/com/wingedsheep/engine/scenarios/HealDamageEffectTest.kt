package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ReplaceDestructionWith
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * [com.wingedsheep.sdk.scripting.effects.HealDamageEffect] — remove all damage marked on a permanent
 * now. As a one-shot spell effect it clears the damage without touching anything else; as the body of
 * a destruction replacement ("instead all damage already dealt to it is healed") the permanent
 * survives lethal damage, isn't tapped (it isn't regeneration), and the heal happens before the next
 * state-based-action check could see the damage again.
 */
class HealDamageEffectTest : ScenarioTestBase() {

    private val testMending = card("Test Mending") {
        manaCost = "{W}"
        typeLine = "Instant"
        oracleText = "Remove all damage from target creature."
        spell {
            val creature = target(TargetFilter.Creature)
            effect = Effects.HealDamage(creature)
        }
        metadata { rarity = Rarity.COMMON }
    }

    private val testSurgeonsWard = card("Test Surgeon's Ward") {
        manaCost = "{W}"
        typeLine = "Enchantment — Aura"
        oracleText = "Enchant creature\nIf enchanted creature would be destroyed, instead all damage " +
            "already dealt to it is healed."
        auraTarget = TargetObject(filter = TargetFilter.Creature)
        replacementEffect(
            ReplaceDestructionWith(
                replacementEffect = Effects.HealDamage(EffectTarget.TriggeringEntity),
                appliesTo = EventPattern.DestroyEvent(GameObjectFilter.Creature.attachedToBySource())
            )
        )
        metadata { rarity = Rarity.COMMON }
    }

    private fun TestGame.damageOn(id: EntityId): Int = state.getEntity(id)?.get<DamageComponent>()?.amount ?: 0

    init {
        cardRegistry.register(testMending)
        cardRegistry.register(testSurgeonsWard)

        test("a one-shot heal removes all marked damage and nothing else") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardInHand(1, "Shock")
                .withCardInHand(1, "Test Mending")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withLandsOnBattlefield(1, "Plains", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val giant = game.findPermanent("Hill Giant")!!

            game.castSpell(1, "Shock", giant).error shouldBe null
            game.resolveStack()
            game.damageOn(giant) shouldBe 2

            game.castSpell(1, "Test Mending", giant).error shouldBe null
            game.resolveStack()

            game.damageOn(giant) shouldBe 0
            game.isOnBattlefield("Hill Giant") shouldBe true
        }

        test("healing a creature with no damage is a no-op that still resolves") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardInHand(1, "Test Mending")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val giant = game.findPermanent("Hill Giant")!!

            game.castSpell(1, "Test Mending", giant).error shouldBe null
            game.resolveStack()

            game.damageOn(giant) shouldBe 0
            game.isInGraveyard(1, "Test Mending") shouldBe true
        }

        test("as a destruction replacement, lethal damage is healed and the creature survives untapped") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardAttachedTo(1, "Test Surgeon's Ward", "Hill Giant")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val giant = game.findPermanent("Hill Giant")!!

            game.castSpell(1, "Lightning Bolt", giant).error shouldBe null
            game.resolveStack()

            withClue("the lethal-damage destruction is replaced") { game.isOnBattlefield("Hill Giant") shouldBe true }
            withClue("and the damage is gone, so the next SBA pass doesn't loop") { game.damageOn(giant) shouldBe 0 }
            withClue("not regeneration: the creature isn't tapped") {
                game.state.getEntity(giant)?.has<TappedComponent>() shouldBe false
            }
            game.state.pendingReplacementRiders shouldBe emptyList()
        }

        test("as a destruction replacement, a destroy effect is replaced too") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardAttachedTo(1, "Test Surgeon's Ward", "Hill Giant")
                .withCardInHand(1, "Doom Blade")
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Doom Blade", game.findPermanent("Hill Giant")!!).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Hill Giant") shouldBe true
        }
    }
}
