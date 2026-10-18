package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.LoseAllAbilities
import com.wingedsheep.sdk.scripting.RegenerateInsteadOfDestruction
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * [RegenerateInsteadOfDestruction] — "If this creature would be destroyed, regenerate it": static
 * regeneration (CR 701.19b), which replaces destruction each time rather than once.
 *
 * Pins: every destruction is replaced (destroy effects, lethal damage, deathtouch, board wipes) and
 * nothing is spent; it is regeneration, so "can't be regenerated" — an effect or a Wrath — stops it;
 * it is one of the host's abilities; a banked shield is kept; a sacrifice isn't a destruction; and a
 * non-self `appliesTo` regenerates other permanents.
 */
class RegenerateInsteadOfDestructionTest : ScenarioTestBase() {

    private val testTroll = card("Test Undying Troll") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Troll"
        oracleText = "If this creature would be destroyed, regenerate it."
        power = 2
        toughness = 2
        replacementEffect(RegenerateInsteadOfDestruction())
        metadata { rarity = Rarity.COMMON }
    }

    private val testSanctuary = card("Test Regeneration Sanctuary") {
        manaCost = "{2}{G}"
        typeLine = "Enchantment"
        oracleText = "If a creature you control would be destroyed, regenerate it."
        replacementEffect(RegenerateInsteadOfDestruction(EventPattern.DestroyEvent(GameObjectFilter.Creature.youControl())))
        metadata { rarity = Rarity.COMMON }
    }

    private val testWipe = card("Test Wipe") {
        manaCost = "{W}"
        typeLine = "Sorcery"
        oracleText = "Destroy all creatures."
        spell { effect = Effects.DestroyAll(GameObjectFilter.Creature) }
    }

    private val testWrath = card("Test Wrath") {
        manaCost = "{W}"
        typeLine = "Sorcery"
        oracleText = "Destroy all creatures. They can't be regenerated."
        spell { effect = Effects.DestroyAll(GameObjectFilter.Creature, noRegenerate = true) }
    }

    private val testCurse = card("Test Unregenerating Curse") {
        manaCost = "{B}"
        typeLine = "Instant"
        oracleText = "Target creature can't be regenerated this turn."
        spell {
            target = TargetObject(filter = TargetFilter.Creature)
            effect = Effects.CantBeRegenerated(EffectTarget.ContextTarget(0))
        }
    }

    private val testShield = card("Test Shield") {
        manaCost = "{W}"
        typeLine = "Instant"
        oracleText = "Regenerate target creature."
        spell {
            target = TargetObject(filter = TargetFilter.Creature)
            effect = Effects.Regenerate(EffectTarget.ContextTarget(0))
        }
    }

    private val testOffering = card("Test Offering") {
        manaCost = "{B}"
        typeLine = "Sorcery"
        oracleText = "Sacrifice a creature."
        spell { effect = Effects.SacrificeOwn(GameObjectFilter.Creature) }
    }

    private val testSilence = card("Test Creature Silence") {
        manaCost = "{U}"
        typeLine = "Enchantment"
        oracleText = "Creatures lose all abilities."
        staticAbility { ability = LoseAllAbilities(GroupFilter(GameObjectFilter.Creature)) }
    }

    private fun board(vararg extra: Pair<Int, String>) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Test Undying Troll")
        .withLandsOnBattlefield(1, "Swamp", 4)
        .withLandsOnBattlefield(1, "Plains", 2)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .let { b -> extra.fold(b) { acc, (p, name) -> acc.withCardInHand(p, name) } }
        .build()

    private fun TestGame.troll() = findPermanent("Test Undying Troll")!!

    private fun TestGame.assertRegenerated() {
        isOnBattlefield("Test Undying Troll") shouldBe true
        withClue("regenerating taps it") { state.getEntity(troll())!!.has<TappedComponent>() shouldBe true }
        withClue("regenerating removes marked damage") {
            (state.getEntity(troll())!!.get<DamageComponent>()?.amount ?: 0) shouldBe 0
        }
    }

    init {
        listOf(testTroll, testSanctuary, testWipe, testWrath, testCurse, testShield, testOffering, testSilence)
            .forEach(cardRegistry::register)

        test("a destroy effect is replaced every time — nothing is spent") {
            val game = board(1 to "Doom Blade", 1 to "Doom Blade")
            repeat(2) {
                game.castSpell(1, "Doom Blade", game.troll()).error shouldBe null
                game.resolveStack()
                game.assertRegenerated()
            }
        }

        test("lethal damage regenerates in the state-based-action pass") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Test Undying Troll")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.castSpell(1, "Lightning Bolt", game.troll()).error shouldBe null
            game.resolveStack()
            game.assertRegenerated()
        }

        test("deathtouch damage is a destruction too") {
            val game = board()
            game.state = game.state.updateEntity(game.troll()) {
                it.with(DamageComponent(amount = 1, deathtouchDamageReceived = true))
            }
            game.checkStateBasedActions()
            game.assertRegenerated()
        }

        test("a board wipe is replaced") {
            val game = board(1 to "Test Wipe")
            game.castSpell(1, "Test Wipe").error shouldBe null
            game.resolveStack()
            game.assertRegenerated()
        }

        test("a wipe that says they can't be regenerated destroys it") {
            val game = board(1 to "Test Wrath")
            game.castSpell(1, "Test Wrath").error shouldBe null
            game.resolveStack()
            game.isInGraveyard(1, "Test Undying Troll") shouldBe true
        }

        test("\"can't be regenerated this turn\" switches it off") {
            val game = board(1 to "Test Unregenerating Curse", 1 to "Doom Blade")
            game.castSpell(1, "Test Unregenerating Curse", game.troll()).error shouldBe null
            game.resolveStack()
            game.castSpell(1, "Doom Blade", game.troll()).error shouldBe null
            game.resolveStack()
            game.isInGraveyard(1, "Test Undying Troll") shouldBe true
        }

        test("a banked regeneration shield is kept for later") {
            val game = board(1 to "Test Shield", 1 to "Doom Blade")
            game.castSpell(1, "Test Shield", game.troll()).error shouldBe null
            game.resolveStack()
            game.castSpell(1, "Doom Blade", game.troll()).error shouldBe null
            game.resolveStack()
            game.assertRegenerated()
            game.state.floatingEffects.count {
                it.effect.modification is SerializableModification.RegenerationShield && game.troll() in it.effect.affectedEntities
            } shouldBe 1
        }

        test("a sacrifice is not a destruction") {
            val game = board(1 to "Test Offering")
            game.castSpell(1, "Test Offering").error shouldBe null
            game.resolveStack()
            game.isInGraveyard(1, "Test Undying Troll") shouldBe true
        }

        test("a creature that lost all abilities no longer regenerates") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Test Undying Troll")
                .withCardOnBattlefield(2, "Test Creature Silence")
                .withCardInHand(1, "Doom Blade")
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.castSpell(1, "Doom Blade", game.troll()).error shouldBe null
            game.resolveStack()
            game.isInGraveyard(1, "Test Undying Troll") shouldBe true
        }

        test("appliesTo reaches other permanents, read relative to the host") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Test Regeneration Sanctuary")
                .withCardOnBattlefield(1, "Centaur Courser")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInHand(1, "Test Wipe")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.castSpell(1, "Test Wipe").error shouldBe null
            game.resolveStack()
            withClue("the Sanctuary's controller's creature regenerates") {
                game.isOnBattlefield("Centaur Courser") shouldBe true
            }
            withClue("the opponent's creature is destroyed") {
                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            }
        }
    }
}
