package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Umbra armor (CR 702.89a): "If enchanted permanent would be destroyed, instead remove all damage
 * marked on it and destroy this Aura." Each test pins one of the rulings shared by every umbra
 * armor card (Dog Umbra, Lion Umbra, the Zendikar-block totem armor Auras).
 */
class UmbraArmorTest : ScenarioTestBase() {

    private val testUmbra = card("Test Umbra") {
        manaCost = "{W}"
        typeLine = "Enchantment — Aura"
        oracleText = "Enchant creature\nUmbra armor"
        keywords(Keyword.UMBRA_ARMOR)
        auraTarget = TargetObject(filter = TargetFilter.Creature)
    }

    private val testSecondUmbra = card("Test Second Umbra") {
        manaCost = "{W}"
        typeLine = "Enchantment — Aura"
        oracleText = "Enchant creature\nUmbra armor"
        keywords(Keyword.UMBRA_ARMOR)
        auraTarget = TargetObject(filter = TargetFilter.Creature)
    }

    private val testIroncladUmbra = card("Test Ironclad Umbra") {
        manaCost = "{W}"
        typeLine = "Enchantment — Aura"
        oracleText = "Enchant creature\nIndestructible\nUmbra armor"
        keywords(Keyword.UMBRA_ARMOR, Keyword.INDESTRUCTIBLE)
        auraTarget = TargetObject(filter = TargetFilter.Creature)
    }

    private val testWrath = card("Test Wrath") {
        manaCost = "{W}"
        typeLine = "Sorcery"
        oracleText = "Destroy all creatures. They can't be regenerated."
        spell { effect = Effects.DestroyAll(GameObjectFilter.Creature, noRegenerate = true) }
    }

    private val testPurge = card("Test Purge") {
        manaCost = "{W}"
        typeLine = "Sorcery"
        oracleText = "Destroy all creatures and enchantments."
        spell { effect = Effects.DestroyAll(GameObjectFilter.Creature or GameObjectFilter.Enchantment) }
    }

    private fun baseScenario() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Centaur Courser")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    private fun TestGame.courser() = findPermanent("Centaur Courser")!!

    init {
        cardRegistry.register(testUmbra)
        cardRegistry.register(testSecondUmbra)
        cardRegistry.register(testIroncladUmbra)
        cardRegistry.register(testWrath)
        cardRegistry.register(testPurge)

        test("a destroy effect destroys the Aura instead; the creature is neither tapped nor harmed") {
            val game = baseScenario()
                .withCardAttachedTo(1, "Test Umbra", "Centaur Courser")
                .withCardInHand(1, "Doom Blade")
                .withLandsOnBattlefield(1, "Swamp", 2)
                .build()

            game.castSpell(1, "Doom Blade", game.courser()).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Centaur Courser") shouldBe true
            game.isInGraveyard(1, "Test Umbra") shouldBe true
            withClue("umbra armor is not regeneration — no tap") {
                game.state.getEntity(game.courser())!!.has<TappedComponent>() shouldBe false
            }
        }

        test("lethal damage: all damage is removed and the Aura is destroyed by the state-based action") {
            val game = baseScenario()
                .withCardAttachedTo(1, "Test Umbra", "Centaur Courser")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .build()

            game.castSpell(1, "Lightning Bolt", game.courser()).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Centaur Courser") shouldBe true
            game.isInGraveyard(1, "Test Umbra") shouldBe true
            game.state.getEntity(game.courser())!!.get<DamageComponent>() shouldBe null
        }

        test("lethal deathtouch damage is replaced once, spending a single Aura") {
            val game = baseScenario()
                .withCardAttachedTo(1, "Test Umbra", "Centaur Courser")
                .build()
            game.state = game.state.updateEntity(game.courser()) {
                it.with(DamageComponent(amount = 5, deathtouchDamageReceived = true))
            }

            game.checkStateBasedActions()

            game.isOnBattlefield("Centaur Courser") shouldBe true
            game.isInGraveyard(1, "Test Umbra") shouldBe true
            game.state.getEntity(game.courser())!!.get<DamageComponent>() shouldBe null
        }

        test("\"can't be regenerated\" does not stop umbra armor") {
            val game = baseScenario()
                .withCardAttachedTo(1, "Test Umbra", "Centaur Courser")
                .withCardInHand(1, "Test Wrath")
                .withLandsOnBattlefield(1, "Plains", 1)
                .build()

            game.castSpell(1, "Test Wrath").error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Centaur Courser") shouldBe true
            game.isInGraveyard(1, "Test Umbra") shouldBe true
        }

        for (auraFirst in listOf(false, true)) {
            test("a wipe destroying both the Aura and its creature still saves the creature (Aura first: $auraFirst)") {
                val game = baseScenario()
                    .withCardAttachedTo(1, "Test Umbra", "Centaur Courser")
                    .withCardInHand(1, "Test Purge")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .build()
                if (auraFirst) {
                    // Put the Aura ahead of its host so the wipe visits it first.
                    val key = ZoneKey(game.state.activePlayerId!!, Zone.BATTLEFIELD)
                    val umbra = game.findPermanent("Test Umbra")!!
                    val order = game.state.zones[key]!!
                    game.state = game.state.copy(zones = game.state.zones + (key to (listOf(umbra) + (order - umbra))))
                }

                game.castSpell(1, "Test Purge").error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Centaur Courser") shouldBe true
                game.findCardsInGraveyard(1, "Test Umbra").size shouldBe 1
            }
        }

        test("with two umbra armor Auras only one is destroyed") {
            val game = baseScenario()
                .withCardAttachedTo(1, "Test Umbra", "Centaur Courser")
                .withCardAttachedTo(1, "Test Second Umbra", "Centaur Courser")
                .withCardInHand(1, "Doom Blade")
                .withLandsOnBattlefield(1, "Swamp", 2)
                .build()

            game.castSpell(1, "Doom Blade", game.courser()).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Centaur Courser") shouldBe true
            val spent = game.findCardsInGraveyard(1, "Test Umbra").size +
                game.findCardsInGraveyard(1, "Test Second Umbra").size
            spent shouldBe 1
            (game.isOnBattlefield("Test Umbra") || game.isOnBattlefield("Test Second Umbra")) shouldBe true
        }

        test("an indestructible umbra still saves the creature and survives its own destruction") {
            val game = baseScenario()
                .withCardAttachedTo(1, "Test Ironclad Umbra", "Centaur Courser")
                .withCardInHand(1, "Doom Blade")
                .withLandsOnBattlefield(1, "Swamp", 2)
                .build()

            game.castSpell(1, "Doom Blade", game.courser()).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Centaur Courser") shouldBe true
            game.isOnBattlefield("Test Ironclad Umbra") shouldBe true
        }

        test("an attacking creature saved by umbra armor stays in combat") {
            val game = baseScenario()
                .withCardAttachedTo(1, "Test Umbra", "Centaur Courser")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .build()
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Centaur Courser" to 2)).error shouldBe null

            game.castSpell(1, "Lightning Bolt", game.courser()).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Test Umbra") shouldBe true
            game.state.getEntity(game.courser())!!.get<AttackingComponent>() shouldNotBe null
        }

        test("a creature with no umbra armor Aura is destroyed normally") {
            val game = baseScenario()
                .withCardInHand(1, "Doom Blade")
                .withLandsOnBattlefield(1, "Swamp", 2)
                .build()

            game.castSpell(1, "Doom Blade", game.courser()).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Centaur Courser") shouldBe true
        }
    }
}
