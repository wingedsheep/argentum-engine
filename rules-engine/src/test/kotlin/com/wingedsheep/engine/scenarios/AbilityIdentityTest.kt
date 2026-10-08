package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Suspension
import com.wingedsheep.engine.core.MayAbilityContinuation
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseManaColorContinuation
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.engineSerializersModule
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.event.GrantedActivatedAbility
import com.wingedsheep.engine.event.GrantedStaticAbility
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.effects.stack.CopyTargetSpellOrAbilityExecutor
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.stack.ActivatedAbilityOnStackComponent
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.engine.state.components.stack.triggerIdentityFromCurrentCardDefinition
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
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
import com.wingedsheep.sdk.scripting.ClassLevelAbility
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.ReplaceLandManaColor
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.EachPermanentBecomesCopyOfTargetEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import com.wingedsheep.engine.core.Outcome

/**
 * Tests for the shared [AbilityIdentity] key (backlog/archived/stack-collapse-and-batch-decisions.md §C.2).
 *
 * The key is the definition-scoped pair `(cardDefinitionId, abilityId)`. Its load-bearing property
 * — the one batch decisions and persistent yields both rely on — is that two permanents printed
 * from the same card produce the *same* identity for the same ability. These tests pin:
 *  - a printed activated ability carries semantic identity plus its concrete routing id;
 *  - runtime- and static-granted abilities retain only their concrete routing id;
 *  - an activated stack copy preserves both identity fields;
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

    val printedManaAbilityId = AbilityId("test_printed_identity_mana")
    val printedManaSource = CardDefinition.creature(
        name = "Identity Mana Source",
        manaCost = ManaCost.parse("{1}"),
        subtypes = emptySet(),
        power = 1,
        toughness = 1,
        script = CardScript.permanent(
            ActivatedAbility(
                id = printedManaAbilityId,
                cost = AbilityCost.Free,
                effect = Effects.AddAnyColorMana(1),
                isManaAbility = true,
                timing = TimingRule.ManaAbility,
            )
        ),
    )

    val identityClass = CardDefinition.enchantment(
        name = "Identity Class",
        manaCost = ManaCost.parse("{1}"),
        subtypes = setOf(Subtype.CLASS),
        script = CardScript(
            classLevels = listOf(
                ClassLevelAbility(level = 2, cost = ManaCost.ZERO),
            ),
        ),
    )

    val intrinsicManaChoice = CardDefinition.enchantment(
        name = "Intrinsic Mana Choice",
        manaCost = ManaCost.parse("{1}"),
        script = CardScript(
            staticAbilities = listOf(
                ReplaceLandManaColor(filter = GameObjectFilter.BasicLand.youControl()),
            ),
        ),
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
        driver.submit(ActivateAbility(playerId = player, sourceId = pinger1, abilityId = pingerAbilityId)).outcome shouldBe Outcome.Done
        driver.submit(ActivateAbility(playerId = player, sourceId = pinger2, abilityId = pingerAbilityId)).outcome shouldBe Outcome.Done

        val activatedOnStack = driver.state.stack.mapNotNull {
            driver.state.getEntity(it)?.get<ActivatedAbilityOnStackComponent>()
        }
        activatedOnStack.size shouldBe 2

        val expected = AbilityIdentity("Identity Pinger", pingerAbilityId)
        activatedOnStack.forEach {
            it.abilityIdentity shouldBe expected
            it.activatedAbilityId shouldBe pingerAbilityId
        }
        // The two copies share one identity — the property batch/yield grouping depends on.
        activatedOnStack[0].abilityIdentity shouldBe activatedOnStack[1].abilityIdentity
    }

    test("a runtime-granted activated ability keeps its concrete id without definition identity") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(identityBear))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val player = driver.activePlayer!!
        val receiver = driver.putCreatureOnBattlefield(player, "Identity Bear")
        val grantedId = AbilityId("runtime_granted_identity_test")
        val grantedAbility = ActivatedAbility(
            id = grantedId,
            cost = AbilityCost.Free,
            effect = Effects.GainLife(1),
        )
        driver.replaceState(
            driver.state.copy(
                grantedActivatedAbilities = driver.state.grantedActivatedAbilities +
                    GrantedActivatedAbility(receiver, grantedAbility, Duration.Permanent),
            )
        )

        driver.submit(
            ActivateAbility(playerId = player, sourceId = receiver, abilityId = grantedId)
        ).outcome shouldBe Outcome.Done

        val onStack = driver.state.getEntity(driver.state.stack.last())
            ?.get<ActivatedAbilityOnStackComponent>()
            .shouldNotBeNull()
        onStack.abilityIdentity shouldBe null
        onStack.activatedAbilityId shouldBe grantedId
    }

    test("a flattened static-granted ability keeps its concrete id without definition identity") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(identityBear))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val player = driver.activePlayer!!
        val granter = driver.putCreatureOnBattlefield(player, "Identity Bear")
        val receiver = driver.putCreatureOnBattlefield(player, "Identity Bear")
        val grantedId = AbilityId("flattened_static_granted_identity_test")
        val grantedAbility = ActivatedAbility(
            id = grantedId,
            cost = AbilityCost.Free,
            effect = Effects.GainLife(1),
        )
        driver.replaceState(
            driver.state.copy(
                // Model the flattened GrantStaticAbilityEffect store used after a runtime effect
                // grants an ability-granting static to a permanent.
                grantedStaticAbilities = driver.state.grantedStaticAbilities + GrantedStaticAbility(
                    entityId = granter,
                    ability = GrantActivatedAbility(
                        ability = grantedAbility,
                        filter = GroupFilter(GameObjectFilter.Creature.youControl()),
                    ),
                    duration = Duration.Permanent,
                ),
            )
        )

        driver.submit(
            ActivateAbility(playerId = player, sourceId = receiver, abilityId = grantedId)
        ).outcome shouldBe Outcome.Done

        val onStack = driver.state.getEntity(driver.state.stack.last())
            ?.get<ActivatedAbilityOnStackComponent>()
            .shouldNotBeNull()
        onStack.abilityIdentity shouldBe null
        onStack.activatedAbilityId shouldBe grantedId
    }

    for (staticGrant in listOf(false, true)) {
        test("a ${if (staticGrant) "static" else "runtime"} copy ability retains itself after its grant disappears") {
            val driver = GameTestDriver()
            driver.registerCards(TestCards.all + listOf(identityBear, identityPinger))
            driver.initMirrorMatch(deck = Deck.of("Plains" to 40))
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
            val player = driver.activePlayer!!
            val receiver = driver.putCreatureOnBattlefield(player, "Identity Bear")
            val target = driver.putCreatureOnBattlefield(player, "Identity Pinger")
            val ability = ActivatedAbility(
                id = AbilityId("retained_granted_copy"),
                cost = AbilityCost.Free,
                effect = EachPermanentBecomesCopyOfTargetEffect(
                    target = EffectTarget.SpecificEntity(target),
                    affected = EffectTarget.Self,
                    retainActivatingAbility = true,
                ),
            )
            driver.replaceState(if (staticGrant) {
                driver.state.copy(grantedStaticAbilities = listOf(GrantedStaticAbility(
                    entityId = receiver,
                    ability = GrantActivatedAbility(ability, GroupFilter(GameObjectFilter.Creature.youControl())),
                    duration = Duration.Permanent,
                )))
            } else {
                driver.state.copy(grantedActivatedAbilities = listOf(
                    GrantedActivatedAbility(receiver, ability, Duration.Permanent)
                ))
            })
            driver.submit(ActivateAbility(player, receiver, ability.id)).outcome shouldBe Outcome.Done
            driver.state.getEntity(driver.state.stack.last())
                ?.get<ActivatedAbilityOnStackComponent>()?.abilityIdentity shouldBe null

            // The resolving ability must be independent of the grant that supplied it.
            driver.replaceState(driver.state.copy(
                grantedActivatedAbilities = emptyList(),
                grantedStaticAbilities = emptyList(),
            ))
            driver.bothPass()
            driver.state.getEntity(receiver)
                ?.get<CardComponent>()
                ?.name shouldBe "Identity Pinger"
            driver.state.grantedActivatedAbilities.single { it.entityId == receiver }.ability shouldBe ability
            driver.submit(ActivateAbility(player, receiver, ability.id)).outcome shouldBe Outcome.Done
        }
    }

    test("a copied activated stack object preserves semantic identity and concrete id") {
        val stackEntityId = EntityId.of("original-activated-ability")
        val originalController = EntityId.of("original-controller")
        val copyController = EntityId.of("copy-controller")
        val identity = AbilityIdentity("Identity Pinger", pingerAbilityId)
        val component = ActivatedAbilityOnStackComponent(
            sourceId = EntityId.of("ability-source"),
            sourceName = "Identity Pinger",
            controllerId = originalController,
            effect = Effects.GainLife(1),
            abilityIdentity = identity,
            activatedAbility = identityPinger.activatedAbilities.single(),
        )
        val state = GameState(
            rng = GameRng.seeded(2L),
            entities = mapOf(stackEntityId to ComponentContainer.of(component)),
            stack = listOf(stackEntityId),
        )

        val result = CopyTargetSpellOrAbilityExecutor.cloneAndPush(
            state = state,
            abilityEntityId = stackEntityId,
            controllerId = copyController,
        )

        result.outcome shouldBe Outcome.Done
        val copied = result.newState.getEntity(result.newState.stack.last())
            ?.get<ActivatedAbilityOnStackComponent>()
            .shouldNotBeNull()
        copied.controllerId shouldBe copyController
        copied.abilityIdentity shouldBe identity
        copied.activatedAbilityId shouldBe pingerAbilityId
        val json = Json { serializersModule = engineSerializersModule }
        json.decodeFromString<ActivatedAbilityOnStackComponent>(json.encodeToString(copied)) shouldBe copied
    }

    test("a printed mana ability carries proven identity through off-stack resolution") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + printedManaSource)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val player = driver.activePlayer!!
        val source = driver.putCreatureOnBattlefield(player, "Identity Mana Source")
        driver.submit(
            ActivateAbility(playerId = player, sourceId = source, abilityId = printedManaAbilityId)
        ).error shouldBe null

        val continuation = driver.state.peekContinuation()
            .shouldBeInstanceOf<Suspension>().answer
            .shouldBeInstanceOf<ChooseManaColorContinuation>()
        continuation.baseContext.abilityIdentity shouldBe
            AbilityIdentity("Identity Mana Source", printedManaAbilityId)
        continuation.baseContext.activatedAbilityId shouldBe printedManaAbilityId
    }

    test("a runtime-granted mana ability carries routing id without fabricated identity") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + identityBear)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val player = driver.activePlayer!!
        val receiver = driver.putCreatureOnBattlefield(player, "Identity Bear")
        val grantedId = AbilityId("runtime_granted_identity_mana")
        val grantedAbility = ActivatedAbility(
            id = grantedId,
            cost = AbilityCost.Free,
            effect = Effects.AddAnyColorMana(1),
            isManaAbility = true,
            timing = TimingRule.ManaAbility,
        )
        driver.replaceState(
            driver.state.copy(
                grantedActivatedAbilities = driver.state.grantedActivatedAbilities +
                    GrantedActivatedAbility(receiver, grantedAbility, Duration.Permanent),
            )
        )

        driver.submit(
            ActivateAbility(playerId = player, sourceId = receiver, abilityId = grantedId)
        ).error shouldBe null

        val continuation = driver.state.peekContinuation()
            .shouldBeInstanceOf<Suspension>().answer
            .shouldBeInstanceOf<ChooseManaColorContinuation>()
        continuation.baseContext.abilityIdentity shouldBe null
        continuation.baseContext.activatedAbilityId shouldBe grantedId
    }

    test("a generated Class level-up ability receives the current definition identity") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + identityClass)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val player = driver.activePlayer!!
        val source = driver.putPermanentOnBattlefield(player, "Identity Class", classLevel = 1)
        val levelUpId = AbilityId.classLevelUp(2)
        driver.submit(
            ActivateAbility(playerId = player, sourceId = source, abilityId = levelUpId)
        ).outcome shouldBe Outcome.Done

        val onStack = driver.state.getEntity(driver.state.stack.last())
            ?.get<ActivatedAbilityOnStackComponent>()
            .shouldNotBeNull()
        onStack.abilityIdentity shouldBe AbilityIdentity("Identity Class", levelUpId)
        onStack.activatedAbilityId shouldBe levelUpId
    }

    test("an intrinsic basic-land mana ability retains its id without definition identity") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + intrinsicManaChoice)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val player = driver.activePlayer!!
        driver.putPermanentOnBattlefield(player, "Intrinsic Mana Choice")
        val forest = driver.putLandOnBattlefield(player, "Forest")
        val intrinsicId = AbilityId.intrinsicMana(Color.GREEN.symbol)

        (driver.submit(
            ActivateAbility(playerId = player, sourceId = forest, abilityId = intrinsicId)
        ).outcome is Outcome.Paused) shouldBe true

        val continuation = driver.state.peekContinuation()
            .shouldBeInstanceOf<Suspension>().answer
            .shouldBeInstanceOf<ChooseManaColorContinuation>()
        continuation.baseContext.abilityIdentity shouldBe null
        continuation.baseContext.activatedAbilityId shouldBe intrinsicId
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
        driver.castSpell(player, bear).outcome shouldBe Outcome.Done
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
            abilityIdentity = identity,
            answer = MayAbilityContinuation(
                sourceName = null,
                effectIfNo = null,
                playerId = EntityId.of("player"),
                effectIfYes = Effects.DrawCards(1),
                effectContext = com.wingedsheep.engine.handlers.EffectContext(
                    sourceId = EntityId.of("source"), controllerId = EntityId.of("player")
                )
            )
        )

        val decision = result.pendingDecision as YesNoDecision
        decision.context.abilityIdentity shouldBe identity
    }

    test("the transitional trigger identity derivation returns null without a card definition") {
        // A bare state with no entity for the given id has no CardComponent → no identity, no throw.
        val state = GameState(rng = GameRng.seeded(1L))
        state.triggerIdentityFromCurrentCardDefinition(EntityId.of("ghost"), AbilityId("x")) shouldBe null
    }
})
