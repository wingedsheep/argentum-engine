package com.wingedsheep.assay.grammar

import com.wingedsheep.assay.syntax.Phrase
import com.wingedsheep.assay.syntax.bind
import com.wingedsheep.assay.syntax.phrase
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.DividedDamageEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.effects.GrantAttackBlockTaxPerCreatureTypeEffect
import com.wingedsheep.sdk.scripting.effects.GrantCantBeBlockedExceptByColorEffect
import com.wingedsheep.sdk.scripting.effects.GrantCantBeBlockedExceptByEffect
import com.wingedsheep.sdk.scripting.effects.MustBeBlockedEffect
import com.wingedsheep.sdk.scripting.effects.ReflectCombatDamageEffect
import com.wingedsheep.sdk.scripting.effects.SkipCombatPhasesEffect
import com.wingedsheep.sdk.scripting.effects.SkipUntapEffect
import com.wingedsheep.sdk.scripting.effects.TauntEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Clauses that change how a combat goes — the spell-side siblings of the combat statics in
 * [Statics].
 *
 * A static says what a permanent *is* for as long as it is on the battlefield; these say what
 * happens for a turn, which the SDK models as ordinary resolution-time effects. Both vocabularies
 * name the same combat concepts and neither can be spelled in terms of the other, so they sit in
 * separate files by which slot they reach rather than by what they talk about.
 */
object Combat {

    /**
     * "During target player's next turn, creatures that player controls attack you if able." —
     * Taunt.
     *
     * The whole sentence is one SDK effect, and the "that player" in the second clause is the same
     * player the first clause targeted — a link the model carries as a single `target` field rather
     * than as two references, which is why this is one rule and not a [Continuations] pair.
     */
    private val taunt: Phrase<CardScript> = run {
        val script = CardScript(
            spellEffect = TauntEffect(Targets.bound()),
            targetRequirements = listOf(Targets.player()),
        )
        phrase(
            "during target player's next turn, creatures that player controls attack you if able",
            name = "taunt",
        ) {
            build { script }
            match { if (it == script) bind() else null }
        }
    }

    /**
     * "Black creatures you control can't be blocked this turn except by black creatures." — Dread
     * Charge.
     *
     * Two colours in one sentence and two fields in the model: the group that gains the evasion, and
     * the colour that is still allowed to block it. The second is a bare `Color` rather than a
     * filter because that is the shape the SDK gives it, so the "creatures" after it is a literal
     * here rather than a slot — writing it as one would let the rule print a filter the effect
     * cannot hold.
     */
    private val cantBeBlockedExceptByColor: Phrase<CardScript> = run {
        fun scriptFor(filter: GameObjectFilter, colour: com.wingedsheep.sdk.core.Color) = CardScript(
            spellEffect = GrantCantBeBlockedExceptByColorEffect(
                filter = GroupFilter(filter),
                canOnlyBeBlockedByColor = colour,
            )
        )
        phrase(
            "{filter} can't be blocked this turn except by {color} creatures",
            name = "a group can't be blocked except by a colour",
        ) {
            slot("filter", Filters.plural)
            slot("color", Primitives.color)
            build { scriptFor(it.value("filter"), it.value("color")) }
            match { script ->
                val effect = script.spellEffect as? GrantCantBeBlockedExceptByColorEffect ?: return@match null
                val filter = effect.filter.baseFilter
                if (script != scriptFor(filter, effect.canOnlyBeBlockedByColor)) return@match null
                bind("filter" to filter, "color" to effect.canOnlyBeBlockedByColor)
            }
        }
    }


    // ---------------------------------------------------------------------------------------
    // The durational combat restrictions — "can't be blocked this turn", "can't block this turn"
    // ---------------------------------------------------------------------------------------

    /**
     * One durational combat restriction: the clause English prints for it, and the SDK effect it
     * denotes.
     *
     * The table is the family, and every column but these two is shared — the subject, the object it
     * denotes, the duration, and the fail-closed reconstruct-and-compare. [Statics]' combat
     * restrictions are the same five sentences about a permanent's *printed* abilities, which is why
     * the two files hold one table each rather than one file holding both: a static says what a
     * permanent is for as long as it is on the battlefield, and these say what happens for a turn,
     * and neither vocabulary can be spelled in terms of the other.
     *
     * **"Can't be blocked" is the odd surface, and it is odd for a reason worth stating.** Its model
     * is `GrantKeywordEffect` — the *same* effect "gains flying until end of turn" builds ([Steps]'
     * and [SelfSteps]' grant families) — over `AbilityFlag.CANT_BE_BLOCKED` instead of a
     * `Keyword`. A CR 702.x keyword is a **noun** a creature can be said to gain; an `AbilityFlag`
     * names a whole sentence and has no noun, so Oracle prints the flag as its own predicate with the
     * duration spelled "this turn". That makes it a row of the grant family whose *surface* is
     * irregular rather than a second grant vocabulary, and it is the same flag the unconditional
     * printed static carries on the card ([com.wingedsheep.assay.grammar.Grammar.flagLine]).
     *
     * @param blockerFilterOf non-null only for the row that names a class of blocker, and it is what
     *   marks the row as taking a `{blockers}` slot — the field's presence *is* the flag, so a row
     *   cannot declare the slot and forget to read it back.
     */
    private class Restriction(
        val clause: String,
        val name: String,
        val blockerFilterOf: ((Effect) -> GameObjectFilter?)? = null,
        // Last, so the common rows read as `Restriction(clause, name) { target, _ -> … }`.
        val effect: (EffectTarget, GameObjectFilter?) -> Effect,
    ) {
        val takesBlockers: Boolean get() = blockerFilterOf != null
    }

    private val restrictions: List<Restriction> = listOf(
        Restriction("can't be blocked this turn", "can't be blocked") { target, _ ->
            Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED, target)
        },
        // "{1}: ~ can't be blocked this turn except by creatures with haste." — Gingerbrute.
        Restriction(
            "can't be blocked this turn except by {blockers}",
            "can't be blocked except by",
            blockerFilterOf = { (it as? GrantCantBeBlockedExceptByEffect)?.blockerFilter },
        ) { target, blockers -> Effects.GrantCantBeBlockedExceptBy(target, blockers!!) },
        Restriction("can't block this turn", "can't block") { target, _ -> Effects.CantBlock(target) },
        Restriction("can't attack this turn", "can't attack") { target, _ -> Effects.CantAttack(target) },
        Restriction("can't attack or block this turn", "can't attack or block") { target, _ ->
            Effects.CantAttackOrBlock(target)
        },
    )

    /**
     * The table aimed at an object the sentence has **already fixed** — the source, the pronoun, or
     * the target an earlier clause chose.
     *
     * Written once and instantiated per position, the treatment [SelfSteps.retargetable] and
     * [Prevention.clausesFor] get and for the same reason: the two anaphors denote different things
     * ("~"/"it" in a first clause is the source, "that creature" is the target already chosen), so
     * registering one surface in both positions would be two readings of one text. Only the
     * subject's spelling and the [EffectTarget] move.
     *
     * @param subject the phrase standing in the `{self}` slot, or null when [surface] spells the
     *   subject as a literal — the same split [SelfSteps.retargetable]'s `pronominal` flag makes.
     */
    fun restrictionClauses(
        target: EffectTarget,
        subject: Phrase<Unit>?,
        surface: String,
        tag: String,
    ): List<Phrase<CardScript>> = restrictions.map { restriction ->
        fun scriptFor(blockers: GameObjectFilter?) =
            CardScript(spellEffect = restriction.effect(target, blockers))
        phrase("$surface ${restriction.clause}", name = "${restriction.name}$tag") {
            if (subject != null) slot("self", subject)
            if (restriction.takesBlockers) slot("blockers", Filters.plural)
            build { bindings ->
                scriptFor(if (restriction.takesBlockers) bindings.value("blockers") else null)
            }
            match { script ->
                val effect = script.spellEffect ?: return@match null
                val blockers = restriction.blockerFilterOf?.invoke(effect) ?: if (restriction.takesBlockers) {
                    return@match null
                } else {
                    null
                }
                if (script != scriptFor(blockers)) return@match null
                bind("self" to Unit, "blockers" to blockers)
            }
        }
    }

    /**
     * "Target creature can't be blocked this turn.", "Up to two target creatures can't block this
     * turn.", "X target creatures can't be blocked this turn." — the table over every quantifier
     * English prints in front of "target".
     *
     * "Can't be blocked" was one rule here with the singular row frozen into it, which is exactly the
     * state [Steps.quantifiedPermanentSteps]' KDoc describes replacing: the plural rows were missing
     * because nobody's card had needed them, not because English does not print them. The family takes
     * the whole table rather than [Targets.singularQuantifiers], because its plural changes only the
     * noun and the verb's agreement — unlike damage and counters, whose plurals are a different
     * requirement.
     */
    private val restrictionsOnTargets: List<Phrase<CardScript>> =
        Targets.quantifiers.flatMap { quantifier ->
            restrictions.map { restriction ->
                fun scriptFor(count: Int, filter: GameObjectFilter, blockers: GameObjectFilter?) = CardScript(
                    spellEffect = quantifier.effectOver { restriction.effect(it, blockers) },
                    targetRequirements = listOf(quantifier.requirement(count, filter)),
                )
                phrase(
                    quantifier.splice("{q}target {filter} ${restriction.clause}"),
                    name = "a target ${restriction.name}, ${quantifier.name}",
                ) {
                    if (quantifier.counted) slot(Targets.COUNT_SLOT, Cardinals.word)
                    slot("filter", if (quantifier.plural) Filters.plural else Filters.filter)
                    if (restriction.takesBlockers) slot("blockers", Filters.plural)
                    build { bindings ->
                        scriptFor(
                            if (quantifier.counted) bindings.int(Targets.COUNT_SLOT) else 1,
                            bindings.value("filter"),
                            if (restriction.takesBlockers) bindings.value("blockers") else null,
                        )
                    }
                    match { script ->
                        val member = quantifier.memberOf(script.spellEffect) ?: return@match null
                        val blockers = restriction.blockerFilterOf?.invoke(member)
                            ?: if (restriction.takesBlockers) return@match null else null
                        val requirement = script.targetRequirements.singleOrNull() ?: return@match null
                        val filter = Targets.targetedFilter(requirement) ?: return@match null
                        val count = if (quantifier.counted) requirement.count else 1
                        if (quantifier.counted && !Cardinals.spellable(count)) return@match null
                        if (script != scriptFor(count, filter, blockers)) return@match null
                        bind(Targets.COUNT_SLOT to count, "filter" to filter, "blockers" to blockers)
                    }
                }
            }
        }

    /**
     * The same table over the anaphor — what [Continuations] slots.
     *
     * "That creature" only, and deliberately not "it": in a later clause [Continuations] reads "it"
     * as the target and [SelfSteps] reads it as the source, and nine of the corpus's lines print it
     * about a permanent the *same* clause animated ("{1}{U}{B}: Until end of turn, ~ becomes a 3/2
     * blue and black Elemental creature. It's still a land. It can't be blocked this turn." — Creeping
     * Tar Pit). Registering the pronoun here would read those as the target: byte-perfect and about
     * the wrong creature, which is the reversible-but-wrong class. [Prevention.continuationClauses]
     * can spell the pronoun because its recipient vocabulary makes the two readings disjoint; this
     * table has no such handle, so the pronoun declines and is counted.
     */
    val restrictionContinuationClauses: List<Phrase<CardScript>> =
        restrictionClauses(Targets.bound(), subject = null, surface = "that creature", tag = ", that creature")

    /**
     * "Return one or two target attacking creatures to their owner's hand." — Command of
     * Unsummoning.
     *
     * The one rule in the grammar whose effect refers to its target **positionally**, and it has to:
     * `ForEachTargetEffect` rebinds `ContextTarget(0)` to the current target on each iteration, so a
     * named [EffectTarget.BoundVariable] would name the *whole* declaration rather than the member
     * being processed and would mean a different card. The differential normalizes named and
     * positional references to a slot's position, so this reads the same as every other rule there;
     * only the model differs, and it differs because the iteration requires it.
     *
     * The count pair is spelled by the template rather than by two number slots. "One or two" is a
     * `minCount`/`count` pair in the model and there is exactly one member of the shape so far, so
     * it is written inline — factor it when the second ("up to three target …") appears.
     */
    private val returnOneOrTwoTargets: Phrase<CardScript> = run {
        fun scriptFor(filter: GameObjectFilter) = CardScript(
            spellEffect = ForEachTargetEffect(listOf(Effects.Move(EffectTarget.ContextTarget(0), Zone.HAND))),
            targetRequirements = listOf(
                TargetCreature(count = 2, minCount = 1, filter = TargetFilter(filter), id = Targets.SLOT)
            ),
        )
        phrase(
            "return one or two target {filter} to their owner's hand",
            name = "return one or two targets to hand",
        ) {
            slot("filter", Filters.plural)
            build { scriptFor(it.value("filter")) }
            match { script ->
                val requirement = script.targetRequirements.singleOrNull() ?: return@match null
                val filter = (requirement as? com.wingedsheep.sdk.scripting.targets.TargetObject)
                    ?.filter?.baseFilter ?: return@match null
                if (script != scriptFor(filter)) return@match null
                bind("filter" to filter)
            }
        }
    }

    /**
     * "All creatures able to block target creature this turn do so." — the lure.
     *
     * `MustBeBlockedEffect`'s `allCreatures` default is exactly this sentence; the narrower form
     * ("target creature blocks this turn if able") is a different sentence and refuses to print
     * here, which the reconstruct-and-compare enforces.
     */
    private val mustBeBlocked: Phrase<CardScript> = run {
        fun scriptFor(filter: GameObjectFilter) = CardScript(
            spellEffect = MustBeBlockedEffect(Targets.bound()),
            targetRequirements = listOf(Targets.permanent(filter)),
        )
        phrase("all creatures able to block target {filter} this turn do so", name = "lure") {
            slot("filter", Filters.filter)
            build { scriptFor(it.value("filter")) }
            match { script ->
                val requirement = script.targetRequirements.singleOrNull() ?: return@match null
                val filter = Targets.permanentFilter(requirement) ?: return@match null
                if (script != scriptFor(filter)) return@match null
                bind("filter" to filter)
            }
        }
    }

    /**
     * The turn-shaping effects a player is the object of — skipping a combat, skipping an untap.
     *
     * Each is one effect with one target and no variable, so each is a constant rule paired with the
     * requirement its sentence declares. Note that the *filter* in Exhaustion's printed noun phrase
     * ("Creatures and lands target opponent controls") is not a slot: `SkipUntapEffect` carries
     * `affectsCreatures` and `affectsLands` as booleans rather than a filter, so the phrase is a
     * literal here and a card naming any other combination declines.
     */
    private fun playerEffect(
        template: String,
        name: String,
        requirement: com.wingedsheep.sdk.scripting.targets.TargetRequirement,
        effect: (com.wingedsheep.sdk.scripting.targets.EffectTarget) -> Effect,
    ): Phrase<CardScript> {
        val script = CardScript(
            spellEffect = effect(Targets.bound()),
            targetRequirements = listOf(requirement),
        )
        return phrase(template, name = name) {
            build { script }
            match { if (it == script) bind() else null }
        }
    }

    /** The whole-turn combat effects with no target at all — Deep Wood and Harsh Justice. */
    private fun turnEffect(template: String, name: String, effect: Effect): Phrase<CardScript> {
        val script = CardScript(spellEffect = effect)
        return phrase(template, name = name) {
            build { script }
            match { if (it == script) bind() else null }
        }
    }

    /**
     * "~ deals 4 damage divided as you choose among one, two, or three target creatures." — Forked
     * Lightning.
     *
     * The target-count range appears in the *effect* as well as on the requirement — `minTargets`
     * and `maxTargets` on `DividedDamageEffect`, `minCount` and `count` on the requirement — so the
     * two numbers the phrase spells land in four model fields. They are written as one range in the
     * template rather than as two slots because Oracle enumerates the run ("one, two, or three")
     * rather than stating bounds, and an enumeration is a printed form rather than a number.
     */
    private val dividedDamage: Phrase<CardScript> = run {
        fun scriptFor(total: Int, filter: GameObjectFilter) = CardScript(
            spellEffect = DividedDamageEffect(totalDamage = total, minTargets = 1, maxTargets = 3),
            targetRequirements = listOf(
                TargetCreature(count = 3, minCount = 1, filter = TargetFilter(filter), id = Targets.SLOT)
            ),
        )
        phrase(
            "{self} deals {n} damage divided as you choose among one, two, or three target {filter}",
            name = "divided damage among up to three targets",
        ) {
            slot("self", Primitives.self)
            slot("n", Primitives.cardinal)
            slot("filter", Filters.plural)
            build { scriptFor(it.int("n"), it.value("filter")) }
            match { script ->
                val effect = script.spellEffect as? DividedDamageEffect ?: return@match null
                val requirement = script.targetRequirements.singleOrNull()
                    as? com.wingedsheep.sdk.scripting.targets.TargetObject ?: return@match null
                val filter = requirement.filter.baseFilter
                if (script != scriptFor(effect.totalDamage, filter)) return@match null
                bind("self" to Unit, "n" to effect.totalDamage, "filter" to filter)
            }
        }
    }

    /**
     * "Until end of turn, target creature gains "This creature can't attack or block unless its
     * controller pays {1} for each Cleric on the battlefield."" — Whipgrass Entangler.
     *
     * A whole *ability* granted for a turn, and the SDK names the granted ability as one effect
     * type rather than as a `GrantActivatedAbility` over a constructed static — because the thing
     * granted is a combat *restriction* with a per-creature-type tax, which has no printed form
     * outside this sentence. So the quoted text is spelled here rather than slotted through
     * [Activated.quoted]: what is inside the quotes is not an ability the grammar can otherwise
     * read, and the two variables in it — the type and the tax — are the effect's two fields.
     *
     * "Until end of turn" is at the *front* of this sentence and at the back of every other
     * durational one, which is Oracle's own inconsistency and why the duration is a literal here.
     */
    private val grantAttackBlockTax: Phrase<CardScript> = run {
        fun scriptFor(subtype: Subtype, tax: ManaCost) = CardScript(
            spellEffect = Effects.GrantAttackBlockTaxPerCreatureType(
                target = Targets.bound(),
                creatureType = subtype.value,
                manaCostPer = tax.toString(),
            ),
            targetRequirements = listOf(Targets.permanent(GameObjectFilter.Creature)),
        )
        phrase(
            "until end of turn, target creature gains \"{self} can't attack or block unless its " +
                "controller pays {tax} for each {subtype} on the battlefield.\"",
            name = "grant an attack and block tax",
        ) {
            slot("self", Primitives.self)
            slot("tax", Primitives.manaCost)
            slot("subtype", Primitives.subtype)
            build { scriptFor(it.value("subtype"), it.value("tax")) }
            match { script ->
                val effect = script.spellEffect as? GrantAttackBlockTaxPerCreatureTypeEffect
                    ?: return@match null
                val tax = runCatching { ManaCost.parse(effect.manaCostPer) }.getOrNull() ?: return@match null
                val subtype = Subtype(effect.creatureType)
                if (script != scriptFor(subtype, tax)) return@match null
                bind("self" to Unit, "tax" to tax, "subtype" to subtype)
            }
        }
    }

    /**
     * The clauses that carry their own full stop, because it falls **inside** a quotation.
     *
     * "…gains "This creature can't attack or block …"" ends on a quote mark, not on a stop, so
     * [Steps.sentence] — which spells the stop itself — cannot end a line with one. They are
     * therefore offered beside a sentence rather than inside it; see [Steps.step].
     */
    val selfTerminatingClauses: List<Phrase<CardScript>> = listOf(grantAttackBlockTax)

    val clauses: List<Phrase<CardScript>> = listOf(
        taunt,
        cantBeBlockedExceptByColor,
        returnOneOrTwoTargets,
        mustBeBlocked,
        dividedDamage,
        playerEffect(
            "target player skips all combat phases of their next turn",
            "skip combat phases",
            Targets.player(),
        ) { SkipCombatPhasesEffect(it) },
        playerEffect(
            "creatures and lands target opponent controls don't untap during their next untap step",
            "skip untap",
            Targets.opponent(),
        ) { SkipUntapEffect(it) },
        // "Prevent all damage that would be dealt to you this turn by attacking creatures." used to
        // be a whole-sentence rule here. It is now [Prevention]'s recipient clause wearing that
        // family's source layer — one sentence assembled from three things that vary — and leaving
        // both would have been two readings of one text with the same model, which the report counts
        // as grammar redundancy and which is how it was found.
        turnEffect(
            "this turn, whenever an attacking creature deals combat damage to you, it deals that " +
                "much damage to its controller",
            "reflect combat damage",
            ReflectCombatDamageEffect(),
        ),
    ) + restrictionsOnTargets
}
