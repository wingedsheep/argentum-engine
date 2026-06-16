package com.wingedsheep.gameserver.scenario

import com.wingedsheep.engine.mechanics.layers.StaticAbilityHandler
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.battlefield.ChoiceValue
import com.wingedsheep.engine.state.components.battlefield.ClassLevelComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.SagaComponent
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.battlefield.withCastChoice
import com.wingedsheep.engine.state.components.identity.*
import com.wingedsheep.engine.state.components.player.LandDropsComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ProtectionScope
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicLong

/** Result of building a scenario: the injectable state plus the seat ids in turn order. */
data class ScenarioBuildResult(
    val state: GameState,
    val playerIds: List<EntityId>,
) {
    val player1Id: EntityId get() = playerIds[0]
    val player2Id: EntityId get() = playerIds[1]
}

/**
 * Builds an injectable [GameState] from a [ScenarioRequest]. Shared by the dev and
 * production scenario controllers so the board-construction logic lives in exactly one
 * place. Holds no per-request state — each call constructs a fresh builder.
 */
@Service
class ScenarioBuilderService(
    private val cardRegistry: CardRegistry
) {
    companion object {
        /** Per-zone card cap enforced for the public endpoint. */
        const val MAX_CARDS_PER_ZONE = 100

        /** Total card-entity cap across the whole scenario for the public endpoint. */
        const val MAX_TOTAL_CARDS = 400
    }

    /**
     * Validate a request before building. Returns a list of human-readable error messages
     * (empty ⇒ valid). Catches unknown card names, bad counter-type / color keys, and —
     * when [enforceLimits] is set (production) — size caps, so the public endpoint rejects
     * abusive or malformed input cleanly instead of throwing deep in the builder.
     */
    fun validate(request: ScenarioRequest, enforceLimits: Boolean): List<String> {
        val errors = mutableListOf<String>()
        var total = 0

        fun checkZone(label: String, names: List<String>?) {
            val list = names ?: return
            total += list.size
            if (enforceLimits && list.size > MAX_CARDS_PER_ZONE) {
                errors += "Too many cards in $label (${list.size}); max $MAX_CARDS_PER_ZONE."
            }
            for (name in list) {
                if (!cardRegistry.hasCard(name)) errors += "Unknown card: $name"
            }
        }

        fun checkPlayer(label: String, config: PlayerConfig?) {
            config ?: return
            checkZone("$label hand", config.hand)
            checkZone("$label graveyard", config.graveyard)
            checkZone("$label library", config.library)
            checkZone("$label exile", config.exile)
            checkZone("$label commanders", config.commanders)
            val battlefield = config.battlefield ?: emptyList()
            total += battlefield.size
            if (enforceLimits && battlefield.size > MAX_CARDS_PER_ZONE) {
                errors += "Too many cards on $label battlefield (${battlefield.size}); max $MAX_CARDS_PER_ZONE."
            }
            for (card in battlefield) {
                if (!cardRegistry.hasCard(card.name)) errors += "Unknown card: ${card.name}"
                card.counters?.keys?.forEach { key ->
                    if (runCatching { CounterType.valueOf(key) }.isFailure) {
                        errors += "Unknown counter type '$key' on ${card.name}."
                    }
                }
                card.chosenColor?.let { color ->
                    if (runCatching { Color.valueOf(color) }.isFailure) {
                        errors += "Unknown color '$color' on ${card.name}."
                    }
                }
            }
        }

        val seats = request.seats()
        if (seats.size < 2 || seats.size > 4) {
            errors += "Scenario needs 2-4 seats (got ${seats.size})."
        }
        if (seats.size > 2 && request.effectiveMode != ScenarioMode.SELF) {
            errors += "Pods of more than two seats only support SELF (hotseat) mode."
        }
        request.teams?.let { teams ->
            val flat = teams.flatten().sorted()
            if (flat != seats.indices.toList()) {
                errors += "teams must partition all ${seats.size} seat indices exactly once (got $flat)."
            }
        }
        seats.forEachIndexed { i, (_, config) -> checkPlayer("player${i + 1}", config) }

        if (enforceLimits && total > MAX_TOTAL_CARDS) {
            errors += "Scenario has too many cards ($total); max $MAX_TOTAL_CARDS."
        }
        return errors
    }

    /** Construct the scenario state. Assumes [validate] has already passed. */
    fun buildScenario(request: ScenarioRequest): ScenarioBuildResult {
        val seats = request.seats()
        val builder = ScenarioBuilder(cardRegistry)
        builder.withPlayers(seats.map { it.first })

        seats.forEachIndexed { i, (_, config) -> applyPlayer(builder, i + 1, config) }

        request.phase?.let { phase ->
            val step = request.step ?: phaseToDefaultStep(phase)
            builder.inPhase(phase, step)
        }
        request.activePlayer?.let { builder.withActivePlayer(it) }
        request.priorityPlayer?.let { builder.withPriorityPlayer(it) }
        request.teams?.let { builder.withTeams(it, teamVsTeam = request.teamVsTeam == true) }

        val (state, playerIds) = builder.build()
        return ScenarioBuildResult(state, playerIds)
    }

    private fun applyPlayer(builder: ScenarioBuilder, n: Int, config: PlayerConfig?) {
        config ?: return
        config.lifeTotal?.let { builder.withLifeTotal(n, it) }
        config.commanders?.forEach { builder.withCommander(n, it) }
        config.hand?.forEach { builder.withCardInHand(n, it) }
        config.battlefield?.forEach { card ->
            builder.withCardOnBattlefield(
                n, card.name,
                tapped = card.tapped ?: false,
                summoningSickness = card.summoningSickness ?: false,
                counters = card.counters ?: emptyMap(),
                chosenCreatureType = card.chosenCreatureType,
                chosenColor = card.chosenColor
            )
        }
        config.battlefield?.forEach { card ->
            card.attachedTo?.let { hostName -> builder.wireAttachment(card.name, hostName) }
        }
        config.graveyard?.forEach { builder.withCardInGraveyard(n, it) }
        config.library?.forEach { builder.withCardInLibrary(n, it) }
        config.exile?.forEach { builder.withCardInExile(n, it) }
    }

    private fun phaseToDefaultStep(phase: Phase): Step = when (phase) {
        Phase.BEGINNING -> Step.UNTAP
        Phase.PRECOMBAT_MAIN -> Step.PRECOMBAT_MAIN
        Phase.COMBAT -> Step.BEGIN_COMBAT
        Phase.POSTCOMBAT_MAIN -> Step.POSTCOMBAT_MAIN
        Phase.ENDING -> Step.END
    }

    /**
     * Builds a [GameState] one card at a time (similar to ScenarioTestBase.ScenarioBuilder).
     * One instance per scenario — not thread-safe and not reused.
     */
    private class ScenarioBuilder(private val cardRegistry: CardRegistry) {
        private val entityIdCounter = AtomicLong(1000)
        private var state = GameState()

        private val playerIds = mutableListOf<EntityId>()

        /** Seat number (1-based, turn order) → player entity id. */
        private fun playerFor(playerNumber: Int): EntityId = playerIds[playerNumber - 1]

        fun withPlayers(names: List<String>): ScenarioBuilder {
            require(names.size >= 2) { "Scenario needs at least two players" }
            names.forEachIndexed { i, name ->
                val playerId = EntityId.of("player-${i + 1}")
                playerIds += playerId
                state = state.withEntity(
                    playerId,
                    ComponentContainer.of(
                        PlayerComponent(name),
                        LifeTotalComponent(20),
                        ManaPoolComponent(),
                        LandDropsComponent(remaining = 1, maxPerTurn = 1)
                    )
                )
            }

            state = state.copy(
                turnOrder = playerIds.toList(),
                activePlayerId = playerIds[0],
                priorityPlayerId = playerIds[0],
                phase = Phase.PRECOMBAT_MAIN,
                step = Step.PRECOMBAT_MAIN,
                turnNumber = 1
            )

            // Initialize empty zones for every player
            for (playerId in playerIds) {
                for (zoneType in listOf(Zone.HAND, Zone.LIBRARY, Zone.GRAVEYARD, Zone.BATTLEFIELD, Zone.EXILE, Zone.COMMAND)) {
                    val zoneKey = ZoneKey(playerId, zoneType)
                    state = state.copy(zones = state.zones + (zoneKey to emptyList()))
                }
            }

            return this
        }

        fun withCardInHand(playerNumber: Int, cardName: String): ScenarioBuilder {
            val playerId = playerFor(playerNumber)
            val cardId = createCard(cardName, playerId)
            state = state.addToZone(ZoneKey(playerId, Zone.HAND), cardId)
            return this
        }

        /** Tracks placed battlefield card entity IDs by card name for attachment wiring */
        private val placedCardIds = mutableListOf<Pair<String, EntityId>>()

        fun withCardOnBattlefield(
            playerNumber: Int,
            cardName: String,
            tapped: Boolean = false,
            summoningSickness: Boolean = false,
            counters: Map<String, Int> = emptyMap(),
            chosenCreatureType: String? = null,
            chosenColor: String? = null
        ): ScenarioBuilder {
            val playerId = playerFor(playerNumber)
            val cardId = createCard(cardName, playerId)

            state = state.addToZone(ZoneKey(playerId, Zone.BATTLEFIELD), cardId)

            var container = state.getEntity(cardId)!!
            container = container.with(ControllerComponent(playerId))

            if (tapped) {
                container = container.with(TappedComponent)
            }

            if (summoningSickness) {
                container = container.with(SummoningSicknessComponent)
            }

            if (counters.isNotEmpty()) {
                val counterMap = counters.mapKeys { CounterType.valueOf(it.key) }
                container = container.with(CountersComponent(counterMap))
            }

            if (chosenCreatureType != null) {
                container = container.withCastChoice(
                    ChoiceSlot.CREATURE_TYPE, ChoiceValue.TextChoice(chosenCreatureType)
                )
            }

            if (chosenColor != null) {
                container = container.withCastChoice(
                    ChoiceSlot.COLOR,
                    ChoiceValue.ColorChoice(Color.valueOf(chosenColor))
                )
            }

            // Add continuous effects from static abilities and replacement effects
            val cardDef = cardRegistry.getCard(cardName)
            if (cardDef != null) {
                val staticHandler = StaticAbilityHandler(cardRegistry)
                container = staticHandler.addContinuousEffectComponent(container, cardDef)
                container = staticHandler.addReplacementEffectComponent(container, cardDef)

                // Add ClassLevelComponent for Class enchantments (starts at level 1)
                if (cardDef.isClass) {
                    container = container.with(ClassLevelComponent(currentLevel = 1))
                }

                // Add SagaComponent for sagas, marking chapters as triggered based on lore counters
                if (cardDef.finalChapter != null) {
                    val loreCount = counters["LORE"] ?: 0
                    val triggeredChapters = cardDef.sagaChapters
                        .filter { it.chapter <= loreCount }
                        .map { it.chapter }
                        .toSet()
                    container = container.with(SagaComponent(triggeredChapters))
                }

                // Add DoubleFacedComponent for DFCs (Rule 712)
                if (cardDef.isDoubleFaced && cardDef.backFace != null) {
                    container = container.with(
                        DoubleFacedComponent(
                            frontCardDefinitionId = cardDef.name,
                            backCardDefinitionId = cardDef.backFace!!.name,
                            currentFace = DoubleFacedComponent.Face.FRONT
                        )
                    )
                }
            }

            state = state.withEntity(cardId, container)
            placedCardIds.add(cardName to cardId)
            return this
        }

        fun wireAttachment(auraName: String, hostName: String): ScenarioBuilder {
            val auraId = placedCardIds.lastOrNull { it.first == auraName }?.second
                ?: error("Aura not found on battlefield: $auraName")
            val hostId = placedCardIds.lastOrNull { it.first == hostName }?.second
                ?: error("Host not found on battlefield: $hostName")

            // Set AttachedToComponent on the aura
            state = state.updateEntity(auraId) { it.with(AttachedToComponent(hostId)) }

            // Set/merge AttachmentsComponent on the host
            val existingAttachments = state.getEntity(hostId)?.get<AttachmentsComponent>()
            val attachedIds = (existingAttachments?.attachedIds ?: emptyList()) + auraId
            state = state.updateEntity(hostId) { it.with(AttachmentsComponent(attachedIds)) }

            return this
        }

        fun withCardInGraveyard(playerNumber: Int, cardName: String): ScenarioBuilder {
            val playerId = playerFor(playerNumber)
            val cardId = createCard(cardName, playerId)
            state = state.addToZone(ZoneKey(playerId, Zone.GRAVEYARD), cardId)
            return this
        }

        fun withCardInLibrary(playerNumber: Int, cardName: String): ScenarioBuilder {
            val playerId = playerFor(playerNumber)
            val cardId = createCard(cardName, playerId)
            state = state.addToZone(ZoneKey(playerId, Zone.LIBRARY), cardId)
            return this
        }

        fun withCardInExile(playerNumber: Int, cardName: String): ScenarioBuilder {
            val playerId = playerFor(playerNumber)
            val cardId = createCard(cardName, playerId)
            state = state.addToZone(ZoneKey(playerId, Zone.EXILE), cardId)
            return this
        }

        /**
         * Designate a card as the player's commander, placing it in the command zone with a
         * [CommanderComponent] and appending it to the player's [CommanderRegistryComponent].
         * Supports multiple calls per player (Partner / Background).
         */
        fun withCommander(playerNumber: Int, cardName: String): ScenarioBuilder {
            val playerId = playerFor(playerNumber)
            val cardId = createCard(cardName, playerId)
            state = state.updateEntity(cardId) { it.with(CommanderComponent(ownerId = playerId)) }
            state = state.addToZone(ZoneKey(playerId, Zone.COMMAND), cardId)
            state = state.updateEntity(playerId) { container ->
                val existing = container.get<CommanderRegistryComponent>()
                val ids = (existing?.commanderIds ?: emptyList()) + cardId
                container.with(CommanderRegistryComponent(ids))
            }
            return this
        }

        fun withLifeTotal(playerNumber: Int, life: Int): ScenarioBuilder {
            val playerId = playerFor(playerNumber)
            state = state.updateEntity(playerId) { container ->
                container.with(LifeTotalComponent(life))
            }
            return this
        }

        fun inPhase(phase: Phase, step: Step): ScenarioBuilder {
            state = state.copy(phase = phase, step = step)
            return this
        }

        fun withActivePlayer(playerNumber: Int): ScenarioBuilder {
            val playerId = playerFor(playerNumber)
            state = state.copy(activePlayerId = playerId, priorityPlayerId = playerId)
            return this
        }

        fun withPriorityPlayer(playerNumber: Int): ScenarioBuilder {
            val playerId = playerFor(playerNumber)
            state = state.copy(priorityPlayerId = playerId)
            return this
        }

        /**
         * Stamp team membership for a team variant. [teams] entries are lists of 0-based seat
         * indices; each becomes a team via [TeamComponent]. Also switches the game to the matching
         * format so the engine reads the right rules: [teamVsTeam] = false runs Two-Headed Giant
         * (CR 810 — shared life/turns, 15-poison threshold), true runs Team vs. Team (CR 808 —
         * per-player life/turns, individual elimination). Mirrors GameInitializer's stamping for the
         * freshly-built (lobby/quick-game) path.
         */
        fun withTeams(teams: List<List<Int>>, teamVsTeam: Boolean = false): ScenarioBuilder {
            teams.forEachIndexed { teamIndex, memberIndices ->
                for (memberIndex in memberIndices) {
                    val pid = playerIds[memberIndex]
                    state = state.updateEntity(pid) { container ->
                        container.with(TeamComponent(teamIndex))
                    }
                }
            }
            state = state.copy(
                format = if (teamVsTeam) com.wingedsheep.sdk.core.Format.TeamVsTeam()
                else com.wingedsheep.sdk.core.Format.TwoHeadedGiant()
            )
            return this
        }

        fun build(): Pair<GameState, List<EntityId>> {
            return state to playerIds.toList()
        }

        private fun createCard(cardName: String, ownerId: EntityId): EntityId {
            val cardDef = cardRegistry.getCard(cardName)
                ?: error("Card not found in registry: $cardName")

            val cardId = EntityId.of("card-${entityIdCounter.incrementAndGet()}")

            // Use Name#SetCode-CollectorNumber when both are available so the resulting
            // CardComponent.cardDefinitionId matches the key used by CardRegistry. Without the
            // set-code prefix, registry lookups (e.g. TriggerAbilityResolver.getTriggeredAbilities)
            // miss and the card silently has no triggered abilities.
            val definitionId = cardDef.metadata.collectorNumber?.let { cn ->
                if (cardDef.setCode != null) "${cardDef.name}#${cardDef.setCode}-$cn"
                else "${cardDef.name}#$cn"
            } ?: cardDef.name
            val cardComponent = CardComponent(
                cardDefinitionId = definitionId,
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
            )

            var container = ComponentContainer.of(
                cardComponent,
                OwnerComponent(ownerId),
                ControllerComponent(ownerId)
            )

            if (cardDef.script.cantBeCountered) {
                container = container.with(CantBeCounteredComponent)
            }

            if (cardDef.keywordAbilities.any { it is KeywordAbility.Morph }) {
                container = container.with(HasMorphAbilityComponent)
            }

            // Add ProtectionComponent for cards with protection from color/subtype
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

            // Add HexproofFromColorComponent for cards with hexproof from color
            val hexproofFromColors = cardDef.keywordAbilities
                .filterIsInstance<KeywordAbility.Hexproof>()
                .mapNotNull { (it.scope as? ProtectionScope.Color)?.color }
                .toSet()
            if (hexproofFromColors.isNotEmpty()) {
                container = container.with(HexproofFromColorComponent(hexproofFromColors))
            }

            // Add ToxicComponent for cards with printed Toxic N (sums per Rule 702.164b)
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
}
