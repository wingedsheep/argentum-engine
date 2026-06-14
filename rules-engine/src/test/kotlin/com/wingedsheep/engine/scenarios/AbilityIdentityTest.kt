package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.stack.ActivatedAbilityOnStackComponent
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.engine.state.components.stack.abilityIdentityOf
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.model.GameRng
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.AbilityIdentity
import com.wingedsheep.sdk.scripting.ActivatedAbility
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Tests for the shared [AbilityIdentity] key (backlog/stack-collapse-and-batch-decisions.md §C.2).
 *
 * The key is the definition-scoped pair `(cardDefinitionId, abilityId)`. Its load-bearing property
 * — the one batch decisions and persistent yields both rely on — is that two permanents printed
 * from the same card produce the *same* identity for the same ability. These tests pin:
 *  - an activated ability on the stack carries its identity, and two copies of the card share it;
 *  - a triggered ability on the stack carries its identity, and two copies share it;
 *  - the may-question decision raised for an ability carries the identity in its context;
 *  - the resolver returns null (rather than throwing) for a source with no card definition.
 */
class AbilityIdentityTest : FunSpec({

    // An activated ability with a fixed id so the test can assert the exact identity. The effect is
    // a targetless life gain so activation goes straight on the stack with no intervening decision.
    val pingerAbilityId = AbilityId("test_identity_pinger_gain")
    val identityPinger = CardDefinition.creature(
        name = "Identity Pinger",
        manaCost = ManaCost.parse("{1}"),
        subtypes = emptySet(),
        power = 1,
        toughness = 1,
        script = CardScript.permanent(
            ActivatedAbility(
                id = pingerAbilityId,
                cost = AbilityCost.Tap,
                effect = Effects.GainLife(1)
            )
        )
    )

    // A vanilla creature used only as the "another creature" that makes Soul Warden trigger.
    val identityBear = CardDefinition.creature(
        name = "Identity Bear",
        manaCost = ManaCost.parse("{1}"),
        subtypes = emptySet(),
        power = 2,
        toughness = 2
    )

    test("an activated ability on the stack carries its AbilityIdentity, shared by two copies") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(identityPinger))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val player = driver.activePlayer!!
        val pinger1 = driver.putCreatureOnBattlefield(player, "Identity Pinger")
        val pinger2 = driver.putCreatureOnBattlefield(player, "Identity Pinger")
        driver.removeSummoningSickness(pinger1)
        driver.removeSummoningSickness(pinger2)

        // Activating a non-mana ability keeps priority with the activator, so both can go on the
        // stack back-to-back without an intervening pass.
        driver.submit(ActivateAbility(playerId = player, sourceId = pinger1, abilityId = pingerAbilityId)).isSuccess shouldBe true
        driver.submit(ActivateAbility(playerId = player, sourceId = pinger2, abilityId = pingerAbilityId)).isSuccess shouldBe true

        val activatedOnStack = driver.state.stack.mapNotNull {
            driver.state.getEntity(it)?.get<ActivatedAbilityOnStackComponent>()
        }
        activatedOnStack.size shouldBe 2

        val expected = AbilityIdentity("Identity Pinger", pingerAbilityId)
        activatedOnStack.forEach { it.abilityIdentity shouldBe expected }
        // The two copies share one identity — the property batch/yield grouping depends on.
        activatedOnStack[0].abilityIdentity shouldBe activatedOnStack[1].abilityIdentity
    }

    test("triggered abilities from two copies of the same card share one AbilityIdentity") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(identityBear))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val player = driver.activePlayer!!
        // Two Soul Wardens: "Whenever another creature enters, you gain 1 life." Non-optional,
        // no target — each goes directly on the stack when the bear enters.
        driver.putCreatureOnBattlefield(player, "Soul Warden")
        driver.putCreatureOnBattlefield(player, "Soul Warden")

        driver.giveColorlessMana(player, 1)
        val bear = driver.putCardInHand(player, "Identity Bear")
        driver.castSpell(player, bear).isSuccess shouldBe true
        driver.bothPass() // resolve the bear; it enters and both Soul Wardens trigger

        val soulWardenTriggers = driver.state.stack.mapNotNull {
            driver.state.getEntity(it)?.get<TriggeredAbilityOnStackComponent>()
        }.filter { it.sourceName == "Soul Warden" }

        soulWardenTriggers.size shouldBe 2
        soulWardenTriggers.forEach { trigger ->
            val identity = trigger.abilityIdentity.shouldNotBeNull()
            identity.cardDefinitionId shouldBe "Soul Warden"
        }
        soulWardenTriggers[0].abilityIdentity shouldBe soulWardenTriggers[1].abilityIdentity
    }

    test("the may-question decision carries the ability's identity in its context") {
        val handler = DecisionHandler()
        val identity = AbilityIdentity("Some Card", AbilityId("some_ability"))

        val result = handler.createYesNoDecision(
            state = GameState(rng = GameRng.seeded(1L)),
            playerId = EntityId.of("player"),
            sourceId = EntityId.of("source"),
            sourceName = "Some Card",
            prompt = "You may do the thing?",
            phase = DecisionPhase.RESOLUTION,
            abilityIdentity = identity
        )

        val decision = result.pendingDecision as YesNoDecision
        decision.context.abilityIdentity shouldBe identity
    }

    test("abilityIdentityOf returns null for a source with no card definition") {
        // A bare state with no entity for the given id has no CardComponent → no identity, no throw.
        val state = GameState(rng = GameRng.seeded(1L))
        state.abilityIdentityOf(EntityId.of("ghost"), AbilityId("x")) shouldBe null
    }
})
