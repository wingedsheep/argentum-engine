package com.wingedsheep.ai.engine.knowledge

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardFace
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.CantAttack
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GrantDynamicStatsEffect
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.StaticAbility
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.effects.*
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.Scope
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget
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
 * for why widening it is a separate change. A card whose whole payoff is inside one of those reads
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

    private val cardCache = ConcurrentHashMap<String, CardIntent>()
    private val selfCache = ConcurrentHashMap<String, CardIntent>()
    private val faceCache = ConcurrentHashMap<FaceKey, CardIntent>()

    private data class FaceKey(val cardName: String, val faceName: String)

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
    fun analyzeEffect(effect: Effect): CardIntent {
        val tags = mutableSetOf<IntentTag>()
        var removalReach: Int? = null
        for (leaf in EffectWalker.leaves(effect)) {
            tags += tagsOf(leaf)
            reachOf(leaf)?.let { removalReach = maxOf(removalReach ?: 0, it) }
        }
        return CardIntent.UNKNOWN.copy(tags = tags, removalReach = removalReach)
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
        val leaves = scripts.flatMap { script ->
            EffectWalker.slots(script).map { it.effect } +
                script.stateTriggeredAbilities.map { it.effect }
        }.flatMap { EffectWalker.leaves(it) }

        val tags = mutableSetOf<IntentTag>()
        var removalReach: Int? = null
        var cardsDrawn: Int? = null
        var expiringPump = false
        var pumpToughness = 0

        for (effect in leaves) {
            tags += tagsOf(effect)
            reachOf(effect)?.let { removalReach = maxOf(removalReach ?: 0, it) }
            drawsOf(effect)?.let { cardsDrawn = maxOf(cardsDrawn ?: 0, it) }
            if (isExpiringPump(effect)) {
                expiringPump = true
                pumpToughness = maxOf(pumpToughness, expiringToughnessOf(effect))
            }
        }

        var anthemBonus = 0
        for (static in scripts.flatMap { it.staticAbilities }) {
            tags += tagsOf(static)
            anthemBonus = maxOf(anthemBonus, anthemBonusOf(static))
        }

        val activated = scripts.flatMap { it.activatedAbilities }
        if (activated.any { it.isManaAbility }) tags += IntentTag.RAMP
        if (activated.any { !it.isManaAbility && sacrificesOthers(it.cost) }) {
            tags += IntentTag.SACRIFICE_OUTLET
        }
        // An Aura/Equipment whose whole point is the creature it sits on is a pump, not an anthem.
        if (scripts.any { it.isAura && it.staticAbilities.any { static -> static is ModifyStats } }) {
            tags += IntentTag.PUMP
        }

        // A combat trick is an instant pump that *expires*. A "+1/+1 counter at instant speed" is
        // a permanent investment you can make whenever you like, and holding it for combat is not
        // the same decision — so it stays PUMP and never reaches [HoldPolicy]'s no-window verdict.
        val speed = speedOf(card, scripts, tags)
        if (speed == Speed.INSTANT && expiringPump) tags += IntentTag.COMBAT_TRICK

        val repeatable = repeatableOf(card, scripts)
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
            entersTapped = scripts.any(::alwaysEntersTapped),
            flashPermanent = card.typeLine.isPermanent && Keyword.FLASH in card.keywords,
            hasHaste = Keyword.HASTE in card.keywords,
            targetsOnlyOurPermanents = targetsOnlyOurPermanents(scripts),
        )
        return intent.copy(staticPriorValue = priorValueOf(card, intent))
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Effect → tags
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * @param insideIteration whether this effect is the body of a [ForEachEffect]. It changes what
     *   [EffectTarget.Self] means: at the top level it is the card itself (so "put this into your
     *   graveyard" is a sacrifice clause, not removal), and inside an iteration it is the element
     *   currently being iterated over (so Wrath of God's "destroy it" *is* removal). Getting this
     *   backwards would price every Saga's final chapter as a removal spell.
     */
    private fun tagsOf(effect: Effect, insideIteration: Boolean = false): Set<IntentTag> = when (effect) {
        is MoveToZoneEffect -> when {
            // A group-targeted move is a wrath; a single-target one is spot removal. Both only
            // count as removal when they take *someone else's* permanent off the battlefield.
            effect.destination == Zone.BATTLEFIELD -> setOfNotNull(
                IntentTag.RECURSION.takeIf { effect.fromZone == Zone.GRAVEYARD }
            )

            effect.fromZone == Zone.GRAVEYARD -> setOf(IntentTag.RECURSION)
            !hitsAnotherPermanent(effect.target, insideIteration) -> emptySet()
            effect.destination == Zone.EXILE ->
                removalTags(effect.target) + IntentTag.EXILE_REMOVAL

            else -> removalTags(effect.target)
        }

        is ExileUntilLeavesEffect -> setOf(IntentTag.REMOVAL, IntentTag.EXILE_REMOVAL)
        is PhaseOutUntilLeavesEffect -> setOf(IntentTag.REMOVAL)
        is ForceSacrificeEffect -> setOf(IntentTag.REMOVAL)
        is SacrificeTargetEffect -> setOf(IntentTag.REMOVAL)
        is GainControlEffect -> setOf(IntentTag.REMOVAL)
        is FightEffect -> setOf(IntentTag.REMOVAL, IntentTag.FIGHT)

        // Damage to a *player* is a clock, not an answer — a permanent that pings the opponent for
        // 1 each upkeep must not be priced as repeatable removal.
        is DealDamageEffect ->
            if (!hitsAnotherPermanent(effect.target, insideIteration)) emptySet()
            else removalTags(effect.target)

        is DividedDamageEffect -> setOf(IntentTag.REMOVAL)

        is ModifyStatsEffect -> {
            val power = fixed(effect.powerModifier) ?: 0
            val toughness = fixed(effect.toughnessModifier) ?: 0
            when {
                // "-N/-N" is removal by another name, and reaches exactly N toughness.
                toughness < 0 && hitsAnotherPermanent(effect.target, insideIteration) ->
                    removalTags(effect.target)

                toughness < 0 -> emptySet()
                power > 0 || toughness > 0 -> setOf(IntentTag.PUMP)
                else -> emptySet()
            }
        }

        is TapUntapEffect -> if (effect.tap) setOf(IntentTag.TAPPER) else emptySet()
        is TapUntapCollectionEffect -> if (effect.tap) setOf(IntentTag.TAPPER) else emptySet()

        is DrawCardsEffect, is DrawUpToEffect -> setOf(IntentTag.DRAW)
        is CounterEffect, is CounterAllOnStackEffect -> setOf(IntentTag.COUNTERSPELL)
        is GainLifeEffect, is DrainLifeEffect -> setOf(IntentTag.LIFEGAIN)
        is CreateTokenEffect, is CreatePredefinedTokenEffect, is CreateTokenCopyOfTargetEffect,
        is CreateTokenCopyOfSourceEffect -> setOf(IntentTag.TOKEN_MAKER)

        is AddManaEffect, is AddColorlessManaEffect, is AddDynamicManaEffect,
        is AddManaOfChoiceEffect, is PlayAdditionalLandsEffect -> setOf(IntentTag.RAMP)

        is RegenerateEffect, is PreventDamageEffect -> setOf(IntentTag.PROTECTION)

        is GrantKeywordEffect -> keywordTags(effect.keyword)
        is GrantEvasionKeywordEffect -> setOf(IntentTag.EVASION_GRANT)

        is GatherCardsEffect ->
            if ((effect.source as? CardSource.FromZone)?.zone == Zone.LIBRARY) setOf(IntentTag.TUTOR)
            else emptySet()

        is EachPlayerDiscardsOrLoseLifeEffect -> setOf(IntentTag.DISCARD)
        is ReturnSameNamedFromGraveyardEffect -> setOf(IntentTag.RECURSION)

        // A wrath is "for each creature: destroy it" — an iteration, not a group-targeted move, so
        // the shape only shows up here. [EffectWalker] deliberately treats a ForEach as a leaf (see
        // there for why), which makes this the analyzer's own reading of the node: interpret the
        // body, and promote it to a sweeper when the iteration spans a whole group of permanents.
        is ForEachEffect -> {
            val inner = EffectWalker.leaves(effect.body)
                .flatMap { tagsOf(it, insideIteration = true) }
                .toSet()
            if (effect.space is IterationSpace.Group && IntentTag.REMOVAL in inner) {
                inner + IntentTag.SWEEPER
            } else {
                inner
            }
        }

        else -> emptySet()
    }

    /** Spot removal or a wrath, depending on whether the effect names a group. */
    private fun removalTags(target: EffectTarget): Set<IntentTag> =
        if (targetsAGroup(target)) setOf(IntentTag.REMOVAL, IntentTag.SWEEPER)
        else setOf(IntentTag.REMOVAL)

    private fun targetsAGroup(target: EffectTarget): Boolean = target is EffectTarget.GroupRef

    /**
     * Whether an effect aimed at [target] lands on a permanent that is not the card itself — the
     * precondition for calling anything "removal".
     *
     * Two exclusions. A **player** target is a clock, not an answer. And **[EffectTarget.Self]** is
     * the card sacrificing or exiling itself, unless [insideIteration], where it names whichever
     * permanent the surrounding `ForEach` is currently visiting.
     */
    private fun hitsAnotherPermanent(target: EffectTarget, insideIteration: Boolean): Boolean = when (target) {
        is EffectTarget.PlayerRef, EffectTarget.Controller, EffectTarget.TargetController,
        EffectTarget.ControllerOfTriggeringEntity, EffectTarget.ControllerOfDamageSource -> false

        EffectTarget.Self -> insideIteration
        else -> true
    }

    private fun keywordTags(keyword: String): Set<IntentTag> {
        val parsed = runCatching { Keyword.valueOf(keyword.uppercase()) }.getOrNull()
            ?: return emptySet()
        return when (parsed) {
            in EVASION_KEYWORDS -> setOf(IntentTag.EVASION_GRANT)
            in PROTECTIVE_KEYWORDS -> setOf(IntentTag.PROTECTION)
            else -> emptySet()
        }
    }

    /** The largest creature this effect answers by damage or shrinking, if it answers by size. */
    private fun reachOf(effect: Effect): Int? = when (effect) {
        // Face damage answers no creature, so it contributes no reach — same exclusion as the tag.
        is DealDamageEffect ->
            if (hitsAnotherPermanent(effect.target, insideIteration = false)) fixed(effect.amount) else null

        is DividedDamageEffect -> effect.totalDamage
        is ModifyStatsEffect -> fixed(effect.toughnessModifier)?.takeIf { it < 0 }?.let { -it }
        else -> null
    }

    /** How much toughness an [isExpiringPump] effect grants. Zero for a power-only trick. */
    private fun expiringToughnessOf(effect: Effect): Int =
        ((effect as? ModifyStatsEffect)?.let { fixed(it.toughnessModifier) } ?: 0).coerceAtLeast(0)

    /** A stat boost that goes away at cleanup — the thing a combat trick is made of. */
    private fun isExpiringPump(effect: Effect): Boolean {
        if (effect !is ModifyStatsEffect) return false
        if (effect.duration != Duration.EndOfTurn) return false
        val power = fixed(effect.powerModifier) ?: 0
        val toughness = fixed(effect.toughnessModifier) ?: 0
        return power > 0 || toughness > 0
    }

    private fun drawsOf(effect: Effect): Int? = when (effect) {
        is DrawCardsEffect -> fixed(effect.count)
        is DrawUpToEffect -> effect.maxCards
        else -> null
    }

    private fun fixed(amount: DynamicAmount): Int? = (amount as? DynamicAmount.Fixed)?.amount

    // ═════════════════════════════════════════════════════════════════════════
    // Static abilities
    // ═════════════════════════════════════════════════════════════════════════

    private fun tagsOf(static: StaticAbility): Set<IntentTag> = when (static) {
        is ModifyStats -> when {
            static.filter.scope != Scope.Battlefield -> setOf(IntentTag.PUMP)
            static.powerBonus > 0 || static.toughnessBonus > 0 -> setOf(IntentTag.ANTHEM)
            static.powerBonus < 0 || static.toughnessBonus < 0 -> setOf(IntentTag.REMOVAL)
            else -> emptySet()
        }

        is GrantDynamicStatsEffect ->
            if (static.filter.scope == Scope.Battlefield) setOf(IntentTag.ANTHEM) else setOf(IntentTag.PUMP)

        is GrantKeyword -> keywordTags(static.keyword)
        is CantAttack -> neutralizeTags(static.filter)
        is CantBlock -> neutralizeTags(static.filter)
        else -> emptySet()
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
        if (filter.scope == Scope.AttachedTo) setOf(IntentTag.NEUTRALIZE) else emptySet()

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
        if (scripts.any { it.staticAbilities.isNotEmpty() } || IntentTag.ANTHEM in tags) return Speed.STATIC
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
            script.activatedAbilities.any { !it.isManaAbility && !consumesSource(it.cost) } ||
                script.triggeredAbilities.any { canFireMoreThanOnce(it) }
        }
    }

    private fun consumesSource(cost: AbilityCost): Boolean = when (cost) {
        AbilityCost.SacrificeSelf, AbilityCost.ExileSelf, AbilityCost.ReturnSelfToHand -> true
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
    private fun canFireMoreThanOnce(ability: TriggeredAbility): Boolean {
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
    private fun priorValueOf(card: CardDefinition, intent: CardIntent): Double {
        if (!card.typeLine.isPermanent) return 0.0
        if (card.typeLine.isCreature) {
            // Creatures are priced from their projected stats, which the evaluator can already
            // see; a prior on top would double-count them.
            return CardIntent.UNKNOWN.staticPriorValue
        }
        val tags = intent.tags
        val repeatable = intent.repeatable

        val candidates = buildList {
            // Removal that can answer a *new* permanent every turn is the strongest thing a
            // non-creature permanent does.
            if (repeatable && (IntentTag.REMOVAL in tags || IntentTag.EXILE_REMOVAL in tags)) add(4.0)
            // A repeatable tapper answers the same permanent every turn, and an O-Ring answers one
            // permanent for as long as it lives. Different mechanisms, comparable worth — and both
            // are things you spend a Disenchant on.
            if (repeatable && IntentTag.TAPPER in tags) add(3.5)
            if (IntentTag.REMOVAL in tags || IntentTag.EXILE_REMOVAL in tags) add(3.5)
            if (repeatable && IntentTag.DRAW in tags) add(3.0)
            if (repeatable && IntentTag.TOKEN_MAKER in tags) add(3.0)
            if (IntentTag.ANTHEM in tags) add(3.0)
            if (IntentTag.SACRIFICE_OUTLET in tags) add(1.5)
            if (IntentTag.EVASION_GRANT in tags || IntentTag.PROTECTION in tags) add(1.5)
            // A mana rock is worth about a land, and an untapped land is 0.6.
            if (IntentTag.RAMP in tags) add(0.7)
            if (repeatable) add(1.5)
            if (intent.speed == Speed.STATIC && tags.isNotEmpty()) add(1.0)
        }
        return candidates.maxOrNull() ?: CardIntent.UNKNOWN.staticPriorValue
    }

    private val OPPONENT_FACING = setOf(
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
        Keyword.TRAMPLE, Keyword.SWAMPWALK, Keyword.FORESTWALK, Keyword.ISLANDWALK,
        Keyword.MOUNTAINWALK, Keyword.PLAINSWALK,
    )

    private val PROTECTIVE_KEYWORDS = setOf(
        Keyword.HEXPROOF, Keyword.SHROUD, Keyword.INDESTRUCTIBLE, Keyword.PROTECTION,
        Keyword.WARD,
    )
}
