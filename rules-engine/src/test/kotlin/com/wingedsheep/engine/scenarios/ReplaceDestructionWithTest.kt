package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.LoseAllAbilities
import com.wingedsheep.sdk.scripting.ReplaceDestructionWith
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * [ReplaceDestructionWith] — "If enchanted permanent would be destroyed, instead sacrifice this Aura
 * and that permanent gains indestructible until end of turn" (Crackling Emergence's shape, on a
 * creature Aura so the engine's own test cards can carry it).
 *
 * Pins the rulings: it replaces every destruction (destroy effects, board wipes, the lethal-damage and
 * deathtouch state-based actions) but not a sacrifice, and the rest of the replacement happens before
 * the next state-based-action pass looks at the permanent.
 */
class ReplaceDestructionWithTest : ScenarioTestBase() {

    private val testEmergence = card("Test Emergence") {
        manaCost = "{G}"
        typeLine = "Enchantment — Aura"
        oracleText = "Enchant creature\nIf enchanted creature would be destroyed, instead sacrifice this " +
            "Aura and that creature gains indestructible until end of turn."
        auraTarget = TargetObject(filter = TargetFilter.Creature)
        replacementEffect(
            ReplaceDestructionWith(
                replacementEffect = SacrificeSelfEffect then
                    Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, EffectTarget.TriggeringEntity),
                appliesTo = EventPattern.DestroyEvent(GameObjectFilter.Creature.attachedToBySource())
            )
        )
        metadata { rarity = Rarity.COMMON }
    }

    private val testPurge = card("Test Purge") {
        manaCost = "{W}"
        typeLine = "Sorcery"
        oracleText = "Destroy all creatures and enchantments."
        spell { effect = Effects.DestroyAll(GameObjectFilter.Creature or GameObjectFilter.Enchantment) }
    }

    private val testOffering = card("Test Offering") {
        manaCost = "{B}"
        typeLine = "Sorcery"
        oracleText = "Sacrifice a creature."
        spell { effect = Effects.SacrificeOwn(GameObjectFilter.Creature) }
    }

    private val testWall = card("Test Iron Wall") {
        manaCost = "{1}"
        typeLine = "Artifact Creature — Wall"
        oracleText = "Indestructible"
        power = 0
        toughness = 4
        keywords(Keyword.INDESTRUCTIBLE)
    }

    private val testSilence = card("Test Enchantment Silence") {
        manaCost = "{U}"
        typeLine = "Enchantment"
        oracleText = "Auras lose all abilities."
        staticAbility {
            ability = LoseAllAbilities(GroupFilter(GameObjectFilter.Enchantment.withSubtype("Aura")))
        }
    }

    private fun baseScenario() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Centaur Courser")
        .withCardAttachedTo(1, "Test Emergence", "Centaur Courser")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    private fun TestGame.courser() = findPermanent("Centaur Courser")!!

    private fun TestGame.assertSaved() {
        isOnBattlefield("Centaur Courser") shouldBe true
        isInGraveyard(1, "Test Emergence") shouldBe true
        withClue("that creature gains indestructible") {
            state.projectedState.hasKeyword(courser(), Keyword.INDESTRUCTIBLE) shouldBe true
        }
        state.pendingReplacementRiders shouldBe emptyList()
    }

    init {
        cardRegistry.register(testEmergence)
        cardRegistry.register(testPurge)
        cardRegistry.register(testOffering)
        cardRegistry.register(testWall)
        cardRegistry.register(testSilence)

        test("a destroy effect is replaced: the Aura is sacrificed and the creature gains indestructible") {
            val game = baseScenario()
                .withCardInHand(1, "Doom Blade")
                .withLandsOnBattlefield(1, "Swamp", 2)
                .build()

            game.castSpell(1, "Doom Blade", game.courser()).error shouldBe null
            game.resolveStack()

            game.assertSaved()
        }

        test("lethal damage is replaced inside the state-based-action pass, and the damage stays marked") {
            val game = baseScenario()
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .build()

            game.castSpell(1, "Lightning Bolt", game.courser()).error shouldBe null
            game.resolveStack()

            game.assertSaved()
            game.state.gameOver shouldBe false
            withClue("unlike umbra armor, nothing removes the damage — indestructible is what keeps it alive") {
                game.state.getEntity(game.courser())!!.get<DamageComponent>()?.amount shouldBe 3
            }
        }

        test("deathtouch damage is a destruction too") {
            val game = baseScenario().build()
            game.state = game.state.updateEntity(game.courser()) {
                it.with(DamageComponent(amount = 1, deathtouchDamageReceived = true))
            }

            game.checkStateBasedActions()

            game.assertSaved()
        }

        test("a board wipe that also destroys the Aura still saves the creature") {
            val game = baseScenario()
                .withCardInHand(1, "Test Purge")
                .withLandsOnBattlefield(1, "Plains", 1)
                .build()

            game.castSpell(1, "Test Purge").error shouldBe null
            game.resolveStack()

            game.assertSaved()
        }

        test("a sacrifice is not a destruction and is not replaced") {
            val game = baseScenario()
                .withCardInHand(1, "Test Offering")
                .withLandsOnBattlefield(1, "Swamp", 1)
                .build()

            game.castSpell(1, "Test Offering").error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Centaur Courser") shouldBe true
        }

        test("an indestructible permanent is never destroyed, so the Aura is not spent") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Test Iron Wall")
                .withCardAttachedTo(1, "Test Emergence", "Test Iron Wall")
                .withCardInHand(1, "Doom Blade")
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Doom Blade", game.findPermanent("Test Iron Wall")!!).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Test Iron Wall") shouldBe true
            game.isOnBattlefield("Test Emergence") shouldBe true
        }

        test("an Aura that has lost all its abilities no longer replaces the destruction") {
            val game = baseScenario()
                .withCardOnBattlefield(1, "Test Enchantment Silence")
                .withCardInHand(1, "Doom Blade")
                .withLandsOnBattlefield(1, "Swamp", 2)
                .build()

            game.castSpell(1, "Doom Blade", game.courser()).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Centaur Courser") shouldBe true
        }
    }
}
