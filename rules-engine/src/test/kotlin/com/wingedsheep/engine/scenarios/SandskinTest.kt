package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.battlefield.ReplacementEffectSourceComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.onslaught.cards.Sandskin
import com.wingedsheep.sdk.scripting.PreventDamage
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Sandskin (ONS #52).
 *
 * Sandskin: {2}{W}
 * Enchantment — Aura
 * Enchant creature
 * Prevent all combat damage that would be dealt to and dealt by enchanted creature.
 */
class SandskinTest : FunSpec({

    // A sturdy creature that survives 3 damage (for damage verification)
    val SturdyCreature = CardDefinition.creature(
        name = "Sturdy Creature",
        manaCost = ManaCost.parse("{2}{G}"),
        subtypes = setOf(Subtype("Beast")),
        power = 3,
        toughness = 5,
        oracleText = ""
    )

    /**
     * Put an aura on the battlefield attached to a creature.
     */
    fun GameTestDriver.putAuraOnBattlefield(
        playerId: EntityId,
        cardDef: CardDefinition,
        targetCreatureId: EntityId
    ): EntityId {
        val auraId = EntityId.generate()

        val cardComponent = CardComponent(
            cardDefinitionId = cardDef.name,
            name = cardDef.name,
            manaCost = cardDef.manaCost,
            typeLine = cardDef.typeLine,
            oracleText = cardDef.oracleText,
            baseStats = cardDef.creatureStats,
            baseKeywords = cardDef.keywords,
            baseFlags = cardDef.flags,
            colors = cardDef.colors,
            ownerId = playerId,
            spellEffect = cardDef.spellEffect
        )

        // Filter replacement effects that need runtime components
        val runtimeEffects = cardDef.script.replacementEffects.filter {
            it is PreventDamage
        }

        var container = ComponentContainer.of(
            cardComponent,
            OwnerComponent(playerId),
            ControllerComponent(playerId),
            AttachedToComponent(targetCreatureId)
        )

        if (runtimeEffects.isNotEmpty()) {
            container = container.with(ReplacementEffectSourceComponent(runtimeEffects))
        }

        var newState = state.withEntity(auraId, container)

        // Add to battlefield
        val battlefieldZone = ZoneKey(playerId, Zone.BATTLEFIELD)
        newState = newState.addToZone(battlefieldZone, auraId)

        // Update the target creature's attachments
        val existingAttachments = newState.getEntity(targetCreatureId)
            ?.get<AttachmentsComponent>()?.attachedIds ?: emptyList()
        newState = newState.updateEntity(targetCreatureId) { c ->
            c.with(AttachmentsComponent(existingAttachments + auraId))
        }

        replaceState(newState)
        return auraId
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(SturdyCreature))
        return driver
    }

    test("prevents combat damage TO enchanted creature") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a 3/5 creature on our battlefield with Sandskin
        val creature = driver.putCreatureOnBattlefield(activePlayer, "Sturdy Creature")
        driver.removeSummoningSickness(creature)
        driver.putAuraOnBattlefield(activePlayer, Sandskin, creature)

        // Opponent has a 3/3 creature
        val opponentCreature = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")
        driver.removeSummoningSickness(opponentCreature)

        // We attack, opponent blocks
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(activePlayer, listOf(creature), opponent)
        driver.bothPass()
        driver.declareBlockers(opponent, mapOf(opponentCreature to listOf(creature)))
        driver.bothPass()

        // First strike step (no first strikers)
        driver.bothPass()

        // Combat damage - our creature should take 0 damage (prevented by Sandskin)
        driver.bothPass()

        val creatureDamage = driver.state.getEntity(creature)?.get<DamageComponent>()?.amount ?: 0
        creatureDamage shouldBe 0
    }

    test("prevents combat damage FROM enchanted creature") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a 3/5 creature on our battlefield with Sandskin
        val creature = driver.putCreatureOnBattlefield(activePlayer, "Sturdy Creature")
        driver.removeSummoningSickness(creature)
        driver.putAuraOnBattlefield(activePlayer, Sandskin, creature)

        // Opponent has a 3/5 creature to block with (survives combat)
        val opponentCreature = driver.putCreatureOnBattlefield(opponent, "Sturdy Creature")
        driver.removeSummoningSickness(opponentCreature)

        // We attack
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(activePlayer, listOf(creature), opponent)
        driver.bothPass()

        // Opponent blocks
        driver.declareBlockers(opponent, mapOf(opponentCreature to listOf(creature)))
        driver.bothPass()

        // First strike step
        driver.bothPass()

        // Combat damage - opponent's creature should take 0 damage (Sandskin prevents FROM)
        driver.bothPass()

        val opponentCreatureDamage = driver.state.getEntity(opponentCreature)?.get<DamageComponent>()?.amount ?: 0
        opponentCreatureDamage shouldBe 0
    }

    test("does not prevent non-combat (spell) damage to enchanted creature") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a 3/5 creature on our battlefield with Sandskin (survives 3 damage)
        val creature = driver.putCreatureOnBattlefield(activePlayer, "Sturdy Creature")
        driver.putAuraOnBattlefield(activePlayer, Sandskin, creature)

        // Deal 3 damage via Lightning Bolt (non-combat damage)
        driver.giveMana(activePlayer, Color.RED, 1)
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")
        driver.castSpellWithTargets(activePlayer, bolt, listOf(ChosenTarget.Permanent(creature)))
        driver.bothPass()

        // Full 3 damage - Sandskin only prevents combat damage
        val damage = driver.state.getEntity(creature)?.get<DamageComponent>()?.amount ?: 0
        damage shouldBe 3
    }

    test("does not prevent combat damage to non-enchanted creatures") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put creature A (3/5) with Sandskin, and creature B (3/5) without
        val creatureA = driver.putCreatureOnBattlefield(activePlayer, "Sturdy Creature")
        driver.removeSummoningSickness(creatureA)
        driver.putAuraOnBattlefield(activePlayer, Sandskin, creatureA)

        val creatureB = driver.putCreatureOnBattlefield(activePlayer, "Sturdy Creature")
        driver.removeSummoningSickness(creatureB)

        // Opponent has a 3/3 creature
        val opponentCreature = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")
        driver.removeSummoningSickness(opponentCreature)

        // We attack with creature B (no Sandskin)
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(activePlayer, listOf(creatureB), opponent)
        driver.bothPass()

        // Opponent blocks with their creature
        driver.declareBlockers(opponent, mapOf(opponentCreature to listOf(creatureB)))
        driver.bothPass()

        // First strike step
        driver.bothPass()

        // Combat damage - creature B should take full 3 damage (no Sandskin)
        driver.bothPass()

        val creatureBDamage = driver.state.getEntity(creatureB)?.get<DamageComponent>()?.amount ?: 0
        creatureBDamage shouldBe 3
    }
})
