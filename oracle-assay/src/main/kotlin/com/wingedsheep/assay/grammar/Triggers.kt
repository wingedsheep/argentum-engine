package com.wingedsheep.assay.grammar

import com.wingedsheep.assay.normalize.Normalizer
import com.wingedsheep.assay.syntax.Phrase
import com.wingedsheep.assay.syntax.alternate
import com.wingedsheep.assay.syntax.bind
import com.wingedsheep.assay.syntax.oneOf
import com.wingedsheep.assay.syntax.phrase
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.Gate
import com.wingedsheep.sdk.scripting.effects.GatedEffect
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.conditions.Condition
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.dsl.Triggers as SdkTriggers

/**
 * The trigger prefix — "When ~ enters, draw a card." — and with it the first rules that reach a
 * `CardScript` slot other than the spell effect.
 *
 * ### The prefix is a rule; the clause after it is [Steps]
 *
 * A trigger is a sentence made of a *when* clause and an *effect* clause, and the effect clause is
 * the same English a spell prints: "draw a card.", "destroy target creature." So the rules here slot
 * [Steps.step] whole and lift its `CardScript` onto the ability — the effect becomes the trigger's
 * effect, and a target it declared becomes the trigger's `targetRequirement`, which is where a
 * `TriggeredAbility` keeps it. That lift is the entire relationship between the two files, and it is
 * why adding a step rule makes every trigger rule richer for free.
 *
 * ### Self-reference is normalization's problem, not the grammar's
 *
 * The rules spell the source as `~`. Both printed spellings — the card's own name on older cards,
 * "this creature" on modern ones — are abstracted to that token by
 * [com.wingedsheep.assay.normalize.Normalizer], which restores the exact printed word afterwards.
 * The grammar therefore never has to know which noun a card's type line makes it print, and neither
 * spelling is privileged over the other.
 *
 * ### `AbilityId` is arbitrary, in exactly the way a target slot's name is
 *
 * `CardDefinition`s carry generated ids — Kavu Climber's golden says `"ability_1"` — that no printed
 * text determines. The grammar mints one fixed id and the differential normalizes both sides by
 * position, the same treatment target slot names get. A rule that tried to reproduce the id would be
 * reading a counter, not a card.
 */
object Triggers {

    /**
     * The id every parsed trigger carries.
     *
     * One constant rather than a generator: the printed text does not determine it, so any value is
     * as right as any other, and a fixed one keeps two parses of the same line equal. The
     * differential renames it by position before comparing.
     */
    private val ID = AbilityId("trigger")

    /**
     * A trigger's *when* clause, plus the effect vocabulary its payoff clause takes.
     *
     * The two halves of a trigger sentence, held apart because only one of them is reusable. A
     * `when` clause is a [TriggerSpec] and nothing else, so it can be slotted anywhere a sentence
     * names an event — once by [sentence], **twice** by [joinedRule], which is the whole reason this
     * type exists. What cannot travel inside it is which effect cascade the payoff takes: a trigger
     * whose event names an object of its own reads "it" as that object ([Steps.triggeredStep]) and
     * every other trigger reads it as the source ([Steps.step]), and that is a property of the event
     * rather than of the clause after the comma. So the cascade rides beside the prefix and
     * [sentence] is the one place the two meet.
     */
    private data class Prefix(val phrase: Phrase<TriggerSpec>, val effect: Phrase<CardScript>)

    /**
     * "when ~ enters, {effect}" — a whole triggered ability, from a prefix and its effect cascade.
     *
     * The `match` half is fail-closed the same way the step rules are: it reconstructs what `build`
     * would have produced from the ability's own effect and target and compares the whole thing, so
     * an ability carrying anything the phrase does not spell — an intervening-if condition, an
     * `elseEffect`, a graveyard `activeZones`, "you may", a once-per-turn cap — refuses to print
     * rather than printing a sentence that quietly drops it. Only the id is exempt, because the id
     * is not in the text.
     *
     * The event itself is read straight off the ability rather than compared against a constant,
     * and nothing is lost by that: `TriggerSpec` is exactly `(event, binding)`, so the spec a
     * printed ability denotes is total, and whether *this* prefix can spell it is the prefix's own
     * question — its `match` is what refuses. That split is what lets one reconstruction serve every
     * prefix in the file instead of one per family.
     */
    private fun sentence(prefix: Prefix): Phrase<TriggeredAbility> =
        phrase("{trigger}, {effect}", name = prefix.phrase.name) {
            slot("trigger", prefix.phrase)
            slot("effect", prefix.effect)
            build { abilityFor(it.value("trigger"), it.value("effect")) }
            match { ability ->
                val spec = specOf(ability)
                val script = scriptFor(ability)
                if (abilityFor(spec, script)?.copy(id = ability.id) != ability) return@match null
                bind("trigger" to spec, "effect" to script)
            }
        }

    /** A prefix whose event is a constant — "when ~ enters", "whenever ~ attacks". */
    private fun triggerRule(
        surface: String,
        spec: TriggerSpec,
        effect: Phrase<CardScript> = Steps.step,
    ): Prefix = Prefix(
        phrase(surface, name = surface) {
            build { spec }
            match { if (it == spec) bind() else null }
        },
        effect,
    )

    /** The spec a printed ability's `when` clause denotes. Total: a `TriggerSpec` is these two fields. */
    private fun specOf(ability: TriggeredAbility): TriggerSpec =
        TriggerSpec(ability.trigger, ability.binding)

    /**
     * Build the ability a trigger's effect clause denotes.
     *
     * **"You may …" needs nothing done to it here, and that is new.** A triggered ability used to
     * spell the controller's choice with an `optional` flag of its own while a spell spelled the
     * identical English as a `MayEffect`, so this function had to lower one into the other — one
     * sentence, two SDK spellings, and a rule per spelling would have been two readings of one text.
     * `TriggeredAbility.optional` is gone; the gate the engine always built from it is the model
     * now, and a trigger's effect clause is the same value a spell's clause is. The lowering, its
     * inverse in [scriptFor], and the differential fold that bridged the two spellings all deleted
     * together.
     */
    private fun abilityFor(spec: TriggerSpec, script: CardScript): TriggeredAbility? {
        val effect = script.spellEffect ?: return null
        return TriggeredAbility(
            id = ID,
            trigger = spec.event,
            binding = spec.binding,
            // CR 603.4's intervening-if is **lifted, not duplicated**. A condition printed between
            // the event and the effect belongs in `interveningIf`, which is the SDK's dedicated
            // slot for it, and the clause's own `Gate.WhenCondition` is then the same fact written a
            // second time — the thing this module's rule "a value the SDK carries twice is derived,
            // not spelled" exists to stop. So the gate is stripped from the effect exactly when it
            // is lifted, which is also what every hand-written card does. The differential reported
            // Beastbond Outcaster, Donatello and Phage the Untouchable while the gate was kept.
            //
            // `interveningIf` is the *only* condition slot this rule may write. Its sibling
            // `triggerRestriction` holds a CR 603.2 restriction on the trigger event — a "while"
            // clause, a "during your turn" narrowing — which is a different printed shape read by a
            // different rule, and which the engine deliberately never re-checks on resolution.
            // Leaving it null here is what makes [scriptFor] refuse to print an ability that
            // carries one: the reconstruction below compares the whole model, so a "while" card
            // declines rather than printing an "if" sentence that means something else.
            effect = liftInterveningIf(effect),
            interveningIf = interveningIf(effect),
            // A `TriggeredAbility` keeps its first requirement in a field of its own and the rest in
            // a list beside it, which is the shape a clause declaring two targets lands in —
            // Chromeshell Crab's exchange. The split is the SDK's; nothing in the text says it.
            targetRequirement = script.targetRequirements.firstOrNull(),
            additionalTargetRequirements = script.targetRequirements.drop(1),
            // CR 113.6m, the same derivation [Activated.abilityFor] makes: a trigger whose payoff
            // returns the source from a zone functions in that zone. `putsSourceInto` is the rule's
            // own "unless" — see [sourceLandsIn].
            activeZones = Recursion.functionsIn(effect, putsSourceInto = sourceLandsIn(spec))
                ?.let { setOf(it) }
                ?: setOf(Zone.BATTLEFIELD),
        )
    }

    /**
     * The zone this trigger's *event* puts the source into, or null when it does not move the source
     * at all — CR 113.6m's "unless its trigger condition … specifies that the object is put into
     * that zone".
     *
     * The rule without this clause reads the Ojer cycle backwards. "When Ojer Taq dies, return it to
     * the battlefield transformed" carries the same graveyard source as Bloodghast's landfall trigger
     * and functions on the battlefield, because the dies trigger is what put the card in the
     * graveyard; a derivation that ignored the event would make the ability wait in a graveyard it
     * can only reach by having already fired.
     *
     * Only a zone change *of the source* counts, which is what the binding decides: Bloodghast's
     * event is a land entering (`TriggerBinding.ANY`), so it says nothing about where this card is.
     */
    private fun sourceLandsIn(spec: TriggerSpec): Zone? {
        val zoneChange = spec.event as? EventPattern.ZoneChangeEvent ?: return null
        return zoneChange.to?.takeIf { spec.binding == TriggerBinding.SELF }
    }

    /**
     * The condition an intervening-if states, or null when the effect does not open with one.
     *
     * Only a *top-level* `Gate.WhenCondition` counts, and only where the gate is the whole of the
     * effect: "When ~ enters, if X, do Y." is an intervening-if, while a condition buried inside a
     * later clause of a sequence is an ordinary conditional that resolves once.
     */
    private fun interveningIf(effect: com.wingedsheep.sdk.scripting.effects.Effect): Condition? {
        val gated = effect as? GatedEffect ?: return null
        return (gated.gate as? Gate.WhenCondition)?.condition
    }

    /** The effect that is left once [interveningIf] has taken the condition out of it. */
    private fun liftInterveningIf(
        effect: com.wingedsheep.sdk.scripting.effects.Effect,
    ): com.wingedsheep.sdk.scripting.effects.Effect =
        if (interveningIf(effect) != null) (effect as GatedEffect).then else effect

    /**
     * The clause script an ability's effect and targets denote — the inverse of [abilityFor].
     *
     * One wrapper to put back, the intervening-if [abilityFor] lifted, so the round trip is over the
     * same value in both halves.
     */
    private fun scriptFor(ability: TriggeredAbility): CardScript = CardScript(
        // `interveningIf`, never the derived `triggerCondition`: the derivation folds in
        // `triggerRestriction` too, and printing that as an "if" clause would spell a "while"
        // card's trigger-time-only gate as a condition the engine re-checks on resolution — the
        // reversible-but-wrong class this module's fail-closed matching exists to catch.
        spellEffect = ability.interveningIf
            ?.let { ConditionalEffect(condition = it, effect = ability.effect) }
            ?: ability.effect,
        targetRequirements = listOfNotNull(ability.targetRequirement) +
            ability.additionalTargetRequirements,
    )

    /**
     * The trigger events with an unambiguous one-clause surface form.
     *
     * "When" versus "Whenever" is a property of the event rather than a choice: an event that
     * happens once to a permanent is templated "When", a repeatable one "Whenever". Baking the word
     * into each rule is what keeps one printed form per model.
     */
    /**
     * "At the beginning of your upkeep, …" — the step triggers, which are one family in the SDK
     * (`StepEvent(step, player)`) and one *rule* here.
     *
     * They are the same rule shape as the event triggers with a different prefix, which is the whole
     * reason [triggerRule] was written as a function: a step trigger's effect clause is the same
     * English a spell prints, so it slots [Steps.step] and inherits every step rule for free.
     *
     * What changed is the prefix. It used to be thirteen frozen surfaces, one per printed sentence,
     * each naming one of `dsl.Triggers`' constants — and those constants are calls to
     * `Triggers.phase(step, player, binding)` with every argument frozen. So the prefix is a
     * [Phases] slot now, and "at the beginning of each opponent's end step" stopped being a rule
     * nobody had written. Everything the family can say lives in that file; this is the one place it
     * meets an effect clause.
     */
    /**
     * "At the beginning of your upkeep, if ~ is in your graveyard, …" — Ghastly Remains.
     *
     * The clause is not a condition at all: it says *where the ability works from*, which the model
     * carries as `activeZones` on the ability rather than as an intervening-if. Reading it as a
     * condition would round-trip and mean a different card — an upkeep trigger that fires from the
     * battlefield and then checks something. So it is a prefix variant rather than a
     * [Conditions] row, and the zone it names is the rule's parameter.
     *
     * The step it fires on is [Phases.phase], the same slot the unzoned rule takes: the zone rider
     * and the step vocabulary are independent, and pinning this one to the upkeep would make the
     * next card that prints it on an end step a rule rather than a card.
     */
    private fun zonedTriggerRule(surface: String, zone: Zone): Phrase<TriggeredAbility> =
        phrase("at the beginning of {when}, $surface, {effect}", name = surface) {
            slot("when", Phases.phase)
            slot("effect", Steps.step)
            build {
                abilityFor(it.value("when"), it.value("effect"))?.copy(activeZones = setOf(zone))
            }
            match { ability ->
                if (ability.activeZones != setOf(zone)) return@match null
                val spec = stepSpec(specOf(ability)) ?: return@match null
                val script = scriptFor(ability)
                val rebuilt = abilityFor(spec, script)?.copy(id = ability.id, activeZones = setOf(zone))
                if (rebuilt != ability) return@match null
                bind("when" to spec, "effect" to script)
            }
        }

    /**
     * A spec narrowed to the step-trigger family, or null when the event is not a step.
     *
     * Only a *candidate*, in [slottedTriggerRule]'s sense: the reconstruction in [sentence]
     * compares the whole ability, and [Phases.phase] refuses to print a spec it cannot spell, so
     * nothing here has to decide whether the event is one this family covers.
     */
    private fun stepSpec(spec: TriggerSpec): TriggerSpec? =
        spec.takeIf { it.event is EventPattern.StepEvent }

    private val phasePrefixes: List<Prefix> = listOf(
        slottedTriggerRule(
            surface = "at the beginning of {when}",
            name = "a step trigger",
            noun = Phases.phase,
            effect = Steps.step,
            valueOf = ::stepSpec,
            spec = { it },
            slotName = "when",
        ),
    )

    /**
     * The same shape over a trigger whose event names a **filter** — "Whenever a Beast enters, …",
     * "Whenever another creature enters, …".
     *
     * Written as a function of the spec *builder* rather than of a fixed spec, which is the whole
     * difference from [triggerRule]: the noun phrase is a slot, so the event has to be reconstructed
     * from whatever the filter turns out to be before the fail-closed comparison can run. Everything
     * else — the lift of [Steps.step], the "you may" lowering, the id exemption — is identical, and
     * a filtered rule therefore inherits every effect rule exactly as an unfiltered one does.
     *
     * The binding is part of the *surface*: "a Beast" is `ANY` and "another Beast" is `OTHER`, and
     * the word "another" is the only thing in the text that says so. Two rows, not an optional
     * literal, so the model decides which prints.
     *
     * [noun] says which noun phrase the surface takes, and it is a property of the surface rather
     * than of the filter: "a Beast" carries its indefinite article and "another Beast" does not,
     * because "another" is already a determiner. [Filters.indefinite] owns the article in both
     * directions, so a rule that took the wrong one would print "another a Beast". A cast trigger
     * passes [Spells.indefinite] through the same parameter — its event names a *spell*, and the
     * noun phrase for one is a different vocabulary over the identical `GameObjectFilter`.
     */
    private fun filteredTriggerRule(
        surface: String,
        name: String,
        noun: Phrase<GameObjectFilter>,
        spec: (GameObjectFilter) -> TriggerSpec,
    ): Prefix =
        // [Steps.triggeredStep], not [Steps.step]: this trigger's event mentions an object of its
        // own, so "it" in the effect clause is that object rather than the source. See the
        // third-anaphor section on [SelfSteps]; the differential caught Tattered Ratter reading
        // "Whenever a Rat you control becomes blocked, it gets +2/+0" as pumping the *Ratter*.
        slottedTriggerRule(surface, name, noun, Steps.triggeredStep, { triggeredFilter(it.event) }, spec)

    /**
     * [filteredTriggerRule]'s shape, with the slot's *type* and the event's reader as parameters.
     *
     * The filtered rules read one `GameObjectFilter` off three event shapes through the shared
     * [triggeredFilter]; the batch family below reads one off a dozen, and several of those rows
     * need the value **stripped of the field the surface itself spells** before it can reach the
     * noun phrase — a batch subject's controller clause is a word in the sentence rather than a
     * layer of the noun, for the reason [Filters.pluralSubject] records. A shared `when` cannot do
     * that, because the stripping is a property of the *row* and not of the event type. So the
     * reader is a parameter, the slot's type follows it, and the counter family's `String` slot is
     * this same shape rather than a third copy of it.
     *
     * What the reader returns is still only a *candidate*: the reconstruction compares the whole
     * ability, so a row whose event carries a field the surface does not spell refuses to print
     * rather than printing a sentence that drops it.
     */
    private fun <T : Any> slottedTriggerRule(
        surface: String,
        name: String,
        noun: Phrase<T>,
        effect: Phrase<CardScript>,
        valueOf: (TriggerSpec) -> T?,
        spec: (T) -> TriggerSpec,
        // The slot's name is part of the *surface*, so it is a parameter rather than a constant: the
        // step triggers slot a `TriggerSpec` under `{when}`, and calling that "filter" would leave
        // every template in the file lying about what it holds.
        slotName: String = "filter",
    ): Prefix = Prefix(
        phrase(surface, name = name) {
            slot(slotName, noun)
            build { spec(it.value(slotName)) }
            match { triggerSpec ->
                val value = valueOf(triggerSpec) ?: return@match null
                // The row's own fail-closed step: an event carrying a field the surface does not
                // spell rebuilds to something else and refuses, rather than printing a sentence
                // that drops it. [sentence] then does the same over the rest of the ability.
                if (spec(value) != triggerSpec) return@match null
                bind(slotName to value)
            }
        },
        effect,
    )

    /**
     * The filter an event names, read back off the two event shapes the filtered rules produce.
     *
     * Only a *candidate*: the reconstructions in [slottedTriggerRule] and [sentence] are what
     * decide whether the whole ability is this sentence, so nothing here has to check the event's
     * other fields.
     */
    private fun triggeredFilter(event: EventPattern): GameObjectFilter? =
        when (event) {
            is EventPattern.ZoneChangeEvent -> event.filter
            is EventPattern.BecomesBlockedEvent -> event.filter
            is EventPattern.SpellCastEvent -> event.spellFilter
            else -> null
        }

    /**
     * "Whenever you cast a noncreature spell, …" — the spell-cast triggers.
     *
     * They are [filteredTriggerRule]s with [Spells.indefinite] in the noun slot rather than
     * [Filters.indefinite], and that substitution is the whole of the first three rows: the event's
     * filter is the same `GameObjectFilter` a battlefield trigger's is, and only the English for it
     * differs. Widening the rule's `article: Boolean` into a noun-phrase parameter is what let the
     * family be rows here instead of a second copy of the shape.
     *
     * **The caster is a parameter of the event and not of the noun.** "You", "an opponent" and "a
     * player" are `Player.You` / `EachOpponent` / `Each` on `SpellCastEvent`, so they are three rows
     * over one surface skeleton — not a subject vocabulary slotted into one rule, because a
     * `Player` in that position would also let the rule print "each player casts", which no card
     * writes.
     *
     * **The effect clause is [Steps.triggeredStep], for [filteredTriggerRule]'s reason.** This
     * trigger's event names an object of its own — the spell being cast — so "it" in the payoff is
     * that spell rather than the source, which is the third anaphor position exactly as a filtered
     * enters-trigger is. "When you cast this spell" is the one row that uses the *source* cascade
     * instead, and it has to: there the spell being cast **is** the source, so its "it" is the
     * ordinary self-anaphor and reading it as the triggering entity would be a second spelling of
     * one object.
     */
    private val castPrefixes: List<Prefix> = listOf(
        filteredTriggerRule(
            "whenever you cast {filter}", "whenever you cast a spell", Spells.indefinite,
        ) { SdkTriggers.youCastSpell(it) },
        filteredTriggerRule(
            "whenever an opponent casts {filter}", "whenever an opponent casts a spell", Spells.indefinite,
        ) { SdkTriggers.opponentCasts(it) },
        filteredTriggerRule(
            "whenever a player casts {filter}", "whenever a player casts a spell", Spells.indefinite,
        ) { SdkTriggers.anyPlayerCasts(it) },
        triggerRule("when you cast this spell", SdkTriggers.WhenYouCastThisSpell()),
        // "Adventure" is the one word in the spell noun's subtype slot that is not a
        // characteristic — CR 715.3 makes an Adventure spell one *cast as* an Adventure, which is
        // `SpellCastPredicate.CastAsAdventure` and not a subtype on the object. So the phrase is a
        // row of its own and [Spells.spellSubtype] refuses the word, which is what keeps one
        // printed form from having two models. See the leaf's KDoc for what the differential found.
        triggerRule(
            "whenever you cast an Adventure spell",
            SdkTriggers.youCastSpell(requires = setOf(SpellCastPredicate.CastAsAdventure)),
            effect = Steps.triggeredStep,
        ),
        nthCastRule("whenever you cast your", "whenever you cast your nth spell", Player.You),
        nthCastRule("whenever a player casts their", "whenever a player casts their nth spell", Player.Each),
    )

    /**
     * "Whenever you cast your second spell each turn, …" — the ordinal cast trigger.
     *
     * A rule of its own rather than a row above, because the event is a different SDK type:
     * `NthSpellCastEvent` counts a caster's spells within the turn where `SpellCastEvent` watches
     * every one. The possessive is part of the surface and tracks the subject — "you cast **your**
     * second", "a player casts **their** second" — so it is baked into each row's prefix rather than
     * being a vocabulary, for the same reason the caster is.
     *
     * **`GameObjectFilter.Any` is mapped to a null `spellFilter`, in both directions.** The SDK
     * spells "count every spell" as `null` here and as `Any` on `SpellCastEvent`, and all 24
     * hand-written ordinal triggers write the `null`. So the bare "spell" builds null and null reads
     * back as the bare noun, which makes an event carrying `Any` **unprintable** by this rule — the
     * fail-closed answer, since that value would be a second spelling of the same fact and nothing
     * in the text chooses between them.
     */
    private fun nthCastRule(prefix: String, name: String, player: Player): Prefix = Prefix(
        phrase("$prefix {ordinal} {filter} each turn", name = name) {
            slot("ordinal", Cardinals.ordinal)
            slot("filter", Spells.spell)
            build { bindings ->
                val filter = bindings.value<GameObjectFilter>("filter")
                SdkTriggers.NthSpellCast(
                    n = bindings.int("ordinal"),
                    player = player,
                    spellFilter = filter.takeIf { it != GameObjectFilter.Any },
                )
            }
            match { spec ->
                val event = spec.event as? EventPattern.NthSpellCastEvent ?: return@match null
                val filter = event.spellFilter ?: GameObjectFilter.Any
                // Rebuilt through the *build* half's mapping, not through `event.spellFilter`:
                // that is what makes an event carrying `Any` refuse to print rather than printing
                // the bare noun that means `null`.
                val rebuilt = SdkTriggers.NthSpellCast(
                    event.nthSpell,
                    player,
                    filter.takeIf { it != GameObjectFilter.Any },
                )
                if (rebuilt != spec) return@match null
                bind("ordinal" to event.nthSpell, "filter" to filter)
            }
        },
        Steps.triggeredStep,
    )

    // ---------------------------------------------------------------------------------------
    // Batch triggers — CR 603.2c's "one or more …"
    // ---------------------------------------------------------------------------------------

    /**
     * CR 603.2c: an ability triggers once each time its event occurs, and one event can hold
     * several occurrences — a sweeper killing four creatures, a multi-block, one resolution putting
     * counters on three permanents. Oracle templates that reading as "**one or more**", and the
     * distinction is not decoration: the per-object spelling of the same payoff fires four times
     * where this one fires once.
     *
     * `mtg-sdk` spells it as a dedicated `EventPattern` per family rather than as a flag on the
     * per-object one — `PermanentsEnteredEvent`, `CreaturesYouControlDiedEvent`,
     * `CardsLeftYourGraveyardEvent`, `OneOrMoreDealCombatDamageToPlayerEvent` and their siblings —
     * so the whole family is rows over [slottedTriggerRule] with a plural subject in the noun slot.
     * Nothing here is a new lowering; all but one of the events already publish a `dsl.Triggers`
     * facade, and the rows call it.
     *
     * ### The controller clause belongs to the sentence, not to the noun
     *
     * [Filters.pluralSubject] records why: these events fold an absent `controllerPredicate` to
     * "you control", so the bare plural does not mean what it means on the battlefield. Each scope
     * is therefore a row — three of them, generated from [SCOPES] — and the noun phrase in the slot
     * stops below the layer that owns the field. That also gives each family its "other" wording
     * for free, because `excludeSource` / `excludeSelf` is the second axis of the same product.
     *
     * ### The effect clause is [Steps.step] — a batch has no "it"
     *
     * The filtered trigger rules slot [Steps.triggeredStep] because their event names one object,
     * and "it" in the payoff is that object. A batch names a *set*: the engine hands the ability a
     * captured collection, and Oracle's payoffs say "them", "those creatures", "that many". None of
     * that vocabulary exists yet, so those lines decline — but reading them through the third
     * anaphor would be worse than declining, since it would quietly resolve a plural to whichever
     * member the engine happened to record. The source anaphor is the one that stays true: it is
     * what "Whenever one or more +1/+1 counters are put on ~, **it** gains menace" means, and it is
     * the only bare "it" the corpus prints after a batch trigger.
     *
     * ### Two write-offs with a stated reason
     *
     * **Attacks.** "Whenever one or more Merfolk you control attack" is `YouAttackEvent`, and the
     * family is left out rather than half-read for two reasons that would each cost more than the
     * rows are worth: 8 of its lines print "attack **a player**", which is a narrowing
     * (`AttackPredicate.DefenderIsPlayer` exists only on the per-creature `AttackEvent`) that the
     * batch event cannot carry — so the two English sentences would collapse to one model — and the
     * corpus writes the attacker filter's card type three different ways. A probe over the family's
     * declined lines puts the payoff at 6 readable lines out of 48; the rest are blocked on "that
     * many" and "them" regardless.
     *
     * **The missing facade.** `OneOrMoreDealCombatDamageToPlayerEvent` is the one event here with
     * no `dsl.Triggers` factory — the five hand-written cards that use it write the raw
     * `TriggerSpec`, and so does the row below, which keeps the grammar and the cards one
     * definition. Naming it would be the right change and it is an `mtg-sdk` one.
     */
    private data class Scope(val words: String, val predicate: ControllerPredicate?)

    /**
     * The three controller clauses a batch subject prints, and the predicate each denotes.
     *
     * "You control" is the *absent* predicate rather than `ControlledByYou`: the events fold the
     * two together and the corpus writes the absent one far more often, so spelling it would be a
     * second spelling of one value. `ControlledByYou` therefore has no printed form here and the
     * cards that carry it are reported — the same treatment `ManaColorSet.Specific` gets, and the
     * same membership check: a card in the minority whose line otherwise reads is a card to fix.
     */
    private val SCOPES: List<Scope> = listOf(
        Scope(" you control", null),
        Scope("", ControllerPredicate.ControlledByAny),
        Scope(" your opponents control", ControllerPredicate.ControlledByOpponent),
    )

    /** The event filter a subject printed under this scope denotes. */
    private fun Scope.scoped(subject: GameObjectFilter): GameObjectFilter =
        if (predicate == null) subject else subject.copy(controllerPredicate = predicate)

    /** …and its inverse — null when the filter is scoped to a different one of [SCOPES]. */
    private fun Scope.subjectOf(filter: GameObjectFilter): GameObjectFilter? =
        filter.takeIf { it.controllerPredicate == predicate }?.copy(controllerPredicate = null)

    /**
     * One batch family as its scope × "other" product — six rows from one sentence skeleton.
     *
     * [verb] is everything after the subject, [reader] pulls the event's own filter out (returning
     * null for any other event), and [exclusion] reads the family's own "other" flag. The `spec`
     * takes the scoped filter and the flag, so a family whose facade spells the opponent scope with
     * a factory of its own still goes through one row here — the two are the same value.
     */
    private fun batchProduct(
        verb: String,
        noun: String,
        reader: (EventPattern) -> Pair<GameObjectFilter, Boolean>?,
        spec: (GameObjectFilter, Boolean) -> TriggerSpec,
    ): List<Prefix> = SCOPES.flatMap { scope ->
        listOf(false, true).map { other ->
            val another = if (other) "other " else ""
            slottedTriggerRule(
                surface = "whenever one or more $another{filter}${scope.words} $verb",
                name = "whenever one or more $another$noun${scope.words} $verb",
                noun = Filters.pluralSubject,
                effect = Steps.step,
                valueOf = { spec ->
                    reader(spec.event)
                        ?.takeIf { (_, excluded) -> excluded == other }
                        ?.let { (filter, _) -> scope.subjectOf(filter) }
                },
                spec = { spec(scope.scoped(it), other) },
            )
        }
    }

    /** A batch family with no controller axis at all — the subject's scope is in the event's name. */
    private fun batchRule(
        surface: String,
        name: String,
        noun: Phrase<GameObjectFilter>,
        reader: (EventPattern) -> GameObjectFilter?,
        spec: (GameObjectFilter) -> TriggerSpec,
    ): Prefix =
        slottedTriggerRule(surface, name, noun, Steps.step, { reader(it.event) }, spec)

    /**
     * "Whenever one or more +1/+1 counters are put on ~, …" — the counter-placement batch.
     *
     * Two slots of different types, which is why it is a rule rather than a row: the counter *kind*
     * is a `String` on `CountersPlacedEvent` and the recipient is a `GameObjectFilter`. The
     * recipient slot is optional for [Triggers.pairedTriggerRule]'s reason — the self-bound wording
     * names no noun at all, and an event whose filter is anything but `Any` refuses to print
     * through it.
     *
     * `firstTimeEachTurn` is passed explicitly because the facade defaults it to `!batch`: that
     * default is the Stalwart Successor shape, whose printed rider these sentences do not carry,
     * and inheriting it would silently narrow every one of them.
     */
    private fun countersPlacedRule(
        surface: String,
        name: String,
        recipient: Phrase<GameObjectFilter>?,
        placedBy: Player?,
    ): Prefix {
        fun spec(kind: String, filter: GameObjectFilter) = SdkTriggers.countersPlacedOn(
            filter = filter,
            counterType = kind,
            firstTimeEachTurn = false,
            binding = if (recipient == null) TriggerBinding.SELF else TriggerBinding.ANY,
            placedBy = placedBy,
        )
        return Prefix(
            phrase(surface, name = name) {
                slot("kind", Primitives.counterKind)
                if (recipient != null) slot("recipient", recipient)
                build { bindings ->
                    val filter = if (recipient == null) GameObjectFilter.Any else bindings.value("recipient")
                    spec(bindings.value("kind"), filter)
                }
                match { triggerSpec ->
                    val event = triggerSpec.event as? EventPattern.CountersPlacedEvent ?: return@match null
                    if (spec(event.counterType, event.filter) != triggerSpec) return@match null
                    bind("kind" to event.counterType, "recipient" to event.filter)
                }
            },
            Steps.step,
        )
    }

    private val batchPrefixes: List<Prefix> =
        batchProduct(
            verb = "enter",
            noun = "permanents",
            reader = { (it as? EventPattern.PermanentsEnteredEvent)?.let { e -> e.filter to e.excludeSource } },
        ) { filter, other -> SdkTriggers.OneOrMorePermanentsEnter(filter, excludeSource = other) } +
        batchProduct(
            verb = "die",
            noun = "creatures",
            reader = { (it as? EventPattern.CreaturesYouControlDiedEvent)?.let { e -> e.filter to e.excludeSelf } },
        ) { filter, other -> SdkTriggers.OneOrMoreCreaturesYouControlDie(filter, excludeSelf = other) } +
        listOf(
            // The one row whose event has no facade; see this family's KDoc.
            batchRule(
                "whenever one or more {filter} you control deal combat damage to a player",
                "whenever one or more creatures you control deal combat damage to a player",
                Filters.pluralSubject,
                { (it as? EventPattern.OneOrMoreDealCombatDamageToPlayerEvent)?.sourceFilter },
            ) {
                TriggerSpec(
                    event = EventPattern.OneOrMoreDealCombatDamageToPlayerEvent(sourceFilter = it),
                    binding = TriggerBinding.ANY,
                )
            },
            batchRule(
                "whenever one or more {filter} deal combat damage to you",
                "whenever one or more creatures deal combat damage to you",
                Filters.pluralSubject,
                { (it as? EventPattern.OneOrMoreDealCombatDamageToYouEvent)?.sourceFilter },
            ) { SdkTriggers.OneOrMoreCreaturesDealCombatDamageToYou(it) },
            batchRule(
                "whenever one or more {filter} leave your graveyard",
                "whenever one or more cards leave your graveyard",
                Filters.pluralCards,
                { (it as? EventPattern.CardsLeftYourGraveyardEvent)?.filter },
            ) { SdkTriggers.CardsLeaveYourGraveyard(it) },
            batchRule(
                "whenever one or more {filter} are put into your graveyard from anywhere",
                "whenever one or more cards are put into your graveyard from anywhere",
                Filters.pluralCards,
                { (it as? EventPattern.CardsPutIntoYourGraveyardEvent)?.filter },
            ) { SdkTriggers.CardsPutIntoYourGraveyard(it) },
            // The library variant publishes only two fixed vals rather than a function of the
            // filter, so this row writes the `TriggerSpec` the way the two hand-written cards do.
            batchRule(
                "whenever one or more {filter} are put into your graveyard from your library",
                "whenever one or more cards are put into your graveyard from your library",
                Filters.pluralCards,
                { (it as? EventPattern.CardsPutIntoGraveyardFromLibraryEvent)?.filter },
            ) {
                TriggerSpec(
                    event = EventPattern.CardsPutIntoGraveyardFromLibraryEvent(filter = it),
                    binding = TriggerBinding.ANY,
                )
            },
            countersPlacedRule(
                "whenever one or more {kind} counters are put on ${Normalizer.SELF}",
                "whenever one or more counters are put on the source",
                recipient = null,
                placedBy = null,
            ),
            countersPlacedRule(
                "whenever one or more {kind} counters are put on {recipient}",
                "whenever one or more counters are put on a permanent",
                recipient = Filters.indefinite,
                placedBy = null,
            ),
            // The active voice is a *different* model rather than a second spelling: `placedBy`
            // asks who put them (CR 122.6), so "whenever **you** put" declines a placement by an
            // opponent that the passive sentence would fire on.
            countersPlacedRule(
                "whenever you put one or more {kind} counters on {recipient}",
                "whenever you put one or more counters on a permanent",
                recipient = Filters.indefinite,
                placedBy = Player.You,
            ),
            triggerRule("whenever one or more creatures attack you", SdkTriggers.CreaturesAttackYou),
            triggerRule(
                "whenever one or more of your opponents are attacked",
                SdkTriggers.CreaturesAttackYourOpponent,
            ),
        ) +
        listOf(false, true).map { other ->
            val another = if (other) "other " else ""
            batchRule(
                "whenever one or more $another{filter} you control leave the battlefield without dying",
                "whenever one or more ${another}creatures you control leave the battlefield without dying",
                Filters.pluralSubject,
                { trigger ->
                    (trigger as? EventPattern.LeaveBattlefieldWithoutDyingEvent)
                        ?.takeIf { it.excludeSelf == other }
                        ?.filter
                },
            ) { SdkTriggers.OneOrMoreLeaveWithoutDying(it, excludeSelf = other) }
        }

    private val eventPrefixes: List<Prefix> = listOf(
        triggerRule("when ${Normalizer.SELF} enters", SdkTriggers.EntersBattlefield),
        triggerRule("when ${Normalizer.SELF} dies", SdkTriggers.Dies),
        triggerRule("when ${Normalizer.SELF} leaves the battlefield", SdkTriggers.LeavesBattlefield),
        triggerRule("whenever ${Normalizer.SELF} attacks", SdkTriggers.Attacks),
        triggerRule("whenever ${Normalizer.SELF} blocks", SdkTriggers.Blocks),
        triggerRule("whenever ${Normalizer.SELF} becomes blocked", SdkTriggers.BecomesBlocked),
        triggerRule(
            "whenever ${Normalizer.SELF} deals combat damage to a player",
            SdkTriggers.DealsCombatDamageToPlayer,
        ),
        triggerRule(
            "whenever ${Normalizer.SELF} deals combat damage to a creature",
            SdkTriggers.DealsCombatDamageToCreature,
        ),
        // "Whenever this creature deals combat damage, …" — no recipient clause at all, which is a
        // third event rather than a shorter spelling of either of the two above: Drinker of Sorrow
        // triggers on damage to anything.
        triggerRule(
            "whenever ${Normalizer.SELF} deals combat damage",
            SdkTriggers.dealsDamage(damageType = DamageType.Combat),
        ),
        triggerRule("whenever ${Normalizer.SELF} is dealt damage", SdkTriggers.TakesDamage),
        // Valiant, and one row rather than a shape over `BecomesTargetEvent`'s six flags: the SDK
        // publishes the whole configuration as `Triggers.Valiant`, which is the lowering this file's
        // rule says to call rather than restate. The other flag combinations (by an opponent, a
        // filtered target, spells only) are separate printed sentences and become rows of their own
        // when a card needs them — not a template with the flags as slots, which would print
        // "for the first time each turn" as an optional phrase the model cannot decide.
        triggerRule(
            "whenever ${Normalizer.SELF} becomes the target of a spell or ability you control " +
                "for the first time each turn",
            SdkTriggers.Valiant,
        ),
        // Morph's payoff. "Is turned face up" is a `When` rather than a `Whenever` because it can
        // happen once to a permanent, which is the property that decides the word (see [rules]).
        triggerRule("when ${Normalizer.SELF} is turned face up", SdkTriggers.TurnedFaceUp),
        // Cycling's two triggers. `YouCycleThis` is the card's own cycling ("When you cycle this
        // card, …") and `AnyPlayerCycles` watches the table, so they are separate specs rather than
        // one with a player field — which is what the SDK says too.
        triggerRule("when you cycle ${Normalizer.SELF}", SdkTriggers.YouCycleThis),
        triggerRule("whenever a player cycles a card", SdkTriggers.AnyPlayerCycles),
        // Gift's payoff trigger (CR 702.174c) — Jolly Gerbils. `YouGiveAGift` is a whole
        // `TriggerSpec` the SDK publishes, so there is nothing to slot; the giver is baked into the
        // event as `Player.You` and no card prints another one.
        triggerRule("whenever you give a gift", SdkTriggers.YouGiveAGift),
        // The life-change triggers. `YouGainLife` / `YouLoseLife` are whole specs the SDK publishes,
        // so these are constants beside the events above rather than a shape over a player field —
        // "whenever an opponent gains life" is a different event and becomes its own row when a card
        // needs one. Their "during your turn" siblings (Wax-Wane Witness, Moonstone Harbinger) are
        // *not* here: that clause is a `triggerRestriction`, which [abilityFor] deliberately never
        // writes, and reading it as an intervening-if would mean a different card.
        triggerRule("whenever you gain life", SdkTriggers.YouGainLife),
        triggerRule("whenever you lose life", SdkTriggers.YouLoseLife),
        // Expend — "you spend your Nth total mana to cast spells this turn". A trigger event with
        // a number in it, so it is the [slottedTriggerRule] shape rather than a constant per
        // threshold: the corpus prints 4 and 8 and nothing in the sentence says those are the only
        // two. `Triggers.Expend(n)` freezes the watched player at `Player.You`, which is the only
        // subject Oracle prints, so the threshold is the rule's one slot — and it is [Primitives.cardinal] rather than
        // [Cardinals.word] because Oracle writes it as a numeral: "Whenever you expend **4**".
        slottedTriggerRule(
            surface = "whenever you expend {n}",
            name = "whenever you expend a number of mana",
            noun = Primitives.cardinal,
            effect = Steps.step,
            valueOf = { (it.event as? EventPattern.ExpendEvent)?.threshold },
            spec = { SdkTriggers.Expend(it) },
            slotName = "n",
        ),
        filteredTriggerRule(
            "whenever {filter} enters", "whenever a permanent enters", Filters.indefinite,
        ) { SdkTriggers.entersBattlefield(it, TriggerBinding.ANY) },
        filteredTriggerRule(
            "whenever another {filter} enters", "whenever another permanent enters", Filters.filter,
        ) { SdkTriggers.entersBattlefield(it, TriggerBinding.OTHER) },
        // "Whenever this creature or another Zombie enters" — Noxious Ghoul, Goblin Assassin. The
        // source is *in* the watched class, so the model is the plain `ANY` binding and the printed
        // "~ or another" is how Oracle spells that when the source matches the filter. It is a row
        // rather than a case inside the rule above because the two surfaces denote the same value
        // and only one of them may print: this one carries the noun the card prints.
        filteredTriggerRule(
            "whenever ${Normalizer.SELF} or another {filter} enters",
            "whenever the source or another permanent enters",
            Filters.filter,
        ) { SdkTriggers.entersBattlefield(it, TriggerBinding.ANY) },
        filteredTriggerRule(
            "whenever {filter} becomes blocked", "whenever a creature becomes blocked", Filters.indefinite,
        ) { SdkTriggers.becomesBlocked(it, TriggerBinding.ANY) },
        // "Whenever ~ or another creature dies, …" — Blood Artist, Skirk Drill Sergeant. One
        // ability with an `ANY` binding covers both halves, because the source is itself a member of
        // the watched class; the printed "~ or another" is how Oracle spells that, exactly as it is
        // in the enters rule above. Five hand-written cards write it as one ability and one writes
        // it as two, so the grammar emits the majority and the differential reports the rest.
        filteredTriggerRule(
            "whenever ${Normalizer.SELF} or another {filter} dies",
            "whenever the source or another permanent dies",
            Filters.filter,
        ) { SdkTriggers.leavesBattlefield(filter = it, to = Zone.GRAVEYARD, binding = TriggerBinding.ANY) },
    )

    /**
     * Every `when` clause the grammar reads, as the event it denotes.
     *
     * The vocabulary [joinedRule] slots on both sides of its "and", and the reason the prefix is a
     * value in this file rather than a string baked into forty templates: a rule that spelled the
     * prefixes a second time would agree with these until someone edited one of them, which is the
     * drift the kernel's [com.wingedsheep.assay.syntax.PhraseBuilder.alsoSpelled] exists to make
     * impossible one rule at a time and this list makes impossible across a whole family.
     */
    private val prefixes: List<Prefix> = eventPrefixes + castPrefixes + phasePrefixes + batchPrefixes

    /** The `when` clause vocabulary as one alternation, for the contexts that slot it. */
    private val event: Phrase<TriggerSpec> = oneOf("a trigger event", prefixes.map { it.phrase })

    /** Every trigger sentence without the cap [onceEachTurn] can put on one. */
    private val uncapped: Phrase<TriggeredAbility> = oneOf(
        "a triggered ability",
        prefixes.map(::sentence) +
            // The one trigger sentence that is not a prefix plus a payoff: its zone rider lands on
            // the *ability* rather than on the event, so it cannot be a [Prefix] and cannot be
            // joined. See [zonedTriggerRule].
            zonedTriggerRule("if ${Normalizer.SELF} is in your graveyard", Zone.GRAVEYARD),
    )

    /**
     * "…, draw a card. **This ability triggers only once each turn.**" — the printed trigger cap.
     *
     * A wrapper rather than a row in every family, which is the whole point: it is a rider on the
     * *ability* (`TriggeredAbility.oncePerTurn`) and not part of any event, so one rule reaches
     * every trigger sentence the grammar can read — and it had to, because the fail-closed
     * reconstruction each family does compares the whole model, so until now a capped ability
     * refused to print and every card carrying the rider declined. 49 of the batch family's own
     * lines carry it; so do a hundred-odd elsewhere.
     *
     * It is the *trigger* cap, spent by the first trigger whether or not anything came of it — not
     * `effectOncePerTurn`, which is the "Do this only once each turn." rider CR 603.2h defines over
     * the action taken on resolution. The SDK keeps them as two fields for exactly that reason and
     * the two English sentences are different, so this rule prints one of them and the other keeps
     * declining until someone writes its row.
     *
     * The rider is a **second sentence**, and the template says so by putting no full stop of its
     * own in front of the slot: [Steps.step] is `"{clause}."` — a sentence spells the stop that ends
     * it — so the trigger has already consumed the one after its payoff, and what is left to match
     * is a space and a sentence of its own. Its first letter is lowercase here because
     * `SentenceCase` lowercases the clause after every full stop; see the case section in this
     * module's AGENTS.md.
     */
    private val onceEachTurn: Phrase<TriggeredAbility> =
        phrase("{trigger} this ability triggers only once each turn.", name = "a trigger capped at once each turn") {
            slot("trigger", uncapped)
            build { it.value<TriggeredAbility>("trigger").copy(oncePerTurn = true) }
            match { ability ->
                if (!ability.oncePerTurn) return@match null
                bind("trigger" to ability.copy(oncePerTurn = false))
            }
        }

    val trigger: Phrase<TriggeredAbility> = oneOf("a triggered ability", uncapped, onceEachTurn)

    /** One trigger, lifted into the one-element list a line usually denotes. */
    private val single: Phrase<List<TriggeredAbility>> = phrase("{one}", name = "a triggered ability") {
        slot("one", trigger)
        build { listOf(it.value<TriggeredAbility>("one")) }
        match { it.singleOrNull()?.let { ability -> bind("one" to ability) } }
    }

    /**
     * "Whenever ~ attacks or blocks, …" — **two** triggered abilities from one printed sentence.
     *
     * A `TriggeredAbility` watches one event, and Oracle's "or" here joins two: attacking and
     * blocking are different events with the same payoff, so the card carries two abilities and
     * Embalmed Brawler's golden says so in a comment. That makes this the trigger side of
     * [Keywords.qualityRun] — a rule that denotes several models from one phrase — and the reason a
     * trigger *line* is a list rather than one ability.
     *
     * The two specs are a parameter rather than a slot because the joined phrase is one printed
     * form: "attacks or blocks" is not "attacks" plus a word, and the pairs Oracle joins this way
     * are a short measured list ([pairedRules]) rather than the cross product of the self-events.
     *
     * [alsoSpelled] carries the second surface of a pair whose two spellings denote one model —
     * "When" for "Whenever", "is put into a graveyard from the battlefield" for "dies". It shares
     * this rule's `build` and `match` by construction, which is the whole reason the kernel has it:
     * a copied pair of halves agrees until someone edits one of them.
     */
    private fun pairedTriggerRule(
        surface: String,
        name: String,
        specs: (GameObjectFilter?) -> List<TriggerSpec>,
        filtered: Boolean,
        alternateSurfaces: List<String> = emptyList(),
    ): Phrase<List<TriggeredAbility>> =
        phrase("$surface, {effect}", name = name) {
            alternateSurfaces.forEach { alsoSpelled("$it, {effect}", "$it (alternate spelling)") }
            if (filtered) slot("filter", Filters.filter)
            slot("effect", Steps.step)
            build { bindings ->
                val script = bindings.value<CardScript>("effect")
                val built = specs(if (filtered) bindings.value("filter") else null)
                    .map { abilityFor(it, script) }
                if (built.any { it == null }) null else built.filterNotNull()
            }
            match { abilities ->
                if (abilities.size != 2) return@match null
                val script = scriptFor(abilities.first())
                val filter = if (filtered) triggeredFilter(abilities[1].trigger) ?: return@match null else null
                val rebuilt = specs(filter).map { abilityFor(it, script) }
                if (rebuilt.size != abilities.size) return@match null
                val matches = rebuilt.zip(abilities).all { (built, ability) ->
                    built?.copy(id = ability.id) == ability
                }
                if (!matches) return@match null
                bind("filter" to filter, "effect" to script)
            }
        }

    /** One row of [contractions]: the pair, the sentence that contracts it, and its older spellings. */
    private data class Contraction(
        val surface: String,
        val name: String,
        val specs: List<TriggerSpec>,
        val alsoSpelled: List<String> = emptyList(),
    )

    /**
     * The pairs Oracle joins with "or", one row each.
     *
     * Measured rather than enumerated: these are the joins the corpus actually prints over the
     * self-events this file already reads, in the order of how many lines each carries — enters or
     * attacks (164), attacks or blocks (41 plus 14 spelling it "When"), enters or dies (25 plus 6
     * spelling it CR 700.4's long form), enters or leaves the battlefield (14), enters or is turned
     * face up (7). A cross product of the self-event vocabulary would be forty-odd rules for five
     * sentences, and most of the products it invented are not English anyone has printed.
     *
     * **The order of the two abilities is the sentence's order**, and the fail-closed `match` in
     * [pairedTriggerRule] holds it there: a card that writes the same pair the other way round
     * declines rather than printing a sentence that reorders it. Every hand-written card checked
     * writes them as the text does (Queen's Bay Paladin, Ponyback Brigade, Mardu Blazebringer).
     *
     * The joins left out and why: "or specializes" (5) and "or the creature it haunts dies" (7) name
     * events with no `TriggerSpec`; "or transforms into <name>" (7) names one per card; and
     * "blocks or becomes blocked by …" (39) is not a pair of self-events but a filtered second
     * event, so it belongs to a rule that can slot the filter rather than to this list.
     *
     * The list is *read* as well as printed from: [joinedRule] is the general form of the same
     * model, and it declines exactly the pairs listed here, so which of the two sentences prints is
     * decided by the pair rather than by an alternation's order. That is why the pairs live in a data
     * list and not inside the rule that spells them.
     */
    private val contractions: List<Contraction> = listOf(
        Contraction(
            "whenever ${Normalizer.SELF} enters or attacks",
            "whenever the source enters or attacks",
            listOf(SdkTriggers.EntersBattlefield, SdkTriggers.Attacks),
        ),
        Contraction(
            "whenever ${Normalizer.SELF} attacks or blocks",
            "whenever the source attacks or blocks",
            listOf(SdkTriggers.Attacks, SdkTriggers.Blocks),
            // Fourteen older cards spell the same two events with "When" — Mardu Blazebringer,
            // Windscouter, Ceremonial Guard. One model, so one of the two prints.
            alsoSpelled = listOf("when ${Normalizer.SELF} attacks or blocks"),
        ),
        Contraction(
            "when ${Normalizer.SELF} enters or dies",
            "when the source enters or dies",
            listOf(SdkTriggers.EntersBattlefield, SdkTriggers.Dies),
            // CR 700.4 defines "dies" as exactly this, and the artifact cycle that predates the
            // word spells it out — Ichor Wellspring, Mycosynth Wellspring, Prized Statue.
            alsoSpelled = listOf(
                "when ${Normalizer.SELF} enters or is put into a graveyard from the battlefield",
            ),
        ),
        Contraction(
            "when ${Normalizer.SELF} enters or leaves the battlefield",
            "when the source enters or leaves the battlefield",
            listOf(SdkTriggers.EntersBattlefield, SdkTriggers.LeavesBattlefield),
        ),
        Contraction(
            "when ${Normalizer.SELF} enters or is turned face up",
            "when the source enters or is turned face up",
            listOf(SdkTriggers.EntersBattlefield, SdkTriggers.TurnedFaceUp),
        ),
    )

    private val pairedRules: List<Phrase<List<TriggeredAbility>>> = contractions.map { contraction ->
        pairedTriggerRule(
            contraction.surface,
            contraction.name,
            specs = { contraction.specs },
            filtered = false,
            alternateSurfaces = contraction.alsoSpelled,
        )
    }

    /**
     * "When ~ enters **and** whenever you cast a spell with mana value 5 or greater, draw a card."
     * — the general join: two `when` clauses, one payoff, two abilities.
     *
     * The same model [pairedTriggerRule] produces and the opposite way of getting there. That rule
     * is a *counted table* — Oracle contracts five pairs of self-events into one trigger word, and a
     * cross product of the vocabulary would have been forty-odd rules for five sentences. This one
     * is the product, and it is the product because the printed sentence *is* one: each half repeats
     * its own trigger word ("and whenever …", "and at the beginning of …", "and when …"), so the two
     * halves are complete clauses drawn from the same vocabulary rather than one clause with a word
     * added. Slotting [event] twice is therefore the whole rule, and every trigger family the
     * grammar learns to read becomes a legal half of it without being told — 112 corpus lines today,
     * 31 of them opening with "When ~ enters".
     *
     * **The payoff is [Steps.step], and for a join that is the only sound reading.** The filtered
     * rules slot [Steps.triggeredStep] because their one event names one object, so "it" is that
     * object; here two different events share one effect, and an "it" resolved to a triggering
     * object would mean a different thing under each of them. The source anaphor is the one that
     * stays true for both — "When ~ enters and whenever you expend 4, put a stash counter on **it**"
     * is Hoarder's Overflow, and it means the source.
     *
     * **A pair [contractions] owns is declined in both directions**, by [contracted] rather than by
     * this alternation's position, because "one printed form per model" is a property of the model:
     * `[EntersBattlefield, Dies]` prints "when ~ enters or dies" and must not also be printable as
     * "when ~ enters and when ~ dies". No corpus line writes a contracted pair the long way, so the
     * guard costs nothing and is what keeps the printed form out of `oneOf`'s hands.
     *
     * A join of an event with *itself* is declined for the same reason and a simpler one: it denotes
     * two identical abilities, which is a shape no card prints and one that any single-trigger line
     * appearing twice on a card already denotes.
     */
    private val joinedRule: Phrase<List<TriggeredAbility>> =
        phrase("{first} and {second}, {effect}", name = "two trigger events with one payoff") {
            slot("first", event)
            slot("second", event)
            slot("effect", Steps.step)
            build { bindings ->
                val specs = listOf<TriggerSpec>(bindings.value("first"), bindings.value("second"))
                if (!joinable(specs)) return@build null
                val script = bindings.value<CardScript>("effect")
                val built = specs.map { abilityFor(it, script) }
                if (built.any { it == null }) null else built.filterNotNull()
            }
            match { abilities ->
                if (abilities.size != 2) return@match null
                val specs = abilities.map(::specOf)
                if (!joinable(specs)) return@match null
                val script = scriptFor(abilities.first())
                val rebuilt = specs.map { abilityFor(it, script) }
                val holds = rebuilt.zip(abilities).all { (built, ability) ->
                    built?.copy(id = ability.id) == ability
                }
                if (!holds) return@match null
                bind("first" to specs[0], "second" to specs[1], "effect" to script)
            }
        }

    /** Whether a pair of events is one this sentence may spell; see [joinedRule]. */
    private fun joinable(specs: List<TriggerSpec>): Boolean =
        specs[0] != specs[1] && contractions.none { it.specs == specs }

    /**
     * Every trigger line, as the list of abilities it denotes.
     *
     * [single] is exactly one ability and both two-ability rules are exactly two, so the sizes
     * separate the one-ability case from the rest. Between [pairedRules] and [joinedRule] the model
     * decides too: a pair [contractions] lists prints only through the paired rule, and every other
     * pair only through the join. So printing is determined by the model rather than by the
     * alternation's order, the property every `oneOf` in this grammar is written to have.
     */
    val line: Phrase<List<TriggeredAbility>> = oneOf(
        "a triggered ability line",
        pairedRules + joinedRule + single,
    )
}
