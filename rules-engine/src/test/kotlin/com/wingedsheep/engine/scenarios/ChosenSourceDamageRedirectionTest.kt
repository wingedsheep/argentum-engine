package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.ObjectReferenceEnvironment
import com.wingedsheep.engine.handlers.effects.DamageUtils
import com.wingedsheep.engine.handlers.effects.combat.*
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.stack.*
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.RedirectDamageFromChosenSourceEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.matchers.shouldBe
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import kotlinx.serialization.json.Json

class ChosenSourceDamageRedirectionTest : ScenarioTestBase() {
    init {
        val shooter = card("Source Choice Shooter") {
            manaCost = "{0}"
            typeLine = "Creature — Human"
            power = 1; toughness = 4
            activatedAbility { cost = Costs.Free; val creature = target(com.wingedsheep.sdk.scripting.filters.unified.TargetFilter.Creature); effect = Effects.DealDamage(3, creature) }
        }
        cardRegistry.register(shooter)
        fun board() = scenario().withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardOnBattlefield(2, "Hill Giant")
            .withCardOnBattlefield(2, "Mountain")
            .withCardInLibrary(1, "Forest").withCardInLibrary(2, "Forest")
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()

        fun install(game: TestGame, chosen: EntityId = game.findPermanent("Hill Giant")!!,
                    recipient: EntityId = game.player1Id, duration: Duration = Duration.EndOfTurn) {
            val bear = game.findPermanent("Grizzly Bears")!!
            val effect = RedirectDamageFromChosenSourceEffect(EffectTarget.ContextTarget(0),
                if (recipient == game.player1Id) EffectTarget.Controller else EffectTarget.ContextTarget(1), duration)
            val result = RedirectDamageFromChosenSourceExecutor().execute(game.state, effect,
                EffectContext(sourceId = null, controllerId = game.player1Id, targets = listOf(ChosenTarget.Permanent(bear), ChosenTarget.Permanent(recipient))))
            game.state = result.state
            game.selectCards(listOf(chosen)).error shouldBe null
        }
        fun redirect(state: GameState, source: EntityId, target: EntityId, amount: Int = 3) =
            DamageUtils.checkDamageRedirection(state, target, amount, sourceId = source)

        val doubler = card("Source Choice Doubler") {
            manaCost = "{0}"
            typeLine = "Enchantment"
            replacementEffect(com.wingedsheep.sdk.scripting.DoubleDamage(
                appliesTo = com.wingedsheep.sdk.scripting.EventPattern.DamageEvent()
            ))
        }
        cardRegistry.register(doubler)
        fun combatBoard(doubleDamage: Boolean = false): TestGame {
            val setup = scenario().withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
                .withCardInLibrary(1, "Forest").withCardInLibrary(2, "Forest")
                .withActivePlayer(2).withPriorityPlayer(2)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            if (doubleDamage) setup.withCardOnBattlefield(2, "Source Choice Doubler")
            val game = setup.build()
            game.declareAttackers(mapOf("Hill Giant" to 1)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Grizzly Bears" to listOf("Hill Giant"))).error shouldBe null
            return game
        }
        test("redirected combat damage observes destination player protection") {
            val game = combatBoard()
            install(game)
            game.state = game.state.updateEntity(game.player1Id) { it.with(
                com.wingedsheep.engine.state.components.player.PlayerProtectionComponent(
                    scopes = listOf(com.wingedsheep.sdk.scripting.ProtectionScope.Everything))) }
            game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
            game.getLifeTotal(1) shouldBe 20
            game.findPermanent("Grizzly Bears").shouldNotBeNull()
            game.state.floatingEffects.isEmpty() shouldBe true
        }
        test("redirected combat damage observes destination prevention and is amplified once") {
            val game = combatBoard(doubleDamage = true)
            install(game)
            game.state = services.effectExecutorRegistry.execute(game.state,
                Effects.PreventDamage(amount = com.wingedsheep.sdk.scripting.values.DynamicAmount.Fixed(2)),
                EffectContext(sourceId = null, controllerId = game.player1Id)).state
            game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
            game.getLifeTotal(1) shouldBe 16
            game.findPermanent("Grizzly Bears").shouldNotBeNull()
        }
        test("chosen-source retargeting survives a destination optional-redirection pause") {
            val game = combatBoard()
            install(game)
            val shield = services.effectExecutorRegistry.execute(game.state,
                Effects.RedirectNextDamage(listOf(EffectTarget.Controller), EffectTarget.ContextTarget(0), optional = true),
                EffectContext(sourceId = null, controllerId = game.player1Id,
                    targets = listOf(ChosenTarget.Player(game.player2Id))))
            shield.error shouldBe null
            game.state = shield.state
            repeat(8) {
                if (!game.hasPendingDecision()) game.passPriority().error shouldBe null
            }
            (game.getPendingDecision() is YesNoDecision) shouldBe true
            game.state.floatingEffects.count {
                (it.effect.modification as? SerializableModification.RedirectNextDamage)?.chosenSource != null
            } shouldBe 1
            game.answerYesNo(true).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
            game.getLifeTotal(1) shouldBe 20
            game.getLifeTotal(2) shouldBe 17
            game.findPermanent("Grizzly Bears").shouldNotBeNull()
            game.state.floatingEffects.isEmpty() shouldBe true
            game.state.getEntity(game.player2Id)!!
                .get<com.wingedsheep.engine.state.components.player.CombatDamageReceivedThisTurnComponent>()!!.amount shouldBe 3
        }
        test("chosen-source combat redirections chain before destination damage") {
            val game = combatBoard()
            install(game)
            val result = RedirectDamageFromChosenSourceExecutor().execute(game.state,
                RedirectDamageFromChosenSourceEffect(EffectTarget.Controller,
                    EffectTarget.PlayerRef(com.wingedsheep.sdk.scripting.references.Player.AnOpponent)),
                EffectContext(sourceId = null, controllerId = game.player1Id))
            game.state = result.state
            game.selectCards(listOf(game.findPermanent("Hill Giant")!!)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
            game.getLifeTotal(1) shouldBe 20
            game.getLifeTotal(2) shouldBe 17
            game.findPermanent("Grizzly Bears").shouldNotBeNull()
            game.state.floatingEffects.isEmpty() shouldBe true
        }
        test("only the chosen source and protected object consume the shield") {
            val game = board(); val bear = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!; val mountain = game.findPermanent("Mountain")!!
            install(game)
            redirect(game.state, mountain, bear).second shouldBe null
            redirect(game.state, giant, game.player1Id).second shouldBe null
            val result = redirect(game.state, giant, bear)
            result.second shouldBe game.player1Id; result.third shouldBe 3
            redirect(result.first, giant, bear).second shouldBe null
        }
        test("zero damage does not consume a next-instance shield") {
            val game = board(); install(game)
            val source = game.findPermanent("Hill Giant")!!; val bear = game.findPermanent("Grizzly Bears")!!
            val zero = redirect(game.state, source, bear, 0)
            zero.first.floatingEffects shouldBe game.state.floatingEffects
            redirect(zero.first, source, bear).second shouldBe game.player1Id
        }
        test("a land with no damage ability is a legal source choice") {
            val game = board(); val mountain = game.findPermanent("Mountain")!!
            damageSourceChoices(game.state).map { it.reference.entityId } shouldContain mountain
            install(game, mountain)
            redirect(game.state, mountain, game.findPermanent("Grizzly Bears")!!).second shouldBe game.player1Id
        }
        test("the chosen source cannot follow a later battlefield visit") {
            val game = board(); val giant = game.findPermanent("Hill Giant")!!; install(game)
            val owner = game.player2Id
            val moved = game.state.removeFromZone(ZoneKey(owner, Zone.BATTLEFIELD), giant)
                .addToZone(ZoneKey(owner, Zone.HAND), giant)
                .removeFromZone(ZoneKey(owner, Zone.HAND), giant)
                .addToZone(ZoneKey(owner, Zone.BATTLEFIELD), giant)
            redirect(moved, giant, game.findPermanent("Grizzly Bears")!!).second shouldBe null
        }
        test("the protected object cannot follow a later battlefield visit") {
            val game = board(); val bear = game.findPermanent("Grizzly Bears")!!; install(game)
            val owner = game.player1Id
            val moved = game.state.removeFromZone(ZoneKey(owner, Zone.BATTLEFIELD), bear)
                .addToZone(ZoneKey(owner, Zone.HAND), bear)
                .removeFromZone(ZoneKey(owner, Zone.HAND), bear)
                .addToZone(ZoneKey(owner, Zone.BATTLEFIELD), bear)
            redirect(moved, game.findPermanent("Hill Giant")!!, bear).second shouldBe null
        }
        test("a departed redirect recipient leaves damage and shield unchanged") {
            val game = board(); val giant = game.findPermanent("Hill Giant")!!
            install(game, recipient = giant)
            val moved = game.state.removeFromZone(ZoneKey(game.player2Id, Zone.BATTLEFIELD), giant)
                .addToZone(ZoneKey(game.player2Id, Zone.GRAVEYARD), giant)
            val result = redirect(moved, giant, game.findPermanent("Grizzly Bears")!!)
            result.second shouldBe null; result.first.floatingEffects shouldBe moved.floatingEffects
        }
        test("pending ability source remains choosable after departure with its original identity") {
            val game = board(); val giant = game.findPermanent("Hill Giant")!!
            val old = game.state.objectRef(giant)!!
            val abilityId = EntityId.generate()
            val ability = ActivatedAbilityOnStackComponent(giant, "Hill Giant", game.player2Id,
                Effects.DealDamage(3, EffectTarget.ContextTarget(0)), objectReferences = ObjectReferenceEnvironment(captured = true, origin = old, source = old))
            val moved = game.state.removeFromZone(ZoneKey(game.player2Id, Zone.BATTLEFIELD), giant)
                .addToZone(ZoneKey(game.player2Id, Zone.GRAVEYARD), giant)
                .withEntity(abilityId, com.wingedsheep.engine.state.ComponentContainer.of(ability)).pushToStack(abilityId)
            val choices = damageSourceChoices(moved)
            choices.map { it.reference } shouldContain old
            choices.map { it.reference } shouldNotContain moved.objectRef(giant)
            val bear = game.findPermanent("Grizzly Bears")!!
            val paused = RedirectDamageFromChosenSourceExecutor().execute(moved,
                RedirectDamageFromChosenSourceEffect(EffectTarget.ContextTarget(0), EffectTarget.Controller),
                EffectContext(sourceId = null, controllerId = game.player1Id, targets = listOf(ChosenTarget.Permanent(bear))))
            game.state = paused.state
            val question = game.getPendingDecision() as ChooseOptionDecision
            game.submitDecision(OptionChosenResponse(question.id, choices.indexOfFirst { it.reference == old })).error shouldBe null
            val dealt = services.effectExecutorRegistry.execute(game.state, Effects.DealDamage(3, EffectTarget.ContextTarget(0)),
                EffectContext(sourceId = giant, controllerId = game.player2Id, targets = listOf(ChosenTarget.Permanent(bear)),
                    objectReferences = ability.objectReferences))
            dealt.events.filterIsInstance<DamageDealtEvent>().single().targetId shouldBe game.player1Id
            dealt.events.filterIsInstance<DamageDealtEvent>().single().sourceId shouldBe giant
        }
        test("redirection applies to unpreventable damage and retains its source") {
            val game = board(); install(game); val giant = game.findPermanent("Hill Giant")!!
            val result = DamageUtils.dealDamageToTarget(zones, game.state, game.findPermanent("Grizzly Bears")!!,
                3, giant, cantBePrevented = true)
            result.events.filterIsInstance<DamageDealtEvent>().single().sourceId shouldBe giant
            result.events.filterIsInstance<DamageDealtEvent>().single().targetId shouldBe game.player1Id
        }
        test("independent instances stack and are consumed one at a time") {
            val game = board(); install(game); install(game)
            val giant = game.findPermanent("Hill Giant")!!; val bear = game.findPermanent("Grizzly Bears")!!
            val first = redirect(game.state, giant, bear)
            first.first.floatingEffects.size shouldBe 1
            redirect(first.first, giant, bear).first.floatingEffects.size shouldBe 0
        }
        test("shield and paused source decision serialize with object identities") {
            val game = board(); install(game, duration = Duration.EndOfCombat)
            val json = Json { serializersModule = engineSerializersModule; allowStructuredMapKeys = true; encodeDefaults = true }
            val restored = json.decodeFromString<GameState>(json.encodeToString(game.state))
            restored.floatingEffects shouldBe game.state.floatingEffects
            redirect(restored, game.findPermanent("Hill Giant")!!, game.findPermanent("Grizzly Bears")!!).second shouldBe game.player1Id
            val paused = RedirectDamageFromChosenSourceExecutor().execute(game.state,
                RedirectDamageFromChosenSourceEffect(EffectTarget.Controller, EffectTarget.Controller),
                EffectContext(sourceId = null, controllerId = game.player1Id))
            json.decodeFromString<GameState>(json.encodeToString(paused.state)).continuationStack shouldBe paused.state.continuationStack
        }
        test("a chosen permanent spell follows exactly its normal resolution into a permanent") {
            val game = scenario().withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Grizzly Bears").withCardInHand(2, "Source Choice Shooter")
                .withCardInLibrary(1, "Forest").withCardInLibrary(2, "Forest")
                .withActivePlayer(2).withPriorityPlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            game.castSpell(2, "Source Choice Shooter").error shouldBe null
            val spellId = game.state.stack.last(); val spellRef = game.state.objectRef(spellId)!!
            install(game, chosen = spellId)
            game.resolveStack()
            val chosen = (game.state.floatingEffects.single().effect.modification as SerializableModification.RedirectNextDamage).chosenSource!!
            chosen.reference shouldBe game.state.objectRef(spellId)
            (chosen.reference == spellRef) shouldBe false
            game.execute(ActivateAbility(game.player2Id, spellId, shooter.script.activatedAbilities.single().id,
                targets = listOf(ChosenTarget.Permanent(game.findPermanent("Grizzly Bears")!!)))).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 17
            game.findPermanent("Grizzly Bears").shouldNotBeNull()
        }
        test("face-up command objects are offered and face-down command objects are hidden") {
            val game = board(); val giant = game.findPermanent("Hill Giant")!!
            val command = game.state.removeFromZone(ZoneKey(game.player2Id, Zone.BATTLEFIELD), giant)
                .addToZone(ZoneKey(game.player2Id, Zone.COMMAND), giant)
            damageSourceChoices(command).map { it.reference.entityId } shouldContain giant
            val hidden = command.updateEntity(giant) { it.with(FaceDownComponent) }
            damageSourceChoices(hidden).map { it.reference.entityId } shouldNotContain giant
        }
        test("delayed triggers preserve a departed source choice") {
            val game = board(); val giant = game.findPermanent("Hill Giant")!!
            val old = game.state.objectRef(giant)!!
            val delayed = com.wingedsheep.engine.event.DelayedTriggeredAbility("source-choice-delayed", Effects.GainLife(1),
                Step.END, giant, "Hill Giant", game.player2Id,
                objectReferences = ObjectReferenceEnvironment(captured = true, origin = old, source = old))
            val departed = game.state.removeFromZone(ZoneKey(game.player2Id, Zone.BATTLEFIELD), giant)
                .addToZone(ZoneKey(game.player2Id, Zone.GRAVEYARD), giant).copy(delayedTriggers = listOf(delayed))
            damageSourceChoices(departed).map { it.reference } shouldContain old
        }
        test("a waiting prevention shield preserves its departed source identity for a later source choice") {
            val game = board(); val giant = game.findPermanent("Hill Giant")!!; val old = game.state.objectRef(giant)!!
            val shielded = services.effectExecutorRegistry.execute(
                game.state, com.wingedsheep.sdk.scripting.effects.PreventDamageEffect(
                    target = EffectTarget.ContextTarget(0), direction = com.wingedsheep.sdk.scripting.effects.PreventionDirection.FromTarget),
                EffectContext(sourceId = null, controllerId = game.player1Id, targets = listOf(ChosenTarget.Permanent(giant))))
            val moved = shielded.state.removeFromZone(ZoneKey(game.player2Id, Zone.BATTLEFIELD), giant)
                .addToZone(ZoneKey(game.player2Id, Zone.GRAVEYARD), giant)
            damageSourceChoices(moved).map { it.reference } shouldContain old
        }
        test("sacrificed cost objects remain valid source choices after leaving the battlefield") {
            val game = board(); val giant = game.findPermanent("Hill Giant")!!; val old = game.state.objectRef(giant)!!
            val snapshot = captureEntitySnapshots(listOf(giant), game.state.projectedState, game.state).single()
            snapshot.objectRef shouldBe old
            val abilityId = EntityId.generate()
            val ability = ActivatedAbilityOnStackComponent(game.findPermanent("Grizzly Bears")!!, "Grizzly Bears", game.player1Id,
                Effects.GainLife(1), sacrificedPermanents = listOf(snapshot))
            val moved = game.state.removeFromZone(ZoneKey(game.player2Id, Zone.BATTLEFIELD), giant)
                .addToZone(ZoneKey(game.player2Id, Zone.GRAVEYARD), giant)
                .withEntity(abilityId, com.wingedsheep.engine.state.ComponentContainer.of(ability)).pushToStack(abilityId)
            damageSourceChoices(moved).map { it.reference } shouldContain old
        }
        test("a waiting redirection shield makes its departed source choosable") {
            val game = board(); val giant = game.findPermanent("Hill Giant")!!; val old = game.state.objectRef(giant)!!
            install(game)
            val moved = game.state.removeFromZone(ZoneKey(game.player2Id, Zone.BATTLEFIELD), giant)
                .addToZone(ZoneKey(game.player2Id, Zone.GRAVEYARD), giant)
            damageSourceChoices(moved).map { it.reference } shouldContain old
        }
        test("target references on the stack preserve the referred departed object") {
            val game = board(); val giant = game.findPermanent("Hill Giant")!!; val old = game.state.objectRef(giant)!!
            val targets = TargetsComponent.capture(game.state, listOf(ChosenTarget.Permanent(giant)))
            val spellId = EntityId.generate()
            val referred = game.state.withEntity(spellId, com.wingedsheep.engine.state.ComponentContainer.of(targets)).pushToStack(spellId)
                .removeFromZone(ZoneKey(game.player2Id, Zone.BATTLEFIELD), giant)
                .addToZone(ZoneKey(game.player2Id, Zone.GRAVEYARD), giant)
            damageSourceChoices(referred).map { it.reference } shouldContain old
        }
        test("a recipient that stops being a creature cannot receive redirection") {
            val game = board(); val giant = game.findPermanent("Hill Giant")!!; install(game, recipient = giant)
            val card = game.state.getEntity(giant)!!.get<com.wingedsheep.engine.state.components.identity.CardComponent>()!!
            val changed = game.state.updateEntity(giant) { it.with(card.copy(typeLine = TypeLine(cardTypes = setOf(CardType.ARTIFACT)))) }
            redirect(changed, giant, game.findPermanent("Grizzly Bears")!!).second shouldBe null
        }
        test("a phased-out recipient cannot receive redirection") {
            val game = board(); val giant = game.findPermanent("Hill Giant")!!; install(game, recipient = giant)
            val phased = game.state.updateEntity(giant) { it.with(com.wingedsheep.engine.state.components.battlefield.PhasedOutComponent(game.player2Id)) }
            redirect(phased, giant, game.findPermanent("Grizzly Bears")!!).second shouldBe null
        }
        test("cleanup expires an unused shield") {
            val game = board(); install(game)
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.floatingEffects.isEmpty() shouldBe true
        }
        test("face-down source labels do not reveal the card name") {
            val game = board(); val giant = game.findPermanent("Hill Giant")!!
            val hidden = game.state.updateEntity(giant) { it.with(FaceDownComponent) }
            damageSourceChoices(hidden).single { it.reference.entityId == giant }.name shouldBe "Face-down source"
        }
    }
}
