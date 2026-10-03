package com.wingedsheep.assay.grammar

import com.wingedsheep.assay.syntax.Phrase
import com.wingedsheep.assay.syntax.bind
import com.wingedsheep.assay.syntax.oneOf
import com.wingedsheep.assay.syntax.phrase
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.effects.AddColorlessManaEffect
import com.wingedsheep.sdk.scripting.effects.AddDynamicManaEffect
import com.wingedsheep.sdk.scripting.effects.AddManaOfChoiceEffect
import com.wingedsheep.sdk.scripting.effects.AddManaEffect
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CascadeEffect
import com.wingedsheep.sdk.scripting.effects.CastFromCollectionWithoutPayingCostEffect
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.DiscoverEffect
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.ExileFromTopRepeatingEffect
import com.wingedsheep.sdk.scripting.effects.ExileLibraryUntilManaValueEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.PutOnLibraryPositionOfChoiceEffect
import com.wingedsheep.sdk.scripting.effects.SurveilEffect

/**
 * "{T}: Add {G}." — the cost-colon-effect sentence, and the third `CardScript` slot the grammar
 * reaches.
 *
 * ### The clause after the colon is [Steps], not a second effect vocabulary
 *
 * The rule slots [Steps.step] whole and lifts its `CardScript` onto the ability, exactly as
 * [Triggers] does: the effect becomes the ability's effect and a target it declared becomes the
 * ability's `targetRequirements`. So `{T}: Draw a card.` and `{2}: Target creature gets +1/+1
 * until end of turn.` come for free with the mana lines, and every future step rule enriches this
 * file without touching it. The capital on "Add" is not this rule's business either — an ability's
 * effect clause is a sentence start, which [com.wingedsheep.assay.syntax.SentenceCase] owns for
 * the same reason it owns the capital on the line's first word.
 *
 * ### Mana-ability-ness is derived from the effect, because CR 605.1a derives it
 *
 * A `CardDefinition` carries the fact twice — `isManaAbility` and `timing = ManaAbility` — and no
 * printed text says either. CR 605.1a defines a mana ability as one that "doesn't require a
 * target, … could add mana to a player's mana pool when it resolves, and … isn't a loyalty
 * ability", which is a property of the ability rather than a word in the sentence. The rule
 * therefore computes both flags from the effect and the target list rather than spelling them, and
 * an ability that disagrees with the derivation refuses to print.
 *
 * That the SDK needs two fields for one fact was itself a finding, and it has since been acted on:
 * 620 hand-written mana abilities set both and 24 set `isManaAbility` while leaving `timing` at its
 * `InstantSpeed` default, which mattered because the AI's `ExpiringGrantWindow` tests
 * `timing == InstantSpeed` exactly. The differential reported every card in the second group, and
 * `CardBuilder`'s `manaAbility` flag now derives the timing, so the two spellings can no longer
 * drift apart. Deriving both here is what made that reportable in the first place.
 *
 * ### One line can be several abilities
 *
 * "{T}: Add {B} or {G}." is two abilities sharing a cost — see [Mana] for why, and
 * [Keywords.qualityRun] for the shape. The rules therefore hand back a *list*, and the single case
 * is the one-element member of it.
 */
object Activated {

    /**
     * The id every parsed ability carries.
     *
     * One constant rather than a generator, for the reason [Triggers]' is one: the printed text
     * does not determine it, so any value is as right as any other, and the differential renames
     * both sides by position before comparing.
     */
    private val ID = AbilityId("activated")

    /**
     * Build the ability a cost and an effect clause denote, or null when the clause carries
     * something an activated ability has nowhere to put.
     *
     * Shared by both directions so `match` can reconstruct and compare the whole value: an ability
     * carrying a restriction, a `descriptionOverride`, convoke, exhaust or any of the two dozen other
     * fields on `ActivatedAbility` fails the equality and refuses to print, rather than printing a
     * sentence that quietly drops it. Only the id is exempt, because the id is not in the text.
     *
     * **The activation zone used to be on that list and is now derived.** Nothing in a printed line
     * says "this ability works from the graveyard" in words of its own — the effect clause says
     * "return this card **from your graveyard** to your hand" and CR 113.6m does the rest. So
     * [Recursion.functionsIn] reads it off the cost and the effect for the same reason
     * [producesMana] reads mana-ability-ness off them, and 74 cards stopped refusing to print over a
     * field their own sentence already determined. The cost can say it too — "Exile this card from
     * your graveyard" — and [Payment] carries that zone in; an ability whose cost and effect *both*
     * name one is a shape no card prints, so it refuses rather than choosing.
     */
    private fun abilityFor(
        payment: Payment,
        script: CardScript,
        restrictions: List<ActivationRestriction> = emptyList(),
        sorcerySpeed: Boolean = false,
    ): ActivatedAbility? {
        val cost = payment.cost
        val effect = script.spellEffect ?: return null
        val targets = script.targetRequirements
        val derivedZone = Recursion.functionsIn(effect, cost)
        if (payment.zone != null && derivedZone != null) return null
        if (script != CardScript(spellEffect = effect, targetRequirements = targets)) return null
        if (removesTheSource(cost) && Slots.readsPropertyOf(script, entity = "Source", property = "CounterCount")) return null
        val manaAbility = targets.isEmpty() && producesMana(effect) && !movesLibraryCard(cost, effect)
        return ActivatedAbility(
            id = ID,
            cost = cost,
            effect = effect,
            targetRequirements = targets,
            timing = when {
                sorcerySpeed -> TimingRule.SorcerySpeed
                manaAbility -> TimingRule.ManaAbility
                else -> TimingRule.InstantSpeed
            },
            isManaAbility = manaAbility,
            restrictions = restrictions,
            activateFromZone = payment.zone ?: derivedZone ?: Zone.BATTLEFIELD,
        )
    }

    /**
     * A cost that puts the source somewhere else before the ability resolves — CR 113.7a's
     * last-known-information case, as an activation cost.
     *
     * "{4}, {T}, Sacrifice ~: Each opponent loses life equal to the number of soul counters on ~."
     * (Ravenous Amulet) is the shape. [Amounts]' `counterCount` reads the source's **live** tally,
     * and by resolution the source is in the graveyard, so that reading evaluates to zero: the
     * sentence round-trips byte-perfectly and means a different card. The SDK has the right value
     * for this position, `DynamicAmount.LastKnownSourceCounters`, and the card corpus writes it.
     *
     * Which of the two a clause means is decided by the *ability around it* and a step cannot see
     * that — the same finding [Amounts]' `namesX` records for the trigger position, refused there
     * for the same reason. So this refuses too, rather than emitting the model that evaluates to
     * zero. Deriving the translation instead is the better answer and it is a piece of work of its
     * own: it has to run in both directions, because a golden carrying the last-known value must
     * come back as the live one before [Steps.step] can print it.
     *
     * P/T needs no such rule and that asymmetry is the engine's, not an oversight: the SDK applies
     * the last-known snapshot to `EntityProperty(Self, Power|Toughness)` automatically, so
     * "{T}, Sacrifice ~: You gain life equal to its power." is already correct as written.
     */
    private fun removesTheSource(cost: AbilityCost): Boolean = when (cost) {
        AbilityCost.SacrificeSelf, AbilityCost.ExileSelf -> true
        is AbilityCost.Composite -> cost.costs.any(::removesTheSource)
        else -> false
    }

    /**
     * CR 605.1a's "could add mana to a player's mana pool when it resolves".
     *
     * Every effect that adds mana counts, not only the symbol form: "Add one mana of any color" and
     * "Add three mana in any combination of {R} and/or {G}" are mana abilities by the same rule, and
     * reading only the two symbol effects made Blood Celebrant, Goblin Clearcutter and Wirewood
     * Channeler come out as instant-speed abilities that go on the stack. The differential found all
     * three the first time the grammar could read them.
     *
     * **Most riders do not stop it being one.** The rule says "could add mana … when it resolves",
     * not "does nothing else", so a mana line with something attached is still a mana line and the
     * walk has to see past the outermost effect. Reading only that made Cryptex's unlock counter and
     * Path of Ancestry's scry come out as instant-speed abilities that go on the stack, which are
     * different cards. The walk therefore descends through the one shape a multi-clause line builds
     * — a `CompositeEffect` — and no further: a mana effect buried under a gate or a `ForEach` is one
     * that *might not* happen, and CR 605.1a's "could" is about the ability, not about a branch the
     * grammar has not proved reachable.
     *
     * The exception is a rider that touches a library, which [movesLibraryCard] handles separately —
     * see there for why "{1}, {T}, Sacrifice this artifact: Add one mana of any color. Draw a card."
     * is no longer a mana ability.
     */
    private fun producesMana(effect: Effect): Boolean = when (effect) {
        is AddManaEffect, is AddColorlessManaEffect, is AddManaOfChoiceEffect, is AddDynamicManaEffect -> true
        is CompositeEffect -> effect.effects.any(::producesMana)
        else -> false
    }

    /**
     * CR 605.1a's "and its cost and effect don't move any card to or from a library".
     *
     * The August 7, 2026 rules update added this clause, and it reclassified seven cards the
     * grammar already read: Chromatic Sphere and the five Odyssey Eggs ("Add {W}{U}. Draw a card.")
     * lose it on the effect, Deranged Assistant ("{T}, Mill a card: Add {C}.") on the cost. Before
     * that update all seven were mana abilities — Chromatic Sphere's own printed ruling still says
     * so — which is exactly why the derivation has to carry the clause rather than the card text:
     * nothing in any of those seven printed lines changed on that date.
     *
     * Cost and effect are read with the same walk, and it descends only through the shapes a printed
     * line builds — `CompositeEffect` and `AbilityCost.Composite` — for [producesMana]'s reason.
     *
     * This has to agree with `CardLinter`'s `MisflaggedManaAbility` rule card-for-card, because the
     * differential gate compares what the grammar derives against what the hand-written cards
     * declare: a disagreement here is reported as a card defect rather than as the rule drift it
     * would actually be. The two therefore split the vocabulary the same way — the nodes that always
     * move a library card, and the pipeline shapes where only [crossesLibraryBoundary] can tell.
     *
     * Scry is not a disqualifier: it reorders cards *within* a library and moves none to or from it,
     * so Path of Ancestry keeps its classification.
     */
    private fun movesLibraryCard(cost: AbilityCost, effect: Effect): Boolean =
        movesLibraryCard(cost) || movesLibraryCard(effect) || crossesLibraryBoundary(effect)

    /**
     * Nodes that move a library card whatever their arguments. `Surveil` puts cards from a library
     * into a graveyard; cascade, discover and the exile-from-the-top family all take cards off a
     * library; `PutOnLibraryPositionOfChoice` puts one back onto it.
     */
    private fun movesLibraryCard(effect: Effect): Boolean = when (effect) {
        is DrawCardsEffect,
        is SurveilEffect,
        is CascadeEffect,
        is DiscoverEffect,
        is ExileFromTopRepeatingEffect,
        is ExileLibraryUntilManaValueEffect,
        is PutOnLibraryPositionOfChoiceEffect -> true
        is CompositeEffect -> effect.effects.any(::movesLibraryCard)
        else -> false
    }

    /**
     * True if a card *crosses* the library boundary somewhere in [effect] — the pipeline half, and
     * the reason a `TopOfLibrary` gather is not on its own a disqualifier.
     *
     * `CardSource.TopOfLibrary` is the shared gather for mill, exile-the-top, surveil, scry **and
     * look-at-top**, and `CardDestination.ToZone(Zone.LIBRARY)` is the shared put-back for
     * shuffle-in, put-on-top *and* the same reorders. Either alone says only that a library was
     * touched; 605.1a asks whether a card ended up on the other side of it. So the default is that
     * touching a library counts, and exactly one shape is carved out: a **reorder**, where cards come
     * off a library and every one of them goes straight back into it —
     * `LibraryPatterns.lookAtTopAndReorder` and the pipeline `Scry` expands to, which must classify
     * the same way the compact `Scry` node does.
     */
    private fun crossesLibraryBoundary(effect: Effect): Boolean {
        val fromLibrary = anyEffect(effect) {
            it is GatherCardsEffect && it.source.let { source ->
                source is CardSource.TopOfLibrary || source is CardSource.BottomOfLibrary ||
                    (source is CardSource.FromZone && source.zone == Zone.LIBRARY) ||
                    (source is CardSource.FromMultipleZones && Zone.LIBRARY in source.zones)
            }
        }
        val toLibrary = anyEffect(effect) { it.movesTo(Zone.LIBRARY) == true }
        if (!fromLibrary && !toLibrary) return false

        val toElsewhere = anyEffect(effect) { it.movesTo(Zone.LIBRARY) == false }
        // A cast takes the card to the stack, so a gather it consumes has left the library even
        // though no destination says so.
        val castsFromCollection = anyEffect(effect) { it is CastFromCollectionWithoutPayingCostEffect }
        val reorder = fromLibrary && toLibrary && !toElsewhere && !castsFromCollection
        return !reorder
    }

    /** Null unless this is a `MoveCollection` to a fixed zone; else whether that zone is [zone]. */
    private fun Effect.movesTo(zone: Zone): Boolean? =
        (this as? MoveCollectionEffect)?.destination
            ?.let { it as? CardDestination.ToZone }
            ?.let { it.zone == zone }

    /** True if [effect] or any effect inside its `CompositeEffect` chain satisfies [predicate]. */
    private fun anyEffect(effect: Effect, predicate: (Effect) -> Boolean): Boolean =
        predicate(effect) ||
            (effect is CompositeEffect && effect.effects.any { anyEffect(it, predicate) })

    private fun movesLibraryCard(cost: AbilityCost): Boolean = when (cost) {
        is AbilityCost.Atom -> cost.atom.let { atom ->
            atom is CostAtom.Mill || (atom is CostAtom.ExileFrom && atom.zone == Zone.LIBRARY)
        }
        is AbilityCost.Composite -> cost.costs.any(::movesLibraryCard)
        else -> false
    }

    /**
     * An activation cost together with the zone its printing names, when it names one.
     *
     * Not a model of the text: both fields are SDK values copied straight onto the ability. It exists
     * because "Exile ~" and "Exile ~ from your graveyard" are the one `AbilityCost.ExileSelf`, and the
     * word that tells them apart lands on `ActivatedAbility.activateFromZone` — a field the cost slot
     * cannot reach. CR 113.6m makes that zone a consequence of the cost ("an ability whose cost …
     * specifies that it moves the object it's on out of a particular zone functions only in that
     * zone"), so the cost rule reports it and [abilityFor] derives the field, as it does for the
     * self-moving effects [Recursion.functionsIn] reads.
     */
    private data class Payment(val cost: AbilityCost, val zone: Zone? = null)

    private val payment: Phrase<Payment> = oneOf(
        "an activation cost",
        phrase("{cost}", name = "an activation cost on the battlefield") {
            slot("cost", Costs.cost)
            build { Payment(it.value("cost")) }
            match { paid -> if (paid.zone == null) bind("cost" to paid.cost) else null }
        },
        phrase("{cost}", name = "an activation cost paid from the graveyard") {
            slot("cost", Costs.fromGraveyard)
            build { Payment(it.value("cost"), Zone.GRAVEYARD) }
            match { paid -> if (paid.zone == Zone.GRAVEYARD) bind("cost" to paid.cost) else null }
        },
    )

    /**
     * The payment [ability] was printed with: the graveyard spelling when the ability works from the
     * graveyard and the card's last cost exiles it, the bare cost otherwise. [abilityFor]'s
     * reconstruction is what checks the guess, so a graveyard ability whose zone comes from its
     * effect instead still prints through the bare cost.
     */
    private fun paymentOf(ability: ActivatedAbility): Payment {
        val last = (ability.cost as? AbilityCost.Composite)?.costs?.lastOrNull() ?: ability.cost
        val exilesFromGraveyard = ability.activateFromZone == Zone.GRAVEYARD && last == AbilityCost.ExileSelf
        return Payment(ability.cost, Zone.GRAVEYARD.takeIf { exilesFromGraveyard })
    }

    /** "{cost}: {effect}" — one ability, whatever [Steps] can read after the colon. */
    private val single: Phrase<List<ActivatedAbility>> =
        phrase("{cost}: {effect}", name = "an activated ability") {
            slot("cost", payment)
            slot("effect", Steps.step)
            build { bindings ->
                abilityFor(bindings.value<Payment>("cost"), bindings.value("effect"))?.let { listOf(it) }
            }
            match { abilities ->
                val ability = abilities.singleOrNull() ?: return@match null
                val script = CardScript(
                    spellEffect = ability.effect,
                    targetRequirements = ability.targetRequirements,
                )
                if (abilityFor(paymentOf(ability), script)?.copy(id = ability.id) != ability) return@match null
                bind("cost" to paymentOf(ability), "effect" to script)
            }
        }

    /**
     * "{cost}: Add {B} or {G}." — several abilities, one per kind of mana, sharing the cost.
     *
     * Not a member of [single] over a list-valued effect slot: the abilities differ only in their
     * effect, and every other field — including the derived mana-ability flags — has to come out
     * identical for the line to be printable at all, which is what the reconstruct-and-compare in
     * `match` checks one ability at a time.
     */
    private val choice: Phrase<List<ActivatedAbility>> =
        phrase("{cost}: {alternatives}", name = "an activated mana ability with a choice") {
            slot("cost", payment)
            slot(
                "alternatives",
                oneOf("several kinds of mana", Mana.addedAlternatives, Mana.addedAlternativesRestricted),
            )
            build { bindings ->
                val payment = bindings.value<Payment>("cost")
                val built = bindings.value<List<Effect>>("alternatives")
                    .map { abilityFor(payment, CardScript(spellEffect = it)) }
                if (built.any { it == null }) null else built.filterNotNull()
            }
            match { abilities ->
                if (abilities.size < 2) return@match null
                val payment = paymentOf(abilities.first())
                val printable = abilities.all { ability ->
                    abilityFor(payment, CardScript(spellEffect = ability.effect))
                        ?.copy(id = ability.id) == ability
                }
                if (!printable) return@match null
                bind("cost" to payment, "alternatives" to abilities.map { it.effect })
            }
        }

    /**
     * "{T}: Add {U} or {R}. ~ deals 1 damage to you." — [choice] with a sentence after the mana,
     * which the painlands and the Talismans print.
     *
     * The rider is one sentence that follows the choice, so it belongs to every ability the choice
     * denotes: the hand-written cards spell each one `Effects.AddMana(colour) then rider`, and that
     * is what this builds. The rider is any [Steps] line, read once and copied onto each ability,
     * and `match` insists every ability carries the *same* one — two abilities with different
     * riders were not printed as one line. A rider that targets is refused: the abilities would
     * share one target slot on paper and hold two in the model.
     *
     * Disjoint from [choice] by model: [Mana.addedAlternatives] refuses anything that is not a bare
     * mana effect, and this refuses anything that is.
     */
    private val choiceWithRider: Phrase<List<ActivatedAbility>> =
        phrase("{cost}: {alternatives} {rider}", name = "an activated mana ability with a choice and a rider") {
            slot("cost", payment)
            slot("alternatives", Mana.addedAlternatives)
            slot("rider", Steps.step)
            build { bindings ->
                val rider = bindings.value<CardScript>("rider")
                val after = rider.spellEffect ?: return@build null
                if (rider != CardScript(spellEffect = after)) return@build null
                val payment = bindings.value<Payment>("cost")
                val built = bindings.value<List<Effect>>("alternatives")
                    .map { abilityFor(payment, CardScript(spellEffect = it then after)) }
                if (built.any { it == null }) null else built.filterNotNull()
            }
            match { abilities ->
                if (abilities.size < 2) return@match null
                val payment = paymentOf(abilities.first())
                val split = abilities.map { ability ->
                    val parts = (ability.effect as? CompositeEffect)?.effects ?: return@match null
                    if (parts.size != 2) return@match null
                    parts[0] to parts[1]
                }
                val after = split.first().second
                if (split.any { it.second != after }) return@match null
                val printable = abilities.zip(split).all { (ability, parts) ->
                    abilityFor(payment, CardScript(spellEffect = parts.first then after))
                        ?.copy(id = ability.id) == ability
                }
                if (!printable) return@match null
                bind(
                    "cost" to payment,
                    "alternatives" to split.map { it.first },
                    "rider" to CardScript(spellEffect = after),
                )
            }
        }

    /**
     * "{cost}: {effect} Activate only during your turn, before attackers are declared." — the same
     * ability with the sentence that says when it may be activated.
     *
     * A second rule rather than an optional slot, because an optional literal would leave printing
     * underdetermined between the two forms for an ability whose restriction list is empty. The two
     * take disjoint models — this one refuses an empty list — so the model decides which prints, the
     * property every alternation in this grammar is written to have.
     */
    private val restricted: Phrase<List<ActivatedAbility>> =
        phrase("{cost}: {effect} {restrictions}", name = "an activated ability with a restriction") {
            slot("cost", payment)
            slot("effect", Steps.step)
            slot("restrictions", Restrictions.activationSentence)
            build { bindings ->
                val restrictions = bindings.value<List<ActivationRestriction>>("restrictions")
                if (restrictions.isEmpty()) return@build null
                abilityFor(bindings.value<Payment>("cost"), bindings.value("effect"), restrictions)?.let { listOf(it) }
            }
            match { abilities ->
                val ability = abilities.singleOrNull() ?: return@match null
                if (ability.restrictions.isEmpty()) return@match null
                val script = CardScript(
                    spellEffect = ability.effect,
                    targetRequirements = ability.targetRequirements,
                )
                if (abilityFor(paymentOf(ability), script, ability.restrictions)?.copy(id = ability.id) != ability) {
                    return@match null
                }
                bind("cost" to paymentOf(ability), "effect" to script, "restrictions" to ability.restrictions)
            }
        }

    /**
     * "{2}{B}, {T}, Sacrifice a Zombie: Destroy target non-Zombie creature. It can't be regenerated.
     * Activate only as a sorcery." — Deathmark Prelate.
     *
     * The same trailing sentence [restricted] reads, and yet **not** an `ActivationRestriction`: the
     * SDK spells sorcery-speed as `TimingRule.SorcerySpeed`, a field the ability already has, so the
     * sentence sets a timing rather than adding to a list. That is why it is a rule of its own and
     * not a row in [Restrictions] — a row there would produce a restriction no hand-written card
     * carries, and it would round-trip while disagreeing with every card that prints the sentence.
     */
    private val sorcerySpeed: Phrase<List<ActivatedAbility>> =
        phrase("{cost}: {effect} activate only as a sorcery.", name = "an activated ability at sorcery speed") {
            slot("cost", payment)
            slot("effect", Steps.step)
            build { bindings ->
                abilityFor(bindings.value<Payment>("cost"), bindings.value("effect"), sorcerySpeed = true)
                    ?.let { listOf(it) }
            }
            match { abilities ->
                val ability = abilities.singleOrNull() ?: return@match null
                if (ability.timing != TimingRule.SorcerySpeed) return@match null
                val script = CardScript(
                    spellEffect = ability.effect,
                    targetRequirements = ability.targetRequirements,
                )
                if (abilityFor(paymentOf(ability), script, sorcerySpeed = true)?.copy(id = ability.id) != ability) {
                    return@match null
                }
                bind("cost" to paymentOf(ability), "effect" to script)
            }
        }

    val abilities: Phrase<List<ActivatedAbility>> =
        oneOf("an activated ability", single, restricted, sorcerySpeed, choice, choiceWithRider)

    /**
     * `"{T}: Regenerate target Sliver."` — one activated ability inside the quotation marks a
     * *granted* ability is printed in.
     *
     * The quotes are the whole rule. Everything inside them is [abilities] unchanged, which is the
     * point: a granted ability is the same English an ability line prints, so `Statics`' lord rules
     * slot this and inherit the entire activated-ability grammar rather than restating a verb.
     *
     * Exactly one ability, because `GrantActivatedAbility` holds one. The list form the line rule
     * needs — "{T}: Add {B} or {G}." as two abilities sharing a cost — has nowhere to go in a grant,
     * so it declines here rather than being silently truncated to its first member.
     *
     * ### `~` inside the quotes is not the card whose line this is
     *
     * A self-reference has two printed shapes and CR 201.4 keeps them apart: "this creature" is the
     * object that *has* the ability — the creature the Equipment or Aura handed it to,
     * [com.wingedsheep.sdk.scripting.targets.EffectTarget.Self] — while the card's own **name**
     * still means the card that printed it, `EffectTarget.GrantingSource`. Outside a quote the two
     * denote one object and `Normalizer` abstracts both to `~`; inside one they come apart, so
     * normalization keeps the name as its own token there and nothing under these quotes can read
     * it. The effect vocabulary spells `~` as `Self`, which is the reading that is true for the
     * noun form and the only form that reaches here.
     *
     * Trusty Boomerang is what made that necessary: "Equipped creature has "{1}, {T}: Tap target
     * creature. Return Trusty Boomerang to its owner's hand."" bounces the Equipment, and reading it
     * as the equipped creature round-trips byte-perfectly while meaning the wrong permanent. It
     * declines instead, and is counted; the twenty-one Slivers and Auras that spell the *noun* are
     * unaffected, because the distinction is now in the text the grammar sees.
     */
    val quoted: Phrase<ActivatedAbility> = phrase("\"{ability}\"", name = "a quoted activated ability") {
        slot("ability", abilities)
        build { it.value<List<ActivatedAbility>>("ability").singleOrNull() }
        match { bind("ability" to listOf(it)) }
    }
}
