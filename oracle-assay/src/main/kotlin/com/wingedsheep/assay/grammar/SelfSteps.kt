package com.wingedsheep.assay.grammar

import com.wingedsheep.assay.syntax.Phrase
import com.wingedsheep.assay.syntax.alternate
import com.wingedsheep.assay.syntax.bind
import com.wingedsheep.assay.syntax.phrase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.costs.PayCost
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.BecomeCreatureEffect
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.Gate
import com.wingedsheep.sdk.scripting.effects.GatedEffect
import com.wingedsheep.sdk.scripting.effects.PayOrSufferEffect
import com.wingedsheep.sdk.scripting.effects.RegenerateEffect
import com.wingedsheep.sdk.scripting.effects.RemoveKeywordEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Clauses about the **source** — "it gets +2/+0 until end of turn.", "put it on top of its owner's
 * library.", "sacrifice it unless you discard a land card."
 *
 * Oracle's "it" here is the permanent whose ability this is, which is [EffectTarget.Self] and needs
 * no earlier sentence to introduce it — so unlike [Continuations] these are ordinary clauses that
 * can stand alone. The two anaphors are kept in separate vocabularies precisely because they point
 * at different things: "that creature" is the target the spell already chose, "it" is the source.
 *
 * Almost every card that prints one of these prints it inside a triggered ability ("When this
 * creature dies, put it on top of its owner's library"), and none of these rules knows that:
 * [Triggers] slots [Steps.step] whole, so the clause is the same clause wherever it lands.
 *
 * ### The third anaphor
 *
 * That last sentence holds everywhere but one place. A trigger whose event names a **filter**
 * mentions an object of its own, and English resolves "it" to the most recent mention: "Whenever a
 * Rat you control becomes blocked, **it** gets +2/+0" pumps the *Rat*, not the source. So the same
 * clause denotes [EffectTarget.TriggeringEntity] there and [EffectTarget.Self] everywhere else,
 * while the *name* still denotes the source in both.
 *
 * The vocabulary is therefore written once, as [retargetable], and instantiated per position rather
 * than copied — the members, their models and their fail-closed matches are one piece of code, and
 * only the subject's spelling and the target it denotes move. [Steps.step] takes the source reading
 * and [Steps.triggeredStep] takes both, which is what keeps one printed form per model: in the
 * filtered-trigger cascade the name reads the source and the pronoun reads the triggering entity, so
 * the two surfaces are disjoint and neither can print the other's sentence.
 */
object SelfSteps {

    /**
     * The clauses whose object can be *any* single permanent the sentence has already fixed.
     *
     * One shape, instantiated per anaphor position. [subject] is what stands in a `{self}` slot and
     * [target] is what the whole clause acts on; the two move together, which is the entire content
     * of the third-anaphor split described on this object.
     *
     * **Every member states its subject in a slot, and that is load-bearing.** Four of them used to
     * spell the pronoun as literal template text — "exile **it**", "put **it** on top of its
     * owner's library" — which needed a `pronominal` flag to keep them out of the positions that
     * read the name, and cost the grammar every line that prints the noun instead: "Exile ~." was
     * unreadable on twenty-nine cards for no reason but a frozen word. A subject that is a slot is
     * what makes one position's spelling another position's, so the flag is gone and the four rows
     * are ordinary members.
     *
     * @param tag distinguishes the rule names across instantiations so an ambiguity diagnostic can
     *   still say which side it found.
     */
    fun retargetable(
        target: EffectTarget,
        subject: Phrase<Unit>,
        tag: String,
    ): List<Phrase<CardScript>> {
        val named = listOf(
            selfGets(target, subject, tag),
            selfGetsAndGains(target, subject, tag),
            selfGainsKeywords(target, subject, tag),
            selfLosesKeyword(target, subject, tag),
            move("untap {self}", "untap$tag", Effects.Untap(target), subject),
            // Untap's twin, and a row for exactly the reason untap is one: "Target creature gets
            // -1/-1 until end of turn. Tap that creature." (Stabbing Pain) is the same shape said of
            // the other verb, and a family that reads one and not the other is the count sitting in
            // the rule instead of in the slot.
            move("tap {self}", "tap$tag", Effects.Tap(target), subject),
            move("regenerate {self}", "regenerate$tag", RegenerateEffect(target), subject),
            // "Transform ~." — CR 701.28, the verb a double-faced permanent's own ability uses on
            // itself: the daybound/nightbound upkeep triggers (62 lines), the "{5}{G}{G}: Transform
            // ~." activated flips, and every Innistrad front face that turns over on a condition.
            // A row rather than a rule of its own because its object is an ordinary [EffectTarget]
            // that moves with the position exactly as untap's and regenerate's do — unlike
            // `SacrificeSelfEffect`, which the SDK models as a verb with no object at all.
            move("transform {self}", "transform$tag", TransformEffect(target), subject),
            // The four zone verbs the pronoun used to be frozen into. "Exile ~." is the standalone
            // sentence twenty-nine spells print about themselves and "exile it" is what the same
            // verb looks like after a clause has already named the source; one rule, one model, and
            // the subject slot is the only difference between them.
            move("exile {self}", "exile$tag", Effects.Move(target, Zone.EXILE), subject),
            move(
                "put {self} on top of its owner's library",
                "put$tag on top of its library",
                Effects.PutOnTopOfLibrary(target),
                subject,
            ),
            move(
                "shuffle {self} into its owner's library",
                "shuffle$tag into its library",
                Effects.Move(target, Zone.LIBRARY, ZonePlacement.Shuffled),
                subject,
            ),
            move(
                "return {self} to its owner's hand",
                "return$tag to its owner's hand",
                Effects.Move(target, Zone.HAND),
                subject,
            ),
            // "…return it to your hand." — Ghastly Remains. The same move: a card returning itself
            // goes to its owner's hand, and the owner of a card you are returning from your own
            // graveyard is you. Two printed forms, one model, so this one parses and never prints.
            alternate(
                move(
                    "return {self} to your hand",
                    "return$tag to your hand",
                    Effects.Move(target, Zone.HAND),
                    subject,
                )
            ),
        ) + putCounters(target, subject, tag) + selfAnimates(target, subject, tag) +
            // "{2}{U}: ~ can't be blocked this turn." — the durational evasion, whose whole family
            // lives in [Combat] beside the combat statics it is the spell-side sibling of. It is a
            // member here rather than a clause of its own because its object moves with every other
            // member's: 24 printed lines say it about the source, and being a clause is also what
            // lets "~ gets +1/+0 until end of turn and can't be blocked this turn." read as the two
            // clauses it is.
            Combat.restrictionClauses(target, subject, surface = "{self}", tag = tag)
        return named
    }

    /**
     * "Put a +1/+1 counter on ~.", "Put two +1/+1 counters on it." — the counter verb aimed at the
     * source, and the single commonest effect shape in the whole hand-written corpus: 363 of the
     * 951 `AddCounters` a golden carries are exactly this one.
     *
     * The subject is a slot, so both spellings read and the name is what prints, the same treatment
     * [selfGets] gets. Being in [anaphoric] is what makes the pronoun safe: [Steps] drops this whole
     * list from every position after the first in a sequence, so once a clause has introduced a
     * target, "on it" is [Continuations]' to read and means that target. Registering the pronoun in
     * both places would be two readings of one text — the bug the differential caught on "Untap
     * target creature. It gets +2/+4", in a sentence where it would be just as invisible.
     *
     * Singular and plural are two rules over disjoint quantities for [Steps]' reason; everything
     * about why is written there, on the targeted twin of this pair.
     */
    private fun putCounters(
        target: EffectTarget,
        subject: Phrase<Unit>,
        tag: String,
    ): List<Phrase<CardScript>> {
        fun scriptFor(kind: CounterType, count: Int) =
            CardScript(spellEffect = Effects.AddCounters(kind, count, target))
        fun rule(template: String, name: String, quantity: Phrase<*>?) =
            phrase(template, name = name) {
                slot("kind", if (quantity == null) Primitives.singularCounterKind else Primitives.counterKind)
                if (quantity != null) slot("n", quantity)
                slot("self", subject)
                build { scriptFor(it.value("kind"), if (quantity == null) 1 else it.int("n")) }
                match { script ->
                    val (kind, count) =
                        Steps.countersAdded(script.spellEffect, target) ?: return@match null
                    if (quantity == null && count != 1) return@match null
                    if (quantity != null && !(count >= 2 && Cardinals.spellable(count))) return@match null
                    if (script != scriptFor(kind, count)) return@match null
                    bind("kind" to kind, "n" to count, "self" to Unit)
                }
            }
        // The count named by a trailing clause instead of by a number word. One rule, both of
        // Oracle's spellings, over the SDK's dynamic counter effect — and no bare-"X" row, for the
        // reason [Amounts.namesX] gives: this clause is one [Triggers] lifts, and the announced X is
        // silently zero anywhere it lands but a spell.
        fun dynamicScriptFor(kind: CounterType, amount: DynamicAmount) =
            CardScript(spellEffect = Effects.AddDynamicCounters(kind, amount, target))
        val defined = phrase<CardScript>(
            "put X {kind} counters on {self}${Amounts.WHERE_X}",
            name = "put a counted number of counters on$tag",
        ) {
            definedByCount()
            slot("kind", Primitives.counterKind)
            slot("self", subject)
            slot("amount", Amounts.count)
            build {
                val amount = it.value<DynamicAmount>("amount")
                if (Amounts.namesX(amount)) dynamicScriptFor(it.value("kind"), amount) else null
            }
            match { script ->
                val (kind, amount) =
                    Steps.dynamicCountersAdded(script.spellEffect, target) ?: return@match null
                if (!Amounts.namesX(amount)) return@match null
                if (script != dynamicScriptFor(kind, amount)) return@match null
                bind("kind" to kind, "amount" to amount, "self" to Unit)
            }
        }
        return listOf(
            rule("put {kind} counter on {self}", "put a counter on$tag", null),
            rule("put {n} {kind} counters on {self}", "put counters on$tag", Cardinals.word),
            defined,
        )
    }

    /**
     * "This creature gets +1/+1 until end of turn." — firebreathing's effect clause, and Charging
     * Bandits' attack trigger spelled with the pronoun.
     *
     * The subject is a slot, so both of Oracle's spellings read and the noun is what prints. That
     * ordering is the corpus's: a card *naming* itself is how nearly every activated pump is
     * templated ("{R}: This creature gets +1/+0 until end of turn."), while the pronoun only appears
     * where an earlier clause in the same ability already named the source. Cards printing the
     * pronoun come back as a [com.wingedsheep.assay.gate.LineVerdict.VARIANT], which says the
     * reading was right and only the spelling moved.
     */
    private fun selfGets(target: EffectTarget, subject: Phrase<Unit>, tag: String): Phrase<CardScript> {
        fun scriptFor(modifiers: Pair<Int, Int>) = CardScript(
            spellEffect = Effects.ModifyStats(modifiers.first, modifiers.second, target)
        )
        return phrase("{self} gets {mod} until end of turn", name = "$tag gets".trim()) {
            frontedDuration()
            slot("self", subject)
            slot("mod", Primitives.statModifiers)
            build { scriptFor(it.value("mod")) }
            match { script ->
                val modifiers = Steps.fixedModifiers(script.spellEffect) ?: return@match null
                if (script != scriptFor(modifiers)) return@match null
                bind("self" to Unit, "mod" to modifiers)
            }
        }
    }

    /**
     * "This creature gets +2/+2 and gains trample until end of turn." — Clickslither, Glintwing
     * Invoker, Unstable Hulk.
     *
     * [Steps.pumpAndGrantTarget]'s source-side twin, and one rule for the same reason: the second
     * clause has no subject of its own in the text, and the model is a two-element composite over
     * one object. A [Steps.sequence] would need the second clause to name what it acts on.
     */
    private fun selfGetsAndGains(
        target: EffectTarget,
        subject: Phrase<Unit>,
        tag: String,
    ): Phrase<CardScript> {
        fun scriptFor(modifiers: Pair<Int, Int>, keywords: List<Keyword>) = CardScript(
            spellEffect = Effects.Composite(
                listOf(Effects.ModifyStats(modifiers.first, modifiers.second, target)) +
                    keywords.map { Effects.GrantKeyword(it, target) }
            )
        )
        return phrase(
            "{self} gets {mod} and gains {kws} until end of turn",
            name = "$tag gets and gains".trim(),
        ) {
            frontedDuration()
            slot("self", subject)
            slot("mod", Primitives.statModifiers)
            slot("kws", Keywords.keywordRun)
            build { scriptFor(it.value("mod"), it.value("kws")) }
            match { script ->
                val effects = (script.spellEffect as? CompositeEffect)?.effects ?: return@match null
                val modifiers = Steps.fixedModifiers(effects.firstOrNull()) ?: return@match null
                val keywords = Steps.grantedKeywords(effects.drop(1)) ?: return@match null
                if (script != scriptFor(modifiers, keywords)) return@match null
                bind("self" to Unit, "mod" to modifiers, "kws" to keywords)
            }
        }
    }

    /**
     * "~ gains trample until end of turn.", "~ gains flying and shroud until end of turn." — Warped
     * Researcher and every card that grants itself a run.
     *
     * One rule over [Keywords.keywordRun] rather than one per count. It used to be a two-keyword
     * rule with no singular sibling, which is the shape a family looks like before it is one: the
     * count was in the rule instead of in the slot, so "gains trample" had nowhere to parse.
     */
    private fun selfGainsKeywords(
        target: EffectTarget,
        subject: Phrase<Unit>,
        tag: String,
    ): Phrase<CardScript> {
        fun scriptFor(keywords: List<Keyword>) = CardScript(spellEffect = Steps.grants(keywords, target))
        return phrase(
            "{self} gains {kws} until end of turn",
            name = "$tag gains keywords".trim(),
        ) {
            frontedDuration()
            slot("self", subject)
            slot("kws", Keywords.keywordRun)
            build { scriptFor(it.value("kws")) }
            match { script ->
                val keywords = Steps.grantedKeywords(script.spellEffect) ?: return@match null
                if (script != scriptFor(keywords)) return@match null
                bind("self" to Unit, "kws" to keywords)
            }
        }
    }

    /**
     * "**This artifact becomes a 3/3 Vampire artifact creature with haste until end of turn.**" —
     * Sanguine Statuette, Sanguine Brushstroke, and the self side of the animate.
     *
     * [Steps.animateTargetPermanent]'s payload with the **token noun phrase** instead of the
     * "with base power and toughness 5/5" clause, and the two are two rules because the P/T changes
     * *position*: Oracle puts it in front of the type when the sentence names the resulting creature
     * as a whole ("a 3/3 Vampire artifact creature") and behind it when the sentence modifies an
     * existing permanent ("target creature becomes a blue Serpent with base power and toughness
     * 5/5"). Neither template can be derived from the other, which is exactly [Steps.countedStepPair]'s
     * criterion for two strings.
     *
     * ### "artifact" is a model field, and the corpus already writes it that way
     *
     * "a 3/3 Vampire **artifact** creature" names the types the permanent ends up with, and the line
     * grammar has no type line to derive the word from — a rule that treated it as ornament would
     * decline every card that prints it, which is the same wall the gift line hit. So it is a row of
     * an omissible layer over `addTypes`, the field Relic's Roar and Phantom Train already use for
     * this word. On a permanent that is already an artifact the value is a no-op union, which is
     * what makes the reading safe as well as literal.
     *
     * ### One keyword, one creature type, one colour
     *
     * All three are `Set`s on `BecomeCreatureEffect` and a set has no order for a printer to
     * recover, so this rule reads exactly one of each and a two-keyword animate declines —
     * [Steps.animateTargetPermanent]'s finding, unchanged, and the honest verdict rather than a
     * guess at which word leads.
     *
     * ### The "may" form is a variant of this shape rather than a wrapper
     *
     * [Steps]' `you may {inner}` spells a clause that states no subject; this one states one, and
     * English contracts the two into the causative "you may **have** ~ **become** …" rather than
     * repeating them. That is [Steps.mayCountedStep]'s contraction, one family over: both spellings
     * are generated from one call site so the pair cannot drift, and the model is the same
     * `Effects.May` either way.
     */
    private fun selfAnimate(
        target: EffectTarget,
        subject: Phrase<Unit>,
        tag: String,
        coloured: Boolean,
        artifact: Boolean,
        keyworded: Boolean,
        may: Boolean,
    ): Phrase<CardScript> {
        fun scriptFor(
            stats: Pair<Int, Int>,
            colour: Color?,
            type: Subtype,
            keyword: Keyword?,
        ): CardScript {
            val animate = Effects.BecomeCreature(
                target = target,
                power = stats.first,
                toughness = stats.second,
                keywords = setOfNotNull(keyword),
                creatureTypes = setOf(type.value),
                addTypes = if (artifact) setOf("ARTIFACT") else emptySet(),
                colors = colour?.let { setOf(it.name) },
                duration = Duration.EndOfTurn,
            )
            return CardScript(spellEffect = if (may) Effects.May(animate) else animate)
        }

        val noun = "a {p}/{t} " +
            (if (coloured) "{colour} " else "") +
            "{type} " +
            (if (artifact) "artifact " else "") +
            "creature" +
            (if (keyworded) " with {kw}" else "")
        val template = if (may) {
            "you may have {self} become $noun until end of turn"
        } else {
            "{self} becomes $noun until end of turn"
        }
        val name = buildString {
            append(tag.trim().ifEmpty { "the source" })
            append(" becomes a creature")
            if (may) append(" (optional)")
            if (coloured) append(" (coloured)")
            if (artifact) append(" (artifact)")
            if (keyworded) append(" (with a keyword)")
        }
        return phrase(template, name = name) {
            if (!may) frontedDuration()
            slot("self", subject)
            slot("p", Primitives.cardinal)
            slot("t", Primitives.cardinal)
            if (coloured) slot("colour", Primitives.color)
            slot("type", Primitives.creatureSubtype)
            if (keyworded) slot("kw", Keywords.keyword)
            build {
                scriptFor(
                    it.int("p") to it.int("t"),
                    if (coloured) it.value<Color>("colour") else null,
                    it.value("type"),
                    if (keyworded) it.value<Keyword>("kw") else null,
                )
            }
            match { script ->
                val inner = if (may) {
                    val gated = script.spellEffect as? GatedEffect ?: return@match null
                    if (gated.gate !is Gate.MayDecide) return@match null
                    gated.then
                } else {
                    script.spellEffect
                }
                val animate = inner as? BecomeCreatureEffect ?: return@match null
                val colour = animate.colors?.singleOrNull()
                    ?.let { name -> Color.entries.firstOrNull { it.name == name } }
                if (coloured != (colour != null)) return@match null
                if (artifact != animate.addTypes.isNotEmpty()) return@match null
                val keyword = animate.keywords.singleOrNull()
                if (keyworded != (keyword != null)) return@match null
                val type = animate.creatureTypes.singleOrNull() ?: return@match null
                val power = (animate.power as? DynamicAmount.Fixed)?.amount ?: return@match null
                val toughness = (animate.toughness as? DynamicAmount.Fixed)?.amount ?: return@match null
                if (script != scriptFor(power to toughness, colour, Subtype(type), keyword)) {
                    return@match null
                }
                bind(
                    "self" to Unit,
                    "p" to power,
                    "t" to toughness,
                    "colour" to colour,
                    "type" to Subtype(type),
                    "kw" to keyword,
                )
            }
        }
    }

    /** The [selfAnimate] product — the three omissible layers crossed with both "may" spellings. */
    private fun selfAnimates(
        target: EffectTarget,
        subject: Phrase<Unit>,
        tag: String,
    ): List<Phrase<CardScript>> =
        listOf(false, true).flatMap { may ->
            listOf(false, true).flatMap { coloured ->
                listOf(false, true).flatMap { artifact ->
                    listOf(false, true).map { keyworded ->
                        selfAnimate(target, subject, tag, coloured, artifact, keyworded, may)
                    }
                }
            }
        }

    /** "~ loses flying until end of turn." — Swooping Talon, the grant rules' negation. */
    private fun selfLosesKeyword(
        target: EffectTarget,
        subject: Phrase<Unit>,
        tag: String,
    ): Phrase<CardScript> {
        fun scriptFor(keyword: Keyword) =
            CardScript(spellEffect = Effects.RemoveKeyword(keyword, target))
        return phrase("{self} loses {kw} until end of turn", name = "$tag loses a keyword".trim()) {
            frontedDuration()
            slot("self", subject)
            slot("kw", Keywords.keyword)
            build { scriptFor(it.value("kw")) }
            match { script ->
                val removal = script.spellEffect as? RemoveKeywordEffect ?: return@match null
                val keyword = Keyword.entries.firstOrNull { it.name == removal.keyword } ?: return@match null
                if (script != scriptFor(keyword)) return@match null
                bind("self" to Unit, "kw" to keyword)
            }
        }
    }

    /**
     * The verbs whose object is one permanent and which carry nothing else — a move to a named zone,
     * an untap, a regeneration.
     *
     * The subject is always a slot, which is what lets one row serve every anaphor position: the
     * source spells it `~`, a filtered trigger's pronoun spells it "it", and a later clause's
     * pronoun points at the target instead. See [retargetable] for why none of these is template
     * text any more.
     */
    private fun move(
        template: String,
        name: String,
        effect: Effect,
        subject: Phrase<Unit>,
    ): Phrase<CardScript> {
        val script = CardScript(spellEffect = effect)
        return phrase(template, name = name) {
            slot("self", subject)
            build { script }
            match { if (it == script) bind("self" to Unit) else null }
        }
    }

    /**
     * "Sacrifice ~." — Ball Lightning's end step, and every other creature that pays for its
     * statistics by leaving.
     *
     * The bare sentence, and it declined until now because the four [sacrificeUnless] rules had the
     * *rider* written into their templates: "sacrifice ~" was only ever readable as the front of
     * "sacrifice ~ unless you pay {2}", so a card that printed the clause and stopped died on its
     * own full stop. An "unless" clause is something English adds to this sentence, not something
     * the sentence is made of, and a rule that cannot be read without its modifier is the shape
     * that puts a line in the `.` decline family.
     *
     * The model is the sacrifice with no cost in front of it — [SacrificeSelfEffect] alone rather
     * than the `PayOrSufferEffect` the riders build — so the two spellings denote different values
     * and neither can print the other's sentence.
     */
    private val sacrificeSelf: Phrase<CardScript> = run {
        val script = CardScript(spellEffect = SacrificeSelfEffect)
        phrase("sacrifice {self}", name = "sacrifice the source") {
            slot("self", Primitives.self)
            build { script }
            match { if (it == script) bind("self" to Unit) else null }
        }
    }

    /**
     * "Sacrifice ~ unless you pay {G}{G}." — Krosan Cloudscraper's upkeep tax.
     *
     * A row of the [sacrificeUnless] shape over a *mana* cost rather than a permanent one, which is
     * why it is written out: the cost has no noun phrase and therefore no article, so
     * [Filters.indefinite] has nothing to do and the slot is a bare mana symbol run.
     */
    private val sacrificeUnlessPay: Phrase<CardScript> = run {
        fun scriptFor(cost: ManaCost) = CardScript(
            spellEffect = PayOrSufferEffect(cost = Costs.pay.Mana(cost), suffer = SacrificeSelfEffect)
        )
        phrase("sacrifice {self} unless you pay {cost}", name = "sacrifice the source unless you pay") {
            slot("self", Primitives.self)
            slot("cost", Primitives.manaCost)
            build { scriptFor(it.value("cost")) }
            match { script ->
                val effect = script.spellEffect as? PayOrSufferEffect ?: return@match null
                val cost = ((effect.cost as? PayCost.Atom)?.atom as? CostAtom.Mana)?.cost ?: return@match null
                if (script != scriptFor(cost)) return@match null
                bind("self" to Unit, "cost" to cost)
            }
        }
    }

    /**
     * "Sacrifice it unless you discard a land card." — the Portal drawback, and one shape with two
     * costs.
     *
     * `PayOrSufferEffect` is the SDK's name for the whole sentence: a cost the controller may pay
     * and the thing that happens if they do not. The two members differ only in the [PayCost], which
     * is why the rule is a function of the cost's surface and both halves of the cost — the printed
     * noun phrase goes through [Filters.indefinite] so the article comes out right, and the cost is
     * reconstructed from the filter for the comparison.
     */
    private fun sacrificeUnless(
        template: String,
        name: String,
        // A discard names a *card* and a sacrifice names a permanent, which are two noun phrases
        // rather than one with a word appended; see [Filters.cardNoun].
        noun: Phrase<GameObjectFilter> = Filters.indefinite,
        cost: (GameObjectFilter) -> PayCost,
    ): Phrase<CardScript> {
        fun scriptFor(filter: GameObjectFilter) = CardScript(
            spellEffect = PayOrSufferEffect(cost = cost(filter), suffer = SacrificeSelfEffect)
        )
        return phrase(template, name = name) {
            slot("filter", noun)
            build { scriptFor(it.value("filter")) }
            match { script ->
                val effect = script.spellEffect as? PayOrSufferEffect ?: return@match null
                val filter = paidFilter(effect.cost) ?: return@match null
                if (script != scriptFor(filter)) return@match null
                bind("filter" to filter)
            }
        }
    }

    /** The filter a one-atom pay cost names, or null when the cost is anything more complicated. */
    private fun paidFilter(cost: PayCost): GameObjectFilter? {
        val atom = (cost as? PayCost.Atom)?.atom ?: return null
        return when (atom) {
            is CostAtom.Discard -> atom.filter
            is CostAtom.Sacrifice -> atom.filter
            else -> null
        }
    }

    /**
     * "Sacrifice it unless you sacrifice three Forests." — the counted cost, Primeval Force's.
     *
     * A row of the [sacrificeUnless] shape would need the count in the cost *and* a plural noun in
     * the text, which changes both slots; the singular rules keep the noun singular and this one
     * carries the number, which is the same singular/plural split every counting rule here makes.
     */
    private val sacrificeUnlessCounted: Phrase<CardScript> = run {
        fun scriptFor(count: Int, filter: GameObjectFilter) = CardScript(
            spellEffect = PayOrSufferEffect(
                cost = Costs.pay.Sacrifice(filter, count = count),
                suffer = SacrificeSelfEffect,
            )
        )
        phrase(
            "sacrifice it unless you sacrifice {n} {filter}",
            name = "sacrifice the source unless you sacrifice several",
        ) {
            slot("n", Cardinals.word)
            slot("filter", Filters.plural)
            build { scriptFor(it.int("n"), it.value("filter")) }
            match { script ->
                val effect = script.spellEffect as? PayOrSufferEffect ?: return@match null
                val atom = (effect.cost as? PayCost.Atom)?.atom as? CostAtom.Sacrifice ?: return@match null
                if (!Cardinals.spellable(atom.count)) return@match null
                if (script != scriptFor(atom.count, atom.filter)) return@match null
                bind("n" to atom.count, "filter" to atom.filter)
            }
        }
    }

    /**
     * "Sacrifice it unless you sacrifice any number of creatures with total power 12 or greater." —
     * Phyrexian Dreadnought, and the third context `CostAtom` names.
     *
     * The whole of [VariableCosts] rather than a row of its own: a payable cost is the same payable
     * thing as an activation cost and an additional cost, so this slots the family the other two
     * slot. That is [Costs]' own argument one context further along, and it is why a verb this
     * sentence has never printed ("unless you tap any number of …") costs nothing to have — the
     * family is the type's product, and a context that can pay it can pay all of it.
     */
    private val sacrificeUnlessVariable: Phrase<CardScript> = run {
        fun scriptFor(atom: CostAtom) = CardScript(
            spellEffect = PayOrSufferEffect(cost = Costs.pay.Atom(atom), suffer = SacrificeSelfEffect)
        )
        phrase("sacrifice it unless you {atom}", name = "sacrifice the source unless you pay a chosen count") {
            slot("atom", VariableCosts.payAtoms)
            build { scriptFor(it.value("atom")) }
            match { script ->
                val effect = script.spellEffect as? PayOrSufferEffect ?: return@match null
                val atom = (effect.cost as? PayCost.Atom)?.atom as? CostAtom.VariablePermanents
                    ?: return@match null
                if (script != scriptFor(atom)) return@match null
                bind("atom" to atom)
            }
        }
    }

    /** "Sacrifice it unless you discard a card at random." — Pillaging Horde. */
    private val sacrificeUnlessRandomDiscard: Phrase<CardScript> = run {
        val script = CardScript(
            spellEffect = PayOrSufferEffect(
                cost = Costs.pay.Discard(random = true),
                suffer = SacrificeSelfEffect,
            )
        )
        phrase(
            "sacrifice it unless you discard a card at random",
            name = "sacrifice the source unless you discard at random",
        ) {
            build { script }
            match { if (it == script) bind() else null }
        }
    }

    /**
     * The clauses that sacrifice the **source itself**, which no anaphor can move.
     *
     * `SacrificeSelfEffect` carries no [EffectTarget] at all — the SDK models "sacrifice this" as a
     * verb about the source rather than a verb with an object — so these are not members of
     * [retargetable] and a filtered trigger's "it" cannot reach them. That is the fail-closed
     * answer, not a gap worked around: a card that meant "sacrifice the creature that triggered
     * this" needs an effect the SDK does not have, so it declines and is counted.
     */
    private val sacrificesSource: List<Phrase<CardScript>> = listOf(
        sacrificeSelf,
        sacrificeUnlessPay,
        sacrificeUnlessCounted,
        sacrificeUnlessVariable,
        sacrificeUnlessRandomDiscard,
        sacrificeUnless(
            "sacrifice it unless you discard {filter}",
            "sacrifice the source unless you discard",
            noun = Filters.indefiniteCard,
        ) { Costs.pay.Discard(filter = it) },
        sacrificeUnless(
            "sacrifice it unless you sacrifice {filter}",
            "sacrifice the source unless you sacrifice",
        ) { Costs.pay.Sacrifice(it) },
    )

    /**
     * The **name** alone — the half of [anaphoric] that means the source in every position there is.
     *
     * `~` is not an anaphor: it denotes the card whatever sentence it stands in, so unlike "it" it
     * needs no earlier mention and cannot be captured by one. That is why this list is offered in a
     * *later* clause of a run as well as a first one ([Steps]' `laterClause`), where the pronoun is
     * [Continuations]' to read: "Draw a card. Put a +1/+1 counter on ~." and "{T}: Add {C}. Put a
     * point counter on ~." were declining on their own full stop, and ninety-four lines of the `.`
     * decline family were this one omission.
     *
     * It is the same list [triggering] takes for its named half, and one `val` rather than two
     * calls on purpose: two instantiations of one shape over one subject spelling would be two rule
     * instances reading one text, which is redundancy the gate counts.
     */
    val named: List<Phrase<CardScript>> =
        retargetable(EffectTarget.Self, Primitives.selfNamed, tag = " the named source")

    /**
     * The same vocabulary aimed at the **target an earlier clause chose** — what [Continuations]
     * slots, and the third position the shape was written for.
     *
     * Reachable only from a later clause of a run, which is what keeps "it" denoting one thing per
     * position. [Steps.merge] refuses a run that reads this slot without declaring it, so the
     * pronoun cannot dangle.
     */
    val continuing: List<Phrase<CardScript>> =
        retargetable(Targets.bound(), Primitives.targetPronoun, tag = " the target")

    /**
     * The clauses whose "it" is the **source** — what every position but a filtered trigger reads.
     *
     * Kept apart from the rest because English resolves an anaphor to the most recently mentioned
     * object: in "Whenever this creature attacks, it gets +2/+0" the only mention is the source, but
     * in "Untap target creature. It gets +2/+4 until end of turn." it is the target the first clause
     * introduced. So these rules are clauses in their own right and are *not* offered in a later
     * position of a sequence — [Continuations] owns "it" there. Registering them in both places
     * would be two readings of one text, which is ambiguity rather than a choice.
     */
    val anaphoric: List<Phrase<CardScript>> =
        retargetable(EffectTarget.Self, Primitives.self, tag = " the source") + sacrificesSource

    /**
     * The same vocabulary inside a **filtered** trigger, where the two spellings come apart.
     *
     * The name still reads the source and the pronoun now reads the object the trigger's filter
     * matched, so the two instantiations have disjoint surfaces and disjoint models — one printed
     * form per model, with nothing for the printer to choose. See the third-anaphor section on this
     * object, and [Steps.triggeredStep] for the only cascade that takes this list.
     */
    val triggering: List<Phrase<CardScript>> =
        named +
            retargetable(
                EffectTarget.TriggeringEntity,
                Primitives.itPronoun,
                tag = " the triggering permanent",
            ) +
            sacrificesSource

    /** Everything in this file that does not turn on the pronoun. Empty for now; the family is "it". */
    val clauses: List<Phrase<CardScript>> = emptyList()
}
