package com.wingedsheep.ai.engine.knowledge

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardFace
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.ActivatedAbilityReach
import com.wingedsheep.sdk.scripting.CascadeMayPutOntoBattlefield
import com.wingedsheep.sdk.scripting.CompositeStaticAbility
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.IncreaseActivatedAbilityCost
import com.wingedsheep.sdk.scripting.MayCastFromGraveyard
import com.wingedsheep.sdk.scripting.MayCastSelfFromZones
import com.wingedsheep.sdk.scripting.MayCastWithoutPayingManaCost
import com.wingedsheep.sdk.scripting.AssignDamageEqualToToughness
import com.wingedsheep.sdk.scripting.CanAttackDespiteDefender
import com.wingedsheep.sdk.scripting.GrantKeywordToOwnSpells
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.ReduceActivatedAbilityCost
import com.wingedsheep.sdk.scripting.RedirectZoneChange
import com.wingedsheep.sdk.scripting.RedirectZoneChangeWith
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantOpponentsCantWinGame
import com.wingedsheep.sdk.scripting.GrantCantLoseGame
import com.wingedsheep.sdk.scripting.CantBeBlocked
import com.wingedsheep.sdk.scripting.CantBeBlockedBy
import com.wingedsheep.sdk.scripting.CantBeBlockedByMoreThan
import com.wingedsheep.sdk.scripting.CantAttack
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.CanOnlyBlockCreaturesWith
import com.wingedsheep.sdk.scripting.CanBlockAdditionalForCreatureGroup
import com.wingedsheep.sdk.scripting.CantAttackUnless
import com.wingedsheep.sdk.scripting.CantAttackUnlessCoAttacker
import com.wingedsheep.sdk.scripting.CantBlockUnless
import com.wingedsheep.sdk.scripting.CantBlockUnlessCoBlocker
import com.wingedsheep.sdk.scripting.MustAttack
import com.wingedsheep.sdk.scripting.MustBlock
import com.wingedsheep.sdk.scripting.MustBeBlocked
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.AdditionalCost
import com.wingedsheep.sdk.scripting.GrantMayCastFromLinkedExile
import com.wingedsheep.sdk.scripting.MayPlayCardsFromExile
import com.wingedsheep.sdk.scripting.costs.PermanentCostAction
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.StaticAbility
import com.wingedsheep.sdk.scripting.UntapLimitPerStep
import com.wingedsheep.sdk.scripting.GrantChosenSubtype
import com.wingedsheep.sdk.scripting.SuppressZoneChangeTriggers
import com.wingedsheep.sdk.scripting.PlayersCantCastSpells
import com.wingedsheep.sdk.scripting.PlayersCantGetCounters
import com.wingedsheep.sdk.scripting.CantReceiveCounters
import com.wingedsheep.sdk.scripting.RedirectTokenCreationController
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.effects.LoseLifeEffect
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.costs.PayCost
import com.wingedsheep.sdk.scripting.effects.*
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.Scope
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.LoseAllAbilities
import com.wingedsheep.sdk.scripting.SetBasePowerToughnessStatic
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetOpponent
import com.wingedsheep.sdk.scripting.targets.TargetOther
import com.wingedsheep.sdk.scripting.targets.TargetPlayer
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import java.util.concurrent.ConcurrentHashMap

/**
 * Derives a [CardIntent] from a [CardDefinition] — structurally, for every card the engine can
 * load, with no per-card table.
 *
 * ## What it reads
 *
 * Two passes over the definition, because the two carry different information:
 *
 *  1. **Effects**, via [EffectWalker] — the same traversal
 *     [com.wingedsheep.ai.engine.LimitedCardRater] uses. This is what recognizes "destroys a
 *     permanent", "taps something", "draws two".
 *  2. **Structure**, read directly — static abilities (an anthem is not an effect), activation
 *     costs (a sacrifice outlet is defined by its *cost*), mana abilities (a rock is a permanent
 *     that makes mana), and where an ability lives (repeatable or not).
 *
 * ## What it does not read
 *
 * Modal interiors, `ForEach` bodies and pipeline stages are leaves to the walk — see [EffectWalker]
 * for why widening it is a separate change. This analyzer opens the first two itself in [tagsOf]: a
 * `ForEach` reads as its body, and a modal spell as the union of its modes (a cast of one reads as
 * the modes it chose — [analyzeModes]). A card whose whole payoff is inside a pipeline still reads
 * as uninterpretable and gets [CardIntent.UNKNOWN]'s flat prior, which is exactly the value the AI
 * gave *every* non-creature permanent before this existed. The failure mode is "no better than
 * before", never "confidently wrong".
 *
 * ## Faces
 *
 * A multi-face card splits its rules text across [CardDefinition.script] and one or more
 * [CardFace.script]s — a Room (CR 709.5), a split card (709), an Adventure (715), an Omen, a modal
 * DFC (712). Reading only the top level reported a Room as uninterpretable altogether, which is how
 * `Unholy Annex // Ritual Chamber` — a repeatable draw engine — priced identically to a vanilla
 * enchantment.
 *
 * There is no single right answer for such a card, so there are three entry points and each
 * answers a different question:
 *
 *  - [analyze] — "what can this *card* do, anywhere?" Every face unioned. A tag is already an
 *    anywhere-on-this-card claim within one script, so this is the same reading widened. It is what
 *    a card in hand is worth deciding about: casting `Virtue of Loyalty` as its Adventure half is a
 *    real option, and hiding that would make the AI blind to it.
 *  - [analyzeSelf] — "what is this card doing as a *permanent*?" Top level only. In every layout
 *    the SDK has, the face that can sit on the battlefield is the top-level one and the extra faces
 *    are spells that never stay there (an Adventure/Omen/modal-DFC back resolves to exile, library
 *    or graveyard; neither half of a split card is a permanent). So an already-spent Adventure must
 *    not inflate the enchantment it left behind.
 *  - [analyzeFace] — "what is this one face doing?" A Room is the exception to [analyzeSelf]: its
 *    top-level script is empty and its halves are permanents, so a Room permanent is read per
 *    *unlocked* door.
 *
 * `IntentCatalog.forPermanent` is the seam that picks between the last two; nothing else should
 * have to know which layout it is holding.
 *
 * ## Purity and caching
 *
 * Each entry point is a pure function of the definition, so results are memoized process-wide by
 * card name. A card name resolves to one canonical [CardDefinition] in a
 * [com.wingedsheep.engine.registry.CardRegistry] (later printings are `Printing` rows, not
 * duplicate definitions), so the name is a sound key and the cost is paid once per process rather
 * than once per game — which matters when the arena plays a thousand of them. The three readings
 * get three caches rather than one keyed by a synthesized string, so no naming convention has to
 * hold for them to stay apart.
 */
object CardIntentAnalyzer {

    /** What a prevention shield's reaction is read as making (see the [PreventDamageEffect] branch of [tagsOf]). */
    private val SHIELD_REACTION_PAYOFFS = setOf(IntentTag.TOKEN_MAKER, IntentTag.LIFEGAIN)

    private val cardCache = ConcurrentHashMap<String, CardIntent>()
    private val selfCache = ConcurrentHashMap<String, CardIntent>()
    private val inPlayCache = ConcurrentHashMap<String, CardIntent>()
    private val ownSweepCache = ConcurrentHashMap<String, Boolean>()
    private val faceCache = ConcurrentHashMap<FaceKey, CardIntent>()

    private data class FaceKey(val cardName: String, val faceName: String)
    private val sagaInPlayCache = ConcurrentHashMap<Pair<String, Int>, CardIntent>()

    /** The [CardIntent] for [card] — its own script and every face's — computing it on first sight. */
    fun analyze(card: CardDefinition): CardIntent =
        cardCache.getOrPut(card.name) { compute(card, listOf(card.script) + card.cardFaces.map { it.script }) }

    /**
     * The [CardIntent] of [card] read as the permanent it becomes: its top-level script alone,
     * with the spell faces (Adventure, Omen, modal-DFC back) left out.
     *
     * For a single-face card this is exactly [analyze]. For a Room it is empty and useless — use
     * [analyzeFace] per unlocked door instead.
     */
    fun analyzeSelf(card: CardDefinition): CardIntent =
        if (card.cardFaces.isEmpty()) analyze(card)
        else selfCache.getOrPut(card.name) { compute(card, listOf(card.script)) }

    /**
     * [analyzeSelf] for a permanent already on the battlefield: its "when this enters" triggers
     * left out, because they have already happened.
     *
     * The board value a noncreature permanent earns from its prior is a claim about what it *does
     * from here on*. Ajani's Anguish's prior is a removal spell's, and read whole it kept that
     * value after its enter trigger had dealt its damage — so casting it for X=1 at an empty board
     * scored a permanent worth a removal spell plus the one damage, and the AI fired it the first
     * turn it could (2026-11-02 user report). The cast's leaf already counts what the trigger did:
     * the simulation resolves it.
     *
     * An O-Ring keeps its value through `BoardPresence`'s own linked-exile floor, which reads the
     * card it is holding rather than the trigger that took it. For a Room it is no different from
     * [analyzeSelf] (empty); use [analyzeFace] per unlocked door.
     */
    fun analyzeInPlay(card: CardDefinition): CardIntent = inPlayCache.getOrPut(card.name) {
        inPlayOf(card, spentChapters = 1)
    }

    /**
     * [analyzeInPlay] for a Saga carrying [loreCounters] lore counters: every chapter numbered at
     * or below that count has already triggered (CR 714.2b), so the Saga is worth only the chapters
     * still ahead of it. A Saga about to sacrifice itself after its last chapter reads as nothing
     * more than the permanent it is; one whose removal chapter is still to come reads as removal.
     */
    fun analyzeSagaInPlay(card: CardDefinition, loreCounters: Int): CardIntent {
        if (card.sagaChapters.isEmpty()) return analyzeInPlay(card)
        val spent = loreCounters.coerceAtLeast(0)
        return sagaInPlayCache.getOrPut(card.name to spent) { inPlayOf(card, spentChapters = spent) }
    }

    private fun inPlayOf(card: CardDefinition, spentChapters: Int): CardIntent {
        val entries = card.script.triggeredAbilities.filter(::isOwnEntry)
        // An ability activated only from another zone — "{1}{W}, Discard this card: …" from the
        // hand (Gideon's Memorial), a graveyard return — is no longer the permanent's to use.
        val offBoard = card.script.activatedAbilities.filter { it.activateFromZone != Zone.BATTLEFIELD }
        // A Saga's chapter I triggers as it enters, like an ETB; the later ones are still to come.
        val spent = card.script.sagaChapters.filter { it.chapter <= spentChapters }
        if (entries.isEmpty() && offBoard.isEmpty() && spent.isEmpty()) return analyzeSelf(card)
        return compute(
            card,
            listOf(
                card.script.copy(
                    triggeredAbilities = card.script.triggeredAbilities - entries.toSet(),
                    activatedAbilities = card.script.activatedAbilities - offBoard.toSet(),
                    sagaChapters = card.script.sagaChapters - spent.toSet(),
                ),
            ),
        )
    }

    /** "When this enters" — bound to the permanent itself, on its way onto the battlefield. */
    private fun isOwnEntry(trigger: TriggeredAbility): Boolean {
        if (trigger.binding != TriggerBinding.SELF) return false
        val pattern = trigger.trigger as? EventPattern.ZoneChangeEvent ?: return false
        return pattern.to == Zone.BATTLEFIELD
    }

    /**
     * The [CardIntent] of one face of [card], read as what that face contributes on its own.
     *
     * This is the reading a Room's battlefield value needs: a locked door's text does not exist
     * (CR 709.5), so a permanent is worth what its *unlocked* faces do, not what the card as a
     * whole could do. The face is analyzed against the card's own type line and keywords, which is
     * sound for a Room because both halves share the printed type line (CR 709.5a) — and it is the
     * card that is (or is not) a permanent.
     */
    fun analyzeFace(card: CardDefinition, face: CardFace): CardIntent =
        faceCache.getOrPut(FaceKey(card.name, face.name)) { compute(card, listOf(face.script)) }

    /**
     * What one [effect] does, with no card behind it — the reading an ability *already on the
     * stack* needs.
     *
     * A triggered or activated ability on the stack is its own object: it carries its effect and
     * no card (see `StackResolver.putActivatedAbility`), so [analyze] cannot reach it and reading
     * the source card instead would answer a different question — an Icy Manipulator's tap ability
     * is not everything Icy Manipulator does. Only the effect-derived half of a [CardIntent] is
     * knowable here; the rest keeps [CardIntent.UNKNOWN]'s values, and `speed` in particular is
     * meaningless for something that is already resolving.
     *
     * Not memoized, unlike the card readings: an ability on the stack is a fresh object each time
     * (its effect can be rewritten by text-changing effects and X), so there is no sound key. The
     * fold is one pass over an effect tree and the only caller walks a stack of at most a few.
     */
    fun analyzeEffect(
        effect: Effect,
        requirements: List<TargetRequirement> = emptyList(),
        turnAhead: Boolean = false,
        ownPile: Boolean = false,
        pileCast: Set<IntentTag> = emptySet(),
        /**
         * The card the effect is printed on, when known: a creature-type change reads through that
         * card's own statics ([TypeEvasion]). The card-level readings need it for nothing — every
         * card [TypeEvasion] reads carries the [CantBeBlockedBy] that already tags it
         * [IntentTag.EVASION_GRANT].
         */
        source: CardDefinition? = null,
    ): CardIntent {
        val tags = pipelineTags(effect, turnAhead = turnAhead, ownPile = ownPile, pileCast = pileCast).toMutableSet()
        var removalReach: Int? = null
        val refunds = handRefundDraws(effect) + refundSearches(effect)
        for (leaf in readingLeaves(effect, requirements)) {
            tags += refundedTagsOf(leaf, refunds)
            // "Target creature becomes a Coward" beside "Cowards can't block Warriors" is the
            // single-creature can't-block ([CantBlockEffect]'s evasion grant); "becomes a Warrior"
            // is the evasion itself ([GrantEvasionKeywordEffect]'s).
            if (leaf is SetCreatureSubtypesEffect && TypeEvasion.of(source, leaf) != null) tags += IntentTag.EVASION_GRANT
            reachOf(leaf)?.let { removalReach = maxOf(removalReach ?: 0, it) }
        }
        return CardIntent.UNKNOWN.copy(tags = tags, removalReach = removalReach)
    }

    /**
     * What casting [card] with the modes at [modeIndices] does — the reading a *cast* of a modal
     * spell needs, where [analyze] answers what the card could do with any of them.
     *
     * The two differ exactly where timing is decided: a charm that can counter a spell *or* shrink
     * a creature is not dead on an empty stack once the shrink is the mode chosen, and a charm whose
     * chosen mode is "creatures you control get +1/+2 until end of turn" is a combat trick to hold,
     * which no union of its modes can say. Read as the card with that spell effect, so speed and
     * everything structural come from the card as before.
     *
     * Null when [card] is not a single-face modal spell or an index names no mode — the caller then
     * keeps the card's own reading. Not memoized: a few modes are one cheap pass.
     */
    fun analyzeModes(card: CardDefinition, modeIndices: List<Int>): CardIntent? {
        if (card.cardFaces.isNotEmpty() || modeIndices.isEmpty()) return null
        val modal = card.script.spellEffect as? ModalEffect ?: return null
        val chosen = modeIndices.map { modal.modes.getOrNull(it)?.effect ?: return null }
        val effect = chosen.singleOrNull() ?: CompositeEffect(chosen)
        return compute(card, listOf(card.script.copy(spellEffect = effect)))
    }

    /**
     * The intent [scripts] add up to, read as belonging to [card].
     *
     * Every caller passes the scripts that are in force for what it is asking about — the whole
     * card for [analyze], the top level for [analyzeSelf], one face for [analyzeFace] — and
     * nothing here reads [card] for anything
     * but its printed characteristics (type line, keywords), which faces share.
     */
    private fun compute(card: CardDefinition, scripts: List<CardScript>): CardIntent {
        // Every effect the card can produce, flattened. A tag answers "does this card do X
        // anywhere?", so where in the script the effect sat does not change the answer — the
        // origin discounts [EffectWalker.slots] carries are the rater's concern, not this one's.
        // One walk per script: a slot's effect can be built fresh by each [EffectWalker.slots] call
        // ([EffectWalker.readableEffect]), so the turn-ahead roots must come from the same list.
        val slots = scripts.associateWith { EffectWalker.slots(it) }
        val roots = scripts.flatMap { script ->
            slots.getValue(script).map { it.effect } +
                script.stateTriggeredAbilities.map { it.effect }
        }
        val turnAheadRoots = scripts.flatMap { script -> slots.getValue(script).filter { it.turnAhead }.map { it.effect } }
        val leaves = scripts.flatMap { script ->
            slots.getValue(script).flatMap { readingLeaves(it.effect, it.targets) } +
                script.stateTriggeredAbilities.flatMap { readingLeaves(it.effect) }
        }

        val tags = mutableSetOf<IntentTag>()
        var removalReach: Int? = null
        var cardsDrawn: Int? = null
        var expiringPump = false
        var pumpToughness = 0
        var expiringGroupPump = false
        // Hand selection reads gated interiors too — see [gateInteriorLeaves] for why only here.
        val handLeaves = scripts.flatMap { script ->
            EffectWalker.slots(script).map { it.effect } +
                script.stateTriggeredAbilities.map { it.effect }
        }.flatMap { gateInteriorLeaves(it) }
        var gathersLibraryTop = false
        var movesToHand = false
        val wishFilters = mutableListOf<GameObjectFilter>()
        for (effect in handLeaves) {
            if (effect is GatherCardsEffect && effect.source is CardSource.TopOfLibrary) gathersLibraryTop = true
            wishFilterOf(effect)?.let { wishFilters += it }
            if (effect is MoveCollectionEffect && (effect.destination as? CardDestination.ToZone)?.zone == Zone.HAND) {
                movesToHand = true
            }
        }

        val ownPile = stashesOwnCards(scripts)
        val pileCast = pileCastTags(scripts)
        for (root in roots) tags += pipelineTags(root, turnAhead = turnAheadRoots.any { it === root }, ownPile = ownPile, pileCast = pileCast)
        if (roots.any(::isExpiringProtectionTrick)) expiringPump = true
        // "Discard a card: draw a card" (Murmuring Volume) trades a card for a card: it filters the
        // hand, it gains none. Its draw reads as the card selection it is.
        val rummageLeaves = scripts.flatMap { it.activatedAbilities }.filter(::isRummage)
            .flatMap { readingLeaves(it.effect, it.targetRequirements) }
        val refunds = roots.flatMapTo(HashSet<Effect>()) { handRefundDraws(it) + refundSearches(it) }
        for (effect in leaves) {
            tags += if (rummageLeaves.any { it === effect }) asRummage(tagsOf(effect)) else refundedTagsOf(effect, refunds)
            reachOf(effect)?.let { removalReach = maxOf(removalReach ?: 0, it) }
            drawsOf(effect)?.let { cardsDrawn = maxOf(cardsDrawn ?: 0, it) }
            if (isExpiringPump(effect)) {
                expiringPump = true
                pumpToughness = maxOf(pumpToughness, expiringToughnessOf(effect))
            }
            if (isExpiringGroupPump(effect)) expiringGroupPump = true
            if (isExpiringCopy(effect)) expiringPump = true
        }
        // A replacing base P/T until end of turn is a body for this turn's fight, like a copy —
        // unless it is the setup for removal (a fight beside it), whose timing the removal owns.
        val setToughness = if (IntentTag.REMOVAL in tags) null else leaves.mapNotNull(::expiringBaseToughnessOf).maxOrNull()
        if (IntentTag.REMOVAL !in tags && leaves.any(::isExpiringBaseStats)) expiringPump = true

        var anthemBonus = 0
        for (script in scripts) for (static in script.staticAbilities) {
            // A curse Aura's granted ability is the opponent's consolation, not our payoff.
            if (static is GrantActivatedAbility && TargetPolarityAnalyzer.cursesHost(script.staticAbilities)) continue
            tags += tagsOf(static)
            anthemBonus = maxOf(anthemBonus, anthemBonusOf(static))
        }

        if (scripts.any { script -> script.replacementEffects.any(DamageAmplifiers::reads) }) {
            tags += IntentTag.DAMAGE_AMPLIFIER
        }
        // A standing damage-prevention shield over us and our permanents (Protection of the Hekma,
        // Gideon's Intervention) protects the way a protective static does ([DamageShields]).
        if (scripts.any { script -> script.replacementEffects.any(DamageShields::reads) }) {
            tags += IntentTag.PROTECTION
        }
        // "If you would lose the game, instead …" / "if your life total would be reduced to 0 or
        // less, instead …" (Exquisite Archangel, Enduring Angel): a second life for us ([LossShields]).
        if (scripts.any { script -> script.replacementEffects.any(LossShields::reads) }) {
            tags += IntentTag.PROTECTION
        }
        // "If an effect causes you to discard a card, you may put it on top of your library instead"
        // (Library of Leng, Nephalia Academy): the discarded card is our next draw ([DiscardShields]).
        if (scripts.any { script -> script.replacementEffects.any(DiscardShields::reads) }) {
            tags += IntentTag.DISCARD_SHIELD
        }

        val activated = scripts.flatMap { it.activatedAbilities }
        if (activated.any { it.isManaAbility }) tags += IntentTag.RAMP
        if (activated.any { !it.isManaAbility && sacrificesOthers(it.cost) }) {
            tags += IntentTag.SACRIFICE_OUTLET
        }
        // An Aura/Equipment whose whole point is the creature it sits on is a pump, not an anthem —
        // when it raises it: Dead Weight's -2/-2 is removal ([tagsOf]).
        if (scripts.any { it.isAura && it.staticAbilities.any { static -> static is ModifyStats && (static.powerBonus > 0 || static.toughnessBonus > 0) } }) {
            tags += IntentTag.PUMP
        }
        // "Enchanted permanent doesn't untap during its controller's untap step" (Ice Over, Dehydration):
        // the standing form of the one-shot lock [GrantKeywordEffect] reads — the tapper's payoff on
        // what the Aura enchants. On an Equipment (Vulshok Gauntlets) or the creature itself it is a price.
        if (scripts.any { it.isAura && it.staticAbilities.any(::locksEnchantedTapped) }) tags += IntentTag.TAPPER

        // A combat trick is an instant pump that *expires*. A "+1/+1 counter at instant speed" is
        // a permanent investment you can make whenever you like, and holding it for combat is not
        // the same decision — so it stays PUMP and never reaches [HoldPolicy]'s no-window verdict.
        // A protective keyword that wears off at cleanup (Withstand Death's indestructible) is the
        // same trick without the stats: spent on a fight or on removal on the stack, or wasted.
        val speed = speedOf(card, scripts, tags)
        if (speed == Speed.INSTANT && expiringPump) tags += IntentTag.COMBAT_TRICK

        val repeatable = repeatableOf(card, scripts)
        val arrivalTags = arrivalTagsOf(card, scripts, tags)
        val intent = CardIntent(
            tags = tags,
            speed = speed,
            removalReach = removalReach,
            cardsDrawn = cardsDrawn,
            affectsOpponent = tags.any { it in OPPONENT_FACING },
            repeatable = repeatable,
            staticPriorValue = CardIntent.UNKNOWN.staticPriorValue,
            anthemBonus = anthemBonus,
            pumpToughness = pumpToughness,
            setToughness = setToughness,
            entersTapped = scripts.any(::alwaysEntersTapped),
            flashPermanent = card.typeLine.isPermanent && Keyword.FLASH in card.keywords,
            hasHaste = Keyword.HASTE in card.keywords,
            targetsOnlyOurPermanents = targetsOnlyOurPermanents(scripts),
            protectionOnlyFromBlink = (roots.any { EffectWalker.blinkMoves(it).isNotEmpty() } ||
                leaves.any { it === DELAYED_BLINK || it === OWN_STASH }) &&
                leaves.none { it !== DELAYED_BLINK && it !== OWN_STASH && IntentTag.PROTECTION in tagsOf(it) } &&
                scripts.flatMap { it.staticAbilities }.none { IntentTag.PROTECTION in tagsOf(it) },
            combatEnhancementOnlyFromUntap = leaves.any(::untapsOurCreatures) &&
                leaves.none { !untapsOurCreatures(it) && IntentTag.COMBAT_ENHANCEMENT in tagsOf(it) } &&
                scripts.flatMap { it.staticAbilities }.none { IntentTag.COMBAT_ENHANCEMENT in tagsOf(it) },
            expiringGroupPump = expiringGroupPump,
            selectsIntoHand = (gathersLibraryTop || wishFilters.isNotEmpty()) && movesToHand,
            wishFilters = if (movesToHand) wishFilters else emptyList(),
            arrivalTags = arrivalTags.takeIf { it != tags },
            grantsInstantSpeedLoyalty = leaves.any { it is GrantLoyaltyActivationPermissionEffect && it.instantSpeed },
            counterTax = counterTaxOf(leaves),
            cashableTags = activated.filterNot { it.isManaAbility }.flatMapTo(HashSet()) {
                val cashed = effectTags(it.effect, it.targetRequirements, ownPile, pileCast)
                if (isRummage(it)) asRummage(cashed) else cashed
            } +
                // A Saga chapter still to come is a one-shot payoff waiting on the board too — paid
                // out by the next lore counter rather than an activation. [analyzeSagaInPlay] has
                // already dropped the chapters that have triggered.
                scripts.flatMap { it.sagaChapters }.flatMapTo(HashSet()) {
                    effectTags(it.effect, EffectWalker.chapterTargets(it), ownPile)
                },
        )
        return intent.copy(staticPriorValue = priorValueOf(card, intent, scripts))
    }

    /**
     * [CardIntent.counterTax]: the generic mana every counter among [leaves] asks for, when each one
     * is a fixed "unless its controller pays {N}". Null when there is no counter, or when any of
     * them is a hard counter or a dynamic tax — a card this cannot read whole is not a tax counter.
     */
    private fun counterTaxOf(leaves: List<Effect>): Int? {
        val counters = leaves.filterIsInstance<CounterEffect>()
        if (counters.isEmpty()) return null
        val taxes = counters.map { (it.condition as? CounterCondition.UnlessPaysMana)?.cost?.cmc ?: return null }
        return taxes.max()
    }

    /**
     * [CardIntent.arrivalTags]: [tags] less whatever only a later event of the permanent's own can
     * cash in.
     *
     * Built by subtraction rather than by re-deriving the set: the tags the deferred slots add are
     * taken away only when no slot that can fire this turn adds them too, so a card that draws on
     * entering *and* on attacking keeps [IntentTag.DRAW]. Everything that is not a deferred slot —
     * static abilities, ETBs, "whenever you cast" triggers, activations with no tap symbol — is
     * left exactly as [tags] read it.
     */
    private fun arrivalTagsOf(card: CardDefinition, scripts: List<CardScript>, tags: Set<IntentTag>): Set<IntentTag> {
        if (!card.typeLine.isPermanent) return tags
        // A creature without haste cannot pay a tap or untap cost on the turn it arrives (CR 302.6).
        val sick = card.typeLine.isCreature && Keyword.HASTE !in card.keywords
        val deferred = mutableListOf<Pair<Effect, List<TargetRequirement>>>()
        val immediate = mutableListOf<Pair<Effect, List<TargetRequirement>>>()
        for (script in scripts) {
            script.spellEffect?.let { immediate += it to script.targetRequirements }
            immediate += script.stateTriggeredAbilities.map { it.effect to emptyList() }
            for (trigger in script.triggeredAbilities) {
                val slot = trigger.effect to trigger.allTargetRequirements
                if (waitsForALaterEventOfItsOwn(trigger)) deferred += slot else immediate += slot
            }
            for (ability in script.activatedAbilities.filterNot { it.isManaAbility }) {
                val slot = ability.effect to ability.targetRequirements
                if (sick && tapsOrUntapsItself(ability.cost)) deferred += slot else immediate += slot
            }
            // Chapter I triggers as the Saga enters; every later chapter waits for a later turn's
            // lore counter (CR 714.3b).
            for (chapter in script.sagaChapters) {
                val slot = chapter.effect to EffectWalker.chapterTargets(chapter)
                if (chapter.chapter > 1) deferred += slot else immediate += slot
            }
        }
        // A mana ability is [IntentTag.RAMP] — and a summoning-sick dork's taps for nothing yet.
        val manaAbilities = scripts.flatMap { it.activatedAbilities }.filter { it.isManaAbility }
        val sickRamp = sick && manaAbilities.isNotEmpty() && manaAbilities.all { tapsOrUntapsItself(it.cost) }
        if (deferred.isEmpty() && !sickRamp) return tags
        val ownPile = stashesOwnCards(scripts)
        val pileCast = pileCastTags(scripts)
        fun tagsOfRoots(roots: List<Pair<Effect, List<TargetRequirement>>>) =
            roots.flatMapTo(HashSet()) { (effect, requirements) -> effectTags(effect, requirements, ownPile, pileCast) }
        val later = tagsOfRoots(deferred) + if (sickRamp) setOf(IntentTag.RAMP) else emptySet()
        val onlyLater = later - tagsOfRoots(immediate) -
            scripts.flatMap { it.staticAbilities }.flatMapTo(HashSet()) { tagsOf(it) }
        return tags - onlyLater
    }

    /**
     * A trigger bound to the permanent itself whose event is the permanent fighting or leaving —
     * attacking, blocking, being blocked or unblocked, dealing damage, or leaving the battlefield.
     * None of those can happen to a no-haste body before the opponent's turn, and the ones that
     * can (blocking, dying in a block) happen *in* the ambush rather than before it.
     */
    private fun waitsForALaterEventOfItsOwn(trigger: TriggeredAbility): Boolean {
        if (trigger.binding != TriggerBinding.SELF) return false
        return when (val pattern = trigger.trigger) {
            is EventPattern.AttackEvent, is EventPattern.BlockEvent, is EventPattern.BecomesBlockedEvent,
            EventPattern.BecomesUnblockedEvent, is EventPattern.BlocksOrBecomesBlockedByEvent,
            is EventPattern.DealsDamageEvent -> true
            is EventPattern.ZoneChangeEvent -> pattern.from == Zone.BATTLEFIELD
            else -> false
        }
    }

    /** A trigger its holder fires by dealing combat damage itself ("whenever this creature deals combat damage to a player"). */
    private fun dealsCombatDamage(trigger: TriggeredAbility): Boolean =
        trigger.binding == TriggerBinding.SELF &&
            (trigger.trigger as? EventPattern.DealsDamageEvent)?.damageType == com.wingedsheep.sdk.scripting.events.DamageType.Combat

    private fun tapsOrUntapsItself(cost: AbilityCost): Boolean = when (cost) {
        AbilityCost.Tap, AbilityCost.Untap -> true
        is AbilityCost.Composite -> cost.costs.any { tapsOrUntapsItself(it) }
        else -> false
    }

    /**
     * The leaves this analyzer reads: [EffectWalker.payoffLeaves] — every "if you do" gate opened
     * onto its payoff — with a reflexive trigger ("you may sacrifice a creature. When you do, …")
     * read as the effect it fires. The action before "when you do" is the price, like a gate's.
     *
     * The reflexive payoff is read here and not in [EffectWalker.payoffLeaves] because its targets
     * are chosen when it triggers, against its own requirements: a polarity reader matching its
     * `ContextTarget(0)` against the outer ability's slots would read the wrong target.
     *
     * A delayed trigger is read the same way, as the effect it fires ([EffectWalker.firedLeaves]):
     * "draw a card at the beginning of the next upkeep" is a draw and "destroy that creature at end
     * of combat" is removal, only later. Its price — "sacrifice it at the beginning of the next end
     * step" — is left out, as a gate's cost is.
     */
    private fun readingLeaves(effect: Effect, requirements: List<TargetRequirement> = emptyList()): List<Effect> {
        val leaves = EffectWalker.payoffLeaves(effect)
        // "Exile target creature you control. Return it at the beginning of the next end step"
        // (Liberate, Niko): a blink of ours too, only slower — [DELAYED_BLINK], not removal. Aimed at
        // anyone's creature (Galepowder Mage, Otherworldly Journey, Long Road Home) it still takes a
        // blocker away for the turn — [TEMPORARY_EXILE], removal that comes back, never the exile
        // for good [IntentTag.EXILE_REMOVAL] claims — and, where our own creatures can fill the slot,
        // it is the same slow blink of ours: saving one is what the card is mostly cast for.
        val (ownPairs, otherPairs) = EffectWalker.delayedBlinks(leaves).chunked(2)
            .partition { (exile, _) -> TargetPolarityAnalyzer.targetsOwnSide((exile as MoveToZoneEffect).target, requirements) }
        val tempoPairs = otherPairs.filter { (exile, _) -> slotOf((exile as MoveToZoneEffect).target, requirements) != null }
        val delayed = ownPairs.flatten()
        val tempo = tempoPairs.flatten()
        val blinksOurs = delayed.isNotEmpty() || tempoPairs.any { (exile, _) ->
            TargetPolarityAnalyzer.ownSideCanFill((exile as MoveToZoneEffect).target, requirements)
        }
        val blinks = EffectWalker.blinkMoves(effect) + delayed + tempo
        val payoff = leaves.filterNot { leaf -> blinks.any { it === leaf } }
        // "Exile up to five target creature cards from graveyards … put the pile of your choice onto
        // the battlefield under your control" (Boneyard Parley): graveyard cards brought back onto
        // our side, a reanimation whoever owned them — read as the single-card return it adds up to.
        val reanimates = reanimatesChosenGraveyardTargets(effect, requirements)
        val stored by lazy { storedAmounts(effect) }
        return (if (blinksOurs) listOf(DELAYED_BLINK) else emptyList()) +
            (if (tempo.isEmpty()) emptyList() else listOf(TEMPORARY_EXILE)) +
            (if (reanimates) listOf(REANIMATE_FROM_A_GRAVEYARD) else emptyList()) +
            // Hour of Eternity's exile-and-copy pipeline: the single-card exile to use, below.
            (if (exilesChosenGraveyardTargetsToUse(effect, requirements)) listOf(RECAST_FROM_OUR_GRAVEYARD) else emptyList()) +
            // Wildest Dreams' "return X target cards from your graveyard to your hand" pipeline.
            (if (returnsChosenGraveyardTargetsToHand(effect, requirements)) listOf(RECAST_FROM_OUR_GRAVEYARD) else emptyList()) +
            // Harness the Storm's "you may cast target card … from your graveyard (you still pay its costs)".
            (if (castsChosenGraveyardTargetsPaying(effect, requirements)) listOf(RECAST_FROM_OUR_GRAVEYARD) else emptyList()) +
            payoff.flatMap { leaf ->
            when (leaf) {
                is ReflexiveTriggerEffect -> readingLeaves(leaf.reflexiveEffect, leaf.reflexiveTargetRequirements)
                is CreateDelayedTriggerEffect -> EffectWalker.firedLeaves(leaf, payoff).flatMap { readingLeaves(it) }
                // Damage to a slot only a player fills ("target player", "target opponent": Lava
                // Spike, Risk Factor) is a clock on that player, not an answer to a permanent.
                is DealDamageEffect -> when {
                    TargetPolarityAnalyzer.targetsOwnSide(leaf.target, requirements) -> emptyList()
                    slotOf(leaf.target, requirements) is TargetOpponent ->
                        listOf(leaf.copy(target = EffectTarget.PlayerRef(Player.TargetOpponent)))
                    slotOf(leaf.target, requirements) is TargetPlayer ->
                        listOf(leaf.copy(target = EffectTarget.PlayerRef(Player.TargetPlayer)))
                    else -> listOf(leaf)
                }
                // "Put two -1/-1 counters on target creature you control" (Defiant Greatmaw, Exemplar
                // of Strength): a drawback paid on our own board, like damage to our own creature —
                // no answer to anything. Aimed anywhere else it is the removal [tagsOf] reads.
                is AddCountersEffect -> if (harmsOwnTarget(leaf.counterType, leaf.target, requirements)) emptyList()
                    else listOf(leaf)
                is AddDynamicCountersEffect -> if (harmsOwnTarget(leaf.counterType, leaf.target, requirements)) emptyList()
                    else listOf(leaf)
                // "Create a Cursed Role token attached to target creature you control" (Spiteful
                // Hexmage): the same price paid on our own board. On anyone's creature it is the
                // answer [auraTokenTags] reads.
                is CreateAuraTokenEffect -> if (auraTokenHarmsOwnTarget(leaf, requirements)) emptyList()
                    else listOf(leaf)
                // Pulling our own attacker out of combat (Reconnaissance) saves it — no answer to
                // anything; [tagsOf] reads the move off any other creature as one.
                is RemoveFromCombatEffect -> if (TargetPolarityAnalyzer.targetsOwnSide(leaf.target, requirements)) emptyList()
                    else listOf(leaf)
                // "After that turn, that player takes an extra turn" aimed at a target opponent
                // (Emrakul, the Promised End) is the price of the hijack, not a turn of ours.
                is TakeExtraTurnEffect -> if (namesAnOpponentSlot(leaf.target, requirements)) emptyList()
                    else listOf(leaf)
                is MoveToZoneEffect -> when (graveyardMoveOf(leaf, requirements)) {
                    // Exiled from any graveyard to be used — a token copy (The Scarab God, Abyssal
                    // Harvester), a copy of it (Dimir Doppelganger), a cast (Reenact the Crime): the
                    // card is ours to use again, a recursion whoever owned it.
                    GraveyardMove.HATE -> if (usesTheExiledCard(payoff)) listOf(RECAST_FROM_OUR_GRAVEYARD)
                        else listOf(GRAVEYARD_HATE_LEAF)
                    // Exiled from our own graveyard to be cast from exile (Daring Waverider, The
                    // Dawning Archaic, Seifer Almasy): a spell of ours again — recursion. Otherwise
                    // it is the price or the fuel of what follows (Patchwork Crawler's abilities,
                    // Trove Warden's stored pile), not an answer to anything.
                    GraveyardMove.OWN_EXILE ->
                        if (usesTheExiledCard(payoff)) listOf(RECAST_FROM_OUR_GRAVEYARD) else emptyList()
                    // "{2}, {T}: Exile target creature you control" linked to this permanent
                    // (Endless Sands, Safe Haven): a stash of our own creature to bring back later,
                    // not removal — [OWN_STASH].
                    null -> when {
                        isOwnStash(leaf, requirements) -> listOf(OWN_STASH)
                        // "Exile target creature or planeswalker you control. Reveal cards until …"
                        // (Identity Echo): a permanent of ours taken off the battlefield is the price
                        // or fuel of what follows, never an answer to the opponent's board.
                        // Returned straight away ("… then return it to the battlefield under your
                        // control", Cloudshift, Thassa) it is a blink of ours: it saves the creature.
                        removesOurOwnPermanent(leaf, requirements) ->
                            if (returnedStraightAway(effect, leaf)) listOf(DELAYED_BLINK) else emptyList()
                        else -> listOf(withSourceZone(leaf, requirements))
                    }
                }
                // "Untap target land" / "untap two target lands" (Arbor Elf, Hope Tender): see [untapsATargetLand].
                // Only when the untap is the ability's whole payoff: beside another step on the same
                // land (Wakeroot Elemental's "untap target land you control. It becomes a 5/5
                // Elemental"), the untap sets that step up, and reading it as ramp would hide it.
                // "End the turn" beside another payoff — Hurkyl's Final Meditation's mass bounce,
                // Ultima's wrath — is how that payoff is delivered, not what the card is for: read the
                // card by the payoff, so it keeps its own window. Alone, or beside only the delayed
                // trigger it schedules (Glorious End), it is the card — see [tagsOf].
                EndTheTurnEffect -> if (payoff.any { it != EndTheTurnEffect && it !is CreateDelayedTriggerEffect }) emptyList() else listOf(leaf)
                is TapUntapEffect -> if (payoff.size == 1 && untapsATargetLand(leaf, requirements)) listOf(UNTAP_OUR_LANDS) else listOf(leaf)
                // "Untap up to two target creatures and up to two target lands" (Nissa, Genesis Mage):
                // the pipeline gathers every chosen target and untaps the lot. With a land slot among
                // them it is "untap target land" again — the lands tap for mana once more — and the
                // creature slots ride along; the untap is still the ability's whole payoff.
                is TapUntapCollectionEffect ->
                    if (!leaf.tap && leaf.collectionName in TargetPolarityAnalyzer.allChosenTargets(effect) &&
                        payoff.count { it !is GatherCardsEffect } == 1 && requirements.any(::isUntappableLandSlot)
                    ) listOf(UNTAP_OUR_LANDS) else listOf(leaf)
                is ForEachEffect -> when {
                    payoff.size == 1 && leaf.space == IterationSpace.Targets &&
                        EffectWalker.payoffLeaves(leaf.body).let { body ->
                            body.size == 1 && body.all { it is TapUntapEffect && untapsATargetLand(it, requirements, eachTarget = true) }
                        } -> listOf(UNTAP_OUR_LANDS)
                    // "Return two target Pirate cards from your graveyard to your hand" (March of the
                    // Drowned) runs its body once per target, each seeing it as `ContextTarget(0)`.
                    // With one declared slot that is every pick of that slot, so the body reads
                    // against it — a return out of our graveyard is the recursion it is, not the
                    // bounce an unplaced move to hand reads as. (TargetPolarityAnalyzer does the same.)
                    leaf.space == IterationSpace.Targets && requirements.size == 1 &&
                        EffectWalker.payoffLeaves(leaf.body).all { it is MoveToZoneEffect } ->
                        readingLeaves(leaf.body, requirements)
                    // "One or two target creatures each get +2/+1 until end of turn" (Appeal to
                    // Eirdu, Coordinated Assault's "+1/+0 and gain first strike") runs its pump once
                    // per target, each seeing it as `ContextTarget(0)`: every target gets the same
                    // stats and keywords, so it reads as the pump it is — an expiring one a combat
                    // trick, like the single-target form. "Untap up to two target creatures. They
                    // each get +2/+2" (Synchronized Strike) readies each pumped creature to block or
                    // attack again: a rider on the same trick, not a second payoff.
                    leaf.space == IterationSpace.Targets &&
                        EffectWalker.payoffLeaves(leaf.body).let { body ->
                            body.any { it is ModifyStatsEffect } && body.all {
                                (it is ModifyStatsEffect && it.target == EffectTarget.ContextTarget(0)) ||
                                    (it is GrantKeywordEffect && it.target == EffectTarget.ContextTarget(0)) ||
                                    (it is TapUntapEffect && !it.tap && it.target == EffectTarget.ContextTarget(0))
                            }
                        } -> readingLeaves(leaf.body, requirements)
                    else -> listOf(leaf)
                }
                is GatherCardsEffect -> {
                    val source = leaf.source as? CardSource.TopOfLibrary
                    val slot = when (val player = source?.player) {
                        is Player.BoundVariable -> requirements.firstOrNull { it.id == player.name }
                        is Player.ContextPlayer -> requirements.getOrNull(player.index)
                        Player.TargetPlayer -> requirements.firstOrNull { it is TargetPlayer }
                        else -> null
                    }
                    // A restriction can confine an otherwise generic player target to our own
                    // side. Do not claim an offensive clock without understanding that condition.
                    if (source?.isMill == true && slot is TargetPlayer && slot.restriction != null) {
                        listOf(leaf.copy(source = source.copy(player = Player.You)))
                    } else listOf(leaf)
                }
                // "Destroy another target creature. If a creature is destroyed this way, …" (Noxious
                // Gearhulk) moves the gathered chosen target itself, to track what left: read as the
                // single-target destroy or exile it amounts to, with its own-side checks above.
                is MoveCollectionEffect -> {
                    val slot = TargetPolarityAnalyzer.permanentSlots(requirements).singleOrNull()
                    val zone = (leaf.destination as? CardDestination.ToZone)?.zone
                    if (slot != null && zone != null && TargetPolarityAnalyzer.removesChosenTargets(leaf, effect)) {
                        readingLeaves(
                            MoveToZoneEffect(
                                target = EffectTarget.ContextTarget(slot),
                                destination = zone,
                                byDestruction = leaf.moveType == MoveType.Destroy,
                                fromZone = Zone.BATTLEFIELD,
                            ),
                            requirements,
                        )
                    } else listOf(leaf)
                }
                // "You may pay any amount of {E}. The creature gets -1/-1 until end of turn for each
                // {E} paid this way" (Die Young): the stats read through the number the pipeline
                // stored, so the negated count is the shrink it is — see [storedAmounts].
                is ModifyStatsEffect -> {
                    val power = resolveStored(leaf.powerModifier, stored)
                    val toughness = resolveStored(leaf.toughnessModifier, stored)
                    if (power === leaf.powerModifier && toughness === leaf.toughnessModifier) listOf(leaf)
                    else listOf(leaf.copy(powerModifier = power, toughnessModifier = toughness))
                }
                is GatedEffect -> punisherBranchLeaves(leaf, requirements) ?: listOf(leaf)
                is ChooseActionEffect -> punisherChoiceLeaves(leaf, requirements) ?: listOf(leaf)
                is AnyPlayerMayPayEffect -> opponentOfferLeaves(leaf, requirements) ?: listOf(leaf)
                else -> listOf(leaf)
            }
        }
    }

    /**
     * The branches of an offer only our opponents answer — "any opponent may have you put that card
     * into your graveyard. If a player does, this deals damage to that player … Otherwise, put that
     * card into your hand" (Sin Prodder), "any opponent may sacrifice a creature. If a player does,
     * tap this creature, you gain 3 life, and you draw a card" (Clackbridge Troll). Like
     * [punisherBranchLeaves], whichever branch the opponents pick is ours, so the card is for what
     * either does. The player who accepted ([AnyPlayerMayPayEffect.storePayerAs]) is an opponent, so a
     * step aimed at them is read as one aimed at an opponent. Tapping the source itself is the
     * price of the deal (the Troll and Desecration Demon tap), not a tapper, and is dropped.
     *
     * The branches are returned whether or not each reads alone — unlike [punisherBranchLeaves],
     * which wants every branch to pay off: an offer only opponents answer costs us nothing either
     * way, so an offer with one payoff branch (Clackbridge Troll's "if a player does, … draw a card")
     * is for that payoff, and a branch that moves the gathered cards into our hand reads as card draw
     * only beside the gather ([digTags]), never on its own. Null — the offer
     * stays an unread leaf — for an offer our controller may answer too (Aether Rift's "unless any
     * player pays", Prowling Pangolin's), and when no branch holds a step.
     */
    private fun opponentOfferLeaves(offer: AnyPlayerMayPayEffect, requirements: List<TargetRequirement>): List<Effect>? {
        if (offer.eligiblePlayers != Player.EachOpponent) return null
        val payer = offer.storePayerAs?.let { EffectTarget.PipelineTarget(it) }
        val branches = listOfNotNull(offer.consequence, offer.consequenceIfNonePaid).map { branch ->
            readingLeaves(branch, requirements).map { leaf ->
                when {
                    leaf is TapUntapEffect && leaf.tap && leaf.target == EffectTarget.Self -> null
                    payer == null -> leaf
                    leaf is DealDamageEffect && leaf.target == payer -> leaf.copy(target = EffectTarget.PlayerRef(Player.EachOpponent))
                    leaf is LoseLifeEffect && leaf.target == payer -> leaf.copy(target = EffectTarget.PlayerRef(Player.EachOpponent))
                    else -> leaf
                }
            }.filterNotNull()
        }
        return branches.flatten().takeIf { it.isNotEmpty() }
    }

    /**
     * The branches of a punisher written as a choice another player makes — "that source's
     * controller loses that much life unless they sacrifice that many permanents" (Archfiend of
     * Spite), as a [ChooseActionEffect] the damage source's controller answers. Like
     * [punisherBranchLeaves], the card does whichever branch the decider picks, so it is for what
     * either branch does; the decider takes the branch worse for us, which is the value readers'
     * question. Life the decider loses is the clock it is, so a [LoseLifeEffect] aimed at the
     * deciding player is read as one aimed at an opponent — for a choice only another player makes,
     * the loser is that player, not our controller.
     *
     * Null — the choice stays an unread leaf — for a choice our controller makes (its branches are
     * modes, opened by [EffectWalker.payoffLeaves]) and when any branch reads as nothing for us: a
     * tax the decider can always buy their way out of.
     */
    private fun punisherChoiceLeaves(choice: ChooseActionEffect, requirements: List<TargetRequirement>): List<Effect>? {
        if (EffectWalker.choosesOwnBranch(choice)) return null
        val branches = choice.choices.map { option ->
            readingLeaves(option.effect, requirements).map { leaf ->
                if (leaf is LoseLifeEffect && leaf.target == choice.player)
                    leaf.copy(target = EffectTarget.PlayerRef(Player.EachOpponent))
                else leaf
            }
        }
        return if (branches.size >= 2 && branches.all { leaves -> leaves.any { tagsOf(it).isNotEmpty() } }) branches.flatten() else null
    }

    /**
     * The branches of a punisher — a gate another player decides between two payoffs of ours:
     * "target opponent may have you draw three cards. If the player doesn't, you mill three cards,
     * then this creature deals damage to that player …" (Combustible Gearhulk), Risk Factor's four
     * damage or three cards, Clash of Elements' top or bottom of the library. The card does one
     * branch, the decider's pick, so it is for whatever either branch does — as a modal spell is for
     * any of its modes; how much that is worth (the decider takes the branch worse for us) is the
     * value readers' question. Read against [requirements], so damage to the targeted opponent is
     * the clock it is, not removal.
     *
     * Null — the gate stays an unread leaf — when either branch does nothing for us: that is a tax
     * ("… unless its controller pays {1}", Crystal Shard), and the decider can always buy their way
     * out of the payoff. A branch that wins us the game never happens — no decider ends the game
     * against themselves — so the card does the other one.
     *
     * Every decided gate with an `otherwise` among the cards is decided by another player; a
     * future one its controller decides would be a modal choice, read the same way here but
     * priced at its best branch, not its weakest, by the rater.
     */
    private fun punisherBranchLeaves(gate: GatedEffect, requirements: List<TargetRequirement>): List<Effect>? {
        val otherwise = gate.otherwise ?: return null
        if (gate.decisionMaker == null) return null
        val branches = listOf(gate.then, otherwise).map { readingLeaves(it, requirements) }
            .filterNot { leaves -> leaves.any { IntentTag.GAME_WIN in tagsOf(it) } }
        return when {
            branches.size < 2 -> branches.flatten()
            branches.all { leaves -> leaves.any { tagsOf(it).isNotEmpty() } } -> branches.flatten()
            else -> null
        }
    }

    /**
     * Whether [untap] untaps a target land: its slot's requirement is a land on the battlefield
     * that any player — or we — control. The slot is ours to aim, and the land we aim it at is our
     * own: it taps for mana again, which is the payoff of "untap all lands you control" one land at
     * a time, so [readingLeaves] reads it as [UNTAP_OUR_LANDS]. A land an opponent controls, or a
     * slot that can hold a creature too, stays unread: that untap is a tempo or combat play.
     *
     * Under a for-each over the targets ([eachTarget]), `ContextTarget(0)` is bound to each chosen
     * target in turn, so every declared slot has to be a land.
     */
    private fun untapsATargetLand(untap: TapUntapEffect, requirements: List<TargetRequirement>, eachTarget: Boolean = false): Boolean {
        if (untap.tap) return false
        val slots = when (val target = untap.target) {
            is EffectTarget.ContextTarget -> if (eachTarget) requirements else listOfNotNull(requirements.getOrNull(target.index))
            // A count-N requirement's handles are `id[0]`, `id[1]`, …: each is a pick of that slot.
            is EffectTarget.BoundVariable -> listOfNotNull(requirements.firstOrNull { it.id == target.name.substringBefore('[') })
            else -> emptyList()
        }
        return slots.isNotEmpty() && slots.all(::isUntappableLandSlot)
    }

    /** A slot [untapsATargetLand] reads: a land on the battlefield that any player — or we — control. */
    private fun isUntappableLandSlot(requirement: TargetRequirement): Boolean {
        val filter = (((requirement as? TargetOther)?.baseRequirement ?: requirement) as? TargetObject)?.filter
            ?: return false
        val base = filter.baseFilter
        return filter.zone == Zone.BATTLEFIELD && filter.alternatives.isEmpty() && base.anyOf.isEmpty() &&
            (CardPredicate.IsLand in base.cardPredicates || CardPredicate.IsBasicLand in base.cardPredicates) &&
            (base.controllerPredicate == null || base.controllerPredicate == ControllerPredicate.ControlledByYou)
    }

    /** What a single-card move out of a graveyard is — see [graveyardMoveOf]. */
    enum class GraveyardMove {
        /** A card of a graveyard that may be an opponent's, exiled or put into its library. */
        HATE,

        /** A card only our own graveyard can supply, exiled. */
        OWN_EXILE,
    }

    /**
     * What [move] is when it takes a card out of a graveyard into exile or a library, or null when
     * it does neither (a return to hand or battlefield is recursion, read by [withSourceZone]).
     *
     * The move rarely says where its card comes from: `Effects.Exile` leaves `fromZone` unset and
     * the target *requirement* is what puts the card in a graveyard. A slot any graveyard can fill
     * — "exile target card from a graveyard" (Scavenging Ooze), "from an opponent's graveyard"
     * (Carrion Locust) — is [GraveyardMove.HATE]: it took no permanent off the board, so it is not
     * removal. Into a library out of our own graveyard (Mortuary Mire) stays the recursion it is.
     */
    /**
     * Whether [payoff] goes on to use the card a graveyard exile took — cast it, play it, or copy
     * it — so the exile is the way in rather than the point: recursion, not hate. Casting any
     * number of them, or of their copies, under a budget (Uldaros Theorix) is the same use.
     */
    internal fun usesTheExiledCard(payoff: List<Effect>): Boolean = payoff.any {
        it is GrantFreeCastTargetFromExileEffect || it is GrantMayPlayFromExileEffect ||
            it is GrantPlayWithoutPayingCostEffect || it is CastFromCollectionWithoutPayingCostEffect ||
            it is CastAnyNumberFromCollectionWithoutPayingCostEffect ||
            it is CreateTokenCopyOfTargetEffect || it is EachPermanentBecomesCopyOfTargetEffect
    }

    /**
     * Whether [effect] puts its own chosen targets — or the cards split, picked or piled from among
     * them — onto the battlefield under our control: "exile up to five target creature cards from
     * graveyards. An opponent separates those cards into two piles. Put all cards from the pile of
     * your choice onto the battlefield under your control" (Boneyard Parley). Whatever the pile
     * steps in between (Fact or Fiction's split, a tracked exile), the chosen cards end up ours.
     */
    internal fun putsChosenTargetsOntoOurBattlefield(effect: Effect): Boolean {
        val leaves = EffectWalker.leaves(effect)
        val chosen = leaves.mapNotNullTo(HashSet()) { leaf ->
            (leaf as? GatherCardsEffect)?.takeIf { it.source == CardSource.ChosenTargets }?.storeAs
        }
        if (chosen.isEmpty()) return false
        do {
            val before = chosen.size
            for (leaf in leaves) chosen += derivedCollections(leaf, chosen)
        } while (chosen.size != before)
        return leaves.any { leaf ->
            val move = leaf as? MoveCollectionEffect ?: return@any false
            val to = move.destination as? CardDestination.ToZone ?: return@any false
            move.from in chosen && move.faceDown == null && !move.underOwnersControl &&
                to.zone == Zone.BATTLEFIELD && to.player == Player.You
        }
    }

    /**
     * [putsChosenTargetsOntoOurBattlefield] over target slots that all sit in a graveyard: a
     * reanimation onto our side. Slots on the battlefield would make the same pipeline a blink.
     */
    internal fun reanimatesChosenGraveyardTargets(effect: Effect, requirements: List<TargetRequirement>): Boolean {
        val objects = requirements.map { ((it as? TargetOther)?.baseRequirement ?: it) as? TargetObject }
        if (objects.isEmpty() || objects.any { it?.filter?.zone != Zone.GRAVEYARD }) return false
        return putsChosenTargetsOntoOurBattlefield(effect)
    }

    /**
     * Whether [effect] casts its own chosen graveyard targets on resolution, paying their costs —
     * "you may cast target card with the same name as that spell from your graveyard" (Harness the
     * Storm), "you may cast target instant or sorcery card from your graveyard" (Chandra, Flame's
     * Catalyst): the card comes back through the stack, the recursion a return to hand is
     * ([RECAST_FROM_OUR_GRAVEYARD]). The free cast of the same slot already reads as
     * [IntentTag.FREE_SPELL] — the mana it saves *and* the card — so only the paid cast is this.
     */
    internal fun castsChosenGraveyardTargetsPaying(effect: Effect, requirements: List<TargetRequirement>): Boolean =
        TargetPolarityAnalyzer.castGraveyardSlots(effect, requirements).isNotEmpty() &&
            gateInteriorLeaves(effect).any { it is CastFromCollectionWithoutPayingCostEffect && it.payManaCost }

    /**
     * Whether [effect] returns its own chosen targets, every one a graveyard card, to our hand —
     * "Return X target cards from your graveyard to your hand" (Wildest Dreams) gathers its targets
     * and moves the lot: the single-card return to hand ([IntentTag.RECURSION]) it adds up to.
     */
    internal fun returnsChosenGraveyardTargetsToHand(effect: Effect, requirements: List<TargetRequirement>): Boolean {
        val objects = requirements.map { ((it as? TargetOther)?.baseRequirement ?: it) as? TargetObject }
        if (objects.isEmpty() || objects.any { it?.filter?.zone != Zone.GRAVEYARD }) return false
        val leaves = EffectWalker.leaves(effect)
        val chosen = leaves.mapNotNullTo(HashSet()) { leaf ->
            (leaf as? GatherCardsEffect)?.takeIf { it.source == CardSource.ChosenTargets }?.storeAs
        }
        if (chosen.isEmpty()) return false
        do {
            val before = chosen.size
            for (leaf in leaves) chosen += derivedCollections(leaf, chosen)
        } while (chosen.size != before)
        return leaves.any { leaf ->
            val move = leaf as? MoveCollectionEffect ?: return@any false
            val to = move.destination as? CardDestination.ToZone ?: return@any false
            move.from in chosen && to.zone == Zone.HAND && to.player == Player.You
        }
    }

    /**
     * Whether [effect] exiles its own chosen targets out of a graveyard and goes on to use the
     * exiled cards ([usesTheExiledCard]) — "exile X target creature cards from your graveyard. For
     * each card exiled this way, create a token that's a copy of that card" (Hour of Eternity). The
     * pipeline spelling of the single-card exile-and-copy (The Scarab God): the exile is the way in,
     * so the cards are ours to use again — recursion, not hate.
     */
    internal fun exilesChosenGraveyardTargetsToUse(effect: Effect, requirements: List<TargetRequirement>): Boolean {
        val objects = requirements.map { ((it as? TargetOther)?.baseRequirement ?: it) as? TargetObject }
        if (objects.isEmpty() || objects.any { it?.filter?.zone != Zone.GRAVEYARD }) return false
        val leaves = EffectWalker.leaves(effect)
        val chosen = leaves.mapNotNullTo(HashSet()) { leaf ->
            (leaf as? GatherCardsEffect)?.takeIf { it.source == CardSource.ChosenTargets }?.storeAs
        }
        if (chosen.isEmpty()) return false
        do {
            val before = chosen.size
            for (leaf in leaves) chosen += derivedCollections(leaf, chosen)
        } while (chosen.size != before)
        val exiles = leaves.any { leaf ->
            leaf is MoveCollectionEffect && leaf.from in chosen && leaf.faceDown == null &&
                (leaf.destination as? CardDestination.ToZone)?.zone == Zone.EXILE
        }
        return exiles && usesTheExiledCard(leaves.flatMap { leaf ->
            if (leaf is ForEachEffect) EffectWalker.payoffLeaves(leaf.body) else listOf(leaf)
        })
    }

    internal fun graveyardMoveOf(move: MoveToZoneEffect, requirements: List<TargetRequirement>): GraveyardMove? {
        if (move.destination != Zone.EXILE && move.destination != Zone.LIBRARY) return null
        val slot = when (val target = move.target) {
            is EffectTarget.BoundVariable -> requirements.firstOrNull { it.id == target.name }
            is EffectTarget.ContextTarget -> requirements.getOrNull(target.index)
            else -> null
        } ?: return null
        val filter = (slot as? TargetObject)?.filter
        val fromGraveyard = move.fromZone == Zone.GRAVEYARD || (move.fromZone == null && filter?.zone == Zone.GRAVEYARD)
        if (!fromGraveyard) return null
        val own = TargetPolarityAnalyzer.ownGraveyardOnly(filter)
        return when {
            !own -> GraveyardMove.HATE
            move.destination == Zone.EXILE -> GraveyardMove.OWN_EXILE
            else -> null
        }
    }

    /**
     * [move] with the zone its target comes from filled in from that target's requirement, when the
     * move leaves it unsaid: `Effects.Move(target, Zone.BATTLEFIELD)` over "target creature card in
     * your graveyard" (Reaping Willow, a reflexive "when you do, return target creature card")
     * names no `fromZone`, and only the requirement puts the card in a graveyard. Read so, it is the
     * recursion it is; unread, it was no move [tagsOf] could call.
     */
    private fun withSourceZone(move: MoveToZoneEffect, requirements: List<TargetRequirement>): MoveToZoneEffect {
        // Only a return to hand, battlefield or library: exiling a graveyard card is graveyard hate,
        // which [tagsOf] must not read as getting the card back.
        if (move.fromZone != null || (move.destination != Zone.HAND && move.destination != Zone.BATTLEFIELD && move.destination != Zone.LIBRARY)) return move
        val slot = when (val target = move.target) {
            is EffectTarget.BoundVariable -> requirements.firstOrNull { it.id == target.name }
            is EffectTarget.ContextTarget -> requirements.getOrNull(target.index)
            else -> null
        } ?: return move
        val filter = (slot as? TargetObject)?.filter
        val zone = filter?.zone ?: return move
        // Into a library from any graveyard is graveyard hate, not getting our card back.
        if (move.destination == Zone.LIBRARY && !TargetPolarityAnalyzer.ownGraveyardOnly(filter)) return move
        return if (zone == Zone.GRAVEYARD) move.copy(fromZone = zone) else move
    }

    /**
     * [EffectWalker.leaves], plus the interiors of the gated effects it stops at — the action of an
     * "if you do" ([Gate.DoAction]) and the branches of any gate the walk does not already open.
     *
     * Extrapolate the Impossible is the shape that needs it: "you may reveal two cards from outside
     * the game; an opponent chooses one; put it into your hand" lowers to `May(IfYouDo(reveal, then
     * = put into hand))`, and [EffectWalker] treats the `IfYouDo` as a leaf, so the card read as
     * doing nothing at all. Used for [CardIntent.selectsIntoHand] / [CardIntent.wishFilters] and [wishTags],
     * and by [TargetPolarityAnalyzer.castGraveyardSlots] ("you may cast target card … If you do", Ogre
     * Battlecaster): widening the shared walk would move every tag and the rater's numbers with it,
     * which is a separate change.
     */
    internal fun gateInteriorLeaves(effect: Effect): List<Effect> =
        EffectWalker.leaves(effect).flatMap { leaf ->
            if (leaf !is GatedEffect) return@flatMap listOf(leaf)
            listOfNotNull((leaf.gate as? Gate.DoAction)?.action, leaf.then, leaf.otherwise)
                .flatMap { gateInteriorLeaves(it) }
        }

    /** Every tag [root] reads as: its leaves' own, plus what the pipeline among them adds up to ([pipelineTags]). */
    private fun effectTags(
        root: Effect,
        requirements: List<TargetRequirement> = emptyList(),
        ownPile: Boolean = false,
        pileCast: Set<IntentTag> = emptySet(),
    ): Set<IntentTag> =
        (handRefundDraws(root) + refundSearches(root)).let { refunds ->
            readingLeaves(root, requirements).flatMapTo(HashSet()) { refundedTagsOf(it, refunds) }
        } + pipelineTags(root, ownPile = ownPile, pileCast = pileCast)

    /**
     * [tagsOf], except that a draw or search in [refunds] ([handRefundDraws], [refundSearches]) is
     * not the card's own card draw or tutor.
     */
    private fun refundedTagsOf(leaf: Effect, refunds: Collection<Effect>): Set<IntentTag> = when {
        leaf is DrawCardsEffect && leaf in refunds -> tagsOf(leaf) - IntentTag.DRAW
        leaf is GatherCardsEffect && leaf in refunds -> tagsOf(leaf) - IntentTag.TUTOR
        else -> tagsOf(leaf)
    }

    /**
     * "That player shuffles, then draws a card for each card exiled from their hand this way" (Lost
     * Legacy, The Stone Brain): a draw for a player other than the controller, sized by the cards a
     * pipeline in [root] took out of a hand, gives back what the answer stripped. It is the
     * answer's price — neither card draw for us nor a loot that makes the hand gather our own.
     * "Discard any number of cards, then draw that many" draws for the controller and stays a draw.
     */
    internal fun handRefundDraws(root: Effect): List<DrawCardsEffect> {
        val leaves = readingLeaves(root)
        val fromHand = leaves.mapNotNullTo(HashSet()) { leaf ->
            val zones = when (val source = (leaf as? GatherCardsEffect)?.source) {
                is CardSource.FromZone -> listOf(source.zone)
                is CardSource.FromMultipleZones -> source.zones
                else -> return@mapNotNullTo null
            }
            leaf.storeAs.takeIf { Zone.HAND in zones }
        }
        if (fromHand.isEmpty()) return emptyList()
        do {
            val before = fromHand.size
            for (leaf in leaves) fromHand += derivedCollections(leaf, fromHand)
        } while (fromHand.size != before)
        val sizes = fromHand.mapTo(HashSet()) { "${it}_count" }
        return leaves.filterIsInstance<DrawCardsEffect>().filter { draw ->
            draw.target != EffectTarget.Controller &&
                (draw.count as? DynamicAmount.VariableReference)?.variableName in sizes
        }
    }

    /**
     * "Exile target creature you control" into a pile linked to this permanent (Endless Sands, Safe
     * Haven, Synod Sanctum): a stash of our own permanent to bring back later, not removal. Shared by
     * the intent reader and [com.wingedsheep.ai.engine.LimitedCardRater] so the two never disagree.
     */
    internal fun isOwnStash(leaf: MoveToZoneEffect, requirements: List<TargetRequirement>): Boolean =
        leaf.linkToSource && leaf.destination == Zone.EXILE &&
            TargetPolarityAnalyzer.targetsOwnSide(leaf.target, requirements)

    /**
     * An exile or destroy aimed at a slot only our own permanents can fill ("target creature you
     * control"): what it takes away is ours, so it is no removal. A bounce of our own permanent is a
     * save or a re-buy, read elsewhere, and is left alone here.
     */
    internal fun removesOurOwnPermanent(leaf: MoveToZoneEffect, requirements: List<TargetRequirement>): Boolean =
        (leaf.destination == Zone.EXILE || leaf.destination == Zone.GRAVEYARD) &&
            (leaf.fromZone == null || leaf.fromZone == Zone.BATTLEFIELD) &&
            TargetPolarityAnalyzer.targetsOwnSide(leaf.target, requirements)

    /**
     * Whether [exile] is followed, later in the same sequence of [root] and unconditionally, by a move of
     * its target back onto the battlefield — the second half of a blink. A return behind a condition
     * is not one: it may never happen.
     */
    private fun returnedStraightAway(root: Effect, exile: MoveToZoneEffect): Boolean {
        if (root !is CompositeEffect) return false
        val index = root.effects.indexOfFirst { it === exile }
        if (index >= 0) {
            // A later step, not only the next one: Don & Leo exile an artifact and a creature, then
            // return both.
            return root.effects.drop(index + 1).any { next ->
                next is MoveToZoneEffect && next.destination == Zone.BATTLEFIELD && next.target == exile.target &&
                    (next.fromZone == null || next.fromZone == Zone.EXILE)
            }
        }
        return root.effects.any { returnedStraightAway(it, exile) }
    }

    /**
     * What the gather → move pipelines in [root] add up to, which no single leaf of them says.
     *
     * Internal so [com.wingedsheep.ai.engine.LimitedCardRater] can price the same reading: its leaf
     * fold sees a gather and a move, each worth nothing alone.
     */
    internal fun pipelineTags(
        root: Effect,
        turnAhead: Boolean = false,
        ownPile: Boolean = false,
        pileCast: Set<IntentTag> = emptySet(),
    ): Set<IntentTag> = buildSet {
        addAll(digTags(root, turnAhead))
        addAll(pileFillTags(root, pileCast))
        addAll(pileReleaseTags(root, pileCast))
        addAll(destroyAllTags(root))
        addAll(handDisruptionTags(root, eachPlayer = null))
        addAll(symmetricDiscardTags(root))
        addAll(discardPunisherTags(root, eachPlayer = null))
        addAll(regrowthTags(root, ownPile))
        addAll(wishTags(root))
        addAll(graveyardExileTags(root))
        addAll(libraryExileTags(root))
        addAll(revealUntilMillTags(root))
        addAll(handToBattlefieldTags(root))
        if (selectsOwnLibraryTop(readingLeaves(root))) add(IntentTag.CARD_SELECTION)
        if (EffectWalker.blinkMoves(root).isNotEmpty()) add(IntentTag.PROTECTION)
        // "Exile up to two target creatures you control, then return those cards to the battlefield"
        // (Displace, Ghostly Flicker, Illusionist's Stratagem) as a gather → exile → return pipeline:
        // the blink [EffectWalker.blinkMoves] reads in its single-move spelling, a save and an ETB re-buy.
        if (TargetPolarityAnalyzer.blinksChosenTargets(root)) add(IntentTag.PROTECTION)
        // Exile it, its owner may cast it again (Airbend, Release to the Wind): a bounce that skips
        // the hand, so it reads as a bounce's MoveToZone does in [tagsOf] — tempo removal.
        if (TargetPolarityAnalyzer.ownerRecastsChosenTargets(root)) add(IntentTag.REMOVAL)
        // readingLeaves opens the reflexive payoff but discards its composite boundary. Keep
        // composition readings here too: Escape Protocol's exile-and-return is one blink.
        EffectWalker.payoffLeaves(root).filterIsInstance<ReflexiveTriggerEffect>().forEach {
            addAll(pipelineTags(it.reflexiveEffect))
        }
    }

    /**
     * What a static "you may cast spells from among cards exiled with this permanent"
     * ([GrantMayCastFromLinkedExile]) makes of the cards [scripts] put in that pile — the reading
     * [pileFillTags] hands an exile into the pile. Empty when no face carries the grant.
     *
     * A pile it can cast from on any later turn is [IntentTag.DRAW]: the cards wait in exile instead
     * of in hand, as a lasting impulse grant's do ([isLastingImpulse]). One it can only cast from on
     * the turn the cards arrive ([GrantMayCastFromLinkedExile.exiledThisTurnOnly]) is not — cards
     * that must be spent before cleanup are not banked. Cast without paying the mana cost, either
     * one is [IntentTag.FREE_SPELL].
     */
    fun pileCastTags(scripts: List<CardScript>): Set<IntentTag> = buildSet {
        // "During your turn, if an opponent lost life this turn, you may … cast spells from among
        // cards exiled with this" (Theater of Horrors): a condition on the grant is still the grant.
        val grants = scripts.flatMap { it.staticAbilities }
            .map { (it as? ConditionalStaticAbility)?.ability ?: it }
            .filterIsInstance<GrantMayCastFromLinkedExile>()
        for (grant in grants) {
            if (!grant.exiledThisTurnOnly) add(IntentTag.DRAW)
            if (grant.withoutPayingManaCost) add(IntentTag.FREE_SPELL)
        }
        // "You may play cards exiled with this creature" (Evendo Brushrazer): the play-permission
        // spelling of the same grant, its pile named by `exiledWithSource()` in the filter. A
        // condition on it ("during your turn, as long as …") is still the grant, as above.
        val playsThePile = scripts.flatMap { it.staticAbilities }
            .map { (it as? ConditionalStaticAbility)?.ability ?: it }
            .filterIsInstance<MayPlayCardsFromExile>()
            .any { Zone.EXILE in it.zones && namesLinkedPile(it.filter) }
        if (playsThePile) add(IntentTag.DRAW)
        if (banksOwnLibraryForHand(scripts)) add(IntentTag.DRAW)
    }

    /** Whether [filter] (or one of its `or` branches) is "cards exiled with this" ([StatePredicate.ExiledWithSource]). */
    private fun namesLinkedPile(filter: GameObjectFilter): Boolean =
        StatePredicate.ExiledWithSource in filter.statePredicates || filter.anyOf.any(::namesLinkedPile)

    /**
     * Whether [scripts] stock their linked-exile pile only off **their controller's own library
     * top** and have an ability that puts the pile into a hand — "whenever this attacks, exile the
     * top card of your library face down … sacrifice this: put all cards exiled with this into their
     * owners' hands" (Bomat Courier). The cards are ours, so "their owners' hands" is our hand: the
     * pile is a draw banked in exile, which the fill ([pileFillTags]) and the release
     * ([pileReleaseTags]) both read as [IntentTag.DRAW].
     *
     * Conservative like [stashesOwnCards]: any other way of filling the pile — a single-card or
     * spell exile, exile-until-leaves, a zone-change redirect, a top-of-library exile that can reach
     * another player's library — may hand an opponent their own card back, and answers false.
     */
    private fun banksOwnLibraryForHand(scripts: List<CardScript>): Boolean {
        var filled = false
        var released = false
        for (script in scripts) {
            if (script.replacementEffects.any {
                    (it is RedirectZoneChange && it.linkToSource) || (it is RedirectZoneChangeWith && it.linkToSource)
                }
            ) return false
            for (slot in EffectWalker.slots(script)) {
                val leaves = EffectWalker.leaves(slot.effect)
                val ownTop = collectionsFromTop(leaves) { it == Player.You }
                for (leaf in leaves) when {
                    leaf is MoveCollectionEffect && leaf.linkToSource -> if (leaf.from in ownTop) filled = true else return false
                    leaf is MoveToZoneEffect && leaf.linkToSource -> return false
                    leaf is ExileUntilLeavesEffect -> return false
                    leaf is ExileTargetSpellEffect && leaf.linkToSource -> return false
                }
                if (releasesPileToHand(leaves)) released = true
            }
        }
        return filled && released
    }

    /** Whether [leaves] gather the source's linked-exile pile and move those cards to a hand. */
    private fun releasesPileToHand(leaves: List<Effect>): Boolean {
        val pile = leaves.mapNotNullTo(HashSet()) { leaf ->
            (leaf as? GatherCardsEffect)?.takeIf { it.source is CardSource.FromLinkedExile }?.storeAs
        }
        if (pile.isEmpty()) return false
        do {
            val before = pile.size
            for (leaf in leaves) pile += derivedCollections(leaf, pile)
        } while (pile.size != before)
        return leaves.any { leaf ->
            leaf is MoveCollectionEffect && leaf.from in pile &&
                (leaf.destination as? CardDestination.ToZone)?.zone == Zone.HAND
        }
    }

    /**
     * The release half of [banksOwnLibraryForHand]: an ability putting the pile into a hand when
     * [pileCast] says the pile is a banked draw. A pile the card may cast from ([pileCastTags]'s
     * grant) is ours to cast too, so handing it over is the same draw.
     */
    private fun pileReleaseTags(root: Effect, pileCast: Set<IntentTag>): Set<IntentTag> =
        if (IntentTag.DRAW in pileCast && releasesPileToHand(readingLeaves(root))) setOf(IntentTag.DRAW) else emptySet()

    /**
     * "Exile the top two cards of target opponent's library" into a pile this permanent can cast
     * from (Maralen, Fae Ascendant; Theater of Horrors; Rona, Disciple of Gix; Jacob Hauken): a
     * gather off any player's library top, moved to exile linked to the source. The move alone is
     * plumbing; with the card's own cast-from-the-pile grant it is what [pileCast] says the pile is
     * worth ([pileCastTags]) — the impulse draw split across two abilities.
     */
    private fun pileFillTags(root: Effect, pileCast: Set<IntentTag>): Set<IntentTag> {
        if (pileCast.isEmpty()) return emptySet()
        val leaves = readingLeaves(root)
        val fromAnyTop = collectionsFromTop(leaves) { true }
        val fillsThePile = leaves.any {
            it is MoveCollectionEffect && it.linkToSource && it.from in fromAnyTop &&
                (it.destination as? CardDestination.ToZone)?.zone == Zone.EXILE
        }
        return if (fillsThePile) pileCast else emptySet()
    }

    /**
     * Whether everything [scripts] exile "with this" is their controller's own: every pile filler is
     * a single-target exile aimed only at "… you control" ("{2}, {T}: Exile target creature you
     * control" — Endless Sands, Safe Haven), or the spell's own "exile any number of … from your
     * hand / graveyard" discount cost, which only ever takes the caster's cards (Gorex, the
     * Tombshell). Such a pile holds our own cards, so returning it "under its owner's control" or to
     * "its owner's hand" brings them back to us ([regrowthTags]).
     *
     * Conservative: any other way of filling the pile — a group or pipeline exile, exile-until-leaves,
     * an exiled spell, a zone-change redirect — may reach another player's cards and answers false.
     */
    fun stashesOwnCards(scripts: List<CardScript>): Boolean {
        var filled = false
        for (script in scripts) {
            if (script.additionalCosts.any { it is AdditionalCost.AnyNumberForCostReduction && it.action == PermanentCostAction.EXILE }) {
                filled = true
            }
            if (script.replacementEffects.any {
                    (it is RedirectZoneChange && it.linkToSource) || (it is RedirectZoneChangeWith && it.linkToSource)
                }
            ) return false
            for (slot in EffectWalker.slots(script)) {
                for (leaf in EffectWalker.leaves(slot.effect)) {
                    when {
                        leaf is MoveToZoneEffect && leaf.linkToSource -> {
                            if (!TargetPolarityAnalyzer.targetsOwnSide(leaf.target, slot.targets)) return false
                            filled = true
                        }
                        leaf is MoveCollectionEffect && leaf.linkToSource -> return false
                        leaf is ExileUntilLeavesEffect -> return false
                        leaf is ExileTargetSpellEffect && leaf.linkToSource -> return false
                    }
                }
            }
        }
        return filled
    }

    /**
     * "Return all nonland permanent cards from your graveyard to the battlefield" (Return to the
     * Light Realms), "return up to two creature cards from your graveyard to your hand" — a gather
     * from **our own graveyard** whose cards, or the ones picked from among them, are moved onto the
     * battlefield, into our hand or onto our library's top — reads as [IntentTag.RECURSION], as the single-card
     * `MoveToZoneEffect` spelling of the same return does in [tagsOf].
     *
     * The source's linked-exile pile counts as that stock too, onto our battlefield only: "put a
     * creature card exiled with this permanent onto the battlefield under your control" (Tomb of the
     * Dusk Rose).
     *
     * Only our graveyard (or that pile) and only to our side: reanimating an opponent's creature is a steal, and
     * a graveyard exiled is graveyard hate, both read (or not) elsewhere.
     *
     * "Under its owner's control" hands the pile back to whoever owned each card — a release, not a
     * recursion — unless [ownPile]: the card only ever stashes its controller's own cards there
     * ([stashesOwnCards]: Endless Sands, Safe Haven, Gorex), so the cards coming back — onto the
     * battlefield or into hand — are ours.
     */
    private fun regrowthTags(root: Effect, ownPile: Boolean = false): Set<IntentTag> {
        val leaves = readingLeaves(root)
        val ourGraveyard = leaves.mapNotNullTo(HashSet()) { leaf ->
            val source = (leaf as? GatherCardsEffect)?.source as? CardSource.FromZone ?: return@mapNotNullTo null
            leaf.storeAs.takeIf { source.zone == Zone.GRAVEYARD && source.player == Player.You }
        }
        // The pile this permanent exiled is a stock to bring back too, whoever owned the cards — but
        // only onto our battlefield (Tomb of the Dusk Rose). A hand is always its owner's, so
        // "return the exiled cards to their owners' hands" (Angel of Serenity, Ashiok's Erasure) can
        // hand an opponent their card back: an exile-until's release, not a recursion.
        // A pile the same resolution fills is an exile-and-return (Ghostly Flicker, Another Round):
        // a blink of what was just exiled, not a recursion of what was stored earlier.
        val fillsThePile = leaves.any { leaf ->
            (leaf is MoveCollectionEffect && leaf.linkToSource) || (leaf is MoveToZoneEffect && leaf.linkToSource)
        }
        val linkedExile = if (fillsThePile) HashSet() else
            leaves.mapNotNullTo(HashSet()) { leaf ->
                (leaf as? GatherCardsEffect)?.takeIf { it.source is CardSource.FromLinkedExile }?.storeAs
            }
        if (ourGraveyard.isEmpty() && linkedExile.isEmpty()) return emptySet()
        fun closed(seed: MutableSet<String>): Set<String> {
            do {
                val before = seed.size
                for (leaf in leaves) seed += derivedCollections(leaf, seed)
            } while (seed.size != before)
            return seed
        }
        val fromGraveyard = closed(ourGraveyard)
        val fromLinkedExile = closed(linkedExile)
        val returnsThem = leaves.any { leaf ->
            val move = (leaf as? MoveCollectionEffect)?.takeIf { it.faceDown == null } ?: return@any false
            val to = move.destination as? CardDestination.ToZone ?: return@any false
            if (to.player != Player.You) return@any false
            when (move.from) {
                // "Put an artifact card from your graveyard on top of your library" (Sequestered
                // Stash) is a regrowth paid for with the next draw, as the single-card move is.
                in fromGraveyard -> to.zone == Zone.HAND || to.zone == Zone.BATTLEFIELD ||
                    (to.zone == Zone.LIBRARY && to.placement == ZonePlacement.Top)
                // "Under their owners' control" hands the pile back (Lagrella), unless only our
                // own cards were ever stashed in it (Endless Sands, Safe Haven). Its owner's hand is
                // ours on the same terms (Gorex, the Tombshell's own graveyard cards).
                in fromLinkedExile -> (to.zone == Zone.BATTLEFIELD && (!move.underOwnersControl || ownPile)) ||
                    (to.zone == Zone.HAND && ownPile)
                else -> false
            }
        }
        return if (returnsThem) setOf(IntentTag.RECURSION) else emptySet()
    }

    /**
     * "Choose a sorcery card you own from outside the game, reveal that card, and put it into your
     * hand" (Burning Wish), "reveal two cards you own from outside the game; an opponent chooses one
     * of them; put that card into your hand" (Extrapolate the Impossible): a gather from our own
     * sideboard ([wishFilterOf]) whose cards, or the ones picked from among them, end in our hand is
     * a search for the card we need — [IntentTag.TUTOR], from outside the game instead of the library.
     * Read over [gateInteriorLeaves]: Extrapolate's reveal is the action of an "if you do".
     */
    private fun wishTags(root: Effect): Set<IntentTag> {
        val leaves = gateInteriorLeaves(root)
        val wished = leaves.mapNotNullTo(HashSet()) { leaf ->
            (leaf as? GatherCardsEffect)?.takeIf { wishFilterOf(it) != null }?.storeAs
        }
        if (wished.isEmpty()) return emptySet()
        do {
            val before = wished.size
            for (leaf in leaves) wished += derivedCollections(leaf, wished)
        } while (wished.size != before)
        val toOurHand = leaves.any { leaf ->
            val move = leaf as? MoveCollectionEffect ?: return@any false
            val to = move.destination as? CardDestination.ToZone ?: return@any false
            move.from in wished && to.zone == Zone.HAND && to.player == Player.You
        }
        return if (toOurHand) setOf(IntentTag.TUTOR) else emptySet()
    }

    /**
     * "Put a creature card from your hand onto the battlefield" (Kinscaer Sentry, Goblin Lackey,
     * Kaalia), "you may put a land card from your hand onto the battlefield" (Arboreal Grazer,
     * Burgeoning) — a gather from **our own hand** whose cards, or the ones picked from among them,
     * move onto our battlefield. Neither leaf says it: the gather is how a discard starts too.
     *
     * A land-only gather is an extra land drop, [IntentTag.RAMP]. Anything else is a permanent that
     * skipped its mana cost — the payoff [IntentTag.FREE_SPELL] names for a spell cast for free.
     * A face-down move (manifest) is a 2/2, not the card, and stays unread.
     */
    private fun handToBattlefieldTags(root: Effect): Set<IntentTag> {
        val leaves = readingLeaves(root)
        val fromHand = HashMap<String, Boolean>()
        for (leaf in leaves) {
            val source = (leaf as? GatherCardsEffect)?.source as? CardSource.FromZone ?: continue
            if (source.zone == Zone.HAND && source.player == Player.You) fromHand[leaf.storeAs] = isLandOnly(source.filter)
        }
        if (fromHand.isEmpty()) return emptySet()
        do {
            val before = fromHand.size
            for (leaf in leaves) for (from in sourcesOf(leaf)) {
                val landOnly = fromHand[from] ?: continue
                derivedCollections(leaf, setOf(from)).forEach { fromHand.putIfAbsent(it, landOnly) }
            }
        } while (fromHand.size != before)
        return leaves.flatMapTo(HashSet()) { leaf ->
            val move = leaf as? MoveCollectionEffect ?: return@flatMapTo emptySet()
            val landOnly = fromHand[move.from] ?: return@flatMapTo emptySet()
            val to = move.destination as? CardDestination.ToZone ?: return@flatMapTo emptySet()
            if (move.faceDown != null || to.zone != Zone.BATTLEFIELD || to.player != Player.You) emptySet()
            else setOf(if (landOnly) IntentTag.RAMP else IntentTag.FREE_SPELL)
        }
    }

    private fun isLandOnly(filter: GameObjectFilter): Boolean =
        filter.anyOf.isEmpty() &&
            (CardPredicate.IsLand in filter.cardPredicates || CardPredicate.IsBasicLand in filter.cardPredicates)

    /**
     * "Look at the top four cards of your library, then put them back in any order" (Sage Owl),
     * "mill four cards, then you may put a creature or land card from among them on top of your
     * library" (Lluwen) — a gather from **our own** library top whose cards only go back to that
     * library or into our graveyard, arranged or picked on the way: scry's card flow
     * ([IntentTag.CARD_SELECTION]), spelled as a pipeline. A move to a hand, the battlefield or exile
     * is a draw, a cheat or an impulse, read elsewhere; a random bottom with no pick arranges nothing.
     *
     * Internal so the reading audit can skip such a pipeline the way it skips a scry leaf.
     */
    internal fun selectsOwnLibraryTop(leaves: List<Effect>): Boolean {
        val ours = collectionsFromTop(leaves) { it == Player.You }
        if (ours.isEmpty()) return false
        val moves = leaves.filterIsInstance<MoveCollectionEffect>().filter { it.from in ours }
        val zones = moves.map { (it.destination as? CardDestination.ToZone)?.zone }
        if (Zone.LIBRARY !in zones || zones.any { it != Zone.LIBRARY && it != Zone.GRAVEYARD }) return false
        return leaves.any { it is SelectFromCollectionEffect && it.from in ours } ||
            moves.any { it.order == CardOrder.ControllerChooses || it.order == CardOrder.OwnerChooses }
    }

    /**
     * "Search target player's library for up to five cards and exile them" (Supreme Inquisitor):
     * cards taken out of another player's library for good, by us — the exile form of milling them
     * toward an empty library, [IntentTag.MILL]. Not our tutor ([searchesOwnZones]); a search we
     * hand to its owner (Settle the Wreckage), or cards exiled to be played (a theft, see
     * [usesTheExiledCard]), are not this.
     */
    private fun libraryExileTags(root: Effect): Set<IntentTag> {
        val leaves = readingLeaves(root)
        if (usesTheExiledCard(leaves)) return emptySet()
        val seed = leaves.mapNotNullTo(HashSet()) { leaf ->
            val gather = leaf as? GatherCardsEffect ?: return@mapNotNullTo null
            val source = gather.source as? CardSource.FromZone ?: return@mapNotNullTo null
            gather.storeAs.takeIf {
                source.zone == Zone.LIBRARY && gather.searcher == Player.You && canMillOpponent(source.player)
            }
        }
        if (seed.isEmpty()) return emptySet()
        do {
            val before = seed.size
            for (leaf in leaves) seed += derivedCollections(leaf, seed)
        } while (seed.size != before)
        val exiles = leaves.any { leaf ->
            leaf is MoveCollectionEffect && leaf.from in seed &&
                (leaf.destination as? CardDestination.ToZone)?.zone == Zone.EXILE
        }
        return if (exiles) setOf(IntentTag.MILL) else emptySet()
    }

    /**
     * "Exile target player's graveyard" (Tormod's Crypt, Bojuka Bog), "exile each opponent's
     * graveyard" — a gather from a graveyard that is not only ours whose cards, or the ones picked
     * from among them, go to exile — reads as [IntentTag.GRAVEYARD_HATE], as the single-card
     * `MoveToZoneEffect` spelling does in [readingLeaves]. Our own graveyard exiled is a cost or
     * fuel (delve, escape-likes), never an answer.
     */
    private fun graveyardExileTags(root: Effect, eachPlayer: Player? = null): Set<IntentTag> {
        val leaves = readingLeaves(root)
        // Exiled to be cast from exile (Mnemonic Betrayal, Shadow of the Enemy): a theft, not hate.
        if (usesTheExiledCard(leaves)) return emptySet()
        // Inside "each player …" / "each opponent …" ([eachPlayer]), `You` is the player iterated,
        // whose graveyard is an opponent's too.
        val iteratesOpponents = eachPlayer == Player.Each || eachPlayer in OPPONENT_PLAYERS
        val seed = leaves.mapNotNullTo(HashSet()) { leaf ->
            val source = (leaf as? GatherCardsEffect)?.source as? CardSource.FromZone ?: return@mapNotNullTo null
            leaf.storeAs.takeIf { source.zone == Zone.GRAVEYARD && (source.player != Player.You || iteratesOpponents) }
        }
        if (seed.isEmpty()) return emptySet()
        do {
            val before = seed.size
            for (leaf in leaves) seed += derivedCollections(leaf, seed)
        } while (seed.size != before)
        // "Each player shuffles their graveyard into their library" (Survive) takes every
        // opponent's graveyard away as surely as an exile does. A lone
        // player's graveyard shuffled back (Elixir of Immortality, Gaea's Blessing) is a refill.
        val takenTo = if (iteratesOpponents) setOf(Zone.EXILE, Zone.LIBRARY) else setOf(Zone.EXILE)
        val exiles = leaves.any { leaf ->
            leaf is MoveCollectionEffect && leaf.from in seed &&
                (leaf.destination as? CardDestination.ToZone)?.zone in takenTo
        }
        return if (exiles) setOf(IntentTag.GRAVEYARD_HATE) else emptySet()
    }

    /**
     * "Target player discards two cards" (Mind Rot), "each opponent discards a card" (Rank Rat),
     * "target opponent reveals their hand; you choose a nonland card from it; exile that card"
     * (Null Summoner), "search target player's graveyard, hand, and library for all cards with that
     * name and exile them" (Cranial Extraction) — a gather from **another player's hand** whose cards (or the ones picked
     * from among them) are moved to a graveyard, exile or a library — reads as [IntentTag.DISCARD].
     * `Patterns.Hand` lowers every one of these to the same gather → select → move pipeline, so no
     * single leaf says it.
     *
     * Whose hand it is decides it. An opponent's ([OPPONENT_PLAYERS]) always; `Player.You` only
     * inside an iteration over opponents ([eachPlayer]), where it is the player iterated. A target
     * player slot is aimed at an opponent too — unless the same effect also draws: "target player
     * draws three cards, then discards a card" is a loot its caster points at themselves. Our own
     * hand is a cost or a loot, never an answer, so it stays unread.
     *
     * A symmetric "each player discards" is read by [symmetricDiscardTags], which sets [symmetric]
     * once it has ruled out a wheel.
     */
    private fun handDisruptionTags(root: Effect, eachPlayer: Player?, symmetric: Boolean = false): Set<IntentTag> {
        val leaves = readingLeaves(root)
        // A refund ("draws a card for each card exiled from their hand this way") is the answer's
        // price, not the loot that would make a target player's hand our own.
        val refunds = handRefundDraws(root)
        val draws = leaves.any { (it is DrawCardsEffect && it !in refunds) || it is DrawUpToEffect }
        val theirHand = handCollections(leaves, eachPlayer, draws, symmetric)
        // "Each player discards a card" written as a loop that has each player pick their card, in
        // turn order, and hands the picks out to discard all at once after it (Creeping Dread,
        // Liliana, Waker of the Dead, Kroxa's "each opponent discards a card"): the loop's hand
        // collections, read in its frame, become the aggregates it collects them into — as
        // [boardGroups] follows a collecting loop's permanents. Over every player, `you` in the
        // body is an opponent in turn only once [symmetricDiscardTags] has ruled out a wheel.
        for (loop in leaves.filterIsInstance<ForEachEffect>()) {
            val players = (loop.space as? IterationSpace.Players)?.players ?: continue
            if (loop.collectCollections.isEmpty()) continue
            val inner = handCollections(readingLeaves(loop.body), players, draws, symmetric && players in SYMMETRIC_PLAYERS)
            for ((each, all) in loop.collectCollections) if (each in inner) theirHand += all
        }
        if (theirHand.isEmpty()) return emptySet()
        do {
            val before = theirHand.size
            for (leaf in leaves) theirHand += derivedCollections(leaf, theirHand)
        } while (theirHand.size != before)
        val takesThem = leaves.any {
            it is MoveCollectionEffect && it.from in theirHand &&
                (it.destination as? CardDestination.ToZone)?.zone in HAND_DISRUPTION_DESTINATIONS
        }
        return if (takesThem) setOf(IntentTag.DISCARD) else emptySet()
    }

    /**
     * The collections of [leaves] gathered from another player's hand ([isAnotherPlayersHand]),
     * with every collection split or narrowed out of them — [handDisruptionTags]'s seed.
     */
    private fun handCollections(leaves: List<Effect>, eachPlayer: Player?, draws: Boolean, symmetric: Boolean): HashSet<String> {
        val theirHand = leaves.mapNotNullTo(HashSet()) { leaf ->
            // "Search target player's graveyard, hand, and library for all cards with that name"
            // (Cranial Extraction, Dispossess, Necromentia): a several-zone search that reaches the
            // hand strips it the same way.
            val (zones, player) = when (val source = (leaf as? GatherCardsEffect)?.source) {
                is CardSource.FromZone -> listOf(source.zone) to source.player
                is CardSource.FromMultipleZones -> source.zones to source.player
                else -> return@mapNotNullTo null
            }
            leaf.storeAs.takeIf { Zone.HAND in zones && isAnotherPlayersHand(player, eachPlayer, draws, symmetric) }
        }
        do {
            val before = theirHand.size
            for (leaf in leaves) theirHand += derivedCollections(leaf, theirHand)
        } while (theirHand.size != before)
        return theirHand
    }

    /**
     * "Each opponent loses 3 life unless that player sacrifices a nonland permanent or discards a
     * card" (Torment of Hailfire, Torment of Scarabs' upkeep trigger) — an unless-clause an opponent
     * pays out of their hand — reads as [IntentTag.DISCARD], the same punisher
     * [EachPlayerDiscardsOrLoseLifeEffect] reads as: whichever they pick, the card or the life is
     * theirs to lose. The payer decides it, as in [handDisruptionTags]: an opponent by name, the
     * enchanted player (a Curse's victim), a target player slot, or `you` inside an iteration over
     * opponents. Our own "unless" is an upkeep cost and stays unread.
     *
     * The same punisher written as the opponent's choice between branches — "target opponent loses
     * 3 life unless they sacrifice a nonland permanent or discard a card" as a [ChooseActionEffect]
     * that player makes (Thornplate Intimidator) — reads the same when one branch takes a card from
     * their hand.
     */
    private fun discardPunisherTags(root: Effect, eachPlayer: Player?): Set<IntentTag> {
        val punishes = readingLeaves(root).any { leaf ->
            (leaf is PayOrSufferEffect && paidByAnOpponent(leaf.player, eachPlayer) && offersDiscard(leaf.cost)) ||
                (leaf is ChooseActionEffect && !EffectWalker.choosesOwnBranch(leaf) && paidByAnOpponent(leaf.player, eachPlayer) &&
                    leaf.choices.any { IntentTag.DISCARD in handDisruptionTags(it.effect, eachPlayer) })
        }
        return if (punishes) setOf(IntentTag.DISCARD) else emptySet()
    }

    private fun paidByAnOpponent(payer: EffectTarget, eachPlayer: Player?): Boolean = when (payer) {
        is EffectTarget.ContextTarget, is EffectTarget.BoundVariable -> true
        is EffectTarget.PlayerRef -> payer.player in OPPONENT_PLAYERS || payer.player == Player.EnchantedPlayer ||
            payer.player == Player.TriggeringPlayer ||
            (payer.player == Player.You && eachPlayer != null && !includesYou(eachPlayer))
        EffectTarget.Controller -> eachPlayer != null && !includesYou(eachPlayer)
        else -> false
    }

    private fun offersDiscard(cost: PayCost): Boolean = when (cost) {
        is PayCost.Atom -> cost.atom is CostAtom.Discard || cost.atom is CostAtom.DiscardHand
        is PayCost.Choice -> cost.options.any(::offersDiscard)
        else -> false
    }

    /** Whether a gather from [player]'s hand reaches someone other than the ability's controller — see [handDisruptionTags]. */
    private fun isAnotherPlayersHand(player: Player, eachPlayer: Player?, draws: Boolean, symmetric: Boolean): Boolean = when (player) {
        in OPPONENT_PLAYERS -> true
        Player.You -> eachPlayer != null && (!includesYou(eachPlayer) || symmetric)
        // "That player discards that many cards" off a combat-damage trigger (Dreamstealer,
        // Hypnotic Specter): the player the trigger names is the one hit, as [losesAnOpponent]
        // reads them. An any-player trigger (Necrogen Mists, Oppression) names its controller too,
        // but those are the discard deck's locks, not a wheel's price.
        Player.TargetPlayer, Player.EachTargetedPlayer, Player.TriggeringPlayer, is Player.ContextPlayer, is Player.BoundVariable -> !draws
        else -> false
    }

    /**
     * "Each player discards a card" with no draw anywhere in the effect (Miasmic Mummy, Liliana of the
     * Veil's +1, Enemy of Enlightenment) reads as [IntentTag.DISCARD]: `you` is every player in
     * turn, opponents among them, and the card trades one of ours for one of theirs — the discard
     * deck's parity attack. A draw anywhere beside it — in the same iteration or a later pass —
     * makes it a wheel (Windfall, Wheel of Fortune) whose discard is the price of the refill, so it
     * stays unread.
     */
    private fun symmetricDiscardTags(root: Effect): Set<IntentTag> {
        val leaves = readingLeaves(root)
        val iterations = leaves.filterIsInstance<ForEachEffect>()
        val symmetric = iterations.filter { (it.space as? IterationSpace.Players)?.players in SYMMETRIC_PLAYERS }
        if (symmetric.isEmpty()) return emptySet()
        val draws = (leaves + iterations.flatMap { readingLeaves(it.body) })
            .any { it is DrawCardsEffect || it is DrawUpToEffect }
        if (draws) return emptySet()
        val discards = symmetric.any {
            IntentTag.DISCARD in handDisruptionTags(it.body, (it.space as IterationSpace.Players).players, symmetric = true)
        } ||
            // The picks collected out of the loop and discarded together after it (Creeping Dread):
            // the move is outside the body, so the whole effect is read, the loop in its own frame.
            (symmetric.any { it.collectCollections.isNotEmpty() } &&
                IntentTag.DISCARD in handDisruptionTags(root, eachPlayer = null, symmetric = true))
        return if (discards) setOf(IntentTag.DISCARD) else emptySet()
    }

    /** The iteration spaces that visit every player, opponents included — "each player". */
    private val SYMMETRIC_PLAYERS: Set<Player> = setOf(
        Player.Each, Player.ActivePlayerFirst, Player.EachStartingWithYou, Player.EachStartingWithNextOpponent,
    )

    private val HAND_DISRUPTION_DESTINATIONS = setOf(Zone.GRAVEYARD, Zone.EXILE, Zone.LIBRARY)

    /**
     * "Destroy all creatures" — a gather of the battlefield moved to the graveyard by destruction or
     * sacrifice, to exile, or to hand — reads as a wrath: [IntentTag.REMOVAL] and [IntentTag.SWEEPER], as the `ForEach`
     * spelling of the same card does in [tagsOf]. `Effects.DestroyAll` lowers to this shape.
     *
     * The gathered collection is followed through the steps that split or narrow it, as [digTags]
     * does. Narrowed by a filter, a pile or the remainder of a pick ("choose one to save, destroy the
     * rest"), it is still a group; the *picked* side of a selection (Haphazard Bombardment destroying
     * one of its aimed permanents) is spot removal, so it keeps [IntentTag.REMOVAL] alone.
     *
     * A gather of only our own permanents is a drawback or a cost, not an answer, and stays unread.
     */
    private fun destroyAllTags(root: Effect): Set<IntentTag> = destroyAll(root, eachPlayer = null).tags

    /**
     * [destroyAllTags]'s reading, plus whether a whole group it wipes can hold our own creatures.
     *
     * [eachPlayer] is the iteration a `ForEach` over players runs the pipeline in, or null outside
     * one. Inside it, `Player.You` is the *iterated* player, so "gather the creatures you control,
     * destroy them" spans every player the iteration visits — the Winnowing shape Unstable
     * Glyphbridge and Tragic Arrogance are written in. Outside it, the same gather is our own side
     * only, a drawback or a cost, and stays unread.
     */
    private fun destroyAll(root: Effect, eachPlayer: Player?): DestroyAllReading {
        val leaves = readingLeaves(root)
        val (groups, ours) = boardGroups(leaves, eachPlayer)
        if (groups.isEmpty()) return DestroyAllReading.NONE
        var reading = DestroyAllReading.NONE
        for (leaf in leaves) {
            // "Exile all artifacts your opponents control until this leaves the battlefield"
            // (Consulate Crackdown), "exile each creature with odd mana value" (Extinction Event):
            // the exile-all, a wrath that leaves nothing to recur — or one exiled pick, spot removal.
            // A pile the same effect brings back (Planar Guide's end-step return, Flickerform) is a
            // blink, not removal.
            val exiles = leaf is MoveCollectionEffect &&
                (leaf.destination as? CardDestination.ToZone)?.zone == Zone.EXILE &&
                leaves.none { other ->
                    other !== leaf && other is MoveCollectionEffect && other.from == leaf.from &&
                        (other.destination as? CardDestination.ToZone)?.zone != Zone.EXILE
                }
            val from = when {
                exiles && (leaf as MoveCollectionEffect).from in groups -> leaf.from
                leaf is MoveCollectionEffect && leaf.from in groups &&
                    (leaf.destination as? CardDestination.ToZone)?.zone == Zone.GRAVEYARD &&
                    (leaf.moveType == MoveType.Destroy || leaf.moveType == MoveType.Sacrifice) -> leaf.from
                // "Return all non-Elemental creatures to their owners' hands" (Sunderflock,
                // Aetherize, Hurkyl's Recall): the bounce-all, as the group-targeted bounce reads
                // in [tagsOf] — tempo removal, a wrath when it is still a whole group.
                leaf is MoveCollectionEffect && leaf.from in groups && leaf.faceDown == null &&
                    (leaf.destination as? CardDestination.ToZone)?.zone == Zone.HAND -> leaf.from
                // "Each opponent blights 1", "target opponent blights 2" (High Perfect Morcant,
                // Champion of the Weird): a -1/-1 counter on a creature they pick — the edict's
                // shrinking cousin, the same answer [counterTags] reads in a targeted -1/-1 counter.
                leaf is AddCountersToCollectionEffect && leaf.collectionName in groups &&
                    TargetPolarityAnalyzer.counterPolarity(leaf.counterType) == TargetPolarity.HARMFUL &&
                    leaf.counterType.ptDelta != null -> leaf.collectionName
                else -> continue
            }
            val exile = if (exiles) setOf(IntentTag.EXILE_REMOVAL) else emptySet()
            reading = if (groups[from] == true) {
                DestroyAllReading(reading.tags + setOf(IntentTag.REMOVAL, IntentTag.SWEEPER) + exile, reading.hitsOwnSide || from in ours)
            } else {
                DestroyAllReading(setOf(IntentTag.REMOVAL) + reading.tags + exile, reading.hitsOwnSide)
            }
        }
        return reading
    }

    /**
     * The battlefield groups [destroyAll] reads a wipe off: collection name -> whether it is still a
     * whole group rather than a picked few, and the collections that can hold our own creatures.
     *
     * A loop over players that hands its per-player collections out ([ForEachEffect.collectCollections])
     * hands its groups out too: "each player chooses an artifact, a creature, an enchantment and a
     * planeswalker they control, then sacrifices the rest" (Cataclysmic Gearhulk, Mythos of
     * Snapdax, Single Combat) gathers each player's permanents inside the loop, read in the loop's
     * frame, and sacrifices the unchosen rest of all of them at once after it.
     */
    private fun boardGroups(leaves: List<Effect>, eachPlayer: Player?): Pair<HashMap<String, Boolean>, HashSet<String>> {
        val groups = HashMap<String, Boolean>()
        val ours = HashSet<String>()
        for (leaf in leaves) {
            val loop = leaf as? ForEachEffect
            val players = (loop?.space as? IterationSpace.Players)?.players
            if (loop != null && players != null && loop.collectCollections.isNotEmpty()) {
                val (inner, innerOurs) = boardGroups(readingLeaves(loop.body), players)
                for ((each, all) in loop.collectCollections) {
                    val whole = inner[each] ?: continue
                    groups[all] = (groups[all] ?: false) || whole
                    if (each in innerOurs) ours += all
                }
                continue
            }
            if (leaf !is GatherCardsEffect) continue
            val (player, filter) = when (val source = leaf.source) {
                is CardSource.BattlefieldMatching -> source.player to source.filter
                is CardSource.ControlledPermanents -> source.player to source.filter
                else -> continue
            }
            val ownOnly = player == Player.You || filter.controllerPredicate == ControllerPredicate.ControlledByYou
            if (ownOnly && eachPlayer == null) continue
            // "Exile those tokens when Mysterio leaves the battlefield", Tetravus taking its
            // Tetravites back: the source's own creations, wherever they now sit, are its upkeep or
            // its drawback — never an answer to anyone's board.
            if (StatePredicate.CreatedBySource in filter.statePredicates) continue
            if (filter.controllerPredicate == ControllerPredicate.ControlledByYou && eachPlayer != null) continue
            groups[leaf.storeAs] = true
            // "Exile all attacking creatures target player controls" (Settle the Wreckage) answers
            // the player you point it at — one-sided, like a target-opponent gather.
            val opponentsOnly = opponentsOnly(filter.controllerPredicate) ||
                filter.controllerPredicate == ControllerPredicate.ControlledByTargetPlayer ||
                filter.controllerPredicate is ControllerPredicate.ControlledByReferencedPlayer ||
                (player == Player.You && eachPlayer != null && !includesYou(eachPlayer)) ||
                player in OPPONENT_PLAYERS
            if (!opponentsOnly && SynergyAxes.couldBeCreature(filter)) ours += leaf.storeAs
        }
        do {
            val before = groups.toMap()
            for (leaf in leaves) {
                // The *picked* side of a choice is a few, not the group: a selection's chosen cards,
                // or the one-per-category picks of a per-category edict (Perilous Predicament
                // sacrificing an artifact creature and a nonartifact creature is spot removal).
                val picked = when (leaf) {
                    is SelectFromCollectionEffect -> leaf.storeSelected.takeIf { leaf.from in groups }
                    is ChooseOnePerCategoryEffect -> leaf.storeAs.takeIf { leaf.from in groups }
                    else -> null
                }
                for (name in derivedCollections(leaf, groups.keys)) {
                    val parentIsGroup = sourcesOf(leaf).any { groups[it] == true }
                    groups[name] = (groups[name] ?: false) || (parentIsGroup && name != picked)
                    if (sourcesOf(leaf).any { it in ours }) ours += name
                }
            }
        } while (groups != before)
        return groups to ours
    }

    private data class DestroyAllReading(val tags: Set<IntentTag>, val hitsOwnSide: Boolean) {
        companion object {
            val NONE = DestroyAllReading(emptySet(), hitsOwnSide = false)
        }
    }

    /** Whether an iteration over [players] visits the ability's controller. */
    private fun includesYou(players: Player): Boolean = players !in OPPONENT_PLAYERS

    private val OPPONENT_PLAYERS: Set<Player> = setOf(
        Player.EachOpponent, Player.AnOpponent, Player.TargetOpponent, Player.ChosenOpponent,
        Player.DefendingPlayer, Player.EachDefendingPlayer, Player.EachAttackedPlayer,
    )

    /**
     * Whether [card] has a sweeper that can also hit its own controller's creatures — Wrath of God,
     * Pyroclasm, Bane of the Living's "all creatures get -X/-X", Unstable Glyphbridge — rather than
     * a one-sided one (Plague Wind, Massacre Wurm) that only clears the opponent's board, or a wipe
     * of something no creature is (Global Ruin's lands, an artifact wipe).
     *
     * Reads the spell and its triggered abilities, not its activated ones: a symmetric activation
     * (Pestilence, Thrashing Wumpus) is a choice the controller makes each turn, and a creature
     * deck can simply not use it, while a wrath on resolution or an enters-the-battlefield wipe
     * happens whether the deck likes it or not. This is the deckbuilder's question — "does this
     * fight the rest of my deck?" — and nothing else reads it.
     */
    fun sweepsOwnSide(card: CardDefinition): Boolean = ownSweepCache.getOrPut(card.name) {
        (listOf(card.script) + card.cardFaces.map { it.script }).any { script ->
            EffectWalker.slots(script).any { it.origin != EffectWalker.Origin.ACTIVATED && ownSideSweep(it.effect) } ||
                script.stateTriggeredAbilities.any { ownSideSweep(it.effect) }
        }
    }

    private fun ownSideSweep(root: Effect): Boolean {
        if (destroyAll(root, eachPlayer = null).hitsOwnSide) return true
        return readingLeaves(root).any { leaf ->
            when {
                leaf is ForEachEffect -> when (val space = leaf.space) {
                    is IterationSpace.Group -> IntentTag.SWEEPER in tagsOf(leaf) && groupHitsYou(space.filter)
                    is IterationSpace.Players ->
                        includesYou(space.players) &&
                            (destroyAll(leaf.body, eachPlayer = space.players).hitsOwnSide || ownSideSweep(leaf.body))
                    else -> false
                }
                IntentTag.SWEEPER !in tagsOf(leaf) -> false
                else -> (groupTargetOf(leaf) as? EffectTarget.GroupRef)?.let { groupHitsYou(it.filter) } ?: false
            }
        }
    }

    /** A harmful counter (see [TargetPolarityAnalyzer.counterPolarity]) put on a target only our side can fill. */
    internal fun harmsOwnTarget(type: CounterType, target: EffectTarget, requirements: List<TargetRequirement>): Boolean =
        TargetPolarityAnalyzer.counterPolarity(type) == TargetPolarity.HARMFUL &&
            TargetPolarityAnalyzer.targetsOwnSide(target, requirements)

    /** The target of the leaf shapes [removalTags] reads a group off. */
    private fun groupTargetOf(effect: Effect): EffectTarget? = when (effect) {
        is MoveToZoneEffect -> effect.target
        is MoveUntilSourceLeavesEffect -> effect.target
        is PutOnLibraryPositionOfChoiceEffect -> effect.target
        is DealDamageEffect -> effect.target
        is ModifyStatsEffect -> effect.target
        is SetBaseStatsEffect -> effect.target
        is AddCountersEffect -> effect.target
        is AddDynamicCountersEffect -> effect.target
        else -> null
    }

    private fun groupHitsYou(group: GroupFilter): Boolean =
        !opponentsOnly(group.baseFilter.controllerPredicate) && SynergyAxes.couldBeCreature(group.baseFilter)

    /** "Creatures your opponents control", "creatures you don't control" (Plague Wind), and the like. */
    /** Whether a filter with [predicate] only matches permanents an opponent controls. */
    internal fun opponentsOnly(predicate: ControllerPredicate?): Boolean = when (predicate) {
        ControllerPredicate.ControlledByOpponent,
        ControllerPredicate.ControlledByTargetOpponent,
        ControllerPredicate.ControlledByDefendingPlayer -> true
        is ControllerPredicate.Not -> predicate.predicate == ControllerPredicate.ControlledByYou
        is ControllerPredicate.And -> predicate.predicates.any(::opponentsOnly)
        else -> false
    }

    /** The collections [leaf] reads from, for the steps [derivedCollections] follows. */
    private fun sourcesOf(leaf: Effect): List<String> = when (leaf) {
        is SelectFromCollectionEffect -> listOf(leaf.from)
        is FilterCollectionEffect -> listOf(leaf.from)
        is ChoosePileEffect -> listOf(leaf.pileA, leaf.pileB)
        is ChooseOnePerCategoryEffect -> listOf(leaf.from)
        is MoveCollectionEffect -> listOf(leaf.from)
        else -> emptyList()
    }

    /**
     * "Look at the top N cards of your library and put one of them into your hand" — a gather from
     * our own library top, and a move of those same cards (or the ones picked from among them) into
     * our hand — reads as [IntentTag.DRAW].
     *
     * Neither leaf says it alone: the gather is plumbing every mill, impulse and scry shares, and
     * the move to hand is how a regrowth ends too. So the move must carry the gathered cards: the
     * named collection is followed through every step that splits or narrows it (a selection, a
     * pile, a filter). Grapple with the Past mills and then returns a card from the *graveyard* —
     * a different collection — and stays unread here.
     *
     * A lasting impulse grant reads the same off **any** player's library top: "exile cards equal to
     * its power from the top of its owner's library; you may cast spells from among them" (Dead
     * Man's Chest, an opponent's library) hands us cards as surely as an impulse off our own does.
     * A move into a hand stays own-library only: whose hand the cards land in is the question there.
     *
     * So does casting them on the spot: "exile the top card of each player's library, then you may
     * cast any number of spells from among those cards without paying their mana costs" (Etali,
     * Primal Storm; Gix's and Kotis's spins on it) puts spells on the stack that never cost a card
     * from hand — the impulse with its window closed at resolution instead of at cleanup, which is
     * no loss, since nothing waits to be spent later.
     *
     * So does offering them one at a time: "for each of those cards, put that card into your hand
     * unless any opponent pays 3 life" (Sword-Point Diplomacy) — a for-each over the gathered cards
     * whose body moves each into our hand, directly or behind an opponents' punisher payment. The
     * opponent picks which, but either branch is ours: the card, or the life it cost them.
     *
     * [turnAhead] — the slot is a trigger at the start of our own turn ([EffectWalker.firesWithItsTurnAhead]) —
     * lets a "this turn" impulse read as a draw too: see [isLastingImpulse].
     */
    private fun digTags(root: Effect, turnAhead: Boolean = false): Set<IntentTag> {
        val leaves = readingLeaves(root)
        val fromOurTop = collectionsFromTop(leaves) { it == Player.You }
        val fromAnyTop = collectionsFromTop(leaves) { true }
        val movesThemToOurHand = leaves.any {
            it is MoveCollectionEffect && it.from in fromOurTop &&
                (it.destination as? CardDestination.ToZone)?.let { to -> to.zone == Zone.HAND && to.player == Player.You } == true
        }
        val grantsThemToUs = leaves.any { it is GrantMayPlayFromExileEffect && it.from in fromAnyTop && isLastingImpulse(it, turnAhead) }
        // The same grant closing at this turn's cleanup, timed by us (Act on Impulse, Scion of
        // Opulence): cards to spend now, not cards banked — [IntentTag.IMPULSE].
        val grantsThemThisTurn = leaves.any { it is GrantMayPlayFromExileEffect && it.from in fromAnyTop && grantsToUs(it) }
        val castsThemNow = leaves.any { it is CastAnyNumberFromCollectionWithoutPayingCostEffect && it.from in fromAnyTop }
        val offersThemToOurHand = leaves.any {
            it is ForEachEffect && (it.space as? IterationSpace.Collection)?.collection in fromOurTop &&
                EffectWalker.payoffLeaves(it.body).any(::putsTheIteratedCardIntoHand)
        }
        // "Put any number of creature and/or land cards from among them onto the battlefield"
        // (Nissa, Genesis Mage's −10, Collected Company, Animist's Awakening): the cards come off our
        // library top into play instead of into hand — card advantage that skips the casting. A
        // face-down move (manifest, cloak) is a 2/2 rather than the card itself, and stays unread.
        val putsThemOntoOurBattlefield = leaves.any {
            it is MoveCollectionEffect && it.from in fromOurTop && it.faceDown == null &&
                (it.destination as? CardDestination.ToZone)?.let { to -> to.zone == Zone.BATTLEFIELD && to.player == Player.You } == true
        }
        return when {
            movesThemToOurHand || grantsThemToUs || castsThemNow || offersThemToOurHand || putsThemOntoOurBattlefield ->
                setOf(IntentTag.DRAW)
            grantsThemThisTurn -> setOf(IntentTag.IMPULSE)
            else -> emptySet()
        }
    }

    /**
     * The body step of a for-each over our gathered cards that puts the one iterated into its owner's
     * (our) hand — directly, or as what happens when no opponent pays to stop it (the punisher shape;
     * an opponent paying spends a resource of theirs, so either branch is a payoff of ours).
     */
    private fun putsTheIteratedCardIntoHand(leaf: Effect): Boolean = when (leaf) {
        is MoveToZoneEffect -> leaf.target == EffectTarget.IterationEntity && leaf.destination == Zone.HAND
        is AnyPlayerMayPayEffect -> leaf.eligiblePlayers == Player.EachOpponent &&
            leaf.consequenceIfNonePaid?.let { EffectWalker.payoffLeaves(it).any(::putsTheIteratedCardIntoHand) } == true
        else -> false
    }

    /**
     * "Target player reveals cards from the top of their library until they reveal a land card, then
     * puts those cards into their graveyard" (Balustrade Spy; Tunnel Vision; Sméagol, Helpful Guide's
     * "and the rest into their graveyard"): a [GatherUntilMatchEffect] off a library that can be an
     * opponent's, whose cards — or the ones split from them — are binned. A mill of unknown depth,
     * read as [IntentTag.MILL] like the fixed-count mill ([CardSource.TopOfLibrary.isMill]) is, under
     * the same [canMillOpponent] line. Inside "each opponent …" ([eachPlayer], Consuming
     * Aberration) `You` is the opponent visited. Our own library binned is self-mill fuel, no MILL
     * claim; revealed cards put onto the battlefield, into a hand or back into the library
     * (Polymorph, Madcap Experiment, Aurora Awakener) are no mill either.
     */
    private fun revealUntilMillTags(root: Effect, eachPlayer: Player? = null): Set<IntentTag> {
        val leaves = readingLeaves(root)
        val iteratesOpponents = eachPlayer in OPPONENT_PLAYERS
        val revealed = HashSet<String>()
        for (leaf in leaves) {
            if (leaf is GatherUntilMatchEffect && (canMillOpponent(leaf.player) || (iteratesOpponents && leaf.player == Player.You))) {
                revealed += listOf(leaf.storeMatch, leaf.storeRevealed)
            }
        }
        if (revealed.isEmpty()) return emptySet()
        do {
            val before = revealed.size
            for (leaf in leaves) revealed += derivedCollections(leaf, revealed)
        } while (revealed.size != before)
        val binned = leaves.any {
            it is MoveCollectionEffect && it.from in revealed &&
                (it.destination as? CardDestination.ToZone)?.zone == Zone.GRAVEYARD
        }
        return if (binned) setOf(IntentTag.MILL) else emptySet()
    }

    /** The collections gathered from the library top of a player [whose] accepts, and every one split or narrowed from them. */
    private fun collectionsFromTop(leaves: List<Effect>, whose: (Player) -> Boolean): Set<String> {
        val names = HashSet<String>()
        for (leaf in leaves) when (leaf) {
            is GatherCardsEffect ->
                if ((leaf.source as? CardSource.TopOfLibrary)?.player?.let(whose) == true) names += leaf.storeAs
            // "Reveal cards from the top of your library until you reveal X permanent cards" (Aurora
            // Awakener, Sanar): a dig of unknown depth, the same cards off the same top.
            is GatherUntilMatchEffect -> if (whose(leaf.player)) names += listOf(leaf.storeMatch, leaf.storeRevealed)
            // "Each opponent exiles cards from the top of their library until …" (Dream Harvest).
            is ExileLibraryUntilManaValueEffect -> if (whose(leaf.players)) names += leaf.storeAs
            else -> {}
        }
        if (names.isEmpty()) return names
        do {
            val before = names.size
            for (leaf in leaves) names += derivedCollections(leaf, names)
        } while (names.size != before)
        return names
    }

    /**
     * An impulse draw whose cards we can still play after this turn — "until the end of your next
     * turn", "for as long as it remains exiled" — is a draw, the cards only waiting in exile instead
     * of in hand. One that closes this turn ("until end of turn") is not
     * read as one: a DRAW reading earns the idle-mana and end-step windows, and cards that must be
     * spent before cleanup are worth nothing cast into them.
     *
     * Unless the grant is made at the start of our own turn ([turnAhead]: an "at the beginning of
     * your upkeep" trigger, Vance's Blasting Cannons): the ability is never ours to time, and both
     * main phases of the turn are still ahead to spend the card in, as a card drawn that turn is.
     *
     * "Until your next end step" is a draw from any source (Dragonhawk, Fate's Tempest): granted on
     * our turn before the end step, a main phase is still ahead or under way; granted in our end
     * step or on an opponent's turn, the window runs through our whole next turn.
     */
    private fun isLastingImpulse(grant: GrantMayPlayFromExileEffect, turnAhead: Boolean = false): Boolean =
        grantsToUs(grant) &&
            when (val expiry = grant.expiry) {
                MayPlayExpiry.EndOfTurn -> turnAhead
                is MayPlayExpiry.UntilControllerStep -> !expiry.includeCurrentTurn || expiry.step == Step.END ||
                    (turnAhead && expiry.step == Step.CLEANUP)
                else -> true
            }

    /** Whether [grant] lets its controller play the cards, unconditionally — an impulse of ours, whatever its window. */
    private fun grantsToUs(grant: GrantMayPlayFromExileEffect): Boolean =
        grant.recipient == EffectTarget.Controller && !grant.ownerControls && grant.condition == null

    /** The collections [leaf] stores from one of [sources] — the pipeline steps that split or narrow a collection. */
    internal fun derivedCollections(leaf: Effect, sources: Set<String>): List<String> = when (leaf) {
        is SelectFromCollectionEffect -> if (leaf.from in sources) listOfNotNull(leaf.storeSelected, leaf.storeRemainder) else emptyList()
        is FilterCollectionEffect -> if (leaf.from in sources) listOfNotNull(leaf.storeMatching, leaf.storeNonMatching) else emptyList()
        is ChoosePileEffect -> if (leaf.pileA in sources || leaf.pileB in sources) listOf(leaf.storeChosenAs, leaf.storeOtherAs) else emptyList()
        is ChooseOnePerCategoryEffect -> if (leaf.from in sources) listOf(leaf.storeAs) else emptyList()
        is MoveCollectionEffect -> if (leaf.from in sources) listOfNotNull(leaf.storeMovedAs) else emptyList()
        // A later step re-gathering a stored collection by name ("an opponent chooses one of them"
        // after the reveal, Extrapolate the Impossible) carries the same cards on.
        is GatherCardsEffect -> {
            val source = leaf.source
            if (source is CardSource.FromVariable && source.variableName in sources) listOf(leaf.storeAs) else emptyList()
        }
        else -> emptyList()
    }

    /**
     * The filter of a gather from its controller's own sideboard — "from outside the game". A gather
     * pooling the sideboard with other zones of ours is a wish too: "reveal an Eldrazi card you own
     * from outside the game or choose a face-up Eldrazi card you own in exile" (Coax from the Blind
     * Eternities, Karn, the Great Creator's −2) is one pick across both, the outside-the-game half
     * the wish. [HoldPolicy]'s dead-wish check reads only the sideboard, so a pick that could only
     * come from exile is not counted live there — conservative, never a wish thrown away.
     * A gather that also pools the library (Turtles Forever) is a library tutor, not a wish: the
     * library half keeps it live, and [DeadSearch] owns library deadness.
     */
    private fun wishFilterOf(effect: Effect): GameObjectFilter? {
        if (effect !is GatherCardsEffect) return null
        return when (val source = effect.source) {
            is CardSource.FromZone ->
                source.filter.takeIf { source.zone == Zone.SIDEBOARD && source.player == Player.You }
            is CardSource.FromMultipleZones ->
                source.filter.takeIf {
                    Zone.SIDEBOARD in source.zones && Zone.LIBRARY !in source.zones && source.player == Player.You
                }
            else -> null
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Effect → tags
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * The tags one effect leaf reads as on its own, for [com.wingedsheep.ai.engine.LimitedCardRater]:
     * a leaf the rater prices by its own fields (a -N/-N) asks here whether it is an answer.
     */
    internal fun leafTags(effect: Effect): Set<IntentTag> = tagsOf(effect)

    private fun tagsOf(effect: Effect): Set<IntentTag> = when {
        effect === DELAYED_BLINK -> setOf(IntentTag.PROTECTION)
        effect === OWN_STASH -> setOf(IntentTag.PROTECTION)
        effect === TEMPORARY_EXILE -> setOf(IntentTag.REMOVAL)
        effect === GRAVEYARD_HATE_LEAF -> setOf(IntentTag.GRAVEYARD_HATE)
        else -> leafTagsOf(effect)
    }

    private fun leafTagsOf(effect: Effect): Set<IntentTag> = when (effect) {
        // "If a source you control would deal damage this turn, it deals double that damage
        // instead" (Insult): a one-sided multiplier for the turn — the same payoff a DoubleDamage
        // permanent reads as, cashed in by what attacks or burns after it resolves.
        is AmplifyDamageThisTurnEffect ->
            if (DamageAmplifiers.reads(effect)) setOf(IntentTag.DAMAGE_AMPLIFIER) else emptySet()
        is ReflexiveTriggerEffect -> analyzeEffect(effect.reflexiveEffect, effect.reflexiveTargetRequirements).tags
        is MoveToZoneEffect -> when {
            // A group-targeted move is a wrath; a single-target one is spot removal. Both only
            // count as removal when they take *someone else's* permanent off the battlefield.
            effect.destination == Zone.BATTLEFIELD -> setOfNotNull(
                IntentTag.RECURSION.takeIf { effect.fromZone == Zone.GRAVEYARD }
            )

            effect.fromZone == Zone.GRAVEYARD -> setOf(IntentTag.RECURSION)
            !hitsAnotherPermanent(effect.target) -> emptySet()
            effect.destination == Zone.EXILE ->
                removalTags(effect.target) + IntentTag.EXILE_REMOVAL

            else -> removalTags(effect.target)
        }

        is MoveUntilSourceLeavesEffect -> when {
            !hitsAnotherPermanent(effect.target) -> emptySet()
            effect.destination == Zone.EXILE -> removalTags(effect.target) + IntentTag.EXILE_REMOVAL
            else -> removalTags(effect.target)
        }
        is ExileUntilLeavesEffect -> setOf(IntentTag.REMOVAL, IntentTag.EXILE_REMOVAL)
        // A tuck whose owner picks the top or bottom of their library (Dire Downdraft, Aether
        // Gust) takes the permanent away like the tuck above: removal, paid back at most as a draw.
        is PutOnLibraryPositionOfChoiceEffect ->
            if (hitsAnotherPermanent(effect.target)) removalTags(effect.target) else emptySet()
        is PhaseOutUntilLeavesEffect -> setOf(IntentTag.REMOVAL)
        is ForceSacrificeEffect -> setOf(IntentTag.REMOVAL)
        // "When this creature becomes the target of a spell or ability, sacrifice it" (Phantasmal
        // Shieldback, the Illusions): the source sacrificing itself is its drawback, never an answer.
        is SacrificeTargetEffect ->
            if (effect.target == EffectTarget.Self || sacrificesOwnAttachedHost(effect)) emptySet() else setOf(IntentTag.REMOVAL)
        is GainControlEffect -> setOf(IntentTag.REMOVAL)
        // "Exchange control of target artifact you control and target artifact or creature you don't
        // control" (Shrewd Negotiation, Switcheroo, Chromeshell Crab): their permanent is taken as
        // [GainControlEffect] takes it; ours, handed back, is the price — a steal, priced as removal.
        is ExchangeControlEffect -> setOf(IntentTag.REMOVAL)
        is FightEffect -> setOf(IntentTag.REMOVAL, IntentTag.FIGHT)

        // Damage to a *player* is a clock, not an answer — a permanent that pings the opponent for
        // 1 each upkeep must not be priced as repeatable removal.
        is DealDamageEffect -> when {
            namesAPlayer(effect.target) -> setOf(IntentTag.DAMAGE_TO_PLAYERS)
            effect.target == EffectTarget.Self -> emptySet()
            else -> removalTags(effect.target)
        }
        // "Each opponent loses life equal to …" (Throne of the God-Pharaoh), "target player loses 2
        // life": the same clock on a player as face damage, minus the source. Life its controller
        // loses is the card's price, not its point.
        is LoseLifeEffect -> if (losesAnOpponent(effect.target)) setOf(IntentTag.DAMAGE_TO_PLAYERS) else emptySet()

        is DividedDamageEffect -> setOf(IntentTag.REMOVAL)

        is ModifyStatsEffect -> {
            val shrinks = AmountSign.of(effect.toughnessModifier) == AmountSign.LOSS
            when {
                // "-N/-N" is removal by another name, and reaches exactly N toughness. A dynamic
                // "-X/-X" (Death Wind's X, Defile's Swamp count) is the same answer with a reach
                // no static reader can bound — read like X burn, which leaves reach null too.
                shrinks && hitsAnotherPermanent(effect.target) -> removalTags(effect.target)

                shrinks -> emptySet()
                // "Target creature an opponent controls gets -2/-0 until end of turn" (Ominous
                // Sphinx, Dizzy Spell): a cut to power alone answers nothing — the creature lives —
                // but blunts it as an attacker or blocker for the fight: a combat effect, the
                // opposing side of a first-strike grant. A creature's cut to its own power is a price.
                AmountSign.of(effect.powerModifier) == AmountSign.LOSS && hitsAnotherPermanent(effect.target) ->
                    setOf(IntentTag.COMBAT_ENHANCEMENT)
                // "+X/+X where X is …" is a pump whenever X cannot go negative — see [AmountSign].
                raises(effect.powerModifier) || raises(effect.toughnessModifier) -> setOf(IntentTag.PUMP)
                else -> emptySet()
            }
        }

        // "Target creature has base toughness 0 until end of turn" kills anything not propped up
        // by counters or a pump — removal with no toughness defence, like destroy.
        // A base of 1/1 or smaller that leaves it alive (Warkite Marauder's 0/1), or no power and
        // little toughness (Sorceress Queen's 0/2), shuts the creature off for the duration — the
        // same line the polarity reader draws for a small base.
        // Any other base — a fixed 4/4 (Square Up), X/X (Mirror Entity), or the source's own power
        // and toughness (Exuberant Wolfbear) — *replaces* the creature's stats for a fight: a combat
        // enhancement, never a pump a plan may add on top. Whose creature gets it is the polarity
        // reader's call (a large base on its own reads flexible there).
        is SetBaseStatsEffect -> when {
            !hitsAnotherPermanent(effect.target) -> emptySet()
            effect.toughness?.let(::fixed) == 0 -> removalTags(effect.target)
            setsSmallBase(effect) -> setOf(IntentTag.NEUTRALIZE)
            else -> setOf(IntentTag.COMBAT_ENHANCEMENT)
        }

        // "Untap up to two target lands you control. They become 5/5 Elemental creatures with flying
        // and haste until end of turn" (Nissa, Steward of Elements), "target land becomes a 3/3
        // creature" (Nissa of Shadowed Boughs, Sylvan Awakening's each-land body): another permanent
        // turned into a creature for the turn's fight — a combat enhancement, like a base P/T it
        // takes from something bigger. The source animating itself (a manland) is just a body.
        is BecomeCreatureEffect -> if (hitsAnotherPermanent(effect.target)) setOf(IntentTag.COMBAT_ENHANCEMENT) else emptySet()
        is AnimateLandEffect -> if (hitsAnotherPermanent(effect.target)) setOf(IntentTag.COMBAT_ENHANCEMENT) else emptySet()

        // "Target creature loses all abilities" strips its evasion, deathtouch and triggers: still on
        // the battlefield, shut off — the Pacifism reading without the static.
        is RemoveAllAbilitiesEffect -> if (hitsAnotherPermanent(effect.target)) setOf(IntentTag.NEUTRALIZE) else emptySet()

        // "Untap target attacking creature an opponent controls and remove it from combat" (Spires
        // of Orazca, Labyrinth of Skophos): the attacker deals no damage and the blocker blocks
        // nothing — one creature shut off for the combat, still on the battlefield. Our own creature
        // pulled out is dropped by [readingLeaves]; the source itself (Gustcloak Runner) is no answer,
        // and neither is the creature that triggered it (Gustcloak Savior's own blocked attacker).
        is RemoveFromCombatEffect ->
            if (hitsAnotherPermanent(effect.target) && effect.target != EffectTarget.TriggeringEntity) setOf(IntentTag.NEUTRALIZE)
            else emptySet()

        // Explore looks at the top card and keeps it or bins it — a scry's card flow — and the land
        // it finds goes to hand or the explorer gets a +1/+1 counter. That counter on another
        // creature is a pump (Enter the Unknown); on the explorer itself it is self-growth, which
        // needs no tag (Jadelight Ranger).
        is ExploreEffect -> setOf(IntentTag.CARD_SELECTION) + counterTags(CounterType.PLUS_ONE_PLUS_ONE, effect.target)

        // "Take an extra turn after this one": a whole turn for its controller, or for the player it
        // targets, who is the controller whenever it is cast to be played.
        is TakeExtraTurnEffect -> if (takesTheTurnOurselves(effect.target)) setOf(IntentTag.EXTRA_TURN) else emptySet()

        is AddCountersEffect -> counterTags(effect.counterType, effect.target)
        is AddDynamicCountersEffect -> counterTags(effect.counterType, effect.target)
        // "Create a Sorcerer Role token attached to target creature you control" (Spellbook Vendor),
        // "… a Cursed Role token attached to target creature an opponent controls" (Diminisher
        // Witch): the token does to its creature what the Aura does — see [auraTokenTags].
        is CreateAuraTokenEffect -> auraTokenTags(effect)
        // "Remove a -1/-1 counter from another target creature you control" (Defiant Greatmaw),
        // "remove all -1/-1 counters from it" (Hapatra's Mark): the stats the counter took come
        // back, a lasting pump. Removing a +1/+1 or a keyword counter names no purpose a tag holds.
        is RemoveCountersEffect -> removedCounterTags(effect.counterType, effect.target)
        is RemoveAllCountersOfTypeEffect -> removedCounterTags(effect.counterType, effect.target)
        // Divided among the targets (each a permanent), and doubling what one holds: the counter
        // decides it, as for a plain placement.
        is DistributeCountersAmongTargetsEffect -> counterTags(effect.counterType, EffectTarget.ContextTarget(0))
        is DoubleCountersEffect -> effect.counterType?.let { counterTags(it, effect.target) } ?: emptySet()
        // "A kind it doesn't have, from among …" reads by the listed kinds when they agree. A kind
        // chosen among those already on the recipient (Animation Module) names none — proliferate's
        // one-kind form, whose worth is the board's counters, so it needs no tag.
        is AddCountersOfChosenKindEffect ->
            effect.kinds.map { counterTags(it, effect.target) }.distinct().singleOrNull() ?: emptySet()

        is TapUntapEffect -> if (effect.tap) setOf(IntentTag.TAPPER) else emptySet()
        // "Creatures target opponent controls don't untap during their next untap step" (Imaginary
        // Threats, Exhaustion): keeps their permanents tapped — the tapper's payoff, a turn later.
        // A chosen target player reads like a cast ban's ([bansAnOpponent]): it is cast at an
        // opponent. The god spells' "lands you control don't untap" price binds its controller,
        // which `AiReadingAudit` reads as needing no intent.
        is SkipUntapEffect -> if (bansAnOpponent(effect.target)) setOf(IntentTag.TAPPER) else emptySet()
        is TapUntapCollectionEffect -> if (effect.tap) setOf(IntentTag.TAPPER) else emptySet()

        is DrawCardsEffect, is DrawUpToEffect -> setOf(IntentTag.DRAW)
        // Surveil is scry whose bottom is the graveyard: the same look at the top, the same card flow.
        is ScryEffect, is SurveilEffect -> setOf(IntentTag.CARD_SELECTION)
        // "Copy that card and you may cast the copy" (God-Eternal Kefnet, Shiko): the copy is a
        // spell the controller gets on top of the cards they hold — card advantage, read as a draw.
        is CopyCardIntoCollectionEffect, is CopyCollectionIntoCollectionEffect -> setOf(IntentTag.DRAW)
        // "This creature becomes prepared" hands its controller a copy of the creature's prepare
        // spell to cast from exile — a spell on top of the cards in hand, the same card advantage
        // as the copy above. A creature with no prepare spell can't become prepared, so the leaf
        // only ever pays off; which creature it readies is the polarity check's question.
        is BecomePreparedEffect -> setOf(IntentTag.DRAW)
        // Discover N exiles down to a nonland card of mana value N or less, which its controller
        // casts free or puts into their hand: either way one more card than they had, like the
        // free cast off the library top that [digTags] reads as a draw.
        is DiscoverEffect -> setOf(IntentTag.DRAW)
        // Cascade is the same free cast off the library top, bounded by the spell's own mana
        // value instead of a number: one more spell than its controller held.
        is CascadeEffect -> setOf(IntentTag.DRAW)
        // "Whenever this creature becomes the target of a spell or ability …, counter that spell or
        // ability" (Glyph Keeper) shields the permanent from what targets it — ward-like
        // protection, not a counterspell: it never answers a spell aimed elsewhere, and reading it
        // as one would mark the creature a dead card on an empty stack.
        is CounterEffect ->
            if (effect.targetSource == CounterTargetSource.TargetingSource) setOf(IntentTag.PROTECTION)
            else setOf(IntentTag.COUNTERSPELL)
        is CounterAllOnStackEffect -> setOf(IntentTag.COUNTERSPELL)
        // "Return target spell to its owner's hand" (Failure, Reprieve) answers a spell on the stack
        // the way a counter does — its caster gets the card back, not the tempo or the mana.
        ReturnSpellToOwnersHandEffect -> setOf(IntentTag.COUNTERSPELL)
        // "Return target spell or permanent to its owner's hand" (Venser, Shaper Savant,
        // Unsubstantiate) reaches the battlefield too, so it is a bounce first: removal, the way a
        // single-target return of a permanent to hand reads above. Not [IntentTag.COUNTERSPELL] —
        // that would brand the card dead on an empty stack, where its permanent half is live.
        is ReturnSpellOrPermanentToOwnersHandEffect -> removalTags(effect.target)
        // "End the turn" as a card's whole payoff (Time Stop, Discontinuity, Glorious End) exiles
        // every spell and ability on the stack — a counter-everything that can't be stopped — and
        // takes the attackers out of combat with it. What it answers is already on the stack or in
        // the fight, so ending our own main phase for nothing is no play ([HoldPolicy]). Beside
        // another payoff it is a rider and never reaches here ([readingLeaves]).
        EndTheTurnEffect -> setOf(IntentTag.COUNTERSPELL)
        // "You may choose new targets for target spell" (Insidious Will, Sideswipe) is the all-targets
        // form of the same redirect.
        is ChangeTargetEffect, is ChangeTriggeringObjectTargetsEffect -> setOf(IntentTag.SPELL_REDIRECT)
        // "Copy target spell", "copy target activated or triggered ability", "when you next cast an
        // instant or sorcery spell this turn, copy it": see [IntentTag.SPELL_COPY].
        is CopyTargetSpellEffect, is CopyTargetSpellOrAbilityEffect, is CopyTargetTriggeredAbilityEffect,
        is CopyEachTargetSpellEffect, is CopyForOtherPossibleTargetsEffect, is CopyNextSpellCastEffect,
        is CopyEachSpellCastEffect -> setOf(IntentTag.SPELL_COPY)
        is GainLifeEffect, is DrainLifeEffect -> setOf(IntentTag.LIFEGAIN)
        // An exchange is carried out as a gain for the player whose total goes up (rule 701.12c),
        // and its controller only casts it to be that player: Psychic Transfer at 4 against 9.
        is ExchangeLifeTotalsEffect -> setOf(IntentTag.LIFEGAIN)
        // "Your life total becomes 26" (Providence) is carried out as a gain of the difference
        // (rule 119.5), and a fixed total above the starting 20 is one its controller only casts
        // when it is a gain. A total set at or below 20 (Rebirth, Form of the Dragon, Angel of
        // Grace), one that hangs on the board, or another player's is no gain to read here.
        is SetLifeTotalEffect -> if (setsOwnLifeAboveStart(effect)) setOf(IntentTag.LIFEGAIN) else emptySet()
        is CreateTokenEffect, is CreatePredefinedTokenEffect, is InvestigateEffect, is CreateTokenCopyOfTargetEffect,
        is CreateTokenCopyOfSourceEffect -> setOf(IntentTag.TOKEN_MAKER)
        // Amass makes the Army token when there is none and grows it when there is: either way a
        // creature body on the battlefield, the same payoff as the token it creates.
        is AmassEffect -> setOf(IntentTag.TOKEN_MAKER)

        is AddManaEffect, is AddColorlessManaEffect, is AddDynamicManaEffect,
        is AddManaOfChoiceEffect, is PlayAdditionalLandsEffect -> setOf(IntentTag.RAMP)

        is RegenerateEffect, is HealDamageEffect -> setOf(IntentTag.PROTECTION)
        // A shield is protection; the reaction it carries ("for each 1 damage prevented this way,
        // create a 2/1 Inkling", Inkshield; "you gain that much life", Awe Strike) is a payoff the
        // card makes on top of it. Only the board payoffs are read off it: everything else in a
        // reaction happens only if the shield fires, so reading Deflecting Palm's reflected damage as
        // burn, or New Way Forward's draw as a cantrip, would aim the card at a face or at the
        // opponent's end step, where it does nothing.
        is PreventDamageEffect -> setOf(IntentTag.PROTECTION) +
            (effect.onPrevented?.let { reaction -> EffectWalker.leaves(reaction).flatMapTo(mutableSetOf()) { tagsOf(it) } }
                ?.intersect(SHIELD_REACTION_PAYOFFS) ?: emptySet())
        // "All damage that would be dealt this turn to you and permanents you control is dealt to
        // enchanted creature instead" (Saving Grace, Gideon's Sacrifice, Glarecaster): a shield over
        // what it protects, the damage soaked by a body chosen for it — a prevention that lands elsewhere.
        is RedirectNextDamageEffect -> setOf(IntentTag.PROTECTION)

        // "Target creature an opponent controls doesn't untap during its controller's next untap
        // step" (Kefnet's Monument, Barl's Cage): keeps a chosen permanent tapped — the tapper's
        // payoff, as [SkipUntapEffect] reads it; which side it lands on is the polarity check's. On
        // the source or its host ("it doesn't untap", Stitcher's Graft's attacker) it is a price.
        is GrantKeywordEffect ->
            if (effect.keyword in LOCKED_TAPPED_FLAGS &&
                (effect.target is EffectTarget.BoundVariable || effect.target is EffectTarget.ContextTarget)
            ) setOf(IntentTag.TAPPER)
            else keywordTags(effect.keyword)
        is GrantEvasionKeywordEffect -> setOf(IntentTag.EVASION_GRANT)
        // "Target creature must be blocked this turn if able" / "all creatures able to block it do
        // so" (Magitek Scythe, Lure effects): the attacker picks its fight, or drags every blocker
        // off the rest of the team. Read for our attacker; nobody lures their opponent's creature.
        is MustBeBlockedEffect -> setOf(IntentTag.COMBAT_ENHANCEMENT)
        // "Target creature blocks this turn if able" (Peema Aether-Seer, Culvert Ambusher): the
        // other half of a lure — the attacker drags an opposing creature into a block it would not
        // choose, onto a deathtoucher or a bigger attacker. Read for our attack, like the lure.
        is MarkMustBlockThisTurnEffect -> if (hitsAnotherPermanent(effect.target)) setOf(IntentTag.COMBAT_ENHANCEMENT) else emptySet()
        // "Target creature defending player controls blocks it this combat if able" (Impetuous
        // Devils, Avalanche Tusker): the same lure, pinned to the attacker that names it.
        is ForceBlockEffect -> if (hitsAnotherPermanent(effect.target)) setOf(IntentTag.COMBAT_ENHANCEMENT) else emptySet()
        // "This creature can attack this turn as though it didn't have defender" (Hightide Hermit,
        // Roving Keep): the one-turn form of the static [CanAttackDespiteDefender] — a wall turned
        // attacker for the fight, to be activated before attacks.
        is CanAttackDespiteDefenderThisTurnEffect -> setOf(IntentTag.COMBAT_ENHANCEMENT)
        // "Attach target Equipment you control to target creature you control", "attach any number
        // of Equipment you control to it": a free equip, see [IntentTag.EQUIPMENT_RELOCATION].
        // An Equipment attaching only itself (Pre-War Formalwear) moves no other Equipment.
        is AttachTargetEquipmentToCreatureEffect ->
            if (effect.equipmentTarget == EffectTarget.Self) emptySet() else setOf(IntentTag.EQUIPMENT_RELOCATION)
        // "This becomes a copy of target creature", "each other creature you control becomes a copy
        // of it": see [IntentTag.PERMANENT_COPY].
        is EachPermanentBecomesCopyOfTargetEffect -> setOf(IntentTag.PERMANENT_COPY)
        // "Creatures your opponents control can't block this turn" / "creatures you control can't be
        // blocked this turn": a turn of team-wide evasion, the same alpha-strike payoff.
        is CantBlockGroupEffect, is CantBeBlockedGroupEffect -> setOf(IntentTag.EVASION_GRANT)
        // "Target creature can't block this turn" (Earthshaker Khenra, Gilded Cerodon): one blocker
        // taken out of the opponent's defence — the single-creature form of the team-wide evasion
        // above. "This creature can't block" on its own source is a drawback, not a payoff.
        is CantBlockEffect -> if (hitsAnotherPermanent(effect.target)) setOf(IntentTag.EVASION_GRANT) else emptySet()
        // "You may cast an instant or sorcery spell from your hand without paying its mana cost"
        // (Wildfire Eternal, Omnispell Adept), "… cast it without paying its mana cost" off the top
        // of a library or out of exile: a spell for free, see [IntentTag.FREE_SPELL]. A cast that
        // still pays the spell's mana cost is only a zone it may be cast from, and earns no tag.
        is CastFromCollectionWithoutPayingCostEffect -> if (effect.payManaCost) emptySet() else setOf(IntentTag.FREE_SPELL)
        // "Until end of turn, you may cast cards exiled this way without paying their mana costs"
        // (Dream Harvest, Mind's Desire, a hideaway land's "play the exiled card without paying its
        // mana cost"): the same free spell, granted for later in the turn instead of cast now.
        // Handed back to each card's owner, it is theirs to cast, not ours.
        is GrantPlayWithoutPayingCostEffect -> if (effect.ownerControls) emptySet() else setOf(IntentTag.FREE_SPELL)

        // "Your opponents can't cast spells this turn" / "target player can't cast …": disruption whose
        // payoff is spells never cast. A ban that binds its controller too is a price, not a payoff.
        is CantCastSpellsEffect -> if (bansAnOpponent(effect.target)) setOf(IntentTag.CAST_BAN) else emptySet()

        // "Creatures target opponent controls attack this turn if able" (Imaginary Threats), "each
        // creature your opponents control attacks … if able" (Kardur): an attack the
        // opponent didn't choose. A requirement on our own creatures is a price, not a payoff.
        is MustAttackGroupEffect -> if (opponentsOnly(effect.filter.baseFilter.controllerPredicate)) setOf(IntentTag.FORCED_ATTACK) else emptySet()

        // "Each token that would be created under an opponent's control this turn is created under
        // your control instead" (Crafty Cutpurse): the opponent's tokens become ours for the turn.
        // Only a redirect toward its controller from the opponents' side reads; other granted
        // replacement kinds are still unread.
        is GrantReplacementEffectEffect -> when (val replacement = effect.replacement) {
            is RedirectTokenCreationController ->
                if ((replacement.appliesTo as? com.wingedsheep.sdk.scripting.EventPattern.TokenCreationEvent)
                        ?.controller == com.wingedsheep.sdk.scripting.references.Player.EachOpponent
                ) setOf(IntentTag.TOKEN_THEFT) else emptySet()
            else -> emptySet()
        }

        // A tutor is a search of our own library by us ([searchesOwnZones]). Searching another
        // player's (Cranial Extraction, Supreme Inquisitor, Invasive Surgery's delirium exile) is
        // disruption; a search we hand to someone else (Settle the Wreckage's basic lands for the
        // attacker's controller) is their gain, not ours.
        is GatherCardsEffect -> when (val source = effect.source) {
            is CardSource.FromZone ->
                if (source.zone == Zone.LIBRARY && searchesOwnZones(effect, source.player)) setOf(IntentTag.TUTOR) else emptySet()
            // "Search your library and/or graveyard for a card named …" (Vraska's Scorn, the
            // planeswalker-deck tutors): a search of our own zones that reaches the library is a
            // tutor. Searching an opponent's (Cranial Extraction, Lobotomy) is disruption, not this.
            is CardSource.FromMultipleZones ->
                if (Zone.LIBRARY in source.zones && searchesOwnZones(effect, source.player)) setOf(IntentTag.TUTOR) else emptySet()
            is CardSource.TopOfLibrary -> if (source.isMill && canMillOpponent(source.player) &&
                ((source.count as? DynamicAmount.Fixed)?.amount ?: 1) > 0
            ) setOf(IntentTag.MILL) else emptySet()
            else -> emptySet()
        }

        is EachPlayerDiscardsOrLoseLifeEffect -> setOf(IntentTag.DISCARD)
        is ReturnSameNamedFromGraveyardEffect -> setOf(IntentTag.RECURSION)
        // "Return this card to the battlefield (transformed) attached to target opponent" (Accursed
        // Witch, Radiant Grace, Gryff's Boon, Dragon Wings): our own card back out of the graveyard
        // as an Aura — the single-card return a graveyard-to-battlefield [MoveToZoneEffect] reads
        // as. Whom it lands on is the polarity reader's question, read off what the Aura does.
        is ReturnSelfToBattlefieldAttachedEffect -> setOf(IntentTag.RECURSION)
        // "Return it to the battlefield transformed" out of the graveyard (Harvest Hand, Skin
        // Invasion, the Ojer gods): our own card back onto the battlefield as its other face — the
        // single-card return a graveyard-to-battlefield [MoveToZoneEffect] reads as.
        is ReturnSelfFromZoneTransformedEffect ->
            if (effect.fromZone == Zone.GRAVEYARD) setOf(IntentTag.RECURSION) else emptySet()

        // A wrath is "for each creature: destroy it" — an iteration, not a group-targeted move, so
        // the shape only shows up here. [EffectWalker] deliberately treats a ForEach as a leaf (see
        // there for why), which makes this the analyzer's own reading of the node: interpret the
        // body, and promote it to a sweeper when the iteration spans a whole group of permanents.
        is ForEachEffect -> {
            val inner = effectTags(effect.body)
            val space = effect.space
            when {
                space is IterationSpace.Group && IntentTag.REMOVAL in inner -> inner + IntentTag.SWEEPER
                // "For each player, destroy the creatures they control (but one)": the pipeline
                // gathers `You` — the iterated player — so it only reads as a wrath in the
                // iteration's frame. See [destroyAll].
                // Over opponents only, the body's `you` is the opponent visited: what it gains is
                // theirs, not ours (see [OPPONENT_GIFTS]) — unless a leaf hands it to the source's
                // controller by name (Adeline's tokens, Garruk, Veiled Butcher's draw).
                space is IterationSpace.Players -> (if (includesYou(space.players)) inner else opponentIterationTags(effect.body, inner)) +
                    destroyAll(effect.body, eachPlayer = space.players).tags +
                    handDisruptionTags(effect.body, eachPlayer = space.players) +
                    discardPunisherTags(effect.body, eachPlayer = space.players) +
                    graveyardExileTags(effect.body, eachPlayer = space.players) +
                    revealUntilMillTags(effect.body, eachPlayer = space.players)
                // "Untap all lands you control" (Emrakul, the Exigent Doom's cast trigger): every
                // land of ours taps for mana again, the same payoff as the mana it makes.
                space is IterationSpace.Group && untapsOurLands(space.filter, effect.body) -> inner + IntentTag.RAMP
                // "Untap all creatures you control" (Jace, Ingenious Mind-Mage; an extra combat's
                // "untap all creatures that attacked"): our attackers stand back up to block or attack
                // again — the vigilance a granted keyword gives, read the same way.
                space is IterationSpace.Group && untapsOurCreatures(space.filter, effect.body) -> inner + IntentTag.COMBAT_ENHANCEMENT
                else -> inner
            }
        }

        // An emblem, or a "for the rest of the game / this turn, whenever …" trigger, is the
        // ability it grants: "whenever you cast a spell, draw a card" (Chandra, Chill of Compliance)
        // is card draw and "… deals 5 damage to any target" (Chandra, Torch of Defiance) removal,
        // each paid out later, as a delayed trigger is read as the effect it fires.
        // Temporary grants have the same payoff; HoldPolicy excludes them from automatic
        // end-step card-flow windows because their event still needs to happen.
        is CreateGlobalTriggeredAbilityEffect ->
            effectTags(effect.ability.effect)

        // "Target instant or sorcery card in your graveyard gains flashback" (Stingcaster Mage,
        // Snapcaster Mage): the card can be cast again from the graveyard — a recursion, paid for
        // with its own mana cost.
        is GrantFlashbackEffect -> setOf(IntentTag.RECURSION)

        // "Target land gains '{T}: Add {C}{C}'" (Emrakul, the Exigent Doom): a mana ability handed
        // to a permanent is a mana source, as a mana rock is. Any other helpful ability granted for
        // more than a turn is the holder's own ability from then on, read as if printed:
        // Quicksmith Spy's artifact gaining "{T}: Draw a card" for as long as you control the Spy
        // is a card-advantage engine. A turn-long grant still waits on its activation, so the
        // HoldPolicy card-flow windows must not cash it.
        is GrantActivatedAbilityEffect -> when {
            effect.ability.isManaAbility -> setOf(IntentTag.RAMP)
            effect.duration != Duration.EndOfTurn &&
                TargetPolarityAnalyzer.grantedActivatedPolarity(effect.ability) == TargetPolarity.BENEFICIAL ->
                analyzeEffect(effect.ability.effect, effect.ability.targetRequirements).tags
            else -> emptySet()
        }

        // "You get an emblem with 'Creatures you control get +2/+2'" (Ajani Resolute) is an anthem
        // nothing can remove; its keywords and statics read as the same grants printed on a lord.
        is CreatePermanentEmblemEffect -> buildSet {
            val bonus = effect.powerBonus >= 0 && effect.toughnessBonus >= 0 && effect.powerBonus + effect.toughnessBonus > 0
            if (bonus) add(IntentTag.ANTHEM)
            effect.grantedKeywords.forEach { addAll(keywordTags(it)) }
            effect.ownedStaticAbilities.forEach { addAll(tagsOf(it)) }
        }

        // "Target creature gains 'When this creature dies, return it to the battlefield'" (Feign
        // Death, Undying Malice, earthbend's return): the creature outlives the removal aimed at it,
        // so the grant is protection, as a regeneration shield is.
        // Its "that player loses the game" handed to a creature (Vraska, Scheming Gorgon's −10)
        // is the win it threatens, as the same trigger printed on Phage is.
        // A trigger granted for good that serves the creature holding it is that creature's own
        // ability from then on, read as if printed: Cherished Hatchling's Dinosaurs gaining "when this
        // creature enters, you may have it fight another target creature" are fight removal. Only a
        // permanent grant — one until end of turn still waits on its event, so the HoldPolicy card-flow
        // windows must not see it as the payoff — and only a helpful one, which is ours to hold.
        is GrantTriggeredAbilityEffect -> buildSet {
            if (returnsItselfWhenItLeaves(effect.ability)) add(IntentTag.PROTECTION)
            val ability = effect.ability
            val granted = analyzeEffect(ability.effect, ability.allTargetRequirements).tags
            if (IntentTag.GAME_WIN in granted) add(IntentTag.GAME_WIN)
            val helpful = TargetPolarityAnalyzer.grantedTriggerPolarity(ability) == TargetPolarity.BENEFICIAL
            if (effect.duration == Duration.Permanent && helpful) addAll(granted)
            // "Target creature gains 'whenever this creature deals combat damage to a player, draw
            // a card' until end of turn" (Sygg, Wanderwine Wisdom; Commando Raid): a payoff for
            // this turn's attack connecting — a combat enhancement, like a keyword that pays off on
            // a hit. The granted effect itself waits on combat damage, so it is not read as a draw
            // the HoldPolicy card-flow windows could cash.
            else if (helpful && dealsCombatDamage(ability)) add(IntentTag.COMBAT_ENHANCEMENT)
        }

        // "That player loses the game" aimed away from its controller, or "you win the game": the
        // game itself. A loss its controller suffers is the card's price, not its point.
        is LoseGameEffect -> if (losesAnOpponent(effect.target)) setOf(IntentTag.GAME_WIN) else emptySet()
        is WinGameEffect -> if (effect.target == EffectTarget.Controller || effect.target == EffectTarget.PlayerRef(Player.You)) {
            setOf(IntentTag.GAME_WIN)
        } else emptySet()

        // A gate another player decides whose branches [readingLeaves] did not open as a punisher
        // ([punisherBranchLeaves]): a tax the decider can pay off, so the card does only what both
        // branches do — which, with one branch empty, is nothing.
        is GatedEffect -> {
            val otherwise = effect.otherwise
            if (effect.decisionMaker == null || otherwise == null) emptySet()
            else effectTags(effect.then) intersect effectTags(otherwise)
        }

        // "Choose one —": the card can do whatever any one mode does, so it carries every mode's
        // tags — a charm with a removal mode is removal in hand, as a ForEach body is read above.
        // A tag *also* answers "is this card dead here?" for exactly one consumer: a counterspell
        // with nothing on the stack is a discard ([HoldPolicy]). That is only true of a modal
        // spell when every mode counters, so [IntentTag.COUNTERSPELL] alone needs all of them.
        // A cast with its modes chosen is read off those modes instead — [analyzeModes].
        is ModalEffect -> {
            // Each mode reads against its own targets: "return target creature card from your
            // graveyard to your hand" is recursion only once its slot puts the card in a graveyard.
            val perMode = effect.modes.map { mode -> effectTags(mode.effect, mode.targetRequirements) }
            val union = perMode.flatMapTo(HashSet()) { it }
            if (perMode.all { IntentTag.COUNTERSPELL in it }) union else union - IntentTag.COUNTERSPELL
        }

        // "Choose up to five {P} worth of modes" (the Bloomburrow Seasons) can spend its budget on
        // any one mode, so it reads as the union of its modes exactly as "choose one" does.
        is BudgetModalEffect -> {
            val perMode = effect.modes.map { mode -> effectTags(mode.effect) }
            val union = perMode.flatMapTo(HashSet()) { it }
            if (perMode.all { IntentTag.COUNTERSPELL in it }) union else union - IntentTag.COUNTERSPELL
        }

        else -> emptySet()
    }

    /**
     * Whether [body], run over each permanent of [group], untaps lands of ours and nothing else's —
     * "untap all lands you control". Untapping a group that can hold creatures or an opponent's
     * lands is a combat or tempo play, not mana, and stays unread.
     */
    private fun untapsOurLands(group: GroupFilter, body: Effect): Boolean {
        val filter = group.baseFilter
        if (filter.anyOf.isNotEmpty() || CardPredicate.IsLand !in filter.cardPredicates) return false
        if (filter.controllerPredicate != ControllerPredicate.ControlledByYou) return false
        return EffectWalker.payoffLeaves(body).any { it is TapUntapEffect && !it.tap && it.target == EffectTarget.IterationEntity }
    }

    /**
     * Whether [body], run over each permanent of [group], untaps creatures of ours and nothing
     * else's — "untap all creatures you control". A symmetric untap (Intruder Alarm's "untap all
     * creatures") helps the opponent's board as much as ours and stays unread.
     */
    private fun untapsOurCreatures(effect: Effect): Boolean {
        val space = (effect as? ForEachEffect)?.space as? IterationSpace.Group ?: return false
        return untapsOurCreatures(space.filter, effect.body)
    }

    private fun untapsOurCreatures(group: GroupFilter, body: Effect): Boolean {
        val filter = group.baseFilter
        if (filter.anyOf.isNotEmpty() || CardPredicate.IsCreature !in filter.cardPredicates) return false
        if (filter.controllerPredicate != ControllerPredicate.ControlledByYou) return false
        return EffectWalker.payoffLeaves(body).any { it is TapUntapEffect && !it.tap && it.target == EffectTarget.IterationEntity }
    }

    /**
     * Whether resolving [card] as a spell costs its controller the game, unconditionally — Glorious
     * End's "at the beginning of your next end step, you lose the game". Only a plain sequence and
     * the delayed trigger it schedules are followed: a loss behind a payment or a condition (a Pact's
     * "if you don't [pay], you lose the game") is a price the AI can still choose to pay, and a
     * permanent's own trigger (Phage) is not what the cast does.
     */
    internal fun spellCostsItsControllerTheGame(card: CardDefinition): Boolean =
        card.script.spellEffect?.let(::losesItsControllerTheGame) ?: false

    /** Whether [card]'s spell is "end the turn" and nothing more — Time Stop, Discontinuity. */
    internal fun spellOnlyEndsTheTurn(card: CardDefinition): Boolean =
        card.script.spellEffect == EndTheTurnEffect

    private fun losesItsControllerTheGame(effect: Effect): Boolean = when (effect) {
        is CompositeEffect -> effect.effects.any(::losesItsControllerTheGame)
        is CreateDelayedTriggerEffect -> losesItsControllerTheGame(effect.effect)
        is LoseGameEffect -> effect.target == EffectTarget.Controller || effect.target == EffectTarget.PlayerRef(Player.You)
        else -> false
    }

    /**
     * Whether a cast ban on [target] is aimed at an opponent: each opponent, the defending player, or
     * a chosen target player (whom the polarity reader aims at an opponent). "You" and "each player"
     * bind the controller too, which makes the ban a price the card pays rather than what it is for.
     */
    internal fun bansAnOpponent(target: EffectTarget): Boolean = when (target) {
        is EffectTarget.ContextTarget, is EffectTarget.BoundVariable -> true
        is EffectTarget.PlayerRef -> target.player in OPPONENT_PLAYERS
        else -> false
    }

    /**
     * Whether a [LoseGameEffect] aimed at [target] costs an opponent the game: a chosen target
     * player, an opponent, or the player a trigger names ("whenever this creature deals damage to
     * an opponent, that player loses the game"). Its controller losing is a price, not a win.
     */
    private fun losesAnOpponent(target: EffectTarget): Boolean = when (target) {
        is EffectTarget.ContextTarget, is EffectTarget.BoundVariable -> true
        is EffectTarget.PlayerRef -> target.player in OPPONENT_PLAYERS || target.player == Player.TriggeringPlayer
        else -> false
    }

    /**
     * Whether [ability] is a trigger of the permanent itself leaving the battlefield that gets that
     * same card back — "when this creature dies, return it to the battlefield" (Feign Death) or
     * "… to its owner's hand" (Verdant Rebirth, Ashnod's Intervention). Either way the removal
     * aimed at it costs its controller nothing but tempo.
     */
    internal fun returnsItselfWhenItLeaves(ability: TriggeredAbility): Boolean {
        if (ability.binding != TriggerBinding.SELF) return false
        val pattern = ability.trigger as? EventPattern.ZoneChangeEvent ?: return false
        if (pattern.from != Zone.BATTLEFIELD) return false
        return EffectWalker.payoffLeaves(ability.effect).any {
            it is MoveToZoneEffect && it.target == EffectTarget.Self &&
                (it.destination == Zone.BATTLEFIELD || it.destination == Zone.HAND)
        }
    }

    /**
     * What an iteration over opponents' *own* gains reads as: nothing. "Each opponent may search
     * their library for a basic land card and put it onto the battlefield" (Old-Growth Dryads),
     * "each opponent draws a card" — the body's `you` is the opponent being visited, so a tutor,
     * ramp, draw or token it makes is theirs: a drawback of the card, never its point. What it
     * takes from them (a discard, a sacrifice, life) stays read.
     */
    private val OPPONENT_GIFTS: Set<IntentTag> = setOf(
        IntentTag.DRAW, IntentTag.IMPULSE, IntentTag.TUTOR, IntentTag.CARD_SELECTION, IntentTag.RAMP, IntentTag.LIFEGAIN,
        IntentTag.TOKEN_MAKER, IntentTag.RECURSION, IntentTag.PROTECTION, IntentTag.PUMP, IntentTag.ANTHEM,
    )

    /**
     * Whether [effect] iterates over opponents only and everything its body does is a gain for the
     * opponent visited — [OPPONENT_GIFTS] — so the whole node is a drawback to its controller
     * (Old-Growth Dryads). [com.wingedsheep.ai.engine.LimitedCardRater] prices it as one.
     */
    internal fun giftsOnlyToOpponents(effect: ForEachEffect): Boolean {
        val space = effect.space as? IterationSpace.Players ?: return false
        if (includesYou(space.players)) return false
        if (readingLeaves(effect.body).any(::giftsTheSourceController)) return false
        val inner = effectTags(effect.body)
        return inner.isNotEmpty() && inner.all { it in OPPONENT_GIFTS }
    }

    /** An opponents-only body's tags: the visited opponent's gains dropped, ours by name kept. */
    private fun opponentIterationTags(body: Effect, inner: Set<IntentTag>): Set<IntentTag> =
        (inner - OPPONENT_GIFTS) + readingLeaves(body).filter(::giftsTheSourceController).flatMap { tagsOf(it) }

    /**
     * Whether [leaf] names the source's controller as the one who gains — "for each opponent,
     * create a 1/1 token" under `ControllerOfSource` (Adeline, Resplendent Cathar), "you draw a
     * card" for each opponent who didn't discard (Garruk, Veiled Butcher). Inside an iteration
     * over players `you` is rebound to the player visited, so only this spelling reaches us.
     */
    private fun giftsTheSourceController(leaf: Effect): Boolean {
        val recipient = when (leaf) {
            is DrawCardsEffect -> leaf.target
            is GainLifeEffect -> leaf.target
            is CreateTokenEffect -> leaf.controller
            is CreatePredefinedTokenEffect -> leaf.controller
            else -> null
        }
        return recipient == EffectTarget.PlayerRef(Player.ControllerOfSource)
    }

    /**
     * Counters put on [target]: +1/+1 is a [IntentTag.PUMP] that never wears off, -1/-1 on someone
     * else's creature is removal by another name, and a keyword counter reads as the keyword does.
     *
     * A counter on the card's own source or on a player is left untagged, as the audit leaves it:
     * a creature growing itself has no target to choose and no window to time, and tagging a
     * self-grown flash creature [IntentTag.PUMP] would make [AmbushWindow] stop holding it.
     */
    private fun counterTags(type: CounterType, target: EffectTarget): Set<IntentTag> {
        if (!hitsAnotherPermanent(target)) return emptySet()
        type.ptDelta?.let { (power, toughness) ->
            return when {
                toughness < 0 && power <= 0 -> removalTags(target)
                power >= 0 && toughness >= 0 && (power > 0 || toughness > 0) -> setOf(IntentTag.PUMP)
                else -> emptySet()
            }
        }
        if (type == CounterType.SHIELD) return setOf(IntentTag.PROTECTION)
        // A marker whose only meaning is "this one gets destroyed" picks the victim of the card's
        // removal: marking it is the targeting half of that removal.
        if (type in MARKED_FOR_REMOVAL_COUNTERS) return setOf(IntentTag.REMOVAL)
        return keywordTags(type.name)
    }

    /**
     * What a predefined Aura token does to the object it is created attached to: what the same
     * Aura cast onto that object would ([staticTags] over its statics). A Cursed Role's base 1/1
     * shuts the creature off ([IntentTag.NEUTRALIZE]); every other Role is a lasting
     * [IntentTag.PUMP] (+1/+1 with trample, ward {1}, an attack scry or a drain when it leaves,
     * or Young Hero's +1/+1 counter on each attack); a Settlement hands its land a mana ability
     * ([IntentTag.RAMP], as a granted one reads).
     *
     * Named by token rather than read off its definition: this fold is memoized by card name with
     * no token registry, where [TargetPolarityAnalyzer] reads the Role through a [CardLookup]. The
     * tokens are a fixed family with rules-defined text, like Treasure's;
     * `AuraTokenReadingAiTest` holds this table to their definitions' own polarity, so an Aura
     * token added later without a line here fails there instead of reading as nothing.
     *
     * A token attached to the source itself is untagged, like a counter on it ([counterTags]):
     * Faunsbane Troll's Monster Role is self-growth with no target to choose, Cursed Courtier's
     * Cursed Role its drawback.
     */
    private fun auraTokenTags(effect: CreateAuraTokenEffect): Set<IntentTag> {
        if (!hitsAnotherPermanent(effect.target)) return emptySet()
        return when (effect.tokenName) {
            CURSED_ROLE -> setOf(IntentTag.NEUTRALIZE)
            "Monster Role", "Royal Role", "Sorcerer Role", "Wicked Role", "Young Hero Role" -> setOf(IntentTag.PUMP)
            "Settlement" -> setOf(IntentTag.RAMP)
            else -> emptySet()
        }
    }

    private const val CURSED_ROLE = "Cursed Role"

    /**
     * A Cursed Role put on a creature only our side can fill (Spiteful Hexmage's "target creature
     * you control"): the card's price, no answer — the Aura-token twin of [harmsOwnTarget].
     */
    internal fun auraTokenHarmsOwnTarget(effect: CreateAuraTokenEffect, requirements: List<TargetRequirement>): Boolean =
        effect.tokenName == CURSED_ROLE && TargetPolarityAnalyzer.targetsOwnSide(effect.target, requirements)

    /** Taking a P/T-lowering counter off another permanent gives its stats back: a pump. */
    private fun removedCounterTags(type: CounterType, target: EffectTarget): Set<IntentTag> {
        if (!hitsAnotherPermanent(target)) return emptySet()
        val (power, toughness) = type.ptDelta ?: return emptySet()
        return if (power <= 0 && toughness <= 0 && (power < 0 || toughness < 0)) setOf(IntentTag.PUMP) else emptySet()
    }

    /**
     * Counter kinds with no rule of their own that the cards placing them destroy their holders by
     * (Tetzimoc, Primal Death's prey counters). Harmful to their holder; placing one is removal.
     */
    val MARKED_FOR_REMOVAL_COUNTERS: Set<CounterType> = setOf(CounterType.PREY)

    private fun canMillOpponent(player: Player): Boolean = when (player) {
        Player.EachOpponent, Player.TargetOpponent, Player.TargetPlayer,
        Player.EachTargetedPlayer, is Player.ContextPlayer, is Player.BoundVariable -> true
        else -> false
    }

    /**
     * A base P/T change to fixed values of 1 or less, or to no power and at most 2 toughness — see
     * [IntentTag.NEUTRALIZE]; the polarity reader's `baseStatsPolarity` draws the same line.
     */
    private fun setsSmallBase(effect: SetBaseStatsEffect): Boolean {
        val set = listOfNotNull(effect.power, effect.toughness)
        val small = set.isNotEmpty() && set.all { amount -> fixed(amount)?.let { it <= 1 } == true }
        val toothless = effect.power?.let(::fixed) == 0 && effect.toughness?.let(::fixed)?.let { it <= 2 } == true
        return small || toothless
    }

    /** Whether [target] is bound to a "target opponent" requirement. */
    private fun namesAnOpponentSlot(target: EffectTarget, requirements: List<TargetRequirement>): Boolean =
        slotOf(target, requirements) is TargetOpponent

    /**
     * Whether damage aimed at [target] lands on a player — a named one ("each opponent", "that
     * player", "you") or a slot only a player fills ("target player", "target opponent"). Damage
     * to a player is a clock, not an answer to a permanent; the rater prices it so too.
     */
    fun damagesAPlayer(target: EffectTarget, requirements: List<TargetRequirement>): Boolean =
        namesAPlayer(target) || slotOf(target, requirements).let { it is TargetPlayer || it is TargetOpponent }

    private fun namesAPlayer(target: EffectTarget): Boolean = when (target) {
        is EffectTarget.PlayerRef, is EffectTarget.ControllerOfPipelineTarget, EffectTarget.Controller, EffectTarget.TargetController,
        EffectTarget.ControllerOfTriggeringEntity, EffectTarget.ControllerOfDamageSource -> true
        else -> false
    }

    /** The requirement [target] is bound to, or null when it names no declared slot. */
    private fun slotOf(target: EffectTarget, requirements: List<TargetRequirement>): TargetRequirement? = when (target) {
        is EffectTarget.BoundVariable -> requirements.firstOrNull { it.id == target.name.substringBefore('[') }
        is EffectTarget.ContextTarget -> requirements.getOrNull(target.index)
        else -> null
    }

    /**
     * Whether [effect] sets its controller's own life total to a fixed number above the starting 20
     * (Providence's 26) — the gain [leafTagsOf] reads it as.
     */
    private fun setsOwnLifeAboveStart(effect: SetLifeTotalEffect): Boolean {
        val ours = effect.target == EffectTarget.Controller || effect.target == EffectTarget.PlayerRef(Player.You)
        val amount = effect.amount as? DynamicAmount.Fixed ?: return false
        return ours && amount.amount > STARTING_LIFE
    }

    /**
     * Whether an extra turn aimed at [target] goes to its controller. A player slot is theirs to aim
     * (Time Warp's "target player"), and one only an opponent can fill was dropped in [readingLeaves].
     */
    private fun takesTheTurnOurselves(target: EffectTarget): Boolean =
        target == EffectTarget.Controller || target == EffectTarget.PlayerRef(Player.You) ||
            target is EffectTarget.ContextTarget || target is EffectTarget.BoundVariable

    /** Spot removal or a wrath, depending on whether the effect names a group. */
    /**
     * "Sacrifice it" aimed at the permanent this card is attached to — Thirsting Axe's equipped
     * creature, Breath of Fury's enchanted creature, Stitcher's Graft's host as it becomes unattached.
     * Its controller sacrifices it, and a player can sacrifice only their own permanents (an
     * opponent's host is left alone), so it is the card's drawback or its price, never an answer.
     * [com.wingedsheep.ai.audit.AiReadingAudit] reads the same leaf as needing no intent.
     */
    fun sacrificesOwnAttachedHost(effect: SacrificeTargetEffect): Boolean =
        !effect.sacrificedByItsController && effect.target in ATTACHED_HOSTS

    private val ATTACHED_HOSTS: Set<EffectTarget> = setOf(
        EffectTarget.EquippedCreature,
        EffectTarget.EnchantedCreature,
        EffectTarget.EnchantedPermanent,
        EffectTarget.AttachedToTriggeringPermanent,
    )

    private fun removalTags(target: EffectTarget): Set<IntentTag> =
        if (targetsAGroup(target)) setOf(IntentTag.REMOVAL, IntentTag.SWEEPER)
        else setOf(IntentTag.REMOVAL)

    private fun targetsAGroup(target: EffectTarget): Boolean = target is EffectTarget.GroupRef

    /**
     * Whether an effect aimed at [target] lands on a permanent that is not the card itself — the
     * precondition for calling anything "removal".
     *
     * Two exclusions. A **player** target is a clock, not an answer. And **[EffectTarget.Self]** is
     * the card sacrificing or exiling itself — so "put this into your graveyard" is a sacrifice
     * clause, not removal, while Wrath of God's per-creature "destroy it"
     * ([EffectTarget.IterationEntity]) is.
     */
    private fun hitsAnotherPermanent(target: EffectTarget): Boolean = when (target) {
        // "… deals 2 damage to enchanted artifact's controller" (Gremlin Infestation): a player.
        is EffectTarget.PlayerRef, is EffectTarget.ControllerOfPipelineTarget, EffectTarget.Controller, EffectTarget.TargetController,
        EffectTarget.ControllerOfTriggeringEntity, EffectTarget.ControllerOfDamageSource,
        EffectTarget.Self -> false
        else -> true
    }

    private fun keywordTags(keyword: String): Set<IntentTag> {
        // A keyword string also carries an [AbilityFlag]'s name: "can't be blocked" is the purest
        // evasion there is, and "can't be blocked by more than one creature" is menace's cousin.
        if (keyword in EVASION_FLAGS) return setOf(IntentTag.EVASION_GRANT)
        if (keyword in COMBAT_FLAGS) return setOf(IntentTag.COMBAT_ENHANCEMENT)
        // "Target creature gains protection from red" (Crimson Acolyte, Sygg's "protection from
        // each color"): `Effects.GrantProtectionFromColor` grants it as a `PROTECTION_FROM_<X>`
        // string, the keyword's scoped form — the same shield as [Keyword.PROTECTION].
        if (keyword.startsWith(PROTECTION_FROM)) return setOf(IntentTag.PROTECTION)
        val parsed = runCatching { Keyword.valueOf(keyword.uppercase()) }.getOrNull()
            ?: return emptySet()
        return when (parsed) {
            in EVASION_KEYWORDS -> setOf(IntentTag.EVASION_GRANT)
            in PROTECTIVE_KEYWORDS -> setOf(IntentTag.PROTECTION)
            in COMBAT_KEYWORDS -> setOf(IntentTag.COMBAT_ENHANCEMENT)
            else -> emptySet()
        }
    }

    /** The largest creature this effect answers by damage or shrinking, if it answers by size. */
    private fun reachOf(effect: Effect): Int? = when (effect) {
        // Face damage answers no creature, so it contributes no reach — same exclusion as the tag.
        is DealDamageEffect ->
            if (hitsAnotherPermanent(effect.target)) fixed(effect.amount) else null

        // A dynamic total (Huatli's X, Ureni's land count) is only known on the board, so it
        // claims no fixed reach — not the placeholder 0 its `totalDamage` carries.
        is DividedDamageEffect -> effect.dynamicTotal?.let(::fixed) ?: effect.totalDamage.takeIf { effect.dynamicTotal == null }
        is ModifyStatsEffect -> fixed(effect.toughnessModifier)?.takeIf { it < 0 }?.let { -it }
        is AddCountersEffect ->
            effect.counterType.ptDelta?.second?.takeIf { it < 0 && hitsAnotherPermanent(effect.target) }?.let { -it * effect.count }
        // The largest creature any one mode answers, the union [tagsOf] reads for the tags.
        is ModalEffect -> effect.modes.flatMap { readingLeaves(it.effect) }.mapNotNull { reachOf(it) }.maxOrNull()
        is BudgetModalEffect -> effect.modes.flatMap { readingLeaves(it.effect) }.mapNotNull { reachOf(it) }.maxOrNull()
        else -> null
    }

    /** How much toughness an [isExpiringPump] effect grants. Zero for a power-only trick. */
    private fun expiringToughnessOf(effect: Effect): Int =
        ((effect as? ModifyStatsEffect)?.let { fixed(it.toughnessModifier) } ?: 0).coerceAtLeast(0)

    /** A stat boost that goes away at cleanup — the thing a combat trick is made of. */
    private fun isExpiringPump(effect: Effect): Boolean {
        if (effect !is ModifyStatsEffect) return false
        if (effect.duration != Duration.EndOfTurn) return false
        return raises(effect.powerModifier) || raises(effect.toughnessModifier)
    }

    /**
     * One permanent turned into a copy until end of turn — "until end of turn, it becomes a copy of
     * up to one other target creature" (Fleeting Reflection). Like [isExpiringPump] it is a body for
     * this turn's fight, so an instant carrying one is a combat trick, with no toughness of its own
     * to count. The group form ("each Shapeshifter you control becomes a copy", Absorb Identity)
     * rides on whatever else the spell does.
     */
    private fun isExpiringCopy(effect: Effect): Boolean =
        effect is EachPermanentBecomesCopyOfTargetEffect && effect.duration == Duration.EndOfTurn &&
            effect.affected != null

    /**
     * Another creature given a base power and toughness until end of turn that *replaces* its stats
     * for a fight — "up to two target creatures each have base power and toughness 3/3 and gain
     * flying until end of turn" (Phantasmal Form), alone or run once per target through a `ForEach`.
     * The [IntentTag.COMBAT_ENHANCEMENT] reading of [tagsOf], with a duration: like [isExpiringCopy]
     * it is spent in this turn's fight, so an instant carrying one is a combat trick. A base of 0
     * toughness (removal) or one small enough to neutralize is not this shape.
     */
    private fun isExpiringBaseStats(effect: Effect): Boolean = when (effect) {
        is SetBaseStatsEffect -> effect.duration == Duration.EndOfTurn && hitsAnotherPermanent(effect.target) &&
            effect.toughness?.let(::fixed) != 0 && !setsSmallBase(effect)
        is ForEachEffect -> effect.space == IterationSpace.Targets && readingLeaves(effect.body).any(::isExpiringBaseStats)
        else -> false
    }

    /** The fixed toughness an [isExpiringBaseStats] effect sets, or null when it sets none or not a number. */
    private fun expiringBaseToughnessOf(effect: Effect): Int? = when {
        !isExpiringBaseStats(effect) -> null
        effect is SetBaseStatsEffect -> effect.toughness?.let(::fixed)
        effect is ForEachEffect -> readingLeaves(effect.body).mapNotNull(::expiringBaseToughnessOf).maxOrNull()
        else -> null
    }

    /**
     * A protective keyword granted until end of turn — "target creature gains indestructible until
     * end of turn". Like [isExpiringPump] it buys something only before cleanup, so an instant
     * carrying one is held for a fight or a response ([IntentTag.COMBAT_TRICK]), with no toughness.
     */
    private fun isExpiringProtection(effect: Effect): Boolean =
        effect is GrantKeywordEffect && effect.duration == Duration.EndOfTurn &&
            IntentTag.PROTECTION in keywordTags(effect.keyword)

    /**
     * An ability whose payoff ends at cleanup: an [isExpiringProtection] grant (one creature's or,
     * through a group `ForEach`, the team's), and every other leaf is one too — another expiring
     * keyword or pump, or a rider spent on resolution (a scry, a little life). Anything else may
     * last — Snakeskin Veil's +1/+1 counter, Oblivion's Hunger's draw, Royal Treatment's Role — and
     * a spell with a lasting payoff is worth casting whenever, so it is no trick to hold.
     */
    private fun isExpiringProtectionTrick(root: Effect): Boolean {
        val leaves = readingLeaves(root).flatMap { if (it is ForEachEffect) readingLeaves(it.body) else listOf(it) }
        return leaves.any(::isExpiringProtection) && leaves.all { leaf ->
            isExpiringPump(leaf) ||
                (leaf is GrantKeywordEffect && leaf.duration == Duration.EndOfTurn) ||
                tagsOf(leaf).let { it.isNotEmpty() && it.all { tag -> tag in SPENT_ON_RESOLUTION } }
        }
    }

    /**
     * Whether a stat modifier only ever adds. A dynamic "+X" (a count, a mana value) counts: reading
     * only [DynamicAmount.Fixed] left Accelerated Mutation's +X/+X out of [IntentTag.COMBAT_TRICK].
     */
    private fun raises(amount: DynamicAmount): Boolean = AmountSign.of(amount) == AmountSign.GAIN

    /**
     * A dynamic stat grant that can only take away: neither amount can raise, and one lowers
     * ("gets -1/-1 for each card in your hand", "-X/-0"). On [Scope.Self] it is the creature's own
     * printed drawback, read by the evaluator through projected P/T — shared with the reading audit.
     */
    internal fun isStatPenalty(static: GrantDynamicStats): Boolean {
        val power = AmountSign.of(static.powerBonus)
        val toughness = AmountSign.of(static.toughnessBonus)
        return power != AmountSign.GAIN && toughness != AmountSign.GAIN &&
            (power == AmountSign.LOSS || toughness == AmountSign.LOSS)
    }

    /**
     * Whether [gather] is a player searching their own zones for themselves: the searcher owns
     * the zones searched, and is us or a player the card names — "target player searches their
     * library for a basic land" (Restorative Technique, the same tutor Fertilid's Favor spells as
     * a `ForEachPlayer`), "that player searches their library for a card" (Mornsong Aria).
     * A searcher other than the zones' owner is rummaging through someone else's: inside a
     * `ForEachPlayer` body [Player.You] is the iterated player, and Invasive Surgery's
     * `searcher = ControllerOfSource` is its caster searching the countered spell's controller's
     * zones — disruption, whoever's spell it was. A search handed back as the price of what the
     * card took from that player (Settle the Wreckage) is read in [refundSearches].
     */
    private fun searchesOwnZones(gather: GatherCardsEffect, owner: Player): Boolean =
        gather.searcher == owner && owner in SELF_SEARCHERS

    private val SELF_SEARCHERS = setOf(Player.You, Player.TargetPlayer, Player.TriggeringPlayer)

    /**
     * "Exile all attacking creatures target player controls. That player may search their library
     * for that many basic land cards" (Settle the Wreckage): a search for another player whose
     * size is the count of cards the same effect exiled or destroyed is that player's refund —
     * the answer's price, as [handRefundDraws] reads Lost Legacy's draws — not our tutor.
     */
    internal fun refundSearches(root: Effect): List<GatherCardsEffect> {
        val leaves = readingLeaves(root)
        val removed = leaves.mapNotNullTo(HashSet()) { leaf ->
            (leaf as? MoveCollectionEffect)?.from?.takeIf {
                (leaf.destination as? CardDestination.ToZone)?.zone.let { it == Zone.EXILE || it == Zone.GRAVEYARD }
            }
        }
        if (removed.isEmpty()) return emptyList()
        fun sizedByRemoved(amount: DynamicAmount): Boolean = when (amount) {
            is DynamicAmount.DistinctEntitiesInCollections -> amount.collections.any { it in removed }
            is DynamicAmount.VariableReference -> amount.variableName.removeSuffix("_count") in removed
            else -> false
        }
        return leaves.filterIsInstance<GatherCardsEffect>().filter { gather ->
            gather.searcher != Player.You && leaves.any { leaf ->
                leaf is SelectFromCollectionEffect && leaf.from == gather.storeAs &&
                    (leaf.selection as? SelectionMode.ChooseUpTo)?.count?.let(::sizedByRemoved) == true
            }
        }
    }

    /**
     * A group `ForEach` whose body pumps each member until end of turn — the lowered form of
     * "creatures you control get +2/+1 until end of turn". Only the group's own members are
     * pumped ([EffectTarget.IterationEntity]); a body aimed anywhere else is not this shape.
     *
     * A team grant of a keyword that only changes a fight — "creatures you control gain trample
     * until end of turn" (Waxing Moon), first strike, double strike, deathtouch — is the same trick
     * without the stats (`Patterns.Group.grantKeywordToAll` lowers it the same way): spent in this
     * turn's combat, or not at all.
     */
    private fun isExpiringGroupPump(effect: Effect): Boolean =
        effect is ForEachEffect && effect.space is IterationSpace.Group &&
            readingLeaves(effect.body).any { leaf ->
                when (leaf) {
                    is ModifyStatsEffect -> isExpiringPump(leaf) && leaf.target == EffectTarget.IterationEntity
                    is GrantKeywordEffect -> leaf.duration == Duration.EndOfTurn &&
                        leaf.target == EffectTarget.IterationEntity && leaf.keyword in FIGHT_ONLY_KEYWORDS
                    else -> false
                }
            }

    /** Keywords whose whole payoff is a combat they change — see [isExpiringGroupPump]. */
    private val FIGHT_ONLY_KEYWORDS = setOf(
        Keyword.TRAMPLE, Keyword.FIRST_STRIKE, Keyword.DOUBLE_STRIKE, Keyword.DEATHTOUCH,
    ).map { it.name }.toSet()

    /**
     * An activated ability that pays for its draw with as many discarded cards ("{2}, {T}, Discard a
     * card: Draw a card", Murmuring Volume; Solitary Cell's "discard a legendary card"): card for
     * card, a filter of the hand, never card advantage.
     */
    private fun isRummage(ability: ActivatedAbility): Boolean {
        val discarded = discardedBy(ability.cost)
        if (discarded == 0) return false
        val draws = EffectWalker.leaves(ability.effect).filterIsInstance<DrawCardsEffect>()
        if (draws.isEmpty()) return false
        val drawn = draws.sumOf { fixed(it.count) ?: return false }
        return drawn <= discarded
    }

    private fun discardedBy(cost: AbilityCost): Int = when (cost) {
        is AbilityCost.Atom -> (cost.atom as? CostAtom.Discard)?.count ?: 0
        is AbilityCost.Composite -> cost.costs.sumOf { discardedBy(it) }
        else -> 0
    }

    private fun asRummage(tags: Set<IntentTag>): Set<IntentTag> =
        if (IntentTag.DRAW in tags) tags - IntentTag.DRAW + IntentTag.CARD_SELECTION else tags

    private fun drawsOf(effect: Effect): Int? = when (effect) {
        is DrawCardsEffect -> fixed(effect.count)
        is DrawUpToEffect -> effect.maxCards
        else -> null
    }

    private fun fixed(amount: DynamicAmount): Int? = (amount as? DynamicAmount.Fixed)?.amount

    // ═════════════════════════════════════════════════════════════════════════
    // Static abilities
    // ═════════════════════════════════════════════════════════════════════════

    /** An Aura's "enchanted permanent doesn't untap" — see [analyze]. */
    private fun locksEnchantedTapped(static: StaticAbility): Boolean =
        static is GrantKeyword && static.keyword in LOCKED_TAPPED_FLAGS && static.filter.scope == Scope.AttachedTo

    /** The tags one static ability reads as, for [com.wingedsheep.ai.engine.LimitedCardRater]. */
    internal fun staticTags(static: StaticAbility): Set<IntentTag> = tagsOf(static)

    private fun tagsOf(static: StaticAbility): Set<IntentTag> = when (static) {
        is ConditionalStaticAbility -> tagsOf(static.ability)
        // "You can't lose the game and your opponents can't win the game" (Platinum Angel, Gideon of
        // the Trials' emblem): a shield over its controller rather than any permanent — the game
        // can't be taken from them while it holds.
        is GrantCantLoseGame, is GrantOpponentsCantWinGame -> setOf(IntentTag.PROTECTION)
        is CanBlockAdditionalForCreatureGroup ->
            if (static.count > 0) setOf(IntentTag.EXTRA_BLOCKING) else emptySet()
        is CantBeBlocked -> setOf(IntentTag.EVASION_GRANT)
        // "Your opponents can't untap more than two permanents during their untap steps" (Dovin
        // Baan's emblem): keeps the opponents' permanents tapped — the tapper's payoff, as a
        // "doesn't untap" lock reads. A symmetric cap (Damping Field, Winter Orb) binds its
        // controller too, so it reads as no plan of its own.
        is UntapLimitPerStep -> if (static.players in OPPONENT_PLAYERS) setOf(IntentTag.TAPPER) else emptySet()
        // "Can't be blocked by artifact creatures" (Audacious Infiltrator, Fen Hauler): partial
        // evasion is still evasion — the creature gets through whatever the filter shuts out.
        is CantBeBlockedBy -> setOf(IntentTag.EVASION_GRANT)
        // "Can't be blocked by more than one creature" (Sinuous Predator, Norwood Riders): no gang
        // block can bring it down, so it gets through at its own size — partial evasion too.
        is CantBeBlockedByMoreThan -> setOf(IntentTag.EVASION_GRANT)
        is ModifyStats -> when {
            // A creature's own printed penalty ("this creature gets -2/-2 as long as …", the Djinn
            // of Invasion) is its drawback, never a pump or an answer.
            static.filter.scope == Scope.Self && static.powerBonus <= 0 && static.toughnessBonus <= 0 -> emptySet()
            // An Aura's "enchanted creature gets -2/-2" (Dead Weight) answers what it enchants; a
            // power-only cut (Aether Meltdown's -4/-0) blunts it in every fight without answering
            // it — the lasting form of the one-shot "-2/-0" [leafTagsOf] reads the same way.
            static.filter.scope == Scope.AttachedTo && static.powerBonus <= 0 && static.toughnessBonus <= 0 -> when {
                static.toughnessBonus < 0 -> setOf(IntentTag.REMOVAL)
                static.powerBonus < 0 -> setOf(IntentTag.COMBAT_ENHANCEMENT)
                else -> emptySet()
            }
            static.filter.scope != Scope.Battlefield -> setOf(IntentTag.PUMP)
            static.powerBonus > 0 || static.toughnessBonus > 0 -> setOf(IntentTag.ANTHEM)
            static.powerBonus < 0 || static.toughnessBonus < 0 -> setOf(IntentTag.REMOVAL)
            else -> emptySet()
        }

        // The dynamic "+X/+X" reads as [ModifyStats] does, by the sign of each amount ([AmountSign]):
        // only a grant that cannot take anything away is a pump or an anthem. "This creature gets
        // -1/-1 for each card in your hand" (Grim Strider, Geralf's Masterpiece) is its drawback; an
        // Aura's "-X/-X for each …" (Exotic Curse) answers what it enchants, its "-X/-0" blunts it;
        // "creatures your opponents control get -1/-1 for each …" (Toxrill) is a sweeper.
        is GrantDynamicStats -> {
            val toughness = AmountSign.of(static.toughnessBonus)
            val penalty = isStatPenalty(static)
            when {
                penalty && static.filter.scope == Scope.Self -> emptySet()
                penalty -> when {
                    toughness == AmountSign.LOSS -> setOf(IntentTag.REMOVAL)
                    static.filter.scope == Scope.AttachedTo -> setOf(IntentTag.COMBAT_ENHANCEMENT)
                    else -> emptySet()
                }
                static.filter.scope == Scope.Battlefield -> setOf(IntentTag.ANTHEM)
                else -> setOf(IntentTag.PUMP)
            }
        }

        is GrantKeyword -> keywordTags(static.keyword)
        // "Enchanted creature has 'Whenever this creature deals combat damage to a player, you may
        // draw a card'" (Sixth Sense): a trigger handed to the host for good that serves whoever
        // controls it is that creature's own ability from then on — read as if printed, as the
        // one-shot [GrantTriggeredAbilityEffect] is. A harmful grant (a curse's upkeep drain) is
        // read by the Aura's polarity instead, and tags nothing here.
        is GrantTriggeredAbility ->
            if (static.filter.scope == Scope.AttachedTo &&
                TargetPolarityAnalyzer.grantedTriggerPolarity(static.ability) == TargetPolarity.BENEFICIAL
            ) analyzeEffect(static.ability.effect, static.ability.allTargetRequirements).tags
            else emptySet()
        // "Enchanted creature has '{T}, Discard a card: Draw a card'" (Prophetic Ravings), an
        // Equipment's granted activation: the static form of [GrantActivatedAbilityEffect] — a mana
        // ability is a mana source, any other helpful ability is the host's own from then on, read
        // as if printed (a rummage stays card selection). A harmful grant, or any grant beside a curse
        // (skipped by the caller), is the Aura's polarity's to read, and tags nothing here.
        is GrantActivatedAbility -> when {
            static.filter.scope != Scope.AttachedTo -> emptySet()
            static.ability.isManaAbility -> setOf(IntentTag.RAMP)
            TargetPolarityAnalyzer.grantedActivatedPolarity(static.ability) == TargetPolarity.BENEFICIAL ->
                analyzeEffect(static.ability.effect, static.ability.targetRequirements).tags
                    .let { if (isRummage(static.ability)) asRummage(it) else it }
            else -> emptySet()
        }
        is CantAttack -> neutralizeTags(static.filter)
        is CantBlock -> if (static.filter.scope == Scope.Self) setOf(IntentTag.COMBAT_RESTRICTION)
            else neutralizeTags(static.filter)
        is CantAttackUnlessCoAttacker -> if (static.filter.scope == Scope.Self) setOf(IntentTag.COMBAT_RESTRICTION) else emptySet()
        // "This creature can't attack unless you control an artifact" (Desperate Castaways, Gadrak):
        // a condition on its own attacks — the same drawback on the body as needing a co-attacker.
        is CantAttackUnless -> if (static.filter.scope == Scope.Self) setOf(IntentTag.COMBAT_RESTRICTION) else emptySet()
        is CantBlockUnlessCoBlocker -> if (static.filter.scope == Scope.Self) setOf(IntentTag.COMBAT_RESTRICTION) else emptySet()
        // "This creature can't block unless you control another Wolf or Werewolf" (Howlpack Wolf):
        // the blocking half of [CantAttackUnless] — a condition on its own blocks, a drawback.
        is CantBlockUnless -> if (static.filter.scope == Scope.Self) setOf(IntentTag.COMBAT_RESTRICTION) else emptySet()
        // "This creature can't block artifact creatures" (Hinterland Drake), "can block only
        // creatures with flying" (Cloud Sprite): a narrower "can't block" on its own body.
        is CanOnlyBlockCreaturesWith -> if (static.filter.scope == Scope.Self) setOf(IntentTag.COMBAT_RESTRICTION) else emptySet()
        // "This creature attacks or blocks each combat if able" (Relentless Raptor, Juggernaut) binds
        // its own combat choices the way "can't block" does: a drawback on the body, not a payoff.
        is MustAttack -> if (static.filter.scope == Scope.Self) setOf(IntentTag.COMBAT_RESTRICTION) else emptySet()
        is MustBlock -> if (static.filter.scope == Scope.Self) setOf(IntentTag.COMBAT_RESTRICTION) else emptySet()
        // "This creature must be blocked if able" (Gaea's Protector), "equipped creature … must be
        // blocked by an Eldrazi if able" (Slayer's Cleaver), "All Walls able to block this creature
        // do so" (Marble Priest): the lasting form of the one-shot [MustBeBlockedEffect] — it pulls
        // blockers where we want them. A battlefield-wide lure binds both teams' attackers alike.
        is MustBeBlocked ->
            if (static.filter?.scope == Scope.Battlefield) emptySet() else setOf(IntentTag.COMBAT_ENHANCEMENT)
        // "Spells your opponents cast that target a Merfolk you control cost {2} more" and its
        // activated-ability twin are a ward-like tax on answering our permanents (Kopala, Warden
        // of Waves; Sphinx of New Prahv) — Ward is a [PROTECTIVE_KEYWORDS] member, so the same tag.
        is ModifySpellCost -> when {
            static.target is SpellCostTarget.OpponentsCastTargeting && taxes(static.modification) ->
                setOf(IntentTag.PROTECTION)
            // "Equipment spells you cast cost {1} less": a discount on our own spells only.
            static.target is SpellCostTarget.YouCast && !taxes(static.modification) ->
                setOf(IntentTag.COST_REDUCER)
            // "Noncreature spells your opponents cast cost {1} more" (Thalia, God-Pharaoh's Statue):
            // every spell of theirs that matches is slower, which no board simulation sees.
            static.target is SpellCostTarget.OpponentsCast && taxes(static.modification) ->
                setOf(IntentTag.SPELL_TAX)
            // "Creature spells you cast cost {1} more" (Geist-Fueled Scarecrow, the Leeches): the
            // price of the body, a drawback on our own spells — never disruption, never a discount.
            ownSpellsOnly(static.target) && taxes(static.modification) -> setOf(IntentTag.OWN_SPELL_TAX)
            else -> emptySet()
        }
        // "You may cast spells from your hand without paying their mana costs" (Omniscience,
        // Omnipresence, Fires of Invention): a free spell every time, ours alone. A symmetric grant
        // (Weftwalking's "each player") pays the opponent as well and earns no tag.
        is MayCastWithoutPayingManaCost ->
            if (static.controllerOnly) setOf(IntentTag.FREE_SPELL) else emptySet()
        // "Each creature you control with toughness greater than its power assigns combat damage
        // equal to its toughness" (Ghalta the Immovable, Arcades) and "can attack as though it
        // didn't have defender": better in a fight without a point of +N/+N. Doran's every-creature
        // form helps the opponent's walls just as much and earns no tag.
        is AssignDamageEqualToToughness ->
            if (onlyOurCreatures(static.filter)) setOf(IntentTag.COMBAT_ENHANCEMENT) else emptySet()
        is CanAttackDespiteDefender ->
            if (onlyOurCreatures(static.filter)) setOf(IntentTag.COMBAT_ENHANCEMENT) else emptySet()
        is GrantKeywordToOwnSpells -> ownSpellKeywordTags(static.keyword)
        // "Activated abilities of Equipment you control that target this creature cost {1} less",
        // "Abilities you activate cost {1} less": every printed reduction discounts the
        // controller's own permanents or activations, so only an opponents' reach is excluded.
        is ReduceActivatedAbilityCost ->
            if (static.reach != ActivatedAbilityReach.ACTIVATED_BY_OPPONENTS) setOf(IntentTag.COST_REDUCER) else emptySet()
        is IncreaseActivatedAbilityCost ->
            if (static.targeting != null && static.reach == ActivatedAbilityReach.ACTIVATED_BY_OPPONENTS) {
                setOf(IntentTag.PROTECTION)
            } else emptySet()
        // "Creatures entering the battlefield don't cause abilities to trigger" (Torpor Orb, Tocatli
        // Honor Guard, Hushbringer) and its opponents-only form (Elesh Norn, Mother of Machines).
        is GrantChosenSubtype -> if (static.filter == GroupFilter.AllCreaturesYouControl)
            setOf(IntentTag.TRIBAL_ENABLER) else emptySet()
        is SuppressZoneChangeTriggers -> setOf(IntentTag.TRIGGER_LOCK)
        // "Players can't get counters" (Solemnity) and a battlefield-wide "can't have counters put
        // on them" lock counters out; an Aura's or the creature's own (Blossombind) is read by
        // whatever else the card does to what it enchants.
        is PlayersCantGetCounters -> setOf(IntentTag.COUNTER_LOCK)
        // "Your opponents can't cast spells with the chosen name" (Gideon's Intervention), "…during
        // your turn" (Dragonlord Dromoka): the standing form of a cast ban. A ban that binds us too
        // ("each player") is a symmetric price; an Aura's on its enchanted player is the polarity
        // reader's question.
        is PlayersCantCastSpells -> if (static.affected in OPPONENT_PLAYERS) setOf(IntentTag.CAST_BAN) else emptySet()
        // "As you cascade, you may put a land card from among the exiled cards onto the battlefield
        // tapped" (Averna, the Chaos Bloom): every cascade we resolve also drops a land — an extra
        // land onto our battlefield, as a land-only put from hand reads. Another filter would put a
        // permanent that skipped its mana cost.
        is CascadeMayPutOntoBattlefield ->
            setOf(if (isLandOnly(static.filter)) IntentTag.RAMP else IntentTag.FREE_SPELL)
        is CantReceiveCounters ->
            if (static.filter.scope == Scope.Battlefield) setOf(IntentTag.COUNTER_LOCK) else emptySet()
        // "You may cast spells that have cycling from your graveyard" (Abandoned Sarcophagus): a
        // standing permission to cast cards back out of our graveyard is recursion, every turn.
        is MayCastFromGraveyard -> setOf(IntentTag.RECURSION)
        // "You may cast this card from your graveyard or from exile" (Squee, the Immortal; Eternal
        // Scourge): the card comes back to be cast again after it dies or is exiled — recursion of itself.
        is MayCastSelfFromZones -> setOf(IntentTag.RECURSION)
        // "Enchanted creature loses all abilities" (Lignify) and "creatures enchanted player
        // controls lose all abilities and have base power and toughness 1/1" (Overwhelming
        // Splendor) leave the creatures on the battlefield, no longer threats worth the name.
        is LoseAllAbilities -> neutralizeTags(static.filter)
        is SetBasePowerToughnessStatic ->
            if (static.shrinksToAtMostOneOne()) neutralizeTags(static.filter) else emptySet()
        // A composite static is each of its parts (a conditional one is read above, by the
        // ability it gates).
        is CompositeStaticAbility -> static.abilities.flatMapTo(HashSet()) { tagsOf(it) }
        else -> emptySet()
    }

    /**
     * A static that reaches only creatures on our side: the source itself, what it is attached to
     * (an Aura's or Equipment's host — who that is the polarity reader's question), or a battlefield
     * group restricted to creatures we control.
     */
    private fun onlyOurCreatures(filter: GroupFilter): Boolean = when (filter.scope) {
        Scope.Self, Scope.AttachedTo -> true
        Scope.Battlefield -> filter.baseFilter.controllerPredicate == ControllerPredicate.ControlledByYou
        else -> false
    }

    /**
     * A keyword our own spells are granted ("creature spells you cast have convoke", "instant and
     * sorcery spells you control have split second") read for what it does to the spell:
     * - convoke, improvise and delve pay for the spell with something other than mana — a discount;
     * - storm, casualty and conspire copy the spell — a second helping of it;
     * - split second stops the opponent answering it — protection for the spell on the stack;
     * - lifelink gains life off the spell's damage.
     * Anything else (a spell's deathtouch, cascade) is left unread.
     */
    private fun ownSpellKeywordTags(keyword: Keyword): Set<IntentTag> = when (keyword) {
        Keyword.CONVOKE, Keyword.IMPROVISE, Keyword.DELVE -> setOf(IntentTag.COST_REDUCER)
        Keyword.STORM, Keyword.CASUALTY, Keyword.CONSPIRE -> setOf(IntentTag.SPELL_COPY)
        Keyword.SPLIT_SECOND -> setOf(IntentTag.PROTECTION)
        Keyword.LIFELINK -> setOf(IntentTag.LIFEGAIN)
        else -> emptySet()
    }

    /** A [ModifySpellCost] reach that touches only its controller's own spells. */
    internal fun ownSpellsOnly(target: SpellCostTarget): Boolean =
        target is SpellCostTarget.YouCast || target is SpellCostTarget.YouCastFromZones ||
            target == SpellCostTarget.FaceDownYouCast

    /** Exhaustive, so a new cost-increase variant has to decide whether it reads as a tax. */
    internal fun taxes(modification: CostModification): Boolean = when (modification) {
        is CostModification.IncreaseGeneric, is CostModification.IncreaseGenericBy,
        is CostModification.IncreaseGenericIfAnyTargetMatches,
        is CostModification.IncreaseGenericPerOtherSpellThisTurn,
        is CostModification.IncreaseColored, is CostModification.IncreaseColoredPerUnit,
        is CostModification.IncreaseLife -> true
        is CostModification.ReduceGeneric, is CostModification.ReduceGenericBy,
        is CostModification.ReduceColored, is CostModification.ReduceColoredPerUnit,
        is CostModification.ReduceColoredIfAnyTargetMatches -> false
    }

    /**
     * [IntentTag.NEUTRALIZE] for a "can't attack / can't block" that lands on the **attached**
     * creature, and nothing for any other scope.
     *
     * The scope is the whole test, and both rejections are load-bearing. [Scope.Source] is a
     * creature's own printed drawback (Juggernaut can't block) — reading that as an answer would
     * tag a sizeable slice of the catalog's creatures as removal. [Scope.Battlefield] is a
     * symmetric lock (Meekstone, Blazing Archon) whose worth is a board-wide question this tag's
     * one consumer — a per-target trade — cannot ask.
     *
     * Two static abilities, not one: Pacifism prints them separately, and a card that only takes
     * blocking away (Dead Weight-style "can't block") is still the same decision about the same
     * kind of trade.
     */
    private fun neutralizeTags(filter: GroupFilter): Set<IntentTag> =
        if (filter.scope == Scope.AttachedTo || filter.reachesEnchantedPlayer()) setOf(IntentTag.NEUTRALIZE) else emptySet()
    /**
     * Total P+T a battlefield-wide anthem hands to each creature it covers. Zero for an
     * Aura/Equipment bonus, which lands on one creature and is therefore already visible in that
     * creature's projected power and toughness.
     */
    private fun anthemBonusOf(static: StaticAbility): Int = when {
        static !is ModifyStats -> 0
        static.filter.scope != Scope.Battlefield -> 0
        static.powerBonus <= 0 && static.toughnessBonus <= 0 -> 0
        else -> maxOf(0, static.powerBonus) + maxOf(0, static.toughnessBonus)
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Speed / repeatability
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * [CardIntent.targetsOnlyOurPermanents] — see there for why a consumer cannot get this from
     * [IntentTag.REMOVAL].
     *
     * Requirements are read **flat**, across the spell effect and every triggered and activated
     * ability, without matching each one back to the effect that consumes it. The question is "could
     * any of this card's targeting point across the table", and a flat scan answers exactly that;
     * matching bound variables to requirements would be the beginning of the `hitsAnotherPermanent`
     * repair this field exists to avoid.
     *
     * [TargetObject] is the only requirement kind considered, which is the one that targets a
     * permanent — [com.wingedsheep.sdk.scripting.targets.TargetCreature] and friends are factory
     * functions that build one. A player target says nothing about whose board is affected.
     *
     * A filter carrying [TargetFilter.alternatives] is declined outright rather than descended into:
     * "target creature you control **or** artifact an opponent controls" is a real shape, and every
     * caller of this is better served by a false negative than by a wrong true.
     */
    private fun targetsOnlyOurPermanents(scripts: List<CardScript>): Boolean {
        val permanentTargets = scripts
            .flatMap(::targetRequirementsOf)
            .filterIsInstance<TargetObject>()
        return permanentTargets.isNotEmpty() && permanentTargets.all { requirement ->
            requirement.filter.alternatives.isEmpty() &&
                requirement.filter.baseFilter.controllerPredicate == ControllerPredicate.ControlledByYou
        }
    }

    /** Every target requirement on [script], wherever it is declared. */
    private fun targetRequirementsOf(script: CardScript): List<TargetRequirement> =
        script.targetRequirements +
            script.triggeredAbilities.flatMap {
                listOfNotNull(it.targetRequirement) + it.additionalTargetRequirements
            } +
            script.activatedAbilities.flatMap { it.targetRequirements }

    private fun speedOf(card: CardDefinition, scripts: List<CardScript>, tags: Set<IntentTag>): Speed {
        if (!card.typeLine.isPermanent) {
            return if (card.typeLine.isInstant || Keyword.FLASH in card.keywords) Speed.INSTANT
            else Speed.SORCERY
        }
        if (Keyword.FLASH in card.keywords) return Speed.INSTANT
        if (scripts.any { script -> script.activatedAbilities.any { !it.isManaAbility } }) return Speed.ACTIVATED
        if (scripts.any { it.staticAbilities.isNotEmpty() } || IntentTag.ANTHEM in tags ||
            IntentTag.DAMAGE_AMPLIFIER in tags ||
            scripts.any { script -> script.replacementEffects.any(DamageShields::reads) }) return Speed.STATIC
        return Speed.SORCERY
    }

    /**
     * Whether [script] makes its permanent enter tapped **every time**, with no choice attached.
     *
     * `unlessCondition` and `payLifeCost` are both rejected rather than approximated: a shock land
     * is normally played untapped and a "unless you control two or fewer other lands" land is
     * untapped exactly on the turns that matter most, so reading either as an unconditional
     * tapland would price the *better* card as the worse one. See [CardIntent.entersTapped].
     */
    private fun alwaysEntersTapped(script: CardScript): Boolean =
        script.replacementEffects.filterIsInstance<EntersTapped>()
            .any { it.unlessCondition == null && it.payLifeCost == null }

    /**
     * Whether the card's payoff can happen more than once.
     *
     * Two things that look repeatable and are not. An activated ability that eats its own source
     * (Mind Stone's "{1}, {T}, Sacrifice: draw a card") is a one-shot with extra steps — that is
     * exactly the difference between a mana rock and an engine. And a permanent's *own* enter- or
     * leave-the-battlefield trigger fires once per object (Banishing Light), so counting it would
     * make every ETB permanent in the catalog read as a repeatable one.
     */
    private fun repeatableOf(card: CardDefinition, scripts: List<CardScript>): Boolean {
        if (!card.typeLine.isPermanent) return false
        return scripts.any { script ->
            script.activatedAbilities.any {
                it.activateFromZone == Zone.BATTLEFIELD && !it.isManaAbility && !consumesSource(it.cost)
            } ||
                script.triggeredAbilities.any { canFireMoreThanOnce(it, isAura = script.auraTarget != null) }
        }
    }

    /** "At the beginning of the end step, sacrifice this creature" — a body that leaves the turn it arrives. */
    private fun sacrificesItselfAtEndStep(scripts: List<CardScript>): Boolean = scripts.any { script ->
        script.triggeredAbilities.any { trigger ->
            (trigger.trigger as? EventPattern.StepEvent)?.step == com.wingedsheep.sdk.core.Step.END &&
                EffectWalker.leaves(trigger.effect).any {
                    it is SacrificeSelfEffect || (it is SacrificeTargetEffect && it.target == EffectTarget.Self)
                }
        }
    }

    private fun consumesSource(cost: AbilityCost): Boolean = when (cost) {
        AbilityCost.SacrificeSelf, AbilityCost.ExileSelf, AbilityCost.ReturnSelfToHand,
        is AbilityCost.PutSelfIntoLibrary -> true
        is AbilityCost.Composite -> cost.costs.any { consumesSource(it) }
        else -> false
    }

    /**
     * False for a trigger that fires on the event that put the object into its current state, and
     * then not again while it stays there: "when *this* permanent enters/leaves the battlefield",
     * and a Room half's "when you unlock this door" (CR 709.5h). Counting either would read every
     * ETB permanent, and every already-open Room door, as an engine.
     *
     * A door can in principle be re-locked and unlocked again (CR 709.5g, the SDK's
     * `LockDoorEffect`), so "fires once" is a claim about the common case, not an invariant. That
     * is the right way round for a prior: under-reading a rare re-unlock costs the AI nothing it
     * had, while over-reading every spent door as repeatable value is the bug this exists to avoid.
     */
    private fun canFireMoreThanOnce(ability: TriggeredAbility, isAura: Boolean = false): Boolean {
        // "When enchanted creature dies / leaves the battlefield" (Dead Man's Chest): the host
        // leaves once and the Aura goes to the graveyard with it, so it never fires again. An
        // Equipment's "equipped creature dies" can re-equip and fire again, so only an Aura's counts.
        if (isAura && ability.binding == TriggerBinding.ATTACHED) {
            val pattern = ability.trigger
            if (pattern is EventPattern.ZoneChangeEvent && pattern.from == Zone.BATTLEFIELD) return false
        }
        if (ability.binding != TriggerBinding.SELF) return true
        return when (val pattern = ability.trigger) {
            is EventPattern.ZoneChangeEvent ->
                pattern.to != Zone.BATTLEFIELD && pattern.from != Zone.BATTLEFIELD

            is EventPattern.DoorUnlockedEvent -> false
            else -> true
        }
    }

    private fun sacrificesOthers(cost: AbilityCost): Boolean = when (cost) {
        is AbilityCost.Atom -> cost.atom is CostAtom.Sacrifice
        is AbilityCost.Composite -> cost.costs.any { sacrificesOthers(it) }
        else -> false
    }

    // ═════════════════════════════════════════════════════════════════════════
    // The prior
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * How much this card is worth *sitting on a battlefield*, before anything board-dependent.
     *
     * Non-permanents get `0.0`: they are never on a battlefield, so the only consumer that reads
     * this ([com.wingedsheep.ai.engine.evaluation.BoardPresence]) would never ask. Anything the
     * analyzer could not interpret gets the historical flat `0.5`, so unrecognized permanents are
     * valued exactly as they were before Phase 6.
     *
     * The ladder below is a set of independent claims and the highest one wins, so adding a card
     * shape can only ever raise a value, never silently lower an unrelated one. It is calibrated
     * against the card-advantage cost of *casting removal* at the permanent, not against sibling
     * targets — see [CardIntent.staticPriorValue].
     */
    private fun priorValueOf(card: CardDefinition, intent: CardIntent, scripts: List<CardScript>): Double {
        if (!card.typeLine.isPermanent) return 0.0
        if (card.typeLine.isCreature) {
            // Price targeted removal activations that remain available on the battlefield.
            // Off-zone abilities and self-consuming costs cannot keep answering opposing threats.
            val repeatableRemoval = scripts.any { script ->
                script.activatedAbilities.any { ability ->
                    ability.activateFromZone == Zone.BATTLEFIELD && !ability.isManaAbility &&
                        !consumesSource(ability.cost) &&
                        IntentTag.REMOVAL in analyzeEffect(ability.effect, ability.targetRequirements).tags &&
                        TargetPolarityAnalyzer.forRequirements(ability.effect, ability.targetRequirements)
                            .withIndex().any { (index, polarity) ->
                                !TargetPolarityAnalyzer.isOwnSide(ability.targetRequirements[index]) &&
                                    (polarity == TargetPolarity.HARMFUL || polarity == TargetPolarity.FLEXIBLE)
                            }
                }
            }
            // A graveyard answered every turn (Scavenging Ooze, Ashnod's Harvester) keeps a
            // recursion deck honest, but answers nothing on the board — a third of removal's term.
            val repeatableHate = !repeatableRemoval && scripts.any { script ->
                script.activatedAbilities.any { ability ->
                    ability.activateFromZone == Zone.BATTLEFIELD && !ability.isManaAbility &&
                        !consumesSource(ability.cost) &&
                        IntentTag.GRAVEYARD_HATE in analyzeEffect(ability.effect, ability.targetRequirements).tags
                }
            }
            val amplifier = if (IntentTag.DAMAGE_AMPLIFIER in intent.tags) 1.5 else 0.0
            // A creature that counters whatever targets it (Glyph Keeper) shrugs off the first
            // removal spell aimed at it, a soft hexproof the keyword-only body value can't see.
            // Below hexproof's 1.5: it guards once a turn and turns away its controller's own
            // pump spells too.
            val shieldsItself = scripts.any { script ->
                script.triggeredAbilities.any { trigger ->
                    EffectWalker.leaves(trigger.effect).any {
                        it is CounterEffect && it.targetSource == CounterTargetSource.TargetingSource
                    }
                }
            }
            // A body that hands its controller a card again and again (Dark Confidant's upkeep,
            // a combat-damage draw, Fireglass Mentor's second-main impulse) is a card-advantage
            // engine the creature's stats can't see: kill it and the stream stops. Below repeatable
            // removal: a card a turn is worth less than answering a threat a turn. An ETB or
            // leaves-play draw has already paid out, or pays out in the removal, so it is not counted,
            // and neither is a body that sacrifices itself at the end step (Frostbite Pyromental):
            // it is gone before the stream can run twice.
            // A free permanent or spell every time it fires (Kinscaer Sentry and Goblin Lackey
            // putting a creature from hand onto the battlefield, Goliath Daydreamer recasting a
            // spell) is the same engine paid in mana instead of cards, and so is a card back from
            // the graveyard or a stash every time it fires (Gorex, the Tombshell's attack trigger).
            // Only a trigger that works from the battlefield is a reason to keep this permanent
            // alive: a graveyard-only self-return (Bloodghast) streams from the graveyard instead.
            val ownPile = stashesOwnCards(scripts)
            val repeatableDraw = !repeatableRemoval && !sacrificesItselfAtEndStep(scripts) && scripts.any { script ->
                script.triggeredAbilities.any { trigger ->
                    Zone.BATTLEFIELD in trigger.activeZones && canFireMoreThanOnce(trigger) && analyzeEffect(
                        trigger.effect,
                        trigger.allTargetRequirements,
                        turnAhead = EffectWalker.firesWithItsTurnAhead(trigger),
                        ownPile = ownPile,
                    ).tags.let {
                        IntentTag.DRAW in it || IntentTag.FREE_SPELL in it || IntentTag.IMPULSE in it ||
                            IntentTag.RECURSION in it
                    }
                } || script.activatedAbilities.any { ability ->
                    ability.activateFromZone == Zone.BATTLEFIELD && !ability.isManaAbility &&
                        !consumesSource(ability.cost) && !isRummage(ability) &&
                        analyzeEffect(ability.effect, ability.targetRequirements).tags
                            .let { IntentTag.DRAW in it || IntentTag.IMPULSE in it }
                }
            }
            // A second life (Exquisite Archangel, Enduring Angel — [LossShields]): while it stands,
            // the opponent's lethal turn only resets us, so it is worth repeatable removal's term.
            val lossShield = scripts.any { script -> script.replacementEffects.any(LossShields::reads) }
            val ability = when {
                repeatableRemoval || lossShield -> 1.5
                shieldsItself || repeatableDraw -> 1.0
                repeatableHate -> 0.5
                else -> 0.0
            }
            return CardIntent.UNKNOWN.staticPriorValue + maxOf(ability, amplifier)
        }
        return priorLadder(intent).maxOfOrNull { it.value } ?: CardIntent.UNKNOWN.staticPriorValue
    }

    /** One rung of [priorLadder]: the claim in words, and what it prices the permanent at. */
    data class PriorRung(val claim: String, val value: Double)

    /**
     * Every rung of a non-creature permanent's prior ladder that [intent] reaches — the highest is
     * [CardIntent.staticPriorValue]. Public so a developer view can say *which* claim priced the
     * card; [priorValueOf] is its only in-game reader.
     */
    fun priorLadder(intent: CardIntent): List<PriorRung> {
        val tags = intent.tags
        val repeatable = intent.repeatable
        val removal = IntentTag.REMOVAL in tags || IntentTag.EXILE_REMOVAL in tags
        return buildList {
            // Removal that can answer a *new* permanent every turn is the strongest thing a
            // non-creature permanent does.
            if (repeatable && removal) add(PriorRung("repeatable removal", 4.0))
            // A repeatable tapper answers the same permanent every turn, and an O-Ring answers one
            // permanent for as long as it lives. Different mechanisms, comparable worth — and both
            // are things you spend a Disenchant on.
            if (repeatable && IntentTag.TAPPER in tags) add(PriorRung("repeatable tapper", 3.5))
            if (removal) add(PriorRung("removal", 3.5))
            if (repeatable && IntentTag.MILL in tags) add(PriorRung("repeatable mill win condition", 2.0))
            if (repeatable && IntentTag.DRAW in tags) add(PriorRung("repeatable card draw", 3.0))
            // A card a turn that must be spent the turn it comes (Birgi, Ark of Hunger): an engine,
            // but only on turns with mana to play what it exiles — below a draw engine.
            if (repeatable && IntentTag.IMPULSE in tags) add(PriorRung("repeatable impulse draw", 2.0))
            if (repeatable && IntentTag.TOKEN_MAKER in tags) add(PriorRung("repeatable token maker", 3.0))
            if (IntentTag.ANTHEM in tags) add(PriorRung("anthem", 3.0))
            if (IntentTag.DAMAGE_AMPLIFIER in tags) add(PriorRung("amplifies our damage", 2.0))
            // A standing "cast spells without paying their mana costs" (Omniscience, Omnipresence)
            // is a free spell every turn it stays: an engine as strong as an amplifier, and the
            // permanent the opponent most wants gone. A one-shot free cast is priced by its cast.
            if (IntentTag.FREE_SPELL in tags && intent.speed == Speed.STATIC) add(PriorRung("casts our spells for free", 2.0))
            if (IntentTag.SACRIFICE_OUTLET in tags) add(PriorRung("sacrifice outlet", 1.5))
            if (IntentTag.EVASION_GRANT in tags || IntentTag.PROTECTION in tags) add(PriorRung("grants evasion or protection", 1.5))
            // A mana rock is worth about a land, and an untapped land is 0.6.
            if (IntentTag.RAMP in tags) add(PriorRung("mana source", 0.7))
            if (repeatable) add(PriorRung("repeatable ability", 1.5))
            // An Aura that stops a creature attacking and blocking answers it for as long as it
            // stays, like an O-Ring that leaves the body on the board.
            if (IntentTag.NEUTRALIZE in tags) add(PriorRung("neutralizes a creature", 3.0))
            if (IntentTag.TAPPER in tags) add(PriorRung("tapper", 1.5))
            // A drawback (its own combat restriction, a tax on its controller's spells) is no
            // effect worth a removal spell.
            if (intent.speed == Speed.STATIC && tags.any { it !in DRAWBACK_TAGS }) add(PriorRung("static effect", 1.0))
            // A one-shot payoff still waiting to be cashed — a Spellbomb, a Bauble, an Omen, an
            // incubator — is a card's worth of value sitting on the board, below any repeatable
            // engine but above a permanent nothing reads (0.5).
            // Only an activated ability's payoff is still waiting: an ETB has already paid out by
            // the time the permanent is on the board.
            val cashable = intent.cashableTags
            if (IntentTag.DRAW in cashable || IntentTag.TOKEN_MAKER in cashable) add(PriorRung("one-shot card or token", 1.0))
            if (IntentTag.TUTOR in cashable || IntentTag.RECURSION in cashable) add(PriorRung("one-shot tutor or recursion", 0.8))
            if (IntentTag.CARD_SELECTION in cashable) add(PriorRung("one-shot card selection", 0.7))
            if (IntentTag.IMPULSE in cashable) add(PriorRung("one-shot impulse draw", 0.7))
            if (IntentTag.LIFEGAIN in cashable) add(PriorRung("one-shot life gain", 0.6))
            // A graveyard answer waiting on the board (Tormod's Crypt): real against recursion, idle
            // against anything else. A repeatable one is priced by the generic repeatable rung.
            if (IntentTag.GRAVEYARD_HATE in cashable) add(PriorRung("one-shot graveyard hate", 0.6))
            if (IntentTag.PUMP in tags) add(PriorRung("pump", 1.0))
        }
    }

    /**
     * "Untap all lands you control", the shape [untapsOurLands] reads as ramp. [readingLeaves] reads
     * an untap aimed at a target land ([untapsATargetLand]) as this: the land is ours, and it taps
     * for mana again — one land of the group, the same payoff.
     */
    private val UNTAP_OUR_LANDS: Effect = ForEachEffect(
        IterationSpace.Group(GroupFilter(GameObjectFilter.Land.youControl())),
        TapUntapEffect(EffectTarget.IterationEntity, tap = false),
    )

    /**
     * What [readingLeaves] reads an exile-then-return-at-the-end-step of our own permanent as
     * (Liberate, Niko, Light of Hope): the blink an immediate exile-and-return is
     * ([pipelineTags]'s PROTECTION), only coming back later. Also an immediate blink of ours whose
     * return names a controller (Cloudshift's "under your control"), which [EffectWalker.blinkMoves]
     * leaves out because aimed at anyone's creature it would be a theft. Compared by identity.
     */
    private val DELAYED_BLINK: Effect = CompositeEffect(
        listOf(MoveToZoneEffect(EffectTarget.Self, Zone.EXILE), MoveToZoneEffect(EffectTarget.Self, Zone.BATTLEFIELD)),
    )

    /**
     * What [readingLeaves] reads an exile-then-return-at-the-end-step of a creature anyone may
     * control as (Galepowder Mage, Otherworldly Journey, Long Road Home): a blocker or attacker
     * taken away for the turn — [IntentTag.REMOVAL] — that comes back, so never the permanent
     * answer [IntentTag.EXILE_REMOVAL] promises. Compared by identity.
     */
    private val TEMPORARY_EXILE: Effect = CompositeEffect(
        listOf(MoveToZoneEffect(EffectTarget.ContextTarget(0), Zone.EXILE)),
    )

    /**
     * What [readingLeaves] reads "exile target creature you control" linked to this permanent as
     * (Endless Sands, Safe Haven): our creature stashed out of harm's way until the card returns it
     * ([stashesOwnCards]) — a slow blink, [IntentTag.PROTECTION], never removal. Compared by identity.
     */
    private val OWN_STASH: Effect = CompositeEffect(
        listOf(MoveToZoneEffect(EffectTarget.Self, Zone.EXILE, linkToSource = true)),
    )

    /**
     * What [readingLeaves] reads a single graveyard card exiled or tucked from a graveyard that may
     * be an opponent's as ([GraveyardMove.HATE]): [IntentTag.GRAVEYARD_HATE], never the removal its
     * move would read as on the battlefield. Compared by identity.
     */
    private val GRAVEYARD_HATE_LEAF: Effect =
        MoveToZoneEffect(EffectTarget.Self, Zone.EXILE, fromZone = Zone.GRAVEYARD)

    /**
     * What a [reanimatesChosenGraveyardTargets] pipeline reads as: a card out of a graveyard onto
     * our battlefield — [IntentTag.RECURSION], as the single-card return is in [tagsOf].
     */
    private val REANIMATE_FROM_A_GRAVEYARD: Effect =
        MoveToZoneEffect(EffectTarget.Self, Zone.BATTLEFIELD, fromZone = Zone.GRAVEYARD)

    /**
     * What [readingLeaves] reads a card exiled from our own graveyard and then granted a cast from
     * exile as: the return a recursion's `MoveToZoneEffect` out of the graveyard is
     * ([IntentTag.RECURSION]), only by way of exile. A pipeline returning its graveyard targets to
     * our hand ([returnsChosenGraveyardTargetsToHand]) is that return outright.
     */
    private val RECAST_FROM_OUR_GRAVEYARD: Effect =
        MoveToZoneEffect(EffectTarget.Self, Zone.HAND, fromZone = Zone.GRAVEYARD)

    /** Tags that name what a card costs its controller rather than what it does for them. */
    private val DRAWBACK_TAGS = setOf(IntentTag.COMBAT_RESTRICTION, IntentTag.OWN_SPELL_TAX)

    private val OPPONENT_FACING = setOf(
        IntentTag.CAST_BAN,
        IntentTag.SPELL_TAX,
        IntentTag.FORCED_ATTACK,
        IntentTag.GRAVEYARD_HATE,
        IntentTag.MILL,
        IntentTag.REMOVAL,
        IntentTag.EXILE_REMOVAL,
        IntentTag.SWEEPER,
        IntentTag.NEUTRALIZE,
        IntentTag.COUNTERSPELL,
        IntentTag.DISCARD,
        IntentTag.TAPPER,
    )

    private val EVASION_KEYWORDS = setOf(
        Keyword.FLYING, Keyword.MENACE, Keyword.FEAR, Keyword.INTIMIDATE, Keyword.SHADOW,
        Keyword.SKULK, Keyword.TRAMPLE,
    ) + Keyword.LANDWALK

    private val EVASION_FLAGS = setOf(
        AbilityFlag.CANT_BE_BLOCKED.name, AbilityFlag.CANT_BE_BLOCKED_BY_MORE_THAN_ONE.name,
    )

    /**
     * Flags that change how a creature fights rather than whether it can be blocked: "assigns combat
     * damage equal to its toughness rather than its power" (Plagon, Bill the Pony) turns a wall into
     * an attacker — a combat enhancement, like a granted first strike.
     */
    private val COMBAT_FLAGS = setOf(AbilityFlag.ASSIGNS_COMBAT_DAMAGE_AS_TOUGHNESS.name)

    /** The flags that keep a tapped permanent tapped through its controller's untap step. */
    private val LOCKED_TAPPED_FLAGS = setOf(AbilityFlag.DOESNT_UNTAP.name, AbilityFlag.CANT_BECOME_UNTAPPED.name)

    /** The starting life total (rule 103.4) — see [setsOwnLifeAboveStart]. */
    private const val STARTING_LIFE = 20

    /** Riders that leave nothing behind once resolved — see [isExpiringProtectionTrick]. */
    private val SPENT_ON_RESOLUTION = setOf(IntentTag.CARD_SELECTION, IntentTag.LIFEGAIN)

    /**
     * Keywords that win or open a fight without adding power or toughness — see
     * [IntentTag.COMBAT_ENHANCEMENT]. Haste is here because it is spent attacking this turn;
     * reach (a blocker's keyword), defender, banding and the like are not gifts a controller hands
     * its attacker.
     */
    private val COMBAT_KEYWORDS = setOf(
        Keyword.FIRST_STRIKE, Keyword.DOUBLE_STRIKE, Keyword.DEATHTOUCH, Keyword.LIFELINK,
        Keyword.VIGILANCE, Keyword.HASTE,
        // Damage dealt as -1/-1 counters (Barbed Bloodletter's "gains wither"): the blocker it
        // fights stays shrunk, and infect's poison is the fight's whole point.
        Keyword.WITHER, Keyword.INFECT,
        // A copy attacking each other opponent: spent attacking, as haste is.
        Keyword.MYRIAD,
    )

    private val PROTECTIVE_KEYWORDS = setOf(
        Keyword.HEXPROOF, Keyword.SHROUD, Keyword.INDESTRUCTIBLE, Keyword.PROTECTION,
        Keyword.WARD,
        // "Another target creature you control gains persist until end of turn" (Rhys, the
        // Evermore): the creature comes back when it dies — a death shield, as regeneration is.
        Keyword.PERSIST, Keyword.UNDYING,
    )

    /** The prefix of a scoped protection grant's keyword string — see [keywordTags]. */
    private const val PROTECTION_FROM = "PROTECTION_FROM_"
}
