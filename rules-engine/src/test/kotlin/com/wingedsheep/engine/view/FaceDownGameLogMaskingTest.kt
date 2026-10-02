package com.wingedsheep.engine.view

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.EventCardPresentation
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.SpellCastEvent
import com.wingedsheep.engine.core.engineSerializersModule
import com.wingedsheep.engine.handlers.effects.FaceDownTurnUp
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.PhasedOutComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.EmblemActivatedAbilityComponent
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.components.identity.FaceDownModeComponent
import com.wingedsheep.engine.state.FACE_DOWN_DISPLAY_NAME
import com.wingedsheep.engine.state.nameVisibleToAll
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.engine.state.permissions.MayPlayPermission
import com.wingedsheep.engine.state.permissions.addMayPlayPermission
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.AnyTarget
import com.wingedsheep.sdk.scripting.OpponentsPlayWithHandsRevealed
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.RevealTopOfLibrary
import com.wingedsheep.sdk.scripting.CastSpellTypesFromTopOfLibrary
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * The game log must never print the name of a card that is face down on the battlefield.
 *
 * CR 708.2a: a face-down permanent has no name. The public cast line is masked, but the three
 * lines that follow it were not, so a disguised creature announced itself the moment it
 * resolved:
 *
 * ```
 * Opponent cast Face-down creature
 * Aurelia's Vindicator resolved                                      <- leak
 * Opponent's Aurelia's Vindicator entered the battlefield            <- leak
 * You cast Igneous Inspiration targeting Aurelia's Vindicator        <- leak
 * Opponent's Aurelia's Vindicator triggered: ... unless ... pays {2} <- leak
 * ```
 *
 * [ClientEventTransformer] cannot reconstruct a past audience from a later `GameState`. The cast
 * event therefore carries a viewer-aware presentation captured by `StackResolver`; the remaining
 * audience-agnostic events are masked where they are emitted.
 *
 * The later events mask every player's log: the object genuinely has no name after it resolves.
 * The cast event is viewer-aware instead, so its controller may read the card identity while an
 * opponent cannot. [ClientStateTransformer] likewise keeps the real name in the controller's card
 * view, which [FaceDownHelperCardVisibilityTest] covers.
 */
class FaceDownGameLogMaskingTest : FunSpec({

    val disguisedAngel = card("Disguised Angel") {
        manaCost = "{2}{W}{W}"
        typeLine = "Creature — Angel"
        power = 4
        toughness = 2
        disguise = "{3}{W}"
    }

    // A targeted *trigger* is the reachable path to a ChooseTargetsDecision: a non-modal spell
    // declares its targets at cast time and never prompts.
    val tapper = card("Test Tapper") {
        manaCost = "{2}{B}"
        typeLine = "Creature — Human"
        power = 1
        toughness = 1
        triggeredAbility {
            trigger = Triggers.self.enters()
            target(TargetFilter.Creature)
            effect = Effects.Tap(EffectTarget.ContextTarget(0))
            description = "When this creature enters, tap target creature."
        }
    }

    val openThoughts = card("Open Thoughts") {
        manaCost = "{1}"
        typeLine = "Artifact"
        staticAbility { ability = OpponentsPlayWithHandsRevealed }
    }

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(disguisedAngel, tapper, openThoughts))
        d.initMirrorMatch(deck = Deck.of("Swamp" to 40))
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    /** Every log line this game has produced, as [viewingPlayerId] would read it. */
    fun GameTestDriver.logAsSeenBy(viewingPlayerId: EntityId): List<String> =
        ClientEventTransformer.transform(events, viewingPlayerId).map { it.description }

    /** Put [cardName] onto the battlefield face down under [mode], as a real face-down entry does. */
    fun GameTestDriver.putFaceDown(playerId: EntityId, cardName: String, mode: FaceDownMode): EntityId {
        val id = putCreatureOnBattlefield(playerId, cardName)
        val cardDef = cardRegistry.requireCard(cardName)
        replaceState(
            state.updateEntity(id) { container ->
                var c = container.with(FaceDownComponent).with(FaceDownModeComponent(mode))
                FaceDownTurnUp.dataFor(cardDef, cardName, mode)?.let { c = c.with(it) }
                c
            }
        )
        return id
    }

    test("a creature cast face down is not named as it resolves or as it enters") {
        val d = driver()
        val caster = d.activePlayer!!
        val opponent = d.getOpponent(caster)

        val cardId = d.putCardInHand(caster, "Disguised Angel")
        d.giveMana(caster, Color.BLACK, 3)
        d.submitSuccess(
            CastSpell(
                playerId = caster,
                cardId = cardId,
                castFaceDown = true,
                paymentStrategy = PaymentStrategy.FromPool,
            )
        )
        val castEvent = d.events.filterIsInstance<SpellCastEvent>().single()
        // Keep the historical public-name field safe while the trusted snapshot retains the
        // underlying identity required by knowledge bookkeeping.
        castEvent.cardName shouldBe FACE_DOWN_DISPLAY_NAME
        castEvent.underlyingCardName shouldBe "Disguised Angel"
        castEvent.cardPresentation?.nameFor(caster) shouldBe "Disguised Angel"
        castEvent.cardPresentation?.nameFor(opponent) shouldBe FACE_DOWN_DISPLAY_NAME
        d.bothPass()

        val opponentLog = d.logAsSeenBy(opponent)
        withClue("opponent's log: $opponentLog") {
            opponentLog.forEach { it shouldNotContain "Disguised Angel" }
            opponentLog.contains("Face-down creature resolved") shouldBe true
            opponentLog.contains("Opponent's Face-down creature entered the battlefield") shouldBe true
        }

        val casterLog = d.logAsSeenBy(caster)
        withClue("caster's own log: $casterLog") {
            casterLog.contains("You cast Disguised Angel") shouldBe true
            casterLog.contains("Your Face-down creature entered the battlefield") shouldBe true
        }

        // The event-time snapshot is authoritative even after the stack object has changed zones.
        ClientEventTransformer.transform(listOf(castEvent), opponent)
            .single().description shouldBe "Opponent cast Face-down creature"
    }

    test("a face-down cast from face-up exile retains its public identity") {
        val d = driver()
        val caster = d.activePlayer!!
        val opponent = d.getOpponent(caster)
        val cardId = d.putCardInExile(caster, "Disguised Angel")
        d.replaceState(
            d.state.addMayPlayPermission(
                MayPlayPermission(
                    id = EntityId.of("cast-disguised-angel-from-exile"),
                    cardIds = setOf(cardId),
                    controllerId = caster,
                    timestamp = d.state.timestamp,
                )
            )
        )
        d.giveColorlessMana(caster, 3)

        val result = d.submitSuccess(
            CastSpell(
                playerId = caster,
                cardId = cardId,
                castFaceDown = true,
                paymentStrategy = PaymentStrategy.FromPool,
            )
        )

        val castEvent = result.events.filterIsInstance<SpellCastEvent>().single()
        castEvent.cardName shouldBe FACE_DOWN_DISPLAY_NAME
        castEvent.underlyingCardName shouldBe "Disguised Angel"
        castEvent.cardPresentation?.nameFor(caster) shouldBe "Disguised Angel"
        castEvent.cardPresentation?.nameFor(opponent) shouldBe "Disguised Angel"
        castEvent.cardPresentation?.nameFor(EntityId.of("spectator"), isSpectator = true) shouldBe
            "Disguised Angel"
        d.logAsSeenBy(opponent).any { it.contains("cast Disguised Angel") } shouldBe true
    }

    test("continuous hand visibility survives a face-down cast") {
        val d = driver()
        val caster = d.activePlayer!!
        val opponent = d.getOpponent(caster)
        d.putPermanentOnBattlefield(opponent, openThoughts.name)
        val cardId = d.putCardInHand(caster, "Disguised Angel")
        d.giveColorlessMana(caster, 3)

        val result = d.submitSuccess(
            CastSpell(
                playerId = caster,
                cardId = cardId,
                castFaceDown = true,
                paymentStrategy = PaymentStrategy.FromPool,
            )
        )

        val castEvent = result.events.filterIsInstance<SpellCastEvent>().single()
        castEvent.cardPresentation?.nameFor(caster) shouldBe "Disguised Angel"
        castEvent.cardPresentation?.nameFor(opponent) shouldBe "Disguised Angel"
        castEvent.cardPresentation?.nameFor(EntityId.of("spectator"), isSpectator = true) shouldBe
            FACE_DOWN_DISPLAY_NAME
    }

    test("cast presentation retains public origin visibility that mana payment disables") {
        val d = driver()
        val caster = d.activePlayer!!
        val opponent = d.getOpponent(caster)
        val publicTopManaSource = card("Public Top Mana Source") {
            manaCost = "{1}"
            typeLine = "Artifact"
            staticAbility { ability = CastSpellTypesFromTopOfLibrary(Filters.Creature) }
            staticAbility {
                ability = ConditionalStaticAbility(
                    ability = RevealTopOfLibrary,
                    condition = Conditions.SourceIsUntapped,
                )
            }
            activatedAbility {
                cost = Costs.Tap
                effect = Effects.AddColorlessMana(3)
                manaAbility = true
            }
        }
        d.registerCard(publicTopManaSource)
        val manaSource = d.putPermanentOnBattlefield(caster, publicTopManaSource.name)
        val cardId = d.putCardOnTopOfLibrary(caster, disguisedAngel.name)
        val visibility = Visibility(d.cardRegistry, conditionEvaluator = PredicateEvaluator(cardRegistry = null).conditions)
        visibility.isCardIdentityVisibleTo(
            d.state, Zone.LIBRARY, cardId, opponent, isSpectator = true,
        ) shouldBe true

        // The origin is public as casting begins. Paying for the spell then taps the only
        // reveal source, so a snapshot taken after payment would incorrectly forget it.
        val result = d.submitSuccess(
            CastSpell(
                playerId = caster,
                cardId = cardId,
                castFaceDown = true,
                paymentStrategy = PaymentStrategy.Explicit(listOf(manaSource)),
            )
        )

        val castEvent = result.events.filterIsInstance<SpellCastEvent>().single()
        d.state.getEntity(manaSource)?.has<com.wingedsheep.engine.state.components.battlefield.TappedComponent>() shouldBe true
        visibility.isCardIdentityVisibleTo(
            d.state, Zone.STACK, cardId, opponent,
        ) shouldBe false
        castEvent.cardPresentation?.nameFor(opponent) shouldBe disguisedAngel.name
        castEvent.cardPresentation?.nameFor(caster, isSpectator = true) shouldBe disguisedAngel.name
        ClientEventTransformer.transform(listOf(castEvent), opponent)
            .single().shouldBeInstanceOf<ClientEvent.SpellCast>().spellName shouldBe disguisedAngel.name
    }

    test("an emblem-granted activation does not reveal its face-down source in events or stack art") {
        val d = driver()
        val controller = d.activePlayer!!
        val opponent = d.getOpponent(controller)
        val hidden = d.putFaceDown(controller, disguisedAngel.name, FaceDownMode.DISGUISE)
        d.removeSummoningSickness(hidden)
        val hiddenImage = "https://example.test/disguised-angel.png"
        d.replaceState(d.state.updateEntity(hidden) { container ->
            container.with(container.get<CardComponent>()!!.copy(imageUri = hiddenImage))
        })
        val ability = ActivatedAbility(
            id = AbilityId("FaceDownGameLogMaskingTest_1"),
            cost = Costs.Tap,
            targetRequirements = listOf(AnyTarget()),
            effect = Effects.DealDamage(DynamicAmounts.sourcePower(), EffectTarget.ContextTarget(0)),
            descriptionOverride = "{T}: This creature deals damage equal to its power to any target.",
        )
        d.replaceState(d.state.withEntity(
            EntityId.of("test-damage-emblem"),
            ComponentContainer.of(
                ControllerComponent(controller),
                EmblemActivatedAbilityComponent(GroupFilter.AllCreaturesYouControl, listOf(ability)),
            ),
        ))

        val result = d.submitSuccess(ActivateAbility(
            playerId = controller,
            sourceId = hidden,
            abilityId = ability.id,
            targets = listOf(ChosenTarget.Player(opponent)),
        ))
        val json = Json { serializersModule = engineSerializersModule }
        val opponentEvents = json.encodeToString(ClientEventTransformer.transform(result.events, opponent))
        val stackId = d.state.stack.last()
        val transformer = ClientStateTransformer(d.cardRegistry, predicateEvaluator = PredicateEvaluator(cardRegistry = null))
        val publicStackCards = listOf(
            transformer.transform(d.state, opponent).cards.getValue(stackId),
            transformer.transform(d.state, controller, isSpectator = true).cards.getValue(stackId),
        )

        val resolution = d.bothPass()
        val resolutionEvents = json.encodeToString(ClientEventTransformer.transform(resolution.events, opponent))
        val (_, untapEvents) = com.wingedsheep.engine.core.untapOrConsumeStun(d.state, hidden)
        val publicUntapEvents = json.encodeToString(ClientEventTransformer.transform(untapEvents, opponent))

        assertSoftly {
            opponentEvents shouldNotContain disguisedAngel.name
            resolutionEvents shouldNotContain disguisedAngel.name
            publicUntapEvents shouldNotContain disguisedAngel.name
            resolution.events.filterIsInstance<com.wingedsheep.engine.core.DamageDealtEvent>()
                .single().amount shouldBe 2
            publicStackCards.forEach { stackCard ->
                stackCard.name shouldBe "Face-down creature ability"
                stackCard.imageUri shouldBe null
                stackCard.colors shouldBe emptySet()
                stackCard.oracleText shouldNotContain disguisedAngel.name
                stackCard.abilityIdentity shouldBe null
                json.encodeToString(stackCard) shouldNotContain disguisedAngel.name
                json.encodeToString(stackCard) shouldNotContain hiddenImage
            }
        }
    }

    test("the ward a disguised permanent triggers is not attributed to the hidden card") {
        val d = driver()
        val activePlayer = d.activePlayer!!
        val opponent = d.getOpponent(activePlayer)

        // The face-down permanent's ward {2} comes from disguise (CR 702.168a), not from any
        // printed ability — a face-down permanent has none (CR 708.2).
        val hidden = d.putFaceDown(opponent, "Disguised Angel", FaceDownMode.DISGUISE)
        val hiddenImage = "https://example.test/disguised-angel-ward.png"
        d.replaceState(d.state.updateEntity(hidden) { container ->
            container.with(container.get<CardComponent>()!!.copy(imageUri = hiddenImage))
        })

        // Exactly {R} for Bolt and nothing left for the ward, so it counters without a prompt.
        d.giveMana(activePlayer, Color.RED, 1)
        val bolt = d.putCardInHand(activePlayer, "Lightning Bolt")
        d.castSpellWithTargets(
            activePlayer, bolt, listOf(ChosenTarget.Permanent(hidden))
        ).outcome shouldBe Outcome.Done

        val wardId = d.state.stack.single { id ->
            d.state.getEntity(id)?.has<TriggeredAbilityOnStackComponent>() == true
        }
        val transformer = ClientStateTransformer(d.cardRegistry, predicateEvaluator = PredicateEvaluator(cardRegistry = null))
        val publicWardCards = listOf(
            transformer.transform(d.state, activePlayer).cards.getValue(wardId),
            transformer.transform(d.state, opponent, isSpectator = true).cards.getValue(wardId),
        )
        assertSoftly {
            publicWardCards.forEach { wardCard ->
                wardCard.name shouldBe "Face-down creature trigger"
                wardCard.imageUri shouldBe null
                wardCard.colors shouldBe emptySet()
                wardCard.oracleText shouldNotContain disguisedAngel.name
                wardCard.abilityIdentity shouldBe null
            }
        }

        d.bothPass()

        val log = d.logAsSeenBy(activePlayer)
        withClue("targeting player's log: $log") {
            log.forEach { it shouldNotContain "Disguised Angel" }
            // The spell that targeted it must not name it either — this is the line that gave the
            // Vindicator away in the transcript this test was written from.
            log.contains("You cast Lightning Bolt targeting Face-down creature") shouldBe true
            log.any { it.startsWith("Opponent's Face-down creature triggered:") } shouldBe true
        }
    }

    test("a spell that answers a face-down spell does not name it either") {
        val d = driver()
        val caster = d.activePlayer!!
        val opponent = d.getOpponent(caster)

        val cardId = d.putCardInHand(caster, "Disguised Angel")
        d.giveMana(caster, Color.BLACK, 3)
        d.submitSuccess(
            CastSpell(
                playerId = caster,
                cardId = cardId,
                castFaceDown = true,
                paymentStrategy = PaymentStrategy.FromPool,
            )
        )

        // A face-down *spell* is nameless too (CR 708.4), so the counter that answers it on the
        // stack must not name it — the leak is the same one, a zone earlier.
        d.passPriority(caster)
        d.giveMana(opponent, Color.BLUE, 2)
        val counter = d.putCardInHand(opponent, "Counterspell")
        d.castSpellWithTargets(
            opponent, counter, listOf(ChosenTarget.Spell(cardId))
        ).outcome shouldBe Outcome.Done

        val log = d.logAsSeenBy(caster)
        withClue("log: $log") {
            // The caster's own event-time cast line may name their spell; the counter's target
            // description remains audience-agnostic and must not repeat that identity.
            log.filterNot { it == "You cast Disguised Angel" }
                .forEach { it shouldNotContain "Disguised Angel" }
            log.contains("Opponent cast Counterspell targeting Face-down creature") shouldBe true
        }
    }

    context("which zone the mask applies in") {

        test("battlefield and stack are masked; a face-down card in hand is not") {
            val d = driver()
            val player = d.activePlayer!!

            // A card in hand is hidden by the zone itself; calling it "Face-down creature" would
            // be a different claim, not a safer one.
            val inHand = d.putCardInHand(player, "Disguised Angel")
            d.replaceState(d.state.updateEntity(inHand) { it.with(FaceDownComponent) })
            nameVisibleToAll(d.state, inHand, "Disguised Angel") shouldBe "Disguised Angel"

            val onBattlefield = d.putFaceDown(player, "Disguised Angel", FaceDownMode.DISGUISE)
            nameVisibleToAll(d.state, onBattlefield, "Disguised Angel") shouldBe "Face-down creature"
        }

        test("a phased-out face-down permanent is still masked") {
            val d = driver()
            val player = d.activePlayer!!
            val hidden = d.putFaceDown(player, "Disguised Angel", FaceDownMode.DISGUISE)

            // GameState.getBattlefield() deliberately hides phased-out permanents, so a naive
            // battlefield check unmasks them. Phasing never changes zones (CR 702.26d) — the
            // permanent is still on the battlefield, and its identity is still hidden.
            d.replaceState(
                d.state.updateEntity(hidden) { it.with(PhasedOutComponent(phasedOutByController = player)) }
            )
            d.state.getBattlefield() shouldNotContain hidden
            nameVisibleToAll(d.state, hidden, "Disguised Angel") shouldBe "Face-down creature"
        }

        test("a card face down in exile reads as a card, not a creature") {
            val d = driver()
            val player = d.activePlayer!!

            val exiled = d.putCardInExile(player, "Disguised Angel")
            d.replaceState(d.state.updateEntity(exiled) { it.with(FaceDownComponent) })

            // Not "Face-down creature" — an exiled face-down card has no characteristics to show
            // and is not a creature.
            nameVisibleToAll(d.state, exiled, "Disguised Angel") shouldBe "Face-down card"
        }
    }

    test("a face-down permanent's controller may still read its name where the text has an audience") {
        val d = driver()
        val controller = d.activePlayer!!
        val opponent = d.getOpponent(controller)
        val hidden = d.putFaceDown(controller, "Disguised Angel", FaceDownMode.DISGUISE)
        val visibility = Visibility(d.cardRegistry, conditionEvaluator = PredicateEvaluator(cardRegistry = null).conditions)

        // CR 708.5 — you may look at a face-down permanent you control. Text with an audience asks
        // the shared identity authority and picks its label from that answer; the flat game-log
        // strings have no audience to ask about and mask for everyone.
        visibility.isCardIdentityVisibleTo(d.state, Zone.BATTLEFIELD, hidden, controller) shouldBe true
        visibility.isCardIdentityVisibleTo(d.state, Zone.BATTLEFIELD, hidden, opponent) shouldBe false
        withClue("a spectator is never the player who may look") {
            visibility.isCardIdentityVisibleTo(
                d.state,
                Zone.BATTLEFIELD,
                hidden,
                controller,
                isSpectator = true,
            ) shouldBe false
        }
        withClue("the flat game-log string masks for everyone, controller included") {
            nameVisibleToAll(d.state, hidden, "Disguised Angel") shouldBe "Face-down creature"
        }
    }

    test("a decision summary does not name the face-down permanent it targeted") {
        val d = driver()
        val chooser = d.activePlayer!!
        val opponent = d.getOpponent(chooser)

        val hidden = d.putFaceDown(opponent, "Disguised Angel", FaceDownMode.DISGUISE)

        // A second legal target, so the choice is a genuine prompt rather than an auto-select.
        d.putCreatureOnBattlefield(chooser, "Grizzly Bears")

        d.giveMana(chooser, Color.BLACK, 3)
        val tapperCard = d.putCardInHand(chooser, "Test Tapper")
        d.submitSuccess(
            CastSpell(
                playerId = chooser,
                cardId = tapperCard,
                paymentStrategy = PaymentStrategy.FromPool,
            )
        )
        d.bothPass()

        d.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        d.submitTargetSelection(chooser, listOf(hidden))

        val log = d.logAsSeenBy(chooser)
        withClue("chooser's log: $log") {
            log.forEach { it shouldNotContain "Disguised Angel" }
            log.contains("You: (Test Tapper) Targeting Face-down creature") shouldBe true
        }
    }

    test("effect-created blocks retain public names after the hidden objects are revealed") {
        val d = driver()
        val player = d.activePlayer!!
        val opponent = d.getOpponent(player)
        val attacker = d.putFaceDown(player, "Disguised Angel", FaceDownMode.DISGUISE)
        val blocker = d.putFaceDown(opponent, "Disguised Angel", FaceDownMode.DISGUISE)
        d.addComponent(attacker, com.wingedsheep.engine.state.components.combat.AttackingComponent(opponent))
        val result = d.services.effectExecutorRegistry.execute(d.state,
            Effects.BecomeBlocking(EffectTarget.ContextTarget(0), EffectTarget.ContextTarget(1)),
            com.wingedsheep.engine.handlers.EffectContext(sourceId = null, controllerId = player,
                targets = listOf(ChosenTarget.Permanent(blocker), ChosenTarget.Permanent(attacker))))
        result.error shouldBe null
        val event = result.events.single() as com.wingedsheep.engine.core.BlocksCreatedEvent
        event.blockerNames[blocker] shouldBe FACE_DOWN_DISPLAY_NAME
        event.attackerNames[attacker] shouldBe FACE_DOWN_DISPLAY_NAME
        val revealed = result.state.updateEntity(attacker) { it.without<FaceDownComponent>() }
            .updateEntity(blocker) { it.without<FaceDownComponent>() }
        for (viewer in listOf(player, opponent, EntityId.of("spectator"))) {
            for (laterState in listOf(revealed, null)) {
                val description = ClientEventTransformer.transform(result.events, viewer, laterState).single().description
                description shouldNotContain "Disguised Angel"
            }
        }
    }

    test("a face-up permanent is still named normally") {
        val d = driver()
        val caster = d.activePlayer!!
        val opponent = d.getOpponent(caster)

        val cardId = d.putCardInHand(caster, "Disguised Angel")
        d.giveMana(caster, Color.WHITE, 4)
        d.submitSuccess(
            CastSpell(
                playerId = caster,
                cardId = cardId,
                paymentStrategy = PaymentStrategy.FromPool,
            )
        )
        d.bothPass()

        val opponentLog = d.logAsSeenBy(opponent)
        withClue("opponent's log: $opponentLog") {
            opponentLog.contains("Disguised Angel resolved") shouldBe true
            opponentLog.contains("Opponent's Disguised Angel entered the battlefield") shouldBe true
        }
    }

    test("spell-cast presentation round-trips and legacy events retain their safe stored names") {
        val json = Json { serializersModule = engineSerializersModule }
        val current: GameEvent = SpellCastEvent(
            spellEntityId = EntityId.of("spell"),
            cardName = FACE_DOWN_DISPLAY_NAME,
            casterId = EntityId.of("caster"),
            cardPresentation = EventCardPresentation(
                semanticName = "Disguised Angel",
                publicName = FACE_DOWN_DISPLAY_NAME,
                identityViewers = setOf(EntityId.of("caster")),
            ),
        )
        json.decodeFromString<GameEvent>(json.encodeToString(current)) shouldBe current

        // The raw event is trusted multi-recipient data. The transport-facing event contains only
        // the one projected label and cannot carry the underlying identity or its audience set.
        val opponentEvent: ClientEvent = ClientEventTransformer.transform(
            listOf(current), EntityId.of("opponent")
        ).single()
        val encodedOpponentEvent = json.encodeToString(opponentEvent)
        encodedOpponentEvent shouldNotContain "Disguised Angel"
        encodedOpponentEvent shouldNotContain "identityViewers"

        // Before viewer-specific capture, cardName was already the event-time public name. Keep
        // ordinary face-up history readable and preserve the generic name stored for face-down
        // casts rather than treating both legacy shapes as hidden.
        val legacyFaceUp = """{"type":"SpellCastEvent","spellEntityId":"face-up","cardName":"Disguised Angel","casterId":"caster"}"""
        val decodedFaceUp = json.decodeFromString<GameEvent>(legacyFaceUp) as SpellCastEvent
        decodedFaceUp.cardPresentation shouldBe null
        ClientEventTransformer.transform(listOf(decodedFaceUp), EntityId.of("opponent"))
            .filterIsInstance<ClientEvent.SpellCast>()
            .single().spellName shouldBe "Disguised Angel"

        val legacyFaceDown = """{"type":"SpellCastEvent","spellEntityId":"face-down","cardName":"Face-down creature","casterId":"caster"}"""
        val decodedFaceDown = json.decodeFromString<GameEvent>(legacyFaceDown) as SpellCastEvent
        decodedFaceDown.cardPresentation shouldBe null
        ClientEventTransformer.transform(listOf(decodedFaceDown), EntityId.of("opponent"))
            .filterIsInstance<ClientEvent.SpellCast>()
            .single().spellName shouldBe FACE_DOWN_DISPLAY_NAME
    }
})
