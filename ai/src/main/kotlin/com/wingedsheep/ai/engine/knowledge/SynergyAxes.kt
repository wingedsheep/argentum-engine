package com.wingedsheep.ai.engine.knowledge

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AdditionalCost
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.filters.unified.Scope
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.conditions.Condition
import com.wingedsheep.sdk.scripting.conditions.PermanentLeftBattlefieldThisTurn
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.effects.AddDynamicCountersEffect
import com.wingedsheep.sdk.scripting.effects.MoveAllCountersEffect
import com.wingedsheep.sdk.scripting.effects.MoveAllLastKnownCountersEffect
import com.wingedsheep.sdk.scripting.effects.MoveCountersEachKindMissingEffect
import com.wingedsheep.sdk.scripting.effects.MoveChosenCountersToTargetEffect
import com.wingedsheep.sdk.scripting.effects.MoveCountersEffect
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CopyEachSpellCastEffect
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.effects.CopyEachTargetSpellEffect
import com.wingedsheep.sdk.scripting.effects.CopyNextSpellCastEffect
import com.wingedsheep.sdk.scripting.effects.CopyTargetSpellEffect
import com.wingedsheep.sdk.scripting.effects.CopyTargetSpellOrAbilityEffect
import com.wingedsheep.sdk.scripting.effects.CreateTokenCopyOfSourceEffect
import com.wingedsheep.sdk.scripting.effects.CreateTokenCopyOfTargetEffect
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.effects.PreventDamageEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect
import com.wingedsheep.sdk.scripting.effects.CreateGlobalTriggeredAbilityEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeTargetEffect
import com.wingedsheep.sdk.scripting.effects.ForEachEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.SurveilEffect
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
import com.wingedsheep.sdk.scripting.targets.TargetSpellOrPermanent
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.TurnTracker
import com.wingedsheep.sdk.scripting.ReplacementEffect
import com.wingedsheep.sdk.scripting.EntersWithExileCounters
import java.util.concurrent.ConcurrentHashMap

/**
 * A resource one card makes and another card cashes in: the unit of synergy.
 *
 * Every axis has a **payoff** side (a card that rewards the event happening — Blood Artist, Ajani's
 * Pridemate, a prowess creature) and a **feeder** side (a card that makes the event happen — a sac
 * outlet, a lifelinker, an instant). A payoff with nothing to feed it is a vanilla body; the same
 * card beside three feeders is an engine. [SynergyAxes] reads both sides off a card's structure.
 */
enum class SynergyAxis {
    /** Creatures dying or being sacrificed. Fed by sacrifice outlets and creature-token makers. */
    DEATH,

    /** Tokens entering or being created. Fed by token makers. */
    TOKENS,

    /** Creatures entering under your control. Fed by creatures and creature-token makers. */
    CREATURES_ENTER,

    /** You gaining life. Fed by life gain and lifelink. */
    LIFEGAIN,

    /** You casting instants and sorceries (or noncreature spells). Fed by instants and sorceries. */
    SPELLS,

    /** +1/+1 counters being put on permanents. Fed by anything that puts them. */
    PLUS_COUNTERS,

    /** Cards in your graveyard. Rewarded per [GraveyardSynergy]; fed by self-mill and surveil. */
    GRAVEYARD,

    /**
     * Cards your opponents own in exile (Battle for Zendikar's ingest/processor pair). Fed by ingest
     * (each hit exiles the top card of the damaged player's library) and by exile removal aimed at
     * their permanents; cashed in by the *processor* cost — "put a card an opponent owns from exile
     * into that player's graveyard" (Cryptic Cruiser, Processor Assault) — which can't be paid with
     * nothing in their exile.
     */
    OPPONENT_EXILE,
}

/**
 * Which axes a card [feeds] and which it [rewards], and the same for creature types: the [tribes]
 * it is or makes (every one, for a changeling: [everyTribe]) and the [tribesRewarded] it asks for
 * ([CreatureTypeSynergy]). The axis sets are empty for most cards; every creature has a tribe.
 *
 * [asks] are the other kinds of card it wants beside it, as the payoff's own filters ([CardAsks]),
 * and [card] is the printed card any other card's asks are matched against ([PrintedFilter]) — so
 * any card can feed an ask, and [card] is set on every profile [SynergyAxes.of] reads.
 *
 * A payoff that [standsAlone] is also an answer — removal, a Pacifism, a counterspell — so its
 * synergy is a rider: fuel makes it better, but it is worth its rating with none.
 *
 * [strength] is how much the payoff gives back per feeder, 1.0 for a typical one ([SynergyStrength]):
 * [DeckSynergy] weighs a fed payoff by it, so a feeder beside a +2/+2 lord gains more than one
 * beside a +1/+1 lord. [punishes] holds the creature types the card *shrinks*, each with how hard —
 * every creature of that type beside it is a loss.
 */
data class SynergyProfile(
    val feeds: Set<SynergyAxis>,
    val rewards: Set<SynergyAxis>,
    val tribes: Set<Subtype> = emptySet(),
    val tribesRewarded: Set<Subtype> = emptySet(),
    val everyTribe: Boolean = false,
    /** Converts our creatures to one chosen tribe; it is not itself a member of every tribe. */
    val grantsChosenTribe: Boolean = false,
    val asks: List<CardAsk> = emptyList(),
    val card: CardDefinition? = null,
    val standsAlone: Boolean = false,
    val strength: Double = 1.0,
    val punishes: Map<Subtype, Double> = emptyMap(),
) {
    /** No synergy reading at all — though [card] may still match another card's [asks]. */
    val isEmpty: Boolean
        get() = feeds.isEmpty() && rewards.isEmpty() && tribes.isEmpty() && tribesRewarded.isEmpty() &&
            !everyTribe && !grantsChosenTribe && asks.isEmpty() && punishes.isEmpty()

    /** Rewards an axis or asks for a kind of card: a card whose value hangs on the rest of the deck. */
    val isPayoff: Boolean get() = grantsChosenTribe || rewards.isNotEmpty() || tribesRewarded.isNotEmpty() || asks.isNotEmpty()

    /** Feeds an axis, is (or makes) some creature type, or is a printed card some ask can match. */
    val isFeeder: Boolean get() = feeds.isNotEmpty() || tribes.isNotEmpty() || everyTribe || card != null

    /** Shrinks some creature type: its members beside it count against the deck. */
    val isPunisher: Boolean get() = punishes.isNotEmpty()

    /** Whether this card is one of the cards [ask] wants. */
    fun answers(ask: CardAsk): Boolean = card != null && !card.typeLine.isLand && SynergyAxes.matches(card, ask.filter)

    fun feedsTribe(tribe: Subtype): Boolean = everyTribe || tribe in tribes

    /** Axes on which this card and [other] complete each other, in either direction. */
    fun sharedWith(other: SynergyProfile): Set<SynergyAxis> =
        (rewards intersect other.feeds) + (feeds intersect other.rewards)

    companion object {
        val NONE = SynergyProfile(emptySet(), emptySet())
    }
}

/**
 * The one structural reading of "what does this card make, and what does it cash in" that every
 * synergy consumer shares — the in-game evaluator's payoff term
 * ([com.wingedsheep.ai.engine.evaluation.SynergyPresence]) and the Commander deck generator's theme
 * score ([com.wingedsheep.ai.engine.deck.CommanderSynergy]).
 *
 * It is read off the card's SDK structure, never its name or its Oracle text:
 *
 *  - **Rewards** are triggered abilities whose [EventPattern] watches an axis's event happening to
 *    *something other than the card itself* ([TriggerBinding.SELF] "when this dies" is not a payoff),
 *    and not scoped to an opponent's objects. Prowess reads as a [SynergyAxis.SPELLS] payoff, and
 *    [GraveyardSynergy.isPayoff] is the [SynergyAxis.GRAVEYARD] payoff reading — that axis is
 *    graveyard-shaped enough (keywords, cast-from-graveyard, threshold counts) that it keeps its own
 *    reader.
 *  - **Feeds** come from what the card's effects do ([EffectWalker] leaves, the same walk
 *    [CardIntentAnalyzer] tags with), its activation and additional costs (a sacrifice), its
 *    keywords (lifelink) and its type line (a creature enters; an instant is cast).
 *
 * Deliberately narrow: static abilities that reward a resource ("creatures you control with +1/+1
 * counters have trample") and keyword mechanics that make tokens on their own (afterlife,
 * fabricate) are not read, so they score as no synergy rather than a guessed one. A false negative
 * costs a missed bonus; a false positive would pull a deck toward cards that do nothing together.
 *
 * Memoized per card name, like [CardIntentAnalyzer] and [GraveyardSynergy].
 */
object SynergyAxes {

    private val cache = ConcurrentHashMap<String, SynergyProfile>()

    fun of(card: CardDefinition): SynergyProfile = cache.getOrPut(card.name) { compute(card) }

    private fun compute(card: CardDefinition): SynergyProfile {
        val scripts = listOf(card.script) + card.cardFaces.map { it.script }
        val rewards = buildSet {
            scripts.flatMap { it.triggeredAbilities }.forEach { ability ->
                addAll(rewardsOf(ability.trigger, ability.binding))
                // Revolt — "if a permanent you controlled left the battlefield this turn" (Call for
                // Unity, Countless Gears Renegade): it pays off what the death axis feeds — a sac
                // outlet, a Servo or Clue to cash in, a creature traded in combat.
                if (ability.interveningIf == PermanentLeftBattlefieldThisTurn(Player.You)) add(SynergyAxis.DEATH)
                // "If you gained life this turn" / "if you gained 3 or more life this turn" (Lone
                // Rider, Angelic Accord, Restless Bloodseeker): the turn's life-gained tracker as the
                // gate pays off what the lifegain axis feeds — a lifelinker, a gain-life spell.
                if (readsLifeGainedThisTurn(ability.interveningIf)) add(SynergyAxis.LIFEGAIN)
            }
            if (Keyword.PROWESS in card.keywords) add(SynergyAxis.SPELLS)
            if (GraveyardSynergy.isPayoff(card)) add(SynergyAxis.GRAVEYARD)
            // The processor cost spends an opponent's exiled card: with none there, the ability
            // can't be activated and the spell can't be cast.
            // So does an as-enters processor (Ulamog's Despoiler): with nothing to put back, it
            // enters without its counters.
            if (scripts.any { script ->
                    script.activatedAbilities.any { processes(it.cost) } || script.additionalCosts.any(::processes) ||
                        script.replacementEffects.any(::processes)
                }
            ) add(SynergyAxis.OPPONENT_EXILE)
            // Carrying counters from one permanent to another — The Ozolith banking a leaving
            // creature's counters and moving them on, Essence Channeler's "put its counters on target
            // creature you control", Aetherborn Marauder gathering them from your other permanents —
            // cashes in the +1/+1 counters the deck puts down: with none
            // placed, the move carries nothing.
            if (scripts.any { script ->
                    (script.triggeredAbilities.map { it.effect } + script.activatedAbilities.map { it.effect })
                        .any(::carriesCounters)
                }
            ) add(SynergyAxis.PLUS_COUNTERS)
            // Copying an instant or sorcery — Fork, Lithoform Engine, "when you next cast an instant
            // or sorcery spell this turn, copy it" — is a second helping of the spells the deck
            // casts: with none to copy, it does nothing. Copying a creature spell (Double Major)
            // rewards creatures, not spells.
            if (scripts.any { script ->
                    copiesSpells(script.spellEffect, script.targetRequirements) ||
                        script.activatedAbilities.any { copiesSpells(it.effect, it.targetRequirements) } ||
                        script.triggeredAbilities.any { copiesSpells(it.effect, it.allTargetRequirements) }
                }
            ) add(SynergyAxis.SPELLS)
        }
        val feeds = buildSet {
            scripts.forEach { addAll(feedsOf(it, card.typeLine.isCreature)) }
            if (card.typeLine.isCreature) add(SynergyAxis.CREATURES_ENTER)
            if (card.typeLine.isInstant || card.typeLine.isSorcery) add(SynergyAxis.SPELLS)
            if (Keyword.LIFELINK in card.keywords) add(SynergyAxis.LIFEGAIN)
            // Ingest exiles the damaged player's top card on every hit; exile removal leaves the
            // opponent's permanent in exile. Both stock what a processor spends.
            if (Keyword.INGEST in card.keywords ||
                IntentTag.EXILE_REMOVAL in CardIntentAnalyzer.analyze(card).tags
            ) add(SynergyAxis.OPPONENT_EXILE)
            // Awaken (CR 702.113): the awakened land gets N +1/+1 counters. The ability is derived
            // from the keyword, not written into the spell's script, so the effect walk misses it.
            if (card.keywordAbilities.any { it is KeywordAbility.Awaken }) {
                add(SynergyAxis.PLUS_COUNTERS)
            }
        }
        return SynergyProfile(
            feeds = feeds,
            rewards = rewards,
            tribes = CreatureTypeSynergy.fed(card),
            tribesRewarded = CreatureTypeSynergy.rewarded(card),
            everyTribe = CreatureTypeSynergy.everyType(card),
            grantsChosenTribe = CreatureTypeSynergy.grantsChosenTribe(card),
            asks = CardAsks.of(card),
            card = card,
            standsAlone = CardIntentAnalyzer.analyze(card).tags.any { it in ANSWERS },
            strength = SynergyStrength.of(card),
            punishes = SynergyStrength.punished(card),
        )
    }

    /**
     * Whether [condition] asks how much life we gained this turn, reaching some amount
     * (`Conditions.YouGainedLifeThisTurn` / `YouGainedLifeThisTurnAtLeast(n)`).
     */
    private fun readsLifeGainedThisTurn(condition: Condition?): Boolean =
        condition is Compare && condition.left == DynamicAmount.TurnTracking(Player.You, TurnTracker.LIFE_GAINED) &&
            (condition.operator == ComparisonOperator.GTE || condition.operator == ComparisonOperator.GT)

    private val matchCache = ConcurrentHashMap<Pair<String, GameObjectFilter>, Boolean>()

    /** [PrintedFilter.matches], memoized per card name and filter; an unreadable answer is a no. */
    internal fun matches(card: CardDefinition, filter: GameObjectFilter): Boolean =
        matchCache.getOrPut(card.name to filter) { PrintedFilter.matches(card, filter) == true }

    // ── Rewards ────────────────────────────────────────────────────────────────

    private fun rewardsOf(pattern: EventPattern, binding: TriggerBinding): Set<SynergyAxis> = when (pattern) {
        is EventPattern.AnyOf -> pattern.events.flatMapTo(mutableSetOf()) { rewardsOf(it, binding) }
        is EventPattern.CountersPlacedEvent ->
            // "Whenever +1/+1 counters are put on this creature" rewards a feeder aimed at it, so the
            // self binding still counts here — unlike a death or an entry, which happen once.
            setOfNotNull(SynergyAxis.PLUS_COUNTERS.takeIf { pattern.counterType == CounterType.PLUS_ONE_PLUS_ONE })
        else -> if (binding == TriggerBinding.SELF) emptySet() else otherRewards(pattern)
    }

    private fun otherRewards(pattern: EventPattern): Set<SynergyAxis> = when (pattern) {
        is EventPattern.ZoneChangeEvent -> when {
            opponents(pattern.filter) -> emptySet()
            // "Whenever an enchanted creature dies" cashes in the deck's Auras ([CardAsks.attachmentAsk]),
            // not its sacrifice fodder.
            pattern.from == Zone.BATTLEFIELD && pattern.to == Zone.GRAVEYARD && couldBeCreature(pattern.filter) ->
                if (CardAsks.attachmentAsk(pattern.filter) != null) emptySet() else setOf(SynergyAxis.DEATH)
            pattern.to == Zone.BATTLEFIELD -> enterAxes(pattern.filter)
            else -> emptySet()
        }
        is EventPattern.PermanentsEnteredEvent ->
            if (opponents(pattern.filter)) emptySet() else enterAxes(pattern.filter)
        is EventPattern.CreaturesYouControlDiedEvent ->
            if (opponents(pattern.filter)) emptySet() else setOf(SynergyAxis.DEATH)
        // "Whenever a player sacrifices a nontoken creature" (Fumulus) cashes in our own sacrifices
        // too, so it rewards the deck's outlets like "whenever you sacrifice" does; an
        // opponent-only watch doesn't.
        is EventPattern.PermanentsSacrificedEvent ->
            if (pattern.sacrificedBy == Player.You || pattern.sacrificedBy == Player.Each) setOf(SynergyAxis.DEATH)
            else emptySet()
        is EventPattern.TokenCreationEvent ->
            if (pattern.controller == Player.You) setOf(SynergyAxis.TOKENS) else emptySet()
        is EventPattern.LifeGainEvent ->
            if (pattern.player == Player.You) setOf(SynergyAxis.LIFEGAIN) else emptySet()
        // "Whenever you cast a spell that targets …" (Frontline Heroism, Vesuvan Duplimancy, Legolas)
        // watches spells that target — in practice instants, sorceries and Auras, the noncreature
        // spells this axis is fed by — so it rewards it like an instant-or-sorcery trigger does.
        is EventPattern.SpellCastEvent ->
            if (pattern.player == Player.You &&
                (rewardsSpells(pattern.spellFilter) || pattern.requires.any(::requiresTargeting))
            ) setOf(SynergyAxis.SPELLS)
            else emptySet()
        // "Whenever you cast your second spell each turn" (Pyromancer's Assault) pays off a turn of
        // two spells, which the deck's cheap instants and sorceries are what make possible.
        is EventPattern.NthSpellCastEvent ->
            if (pattern.player == Player.You && pattern.nthSpell >= 2 &&
                pattern.spellFilter.let { it == null || rewardsSpells(it) }
            ) setOf(SynergyAxis.SPELLS)
            else emptySet()
        // "Whenever a spell or ability you control counters a spell" (Baral) cashes in the deck's
        // counterspells — instants, which is what feeds the spells axis.
        is EventPattern.CounterSpellEvent ->
            if (pattern.counterer == Player.You) setOf(SynergyAxis.SPELLS) else emptySet()
        else -> emptySet()
    }

    private fun enterAxes(filter: GameObjectFilter): Set<SynergyAxis> = when {
        predicates(filter).any { it == CardPredicate.IsToken } -> setOf(SynergyAxis.TOKENS)
        couldBeCreature(filter) && predicates(filter).any { it == CardPredicate.IsCreature } ->
            setOf(SynergyAxis.CREATURES_ENTER)
        else -> emptySet()
    }

    /** "Whenever you cast an instant or sorcery / a noncreature spell" — not "whenever you cast a spell". */
    private fun rewardsSpells(filter: GameObjectFilter): Boolean =
        predicates(filter).any { it in SPELL_PREDICATES }

    /**
     * The filter watches someone else's permanents: an opponent's, or — on a Curse — the enchanted
     * player's ("whenever a creature enchanted player controls enters", Trespasser's Curse), which
     * the AI aims at an opponent, so the deck's own creatures entering feed nothing.
     */
    private fun opponents(filter: GameObjectFilter): Boolean = when (val pred = filter.controllerPredicate) {
        ControllerPredicate.ControlledByOpponent -> true
        is ControllerPredicate.ControlledByReferencedPlayer ->
            (pred.target as? EffectTarget.PlayerRef)?.player == Player.EnchantedPlayer
        else -> false
    }

    /** False only when the filter names a card type that excludes creatures (a land, an artifact). */
    /** Whether a permanent matching [filter] could be a creature — no noncreature type rules it out. */
    internal fun couldBeCreature(filter: GameObjectFilter): Boolean {
        val preds = predicates(filter)
        return CardPredicate.IsCreature in preds || preds.none { it in NONCREATURE_TYPES }
    }

    /** Every card predicate the filter names, with `Or` unions ("instant or sorcery") flattened. */
    private fun predicates(filter: GameObjectFilter): List<CardPredicate> =
        (filter.cardPredicates + filter.anyOf.flatMap { it.cardPredicates }).flatMap(::flatten)

    private fun flatten(predicate: CardPredicate): List<CardPredicate> =
        if (predicate is CardPredicate.Or) predicate.predicates.flatMap(::flatten) else listOf(predicate)

    // ── Feeds ──────────────────────────────────────────────────────────────────

    private fun feedsOf(script: CardScript, creatureSource: Boolean): Set<SynergyAxis> = buildSet {
        script.spellEffect?.let { addAll(effectFeeds(it, creatureSource, script.targetRequirements)) }
        script.triggeredAbilities.forEach { addAll(effectFeeds(it.effect, creatureSource, it.allTargetRequirements)) }
        script.sagaChapters.forEach { addAll(effectFeeds(it.effect, creatureSource, EffectWalker.chapterTargets(it))) }
        for (ability in script.activatedAbilities.filterNot { it.isManaAbility }) {
            addAll(effectFeeds(ability.effect, creatureSource, ability.targetRequirements))
            if (sacrifices(ability.cost, creatureSource)) add(SynergyAxis.DEATH)
            // "{T}, Discard a card: …" bins our cards as a discard effect does (Murmuring Volume).
            if (discards(ability.cost)) add(SynergyAxis.GRAVEYARD)
        }
        // "Enchanted creature has '{T}, Discard a card: Draw a card'" (Prophetic Ravings), an
        // Equipment's "equipped creature has '…'": the ability handed to the host is ours to
        // activate, so its cost and effect feed what the same ability printed on the host would.
        // A curse Aura's grant (Compulsory Rest's "{2}, Sacrifice this creature: You gain 2 life")
        // is the opponent's to activate, so it feeds nothing of ours.
        val curse = TargetPolarityAnalyzer.cursesHost(script.staticAbilities)
        for (static in script.staticAbilities) {
            val grant = static as? GrantActivatedAbility ?: continue
            if (curse || grant.filter.scope != Scope.AttachedTo || grant.ability.isManaAbility) continue
            val creatureHost = couldBeCreature(grant.filter.baseFilter)
            addAll(effectFeeds(grant.ability.effect, creatureHost, grant.ability.targetRequirements))
            if (sacrifices(grant.ability.cost, creatureHost)) add(SynergyAxis.DEATH)
            if (discards(grant.ability.cost)) add(SynergyAxis.GRAVEYARD)
        }
        // "Enchanted creature has 'whenever this creature attacks, create a 4/4 Angel …'"
        // (Invocation of Saint Traft): a helpful trigger handed to our host makes what the same
        // trigger printed on it would. A harmful one (Consuming Fervor's upkeep -1/-1 counter) is
        // the gift's price and feeds nothing.
        for (static in script.staticAbilities) {
            val grant = static as? GrantTriggeredAbility ?: continue
            if (curse || grant.filter.scope != Scope.AttachedTo) continue
            if (TargetPolarityAnalyzer.grantedTriggerPolarity(grant.ability) != TargetPolarity.BENEFICIAL) continue
            addAll(effectFeeds(grant.ability.effect, couldBeCreature(grant.filter.baseFilter), grant.ability.allTargetRequirements))
        }
        if (script.additionalCosts.any(::sacrifices)) add(SynergyAxis.DEATH)
        // "As an additional cost, discard a card / X cards" bins our cards like a discard effect
        // (Nahiri's Wrath, Channeled Force, Tormenting Voice).
        if (script.additionalCosts.any(::discards)) add(SynergyAxis.GRAVEYARD)
    }

    private fun effectFeeds(
        effect: Effect,
        creatureSource: Boolean,
        requirements: List<TargetRequirement> = emptyList(),
    ): Set<SynergyAxis> = buildSet {
        for (leaf in EffectWalker.leaves(effect)) {
            // "Put a +1/+1 counter on each other creature you control" is a ForEach whose body is
            // the feeding leaf; the walk stops at ForEach bodies, so open them here.
            if (leaf is ForEachEffect) {
                addAll(effectFeeds(leaf.body, creatureSource, requirements))
                continue
            }
            // A global trigger produces the same resources when its event happens, including
            // discards from a combat-damage loot. The event is a condition, not a new resource.
            if (leaf is CreateGlobalTriggeredAbilityEffect) {
                addAll(effectFeeds(leaf.ability.effect, creatureSource))
                continue
            }
            // "You may discard a card. If you do, draw a card / put a +1/+1 counter on it" (Nahiri,
            // the Harbinger; Olivia, Mobilized for War): a gate we decide produces what its payoff
            // does, and its action or cost bins our cards like an additional-cost discard does. A
            // gate an opponent decides (the punisher shape) is their call, so it feeds nothing here.
            if (leaf is com.wingedsheep.sdk.scripting.effects.GatedEffect && leaf.decisionMaker == null) {
                addAll(effectFeeds(leaf.then, creatureSource, requirements))
                leaf.otherwise?.let { addAll(effectFeeds(it, creatureSource, requirements)) }
                when (val gate = leaf.gate) {
                    is com.wingedsheep.sdk.scripting.effects.Gate.DoAction ->
                        addAll(effectFeeds(gate.action, creatureSource, requirements))
                    is com.wingedsheep.sdk.scripting.effects.Gate.MayPay ->
                        addAll(effectFeeds(gate.cost, creatureSource, requirements))
                    else -> {}
                }
                continue
            }
            // A delayed trigger produces what its body does when it fires — and "sacrifice it at the
            // beginning of the next end step" (Hazoret's Favor, Junkyo Bell, Through the Breach) is a
            // creature of ours dying, whatever the scheduling ability got for it.
            if (leaf is CreateDelayedTriggerEffect) {
                addAll(effectFeeds(leaf.effect, creatureSource, requirements))
                continue
            }
            // We sacrifice a creature of our own — the only kind we can sacrifice — chosen as a
            // creature target, or the source itself when it is a creature: a death, the sac outlet's
            // feed. A land or artifact sacrificed is no death; "its controller sacrifices it" can be
            // an opponent's edict, and stays unread here.
            if (leaf is SacrificeTargetEffect && !leaf.sacrificedByItsController &&
                sacrificesACreature(leaf.target, requirements, creatureSource)
            ) add(SynergyAxis.DEATH)
            // The reaction a prevention shield carries ("for each 1 damage prevented this way,
            // create a 2/1 Inkling", Inkshield) produces what it does when the shield fires.
            if (leaf is PreventDamageEffect) leaf.onPrevented?.let { addAll(effectFeeds(it, creatureSource)) }
            addAll(leafFeeds(leaf))
            val tags = CardIntentAnalyzer.analyzeEffect(leaf).tags
            if (IntentTag.LIFEGAIN in tags) add(SynergyAxis.LIFEGAIN)
            if (IntentTag.TOKEN_MAKER in tags) add(SynergyAxis.TOKENS)
        }
    }

    private fun sacrificesACreature(
        target: EffectTarget,
        requirements: List<TargetRequirement>,
        creatureSource: Boolean,
    ): Boolean = when (target) {
        EffectTarget.Self -> creatureSource
        is EffectTarget.ContextTarget -> isCreatureSlot(requirements.getOrNull(target.index))
        // A DSL handle (`val creature = target(...)`); a count-N requirement's handles are `id[i]`.
        is EffectTarget.BoundVariable -> target.name.substringBefore('[')
            .let { id -> isCreatureSlot(requirements.firstOrNull { it.id == id }) }
        else -> false
    }

    private fun isCreatureSlot(requirement: TargetRequirement?): Boolean =
        (requirement as? TargetObject)?.let { CardPredicate.IsCreature in it.filter.baseFilter.cardPredicates } ?: false

    private fun leafFeeds(leaf: Any): Set<SynergyAxis> = when (leaf) {
        // A creature token is a body that enters and a body to sacrifice.
        is CreateTokenEffect, is CreateTokenCopyOfSourceEffect, is CreateTokenCopyOfTargetEffect ->
            setOf(SynergyAxis.TOKENS, SynergyAxis.CREATURES_ENTER, SynergyAxis.DEATH)
        is AddCountersEffect -> plusCounters(leaf.counterType)
        is AddDynamicCountersEffect -> plusCounters(leaf.counterType)
        // "Tap any number of untapped Vampires you control; put a +1/+1 counter on each" (Urge to
        // Feed): counters on each permanent of a gathered collection stock the same axis.
        is com.wingedsheep.sdk.scripting.effects.AddCountersToCollectionEffect -> plusCounters(leaf.counterType)
        // "Distribute four +1/+1 counters among any number of target creatures you control" (Verdurous Gearhulk).
        is com.wingedsheep.sdk.scripting.effects.DistributeCountersAmongTargetsEffect -> plusCounters(leaf.counterType)
        is SurveilEffect -> setOf(SynergyAxis.GRAVEYARD)
        // Explore puts a +1/+1 counter on the explorer for a nonland card, which it may bin like a
        // surveil — both stocks Jadelight Ranger and its kin keep filling.
        is com.wingedsheep.sdk.scripting.effects.ExploreEffect -> setOf(SynergyAxis.PLUS_COUNTERS, SynergyAxis.GRAVEYARD)
        is com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect -> {
            val destination = leaf.destination as? com.wingedsheep.sdk.scripting.effects.CardDestination.ToZone
            // A discard, or a plain move of gathered cards into your own graveyard — "search your
            // library for a card, put it into your graveyard" (Entomb, Vizier of the Anointed) —
            // stocks the graveyard the same way a self-mill does.
            val binsOwnCards = destination?.player == Player.You && (
                leaf.moveType == com.wingedsheep.sdk.scripting.effects.MoveType.Discard ||
                    (leaf.moveType == com.wingedsheep.sdk.scripting.effects.MoveType.Default && destination.zone == Zone.GRAVEYARD)
                )
            if (binsOwnCards) setOf(SynergyAxis.GRAVEYARD) else emptySet()
        }
        is GatherCardsEffect -> {
            val source = leaf.source
            if (source is CardSource.TopOfLibrary && source.isMill && source.player == Player.You) {
                setOf(SynergyAxis.GRAVEYARD)
            } else {
                emptySet()
            }
        }
        else -> emptySet()
    }

    private fun carriesCounters(effect: Effect): Boolean = EffectWalker.leaves(effect).any {
        it is MoveAllCountersEffect || it is MoveAllLastKnownCountersEffect || it is MoveCountersEffect ||
            it is MoveChosenCountersToTargetEffect || it is MoveCountersEachKindMissingEffect
    }

    /**
     * Whether [effect] copies an instant or sorcery: a delayed "copy the next / each spell you cast"
     * whose filter names one, or a "copy target spell" whose target requirement does.
     */
    private fun copiesSpells(effect: Effect?, requirements: List<TargetRequirement>): Boolean {
        if (effect == null) return false
        val leaves = EffectWalker.leaves(effect).toList()
        if (leaves.any {
                (it is CopyNextSpellCastEffect && rewardsSpells(it.spellFilter)) ||
                    (it is CopyEachSpellCastEffect && rewardsSpells(it.spellFilter))
            }
        ) return true
        val copiesTarget = leaves.any {
            it is CopyTargetSpellEffect || it is CopyTargetSpellOrAbilityEffect || it is CopyEachTargetSpellEffect
        }
        return copiesTarget && requirements.any { req ->
            when (req) {
                is TargetObject -> rewardsSpells(req.filter.baseFilter)
                is TargetSpellOrPermanent -> req.spellFilter?.let(::rewardsSpells) ?: false
                else -> false
            }
        }
    }

    private fun plusCounters(type: CounterType): Set<SynergyAxis> =
        if (type == CounterType.PLUS_ONE_PLUS_ONE) setOf(SynergyAxis.PLUS_COUNTERS) else emptySet()

    private fun discards(cost: AbilityCost): Boolean = when (cost) {
        is AbilityCost.Atom -> cost.atom is CostAtom.Discard
        // "Discard X cards" (Gix) and "discard a card with mana value X" (Kozilek) stock it too.
        is AbilityCost.DiscardX -> true
        is AbilityCost.Composite -> cost.costs.any(::discards)
        else -> false
    }

    private fun sacrifices(cost: AbilityCost, creatureSource: Boolean): Boolean = when (cost) {
        is AbilityCost.Atom -> cost.atom is CostAtom.Sacrifice
        is AbilityCost.Composite -> cost.costs.any { sacrifices(it, creatureSource) }
        AbilityCost.SacrificeSelf -> creatureSource
        else -> false
    }

    private fun discards(cost: AdditionalCost): Boolean = when (cost) {
        is AdditionalCost.Atom -> cost.atom is CostAtom.Discard
        is AdditionalCost.VariableCards -> cost.action == com.wingedsheep.sdk.scripting.CardCostAction.DISCARD
        is AdditionalCost.Composite -> cost.steps.any(::discards)
        else -> false
    }

    /** A processor cost: a card leaves an opponent's exile for their graveyard ([CostAtom.ExileFrom.exiles] false). */
    private fun processes(atom: CostAtom): Boolean =
        atom is CostAtom.ExileFrom && atom.zone == Zone.EXILE && !atom.exiles

    /** An as-enters move out of exile that isn't a linked exile — the processor shape. */
    private fun processes(effect: ReplacementEffect): Boolean =
        effect is EntersWithExileCounters &&
            effect.sourceZone == Zone.EXILE && effect.destination != Zone.EXILE

    private fun processes(cost: AbilityCost): Boolean = when (cost) {
        is AbilityCost.Atom -> processes(cost.atom)
        is AbilityCost.Composite -> cost.costs.any(::processes)
        else -> false
    }

    private fun processes(cost: AdditionalCost): Boolean = when (cost) {
        is AdditionalCost.Atom -> processes(cost.atom)
        is AdditionalCost.Composite -> cost.steps.any(::processes)
        else -> false
    }

    private fun sacrifices(cost: AdditionalCost): Boolean = when (cost) {
        is AdditionalCost.Atom -> cost.atom is CostAtom.Sacrifice
        is AdditionalCost.Composite -> cost.steps.any(::sacrifices)
        else -> false
    }

    /** What makes a card an answer, worth playing whatever the rest of the deck does. */
    private val ANSWERS: Set<IntentTag> = setOf(
        IntentTag.REMOVAL, IntentTag.EXILE_REMOVAL, IntentTag.SWEEPER, IntentTag.NEUTRALIZE, IntentTag.FIGHT,
        IntentTag.COUNTERSPELL,
    )

    /**
     * The cast-trigger requirement holds only for a spell that targets something — only a
     * noncreature spell, in practice. An `AnyOf` qualifies when every option does.
     */
    private fun requiresTargeting(predicate: SpellCastPredicate): Boolean = when (predicate) {
        SpellCastPredicate.TargetsSource, SpellCastPredicate.TargetsOnlySource,
        SpellCastPredicate.TargetsOpponent, is SpellCastPredicate.TargetsMatching,
        is SpellCastPredicate.CouldTargetOther -> true
        is SpellCastPredicate.AnyOf -> predicate.options.isNotEmpty() && predicate.options.all(::requiresTargeting)
        is SpellCastPredicate.CastFromZone, is SpellCastPredicate.CastFromZoneOtherThan,
        SpellCastPredicate.WasKicked, is SpellCastPredicate.PaidWithManaFromSubtype,
        is SpellCastPredicate.PaidWithManaFromCardType, SpellCastPredicate.PaidWithSnowManaOfItsColors,
        SpellCastPredicate.PaidWithManaFromSource, SpellCastPredicate.IsModal, SpellCastPredicate.HasXInCost,
        SpellCastPredicate.NotOwnedByController,
        SpellCastPredicate.CastAsAdventure, SpellCastPredicate.CastAsPrepareSpell, SpellCastPredicate.IsCard,
        is SpellCastPredicate.SpellMatches -> false
    }

    private val SPELL_PREDICATES: Set<CardPredicate> =
        setOf(CardPredicate.IsInstant, CardPredicate.IsSorcery, CardPredicate.IsNoncreature)

    private val NONCREATURE_TYPES: Set<CardPredicate> = setOf(
        CardPredicate.IsLand, CardPredicate.IsArtifact, CardPredicate.IsEnchantment, CardPredicate.IsPlaneswalker,
        CardPredicate.IsBattle, CardPredicate.IsInstant, CardPredicate.IsSorcery, CardPredicate.IsNoncreature,
    )
}
