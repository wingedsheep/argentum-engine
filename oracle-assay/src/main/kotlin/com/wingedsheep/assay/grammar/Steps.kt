package com.wingedsheep.assay.grammar

import com.wingedsheep.assay.normalize.Normalizer
import com.wingedsheep.assay.syntax.Phrase
import com.wingedsheep.assay.syntax.alternate
import com.wingedsheep.assay.syntax.bind
import com.wingedsheep.assay.syntax.oneOf
import com.wingedsheep.assay.syntax.phrase
import com.wingedsheep.assay.syntax.separated
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets as SdkTargets
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.scripting.conditions.Condition
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect
import com.wingedsheep.sdk.scripting.effects.FlipCoinEffect
import com.wingedsheep.sdk.scripting.effects.ForEachEffect
import com.wingedsheep.sdk.scripting.effects.ForEachPlayerEffect
import com.wingedsheep.sdk.scripting.effects.IterationSpace
import com.wingedsheep.sdk.scripting.effects.ForceSacrificeEffect
import com.wingedsheep.sdk.scripting.effects.MayPayManaEffect
import com.wingedsheep.sdk.scripting.effects.PayManaCostEffect
import com.wingedsheep.sdk.scripting.effects.MayPayXForEffect
import com.wingedsheep.sdk.scripting.effects.RedirectNextDamageEffect
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.effects.GainLifeEffect
import com.wingedsheep.sdk.scripting.effects.GrantKeywordEffect
import com.wingedsheep.sdk.scripting.effects.RegenerateEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeEffect
import com.wingedsheep.sdk.scripting.effects.Gate
import com.wingedsheep.sdk.scripting.effects.GatedEffect
import com.wingedsheep.sdk.scripting.effects.LoseLifeEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.effects.PlayAdditionalLandsEffect
import com.wingedsheep.sdk.scripting.effects.ScryEffect
import com.wingedsheep.sdk.scripting.effects.SurveilEffect
import com.wingedsheep.sdk.scripting.effects.TakeExtraTurnEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * The steps a spell performs — the pipeline family, and the rules that produce a `CardScript`
 * rather than a keyword.
 *
 * Each rule targets `mtg-sdk` through its companion facade ([Effects], `Patterns`) rather than a raw
 * constructor, matching the discipline `FacadeBoundaryTest` enforces for cards. A rule's `match`
 * half necessarily destructures the concrete effect class, since that is the only way to read a
 * model back; the asymmetry is inherent to a bidirectional rule and is why `build` going through the
 * facade matters — it is the half that would otherwise drift from how cards are written.
 *
 * ## A clause, a sentence, and a line
 *
 * The unit the rules below are written in is the **clause** — the verb phrase with no full stop on
 * it. A [sentence] is a clause plus its stop, and a [step] is either one sentence or a [sequence] of
 * them. That three-way split is what lets a card printing two sentences on one line
 * ("Target creature gets +1/+3 until end of turn. Untap that creature.") reuse the ordinary effect
 * vocabulary twice instead of needing a second, capitalized copy of every verb: a full stop is a
 * sentence start, which [com.wingedsheep.assay.syntax.SentenceCase] owns, so every template here is
 * written mid-sentence exactly as the keyword rules spell themselves "flying" rather than "Flying".
 *
 * Everything outside this file slots [step], so a trigger, an activated ability and a spell line all
 * gained sequences at once and none of them had to be told.
 *
 * ## Singular and plural are separate rules
 *
 * "Draw a card." and "Draw two cards." differ in the article *and* the noun, so one template cannot
 * spell both. They are therefore two rules over disjoint counts — [Cardinals.word] starts at two and
 * the singular rule is the only one that builds 1 — which keeps exactly one printed form per model
 * and leaves nothing for the printer to choose. Overlapping them would be an ambiguity hard error on
 * every draw card in the corpus, which is the grammar telling the truth about a bad factoring.
 */
object Steps {

    // ---------------------------------------------------------------------------------------
    // Draw
    // ---------------------------------------------------------------------------------------

    private val drawOne: Phrase<CardScript> = phrase("draw a card", name = "draw a card") {
        build { CardScript(spellEffect = Effects.DrawCards(1)) }
        match { script -> if (drawnByController(script) == 1) bind() else null }
    }

    private val drawMany: Phrase<CardScript> = phrase("draw {n} cards", name = "draw cards") {
        slot("n", Cardinals.word)
        build { CardScript(spellEffect = Effects.DrawCards(it.int("n"))) }
        match { script ->
            val count = drawnByController(script) ?: return@match null
            // The singular is drawOne's to print, and anything Cardinals cannot spell as a word has
            // no surface form here at all. Refusing both is what keeps printing total-or-null
            // rather than total-or-wrong.
            if (count >= 2 && Cardinals.spellable(count)) bind("n" to count) else null
        }
    }

    private val targetPlayerDrawsOne: Phrase<CardScript> =
        phrase("target player draws a card", name = "target player draws a card") {
            build { targetPlayerDraws(1) }
            match { script -> if (drawnByTarget(script) == 1) bind() else null }
        }

    private val targetPlayerDrawsMany: Phrase<CardScript> =
        phrase("target player draws {n} cards", name = "target player draws cards") {
            slot("n", Cardinals.word)
            build { targetPlayerDraws(it.int("n")) }
            match { script ->
                val count = drawnByTarget(script) ?: return@match null
                if (count >= 2 && Cardinals.spellable(count)) bind("n" to count) else null
            }
        }

    // ---------------------------------------------------------------------------------------
    // One permanent, one verb
    // ---------------------------------------------------------------------------------------

    /**
     * How a quantified target rule writes its effect: once against the requirement for a singular
     * quantifier, and once per chosen target for a plural one.
     *
     * The two halves of the same fact [Targets.Quantifier.plural] carries, and the reason it is one
     * column rather than two — a quantifier that admits several targets is exactly one whose effect
     * the SDK spells as an iteration. Every family slotting the table goes through this pair, so the
     * wrapping is decided once rather than per verb.
     */
    private fun Targets.Quantifier.effectOver(member: (EffectTarget) -> Effect): Effect =
        if (plural) ForEachTargetEffect(listOf(member(EffectTarget.ContextTarget(0)))) else member(Targets.bound())

    /**
     * The inverse of [effectOver] — the member effect inside [effect], or null when [effect] is not
     * the shape this quantifier writes. Fail-closed on the iteration space: a `ForEachEffect` over
     * players or a group says something no quantified target sentence does.
     */
    private fun Targets.Quantifier.memberOf(effect: Effect?): Effect? = if (plural) {
        (effect as? ForEachEffect)?.takeIf { it.space is IterationSpace.Targets }?.body
    } else {
        effect
    }

    /**
     * The same verb over **every quantifier English prints in front of "target"** — "Destroy target
     * creature.", "Destroy up to one target creature.", "Destroy two target lands.", "Destroy up to
     * three target creatures.", "Destroy up to X target artifacts." One declaration, one rule per
     * row of [Targets.quantifiers].
     *
     * This replaced two hand-copied shapes — a singular one and a plural one — and the reason to
     * collapse them is what the copies had drifted into: "tap up to three target creatures" was
     * written because a card needed it and "destroy up to three target creatures" was not, on a
     * grammar that already read both halves of that sentence. A quantifier that is a row cannot be
     * present on one verb and missing from another, which is the whole of what this buys.
     *
     * ### Two templates, because English agrees in number past the noun
     *
     * [singular] and [plural] are usually the same string, and for two verbs they are not: "return
     * target creature to **its owner's hand**" pluralizes to "… to **their owners' hands**". The
     * agreement reaches past the noun phrase, so it cannot live in the [Filters] cascade the way the
     * noun's own plural does, and a shape taking one template would have had to spell those verbs by
     * hand — which is the state being replaced. Both templates carry
     * [Targets.QUANTIFIER_PLACEHOLDER] where the quantifier goes; it is a *substitution* rather than
     * a slot, for the reason [Targets.Quantifier] gives.
     *
     * ### What a row decides
     *
     * A plural row admits more than one target, so its effect is a `ForEachTargetEffect` over
     * `ContextTarget(0)` and its noun comes from [Filters.plural]; a singular row keeps the
     * [Targets.bound] reference and [Filters.filter].
     *
     * The `match` half is an **equality test against what `build` would have produced**, not a
     * structural walk. That is deliberate and it is the discipline the whole file follows: a matcher
     * that inspected only the fields it cared about would happily print a script carrying extra
     * content it never looked at, which round-trips and loses meaning — the reversible-but-wrong
     * class. Reconstructing the whole script and comparing makes the check exhaustive by
     * construction, so a rule cannot fall behind the effect it prints — and here it also does the
     * work of telling the rows apart, since a count, an `optional` flag or a `dynamicMaxCount` its
     * own row does not spell is exactly what makes that comparison fail.
     */
    private fun quantifiedPermanentSteps(
        singular: String,
        name: String,
        plural: String = singular,
        pluralAlternate: String? = null,
        effect: (EffectTarget) -> Effect,
    ): List<Phrase<CardScript>> = Targets.quantifiers.map { quantifier ->
        fun scriptFor(count: Int, filter: GameObjectFilter) = CardScript(
            spellEffect = quantifier.effectOver(effect),
            targetRequirements = listOf(quantifier.requirement(count, filter)),
        )
        phrase(
            quantifier.splice(if (quantifier.plural) plural else singular),
            name = "$name, ${quantifier.name}",
        ) {
            if (quantifier.plural && pluralAlternate != null) {
                alsoSpelled(quantifier.splice(pluralAlternate), "$name, ${quantifier.name} (older possessive)")
            }
            if (quantifier.counted) slot(Targets.COUNT_SLOT, Cardinals.word)
            slot("filter", if (quantifier.plural) Filters.plural else Filters.filter)
            build {
                scriptFor(if (quantifier.counted) it.int(Targets.COUNT_SLOT) else 1, it.value("filter"))
            }
            match { script ->
                val requirement = script.targetRequirements.singleOrNull() ?: return@match null
                val filter = Targets.targetedFilter(requirement) ?: return@match null
                // A row spelling no count reconstructs at one, so a requirement carrying two fails
                // the comparison below rather than printing without its number.
                val count = if (quantifier.counted) requirement.count else 1
                if (quantifier.counted && !Cardinals.spellable(count)) return@match null
                if (script != scriptFor(count, filter)) return@match null
                bind(Targets.COUNT_SLOT to count, "filter" to filter)
            }
        }
    }

    // ---------------------------------------------------------------------------------------
    // Counted verbs — "scry 2", "you gain 3 life", "target player gains 3 life"
    // ---------------------------------------------------------------------------------------

    /**
     * A verb whose only variable is a number written in **digits**.
     *
     * Digits, not [Cardinals.word]: Oracle spells a *quantity of cards* as a word ("draw two cards")
     * and a *quantity of life, damage or counters* as a numeral ("you gain 3 life", "deals 2
     * damage"). The two are different conventions in the same text, which is why the draw rules
     * above take one leaf and these take the other — and why neither can borrow the other's, in
     * either direction.
     *
     * The shape takes both halves of the inversion explicitly. `script` is the forward direction and
     * `count` reads the number back out of the effect, because there is no general way to invert an
     * arbitrary builder. Everything *else* the script might carry is still checked the fail-closed
     * way the rest of this file is: `count` only recovers the number, and the equality against
     * `script(n)` is what refuses to print a script carrying anything the sentence does not say.
     */
    private fun countedStep(
        template: String,
        name: String,
        script: (Int) -> CardScript,
        count: (Effect) -> Int?,
    ): Phrase<CardScript> = phrase(template, name = name) {
        slot("n", Primitives.cardinal)
        if (template.contains("{self}")) slot("self", Primitives.self)
        build { script(it.int("n")) }
        match { model ->
            val amount = count(model.spellEffect ?: return@match null) ?: return@match null
            if (model != script(amount)) return@match null
            bind("n" to amount, "self" to Unit)
        }
    }

    /**
     * A counted verb and its **"equal to …" sibling**, generated from one call site.
     *
     * Oracle spells one quantity two ways and the model stores one value: "you gain 3 life" puts the
     * number where a numeral goes, and "you gain life equal to the number of Swamps you control"
     * moves it behind the noun as a whole clause. Neither template can be derived from the other —
     * the amount changes *position*, not just spelling — so the pair is two strings; what must not
     * be two is the script, the reader and the fail-closed reconstruction, because those are what a
     * second copy would drift on. This is [mayCountedStep]'s shape one axis over: one call site,
     * both printed forms.
     *
     * **The forms are disjoint by domain, not by alternation order.** A `Fixed` amount is the
     * numeral and everything else is the clause, so each rule refuses the other's values in `match`
     * and printing stays determined by the model — the same fix `drawOne` versus [Cardinals.word]
     * gets.
     *
     * ### Where the clause goes, and why the model decides
     *
     * A damage sentence puts the "equal to …" clause in **two** places, and both are real Oracle:
     * "deals damage to target creature equal to the number of Mountains you control" (Spitting
     * Earth) trails it, and "deals damage equal to its power to target creature" (every fight-like
     * card) leads with it. 195 printed lines take the first order and 152 the second, so neither is
     * a minority spelling to decline — but two rules that can each print one model is printing left
     * to alternation order, which this module treats as a latent bug rather than a preference.
     *
     * The split the corpus actually draws is on the **shape of the amount**: a property read off an
     * object ("its power", "the number of +1/+1 counters on it") leads, and everything else — the
     * board and zone tallies, which are long noun phrases — trails. That is a fact about the model,
     * so [leading] and the trailing form take disjoint halves of `DynamicAmount` and each refuses
     * the other's. English is following the heavy-noun-phrase rule; `EntityProperty` is where the
     * light ones live.
     *
     * That the amount is a slot at all is what makes this multiplicative: every counted verb naming
     * a second spelling reads the whole of [Amounts.count], and every row added to that vocabulary
     * reaches every one of these verbs without being told.
     */
    private fun countedStepPair(
        template: String,
        equalTo: String,
        name: String,
        script: (DynamicAmount) -> CardScript,
        amount: (Effect) -> DynamicAmount?,
        leading: String? = null,
    ): List<Phrase<CardScript>> {
        /** The amount this model carries, or null when it is not in [domain]. */
        fun amountIn(model: CardScript, domain: (DynamicAmount) -> Boolean): DynamicAmount? {
            val value = amount(model.spellEffect ?: return null) ?: return null
            if (model != script(value)) return null
            return value.takeIf(domain)
        }

        fun clause(surface: String, ruleName: String, domain: (DynamicAmount) -> Boolean) =
            phrase<CardScript>(surface, name = ruleName) {
                slot("amount", Amounts.count)
                if (surface.contains("{self}")) slot("self", Primitives.self)
                build { bindings -> bindings.value<DynamicAmount>("amount").takeIf(domain)?.let(script) }
                match { model -> amountIn(model, domain)?.let { bind("amount" to it, "self" to Unit) } }
            }

        val numeral = phrase<CardScript>(template, name = name) {
            slot("n", Primitives.cardinal)
            if (template.contains("{self}")) slot("self", Primitives.self)
            build { script(DynamicAmount.Fixed(it.int("n"))) }
            match { model ->
                val fixed = amountIn(model) { it is DynamicAmount.Fixed } as? DynamicAmount.Fixed
                    ?: return@match null
                bind("n" to fixed.amount, "self" to Unit)
            }
        }
        if (leading == null) {
            return listOf(numeral, clause(equalTo, "$name, by a count") { it !is DynamicAmount.Fixed })
        }
        val heavy = { value: DynamicAmount -> value !is DynamicAmount.Fixed && value !is DynamicAmount.EntityProperty }
        val light = { value: DynamicAmount -> value is DynamicAmount.EntityProperty }
        return listOf(
            numeral,
            clause(equalTo, "$name, by a count", heavy),
            clause(leading, "$name, by a property of an object", light),
            alternate(clause(equalTo, "$name, by a property of an object (trailing)", light)),
            alternate(clause(leading, "$name, by a count (leading)", heavy)),
        )
    }

    /**
     * The same shape over a [DynamicAmount] the text names in words rather than in digits — "deals
     * **X** damage", "gains life equal to the number of Mountains you control".
     *
     * Kept apart from [countedStep] rather than generalized over the amount, because the *printed
     * form* of the amount is not a slot at all in these: "X" is a literal, and "equal to the number
     * of …" is a whole clause. What varies is which amount the template denotes, so the amount is a
     * parameter of the rule and not of the sentence.
     */
    private fun amountStep(
        template: String,
        name: String,
        amount: DynamicAmount,
        script: (DynamicAmount) -> CardScript,
    ): Phrase<CardScript> = phrase(template, name = name) {
        if (template.contains("{self}")) slot("self", Primitives.self)
        build { script(amount) }
        match { if (it == script(amount)) bind("self" to Unit) else null }
    }

    /**
     * The same shape with "may" inserted after the clause's own subject — "You **may** gain 3 life."
     *
     * [mayClause] cannot reach these. It spells "you may {inner}" over a clause that states no
     * subject of its own ("draw a card"), and a clause that states "you" would come back as "you may
     * you gain 3 life": English contracts the wrapper's subject with the clause's, and the model is
     * the same `MayEffect` either way. So the contraction is a printed-shape fact, and it is written
     * as a *variant of the same shape* rather than as a rule of its own — one call site per clause,
     * with both spellings generated from one template, which is what stops the two drifting.
     */
    private fun mayCountedStep(
        template: String,
        name: String,
        script: (Int) -> CardScript,
        count: (Effect) -> Int?,
    ): Phrase<CardScript> = countedStep(
        template,
        name,
        script = { amount -> wrap(script(amount)) { MayEffect(it) } ?: script(amount) },
        count = { effect ->
            val gated = effect as? GatedEffect
            if (gated == null || gated.gate !is Gate.MayDecide || gated != MayEffect(gated.then)) {
                null
            } else {
                count(gated.then)
            }
        },
    )

    /**
     * The counted verbs. Those whose amount Oracle also spells as an "equal to …" clause are
     * [countedStepPair]s, so both printed forms come from one call site and one reconstruction;
     * scry and surveil are not, because their SDK count is an `Int` and no card writes them any way
     * but as a numeral.
     *
     * **Life is deliberately not one of them, and the reason is a collision rather than an
     * oversight.** "You gain 1 life for each creature you control" and "You gain life equal to the
     * number of creatures you control" are one model, and Oracle prints the first 131 times against
     * the second's 23 — so the "for each" family below is the canonical spelling, and adding the
     * clause here would be a second rule that can print the same value. Making the clause an
     * `alternate` does not work either: [gainLifeForEach] only spells *battlefield* aggregates, so a
     * life gain counting a graveyard would parse with nothing able to print it. The fix is to give
     * the "for each" form the same vocabulary treatment this band gave "equal to" — a singular noun
     * phrase where this one takes [Amounts.count] — and that is a band of its own, not a row here.
     */
    private val countedSteps: List<Phrase<CardScript>> = listOf(
        listOf(
            countedStep(
                "you gain {n} life", "you gain life",
                script = { CardScript(spellEffect = Effects.GainLife(it)) },
                count = ::lifeGained,
            ),
        ),
        listOf(
            mayCountedStep(
                "you may gain {n} life", "you may gain life",
                script = { CardScript(spellEffect = Effects.GainLife(it)) },
                count = ::lifeGained,
            ),
        ),
        listOf(
            countedStep(
                "target player gains {n} life", "target player gains life",
                script = {
                    CardScript(
                        spellEffect = Effects.GainLife(it, Targets.bound()),
                        targetRequirements = listOf(Targets.player()),
                    )
                },
                count = ::lifeGained,
            ),
        ),
        listOf(
            countedStep(
                "you lose {n} life", "you lose life",
                script = { CardScript(spellEffect = Effects.LoseLife(it, EffectTarget.Controller)) },
                count = ::lifeLost,
            ),
        ),
        listOf(
            countedStep(
                "target player loses {n} life", "target player loses life",
                script = {
                    CardScript(
                        spellEffect = Effects.LoseLife(it, Targets.bound()),
                        targetRequirements = listOf(Targets.player()),
                    )
                },
                count = ::lifeLost,
            ),
        ),
        listOf(
            countedStep(
                "scry {n}", "scry",
                script = { CardScript(spellEffect = Effects.Scry(it)) },
                count = { (it as? ScryEffect)?.count },
            ),
            countedStep(
                "surveil {n}", "surveil",
                script = { CardScript(spellEffect = Effects.Surveil(it)) },
                count = { (it as? SurveilEffect)?.count },
            ),
        ),
        countedStepPair(
            "{self} deals {n} damage to any target",
            "{self} deals damage to any target equal to {amount}",
            "deals damage to any target",
            script = {
                CardScript(
                    spellEffect = Effects.DealDamage(it, Targets.bound()),
                    targetRequirements = listOf(Targets.any()),
                )
            },
            amount = ::damageDealtAmount,
            leading = "{self} deals damage equal to {amount} to any target",
        ),
        // Lavaborn Muse. "That player" is the one whose step triggered, which the model names
        // directly — so unlike "target player" this clause declares no requirement at all.
        countedStepPair(
            "{self} deals {n} damage to that player",
            "{self} deals damage to that player equal to {amount}",
            "deals damage to the triggering player",
            script = {
                CardScript(
                    spellEffect = Effects.DealDamage(it, EffectTarget.PlayerRef(Player.TriggeringPlayer))
                )
            },
            amount = ::damageDealtAmount,
            leading = "{self} deals damage equal to {amount} to that player",
        ),
        countedStepPair(
            "{self} deals {n} damage to target player",
            "{self} deals damage to target player equal to {amount}",
            "deals damage to target player",
            script = {
                CardScript(
                    spellEffect = Effects.DealDamage(it, Targets.bound()),
                    targetRequirements = listOf(Targets.player()),
                )
            },
            amount = ::damageDealtAmount,
            leading = "{self} deals damage equal to {amount} to target player",
        ),
        countedStepPair(
            "{self} deals {n} damage to target opponent",
            "{self} deals damage to target opponent equal to {amount}",
            "deals damage to target opponent",
            script = {
                CardScript(
                    spellEffect = Effects.DealDamage(it, Targets.bound()),
                    targetRequirements = listOf(Targets.opponent()),
                )
            },
            amount = ::damageDealtAmount,
            leading = "{self} deals damage equal to {amount} to target opponent",
        ),
        // "Target opponent or planeswalker" is the modern redirection wording, and it is a
        // requirement type of its own rather than a filter — so it is a row beside "target player"
        // rather than a case inside it.
        countedStepPair(
            "{self} deals {n} damage to target opponent or planeswalker",
            "{self} deals damage to target opponent or planeswalker equal to {amount}",
            "deals damage to target opponent or planeswalker",
            script = {
                CardScript(
                    spellEffect = Effects.DealDamage(it, Targets.bound()),
                    targetRequirements = listOf(Targets.opponentOrPlaneswalker()),
                )
            },
            amount = ::damageDealtAmount,
            leading = "{self} deals damage equal to {amount} to target opponent or planeswalker",
        ),
        countedStepPair(
            "{self} deals {n} damage to target player or planeswalker",
            "{self} deals damage to target player or planeswalker equal to {amount}",
            "deals damage to target player or planeswalker",
            script = {
                CardScript(
                    spellEffect = Effects.DealDamage(it, Targets.bound()),
                    targetRequirements = listOf(Targets.playerOrPlaneswalker()),
                )
            },
            amount = ::damageDealtAmount,
            leading = "{self} deals damage equal to {amount} to target player or planeswalker",
        ),
    ).flatten()

    /**
     * The clauses whose whole sentence is one published effect and whose only variable, if any, is a
     * noun phrase the ordinary vocabularies already spell.
     *
     * Each unlocks a single card, which the module's rule says needs a stated reason, and the reason
     * is the same for every one: the *model* is a single effect type or a single published
     * `Patterns` recipe, so the sentence is the unit and there is no smaller rule to write. A shape
     * parameterized over them would be a factory with one member each.
     */
    private val sentenceClauses: List<Phrase<CardScript>> = listOf(
        // Unstable Hulk's drawback, and the only turn-skipping sentence in the set.
        constantClause("you skip your next turn", "you skip your next turn", Effects.SkipNextTurn()),
        // Willbender. `Targets.SpellOrAbilityWithSingleTarget` is a whole requirement rather than a
        // filter — a spell *or* an ability is not an object the noun-phrase cascade can name — so
        // the requirement is slotted verbatim and the effect reads nothing from it.
        run {
            val script = CardScript(
                spellEffect = Effects.ChangeTarget(),
                targetRequirements = listOf(SdkTargets.SpellOrAbilityWithSingleTarget),
            )
            phrase<CardScript>(
                "change the target of target spell or ability with a single target",
                name = "change a spell's target",
            ) {
                build { script }
                match { if (it == script) bind() else null }
            }
        },
        // Planar Guide. Two printed sentences and one recipe: the exile *links* the cards it removed
        // and the delayed trigger returns that link, so the second sentence's "those cards" is the
        // first sentence's slot and neither half denotes anything alone.
        run {
            val script = CardScript(
                spellEffect = Effects.ExileGroupAndLink(GroupFilter.AllCreatures).then(
                    CreateDelayedTriggerEffect(
                        step = Step.END,
                        effect = Effects.ReturnLinkedExileUnderOwnersControl(),
                    )
                )
            )
            phrase<CardScript>(
                "exile all creatures. at the beginning of the next end step, return those cards to " +
                    "the battlefield under their owners' control",
                name = "exile all creatures and return them",
            ) {
                build { script }
                match { if (it == script) bind() else null }
            }
        },
        // Goblin Assassin. "A creature of their choice" is what an edict aimed at every player means,
        // and `ForEachPlayerEffect` rebinding the controller per player is what makes "their" work —
        // which is why the inner sacrifice names `Controller` rather than a player reference.
        run {
            val script = CardScript(
                spellEffect = ForEachPlayerEffect(
                    players = Player.Each,
                    effects = listOf(
                        FlipCoinEffect(
                            lostEffect = ForceSacrificeEffect(
                                filter = GameObjectFilter.Creature,
                                count = 1,
                                target = EffectTarget.Controller,
                            )
                        )
                    ),
                )
            )
            phrase<CardScript>(
                "each player flips a coin. each player whose coin comes up tails sacrifices a " +
                    "creature of their choice",
                name = "each player flips a coin and may sacrifice",
            ) {
                build { script }
                match { if (it == script) bind() else null }
            }
        },
        // Beacon of Destiny. One effect with two references and no variable: the damage you would
        // take is redirected to the source.
        run {
            val script = CardScript(
                spellEffect = RedirectNextDamageEffect(
                    protectedTargets = listOf(EffectTarget.Controller),
                    redirectTo = EffectTarget.Self,
                )
            )
            phrase<CardScript>(
                "the next time a source of your choice would deal damage to you this turn, that " +
                    "damage is dealt to {self} instead",
                name = "redirect the next damage to the source",
            ) {
                slot("self", Primitives.self)
                build { script }
                match { if (it == script) bind("self" to Unit) else null }
            }
        },
        // Riptide Mangler. The target is *read* rather than acted on — the effect sets the source's
        // base power to the chosen creature's — so the requirement is declared and the reference in
        // the effect is a `targetPower`, not a bound variable.
        run {
            fun scriptFor(filter: GameObjectFilter) = CardScript(
                spellEffect = Effects.SetBasePower(
                    target = EffectTarget.Self,
                    power = DynamicAmounts.targetPower(0),
                ),
                targetRequirements = listOf(Targets.permanent(filter)),
            )
            phrase<CardScript>(
                "change {self}'s base power to target {filter}'s power",
                name = "set the source's base power to a target's",
            ) {
                slot("self", Primitives.self)
                slot("filter", Filters.filter)
                build { scriptFor(it.value("filter")) }
                match { script ->
                    val requirement = script.targetRequirements.singleOrNull() ?: return@match null
                    val filter = Targets.permanentFilter(requirement) ?: return@match null
                    if (script != scriptFor(filter)) return@match null
                    bind("self" to Unit, "filter" to filter)
                }
            }
        },
    )

    /** A whole sentence that denotes one fixed effect and nothing else. */
    private fun constantClause(template: String, name: String, effect: Effect): Phrase<CardScript> {
        val script = CardScript(spellEffect = effect)
        return phrase(template, name = name) {
            build { script }
            match { if (it == script) bind() else null }
        }
    }

    /**
     * "You may exchange control of target creature you control and target creature an opponent
     * controls." — Chromeshell Crab, and the first rule in the grammar that declares **two** targets.
     *
     * The two requirements are named by [Targets.slot], which is what [merge]'s KDoc says the
     * missing piece was: a single fixed slot name is enough only while every rule takes at most one
     * target, and a rule that took two would otherwise produce a script with two requirements both
     * called `target`. Numbering them is the whole fix, and the differential compares slots by
     * position so nothing downstream sees the names.
     */
    private val exchangeControl: Phrase<CardScript> = run {
        fun scriptFor(mine: GameObjectFilter, theirs: GameObjectFilter) = CardScript(
            spellEffect = MayEffect(Effects.ExchangeControl(Targets.bound(0), Targets.bound(1))),
            targetRequirements = listOf(Targets.permanent(mine, 0), Targets.permanent(theirs, 1)),
        )
        phrase("you may exchange control of target {mine} and target {theirs}", name = "exchange control") {
            slot("mine", Filters.filter)
            slot("theirs", Filters.filter)
            build { scriptFor(it.value("mine"), it.value("theirs")) }
            match { script ->
                if (script.targetRequirements.size != 2) return@match null
                val mine = Targets.permanentFilter(script.targetRequirements[0]) ?: return@match null
                val theirs = (script.targetRequirements[1] as? TargetObject)?.filter?.baseFilter
                    ?: return@match null
                if (script != scriptFor(mine, theirs)) return@match null
                bind("mine" to mine, "theirs" to theirs)
            }
        }
    }

    /**
     * The one-off clauses: a whole printed sentence that denotes one published effect, with at most
     * one number in it.
     *
     * Each unlocks a single card today, which the module's own rule says needs a stated reason. The
     * reason is the same for all four: the *model* is one effect type with one field, so there is no
     * smaller rule to write — the sentence is the unit, and a shape parameterized over four
     * unrelated effect types would be a factory with one member each.
     */
    private val turnSteps: List<Phrase<CardScript>> = listOf(
        // "You may play up to three additional lands this turn." — Summer Bloom. The count is a
        // word rather than a numeral, so it takes Cardinals rather than [countedStep]'s digit leaf.
        run {
            fun scriptFor(count: Int) = CardScript(spellEffect = PlayAdditionalLandsEffect(count))
            phrase("you may play up to {n} additional lands this turn", name = "play additional lands") {
                slot("n", Cardinals.word)
                build { scriptFor(it.int("n")) }
                match { script ->
                    val count = (script.spellEffect as? PlayAdditionalLandsEffect)?.count ?: return@match null
                    if (!Cardinals.spellable(count) || script != scriptFor(count)) return@match null
                    bind("n" to count)
                }
            }
        },
        // "Take an extra turn after this one. At the beginning of that turn's end step, you lose the
        // game." — Last Chance. Two printed sentences and one model: `loseAtEndStep` is a field on
        // the extra turn rather than a second effect, so there is nothing for [sequenceClause] to
        // split and the rule spans both sentences.
        run {
            val script = CardScript(spellEffect = TakeExtraTurnEffect(loseAtEndStep = true))
            phrase(
                "take an extra turn after this one. at the beginning of that turn's end step, you lose the game",
                name = "take an extra turn and lose",
            ) {
                build { script }
                match { if (it == script) bind() else null }
            }
        },
        // "You lose half your life, rounded up." — Cruel Bargain's second sentence.
        run {
            val script = CardScript(
                spellEffect = Effects.LoseLife(
                    DynamicAmount.Divide(DynamicAmount.LifeTotal(Player.You), DynamicAmount.Fixed(2), roundUp = true),
                    EffectTarget.Controller,
                )
            )
            phrase("you lose half your life, rounded up", name = "lose half your life") {
                build { script }
                match { if (it == script) bind() else null }
            }
        },
        // "If target opponent has more cards in hand than you, draw cards equal to the difference." —
        // Balance of Power. The "if" is inside the amount (`IfPositive` around a subtraction) rather
        // than around the effect, so this is one clause and not [conditionalClause]'s shape.
        run {
            val script = CardScript(
                spellEffect = Effects.DrawCards(DynamicAmounts.handSizeDifferenceFromTargetOpponent()),
                targetRequirements = listOf(Targets.opponent()),
            )
            phrase(
                "if target opponent has more cards in hand than you, draw cards equal to the difference",
                name = "draw the hand-size difference",
            ) {
                build { script }
                match { if (it == script) bind() else null }
            }
        },
    )

    // ---------------------------------------------------------------------------------------
    // A count and a filtered target together
    // ---------------------------------------------------------------------------------------

    /**
     * "~ deals 2 damage to target creature." — the same verb as above over a noun phrase rather than
     * over the fixed "any target" / "target player" forms, so it carries two slots instead of one.
     *
     * And, like those, in two printed forms: the numeral and the "equal to …" clause. It is written
     * out here rather than through [countedStepPair] because the target is a *slot* — the pair's
     * script takes the amount alone, and this sentence's script takes the amount and the filter — so
     * sharing the shape would mean parameterizing it over an arity nothing else needs. What is
     * shared is what matters: one `scriptFor`, one reconstruction, and the same domain split
     * (`Fixed` is the numeral, everything else is the clause).
     */
    private val damageToTargetPermanent: List<Phrase<CardScript>> = run {
        fun scriptFor(
            quantifier: Targets.Quantifier,
            amount: DynamicAmount,
            filter: GameObjectFilter,
        ) = CardScript(
            spellEffect = Effects.DealDamage(amount, Targets.bound()),
            targetRequirements = listOf(quantifier.requirement(1, filter)),
        )

        fun readBack(
            quantifier: Targets.Quantifier,
            script: CardScript,
            domain: (DynamicAmount) -> Boolean,
        ): Pair<DynamicAmount, GameObjectFilter>? {
            val amount = damageDealtAmount(script.spellEffect ?: return null) ?: return null
            if (!domain(amount)) return null
            val requirement = script.targetRequirements.singleOrNull() ?: return null
            val filter = Targets.targetedFilter(requirement) ?: return null
            if (script != scriptFor(quantifier, amount, filter)) return null
            return amount to filter
        }

        val bare = Targets.singularQuantifiers.first { !it.counted && it.prefix.isEmpty() }

        val heavy = { value: DynamicAmount -> value !is DynamicAmount.Fixed && value !is DynamicAmount.EntityProperty }
        val light = { value: DynamicAmount -> value is DynamicAmount.EntityProperty }

        fun clause(surface: String, ruleName: String, domain: (DynamicAmount) -> Boolean) =
            phrase<CardScript>(surface, name = ruleName) {
                slot("self", Primitives.self)
                slot("amount", Amounts.count)
                slot("filter", Filters.filter)
                build { bindings ->
                    bindings.value<DynamicAmount>("amount").takeIf(domain)
                        ?.let { scriptFor(bare, it, bindings.value("filter")) }
                }
                match { script ->
                    val (amount, filter) = readBack(bare, script, domain) ?: return@match null
                    bind("self" to Unit, "amount" to amount, "filter" to filter)
                }
            }

        // The numeral form takes the two singular quantifier rows — 11 printed lines, all of them
        // "up to one" (Chainsaw, Stress Dream, Mjölnir, Path to the World Tree). The plural rows are
        // deliberately absent for the reason [Targets.singularQuantifiers] states: damage over several
        // targets is spelled "divided as you choose among …" and is a different requirement.
        //
        // The "equal to …" clause forms keep the bare row alone, because the corpus prints no
        // quantified line for either of them — factor when the first member appears, not before.
        Targets.singularQuantifiers.map { quantifier ->
            phrase<CardScript>(
                quantifier.splice("{self} deals {n} damage to {q}target {filter}"),
                name = "deals damage to target permanent, ${quantifier.name}",
            ) {
                slot("self", Primitives.self)
                slot("n", Primitives.cardinal)
                slot("filter", Filters.filter)
                build { scriptFor(quantifier, DynamicAmount.Fixed(it.int("n")), it.value("filter")) }
                match { script ->
                    val (amount, filter) =
                        readBack(quantifier, script) { it is DynamicAmount.Fixed } ?: return@match null
                    bind("self" to Unit, "n" to (amount as DynamicAmount.Fixed).amount, "filter" to filter)
                }
            }
        } + listOf(
            clause(
                "{self} deals damage to target {filter} equal to {amount}",
                "deals damage to target permanent, by a count",
                heavy,
            ),
            clause(
                "{self} deals damage equal to {amount} to target {filter}",
                "deals damage to target permanent, by a property of an object",
                light,
            ),
            // The minority order for each domain: real Oracle, never printed. See
            // [countedStepPair] for the split and the counts behind it.
            alternate(
                clause(
                    "{self} deals damage to target {filter} equal to {amount}",
                    "deals damage to target permanent, by a property of an object (trailing)",
                    light,
                )
            ),
            alternate(
                clause(
                    "{self} deals damage equal to {amount} to target {filter}",
                    "deals damage to target permanent, by a count (leading)",
                    heavy,
                )
            ),
        )
    }

    /**
     * "Target creature gets +3/+3 until end of turn." — the pump spell, over each row of
     * [Targets.quantifiers] ("Up to two target creatures each get +2/+2 until end of turn.").
     *
     * The duration is spelled by the template and *not* by a slot: `Duration.EndOfTurn` is
     * `ModifyStats`'s default, and every other duration the SDK has ("as long as", "until your next
     * turn", `WhileSourceTapped`) is a different sentence rather than a different word in this one.
     * The reconstruct-and-compare in `match` is what makes that safe — a script whose duration is
     * anything else refuses to print here rather than losing the distinction.
     *
     * The **second** family to slot the quantifier table, and the reason it is a table: this
     * sentence shares nothing with [quantifiedPermanentSteps] but the noun phrase — it carries a
     * fronted spelling, a stat modifier and a verb that agrees in number — so a quantifier written
     * into one shape would have had to be written into the other. What the two share instead is the
     * table and the [effectOver]/[memberOf] pair.
     */
    private val pumpTargetPermanent: List<Phrase<CardScript>> = Targets.quantifiers.map { quantifier ->
        fun scriptFor(count: Int, modifiers: Pair<Int, Int>, filter: GameObjectFilter) = CardScript(
            spellEffect = quantifier.effectOver { Effects.ModifyStats(modifiers.first, modifiers.second, it) },
            targetRequirements = listOf(quantifier.requirement(count, filter)),
        )
        // "gets" for one creature and "each get" for several: the verb agrees with the quantifier,
        // which is the same reason [quantifiedPermanentSteps] takes two templates.
        val template = quantifier.splice(
            if (quantifier.plural) {
                "{q}target {filter} each get {mod} until end of turn"
            } else {
                "{q}target {filter} gets {mod} until end of turn"
            }
        )
        phrase(template, name = "pump, ${quantifier.name}") {
            frontedDuration()
            if (quantifier.counted) slot(Targets.COUNT_SLOT, Cardinals.word)
            slot("filter", if (quantifier.plural) Filters.plural else Filters.filter)
            slot("mod", Primitives.statModifiers)
            build {
                scriptFor(
                    if (quantifier.counted) it.int(Targets.COUNT_SLOT) else 1,
                    it.value("mod"),
                    it.value("filter"),
                )
            }
            match { script ->
                val modifiers = fixedModifiers(quantifier.memberOf(script.spellEffect)) ?: return@match null
                val requirement = script.targetRequirements.singleOrNull() ?: return@match null
                val filter = Targets.targetedFilter(requirement) ?: return@match null
                val count = if (quantifier.counted) requirement.count else 1
                if (quantifier.counted && !Cardinals.spellable(count)) return@match null
                if (script != scriptFor(count, modifiers, filter)) return@match null
                bind(Targets.COUNT_SLOT to count, "filter" to filter, "mod" to modifiers)
            }
        }
    }

    /**
     * "Put a +1/+1 counter on target creature.", "Put two -1/-1 counters on target Sliver you
     * control." — the counter verb over a noun phrase this clause chooses.
     *
     * ### Two rules for one verb, because English has two quantities
     *
     * The singular carries no number at all — its quantity *is* the article, which
     * [Primitives.singularCounterKind] owns — and the plural takes [Cardinals.word], which starts at
     * two. Disjoint domains, so one printed form per model and nothing for the printer to choose:
     * the same split [drawOne] and [drawMany] are, for the same reason, and the reason neither may
     * borrow the other's leaf.
     *
     * ### The quantifier, but only its singular rows
     *
     * Both rules take [Targets.singularQuantifiers] rather than the whole table — 19 printed lines
     * ("Put a +1/+1 counter on up to one target creature.", Essence Capture, Scale the Heights,
     * Winterthorn Blessing) — because this sentence's plural is not a plural noun. Oracle writes it
     * "put a +1/+1 counter on **each of** up to two target creatures", which is the distribute
     * sentence and its own family; taking the plural rows here would read a distribute model as this
     * one. That subset is a declaration with a reason, not an omission, and the reason lives in
     * [Targets.singularQuantifiers] beside the other sentence that needs it.
     *
     * The count is a **word** here and a numeral in [damageToTargetPermanent] two rules up. That is
     * not an inconsistency to tidy: Oracle spells a quantity of counters as a word ("put two +1/+1
     * counters") and a quantity of damage or life as a numeral ("deals 2 damage"), and the two
     * conventions live in the same sentence often enough that a rule using the wrong one would fail
     * to read the cards it was written for.
     */
    private val putCountersOnTargetPermanent: List<Phrase<CardScript>> =
        Targets.singularQuantifiers.flatMap { quantifier ->
            fun scriptFor(kind: String, count: Int, filter: GameObjectFilter) = CardScript(
                spellEffect = Effects.AddCounters(kind, count, Targets.bound()),
                targetRequirements = listOf(quantifier.requirement(1, filter)),
            )
            fun rule(template: String, name: String, quantity: Phrase<*>?) =
                phrase(quantifier.splice(template), name = "$name, ${quantifier.name}") {
                    slot("kind", if (quantity == null) Primitives.singularCounterKind else Primitives.counterKind)
                    if (quantity != null) slot("n", quantity)
                    slot("filter", Filters.filter)
                    build {
                        scriptFor(it.value("kind"), if (quantity == null) 1 else it.int("n"), it.value("filter"))
                    }
                    match { script ->
                        val (kind, count) = countersAdded(script.spellEffect, Targets.bound()) ?: return@match null
                        if (quantity == null && count != 1) return@match null
                        if (quantity != null && !(count >= 2 && Cardinals.spellable(count))) return@match null
                        val requirement = script.targetRequirements.singleOrNull() ?: return@match null
                        val filter = Targets.targetedFilter(requirement) ?: return@match null
                        if (script != scriptFor(kind, count, filter)) return@match null
                        bind("kind" to kind, "n" to count, "filter" to filter)
                    }
                }
            // `{n}` is the counter count and the quantifier spells no count of its own here — these
            // are the singular rows only, for the reason [Targets.singularQuantifiers] gives — so the
            // two numbers never collide in one template.
            listOf(
                rule("put {kind} counter on {q}target {filter}", "put a counter on a target", null),
                rule("put {n} {kind} counters on {q}target {filter}", "put counters on a target", Cardinals.word),
            )
        }

    /**
     * "Target creature gains flying until end of turn.", "…gains trample and lifelink until end of
     * turn." — the pump rule's keyword sibling, over [Keywords.keywordRun] rather than one keyword.
     *
     * One grant is the bare effect and several are a composite, which is not a special case but the
     * SDK's own shape: `CompositeEffect` means "these effects, in order", and a list of one has
     * nothing to sequence. Reading it the other way would print every single-keyword card's model as
     * a one-element composite and disagree with every hand-written card that spells it.
     *
     * The **third** family to slot [Targets.quantifiers], and the one that shows what the table was
     * for: its plural changes only the noun and the verb's agreement, so it is the same rows the pump
     * takes — "Any number of target creatures each gain double strike until end of turn." (Phalanx
     * Formation), "Up to one target creature you control gains protection from each of your opponents
     * until end of turn." (Courageous Resolve).
     */
    private val grantToTargetPermanent: List<Phrase<CardScript>> = Targets.quantifiers.map { quantifier ->
        fun scriptFor(count: Int, keywords: List<Keyword>, filter: GameObjectFilter) = CardScript(
            spellEffect = quantifier.effectOver { grants(keywords, it) },
            targetRequirements = listOf(quantifier.requirement(count, filter)),
        )
        // "gains" for one and "each gain" for several — the same agreement the pump sentence carries,
        // one verb over.
        val template = quantifier.splice(
            if (quantifier.plural) {
                "{q}target {filter} each gain {kws} until end of turn"
            } else {
                "{q}target {filter} gains {kws} until end of turn"
            }
        )
        phrase(template, name = "grant keywords to a target, ${quantifier.name}") {
            frontedDuration()
            if (quantifier.counted) slot(Targets.COUNT_SLOT, Cardinals.word)
            slot("filter", if (quantifier.plural) Filters.plural else Filters.filter)
            slot("kws", Keywords.keywordRun)
            build {
                scriptFor(
                    if (quantifier.counted) it.int(Targets.COUNT_SLOT) else 1,
                    it.value("kws"),
                    it.value("filter"),
                )
            }
            match { script ->
                val keywords = grantedKeywords(quantifier.memberOf(script.spellEffect)) ?: return@match null
                val requirement = script.targetRequirements.singleOrNull() ?: return@match null
                val filter = Targets.targetedFilter(requirement) ?: return@match null
                val count = if (quantifier.counted) requirement.count else 1
                if (quantifier.counted && !Cardinals.spellable(count)) return@match null
                if (script != scriptFor(count, keywords, filter)) return@match null
                bind(Targets.COUNT_SLOT to count, "filter" to filter, "kws" to keywords)
            }
        }
    }

    /**
     * "Target creature gets +3/+3 and gains flying until end of turn." — Angelic Blessing.
     *
     * One sentence, one target, **two** effects, which is why it is a rule of its own rather than a
     * [sequence]: the second clause has no subject of its own in the text, and the model shares one
     * requirement between the two effects. [Statics.pumpAndKeyword] is the same shape on the static
     * side, and the answer is the same — the model is already right, and what a compound SDK type
     * would buy is nothing.
     *
     * The **fourth** family to slot [Targets.quantifiers], and where the plural rows actually pay:
     * every quantified line the corpus prints for this sentence is plural — "Up to two target
     * creatures each get +1/+1 and gain lifelink until end of turn." (Cutthroat Maneuver, Coordinated
     * Assault, Press the Advantage), "Any number of target creatures each get +2/+0 and gain trample
     * until end of turn." (Rouse the Mob, Ajani's Presence, Aerial Formation). The whole compound
     * goes inside the iteration, because both halves are per-target: [effectOver] wraps the composite
     * rather than composing two iterations.
     *
     * The second verb does *not* take "each" — Oracle writes "each get +1/+1 and **gain** lifelink",
     * the adverb attaching once to the pair.
     */
    private val pumpAndGrantTarget: List<Phrase<CardScript>> = Targets.quantifiers.map { quantifier ->
        fun scriptFor(
            count: Int,
            modifiers: Pair<Int, Int>,
            keywords: List<Keyword>,
            filter: GameObjectFilter,
        ) = CardScript(
            spellEffect = quantifier.effectOver { target ->
                Effects.Composite(
                    listOf(Effects.ModifyStats(modifiers.first, modifiers.second, target)) +
                        keywords.map { Effects.GrantKeyword(it, target) }
                )
            },
            targetRequirements = listOf(quantifier.requirement(count, filter)),
        )
        val template = quantifier.splice(
            if (quantifier.plural) {
                "{q}target {filter} each get {mod} and gain {kws} until end of turn"
            } else {
                "{q}target {filter} gets {mod} and gains {kws} until end of turn"
            }
        )
        phrase(template, name = "pump and grant keywords to a target, ${quantifier.name}") {
            frontedDuration()
            if (quantifier.counted) slot(Targets.COUNT_SLOT, Cardinals.word)
            slot("filter", if (quantifier.plural) Filters.plural else Filters.filter)
            slot("mod", Primitives.statModifiers)
            slot("kws", Keywords.keywordRun)
            build {
                scriptFor(
                    if (quantifier.counted) it.int(Targets.COUNT_SLOT) else 1,
                    it.value("mod"),
                    it.value("kws"),
                    it.value("filter"),
                )
            }
            match { script ->
                val member = quantifier.memberOf(script.spellEffect)
                val effects = (member as? CompositeEffect)?.effects ?: return@match null
                val modifiers = fixedModifiers(effects.firstOrNull()) ?: return@match null
                val keywords = grantedKeywords(effects.drop(1)) ?: return@match null
                val requirement = script.targetRequirements.singleOrNull() ?: return@match null
                val filter = Targets.targetedFilter(requirement) ?: return@match null
                val count = if (quantifier.counted) requirement.count else 1
                if (quantifier.counted && !Cardinals.spellable(count)) return@match null
                if (script != scriptFor(count, modifiers, keywords, filter)) return@match null
                bind(
                    Targets.COUNT_SLOT to count,
                    "filter" to filter,
                    "mod" to modifiers,
                    "kws" to keywords,
                )
            }
        }
    }

    /**
     * "Destroy target nonblack creature. It can't be regenerated." — Skinthinner, Deathmark Prelate.
     *
     * Two printed sentences and one rule, for [destroyAllNoRegenerate]'s reason: `noRegenerate` is a
     * *marker effect placed before the destroy* rather than a second sentence's worth of behaviour,
     * so there is nothing for [sequenceClause] to split and the order in the model is the reverse of
     * the order in the text. `Effects.Destroy(target, noRegenerate = true)` composes exactly that
     * pair, which is why this goes through the facade rather than assembling it.
     */
    private val destroyTargetNoRegenerate: Phrase<CardScript> = run {
        fun scriptFor(filter: GameObjectFilter) = CardScript(
            spellEffect = Effects.Destroy(Targets.bound(), noRegenerate = true),
            targetRequirements = listOf(Targets.permanent(filter)),
        )
        phrase("destroy target {filter}. it can't be regenerated", name = "destroy target without regeneration") {
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
     * "Destroy that creature. It can't be regenerated." — Dripping Dead, Phage, Toxin Sliver.
     *
     * "That creature" here is the creature the *trigger* named, not a target the spell chose, which
     * is a third anaphor beside [SelfSteps]' "it" and [Continuations]' "that creature": the sentence
     * follows "Whenever ~ deals combat damage to a creature", and the model says
     * `EffectTarget.TriggeringEntity`. It is reachable as an ordinary first clause because it
     * introduces nothing — the trigger already did — and it cannot collide with [Continuations],
     * which is only reachable from a *later* clause position.
     */
    private val destroyTriggeringNoRegenerate: Phrase<CardScript> = run {
        val script = CardScript(
            spellEffect = Effects.Destroy(EffectTarget.TriggeringEntity, noRegenerate = true)
        )
        phrase(
            "destroy that creature. it can't be regenerated",
            name = "destroy the triggering creature without regeneration",
        ) {
            build { script }
            match { if (it == script) bind() else null }
        }
    }

    /**
     * "Sacrifice a permanent.", "Sacrifice a land." — the controller sacrifices, with no target.
     *
     * `SacrificeEffect` carries no player at all: the ability's controller is the one who
     * sacrifices, which is what the bare imperative means. `Effects.Sacrifice(filter, 1, target)` is
     * a *different* type (`ForceSacrificeEffect`) naming a player, and the two are a "one concept,
     * two spellings" pair the corpus is split over — Drinker of Sorrow writes the first and Goblin
     * Firebug the second. The grammar emits the one whose model says what the sentence says and lets
     * the differential report the rest, per the module's rule for two SDK spellings.
     */
    private val sacrificeFiltered: Phrase<CardScript> = run {
        fun scriptFor(filter: GameObjectFilter) = CardScript(spellEffect = SacrificeEffect(filter))
        phrase("sacrifice {filter}", name = "sacrifice a permanent") {
            slot("filter", Filters.indefinite)
            build { scriptFor(it.value("filter")) }
            match { script ->
                val effect = script.spellEffect as? SacrificeEffect ?: return@match null
                if (script != scriptFor(effect.filter)) return@match null
                bind("filter" to effect.filter)
            }
        }
    }

    /**
     * "Target creature can't be blocked this turn." — Cephalid Pathmage.
     *
     * The SDK grants the *flag* rather than a keyword here, which is the same two-places-for-one-
     * thing finding [Grammar.flagLine] records: `AbilityFlag.CANT_BE_BLOCKED` is a card-level flag
     * for the permanent form and a `GrantKeywordEffect` over the flag's own name for the durational
     * one. The rule spells the flag's name because that is what the cards carry.
     */
    private val targetCantBeBlocked: Phrase<CardScript> = run {
        fun scriptFor(filter: GameObjectFilter) = CardScript(
            spellEffect = GrantKeywordEffect(AbilityFlag.CANT_BE_BLOCKED.name, Targets.bound()),
            targetRequirements = listOf(Targets.permanent(filter)),
        )
        phrase("target {filter} can't be blocked this turn", name = "target can't be blocked") {
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

    /** "You lose the game." / "That player loses the game." — Phage the Untouchable, both halves. */
    private fun losesTheGame(template: String, name: String, player: EffectTarget): Phrase<CardScript> {
        val script = CardScript(spellEffect = Effects.LoseGame(player))
        return phrase(template, name = name) {
            build { script }
            match { if (it == script) bind() else null }
        }
    }

    /**
     * The verbs [quantifiedPermanentSteps] is instantiated for — one entry, one rule per quantifier.
     *
     * The two verbs carrying a possessive past the noun spell their plural, and every other one is
     * the same string twice because the noun phrase is where its sentence ends.
     *
     * Declared before [permanentSteps] uses it, per the ordering rule [Primitives] states: object
     * initializers run in declaration order.
     */
    private val quantifiedPermanentStepFamilies: List<List<Phrase<CardScript>>> = listOf(
        quantifiedPermanentSteps("destroy {q}target {filter}", "destroy") { Effects.Destroy(it) },
        quantifiedPermanentSteps("regenerate {q}target {filter}", "regenerate") { RegenerateEffect(it) },
        quantifiedPermanentSteps("exile {q}target {filter}", "exile") { Effects.Exile(it) },
        quantifiedPermanentSteps("tap {q}target {filter}", "tap") { Effects.Tap(it) },
        quantifiedPermanentSteps("untap {q}target {filter}", "untap") { Effects.Untap(it) },
        quantifiedPermanentSteps(
            singular = "return {q}target {filter} to its owner's hand",
            name = "return to hand",
            plural = "return {q}target {filter} to their owners' hands",
            // Oracle prints the plural possessive both ways — "their owners' hands" 110 times and
            // "their owner's hand" 55 — so the minority is an alternate on this rule rather than a
            // rule of its own, sharing its `build` and never printing. Older cards (Scapegoat, Aether
            // Burst) carry the singular noun; `Combat.returnOneOrTwoTargets` still spells it whole.
            pluralAlternate = "return {q}target {filter} to their owner's hand",
        ) { Effects.ReturnToHand(it) },
        quantifiedPermanentSteps(
            singular = "put {q}target {filter} on top of its owner's library",
            name = "put on top of its library",
            plural = "put {q}target {filter} on top of their owners' libraries",
        ) { Effects.PutOnTopOfLibrary(it) },
    )

    private val permanentSteps: List<Phrase<CardScript>> = listOf(
        destroyTargetNoRegenerate,
        destroyTriggeringNoRegenerate,
        sacrificeFiltered,
        targetCantBeBlocked,
        losesTheGame("you lose the game", "you lose the game", EffectTarget.Controller),
        losesTheGame(
            "that player loses the game",
            "the triggering player loses the game",
            EffectTarget.PlayerRef(Player.TriggeringPlayer),
        ),
    ) + quantifiedPermanentStepFamilies.flatten()

    // ---------------------------------------------------------------------------------------
    // Whole groups — "Creatures you control get +1/+1", "Destroy all white creatures"
    // ---------------------------------------------------------------------------------------

    /**
     * The mass effects, which the SDK spells as one iteration over a `GroupFilter` with the
     * per-member effect written against [EffectTarget.Self].
     *
     * One shape, four surfaces, because English gives the same model four templates and the
     * difference between them is the *noun phrase*, not the verb: a bare plural subject ("Creatures
     * you control get …"), "all" plus a plural ("Destroy all white creatures"), and "each" plus a
     * singular ("deals 1 damage to each attacking creature"). Which one a card prints is a fact
     * about the sentence's shape rather than about the group, so the templates are enumerated and
     * the group filter is [Filters.plural] or [Filters.filter] slotted whole.
     *
     * `GroupFilter(filter)` and nothing else: `excludeSelf`, `excludeTarget`, a non-battlefield
     * scope and `noRegenerate` all say things these sentences do not, and the reconstruct-and-
     * compare refuses to print a value carrying any of them.
     */
    private fun groupStep(
        template: String,
        name: String,
        plural: Boolean,
        member: (EffectTarget) -> Effect,
    ): Phrase<CardScript> {
        fun scriptFor(filter: GameObjectFilter) = CardScript(
            spellEffect = Effects.ForEachInGroup(GroupFilter(filter), member(EffectTarget.Self)),
        )
        return phrase(template, name = name) {
            slot("filter", if (plural) Filters.plural else Filters.filter)
            build { scriptFor(it.value("filter")) }
            match { script ->
                val filter = iteratedGroup(script.spellEffect) ?: return@match null
                if (script != scriptFor(filter)) return@match null
                bind("filter" to filter)
            }
        }
    }

    /**
     * The mass effects whose per-member effect also carries a number or a keyword.
     *
     * Written as its own shape rather than as a parameter on [groupStep] because the extra slot
     * changes both halves of the inversion: `member` has to be reconstructed from a value read back
     * out of the iterated effect, which [groupStep]'s fixed `member` never needs.
     */
    private fun <V> parameterizedGroupStep(
        template: String,
        name: String,
        parameter: Phrase<V>,
        plural: Boolean,
        member: (V, EffectTarget) -> Effect,
        read: (Effect) -> V?,
        canonicalForm: Boolean = true,
    ): Phrase<CardScript> {
        fun scriptFor(value: V, filter: GameObjectFilter) = CardScript(
            spellEffect = Effects.ForEachInGroup(GroupFilter(filter), member(value, EffectTarget.Self)),
        )
        val rule = phrase<CardScript>(template, name = name) {
            // This shape carries durational and non-durational sentences alike — "{filter} get {v}
            // until end of turn" and "{self} deals {v} damage to each {filter}" — so the position
            // applies to the ones that have a duration to move.
            if (Durations.isDurational(template)) frontedDuration()
            if (template.contains("{self}")) slot("self", Primitives.self)
            slot("filter", if (plural) Filters.plural else Filters.filter)
            slot("v", parameter)
            build { scriptFor(it.value("v"), it.value("filter")) }
            match { script ->
                val filter = iteratedGroup(script.spellEffect) ?: return@match null
                val value = read(iteratedBody(script.spellEffect) ?: return@match null) ?: return@match null
                if (script != scriptFor(value, filter)) return@match null
                bind("self" to Unit, "filter" to filter, "v" to value)
            }
            canonical = canonicalForm
        }
        return if (canonicalForm) rule else alternate(rule)
    }

    /**
     * "Soldier creatures get +1/+1 and gain first strike until end of turn." — Gempalm Avenger.
     *
     * [pumpAndGrantTarget]'s group-side twin, and one rule for the same reason: the second clause
     * has no subject of its own.
     *
     * **One iteration carrying both effects, not two iterations over the same group.** The sentence
     * names its group once and says two things about *those* permanents, and the SDK spells that as
     * a `CompositeEffect` inside a single `ForEachInGroup` — which is what all eight hand-written
     * cards of this shape do (Overrun, Flame-Kin Zealot, Make a Stand, Dance of Shadows, Vampiric
     * Fury, Angel of the Dawn, Stagecoach Security, Preposterous Proportions). The rule used to
     * build two passes and assert that the doubled group "is the SDK's shape"; the differential
     * reported every one of the eight, which is what a claim like that looks like when it is wrong.
     * Two passes also gather twice, and nothing in the printed line says to.
     */
    private fun groupPumpAndGrant(prefix: String, name: String, canonicalForm: Boolean): Phrase<CardScript> {
        fun scriptFor(
            modifiers: Pair<Int, Int>,
            keywords: List<Keyword>,
            filter: GameObjectFilter,
        ) = CardScript(
            spellEffect = Effects.ForEachInGroup(
                GroupFilter(filter),
                Effects.Composite(
                    listOf(Effects.ModifyStats(modifiers.first, modifiers.second, EffectTarget.Self)) +
                        keywords.map { Effects.GrantKeyword(it, EffectTarget.Self) }
                ),
            )
        )
        val rule = phrase<CardScript>("$prefix{filter} get {mod} and gain {kws} until end of turn", name = name) {
            frontedDuration()
            slot("filter", Filters.plural)
            slot("mod", Primitives.statModifiers)
            slot("kws", Keywords.keywordRun)
            build { scriptFor(it.value("mod"), it.value("kws"), it.value("filter")) }
            match { script ->
                val filter = iteratedGroup(script.spellEffect) ?: return@match null
                val body = (iteratedBody(script.spellEffect) as? CompositeEffect)?.effects ?: return@match null
                val modifiers = fixedModifiers(body.firstOrNull()) ?: return@match null
                val keywords = grantedKeywords(body.drop(1)) ?: return@match null
                if (script != scriptFor(modifiers, keywords, filter)) return@match null
                bind("filter" to filter, "mod" to modifiers, "kws" to keywords)
            }
            canonical = canonicalForm
        }
        return if (canonicalForm) rule else alternate(rule)
    }

    /**
     * The same shape over a group that **excludes the source** — "tap all *other* creatures."
     *
     * `excludeSelf` is a field on the `GroupFilter` rather than on the base filter, which is exactly
     * why it is a separate rule and not a [Filters] layer: "other" is a fact about the iteration's
     * relationship to the ability's source, not about what a permanent is.
     */
    private fun otherGroupStep(
        template: String,
        name: String,
        member: (EffectTarget) -> Effect,
    ): Phrase<CardScript> {
        fun scriptFor(filter: GameObjectFilter) = CardScript(
            spellEffect = Effects.ForEachInGroup(
                GroupFilter(filter, excludeSelf = true),
                member(EffectTarget.Self),
            ),
        )
        return phrase(template, name = name) {
            slot("filter", Filters.plural)
            build { scriptFor(it.value("filter")) }
            match { script ->
                val filter = iteratedGroup(script.spellEffect) ?: return@match null
                if (script != scriptFor(filter)) return@match null
                bind("filter" to filter)
            }
        }
    }

    /**
     * "Destroy all creatures. They can't be regenerated." — Wrath of God.
     *
     * Two printed sentences and one model: `noRegenerate` is a field on the *same* iteration, not a
     * second effect, so [sequence] has nothing to split. The rule therefore spans both sentences,
     * which is what makes the plain "Destroy all creatures." rule above safe — a sweep that forbids
     * regeneration refuses to print as one that does not.
     */
    /**
     * "Destroy all creatures." — the sweep, through [Effects.DestroyAll] rather than an iteration.
     *
     * Not `ForEachInGroup(filter, Destroy(Self))`, which is the same sentence's other SDK spelling
     * and the one this rule used to build. `DestroyAll` lowers to the gather-then-move pipeline, and
     * the difference is not cosmetic: the gather reads the battlefield through *projected* state, so
     * a filter that names a characteristic a continuous effect can change ("nonland permanents with
     * mana value 1 or less" — Pest Control) is evaluated against what the permanents actually are.
     * That is this repo's standing rule for battlefield filters, and it is why the facade exists.
     */
    private val destroyAll: Phrase<CardScript> = run {
        fun scriptFor(filter: GameObjectFilter) = CardScript(spellEffect = Effects.DestroyAll(filter))
        phrase("destroy all {filter}", name = "destroy all") {
            slot("filter", Filters.plural)
            build { scriptFor(it.value("filter")) }
            match { script ->
                val filter = destroyedGroup(script.spellEffect) ?: return@match null
                if (script != scriptFor(filter)) return@match null
                bind("filter" to filter)
            }
        }
    }

    private val destroyAllNoRegenerate: Phrase<CardScript> = run {
        fun scriptFor(filter: GameObjectFilter) = CardScript(
            spellEffect = Effects.DestroyAll(filter, noRegenerate = true),
        )
        phrase("destroy all {filter}. they can't be regenerated", name = "destroy all without regeneration") {
            slot("filter", Filters.plural)
            build { scriptFor(it.value("filter")) }
            match { script ->
                val filter = destroyedGroup(script.spellEffect) ?: return@match null
                if (script != scriptFor(filter)) return@match null
                bind("filter" to filter)
            }
        }
    }

    private val groupSteps: List<Phrase<CardScript>> = listOf(
        destroyAll,
        groupStep("exile all {filter}", "exile all", plural = true) { Effects.Exile(it) },
        groupStep("tap all {filter}", "tap all", plural = true) { Effects.Tap(it) },
        groupStep("untap all {filter}", "untap all", plural = true) { Effects.Untap(it) },
        otherGroupStep("tap all other {filter}", "tap all other") { Effects.Tap(it) },
        otherGroupStep("untap all other {filter}", "untap all other") { Effects.Untap(it) },
        destroyAllNoRegenerate,
        parameterizedGroupStep(
            "{filter} get {v} until end of turn", "a group gets",
            parameter = Primitives.statModifiers, plural = true,
            member = { (power, toughness), target -> Effects.ModifyStats(power, toughness, target) },
            read = ::fixedModifiers,
        ),
        parameterizedGroupStep(
            "{filter} gain {v} until end of turn", "a group gains a keyword",
            parameter = Keywords.keyword, plural = true,
            member = { keyword, target -> Effects.GrantKeyword(keyword, target) },
            read = ::grantedKeyword,
        ),
        parameterizedGroupStep(
            "{self} deals {v} damage to each {filter}", "deals damage to each",
            parameter = Primitives.cardinal, plural = false,
            member = { amount, target -> Effects.DealDamage(amount, target) },
            read = ::damageDealt,
        ),
        // "All creatures get -5/-5 until end of turn." — the same value with the word "all" in
        // front, which `GroupFilter` has no room for. The bare form is canonical because it is what
        // the modern lord and mass-pump templating prints; this parses and never prints, so those
        // cards come back as a variant. Same treatment [Statics.lordStatic] gives the static side.
        parameterizedGroupStep(
            "all {filter} get {v} until end of turn", "all of a group gets",
            parameter = Primitives.statModifiers, plural = true,
            member = { (power, toughness), target -> Effects.ModifyStats(power, toughness, target) },
            read = ::fixedModifiers,
            canonicalForm = false,
        ),
        parameterizedGroupStep(
            "all {filter} gain {v} until end of turn", "all of a group gains a keyword",
            parameter = Keywords.keyword, plural = true,
            member = { keyword, target -> Effects.GrantKeyword(keyword, target) },
            read = ::grantedKeyword,
            canonicalForm = false,
        ),
        groupPumpAndGrant("", "a group gets and gains", canonicalForm = true),
        groupPumpAndGrant("all ", "all of a group gets and gains", canonicalForm = false),
    )

    // ---------------------------------------------------------------------------------------
    // Damage whose amount is not a numeral
    // ---------------------------------------------------------------------------------------

    /**
     * "~ deals X damage to any target." — the X spells.
     *
     * `DynamicAmount.XValue` is a *constant* in the model and a literal in the text, so these go
     * through [amountStep] rather than [countedStep]: there is no number to read back, only a
     * reconstruction to compare against.
     */
    private val xDamageSteps: List<Phrase<CardScript>> = listOf(
        amountStep("{self} deals X damage to any target", "deals X damage to any target", DynamicAmount.XValue) {
            CardScript(
                spellEffect = Effects.DealDamage(it, Targets.bound()),
                targetRequirements = listOf(Targets.any()),
            )
        },
        amountStep(
            "{self} deals X damage to target player or planeswalker",
            "deals X damage to target player or planeswalker",
            DynamicAmount.XValue,
        ) {
            CardScript(
                spellEffect = Effects.DealDamage(it, Targets.bound()),
                targetRequirements = listOf(Targets.playerOrPlaneswalker()),
            )
        },
        amountStep(
            "{self} deals damage to target opponent or planeswalker equal to the sacrificed creature's power",
            "deals damage equal to the sacrificed creature's power",
            DynamicAmounts.sacrificedPower(),
        ) {
            CardScript(
                spellEffect = Effects.DealDamage(it, Targets.bound()),
                targetRequirements = listOf(Targets.opponentOrPlaneswalker()),
            )
        },
    )

    /**
     * "~ deals 1 damage to each creature and each player." — the symmetric sweeps.
     *
     * One printed sentence, two effects: the board half is the ordinary [groupStep] iteration and
     * the player half is a single damage effect aimed at `Player.Each`. It is one rule rather than a
     * [sequence] because the sentence is one sentence; a card printing the two halves separately
     * denotes the identical model and comes back as a variant.
     *
     * **The player half has two SDK spellings and this emits one.** `DealDamage(n, PlayerRef(Each))`
     * and `ForEachPlayer(Each, [DealDamage(n, Controller)])` are equivalent for a fixed amount, and
     * the second is what the grammar prints — not because per-player controller rebinding is needed
     * here, but because it is the spelling the corpus actually uses: 21 hand-written cards of this
     * shape write it and none writes the other, so printing the other would be the grammar inventing
     * a house style. The differential reported ten of the 21 the moment it could read them, which is
     * the whole argument: a rule may pick either of two spellings, but not the one nobody writes.
     */
    private fun damageToEachAndEachPlayer(
        template: String,
        name: String,
        amount: Phrase<Int>?,
        fixed: DynamicAmount?,
    ): Phrase<CardScript> {
        fun scriptFor(value: DynamicAmount, filter: GameObjectFilter) = CardScript(
            spellEffect = Effects.Composite(
                listOf(
                    Effects.ForEachInGroup(GroupFilter(filter), Effects.DealDamage(value, EffectTarget.Self)),
                    Effects.ForEachPlayer(
                        Player.Each,
                        listOf(Effects.DealDamage(value, EffectTarget.Controller)),
                    ),
                )
            )
        )
        return phrase(template, name = name) {
            slot("self", Primitives.self)
            slot("filter", Filters.filter)
            if (amount != null) slot("n", amount)
            build { bindings ->
                val value = fixed ?: DynamicAmount.Fixed(bindings.int("n"))
                scriptFor(value, bindings.value("filter"))
            }
            match { script ->
                val effects = (script.spellEffect as? CompositeEffect)?.effects ?: return@match null
                val filter = iteratedGroup(effects.firstOrNull()) ?: return@match null
                val perPlayer = iteratedPlayers(effects.getOrNull(1)) ?: return@match null
                val value = (perPlayer as? DealDamageEffect)?.amount ?: return@match null
                if (fixed != null && value != fixed) return@match null
                val number = if (fixed != null) null else value.fixed() ?: return@match null
                if (script != scriptFor(value, filter)) return@match null
                bind("self" to Unit, "filter" to filter, "n" to number)
            }
        }
    }

    // ---------------------------------------------------------------------------------------
    // Counting the battlefield — "You gain 2 life for each Mountain target opponent controls."
    // ---------------------------------------------------------------------------------------

    /** "Draw a card for each tapped creature target opponent controls." — Theft of Dreams. */
    private val drawForEach: Phrase<CardScript> = run {
        fun scriptFor(counted: GameObjectFilter) = CardScript(
            spellEffect = Effects.DrawCards(DynamicAmount.AggregateBattlefield(Player.TargetOpponent, counted)),
            targetRequirements = listOf(Targets.opponent()),
        )
        phrase("draw a card for each {filter} target opponent controls", name = "draw for each") {
            slot("filter", Filters.filter)
            build { scriptFor(it.value("filter")) }
            match { script ->
                val amount = (script.spellEffect as? DrawCardsEffect)?.count
                    as? DynamicAmount.AggregateBattlefield ?: return@match null
                if (script != scriptFor(amount.filter)) return@match null
                bind("filter" to amount.filter)
            }
        }
    }

    /**
     * "You gain N life for each …" — the first rules whose amount is a [DynamicAmount] rather than
     * a numeral.
     *
     * Two shapes over disjoint counts, for exactly the reason the draw rules are two: the SDK writes
     * "1 life for each X" as the bare count and "2 life for each X" as `Multiply(count, 2)`, so one
     * is not a special case of the other in the model either. Refusing 1 in the multiplying rule is
     * what keeps one printed form per model.
     *
     * The *whose battlefield* half is a `Player` inside the aggregate and, when it is a targeted
     * one, a `TargetRequirement` beside it — two places for one printed clause, which is why the
     * surface and both halves are passed together rather than derived.
     */
    private fun gainLifeForEach(
        surface: String,
        player: Player,
        requirements: List<TargetRequirement>,
    ): List<Phrase<CardScript>> {
        fun scriptFor(amount: DynamicAmount) = CardScript(
            spellEffect = Effects.GainLife(amount),
            targetRequirements = requirements,
        )

        fun count(filter: GameObjectFilter): DynamicAmount = DynamicAmount.AggregateBattlefield(player, filter)

        /** The aggregate this surface spells, or null when the value counts someone else's board. */
        fun aggregate(amount: DynamicAmount?): DynamicAmount.AggregateBattlefield? =
            (amount as? DynamicAmount.AggregateBattlefield)
                ?.takeIf { it == count(it.filter) }

        val one = phrase("you gain 1 life for each {filter} $surface", name = "gain one life for each") {
            slot("filter", Filters.filter)
            build { scriptFor(count(it.value("filter"))) }
            match { script ->
                val total = aggregate((script.spellEffect as? GainLifeEffect)?.amount) ?: return@match null
                if (script != scriptFor(total)) return@match null
                bind("filter" to total.filter)
            }
        }
        val many = phrase("you gain {n} life for each {filter} $surface", name = "gain life for each") {
            slot("n", Primitives.cardinal)
            slot("filter", Filters.filter)
            build { bindings ->
                val multiplier = bindings.int("n")
                if (multiplier < 2) return@build null
                scriptFor(DynamicAmount.Multiply(count(bindings.value("filter")), multiplier))
            }
            match { script ->
                val product = (script.spellEffect as? GainLifeEffect)?.amount as? DynamicAmount.Multiply
                    ?: return@match null
                val total = aggregate(product.amount) ?: return@match null
                if (product.multiplier < 2) return@match null
                if (script != scriptFor(product)) return@match null
                bind("n" to product.multiplier, "filter" to total.filter)
            }
        }
        return listOf(one, many)
    }

    // ---------------------------------------------------------------------------------------
    // The sentence, the sequence, and the line
    // ---------------------------------------------------------------------------------------

    /**
     * Every clause rule, as one alternation.
     *
     * [Mana.addClause] is a member declared elsewhere: producing mana is a spell effect in its own
     * right (Dark Ritual) *and* the effect clause of nearly every activated ability, so its two
     * halves — the sentence and the choice form that denotes several abilities — are kept together
     * in [Mana] rather than split across two files by which slot each one reaches. [Library],
     * [Hand], [SelfSteps] and [Combat] are the same: one file per topic, all of them rows here.
     */
    /**
     * "You gain 3 life for each creature attacking you." — Blessed Reversal.
     *
     * Not a surface of [gainLifeForEach], because "attacking you" is *two* fields in the model — the
     * opponents' battlefield and the attacking state — for one printed phrase, and a rule that
     * slotted the noun would have to put half of the phrase in the slot and half in the surface.
     * Spelled once, with the noun fixed.
     */
    private val gainLifePerAttacker: Phrase<CardScript> = run {
        fun scriptFor(multiplier: Int) = CardScript(
            spellEffect = Effects.GainLife(
                DynamicAmount.Multiply(
                    DynamicAmount.AggregateBattlefield(
                        Player.EachOpponent,
                        GameObjectFilter.Creature.attacking(),
                    ),
                    multiplier,
                )
            )
        )
        phrase("you gain {n} life for each creature attacking you", name = "gain life per attacker") {
            slot("n", Primitives.cardinal)
            build { bindings -> bindings.int("n").takeIf { it >= 2 }?.let(::scriptFor) }
            match { script ->
                val product = (script.spellEffect as? GainLifeEffect)?.amount as? DynamicAmount.Multiply
                    ?: return@match null
                if (product.multiplier < 2 || script != scriptFor(product.multiplier)) return@match null
                bind("n" to product.multiplier)
            }
        }
    }

    /**
     * Every atom that does **not** turn on the pronoun — what both cascades share.
     *
     * The anaphor vocabulary is the one thing a [Cascade] supplies for itself, because it is the one
     * thing whose meaning depends on where the clause sits. Everything here means the same in every
     * position, so it is built once.
     */
    private val nonAnaphoric: List<Phrase<CardScript>> =
        listOf(drawOne, drawMany, targetPlayerDrawsOne, targetPlayerDrawsMany) +
            countedSteps +
            xDamageSteps +
            damageToTargetPermanent +
            damageToEachAndEachPlayer(
                "{self} deals {n} damage to each {filter} and each player",
                "deals damage to each permanent and each player",
                amount = Primitives.cardinal,
                fixed = null,
            ) +
            damageToEachAndEachPlayer(
                "{self} deals X damage to each {filter} and each player",
                "deals X damage to each permanent and each player",
                amount = null,
                fixed = DynamicAmount.XValue,
            ) +
            pumpTargetPermanent +
            grantToTargetPermanent +
            pumpAndGrantTarget +
            putCountersOnTargetPermanent +
            permanentSteps +
            groupSteps +
            drawForEach +
            gainLifePerAttacker +
            gainLifeForEach("on the battlefield", Player.Each, emptyList()) +
            gainLifeForEach("target opponent controls", Player.TargetOpponent, listOf(Targets.opponent())) +
            turnSteps +
            sentenceClauses +
            exchangeControl +
            Stack.clauses +
            Mana.addClause +
            Mana.addClauses +
            Library.clauses +
            TopOfLibrary.clauses +
            Hand.clauses +
            Combat.clauses +
            Graveyard.clauses +
            Amounts.clauses +
            Morph.clauses +
            CreatureTypes.clauses +
            Tokens.clauses +
            Prevention.clauses +
            SelfSteps.clauses

    /**
     * One clause, plus the joined form of two.
     *
     * "~ deals 4 damage to any target and you gain 4 life." and "~ deals 4 damage to any target. You
     * gain 4 life." denote the identical `CompositeEffect`: the model has no room for the
     * conjunction, so exactly one of the two is canonical and the other parses without printing.
     * The sequence wins because it is what the corpus overwhelmingly prints and because it composes
     * to any length; a card printing the join comes back as a
     * [com.wingedsheep.assay.gate.LineVerdict.VARIANT], which says the reading was right and only
     * the spelling moved.
     */

    /**
     * What joins one clause to the next inside a line — a full stop, "and", or ", then".
     *
     * All three denote the same thing, because a `CompositeEffect` has no room for the conjunction:
     * the model says *these effects, in this order* and nothing about the word between them. So the
     * full stop is canonical and the other two are [alternate]s, and a card printing a join comes
     * back as a [com.wingedsheep.assay.gate.LineVerdict.VARIANT] — the reading was right, only the
     * spelling moved, which is worth strictly more than the decline it replaces.
     *
     * The separator belongs to the *tail* rather than to the run, which is what lets one line mix
     * them: "Scry 2, then draw two cards. You lose 2 life." is three clauses joined two different
     * ways, and it folds to the same flat composite the all-full-stops spelling does. A run with one
     * separator could not read that line at all, and a join rule that was itself a clause would have
     * folded it into a *nested* composite — a model no card carries and nothing could print.
     */
    private fun tail(
        separator: String,
        canonicalJoin: Boolean,
        later: Phrase<CardScript>,
        name: String,
    ): Phrase<CardScript> {
        val rule = phrase<CardScript>("$separator{clause}", name = name) {
            slot("clause", later)
            build { it.value("clause") }
            match { bind("clause" to it) }
            canonical = canonicalJoin
        }
        return if (canonicalJoin) rule else alternate(rule)
    }

    private fun tailsOf(later: Phrase<CardScript>, name: String): Phrase<CardScript> = oneOf(
        name,
        tail(". ", canonicalJoin = true, later = later, name = name),
        tail(", then ", canonicalJoin = false, later = later, name = name),
        tail(" and ", canonicalJoin = false, later = later, name = name),
    )

    /**
     * "Target creature gets +1/+3 until end of turn. Untap that creature." — two or more clauses on
     * one printed line, which the SDK models as one `CompositeEffect`.
     *
     * **A target is declared at its first mention, which is not always the first clause.** English
     * introduces a referent before it refers back to one, so the requirement belongs to the clause
     * that names it and every later one is either self-contained or a [Continuations] clause reading
     * the slot that clause declared. Fleshformer is what a line looks like when the introducing
     * clause is *second*: "~ gets +2/+2 and gains fear until end of turn. Target creature gets -2/-2
     * until end of turn."
     *
     * `match` therefore looks for the owning clause rather than assuming index 0 — but it decides by
     * *printability*, not by preference: a clause that needs the requirement cannot print without
     * it, and one that does not cannot print with it, because every rule here reconstructs the whole
     * script and compares. At most one position can satisfy both, so the split stays deterministic
     * and a model no position can print declines rather than being guessed at.
     *
     * The run's separator is the empty string because each tail carries its own; see [tail].
     *
     * The shape is a function rather than a rule because a line is not the only thing built out of
     * clauses joined this way: a gate's consequence is too, and it joins a *narrower* vocabulary
     * (see [gatedConsequence]). One shape, two members — which is when this file factors.
     */
    private fun clauseRun(
        first: Phrase<CardScript>,
        later: Phrase<CardScript>,
        name: String,
    ): Phrase<CardScript> = phrase("{first}{rest}", name = name) {
        slot("first", first)
        slot("rest", separated(name, tailsOf(later, name), separator = "", min = 1))
        build { merge(listOf(it.value<CardScript>("first")) + it.value<List<CardScript>>("rest")) }
        match { script ->
            val composite = script.spellEffect as? CompositeEffect ?: return@match null
            if (composite.effects.size < 2) return@match null
            if (composite != CompositeEffect(composite.effects)) return@match null
            val owner = composite.effects.indices.firstOrNull { index ->
                val candidate = clauseParts(composite.effects, script.targetRequirements, index)
                merge(candidate) == script && printable(candidate, first, later)
            } ?: return@match null
            val parts = clauseParts(composite.effects, script.targetRequirements, owner)
            bind("first" to parts.first(), "rest" to parts.drop(1))
        }
    }

    /** Re-wrap a clause's effect, keeping the targets it declared. Shared by the two wrappers. */
    private fun wrap(inner: CardScript, wrapper: (Effect) -> Effect): CardScript? {
        val effect = inner.spellEffect ?: return null
        if (inner != CardScript(spellEffect = effect, targetRequirements = inner.targetRequirements)) return null
        return CardScript(spellEffect = wrapper(effect), targetRequirements = inner.targetRequirements)
    }

    /** The line's clauses, with the whole line's requirements attached to clause [owner]. */
    private fun clauseParts(
        effects: List<Effect>,
        requirements: List<TargetRequirement>,
        owner: Int,
    ): List<CardScript> = effects.mapIndexed { index, effect ->
        CardScript(
            spellEffect = effect,
            targetRequirements = if (index == owner) requirements else emptyList(),
        )
    }

    /** True when every clause of a split can be printed from the position it sits in. */
    private fun printable(
        parts: List<CardScript>,
        first: Phrase<CardScript>,
        later: Phrase<CardScript>,
    ): Boolean = first.unparse(parts.first()) != null && parts.drop(1).all { later.unparse(it) != null }

    /**
     * Fold clause scripts into the one script the line denotes, or null when they cannot be one.
     *
     * A clause may contribute a spell effect and the targets it declared and nothing else; anything
     * more is content this fold would silently drop, so it refuses instead.
     *
     * ### Two clauses that each declare a target refuse to fold
     *
     * [Targets.SLOT] is a single fixed name, which is enough while every rule takes at most one
     * target — and stops being enough exactly here. "Destroy target land. ~ deals 13 damage to
     * target creature." would fold into a script with *two* requirements both called `target` and
     * two effects both reading that name, which is not the card: it is a model in which the second
     * slot has no way to be referred to. Refusing is the fail-closed answer, and the gap it names is
     * a slot-name **generator** in [Targets] rather than anything about this fold. Until that
     * exists, those lines decline and are counted.
     */
    private fun merge(parts: List<CardScript>): CardScript? {
        if (parts.count { it.targetRequirements.isNotEmpty() } > 1) return null
        val effects = parts.map { part ->
            val effect = part.spellEffect ?: return null
            if (part != CardScript(spellEffect = effect, targetRequirements = part.targetRequirements)) return null
            effect
        }
        return CardScript(
            spellEffect = if (effects.size == 1) effects.single() else Effects.Composite(effects),
            targetRequirements = parts.flatMap { it.targetRequirements },
        )
    }

    /**
     * A clause run with one more clause after it, flattened — the fold [runEndingInScopedClause]
     * needs and [merge] cannot give it, because the head is already *one* script holding a
     * composite rather than the list of clauses it came from.
     *
     * Flat, never nested: a card carries `Composite[A, B, gated]`, and a `Composite[Composite[A, B],
     * gated]` is a model no card holds and nothing else could print. The guards are [merge]'s own —
     * a clause contributes an effect and the targets it declared and nothing else, and two clauses
     * that each declared a target refuse to fold, for the reason [merge] states.
     */
    private fun appendClause(head: CardScript, last: CardScript): CardScript? {
        val headEffect = head.spellEffect ?: return null
        val lastEffect = last.spellEffect ?: return null
        if (head != CardScript(spellEffect = headEffect, targetRequirements = head.targetRequirements)) return null
        if (last != CardScript(spellEffect = lastEffect, targetRequirements = last.targetRequirements)) return null
        if (head.targetRequirements.isNotEmpty() && last.targetRequirements.isNotEmpty()) return null
        val headEffects = (headEffect as? CompositeEffect)
            ?.takeIf { it == CompositeEffect(it.effects) }
            ?.effects
            ?: listOf(headEffect)
        return CardScript(
            spellEffect = Effects.Composite(headEffects + lastEffect),
            targetRequirements = head.targetRequirements + last.targetRequirements,
        )
    }

    /**
     * The whole clause cascade — atoms, the wrappers, the joins, the sentence — over **one** anaphor
     * vocabulary.
     *
     * A class rather than a run of `val`s because there are two instances of it and they differ in
     * exactly one place: what "it" points at. See [SelfSteps] for why a filtered trigger reads the
     * pronoun as the object its filter matched, and why registering both readings in one cascade
     * would be two models for one text rather than a choice. Everything above this line — every
     * atom, every leaf, every filter — is shared between the two; only the ~dozen combinators here
     * are built twice.
     *
     * @param tag suffixes the rule names so an ambiguity diagnostic can say which cascade it found.
     */
    private class Cascade(anaphora: List<Phrase<CardScript>>, val tag: String) {

        /**
         * The atoms alone, for the rules that wrap or join clauses without being one.
         *
         * Declared before everything built from it — initializers run in declaration order, and a
         * `val` reaching a later one reads a null out of a half-initialized instance.
         */
        private val atom: Phrase<CardScript> = oneOf("a spell effect$tag", nonAnaphoric + anaphora)

        /**
         * The clauses a *gate's consequence* can be made of — atoms and [Continuations], joined.
         *
         * Deliberately narrower than [laterClause]: no gate wrapper is a member, so a consequence
         * can never contain a second gate. That is what makes the scope unambiguous. "You may pay
         * {B}. If you do, target player loses 2 life and you gain 2 life." has exactly one reading
         * — the gate owns the whole sentence — because the outer run cannot split at " and " (a
         * gate is not a member of [simpleClause] or [laterClause]) and this run cannot open a
         * second gate. Registering a gate in both places would give one text two models, which is
         * the hard ambiguity the design says never to resolve by ordering an alternation.
         *
         * This is also the reading Oracle templating intends: "If you do," introduces the
         * *consequence* of the payment, and Extort's own reminder text is the worked example — "you
         * may pay {W/B}. If you do, each opponent loses 1 life and you gain that much life." —
         * where both halves of the join plainly depend on the payment.
         */
        private val laterAtom: Phrase<CardScript> = oneOf(
            "a later spell effect$tag",
            nonAnaphoric + Continuations.all,
        )

        private val gatedConsequence: Phrase<CardScript> = oneOf(
            "a gated consequence$tag",
            atom,
            clauseRun(atom, laterAtom, name = "several gated clauses$tag"),
        )

        /**
         * "You may draw a card." — the controller chooses whether the clause happens.
         *
         * Wrapping rather than a vocabulary of its own, so every clause the grammar can read is
         * optional-able for free. Note that a *triggered* ability spells the same English with its
         * own `optional` flag instead; [Triggers] lowers this wrapper into that field, which is
         * what keeps one printed form for the two SDK spellings.
         */
        private val mayClause: Phrase<CardScript> = phrase("you may {inner}", name = "you may$tag") {
            slot("inner", atom)
            build { bindings -> wrap(bindings.value("inner")) { MayEffect(it) } }
            match { script ->
                val gated = script.spellEffect as? GatedEffect ?: return@match null
                if (gated.gate !is Gate.MayDecide) return@match null
                val inner = CardScript(spellEffect = gated.then, targetRequirements = script.targetRequirements)
                if (wrap(inner) { MayEffect(it) } != script) return@match null
                bind("inner" to inner)
            }
        }

        /**
         * "You may pay {B}{B}{B}. If you do, return it to your hand." — Ghastly Remains, Skirk
         * Drill Sergeant, and Hollow Specter's `{X}` sibling.
         *
         * Two printed sentences and one wrapper, because the second is the *consequence* of the
         * first: `Gate.MayPay` holds both the cost and what follows, so there is nothing for
         * [sequenceClause] to split. A wrapper for the same reason [mayClause] is one — every
         * clause the grammar can read becomes payable-for at no cost.
         *
         * The `{X}` form is a separate rule rather than a mana cost that happens to be `{X}`: the
         * model is a different gate, because the player chooses the number rather than paying a
         * printed one. The printed symbol is the same either way, so the mana rule **refuses `{X}`
         * outright** — without that, Decree of Justice's cycling trigger has two readings with two
         * different models, which is the hard ambiguity the design says never to resolve by
         * ordering an alternation.
         */
        private val payX: ManaCost = ManaCost.parse("{X}")

        private val mayPayClauses: List<Phrase<CardScript>> = listOf(
            phrase("you may pay {cost}. if you do, {inner}", name = "you may pay a cost$tag") {
                slot("cost", Primitives.manaCost)
                slot("inner", gatedConsequence)
                build { bindings ->
                    val cost = bindings.value<ManaCost>("cost")
                    if (cost == payX) return@build null
                    wrap(bindings.value("inner")) { MayPayManaEffect(cost, it) }
                }
                match { script ->
                    val gated = script.spellEffect as? GatedEffect ?: return@match null
                    val gate = gated.gate as? Gate.MayPay ?: return@match null
                    val cost = (gate.cost as? PayManaCostEffect)?.cost ?: return@match null
                    if (cost == payX) return@match null
                    val inner = CardScript(spellEffect = gated.then, targetRequirements = script.targetRequirements)
                    if (wrap(inner) { MayPayManaEffect(cost, it) } != script) return@match null
                    bind("cost" to cost, "inner" to inner)
                }
            },
            phrase("you may pay {X}. if you do, {inner}", name = "you may pay X$tag") {
                slot("inner", gatedConsequence)
                build { bindings -> wrap(bindings.value("inner")) { MayPayXForEffect(it) } }
                match { script ->
                    val gated = script.spellEffect as? GatedEffect ?: return@match null
                    if (gated.gate !is Gate.MayPayX) return@match null
                    val inner = CardScript(spellEffect = gated.then, targetRequirements = script.targetRequirements)
                    if (wrap(inner) { MayPayXForEffect(it) } != script) return@match null
                    bind("inner" to inner)
                }
            },
        )

        /**
         * "If an opponent controls more lands than you, search your library for …" — Gift of
         * Estates.
         *
         * The SDK lowers a spell's `condition` into a `ConditionalEffect` wrapping the whole
         * effect, so this is a wrapper for the same reason [mayClause] is one, and the condition is
         * the slot. The condition vocabulary is [Conditions].
         *
         * **Its consequence is a clause run, and the rule is sentence-terminal** — both halves of
         * the same property the pay-gates have, and for the same reason. "If you control three or
         * more creatures, this creature gets +1/+1 until end of turn and you gain 1 life." states
         * one condition over the whole sentence; reading it as a gate on the first clause with the
         * second joined after would round-trip byte-exactly and mean something else. Inside a
         * trigger it would mean something else *in the rules*, too: [Triggers] lifts a top-level
         * gate into `interveningIf`, which CR 603.4 re-checks on resolution, and a gate buried under
         * a `Composite` is not top-level, so the ability would silently lose its intervening-if.
         * Leonin Vanguard is the card the differential caught doing exactly that.
         *
         * So the rule moves out of [simpleClause] and [laterClause] into [clause], where the full
         * stop is the only thing that can follow it, and its slot is [gatedConsequence] — which
         * admits no second gate, so the scope has exactly one reading.
         */
        private val conditionalClause: Phrase<CardScript> =
            phrase("if {cond}, {inner}", name = "a conditional clause$tag") {
                slot("cond", Conditions.condition)
                slot("inner", gatedConsequence)
                build { bindings ->
                    val condition = bindings.value<Condition>("cond")
                    wrap(bindings.value("inner")) { ConditionalEffect(condition, it) }
                }
                match { script ->
                    val gated = script.spellEffect as? GatedEffect ?: return@match null
                    val gate = gated.gate as? Gate.WhenCondition ?: return@match null
                    val inner = CardScript(spellEffect = gated.then, targetRequirements = script.targetRequirements)
                    if (wrap(inner) { ConditionalEffect(gate.condition, it) } != script) return@match null
                    bind("cond" to gate.condition, "inner" to inner)
                }
            }

        /**
         * One self-contained clause: an atom, or an atom under a wrapper.
         *
         * The **pay-gates are not members**, and that omission is the rule that fixes their scope.
         * A clause that can start a run can also be followed by a tail, and a gate that could be
         * followed by a tail would read "You may pay {B}. If you do, A and B." two ways — the gate
         * over `A` with `B` joined after it, or the gate over both. See [gatedConsequence]; the
         * gates are offered at [clause] instead, where the sentence's full stop is the only thing
         * that can follow them.
         */
        private val simpleClause: Phrase<CardScript> =
            oneOf("a spell effect$tag", nonAnaphoric + anaphora + mayClause)

        /**
         * A clause that can only be a *later* one: it refers back to something an earlier clause
         * introduced, so it declares nothing of its own.
         */
        private val laterClause: Phrase<CardScript> = oneOf(
            "a later spell effect$tag",
            // Everything except the source-anaphora: once a clause has introduced a target, "it" means
            // that target, and [Continuations] owns the pronoun from here on. See [SelfSteps.anaphoric].
            nonAnaphoric + mayClause + Continuations.all,
        )

        /** A whole line's clauses, joined. The shape and its KDoc are [clauseRun]. */
        private val sequenceClause: Phrase<CardScript> =
            clauseRun(simpleClause, laterClause, name = "several clauses$tag")

        /**
         * "Counter target spell. **If you control a blue creature, draw a card, then discard a
         * card.**" — a run of ordinary clauses that *ends* in a scoped one.
         *
         * A scoped clause takes the rest of the sentence as its consequence, which is what makes it
         * unavailable as an ordinary run member: "if X, A, then B" would be the condition over both
         * clauses, or over A with B joined after it, and no ordering of an alternation is allowed to
         * decide that. But a line may still *end* in one, and this rule is that position — the
         * scoped clause is the last slot of the phrase, so nothing can follow it and the second
         * reading has nowhere to come from.
         *
         * The head is [clause]'s ordinary vocabulary, so the twelve cards this shape covers keep the
         * whole effect grammar in front of the condition; only the join is fixed, at the full stop
         * the condition's own sentence opens on.
         */
        private val runEndingInScopedClause: Phrase<CardScript> =
            phrase("{head}. {scoped}", name = "clauses ending in a scoped clause$tag") {
                slot("head", oneOf("a clause run$tag", simpleClause, sequenceClause))
                slot("scoped", conditionalClause)
                build { bindings ->
                    appendClause(bindings.value("head"), bindings.value("scoped"))
                }
                match { script ->
                    val composite = script.spellEffect as? CompositeEffect ?: return@match null
                    val effects = composite.effects
                    if (effects.size < 2) return@match null
                    if (composite != CompositeEffect(effects)) return@match null
                    // Which clause declared the line's targets is decided by printability, exactly
                    // as in [clauseRun]: at most one split can print from both positions.
                    for (owner in effects.indices) {
                        val parts = clauseParts(effects, script.targetRequirements, owner)
                        if (merge(parts) != script) continue
                        val scoped = parts.last()
                        if (conditionalClause.unparse(scoped) == null) continue
                        val head = merge(parts.dropLast(1)) ?: continue
                        if (appendClause(head, scoped) != script) continue
                        if (simpleClause.unparse(head) == null && sequenceClause.unparse(head) == null) continue
                        return@match bind("head" to head, "scoped" to scoped)
                    }
                    null
                }
            }

        /**
         * Everything one clause position can hold.
         *
         * The pay-gates and the conditional sit here rather than in [simpleClause] because they are
         * sentence-terminal: their consequence runs to the end of the sentence, so nothing can be
         * joined after one. See [gatedConsequence] and [runEndingInScopedClause].
         */
        private val clause: Phrase<CardScript> =
            oneOf(
                "a clause position$tag",
                listOf(simpleClause, sequenceClause, conditionalClause, runEndingInScopedClause) +
                    mayPayClauses,
            )

        /** One clause and the stop that ends it — what a whole effect line is. */
        private val sentence: Phrase<CardScript> = phrase("{clause}.", name = "a sentence$tag") {
            slot("clause", clause)
            build { it.value("clause") }
            match { bind("clause" to it) }
        }

        /**
         * What a spell's whole effect text denotes, modes aside: one sentence, or a clause that ends
         * itself.
         *
         * [sentence] spells the full stop, which is right for every clause whose text ends on one.
         * A clause ending *inside a quotation* does not — "…gains "This creature can't attack ….""
         * closes on a quote mark — so those are offered beside it rather than inside it, and the
         * two are disjoint by their last character.
         */
        private val plainStep: Phrase<CardScript> =
            oneOf("a spell effect line$tag", listOf(sentence) + Combat.selfTerminatingClauses)

        /**
         * …and with the modes on top.
         *
         * [Modal] reads the bullets as *sentences* rather than as steps, which is what keeps a mode
         * from being modal — and, mechanically, what keeps this `val` constructible: a family
         * reaching the rule it is a member of is left recursion. Offered here rather than inside
         * [clause] because a modal block is sentence-terminal in the strongest sense — its last
         * bullet carries the line's last full stop, so there is nothing a join could follow it with.
         */
        val step: Phrase<CardScript> =
            oneOf("a spell effect line$tag", listOf(plainStep) + Modal.clauses(sentence, tag))
    }

    private val sourceCascade = Cascade(SelfSteps.anaphoric, tag = "")

    /** The cascade a filtered trigger's effect takes; see [SelfSteps.triggering]. */
    private val triggeredCascade = Cascade(SelfSteps.triggering, tag = " in a filtered trigger")

    val step: Phrase<CardScript> = sourceCascade.step

    /**
     * The same vocabulary for a trigger whose event names a filter, where "it" is the object that
     * matched rather than the source. [Triggers.filteredTriggerRule] is the only caller, which is
     * what keeps the third anaphor unreachable from every other position.
     */
    val triggeredStep: Phrase<CardScript> = triggeredCascade.step

    // ---------------------------------------------------------------------------------------
    // Model helpers — the `match` side, kept out of the rules so like rules read alike
    // ---------------------------------------------------------------------------------------

    private fun targetPlayerDraws(count: Int) = CardScript(
        spellEffect = Effects.DrawCards(count, Targets.bound()),
        targetRequirements = listOf(Targets.player()),
    )

    /** The count on a bare "the caster draws" script, or null when the script is anything else. */
    private fun drawnByController(script: CardScript): Int? =
        drawCount(script, requireTarget = false)?.takeIf { script.targetRequirements.isEmpty() }

    /** …and on a "target player draws" script, which must carry the matching requirement. */
    private fun drawnByTarget(script: CardScript): Int? =
        drawCount(script, requireTarget = true)
            ?.takeIf { script.targetRequirements == listOf(Targets.player()) }

    /**
     * The fixed amounts the counted verbs read back.
     *
     * Each recovers only the *number*; nothing here checks the target or the rest of the script,
     * because [countedStep]'s equality against its own `script(n)` already does, exhaustively. A
     * dynamic amount ("equal to the number of…") has no numeral to print, so it declines here.
     */
    internal fun lifeGained(effect: Effect): Int? = (effect as? GainLifeEffect)?.amount?.fixed()

    internal fun lifeLost(effect: Effect): Int? = (effect as? LoseLifeEffect)?.amount?.fixed()

    internal fun damageDealt(effect: Effect): Int? = (effect as? DealDamageEffect)?.amount?.fixed()

    /**
     * The same reader before the `Fixed` unwrap, for [countedStepPair] — which needs the whole
     * amount because the numeral and the "equal to …" clause are two domains of one value, not a
     * number and something else.
     */
    internal fun damageDealtAmount(effect: Effect): DynamicAmount? = (effect as? DealDamageEffect)?.amount

    /**
     * The kind and the count an `AddCounters` effect carries, aimed at [target].
     *
     * The target is checked *here* rather than by the caller because it is what tells the three
     * counter sentences apart — the source's own ("put a +1/+1 counter on ~"), the spell's chosen
     * one ("…on target creature") and the one an earlier clause chose ("…on it"). All three build
     * the same effect with a different [EffectTarget], so a reader that ignored it would let each
     * rule print the others' sentence.
     */
    internal fun countersAdded(effect: Effect?, target: EffectTarget): Pair<String, Int>? {
        val add = effect as? AddCountersEffect ?: return null
        if (add.target != target) return null
        return add.counterType to add.count
    }

    /** The two fixed bonuses a `ModifyStats` effect carries, or null for a dynamic one. */
    internal fun fixedModifiers(effect: Effect?): Pair<Int, Int>? {
        val stats = effect as? ModifyStatsEffect ?: return null
        val power = stats.powerModifier.fixed() ?: return null
        val toughness = stats.toughnessModifier.fixed() ?: return null
        return power to toughness
    }

    /**
     * The [Keyword] a grant effect names, or null when it names a synthesized marker instead.
     *
     * `GrantKeywordEffect` holds a `String`, which is wider than the enum: the SDK also uses the
     * field for markers like `PROTECTION_FROM_BLACK` that no constant names. Reading it back has to
     * find the constant rather than assume one.
     */
    internal fun grantedKeyword(effect: Effect?): Keyword? {
        val grant = effect as? com.wingedsheep.sdk.scripting.effects.GrantKeywordEffect ?: return null
        return Keyword.entries.firstOrNull { it.name == grant.keyword }
    }

    /**
     * What a whole [Keywords.keywordRun] denotes: one grant per keyword, over one object.
     *
     * A list of one is the bare effect rather than a one-element `CompositeEffect`, because that is
     * what the hand-written cards hold and what the SDK's composite means — an ordering of several
     * effects, which one effect does not have.
     */
    internal fun grants(keywords: List<Keyword>, target: EffectTarget): Effect {
        val effects = keywords.map { Effects.GrantKeyword(it, target) }
        return effects.singleOrNull() ?: Effects.Composite(effects)
    }

    /** [grants]'s inverse: the keywords a grant — or a composite of nothing but grants — names. */
    internal fun grantedKeywords(effect: Effect?): List<Keyword>? =
        grantedKeywords((effect as? CompositeEffect)?.effects ?: listOfNotNull(effect))

    /** The same over a clause's tail, for the rules whose first effect is a pump. */
    internal fun grantedKeywords(effects: List<Effect>): List<Keyword>? {
        if (effects.isEmpty()) return null
        return effects.map { grantedKeyword(it) ?: return null }
    }

    /** The group a mass effect iterates, or null when the effect is not a plain battlefield sweep. */
    private fun iteratedGroup(effect: Effect?): GameObjectFilter? {
        val forEach = effect as? com.wingedsheep.sdk.scripting.effects.ForEachEffect ?: return null
        val space = forEach.space as? com.wingedsheep.sdk.scripting.effects.IterationSpace.Group ?: return null
        return space.filter.baseFilter
    }

    /**
     * The filter a [Effects.DestroyAll] pipeline sweeps, or null when [effect] is not one.
     *
     * The pipeline is a two-step composite rather than a single node, so the destructuring reads
     * both halves and the caller's reconstruct-and-compare does the rest.
     */
    private fun destroyedGroup(effect: Effect?): GameObjectFilter? {
        val steps = (effect as? CompositeEffect)?.effects ?: return null
        val gather = steps.firstOrNull() as? GatherCardsEffect ?: return null
        val source = gather.source as? CardSource.BattlefieldMatching ?: return null
        return source.filter
    }

    private fun iteratedBody(effect: Effect?): Effect? =
        (effect as? com.wingedsheep.sdk.scripting.effects.ForEachEffect)?.body

    /**
     * The body of an iteration over **players**, or null when [effect] is not one.
     *
     * The [iteratedGroup] of the player space: `ForEachPlayerEffect` is a factory that lowers to a
     * `ForEachEffect` over `IterationSpace.Players`, so there is no class to test with `as?` and the
     * space has to be destructured the same way a group's is.
     */
    private fun iteratedPlayers(effect: Effect?, players: Player = Player.Each): Effect? {
        val forEach = effect as? com.wingedsheep.sdk.scripting.effects.ForEachEffect ?: return null
        val space = forEach.space as? com.wingedsheep.sdk.scripting.effects.IterationSpace.Players ?: return null
        return if (space.players == players) forEach.body else null
    }

    internal fun DynamicAmount.fixed(): Int? = (this as? DynamicAmount.Fixed)?.amount

    private fun drawCount(script: CardScript, requireTarget: Boolean): Int? {
        val effect = script.spellEffect as? DrawCardsEffect ?: return null
        if (script.copy(spellEffect = null, targetRequirements = emptyList()) != CardScript.EMPTY) return null
        val drawer = if (requireTarget) Targets.isBound(effect.target) else effect.target == EffectTarget.Controller
        if (!drawer) return null
        return (effect.count as? DynamicAmount.Fixed)?.amount
    }
}
