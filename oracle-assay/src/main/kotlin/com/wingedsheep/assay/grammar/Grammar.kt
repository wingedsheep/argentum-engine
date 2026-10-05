package com.wingedsheep.assay.grammar

import com.wingedsheep.assay.normalize.Normalizer
import com.wingedsheep.assay.syntax.Bindings
import com.wingedsheep.assay.syntax.Phrase
import com.wingedsheep.assay.syntax.alternate
import com.wingedsheep.assay.syntax.bind
import com.wingedsheep.assay.syntax.oneOf
import com.wingedsheep.assay.syntax.phrase
import com.wingedsheep.assay.syntax.separated
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.CharacteristicValue
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.EntersWithRevealCounters
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.soulshift
import com.wingedsheep.sdk.dsl.Triggers as SdkTriggers
import com.wingedsheep.sdk.serialization.CardSerialization
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * The entry point: one normalized ability line ⇄ the [CardFragment] it denotes.
 *
 * The unit is the **line**, not the card. Line grouping is a property of the printed text, owned by
 * [com.wingedsheep.assay.normalize.Normalizer]; the card-level model is the fold of its lines'
 * fragments. Keeping the grouping out of the model is what lets "Flying, vigilance" and
 * "Flying\nVigilance" produce the same card while each still round-trips to its own printed shape.
 *
 * A fragment rather than a bare `List<KeywordAbility>` because a line can now fill either of the two
 * behavioural slots a card has — its keywords or its script — and the grammar has started reaching
 * the second one.
 */
object Grammar {

    /** One keyword ability: everything in [Keywords.all], tried in parallel. */
    val keywordAbility: Phrase<KeywordAbility> = oneOf("a keyword ability", Keywords.all)

    /** One keyword ability, lifted into the one-element group most phrases denote. */
    private val singleKeyword: Phrase<List<KeywordAbility>> = phrase("{one}", name = "a keyword ability") {
        slot("one", keywordAbility)
        build { listOf(it.value<KeywordAbility>("one")) }
        match { it.singleOrNull()?.let { ability -> bind("one" to ability) } }
    }

    /**
     * What one comma-separated element of a keyword line denotes: usually one ability, and for the
     * multi-quality forms of protection and hexproof (CR 702.16g / 702.11f) several.
     *
     * Grouping is only a *printing* concern in one direction and a parsing convenience in the other,
     * but it has to exist in the grammar rather than in the gate, because the same flat model has to
     * come back as the joined text the card prints.
     */
    private val keywordGroup: Phrase<List<KeywordAbility>> =
        oneOf("a keyword ability", Keywords.runs + singleKeyword)

    /**
     * "Flying, first strike, protection from black and from red" — the comma-joined keyword line.
     *
     * The model is flat, so printing has to decide where the joins go. Consecutive abilities that a
     * run rule can express are joined maximally, which is what every printed card does; a card that
     * spells two protections as separate comma-separated abilities therefore prints back joined and
     * reports as a [com.wingedsheep.assay.gate.LineVerdict.VARIANT] — reparsed to the identical
     * model, only the spelling normalized. No card in the corpus is in that class today.
     */
    private val keywordList: Phrase<List<KeywordAbility>> = phrase("{groups}", name = "keyword abilities") {
        slot("groups", separated("keyword abilities", keywordGroup, ", "))
        build { it.value<List<List<KeywordAbility>>>("groups").flatten() }
        match { bind("groups" to groupForPrinting(it)) }
    }

    /**
     * Split a line's abilities into the groups it prints as: maximal runs a [Keywords.runs] rule can
     * express, everything else on its own.
     *
     * Maximal-and-greedy is a choice, and it is the one that matches printed text: cards print
     * "protection from black and from red", never the two spelled out separately. It is also the
     * only choice that is deterministic — anything subtler would need to know which grouping the
     * card *printed*, which is exactly the information the model does not carry.
     */
    private fun groupForPrinting(abilities: List<KeywordAbility>): List<List<KeywordAbility>> {
        val groups = mutableListOf<List<KeywordAbility>>()
        var index = 0
        while (index < abilities.size) {
            val run = Keywords.runs.firstNotNullOfOrNull { rule -> longestRun(rule, abilities, index) }
            if (run == null) {
                groups.add(listOf(abilities[index]))
                index++
            } else {
                groups.add(run)
                index += run.size
            }
        }
        return groups
    }

    /** The longest prefix from [index] that [rule] can print, or null if it cannot print any. */
    private fun longestRun(
        rule: Phrase<List<KeywordAbility>>,
        abilities: List<KeywordAbility>,
        index: Int,
    ): List<KeywordAbility>? =
        (abilities.size downTo index + 2)
            .asSequence()
            .map { end -> abilities.subList(index, end).toList() }
            .firstOrNull { rule.unparse(it) != null }

    /**
     * "Flying; banding" — the semicolon-joined form, which ~31 mostly-older cards still print.
     *
     * It is an [alternate]: the separator is a property of the printed line that the model does not
     * carry, since a flat `List<KeywordAbility>` has no room for it. Something has to be picked, so
     * the comma is picked, and those cards report as [com.wingedsheep.assay.gate.LineVerdict.VARIANT]
     * — parsed correctly, printed canonically. That is the honest verdict and it is worth more than
     * the decline it replaces: it says the reading is right and only the spelling was normalized.
     */
    private val semicolonKeywordList: Phrase<List<List<KeywordAbility>>> =
        alternate(separated("keyword abilities", keywordGroup, "; ", min = 2))

    private val keywordLine: Phrase<CardFragment> = phrase("{keywords}", name = "a keyword line") {
        slot("keywords", keywordList)
        build { CardFragment.of(it.value<List<KeywordAbility>>("keywords")) }
        match { fragment ->
            if (fragment.keywordAbilities.isNotEmpty() && fragment.script == CardScript.EMPTY) {
                bind("keywords" to fragment.keywordAbilities)
            } else {
                null
            }
        }
    }

    private val semicolonKeywordLine: Phrase<CardFragment> =
        alternate(
            phrase("{keywords}", name = "a keyword line") {
                slot("keywords", semicolonKeywordList)
                build { CardFragment.of(it.value<List<List<KeywordAbility>>>("keywords").flatten()) }
                canonical = false
            }
        )

    /**
     * A line that is one spell effect — "Draw two cards.", "Target player draws a card."
     *
     * Wrapped into a fragment here rather than in [Steps] so the step rules stay about effects and
     * know nothing about where in a card they land.
     */
    private val spellLine: Phrase<CardFragment> = phrase("{step}", name = "a spell effect line") {
        slot("step", Steps.step)
        build { CardFragment.of(it.value<CardScript>("step")) }
        match { fragment ->
            if (fragment.keywordAbilities.isEmpty() && fragment.script != CardScript.EMPTY) {
                bind("step" to fragment.script)
            } else {
                null
            }
        }
    }

    /**
     * A line that is one triggered ability — "When ~ enters, draw a card."
     *
     * Wrapped here rather than in [Triggers] for the same reason [spellLine] is: the rules stay
     * about abilities and know nothing about which of a card's slots they land in.
     */
    private val triggerLine: Phrase<CardFragment> = phrase("{trigger}", name = "a triggered ability line") {
        slot("trigger", Triggers.line)
        build { CardFragment.of(withLinkedReturn(CardScript(triggeredAbilities = it.value("trigger")))) }
        match { fragment ->
            if (fragment.keywordAbilities.isNotEmpty()) return@match null
            val script = withoutLinkedReturn(fragment.script) ?: return@match null
            val abilities = script.triggeredAbilities.takeIf { it.isNotEmpty() } ?: return@match null
            if (script != CardScript(triggeredAbilities = abilities)) return@match null
            bind("trigger" to abilities)
        }
    }

    /**
     * A line that is one activated ability — "{T}: Add {G}." — or the several a single printed
     * line can denote, since "{T}: Add {B} or {G}." is two abilities sharing a cost.
     *
     * Wrapped here for the same reason [spellLine] and [triggerLine] are: [Activated]'s rules stay
     * about abilities and know nothing about which of a card's slots they land in.
     */
    private val activatedLine: Phrase<CardFragment> = phrase("{abilities}", name = "an activated ability line") {
        slot("abilities", Activated.abilities)
        build { CardFragment.of(withLinkedReturn(CardScript(activatedAbilities = it.value("abilities")))) }
        match { fragment ->
            if (fragment.keywordAbilities.isNotEmpty()) return@match null
            val script = withoutLinkedReturn(fragment.script) ?: return@match null
            val abilities = script.activatedAbilities
            if (abilities.isEmpty()) return@match null
            if (script != CardScript(activatedAbilities = abilities)) return@match null
            bind("abilities" to abilities)
        }
    }

    /**
     * "Exile target creature until ~ leaves the battlefield." — one printed sentence, **two**
     * abilities: the exile, and the leaves-the-battlefield trigger that returns the card. The SDK
     * spells the "until" as a linked exile plus that trigger rather than as a duration, and every
     * hand-written card carrying
     * `ExileUntilLeavesEffect` — 42 of them — pairs it with the same
     * `Triggers.self.leaves()` / `Effects.ReturnLinkedExileUnderOwnersControl()` ability, so the
     * second ability is a lowering of the first rather than a sentence of its own.
     *
     * The same shape as [amplifyLine] and [equipLine] — a line whose meaning fills more of the card
     * than the ability it prints — and it lives here for the same reason: a step rule in [Steps] can
     * only return an effect, and the fragment is the only place the two abilities meet. The pairing
     * is keyed on the effect's own serial discriminator because the exile can sit anywhere inside
     * the ability's effect tree (a composite with "You gain 2 life", a gate, a modal branch).
     */
    private val linkedReturn = TriggeredAbility(
        id = AbilityId("linked-return"),
        trigger = SdkTriggers.self.leaves().event,
        binding = SdkTriggers.self.leaves().binding,
        effect = Effects.ReturnLinkedExileUnderOwnersControl(),
    )

    /** [script] with [linkedReturn] appended when one of its abilities exiles until the source leaves. */
    private fun withLinkedReturn(script: CardScript): CardScript =
        if (exilesUntilLeaves(script)) script.copy(triggeredAbilities = script.triggeredAbilities + linkedReturn) else script

    /**
     * The inverse of [withLinkedReturn]: [script] without its trailing [linkedReturn], or **null**
     * when the script exiles until the source leaves but does not carry the return — a card whose
     * exile never comes back, which the sentence does not say and so must not print.
     */
    private fun withoutLinkedReturn(script: CardScript): CardScript? {
        if (!exilesUntilLeaves(script)) return script
        val last = script.triggeredAbilities.lastOrNull() ?: return null
        if (last.copy(id = linkedReturn.id) != linkedReturn) return null
        return script.copy(triggeredAbilities = script.triggeredAbilities.dropLast(1))
    }

    private fun exilesUntilLeaves(script: CardScript): Boolean =
        EXILE_UNTIL_LEAVES in CardSerialization.json.encodeToString(CardScript.serializer(), script)

    /**
     * `ExileUntilLeavesEffect`'s `@SerialName`, quoted as it appears in the serialized script. No
     * other string value in the SDK is spelled this way, so the quoted name is the discriminator.
     */
    private const val EXILE_UNTIL_LEAVES = "\"ExileUntilLeaves\""

    /**
     * A line that is one Aura's attachment restriction — "Enchant creature".
     *
     * The only line in the grammar whose whole content is a `TargetRequirement`. Wrapped here for
     * the same reason the others are: [Targets] stays about targeting and knows nothing about the
     * fact that this particular requirement is a line rather than a clause.
     */
    private val enchantLine: Phrase<CardFragment> = phrase("{enchant}", name = "an enchant line") {
        slot("enchant", Targets.enchant)
        build { CardFragment.of(CardScript(auraTarget = it.value("enchant"))) }
        match { fragment ->
            val target = fragment.script.auraTarget ?: return@match null
            if (fragment.keywordAbilities.isNotEmpty()) return@match null
            if (fragment.script != CardScript(auraTarget = target)) return@match null
            bind("enchant" to target)
        }
    }

    /**
     * A line that is one static ability — "Enchanted creature gets +1/+2." — or the two a single
     * printed line can denote, since "Enchanted creature gets +2/+2 and has flying." is two.
     */
    private val staticLine: Phrase<CardFragment> = phrase("{statics}", name = "a static ability line") {
        slot("statics", Statics.line)
        build { CardFragment.of(CardScript(staticAbilities = it.value("statics"))) }
        match { fragment ->
            val abilities = fragment.script.staticAbilities
            if (abilities.isEmpty() || fragment.keywordAbilities.isNotEmpty()) return@match null
            if (fragment.script != CardScript(staticAbilities = abilities)) return@match null
            bind("statics" to abilities)
        }
    }

    /**
     * "Amplify 1" — the one line whose two halves land in **two different card slots**.
     *
     * The SDK spells amplify as a bare `Keyword.AMPLIFY` on the card *plus* an
     * `EntersWithRevealCounters` replacement carrying the count, which is where all nine
     * hand-written amplify cards put it. So the printed number is not a parameter of the keyword at
     * all — there is no `Numeric(AMPLIFY, n)` in the corpus — and a rule that minted one would
     * round-trip perfectly while disagreeing with every card that has the mechanic.
     *
     * That is why this is a line rule in [Grammar] rather than a row in [Keywords] or
     * [Replacements]: neither of those files can produce a fragment that fills both slots, and the
     * fragment is the only place a line's two contributions can meet. It is the same reason
     * [flagLine] lives here.
     */
    private val amplifyLine: Phrase<CardFragment> = run {
        fun fragmentFor(count: Int) = CardFragment(
            keywordAbilities = listOf(KeywordAbility.of(Keyword.AMPLIFY)),
            script = CardScript(replacementEffects = listOf(EntersWithRevealCounters(countersPerReveal = count))),
        )
        phrase("amplify {n}", name = "amplify") {
            slot("n", Primitives.cardinal)
            build { fragmentFor(it.int("n")) }
            match { fragment ->
                val replacement = fragment.script.replacementEffects.singleOrNull()
                    as? EntersWithRevealCounters ?: return@match null
                if (fragment != fragmentFor(replacement.countersPerReveal)) return@match null
                bind("n" to replacement.countersPerReveal)
            }
        }
    }

    /**
     * "Equip {1}" — the second line whose two halves land in **two different card slots**, and the
     * largest single sentence shape in the corpus: 563 cards print it.
     *
     * Like [amplifyLine], and for the same reason it is here rather than in [Keywords]: the SDK
     * lowers equip at authoring time into `CardDefinition.equipCost` *plus* a synthesized activated
     * ability, and no single family can produce a fragment filling both. Unlike amplify, the second
     * half is a whole ability with a cost, a timing, an effect and a target requirement — CR 702.6a's
     * "attach this Equipment to target creature you control; activate only as a sorcery" — which is a
     * *lowering to reproduce* rather than a sentence to read.
     *
     * So the rule does not reproduce it. `ActivatedAbility.equip` is the lowering, factored out of
     * `CardBuilder.equipAbility` in the same change and called by both, because a second copy here
     * would be a rule that agrees with the cards until the day someone edits one of them — and the
     * differential would then report every Equipment in the corpus over a change nobody made to a
     * card. The build half is the SDK facade, exactly as the module's rule asks; what makes it
     * unusual is that the facade had to be *created*, since equip's curated surface was a DSL method
     * a parser cannot call.
     *
     * The printed line carries no full stop — it is a keyword ability, not a sentence — and no
     * quality: "Equip Human {1}" (CR 702.6c) pairs a printed word with a target filter that nothing
     * checks against it, so reading one would be inventing the rules half from the wording. Those
     * decline.
     */
    private val equipLine: Phrase<CardFragment> = run {
        // The id this rule mints, for the reason `Activated`'s and `Triggers`' constants exist: no
        // printed word determines it, and the differential renames both sides by position.
        val equipId = AbilityId("equip")
        fun fragmentFor(cost: ManaCost) = CardFragment(
            equipCost = cost,
            script = CardScript(activatedAbilities = listOf(ActivatedAbility.equip(cost, id = equipId))),
        )
        phrase("equip {cost}", name = "equip") {
            slot("cost", Primitives.manaCost)
            build { fragmentFor(it.value("cost")) }
            match { fragment ->
                val cost = fragment.equipCost ?: return@match null
                if (fragment != fragmentFor(cost)) return@match null
                bind("cost" to cost)
            }
        }
    }

    /**
     * "Soulshift 4" — the third line whose two halves land in **two different card slots**: the
     * printed `Numeric(SOULSHIFT, n)` keyword *and* the triggered ability CR 702.46a says it means.
     *
     * The same shape as [equipLine], and built the same way for the same reason: the SDK lowers
     * soulshift at authoring time, so the second half is a lowering to reproduce rather than a
     * sentence to read, and a second copy of it here would agree with the cards only until someone
     * edits one of them. Unlike equip there is no factory to call — the lowering lives in the
     * `CardBuilder.soulshift` DSL method every soulshift card uses — so the rule calls *that*,
     * inside a throwaway `card { }`, and keeps what it produced. The id is re-minted to a constant
     * because no printed word determines it; the differential renames both sides by position.
     */
    private val soulshiftLine: Phrase<CardFragment> = run {
        val soulshiftId = AbilityId("soulshift")
        fun fragmentFor(n: Int): CardFragment {
            val lowered = card("Soulshift") { soulshift(n) }
            return CardFragment(
                keywordAbilities = lowered.keywordAbilities,
                script = CardScript(
                    triggeredAbilities = lowered.script.triggeredAbilities.map { it.copy(id = soulshiftId) },
                ),
            )
        }
        phrase("soulshift {n}", name = "soulshift") {
            slot("n", Primitives.cardinal)
            build { fragmentFor(it.int("n")) }
            match { fragment ->
                val keyword = fragment.keywordAbilities.singleOrNull() as? KeywordAbility.Numeric
                    ?: return@match null
                if (keyword.keyword != Keyword.SOULSHIFT || fragment != fragmentFor(keyword.n)) return@match null
                bind("n" to keyword.n)
            }
        }
    }

    /**
     * A line that is replacement effects and nothing else — "~ enters tapped.", and the kicker
     * sentence whose two halves are two effects ([Replacements.replacements]).
     */
    private val replacementLine: Phrase<CardFragment> = phrase("{replacements}", name = "a replacement effect line") {
        slot("replacements", Replacements.replacements)
        build { CardFragment.of(CardScript(replacementEffects = it.value("replacements"))) }
        match { fragment ->
            val replacements = fragment.script.replacementEffects.takeIf { it.isNotEmpty() } ?: return@match null
            if (fragment.keywordAbilities.isNotEmpty()) return@match null
            if (fragment.script != CardScript(replacementEffects = replacements)) return@match null
            bind("replacements" to replacements)
        }
    }

    /**
     * A line that is one state-triggered ability — "When you control no Islands, sacrifice ~." —
     * landing in `CardScript.stateTriggeredAbilities`, the SDK's list for CR 603.8 beside the event
     * triggers [triggerLine] fills. Wrapped here for the reason the other ability lines are:
     * [StateTriggers] knows nothing about which of a card's slots its ability lands in.
     */
    private val stateTriggerLine: Phrase<CardFragment> = phrase("{trigger}", name = "a state-triggered ability line") {
        slot("trigger", StateTriggers.line)
        build { CardFragment.of(CardScript(stateTriggeredAbilities = listOf(it.value("trigger")))) }
        match { fragment ->
            val ability = fragment.script.stateTriggeredAbilities.singleOrNull() ?: return@match null
            if (fragment != CardFragment.of(CardScript(stateTriggeredAbilities = listOf(ability)))) return@match null
            bind("trigger" to ability)
        }
    }

    /**
     * The empty line. It exists as a rule rather than as a special case in the gate because a
     * reminder-only line normalizes to "" and must still print back to "" — and because a vanilla
     * card, the easy quarter of the corpus, is exactly a face with no lines at all.
     */
    private val emptyLine: Phrase<CardFragment> = phrase("", name = "an empty line") {
        build { CardFragment.EMPTY }
        match { if (it.isEmpty) Bindings.EMPTY else null }
    }

    /**
     * "Cast this spell only during the declare attackers step and only if you've been attacked this
     * step." — a line that constrains rather than does.
     *
     * The first line kind whose fragment carries no effect at all, which is why it is a line rather
     * than a clause: nothing in [Steps] could hold it, and a `CardScript` keeps it in a slot of its
     * own.
     */
    private val castRestrictionLine: Phrase<CardFragment> =
        phrase("{restrictions}", name = "a casting restriction line") {
            slot("restrictions", Restrictions.castLine)
            build { CardFragment.of(CardScript(castRestrictions = it.value("restrictions"))) }
            match { fragment ->
                val restrictions = fragment.script.castRestrictions.takeIf { it.isNotEmpty() }
                    ?: return@match null
                if (fragment != CardFragment.of(CardScript(castRestrictions = restrictions))) return@match null
                bind("restrictions" to restrictions)
            }
        }

    /** "As an additional cost to cast this spell, sacrifice a creature." — the same shape, one slot over. */
    private val additionalCostLine: Phrase<CardFragment> =
        phrase("{costs}", name = "an additional cost line") {
            slot("costs", Restrictions.additionalCostLine)
            build { CardFragment.of(CardScript(additionalCosts = it.value("costs"))) }
            match { fragment ->
                val costs = fragment.script.additionalCosts.takeIf { it.isNotEmpty() } ?: return@match null
                if (fragment != CardFragment.of(CardScript(additionalCosts = costs))) return@match null
                bind("costs" to costs)
            }
        }

    /**
     * "This spell has flash as long as you control a Spirit." — Supernatural Rescue, Colossal
     * Rattlewurm, Take for a Ride.
     *
     * A *timing permission read from hand*, so it is neither a keyword nor a static ability: the SDK
     * gives it `CardScript.conditionalFlash`, a slot of its own, because nothing on the battlefield
     * ever evaluates it. That is the same reason [castRestrictionLine] is a line — the fragment
     * carries no effect at all — with the sign flipped: a cast restriction narrows when the card may
     * be cast, this one widens it.
     *
     * There is no unconditional sibling here. "Flash" with no clause is the printed keyword, which
     * [keywordLine] already reads, so a card is never underdetermined between the two spellings.
     *
     * "This spell" stays literal text for [cantBeCounteredLine]'s reason: [Restrictions] spells the
     * same words inside "Cast this spell only …", so the normalizer leaves the phrase alone.
     */
    private val conditionalFlashLine: Phrase<CardFragment> =
        phrase("this spell has flash as long as {condition}.", name = "a conditional flash line") {
            slot("condition", Conditions.condition)
            build { CardFragment.of(CardScript(conditionalFlash = it.value("condition"))) }
            match { fragment ->
                val condition = fragment.script.conditionalFlash ?: return@match null
                if (fragment != CardFragment.of(CardScript(conditionalFlash = condition))) return@match null
                bind("condition" to condition)
            }
        }

    /**
     * "~ can't be blocked." — the one line whose whole content is a `CardDefinition` flag.
     *
     * A flag rather than a static because that is how the SDK spells the *unconditional* form; every
     * filtered one ("can't be blocked by black creatures") is a `StaticAbility` in [Statics]. Two
     * places to say one kind of thing is a finding the differential can now see, which is the reason
     * [CardFragment] grew a slot for it rather than the grammar picking whichever it preferred.
     */
    private fun flagLine(template: String, flag: AbilityFlag): Phrase<CardFragment> {
        val fragment = CardFragment(flags = setOf(flag))
        return phrase(template, name = "a flag line") {
            build { fragment }
            match { if (it == fragment) Bindings.EMPTY else null }
        }
    }

    /**
     * "This spell can't be countered." — Root Sliver, Vexing Beetle.
     *
     * The one line whose whole content is a `CardScript` **boolean**, which is why it is a line here
     * rather than a [Statics] rule: `cantBeCountered` is a property of the card, and the static
     * `GrantCantBeCountered` next to it on Root Sliver is a different thing — that one is about
     * *other* spells. A card printing both prints two lines, and the fold keeps both.
     *
     * "This spell" is deliberately not abstracted by
     * [com.wingedsheep.assay.normalize.Normalizer]: [Restrictions] spells the same words as a
     * literal inside "Cast this spell only …", so the phrase stays text and this rule reads it.
     */
    private val cantBeCounteredLine: Phrase<CardFragment> = run {
        val fragment = CardFragment.of(CardScript(cantBeCountered = true))
        phrase("this spell can't be countered.", name = "an uncounterable spell line") {
            build { fragment }
            match { if (it == fragment) Bindings.EMPTY else null }
        }
    }

    /**
     * "~ is every creature type." — Mistform Ultimus.
     *
     * Changeling by another name, and the cards say so: Mistform Ultimus carries
     * `Keyword.CHANGELING`, the same value the word "changeling" denotes. One model, two printed
     * forms, so this is an [alternate] and the card comes back as a variant — the reading is right
     * and only the spelling moved. It is a line rather than a [Keywords] row because it is a
     * *sentence*: it ends in a full stop, which a keyword line never does.
     */
    private val everyCreatureTypeLine: Phrase<CardFragment> = alternate(
        phrase("${Normalizer.SELF} is every creature type.", name = "an every-creature-type line") {
            build { CardFragment.of(listOf(KeywordAbility.of(Keyword.CHANGELING))) }
            canonical = false
        }
    )

    /**
     * "~'s power and toughness are each equal to the number of lands you control." — the
     * **characteristic-defining ability** (CR 604.3), and the third line whose content lands outside
     * the script.
     *
     * It is here rather than in [Statics] or [Amounts] for [amplifyLine]'s reason, one slot further
     * out: the SDK puts a CDA in `CardDefinition.creatureStats`, not in an ability list, so no family
     * producing a `CardScript` can express it and the fragment is the only place the line's meaning
     * can land. See [CardFragment.dynamicPower] for why the header spelling is canonical over the
     * Layer 7b static that says the same thing.
     *
     * The four printed shapes are three rows and one pairing rule, because English writes the
     * conjunction two ways round: "power and toughness are each equal to X" defines both from one
     * clause, "power is equal to X" and "toughness is equal to X" each define one — and Yavimaya
     * Kavu prints those two as separate *lines*, which is why [CardFragment.merge] folds them per
     * characteristic. The fourth, "…and its toughness is equal to that number plus 1" (Lhurgoyf,
     * Tarmogoyf), is one sentence with an anaphor in it: "that number" is the same amount the first
     * half named, so the model stores it once and the offset is what the second half adds. That
     * makes it a rule spanning both halves rather than a [sequence] of two, exactly as
     * [Amounts]' where-clause sentences are.
     */
    private val characteristicDefiningLines: List<Phrase<CardFragment>> = run {
        fun fragmentFor(power: CharacteristicValue?, toughness: CharacteristicValue?) =
            CardFragment(dynamicPower = power, dynamicToughness = toughness)

        fun rule(
            template: String,
            name: String,
            power: (DynamicAmount) -> CharacteristicValue?,
            toughness: (DynamicAmount) -> CharacteristicValue?,
        ): Phrase<CardFragment> = phrase(template, name = name) {
            slot("self", Primitives.selfNamed)
            slot("amount", Amounts.count)
            build { fragmentFor(power(it.value("amount")), toughness(it.value("amount"))) }
            match { fragment ->
                // Whichever half this rule defines is where the amount is read back from; the
                // reconstruction below is what refuses a fragment carrying the other half as well.
                val source = (fragment.dynamicPower ?: fragment.dynamicToughness).sourceOrNull()
                    ?: return@match null
                if (fragment != fragmentFor(power(source), toughness(source))) return@match null
                bind("self" to Unit, "amount" to source)
            }
        }

        listOf(
            rule(
                "{self}'s power and toughness are each equal to {amount}.",
                "a power and toughness defined by a count",
                power = { CharacteristicValue.Dynamic(it) },
                toughness = { CharacteristicValue.Dynamic(it) },
            ),
            rule(
                "{self}'s power is equal to {amount}.",
                "a power defined by a count",
                power = { CharacteristicValue.Dynamic(it) },
                toughness = { null },
            ),
            rule(
                "{self}'s toughness is equal to {amount}.",
                "a toughness defined by a count",
                power = { null },
                toughness = { CharacteristicValue.Dynamic(it) },
            ),
            rule(
                "{self}'s power is equal to {amount} and its toughness is equal to that number plus 1.",
                "a power defined by a count and a toughness one greater",
                power = { CharacteristicValue.Dynamic(it) },
                toughness = { CharacteristicValue.DynamicWithOffset(it, 1) },
            ),
        )
    }

    /** The [DynamicAmount] behind a dynamic characteristic, or null when the value is a fixed one. */
    private fun CharacteristicValue?.sourceOrNull(): DynamicAmount? = when (this) {
        is CharacteristicValue.Dynamic -> source
        is CharacteristicValue.DynamicWithOffset -> source
        else -> null
    }

    val abilityLine: Phrase<CardFragment> = oneOf(
        "an ability line",
        emptyLine,
        castRestrictionLine,
        additionalCostLine,
        conditionalFlashLine,
        flagLine("${Normalizer.SELF} can't be blocked.", AbilityFlag.CANT_BE_BLOCKED),
        flagLine("${Normalizer.SELF} doesn't untap during your untap step.", AbilityFlag.DOESNT_UNTAP),
        cantBeCounteredLine,
        everyCreatureTypeLine,
        keywordLine,
        semicolonKeywordLine,
        amplifyLine,
        equipLine,
        soulshiftLine,
        spellLine,
        triggerLine,
        stateTriggerLine,
        activatedLine,
        replacementLine,
        enchantLine,
        staticLine,
        *characteristicDefiningLines.toTypedArray(),
    )
}
