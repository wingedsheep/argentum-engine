package com.wingedsheep.engine.support

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.core.DistributionResponse
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.battlefield.ClassLevelComponent
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CantBeCounteredComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.DoubleFacedComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.state.components.identity.PlayerComponent
import com.wingedsheep.engine.state.components.identity.HexproofFromColorComponent
import com.wingedsheep.engine.state.components.identity.ToxicComponent
import com.wingedsheep.engine.state.components.identity.ProtectionComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.player.LandDropsComponent
import com.wingedsheep.engine.state.components.combat.AttackersDeclaredThisCombatComponent
import com.wingedsheep.engine.state.components.combat.BlockersDeclaredThisCombatComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.mechanics.layers.StaticAbilityHandler
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.view.ClientGameState
import com.wingedsheep.engine.view.ClientStateTransformer
import com.wingedsheep.engine.view.LegalActionEnricher
import com.wingedsheep.engine.view.LegalActionInfo
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.model.GameRng
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ProtectionScope
import io.kotest.core.spec.style.FunSpec
import java.util.concurrent.atomic.AtomicLong

/**
 * Base class for scenario-based tests that directly manipulate game state.
 *
 * This provides a cleaner testing API than the WebSocket-based GameServerTestBase
 * for testing game logic without network overhead.
 *
 * Lives in the engine's `testFixtures` so any module can author static-board scenario
 * tests against the real [ActionProcessor]. The card pool is [TestCards.all] (the full
 * [com.wingedsheep.mtg.sets.MtgSetCatalog] plus test-only cards) together with the
 * predefined tokens.
 */
abstract class ScenarioTestBase : FunSpec() {

    protected val cardRegistry = CardRegistry().apply {
        register(TestCards.all)
        register(PredefinedTokens.allTokens)
    }
    protected val actionProcessor = ActionProcessor(cardRegistry)
    protected val stateTransformer = ClientStateTransformer(cardRegistry)

    /**
     * Builder for constructing test scenarios with specific game states.
     */
    inner class ScenarioBuilder {
        private val entityIdCounter = AtomicLong(1000)
        // Seed the deterministic RNG with fresh entropy per scenario build so coin flips and other
        // "at random" effects vary across repeated builds — scenario tests that run a setup N times
        // to observe both branches of a flip (Goblin Psychopath, Grip of Chaos) depend on this. A
        // bare GameState() would reuse seed 0 every build and freeze the outcome.
        private var state = GameState(rng = GameRng.seeded(System.nanoTime()))

        private var player1Id: EntityId? = null
        private var player2Id: EntityId? = null

        /**
         * Initialize the scenario with two players.
         */
        fun withPlayers(
            player1Name: String = "Player1",
            player2Name: String = "Player2"
        ): ScenarioBuilder {
            player1Id = EntityId.of("player-1")
            player2Id = EntityId.of("player-2")

            // Create player entities
            val p1Container = ComponentContainer.of(
                PlayerComponent(player1Name),
                LifeTotalComponent(20),
                ManaPoolComponent(),
                LandDropsComponent(remaining = 1, maxPerTurn = 1)
            )

            val p2Container = ComponentContainer.of(
                PlayerComponent(player2Name),
                LifeTotalComponent(20),
                ManaPoolComponent(),
                LandDropsComponent(remaining = 1, maxPerTurn = 1)
            )

            state = state
                .withEntity(player1Id!!, p1Container)
                .withEntity(player2Id!!, p2Container)
                .copy(
                    turnOrder = listOf(player1Id!!, player2Id!!),
                    activePlayerId = player1Id,
                    priorityPlayerId = player1Id,
                    phase = Phase.PRECOMBAT_MAIN,
                    step = Step.PRECOMBAT_MAIN,
                    turnNumber = 1
                )

            // Initialize empty zones for both players
            for (playerId in listOf(player1Id!!, player2Id!!)) {
                for (zoneType in listOf(Zone.HAND, Zone.LIBRARY, Zone.GRAVEYARD, Zone.BATTLEFIELD)) {
                    val zoneKey = ZoneKey(playerId, zoneType)
                    state = state.copy(zones = state.zones + (zoneKey to emptyList()))
                }
            }

            return this
        }

        /**
         * Add a card to a player's hand.
         */
        fun withCardInHand(playerNumber: Int, cardName: String): ScenarioBuilder {
            val playerId = if (playerNumber == 1) player1Id!! else player2Id!!
            val cardId = createCard(cardName, playerId)
            state = state.addToZone(ZoneKey(playerId, Zone.HAND), cardId)
            return this
        }

        /**
         * Add multiple copies of a card to a player's hand.
         */
        fun withCardsInHand(playerNumber: Int, cardName: String, count: Int): ScenarioBuilder {
            repeat(count) { withCardInHand(playerNumber, cardName) }
            return this
        }

        /**
         * Add a card to the battlefield under a player's control.
         * By default, removes summoning sickness (as if it's been there).
         *
         * Set [isToken] to mark the permanent as a token (adds [TokenComponent]) — needed so
         * `IsToken` / `IsNontoken` filters and token-only sacrifice costs evaluate correctly.
         * The permanent's stats/types still come from the named (token) card definition, e.g.
         * a predefined token like "Treasure" or a creature-token script.
         */
        fun withCardOnBattlefield(
            playerNumber: Int,
            cardName: String,
            tapped: Boolean = false,
            summoningSickness: Boolean = false,
            classLevel: Int? = null,
            isToken: Boolean = false
        ): ScenarioBuilder {
            val playerId = if (playerNumber == 1) player1Id!! else player2Id!!
            val cardId = createCard(cardName, playerId)

            // Add to battlefield
            state = state.addToZone(ZoneKey(playerId, Zone.BATTLEFIELD), cardId)

            // Update card entity with battlefield-specific components
            var container = state.getEntity(cardId)!!
            container = container.with(ControllerComponent(playerId))

            if (tapped) {
                container = container.with(TappedComponent)
            }

            if (summoningSickness) {
                container = container.with(SummoningSicknessComponent)
            }

            if (isToken) {
                container = container.with(TokenComponent)
            }

            // Add continuous effects from static abilities (e.g., "Other creatures you control have...")
            // and replacement effects (e.g., PreventDamage from Daunting Defender)
            val cardDef = cardRegistry.getCard(cardName)
            if (cardDef != null) {
                // Add ClassLevelComponent for Class enchantments BEFORE static/replacement effects
                // so that class-level-gated abilities are included
                if (cardDef.isClass) {
                    container = container.with(ClassLevelComponent(currentLevel = classLevel ?: 1))
                }

                val staticHandler = StaticAbilityHandler(cardRegistry)
                container = staticHandler.addContinuousEffectComponent(container, cardDef)
                container = staticHandler.addReplacementEffectComponent(container, cardDef)

                // Rule 712.8a: if placing a DFC back face directly on the battlefield, add
                // DoubleFacedComponent with the saved front-face card so ZoneTransitionService
                // can restore it when the entity leaves.
                val frontFaceDef = cardRegistry.getFrontFace(cardName)
                if (frontFaceDef != null) {
                    val frontFaceCard = state.getEntity(cardId)?.get<CardComponent>()?.let {
                        CardComponent(
                            cardDefinitionId = frontFaceDef.name,
                            name = frontFaceDef.name,
                            manaCost = frontFaceDef.manaCost,
                            typeLine = frontFaceDef.typeLine,
                            oracleText = frontFaceDef.oracleText,
                            colors = frontFaceDef.colors,
                            baseKeywords = frontFaceDef.keywords,
                            baseFlags = frontFaceDef.flags,
                            baseStats = frontFaceDef.creatureStats,
                            ownerId = it.ownerId,
                            spellEffect = frontFaceDef.spellEffect,
                            imageUri = frontFaceDef.metadata.imageUri,
                        )
                    }
                    if (frontFaceCard != null) {
                        container = container.with(
                            DoubleFacedComponent(
                                frontCardDefinitionId = frontFaceDef.name,
                                backCardDefinitionId = cardName,
                                currentFace = DoubleFacedComponent.Face.BACK,
                                frontFaceCard = frontFaceCard
                            )
                        )
                    }
                }
            }

            state = state.withEntity(cardId, container)
            return this
        }

        /**
         * Put an Aura (or other attachment) on the battlefield already attached to a permanent
         * named [hostName] (which must already be on the battlefield). Wires the Aura's
         * [AttachedToComponent] and the host's [AttachmentsComponent] and registers the Aura's
         * static/replacement effects, so its "enchanted permanent ..." abilities apply immediately —
         * the deterministic alternative to casting + resolving the Aura. The attachment owner/
         * controller is [playerNumber].
         */
        fun withCardAttachedTo(playerNumber: Int, auraName: String, hostName: String): ScenarioBuilder {
            val playerId = if (playerNumber == 1) player1Id!! else player2Id!!
            val hostId = findPermanentInBuilder(hostName)
                ?: error("Host '$hostName' must already be on the battlefield before attaching '$auraName'")

            withCardOnBattlefield(playerNumber, auraName)
            val auraId = findPermanentInBuilder(auraName)
                ?: error("Failed to place aura '$auraName' on the battlefield")

            state = state.updateEntity(auraId) { it.with(AttachedToComponent(hostId)).with(ControllerComponent(playerId)) }
            state = state.updateEntity(hostId) { container ->
                val existing = container.get<AttachmentsComponent>()?.attachedIds ?: emptyList()
                container.with(AttachmentsComponent(existing + auraId))
            }
            return this
        }

        /** Locate a permanent by name on the battlefield during scenario construction. */
        private fun findPermanentInBuilder(name: String): EntityId? =
            state.getBattlefield().firstOrNull { id ->
                state.getEntity(id)?.get<CardComponent>()?.name == name
            }

        /**
         * Add multiple lands to the battlefield (untapped, ready to tap for mana).
         */
        fun withLandsOnBattlefield(playerNumber: Int, landName: String, count: Int): ScenarioBuilder {
            repeat(count) { withCardOnBattlefield(playerNumber, landName, tapped = false) }
            return this
        }

        /**
         * Add a card to a player's library.
         */
        fun withCardInLibrary(playerNumber: Int, cardName: String): ScenarioBuilder {
            val playerId = if (playerNumber == 1) player1Id!! else player2Id!!
            val cardId = createCard(cardName, playerId)
            state = state.addToZone(ZoneKey(playerId, Zone.LIBRARY), cardId)
            return this
        }

        /**
         * Add a card to a player's graveyard.
         */
        fun withCardInGraveyard(playerNumber: Int, cardName: String): ScenarioBuilder {
            val playerId = if (playerNumber == 1) player1Id!! else player2Id!!
            val cardId = createCard(cardName, playerId)
            state = state.addToZone(ZoneKey(playerId, Zone.GRAVEYARD), cardId)
            return this
        }

        /**
         * Add a card to a player's exile zone.
         */
        fun withCardInExile(playerNumber: Int, cardName: String): ScenarioBuilder {
            val playerId = if (playerNumber == 1) player1Id!! else player2Id!!
            val cardId = createCard(cardName, playerId)
            state = state.addToZone(ZoneKey(playerId, Zone.EXILE), cardId)
            return this
        }

        /**
         * Add a card to a player's sideboard ("outside the game", CR 100.4 / 400.11a). Used to
         * set up "wish" effects (Burning Wish, …) that fetch from the sideboard.
         */
        fun withCardInSideboard(playerNumber: Int, cardName: String): ScenarioBuilder {
            val playerId = if (playerNumber == 1) player1Id!! else player2Id!!
            val cardId = createCard(cardName, playerId)
            state = state.addToZone(ZoneKey(playerId, Zone.SIDEBOARD), cardId)
            return this
        }

        /**
         * Add a card to a player's command zone (e.g. a commander or a Momir Basic Vanguard
         * avatar). The card gets a [VanguardAvatarComponent] when its type line is Vanguard so
         * the command-zone activated-ability path treats it as an avatar.
         */
        fun withCardInCommandZone(playerNumber: Int, cardName: String): ScenarioBuilder {
            val playerId = if (playerNumber == 1) player1Id!! else player2Id!!
            val cardId = createCard(cardName, playerId)
            val cardDef = cardRegistry.getCard(cardName)
            if (cardDef?.typeLine?.cardTypes?.contains(com.wingedsheep.sdk.core.CardType.VANGUARD) == true) {
                state = state.updateEntity(cardId) { c ->
                    c.with(com.wingedsheep.engine.state.components.identity.VanguardAvatarComponent(ownerId = playerId))
                }
            }
            state = state.addToZone(ZoneKey(playerId, Zone.COMMAND), cardId)
            return this
        }

        /**
         * Set the game [com.wingedsheep.sdk.core.Format] (e.g. [com.wingedsheep.sdk.core.Format.MomirBasic]
         * so the random-creature-token effect can read its eligible-creature pool).
         */
        fun withFormat(format: com.wingedsheep.sdk.core.Format): ScenarioBuilder {
            state = state.copy(format = format)
            return this
        }

        /**
         * Pin the deterministic RNG seed (overriding the per-build entropy seed). Use when a test
         * must reproduce an "at random" outcome exactly — e.g. Momir's random-creature pick.
         */
        fun withRngSeed(seed: Long): ScenarioBuilder {
            state = state.copy(rng = com.wingedsheep.sdk.model.GameRng.seeded(seed))
            return this
        }

        /**
         * Set a player's life total.
         */
        fun withLifeTotal(playerNumber: Int, life: Int): ScenarioBuilder {
            val playerId = if (playerNumber == 1) player1Id!! else player2Id!!
            state = state.updateEntity(playerId) { container ->
                container.with(LifeTotalComponent(life))
            }
            return this
        }

        /**
         * Pretend a player has already drawn [count] cards this turn (sets
         * `CardsDrawnThisTurnComponent`). Used to exercise "as long as you've drawn N cards this
         * turn" conditions (Gwaihir the Windlord) without scripting the draws.
         */
        fun withCardsDrawnThisTurn(playerNumber: Int, count: Int): ScenarioBuilder {
            val playerId = if (playerNumber == 1) player1Id!! else player2Id!!
            state = state.updateEntity(playerId) { container ->
                container.with(
                    com.wingedsheep.engine.state.components.player.CardsDrawnThisTurnComponent(count)
                )
            }
            return this
        }

        /**
         * Set the current phase and step.
         */
        fun inPhase(phase: Phase, step: Step): ScenarioBuilder {
            state = state.copy(phase = phase, step = step)
            return this
        }

        /**
         * Set the turn number (default is 1).
         */
        fun withTurnNumber(turnNumber: Int): ScenarioBuilder {
            state = state.copy(turnNumber = turnNumber)
            return this
        }

        /**
         * Set the active player (whose turn it is).
         */
        fun withActivePlayer(playerNumber: Int): ScenarioBuilder {
            val playerId = if (playerNumber == 1) player1Id!! else player2Id!!
            state = state.copy(activePlayerId = playerId, priorityPlayerId = playerId)
            return this
        }

        /**
         * Set the priority player.
         */
        fun withPriorityPlayer(playerNumber: Int): ScenarioBuilder {
            val playerId = if (playerNumber == 1) player1Id!! else player2Id!!
            state = state.copy(priorityPlayerId = playerId)
            return this
        }

        /**
         * Build the scenario, returning a TestGame instance for assertions.
         */
        fun build(): TestGame {
            return TestGame(
                state = state,
                player1Id = player1Id!!,
                player2Id = player2Id!!,
                cardRegistry = cardRegistry,
                actionProcessor = actionProcessor,
                stateTransformer = stateTransformer
            )
        }

        private fun createCard(cardName: String, ownerId: EntityId): EntityId {
            val cardDef = cardRegistry.getCard(cardName)
                ?: error("Card not found in registry: $cardName")

            val cardId = EntityId.of("card-${entityIdCounter.incrementAndGet()}")

            val cardComponent = CardComponent(
                cardDefinitionId = cardDef.name,
                name = cardDef.name,
                manaCost = cardDef.manaCost,
                typeLine = cardDef.typeLine,
                oracleText = cardDef.oracleText,
                colors = cardDef.colors,
                baseKeywords = cardDef.keywords,
                baseFlags = cardDef.flags,
                baseStats = cardDef.creatureStats,
                ownerId = ownerId,
                spellEffect = cardDef.spellEffect,
                imageUri = cardDef.metadata.imageUri,
                hasNonManaActivatedAbility = cardDef.hasNonManaActivatedAbility,
                originalSetCode = cardDef.setCode,
            )

            var container = ComponentContainer.of(
                cardComponent,
                OwnerComponent(ownerId),
                ControllerComponent(ownerId)
            )

            if (cardDef.script.cantBeCountered) {
                container = container.with(CantBeCounteredComponent)
            }

            if (cardDef.script.cantBeCopied) {
                container = container.with(com.wingedsheep.engine.state.components.identity.CantBeCopiedComponent)
            }

            // Attach ProtectionComponent for cards with static protection from color/subtype
            val protections = cardDef.keywordAbilities.filterIsInstance<KeywordAbility.Protection>()
            val protectionColors = protections.flatMap { p ->
                when (val s = p.scope) {
                    is ProtectionScope.Color -> listOf(s.color)
                    is ProtectionScope.Colors -> s.colors
                    else -> emptyList()
                }
            }.toSet()
            val protectionSubtypes = protections.mapNotNull {
                (it.scope as? ProtectionScope.Subtype)?.subtype
            }.toSet()
            val protectionSupertypes = protections.mapNotNull {
                (it.scope as? ProtectionScope.Supertype)?.supertype
            }.toSet()
            if (protectionColors.isNotEmpty() || protectionSubtypes.isNotEmpty() || protectionSupertypes.isNotEmpty()) {
                container = container.with(ProtectionComponent(protectionColors, protectionSubtypes, protectionSupertypes))
            }

            // Attach HexproofFromColorComponent for cards with hexproof from color
            val hexproofFromColors = cardDef.keywordAbilities
                .filterIsInstance<KeywordAbility.Hexproof>()
                .mapNotNull { (it.scope as? ProtectionScope.Color)?.color }
                .toSet()
            if (hexproofFromColors.isNotEmpty()) {
                container = container.with(HexproofFromColorComponent(hexproofFromColors))
            }

            // Attach ToxicComponent for cards with printed Toxic N (sums per Rule 702.164b)
            val toxicAmount = cardDef.keywordAbilities
                .filterIsInstance<KeywordAbility.Numeric>()
                .filter { it.keyword == Keyword.TOXIC }
                .sumOf { it.n }
            if (toxicAmount > 0) {
                container = container.with(ToxicComponent(toxicAmount))
            }

            state = state.withEntity(cardId, container)
            return cardId
        }
    }

    /**
     * Create a new scenario builder.
     */
    protected fun scenario(): ScenarioBuilder = ScenarioBuilder()

    /**
     * Represents a test game that can be queried and advanced.
     */
    class TestGame(
        var state: GameState,
        val player1Id: EntityId,
        val player2Id: EntityId,
        private val cardRegistry: CardRegistry,
        private val actionProcessor: ActionProcessor,
        private val stateTransformer: ClientStateTransformer
    ) {
        /**
         * Execute an action and update the state.
         */
        fun execute(action: GameAction): ExecutionResult {
            val result = actionProcessor.process(state, action).result
            if (result.error == null) {
                state = result.state
            }
            return result
        }

        /**
         * Cast a spell by name from a player's hand, optionally targeting an entity (permanent).
         */
        fun castSpell(
            playerNumber: Int,
            spellName: String,
            targetId: EntityId? = null
        ): ExecutionResult {
            val playerId = if (playerNumber == 1) player1Id else player2Id
            val hand = state.getHand(playerId)
            val cardId = hand.find { entityId ->
                state.getEntity(entityId)?.get<CardComponent>()?.name == spellName
            } ?: error("Card '$spellName' not found in player $playerNumber's hand")

            val targets = if (targetId != null) {
                listOf(ChosenTarget.Permanent(targetId))
            } else {
                emptyList()
            }

            return execute(CastSpell(playerId, cardId, targets))
        }

        /**
         * Cast a modal spell by name, choosing a specific mode.
         * Used for modal spells (including Gift spells modeled as choose-one).
         */
        fun castSpellWithMode(
            playerNumber: Int,
            spellName: String,
            modeIndex: Int,
            targetId: EntityId? = null
        ): ExecutionResult {
            val playerId = if (playerNumber == 1) player1Id else player2Id
            val hand = state.getHand(playerId)
            val cardId = hand.find { entityId ->
                state.getEntity(entityId)?.get<CardComponent>()?.name == spellName
            } ?: error("Card '$spellName' not found in player $playerNumber's hand")

            val targets = if (targetId != null) {
                listOf(ChosenTarget.Permanent(targetId))
            } else {
                emptyList()
            }

            return execute(CastSpell(
                playerId,
                cardId,
                targets,
                chosenModes = listOf(modeIndex),
                modeTargetsOrdered = if (targets.isNotEmpty()) listOf(targets) else emptyList()
            ))
        }

        /**
         * Cast a spell by name from a player's graveyard (for cards with MayCastSelfFromZones).
         */
        fun castSpellFromGraveyard(
            playerNumber: Int,
            spellName: String,
            targetId: EntityId? = null
        ): ExecutionResult {
            val playerId = if (playerNumber == 1) player1Id else player2Id
            val graveyard = state.getGraveyard(playerId)
            val cardId = graveyard.find { entityId ->
                state.getEntity(entityId)?.get<CardComponent>()?.name == spellName
            } ?: error("Card '$spellName' not found in player $playerNumber's graveyard")

            val targets = if (targetId != null) {
                listOf(ChosenTarget.Permanent(targetId))
            } else {
                emptyList()
            }

            return execute(CastSpell(playerId, cardId, targets))
        }

        /**
         * Cast a spell by name from a player's exile zone (for cards with MayCastSelfFromZones).
         */
        fun castSpellFromExile(
            playerNumber: Int,
            spellName: String,
            targetId: EntityId? = null
        ): ExecutionResult {
            val playerId = if (playerNumber == 1) player1Id else player2Id
            val exile = state.getExile(playerId)
            val cardId = exile.find { entityId ->
                state.getEntity(entityId)?.get<CardComponent>()?.name == spellName
            } ?: error("Card '$spellName' not found in player $playerNumber's exile")

            val targets = if (targetId != null) {
                listOf(ChosenTarget.Permanent(targetId))
            } else {
                emptyList()
            }

            return execute(CastSpell(playerId, cardId, targets))
        }

        /**
         * Cast a spell using an alternative casting cost (e.g., Jodah's {W}{U}{B}{R}{G}).
         */
        fun castSpellWithAlternativeCost(
            playerNumber: Int,
            spellName: String,
            targetId: EntityId? = null
        ): ExecutionResult {
            val playerId = if (playerNumber == 1) player1Id else player2Id
            val hand = state.getHand(playerId)
            val cardId = hand.find { entityId ->
                state.getEntity(entityId)?.get<CardComponent>()?.name == spellName
            } ?: error("Card '$spellName' not found in player $playerNumber's hand")

            val targets = if (targetId != null) {
                listOf(ChosenTarget.Permanent(targetId))
            } else {
                emptyList()
            }

            return execute(CastSpell(playerId, cardId, targets, useAlternativeCost = true))
        }

        /**
         * Cast a spell for its Cleave cost (CR 702.148), optionally targeting a permanent. Cleave
         * is an alternative cost, so this drives [CastSpell.useAlternativeCost] gated on
         * [AlternativeCostType.CLEAVE]; the handler swaps in the brackets-removed
         * effect / target-requirement variant.
         *
         * [xValue] threads a chosen X through the cast when the cleave cost itself carries {X}
         * (Lantern Flare's `Cleave {X}{R}{W}`). The cast handler passes [CastSpell.xValue] into
         * resolution unconditionally — regardless of the cleave flag — so the cleaved
         * `DynamicAmount.XValue` reads it. Leave `null` for cleave costs without {X}.
         */
        fun castSpellWithCleave(
            playerNumber: Int,
            spellName: String,
            targetId: EntityId? = null,
            xValue: Int? = null
        ): ExecutionResult {
            val playerId = if (playerNumber == 1) player1Id else player2Id
            val hand = state.getHand(playerId)
            val cardId = hand.find { entityId ->
                state.getEntity(entityId)?.get<CardComponent>()?.name == spellName
            } ?: error("Card '$spellName' not found in player $playerNumber's hand")

            val targets = if (targetId != null) {
                listOf(ChosenTarget.Permanent(targetId))
            } else {
                emptyList()
            }

            return execute(CastSpell(
                playerId, cardId, targets,
                xValue = xValue,
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.CLEAVE
            ))
        }

        /**
         * Cast a spell for its Cleave cost (CR 702.148), targeting a spell on the stack (e.g. the
         * cleaved Wash Away, which counters any spell). Mirrors [castSpellTargetingStackSpell] but
         * pays the cleave alternative cost.
         */
        fun castSpellWithCleaveTargetingStackSpell(
            playerNumber: Int,
            spellName: String,
            targetSpellName: String
        ): ExecutionResult {
            val playerId = if (playerNumber == 1) player1Id else player2Id
            val hand = state.getHand(playerId)
            val cardId = hand.find { entityId ->
                state.getEntity(entityId)?.get<CardComponent>()?.name == spellName
            } ?: error("Card '$spellName' not found in player $playerNumber's hand")

            val targetSpellId = state.stack.find { entityId ->
                state.getEntity(entityId)?.get<CardComponent>()?.name == targetSpellName
            } ?: error("Spell '$targetSpellName' not found on stack")

            return execute(CastSpell(
                playerId, cardId,
                targets = listOf(ChosenTarget.Spell(targetSpellId)),
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.CLEAVE
            ))
        }

        /**
         * Cast a spell using a self-alternative cost that requires tapping permanents.
         * Used for cards like Zahid, Djinn of the Lamp.
         */
        fun castSpellWithSelfAlternativeCost(
            playerNumber: Int,
            spellName: String,
            tapPermanentName: String
        ): ExecutionResult {
            val playerId = if (playerNumber == 1) player1Id else player2Id
            val hand = state.getHand(playerId)
            val cardId = hand.find { entityId ->
                state.getEntity(entityId)?.get<CardComponent>()?.name == spellName
            } ?: error("Card '$spellName' not found in player $playerNumber's hand")

            val battlefield = state.getBattlefield(playerId)
            val tapId = battlefield.find { entityId ->
                state.getEntity(entityId)?.get<CardComponent>()?.name == tapPermanentName
            } ?: error("Permanent '$tapPermanentName' not found on player $playerNumber's battlefield")

            return execute(CastSpell(
                playerId, cardId,
                useAlternativeCost = true,
                additionalCostPayment = com.wingedsheep.sdk.scripting.AdditionalCostPayment(
                    tappedPermanents = listOf(tapId)
                )
            ))
        }

        /**
         * Cast a spell by name, sacrificing a creature and targeting a player.
         */
        fun castSpellWithSacrifice(
            playerNumber: Int,
            spellName: String,
            sacrificeCreatureName: String,
            targetPlayerNumber: Int
        ): ExecutionResult {
            val playerId = if (playerNumber == 1) player1Id else player2Id
            val targetPlayerId = if (targetPlayerNumber == 1) player1Id else player2Id
            val hand = state.getHand(playerId)
            val cardId = hand.find { entityId ->
                state.getEntity(entityId)?.get<CardComponent>()?.name == spellName
            } ?: error("Card '$spellName' not found in player $playerNumber's hand")

            val sacrificeId = state.getBattlefield().find { entityId ->
                val container = state.getEntity(entityId) ?: return@find false
                container.get<CardComponent>()?.name == sacrificeCreatureName &&
                    container.get<ControllerComponent>()?.playerId == playerId
            } ?: error("Creature '$sacrificeCreatureName' not found on player $playerNumber's battlefield")

            val targets = listOf(ChosenTarget.Player(targetPlayerId))
            val payment = com.wingedsheep.sdk.scripting.AdditionalCostPayment(
                sacrificedPermanents = listOf(sacrificeId)
            )
            return execute(CastSpell(playerId, cardId, targets, additionalCostPayment = payment))
        }

        /**
         * Cast a spell by name from a player's hand, targeting a player.
         */
        fun castSpellTargetingPlayer(
            playerNumber: Int,
            spellName: String,
            targetPlayerNumber: Int
        ): ExecutionResult {
            val playerId = if (playerNumber == 1) player1Id else player2Id
            val targetPlayerId = if (targetPlayerNumber == 1) player1Id else player2Id
            val hand = state.getHand(playerId)
            val cardId = hand.find { entityId ->
                state.getEntity(entityId)?.get<CardComponent>()?.name == spellName
            } ?: error("Card '$spellName' not found in player $playerNumber's hand")

            val targets = listOf(ChosenTarget.Player(targetPlayerId))
            return execute(CastSpell(playerId, cardId, targets))
        }

        /**
         * Cast a spell by name from a player's hand, targeting a card in a graveyard.
         */
        fun castSpellTargetingGraveyardCard(
            playerNumber: Int,
            spellName: String,
            targetCardIds: List<EntityId>
        ): ExecutionResult {
            val playerId = if (playerNumber == 1) player1Id else player2Id
            val hand = state.getHand(playerId)
            val cardId = hand.find { entityId ->
                state.getEntity(entityId)?.get<CardComponent>()?.name == spellName
            } ?: error("Card '$spellName' not found in player $playerNumber's hand")

            val targets = targetCardIds.map { targetCardId ->
                ChosenTarget.Card(targetCardId, playerId, Zone.GRAVEYARD)
            }
            return execute(CastSpell(playerId, cardId, targets))
        }

        /**
         * Cast a spell with X in its mana cost.
         * @param playerNumber The player casting the spell (1 or 2)
         * @param spellName The name of the spell to cast
         * @param xValue The value chosen for X
         * @param targetId Optional target for targeted spells
         */
        fun castXSpell(
            playerNumber: Int,
            spellName: String,
            xValue: Int,
            targetId: EntityId? = null
        ): ExecutionResult {
            val playerId = if (playerNumber == 1) player1Id else player2Id
            val hand = state.getHand(playerId)
            val cardId = hand.find { entityId ->
                state.getEntity(entityId)?.get<CardComponent>()?.name == spellName
            } ?: error("Card '$spellName' not found in player $playerNumber's hand")

            val targets = if (targetId != null) {
                listOf(ChosenTarget.Permanent(targetId))
            } else {
                emptyList()
            }

            return execute(CastSpell(playerId, cardId, targets, xValue))
        }

        /**
         * Cycle a card by name from a player's hand.
         * The player pays the cycling cost, discards the card, and draws a card.
         */
        fun cycleCard(playerNumber: Int, cardName: String): ExecutionResult {
            val playerId = if (playerNumber == 1) player1Id else player2Id
            val hand = state.getHand(playerId)
            val cardId = hand.find { entityId ->
                state.getEntity(entityId)?.get<CardComponent>()?.name == cardName
            } ?: error("Card '$cardName' not found in player $playerNumber's hand")

            return execute(CycleCard(playerId, cardId))
        }

        /**
         * Typecycle a card by name from a player's hand.
         * The player pays the typecycling cost, discards the card, and searches their
         * library for a card of the specified type.
         */
        fun typecycleCard(playerNumber: Int, cardName: String): ExecutionResult {
            val playerId = if (playerNumber == 1) player1Id else player2Id
            val hand = state.getHand(playerId)
            val cardId = hand.find { entityId ->
                state.getEntity(entityId)?.get<CardComponent>()?.name == cardName
            } ?: error("Card '$cardName' not found in player $playerNumber's hand")

            return execute(TypecycleCard(playerId, cardId))
        }

        /**
         * Pass priority for the player who currently has it.
         */
        fun passPriority(): ExecutionResult {
            val playerId = state.priorityPlayerId ?: error("No player has priority")
            return execute(PassPriority(playerId))
        }

        /**
         * Pass priority for both players to resolve the stack.
         * Note: This stops when a pending decision is created (the caller should handle it).
         */
        fun resolveStack(): List<ExecutionResult> {
            val results = mutableListOf<ExecutionResult>()
            var iterations = 0
            while (state.stack.isNotEmpty() && state.pendingDecision == null && iterations++ < 20) {
                val result = passPriority()
                results.add(result)
                if (result.error != null) {
                    break  // Stop on error
                }
                if (state.stack.isNotEmpty() && state.pendingDecision == null) {
                    val result2 = passPriority()
                    results.add(result2)
                    if (result2.error != null) {
                        break  // Stop on error
                    }
                }
            }
            return results
        }

        /**
         * Get the client-facing state for a player.
         */
        fun getClientState(playerNumber: Int): ClientGameState {
            val playerId = if (playerNumber == 1) player1Id else player2Id
            return stateTransformer.transform(state, playerId)
        }

        // Built lazily and shared across calls; both are stateless (they take the
        // current [state] as an argument), so a single pair serves the whole game.
        private val legalActionEngine by lazy {
            val services = EngineServices(cardRegistry)
            LegalActionEnumerator(
                services.cardRegistry, services.manaSolver, services.costCalculator,
                services.predicateEvaluator, services.conditionEvaluator, services.turnManager
            ) to LegalActionEnricher(services.manaSolver, services.cardRegistry)
        }

        /**
         * Enumerate the enriched legal actions a player may take right now.
         *
         * Mirrors `GameSession.getLegalActions` (including the priority/actor gating and the
         * pending-decision guard) so legal-action assertions read identically to how they did
         * against the server harness — but the computation is pure engine
         * ([LegalActionEnumerator] + [LegalActionEnricher], both in `rules-engine`).
         */
        fun getLegalActions(playerNumber: Int): List<LegalActionInfo> {
            val playerId = if (playerNumber == 1) player1Id else player2Id
            val priorityPlayer = state.priorityPlayerId ?: return emptyList()
            if (state.actorFor(priorityPlayer) != playerId) return emptyList()
            if (state.pendingDecision != null) return emptyList()
            val (enumerator, enricher) = legalActionEngine
            val engineActions = enumerator.enumerate(state, priorityPlayer)
            return enricher.enrich(engineActions, state, priorityPlayer)
        }

        /**
         * Find a permanent on the battlefield by name.
         */
        fun findPermanent(name: String): EntityId? {
            return state.getBattlefield().find { entityId ->
                state.getEntity(entityId)?.get<CardComponent>()?.name == name
            }
        }

        /**
         * Find all permanents on the battlefield with the given name (e.g. multiple
         * copies of the same token or card).
         */
        fun findPermanents(name: String): List<EntityId> {
            return state.getBattlefield().filter { entityId ->
                state.getEntity(entityId)?.get<CardComponent>()?.name == name
            }
        }

        /**
         * Check if a card with the given name is in a player's graveyard.
         */
        fun isInGraveyard(playerNumber: Int, cardName: String): Boolean {
            val playerId = if (playerNumber == 1) player1Id else player2Id
            return state.getGraveyard(playerId).any { entityId ->
                state.getEntity(entityId)?.get<CardComponent>()?.name == cardName
            }
        }

        /**
         * Check if a card with the given name is on the battlefield.
         */
        fun isOnBattlefield(cardName: String): Boolean {
            return findPermanent(cardName) != null
        }

        /**
         * Check if a card with the given name is in a player's sideboard ("outside the game").
         */
        fun isInSideboard(playerNumber: Int, cardName: String): Boolean {
            val playerId = if (playerNumber == 1) player1Id!! else player2Id!!
            return state.getZone(playerId, Zone.SIDEBOARD).any { entityId ->
                state.getEntity(entityId)?.get<CardComponent>()?.name == cardName
            }
        }

        /** Number of cards in a player's sideboard. */
        fun sideboardSize(playerNumber: Int): Int {
            val playerId = if (playerNumber == 1) player1Id!! else player2Id!!
            return state.getZone(playerId, Zone.SIDEBOARD).size
        }

        /**
         * Check if a card with the given name is in a player's exile zone.
         */
        fun isInExile(playerNumber: Int, cardName: String): Boolean {
            val playerId = if (playerNumber == 1) player1Id!! else player2Id!!
            return state.getExile(playerId).any { entityId ->
                state.getEntity(entityId)?.get<CardComponent>()?.name == cardName
            }
        }

        /**
         * Get a player's library size.
         */
        fun librarySize(playerNumber: Int): Int {
            val playerId = if (playerNumber == 1) player1Id else player2Id
            return state.getLibrary(playerId).size
        }

        /**
         * Get a player's graveyard size.
         */
        fun graveyardSize(playerNumber: Int): Int {
            val playerId = if (playerNumber == 1) player1Id else player2Id
            return state.getGraveyard(playerId).size
        }

        /**
         * Get a player's hand size.
         */
        fun handSize(playerNumber: Int): Int {
            val playerId = if (playerNumber == 1) player1Id else player2Id
            return state.getHand(playerId).size
        }

        /**
         * Check if there's a pending decision.
         */
        fun hasPendingDecision(): Boolean = state.pendingDecision != null

        /**
         * Get the pending decision, if any.
         */
        fun getPendingDecision(): PendingDecision? = state.pendingDecision

        /**
         * Submit a decision response.
         */
        fun submitDecision(response: DecisionResponse): ExecutionResult {
            val playerId = state.pendingDecision?.playerId
                ?: error("No pending decision to respond to")
            return execute(SubmitDecision(playerId, response))
        }

        /**
         * Submit a card selection decision (select specific cards).
         */
        fun selectCards(cardIds: List<EntityId>): ExecutionResult {
            val decisionId = state.pendingDecision?.id
                ?: error("No pending decision to respond to")
            return submitDecision(CardsSelectedResponse(decisionId, cardIds))
        }

        /**
         * Submit a "skip" decision (select no cards).
         */
        fun skipSelection(): ExecutionResult {
            val decisionId = state.pendingDecision?.id
                ?: error("No pending decision to respond to")
            return submitDecision(CardsSelectedResponse(decisionId, emptyList()))
        }

        /**
         * Resolve a pending [ReorderLibraryDecision] (e.g. a Scry reorder prompt) by keeping
         * the cards in their current order. A reorder decision requires an [OrderedResponse];
         * [skipSelection] submits the wrong response type and would be rejected.
         */
        fun keepLibraryOrder(): ExecutionResult {
            val decision = state.pendingDecision as? ReorderLibraryDecision
                ?: error("No pending ReorderLibraryDecision to respond to")
            return submitDecision(OrderedResponse(decision.id, decision.cards))
        }

        /**
         * Submit a number choice decision.
         */
        fun chooseNumber(number: Int): ExecutionResult {
            val decisionId = state.pendingDecision?.id
                ?: error("No pending decision to respond to")
            return submitDecision(NumberChosenResponse(decisionId, number))
        }

        /**
         * Find cards in a player's hand by name.
         */
        fun findCardsInHand(playerNumber: Int, cardName: String): List<EntityId> {
            val playerId = if (playerNumber == 1) player1Id else player2Id
            return state.getHand(playerId).filter { entityId ->
                state.getEntity(entityId)?.get<CardComponent>()?.name == cardName
            }
        }

        /**
         * Check if a card with the given name is in a player's hand.
         */
        fun isInHand(playerNumber: Int, cardName: String): Boolean {
            return findCardsInHand(playerNumber, cardName).isNotEmpty()
        }

        /**
         * Advance the game to a specific phase and step by directly setting the state.
         * Use this when you need to "jump" to a specific step for testing.
         * For tests that need to go through normal priority flow, use passUntilPhase() instead.
         */
        fun advanceToPhase(phase: Phase, step: Step): GameState {
            state = state.copy(phase = phase, step = step)
            return state
        }

        /**
         * Advance the game to a specific phase and step by passing priority.
         * Both players pass priority repeatedly until the target phase/step is reached.
         * This goes through the actual game flow including combat damage processing.
         *
         * Note: this stops *at* the target step's priority window. If the target step has
         * begin-of-step triggers (e.g., "At the beginning of your end step, ..."), they
         * are queued on the stack but not yet resolved when this returns. Call
         * [resolveStack] afterward to drain them. Triggers that pause for player input
         * (target selection, may decisions) will surface as `pendingDecision` instead.
         */
        fun passUntilPhase(phase: Phase, step: Step): GameState {
            var iterations = 0
            val maxIterations = 100

            while ((state.phase != phase || state.step != step) && iterations < maxIterations) {
                if (state.pendingDecision != null) {
                    autoResolveDecision()
                } else {
                    val priorityPlayer = state.priorityPlayerId
                    if (priorityPlayer != null) {
                        // During combat declaration steps, submit empty declarations before passing
                        autoSubmitCombatDeclarationIfNeeded(priorityPlayer)
                        execute(PassPriority(priorityPlayer))
                    } else {
                        // No priority player - might be in a step transition, try advancing
                        break
                    }
                }
                iterations++
            }

            if (iterations >= maxIterations) {
                error("Failed to advance to $phase/$step after $maxIterations iterations. Current: ${state.phase}/${state.step}")
            }

            return state
        }

        /**
         * During combat declaration steps, auto-submit empty declarations so that
         * PassPriority can proceed.
         */
        private fun autoSubmitCombatDeclarationIfNeeded(priorityPlayer: EntityId) {
            if (state.step == Step.DECLARE_ATTACKERS && priorityPlayer == state.activePlayerId) {
                val attackersDeclared = state.getEntity(priorityPlayer)
                    ?.get<AttackersDeclaredThisCombatComponent>() != null
                if (!attackersDeclared) {
                    execute(DeclareAttackers(priorityPlayer, emptyMap()))
                }
            }
            if (state.step == Step.DECLARE_BLOCKERS && priorityPlayer != state.activePlayerId) {
                val blockersDeclared = state.getEntity(priorityPlayer)
                    ?.get<BlockersDeclaredThisCombatComponent>() != null
                if (!blockersDeclared) {
                    execute(DeclareBlockers(priorityPlayer, emptyMap()))
                }
            }
        }

        /**
         * Auto-resolve a pending decision by picking the first valid option.
         * Used by passUntilPhase to handle decisions that arise during phase advancement.
         */
        private fun autoResolveDecision() {
            val decision = state.pendingDecision
                ?: error("No pending decision to auto-resolve")
            when (decision) {
                is SelectCardsDecision -> {
                    val selected = decision.options.take(decision.minSelections)
                    selectCards(selected)
                }
                is YesNoDecision -> answerYesNo(false)
                is ReorderLibraryDecision -> {
                    submitDecision(OrderedResponse(decision.id, decision.cards))
                }
                is OrderObjectsDecision -> {
                    submitDecision(OrderedResponse(decision.id, decision.objects))
                }
                is DistributeDecision -> {
                    val distribution = decision.targets.associateWith { 0 }.toMutableMap()
                    distribution[decision.targets.first()] = decision.totalAmount
                    submitDecision(DistributionResponse(decision.id, distribution))
                }
                is AssignDamageDecision -> {
                    submitDecision(DamageAssignmentResponse(decision.id, decision.defaultAssignments))
                }
                is CombatResolutionDecision -> {
                    submitDecision(
                        CombatResolutionResponse(decision.id, decision.edges.map { DamageEdgeAmount(it.id, it.amount) })
                    )
                }
                else -> error("Cannot auto-resolve decision of type ${decision::class.simpleName}")
            }
        }

        /**
         * Declare attackers.
         * @param attackers Map of permanent names to the player number being attacked (1 or 2)
         */
        fun declareAttackers(attackers: Map<String, Int>): ExecutionResult {
            val attackingPlayer = state.activePlayerId!!
            val attackerMap = attackers.mapNotNull { (name, targetPlayerNum) ->
                val attackerId = findPermanent(name)
                val targetPlayerId = if (targetPlayerNum == 1) player1Id else player2Id
                if (attackerId != null) attackerId to targetPlayerId else null
            }.toMap()

            return execute(DeclareAttackers(attackingPlayer, attackerMap))
        }

        /**
         * Declare attackers with some attacking a planeswalker by name.
         * @param playerAttackers Map of creature names to player number being attacked
         * @param planeswalkerAttackers Map of creature names to planeswalker names being attacked
         */
        fun declareAttackersWithPlaneswalkerTargets(
            playerAttackers: Map<String, Int> = emptyMap(),
            planeswalkerAttackers: Map<String, String> = emptyMap()
        ): ExecutionResult {
            val attackingPlayer = state.activePlayerId!!
            val attackerMap = mutableMapOf<EntityId, EntityId>()

            for ((name, targetPlayerNum) in playerAttackers) {
                val attackerId = findPermanent(name) ?: continue
                val targetPlayerId = if (targetPlayerNum == 1) player1Id else player2Id
                attackerMap[attackerId] = targetPlayerId
            }
            for ((name, planeswalkerName) in planeswalkerAttackers) {
                val attackerId = findPermanent(name) ?: continue
                val targetId = findPermanent(planeswalkerName) ?: continue
                attackerMap[attackerId] = targetId
            }

            return execute(DeclareAttackers(attackingPlayer, attackerMap))
        }

        /**
         * Declare blockers.
         * @param blockers Map of blocker permanent names to list of attacker permanent names being blocked
         */
        fun declareBlockers(blockers: Map<String, List<String>>): ExecutionResult {
            val blockingPlayer = state.turnOrder.first { it != state.activePlayerId }
            val blockerMap = blockers.mapNotNull { (blockerName, attackerNames) ->
                val blockerId = findPermanent(blockerName)
                val attackerIds = attackerNames.mapNotNull { findPermanent(it) }
                if (blockerId != null && attackerIds.isNotEmpty()) blockerId to attackerIds else null
            }.toMap()

            return execute(DeclareBlockers(blockingPlayer, blockerMap))
        }

        /**
         * Declare no blockers (empty blocker declaration).
         */
        fun declareNoBlockers(): ExecutionResult {
            val blockingPlayer = state.turnOrder.first { it != state.activePlayerId }
            return execute(DeclareBlockers(blockingPlayer, emptyMap()))
        }

        /**
         * Find all permanents on the battlefield with a given name.
         */
        fun findAllPermanents(name: String): List<EntityId> {
            return state.getBattlefield().filter { entityId ->
                state.getEntity(entityId)?.get<CardComponent>()?.name == name
            }
        }

        /**
         * Get a player's life total.
         */
        fun getLifeTotal(playerNumber: Int): Int {
            val playerId = if (playerNumber == 1) player1Id else player2Id
            return state.getEntity(playerId)?.get<LifeTotalComponent>()?.life ?: 0
        }

        /**
         * Find cards in a player's graveyard by name.
         */
        fun findCardsInGraveyard(playerNumber: Int, cardName: String): List<EntityId> {
            val playerId = if (playerNumber == 1) player1Id else player2Id
            return state.getGraveyard(playerId).filter { entityId ->
                state.getEntity(entityId)?.get<CardComponent>()?.name == cardName
            }
        }

        /**
         * Find all cards in a player's library matching the given name.
         */
        fun findCardsInLibrary(playerNumber: Int, cardName: String): List<EntityId> {
            val playerId = if (playerNumber == 1) player1Id else player2Id
            return state.getLibrary(playerId).filter { entityId ->
                state.getEntity(entityId)?.get<CardComponent>()?.name == cardName
            }
        }

        /**
         * Submit a target selection for a triggered ability (e.g., Gravedigger's ETB).
         * @param targets List of entity IDs to select as targets
         */
        fun selectTargets(targets: List<EntityId>): ExecutionResult {
            val decisionId = state.pendingDecision?.id
                ?: error("No pending decision to respond to")
            return submitDecision(TargetsResponse(decisionId, mapOf(0 to targets)))
        }

        /**
         * Cast a spell by name from a player's hand, targeting a card in a graveyard.
         */
        fun castSpellTargetingGraveyardCard(
            playerNumber: Int,
            spellName: String,
            graveyardOwnerNumber: Int,
            targetCardName: String
        ): ExecutionResult {
            val playerId = if (playerNumber == 1) player1Id else player2Id
            val graveyardOwnerId = if (graveyardOwnerNumber == 1) player1Id else player2Id
            val hand = state.getHand(playerId)
            val cardId = hand.find { entityId ->
                state.getEntity(entityId)?.get<CardComponent>()?.name == spellName
            } ?: error("Card '$spellName' not found in player $playerNumber's hand")

            val targetCardId = state.getGraveyard(graveyardOwnerId).find { entityId ->
                state.getEntity(entityId)?.get<CardComponent>()?.name == targetCardName
            } ?: error("Card '$targetCardName' not found in player $graveyardOwnerNumber's graveyard")

            val targets = listOf(ChosenTarget.Card(targetCardId, graveyardOwnerId, Zone.GRAVEYARD))
            return execute(CastSpell(playerId, cardId, targets))
        }

        /**
         * Submit a "skip targets" decision (select no targets for optional ability).
         */
        fun skipTargets(): ExecutionResult {
            val decisionId = state.pendingDecision?.id
                ?: error("No pending decision to respond to")
            return submitDecision(TargetsResponse(decisionId, mapOf(0 to emptyList())))
        }

        /**
         * Submit a yes/no response (for may abilities and similar choices).
         */
        fun answerYesNo(choice: Boolean): ExecutionResult {
            val decisionId = state.pendingDecision?.id
                ?: error("No pending decision to respond to")
            return submitDecision(YesNoResponse(decisionId, choice))
        }

        /**
         * Submit a mana sources selection response.
         * Use autoPay=true to auto-tap, or provide specific source entity IDs.
         */
        fun submitManaSourcesDecision(
            selectedSources: List<EntityId> = emptyList(),
            autoPay: Boolean = false
        ): ExecutionResult {
            val decisionId = state.pendingDecision?.id
                ?: error("No pending decision to respond to")
            return submitDecision(ManaSourcesSelectedResponse(decisionId, selectedSources, autoPay))
        }

        /**
         * Shorthand for auto-pay mana sources.
         */
        fun submitManaSourcesAutoPay(): ExecutionResult = submitManaSourcesDecision(autoPay = true)

        /**
         * Submit a distribution response (for divided damage, etc.).
         * @param distribution Map of target ID to amount assigned
         */
        fun submitDistribution(distribution: Map<EntityId, Int>): ExecutionResult {
            val decisionId = state.pendingDecision?.id
                ?: error("No pending decision to respond to")
            return submitDecision(DistributionResponse(decisionId, distribution))
        }

        /**
         * Submit a damage assignment response (for combat damage assignment).
         * @param assignments Map of target ID (creature or player) to damage amount
         */
        fun submitDamageAssignment(assignments: Map<EntityId, Int>): ExecutionResult {
            val decisionId = state.pendingDecision?.id
                ?: error("No pending decision to respond to")
            return submitDecision(DamageAssignmentResponse(decisionId, assignments))
        }

        /**
         * Submit a damage assignment using the default (auto-calculated) assignments.
         * Convenience method when the player accepts the engine's suggested distribution.
         */
        fun submitDefaultDamageAssignment(): ExecutionResult {
            val decision = state.pendingDecision as? AssignDamageDecision
                ?: error("No pending AssignDamageDecision")
            return submitDamageAssignment(decision.defaultAssignments)
        }

        /**
         * Confirm the pending [CombatResolutionDecision] with the engine's default edge amounts.
         * The combat resolution board replaces the old per-attacker AssignDamageDecision flow.
         */
        fun submitDefaultCombatDamage(): ExecutionResult {
            val decision = state.pendingDecision as? CombatResolutionDecision
                ?: error("No pending CombatResolutionDecision (have ${state.pendingDecision})")
            return submitDecision(
                CombatResolutionResponse(decision.id, decision.edges.map { DamageEdgeAmount(it.id, it.amount) })
            )
        }

        /**
         * Submit a custom combat-damage assignment to the pending [CombatResolutionDecision] for the
         * current chooser. [plan] maps (sourceId, targetId) to the damage on that edge; any edge not
         * named keeps its engine-computed default.
         */
        fun submitCombatDamage(plan: Map<Pair<EntityId, EntityId>, Int>): ExecutionResult {
            val decision = state.pendingDecision as? CombatResolutionDecision
                ?: error("No pending CombatResolutionDecision (have ${state.pendingDecision})")
            val edges = decision.edges.map { edge ->
                DamageEdgeAmount(edge.id, plan[edge.sourceId to edge.targetId] ?: edge.amount)
            }
            return submitDecision(CombatResolutionResponse(decision.id, edges))
        }

        /**
         * Cast a spell by name targeting another spell on the stack.
         */
        fun castSpellTargetingStackSpell(
            playerNumber: Int,
            spellName: String,
            targetSpellName: String
        ): ExecutionResult {
            val playerId = if (playerNumber == 1) player1Id else player2Id
            val hand = state.getHand(playerId)
            val cardId = hand.find { entityId ->
                state.getEntity(entityId)?.get<CardComponent>()?.name == spellName
            } ?: error("Card '$spellName' not found in player $playerNumber's hand")

            val targetSpellId = state.stack.find { entityId ->
                state.getEntity(entityId)?.get<CardComponent>()?.name == targetSpellName
            } ?: error("Spell '$targetSpellName' not found on stack")

            val targets = listOf(ChosenTarget.Spell(targetSpellId))
            return execute(CastSpell(playerId, cardId, targets))
        }

        /**
         * Cast a spell with an additional sacrifice cost.
         * @param playerNumber The player casting the spell (1 or 2)
         * @param spellName The name of the spell to cast
         * @param sacrificeCreatureName The name of the creature to sacrifice
         */
        fun castSpellWithAdditionalSacrifice(
            playerNumber: Int,
            spellName: String,
            sacrificeCreatureName: String
        ): ExecutionResult {
            val playerId = if (playerNumber == 1) player1Id else player2Id
            val hand = state.getHand(playerId)
            val cardId = hand.find { entityId ->
                state.getEntity(entityId)?.get<CardComponent>()?.name == spellName
            } ?: error("Card '$spellName' not found in player $playerNumber's hand")

            val sacrificeId = state.getBattlefield().find { entityId ->
                val container = state.getEntity(entityId) ?: return@find false
                container.get<CardComponent>()?.name == sacrificeCreatureName &&
                    container.get<ControllerComponent>()?.playerId == playerId
            } ?: error("Creature '$sacrificeCreatureName' not found on player $playerNumber's battlefield")

            val payment = com.wingedsheep.sdk.scripting.AdditionalCostPayment(
                sacrificedPermanents = listOf(sacrificeId)
            )
            return execute(CastSpell(playerId, cardId, emptyList(), additionalCostPayment = payment))
        }

        /**
         * Cast a spell with an additional exile-from-graveyard cost, targeting a creature.
         * @param playerNumber The player casting the spell (1 or 2)
         * @param spellName The name of the spell to cast
         * @param exileCardNames The names of the creature cards to exile from graveyard
         * @param targetCreatureName The name of the creature to target on the battlefield
         */
        fun castSpellWithExileCost(
            playerNumber: Int,
            spellName: String,
            exileCardNames: List<String>,
            targetCreatureName: String
        ): ExecutionResult {
            val playerId = if (playerNumber == 1) player1Id else player2Id
            val hand = state.getHand(playerId)
            val cardId = hand.find { entityId ->
                state.getEntity(entityId)?.get<CardComponent>()?.name == spellName
            } ?: error("Card '$spellName' not found in player $playerNumber's hand")

            val graveyard = state.getGraveyard(playerId)
            val exileCardIds = exileCardNames.map { name ->
                graveyard.find { entityId ->
                    state.getEntity(entityId)?.get<CardComponent>()?.name == name
                } ?: error("Card '$name' not found in player $playerNumber's graveyard")
            }

            val targetId = state.getBattlefield().find { entityId ->
                state.getEntity(entityId)?.get<CardComponent>()?.name == targetCreatureName
            } ?: error("Creature '$targetCreatureName' not found on battlefield")

            val payment = com.wingedsheep.sdk.scripting.AdditionalCostPayment(
                exiledCards = exileCardIds
            )
            val targets = listOf(ChosenTarget.Permanent(targetId))
            return execute(CastSpell(playerId, cardId, targets, additionalCostPayment = payment))
        }

        /**
         * Cast a spell with a Behold cost (choose a card from battlefield or hand to behold).
         * @param playerNumber The player casting the spell (1 or 2)
         * @param spellName The name of the spell to cast
         * @param beholdCardName The name of the card to behold (from battlefield or hand)
         */
        fun castSpellWithBeholdCost(
            playerNumber: Int,
            spellName: String,
            beholdCardName: String
        ): ExecutionResult {
            val playerId = if (playerNumber == 1) player1Id else player2Id
            val hand = state.getHand(playerId)
            val cardId = hand.find { entityId ->
                state.getEntity(entityId)?.get<CardComponent>()?.name == spellName
            } ?: error("Card '$spellName' not found in player $playerNumber's hand")

            // Look for the behold target on the battlefield first, then in hand
            val beholdId = state.getBattlefield().find { entityId ->
                val container = state.getEntity(entityId) ?: return@find false
                container.get<CardComponent>()?.name == beholdCardName &&
                    container.get<ControllerComponent>()?.playerId == playerId
            } ?: hand.find { entityId ->
                entityId != cardId &&
                    state.getEntity(entityId)?.get<CardComponent>()?.name == beholdCardName
            } ?: error("Card '$beholdCardName' not found on player $playerNumber's battlefield or in hand")

            val payment = com.wingedsheep.sdk.scripting.AdditionalCostPayment(
                beheldCards = listOf(beholdId)
            )
            return execute(CastSpell(playerId, cardId, emptyList(), additionalCostPayment = payment))
        }
    }
}
