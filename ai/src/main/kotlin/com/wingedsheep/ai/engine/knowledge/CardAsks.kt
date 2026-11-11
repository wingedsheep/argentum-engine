package com.wingedsheep.ai.engine.knowledge

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.CompositeStaticAbility
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.conditions.PermanentTypeEnteredBattlefieldThisTurn
import com.wingedsheep.sdk.scripting.CantBeBlocked
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.CascadeMayPutOntoBattlefield
import com.wingedsheep.sdk.scripting.FreeCyclingCosts
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.GrantFlashToSpellType
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.GrantKeywordToOwnSpells
import com.wingedsheep.sdk.scripting.GrantProtection
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.GrantWard
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.StaticAbility
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.effects.AttachTargetEquipmentToCreatureEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.ForEachEffect
import com.wingedsheep.sdk.scripting.effects.GainControlEffect
import com.wingedsheep.sdk.scripting.effects.ReflexiveTriggerEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.Scope
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.Aggregation
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import java.util.IdentityHashMap

/**
 * A kind of card a payoff asks the rest of the deck for, as the filter it is written with — the
 * "noncreature spell" Monastery Mentor watches, the "artifact spells" Etherium Sculptor discounts,
 * the "creature with power 4 or greater" a ferocious trigger wants — and how many such cards make it
 * fully fed ([wanted]).
 */
data class CardAsk(val filter: GameObjectFilter, val wanted: Double)

/**
 * Reads the [CardAsk]s off a card's structure. Where [SynergyAxes] knows a fixed set of resources,
 * this takes the payoff's own filter and leaves the matching to [PrintedFilter], so every variant
 * the SDK can write — a card type, a colour, a mana value, a printed power, a keyword, any union
 * of them — is read the same way, with no list of shapes to keep.
 *
 * It looks where a card cashes in its own deck's cards:
 *
 *  - **triggers** on another object — "whenever you cast a [filter] spell" (prowess is "noncreature
 *    spell"), "whenever a [filter] you control enters / dies / attacks";
 *  - **group statics** that help your permanents — "[filter]s you control get +1/+1", "…have
 *    flying", "…have '{T}: …'" — which pay per body, so they want [DeckSynergy.TRIBE_MEMBERS_WANTED];
 *  - **spell statics** — "[filter] spells you cast cost {1} less", "…have flash";
 *  - **sacrifice costs** of activated abilities — "Sacrifice an enchantment: …" (Auratog);
 *  - **counts of your own permanents** anywhere in the card — "loses life equal to the number of
 *    artifacts you control", "+1/+1 for each Equipment you control" — and the permanents that pay
 *    for a spell: improvise (printed, or granted to your spells) and affinity ask for artifacts.
 *
 * Not read: effects and targets ("destroy target artifact" asks the *opponent* for artifacts),
 * anything scoped to an opponent's objects, a shrinking static ("all creatures get -1/-1"), a
 * filter every plain creature or spell matches ([PrintedFilter.isNarrow]: that is just "a deck"),
 * one only a game can answer ("the chosen colour"), token artifact types (Treasure, Clue — no
 * printed card is one), and creature types — [CreatureTypeSynergy] reads those, changelings and
 * token types included. A card that also asks for something every deck has (landfall's "land you
 * control", Shalai's "other creatures you control") asks for nothing: it is never short of fuel.
 */
internal object CardAsks {

    fun of(card: CardDefinition): List<CardAsk> {
        val asks = LinkedHashMap<GameObjectFilter, Double>()
        var fedByAnyDeck = false
        // [broadFeedsAnyDeck] false: a broad filter is no ask, but doesn't mark the card fed by any
        // deck either — counting "creatures you control" beside Gonti's artifact trigger still
        // leaves that trigger hungry for artifacts.
        fun add(filter: GameObjectFilter?, wanted: Double, spells: Boolean, broadFeedsAnyDeck: Boolean = true) {
            if (filter == null || opponents(filter)) return
            // "Whenever an enchanted creature dies" is fed by the deck's Auras, not its creatures.
            attachmentAsk(filter)?.let { asks.merge(it, wanted, ::minOf); return }
            if (namesCreatureType(filter) || namesTokenType(filter)) return
            when (PrintedFilter.isNarrow(filter, spells)) {
                true -> asks.merge(filter, wanted, ::minOf)
                false -> if (broadFeedsAnyDeck) fedByAnyDeck = true
                null -> Unit
            }
        }
        fun addPayers(filter: GameObjectFilter?) =
            add(filter, DeckSynergy.FEEDERS_WANTED, spells = false, broadFeedsAnyDeck = false)
        for (script in scripts(card)) {
            for (ability in script.triggeredAbilities) {
                if (ability.binding == TriggerBinding.SELF) continue
                triggerFilters(ability.trigger).forEach { (filter, spells) -> add(filter, DeckSynergy.FEEDERS_WANTED, spells) }
            }
            for (static in script.staticAbilities.flatMap(::unwrap)) {
                groupFilter(static)?.let { add(it, DeckSynergy.TRIBE_MEMBERS_WANTED, spells = false) }
                spellFilter(static)?.let { add(it, DeckSynergy.FEEDERS_WANTED, spells = true) }
                // "Nonartifact spells you cast have improvise" (Inspiring Statuary) is paid for by
                // the artifacts you tap: those are what it asks for, not the spells.
                if (static is GrantKeywordToOwnSpells) addPayers(payersOf(static.keyword))
                // "You may pay {0} rather than pay cycling costs" (New Perspectives) is fuelled
                // only by the deck's own cycling cards.
                if (static is FreeCyclingCosts) asks.merge(CYCLING, DeckSynergy.FEEDERS_WANTED, ::minOf)
                // "As you cascade, you may put a land card from among the exiled cards onto the
                // battlefield tapped" (Averna, the Chaos Bloom) does something only when the deck's
                // own cascade cards cascade.
                if (static is CascadeMayPutOntoBattlefield) asks.merge(CASCADE, DeckSynergy.FEEDERS_WANTED, ::minOf)
            }
            for (replacement in script.replacementEffects.filter(DamageAmplifiers::reads)) {
                val event = replacement.appliesTo as EventPattern.DamageEvent
                add(event.source, DeckSynergy.FEEDERS_WANTED, spells = false)
            }
            // "Attach any number of Equipment you control to target creature you control" (Beatrix,
            // Loyal General), "attach target Equipment you control to …" (Halvar, Weapons Vendor): a
            // free equip is fuelled only by the deck's own Equipment. One that attaches what it just
            // stole (Stolen Uniform, Thieving Skydiver) cashes in the opponent's instead.
            if (EffectWalker.slots(script).any { relocatesOwnEquipment(it.effect) }) {
                asks.merge(EQUIPMENT, DeckSynergy.FEEDERS_WANTED, ::minOf)
            }
            // An ability paid for by sacrificing a narrow kind of permanent — Auratog's
            // enchantment, Atog's artifact — is fuelled only by the deck's cards of that kind. A
            // broad sacrifice (a creature, a land) is a sac outlet, which [SynergyAxes] reads, and
            // must not mark the whole card as fed by any deck.
            for (ability in script.activatedAbilities) {
                sacrificeFilters(ability.cost).forEach { filter ->
                    if (!opponents(filter) && !namesCreatureType(filter) && !namesTokenType(filter) &&
                        PrintedFilter.isNarrow(filter, false) == true
                    ) {
                        asks.merge(filter, DeckSynergy.FEEDERS_WANTED, ::minOf)
                    }
                }
            }
        }
        if (Keyword.PROWESS in card.keywords) add(GameObjectFilter.Noncreature, DeckSynergy.FEEDERS_WANTED, spells = true)
        // Improvise and affinity for artifacts make the spell cheaper per artifact you control.
        card.keywords.forEach { addPayers(payersOf(it)) }
        card.keywordAbilities.filterIsInstance<KeywordAbility.Affinity>().forEach { addPayers(ofType(it.forType)?.youControl()) }
        // "Equal to the number of artifacts you control" (Tezzeret, Master of Metal): a count of
        // your own permanents of a kind is cashed in by the deck's cards of that kind.
        ownPermanentCounts(scripts(card)).forEach(::addPayers)
        // A card that also cashes in something every deck has — Shalai's "other creatures you
        // control have hexproof" beside its planeswalkers — is never short of fuel.
        if (fedByAnyDeck) return emptyList()
        return asks.map { (filter, wanted) -> CardAsk(filter, wanted) }
    }

    /**
     * The kind of card that makes a permanent match [filter] when the filter asks for an attachment
     * rather than a printed trait: an enchanted creature needs an Aura on it, an equipped one an
     * Equipment. Hateful Eidolon's "whenever an enchanted creature dies" reads every creature as
     * printed-equal — a deck with no Aura never triggers it — so the ask is for the Auras.
     *
     * [SynergyAxes] reads the same scope to keep such a trigger off the generic death axis.
     */
    internal fun attachmentAsk(filter: GameObjectFilter): GameObjectFilter? {
        // "Creatures you control that are enchanted or equipped" (Halvar, God of Battle) is fed by
        // either kind of attachment.
        fun flatten(state: StatePredicate): List<StatePredicate> =
            if (state is StatePredicate.Or) state.predicates.flatMap(::flatten) else listOf(state)
        val states = (filter.statePredicates + filter.anyOf.flatMap { it.statePredicates }).flatMap(::flatten)
        val enchanted = states.any { it == StatePredicate.IsEnchanted || it is StatePredicate.IsEnchantedByAura }
        val equipped = states.any { it == StatePredicate.IsEquipped }
        return when {
            enchanted && equipped -> AURAS or EQUIPMENT
            enchanted -> AURAS
            equipped -> EQUIPMENT
            else -> null
        }
    }

    /** The permanents you tap to pay for a spell with [keyword]: improvise's artifacts. */
    private fun payersOf(keyword: Keyword): GameObjectFilter? =
        if (keyword == Keyword.IMPROVISE) GameObjectFilter.Artifact.youControl() else null

    private fun ofType(type: CardType): GameObjectFilter? = when (type) {
        CardType.ARTIFACT -> GameObjectFilter.Artifact
        CardType.ENCHANTMENT -> GameObjectFilter.Enchantment
        CardType.CREATURE -> GameObjectFilter.Creature
        CardType.LAND -> GameObjectFilter.Land
        CardType.PLANESWALKER -> GameObjectFilter.Planeswalker
        else -> null
    }

    /**
     * The kinds of permanent [scripts] count among yours — a [DynamicAmount.AggregateBattlefield]
     * count or a battlefield [DynamicAmount.Count] over [Player.You], or a
     * [PermanentTypeEnteredBattlefieldThisTurn] check on your own — each as "<filter> you
     * control". Walked reflectively, like [GraveyardSynergy], so an amount is found wherever an
     * effect, cost, condition or static holds it.
     */
    private fun ownPermanentCounts(scripts: List<CardScript>): List<GameObjectFilter> {
        val found = LinkedHashSet<GameObjectFilter>()
        val seen = IdentityHashMap<Any, Unit>()
        fun visit(node: Any?) {
            if (node == null || node is String || node is Number || node is Boolean || node is Enum<*>) return
            if (seen.put(node, Unit) != null) return
            when (node) {
                is DynamicAmount.AggregateBattlefield ->
                    if (node.player == Player.You && node.aggregation == Aggregation.COUNT && node.counterType == null) {
                        node.filter.takeIf { it.controllerPredicate == null }?.let { found += it.youControl() }
                    }
                is DynamicAmount.Count ->
                    if (node.player == Player.You && node.zone == Zone.BATTLEFIELD) {
                        node.filter.takeIf { it.controllerPredicate == null }?.let { found += it.youControl() }
                    }
                // "If a planeswalker entered the battlefield under your control this turn" (Oath of
                // Chandra, Oath of Liliana): paid off only by the deck's own permanents of that type.
                is PermanentTypeEnteredBattlefieldThisTurn ->
                    if (node.player == Player.You) ofType(node.cardType)?.let { found += it.youControl() }
                else -> Unit
            }
            when (node) {
                is Collection<*> -> node.forEach(::visit)
                is Map<*, *> -> { node.keys.forEach(::visit); node.values.forEach(::visit) }
                is Array<*> -> node.forEach(::visit)
                else -> if (node.javaClass.name.startsWith(GraveyardSynergy.SDK_PACKAGE)) {
                    GraveyardSynergy.fieldsOf(node.javaClass).forEach { visit(runCatching { it.get(node) }.getOrNull()) }
                }
            }
        }
        scripts.forEach(::visit)
        return found.toList()
    }

    private val AURAS: GameObjectFilter = GameObjectFilter.Enchantment.withSubtype(Subtype.AURA.value)
    private val EQUIPMENT: GameObjectFilter = GameObjectFilter.Artifact.withSubtype("Equipment")
    private val CYCLING: GameObjectFilter = GameObjectFilter.Any.withCycling()
    private val CASCADE: GameObjectFilter = GameObjectFilter.Any.withKeyword(Keyword.CASCADE)

    private fun scripts(card: CardDefinition): List<CardScript> = listOf(card.script) + card.cardFaces.map { it.script }

    /** The filters a trigger watches, each with whether it filters spells (true) or permanents. */
    private fun triggerFilters(pattern: EventPattern): List<Pair<GameObjectFilter?, Boolean>> = when (pattern) {
        is EventPattern.AnyOf -> pattern.events.flatMap(::triggerFilters)
        is EventPattern.SpellCastEvent -> listOf(pattern.spellFilter.takeIf { pattern.player == Player.You } to true)
        is EventPattern.NthSpellCastEvent -> listOf(pattern.spellFilter.takeIf { pattern.player == Player.You } to true)
        is EventPattern.ZoneChangeEvent -> listOf(
            pattern.filter.takeIf {
                pattern.to == Zone.BATTLEFIELD || (pattern.from == Zone.BATTLEFIELD && pattern.to == Zone.GRAVEYARD)
            } to false,
        )
        is EventPattern.PermanentsEnteredEvent -> listOf(pattern.filter to false)
        is EventPattern.CreaturesYouControlDiedEvent -> listOf(pattern.filter to false)
        is EventPattern.AttackEvent -> listOf(pattern.filter to false)
        else -> emptyList()
    }

    /** Whether [effect] attaches Equipment to a creature without first taking control of it. */
    private fun relocatesOwnEquipment(effect: Effect): Boolean {
        val leaves = deepLeaves(effect)
        return leaves.any { it is AttachTargetEquipmentToCreatureEffect && it.equipmentTarget != EffectTarget.Self } &&
            leaves.none { it is GainControlEffect }
    }

    private fun deepLeaves(effect: Effect): List<Effect> =
        EffectWalker.payoffLeaves(effect).flatMap {
            when (it) {
                is ForEachEffect -> deepLeaves(it.body)
                // "You may pay {1}. When you do, attach target Equipment …" (Weapons Vendor).
                is ReflexiveTriggerEffect -> deepLeaves(it.reflexiveEffect)
                else -> listOf(it)
            }
        }

    /** The permanents an activation cost sacrifices (other than the source itself). */
    private fun sacrificeFilters(cost: AbilityCost): List<GameObjectFilter> = when (cost) {
        is AbilityCost.Atom -> listOfNotNull((cost.atom as? CostAtom.Sacrifice)?.filter)
        is AbilityCost.Composite -> cost.costs.flatMap(::sacrificeFilters)
        else -> emptyList()
    }

    private fun unwrap(ability: StaticAbility): List<StaticAbility> = when (ability) {
        is ConditionalStaticAbility -> unwrap(ability.ability)
        is CompositeStaticAbility -> ability.abilities.flatMap(::unwrap)
        else -> listOf(ability)
    }

    /** The group a static helps, when it helps a group on the battlefield rather than one creature. */
    private fun groupFilter(ability: StaticAbility): GameObjectFilter? {
        val group: GroupFilter = when (ability) {
            is ModifyStats -> ability.filter.takeIf { ability.powerBonus + ability.toughnessBonus > 0 }
            is GrantDynamicStats -> ability.filter
            is GrantKeyword -> ability.filter
            is GrantTriggeredAbility -> ability.filter
            is GrantActivatedAbility -> ability.filter
            is GrantWard -> ability.filter
            is GrantProtection -> ability.filter
            is CantBeBlocked -> ability.filter
            else -> null
        } ?: return null
        return group.baseFilter.takeIf { group.scope is Scope.Battlefield && group.chosenSubtypeKey == null }
    }

    /** The spells a static makes cheaper or faster for you. */
    private fun spellFilter(ability: StaticAbility): GameObjectFilter? = when (ability) {
        is ModifySpellCost -> (ability.target as? SpellCostTarget.YouCast)?.filter
            ?.takeIf { reduces(ability.modification) }
        is GrantFlashToSpellType -> ability.filter
        // Improvise granted asks for the artifacts that pay ([payersOf]), not for the spells.
        is GrantKeywordToOwnSpells -> ability.spellFilter.takeIf { payersOf(ability.keyword) == null }
        else -> null
    }

    private fun reduces(modification: CostModification): Boolean = when (modification) {
        is CostModification.ReduceGeneric, is CostModification.ReduceGenericBy, is CostModification.ReduceColored,
        is CostModification.ReduceColoredPerUnit, is CostModification.ReduceColoredIfAnyTargetMatches -> true
        else -> false
    }

    private fun opponents(filter: GameObjectFilter): Boolean =
        filter.controllerPredicate in OPPONENT_SCOPES || filter.anyOf.any(::opponents)

    private fun namesCreatureType(filter: GameObjectFilter): Boolean = namesSubtypeIn(filter, CREATURE_TYPES)

    /** Treasure, Clue, Food…: what token makers make, not what a deck's printed cards are. */
    private fun namesTokenType(filter: GameObjectFilter): Boolean = namesSubtypeIn(filter, TOKEN_ARTIFACT_TYPES)

    private fun namesSubtypeIn(filter: GameObjectFilter, types: Set<String>): Boolean =
        (filter.cardPredicates + filter.anyOf.flatMap { it.cardPredicates }).any { namesSubtypeIn(it, types) }

    private fun namesSubtypeIn(predicate: CardPredicate, types: Set<String>): Boolean = when (predicate) {
        is CardPredicate.And -> predicate.predicates.any { namesSubtypeIn(it, types) }
        is CardPredicate.Or -> predicate.predicates.any { namesSubtypeIn(it, types) }
        is CardPredicate.Not -> namesSubtypeIn(predicate.predicate, types)
        is CardPredicate.HasSubtype -> predicate.subtype.value in types
        is CardPredicate.HasAnyOfSubtypes -> predicate.subtypes.any { it.value in types }
        else -> false
    }

    private val CREATURE_TYPES: Set<String> = Subtype.ALL_CREATURE_TYPES.toSet()

    private val TOKEN_ARTIFACT_TYPES: Set<String> = setOf(
        "Blood", "Clue", "Food", "Gold", "Incubator", "Junk", "Map", "Mutagen", "Powerstone", "Treasure",
    )

    private val OPPONENT_SCOPES: Set<ControllerPredicate> = setOf(
        ControllerPredicate.ControlledByOpponent,
        ControllerPredicate.OwnedByOpponent,
        ControllerPredicate.ControlledByDefendingPlayer,
        ControllerPredicate.ControlledByTargetOpponent,
    )
}
