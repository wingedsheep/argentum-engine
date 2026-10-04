package com.wingedsheep.assay.grammar

import com.wingedsheep.assay.syntax.Phrase
import com.wingedsheep.assay.syntax.bind
import com.wingedsheep.assay.syntax.phrase
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.ConditionalOnCollectionEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.DrawUpToEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.ForEachEffect
import com.wingedsheep.sdk.scripting.effects.GainLifeEffect
import com.wingedsheep.sdk.scripting.effects.LookAtTargetHandEffect
import com.wingedsheep.sdk.scripting.effects.RevealHandEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Clauses about hands — looking at one, making a player discard from one, drawing into one.
 *
 * Like [Library], most of these denote a `Patterns` recipe rather than a single effect, and the
 * rules build through the published facade and match by reconstructing it. The exception is looking
 * at a hand, which really is one effect.
 */
object Hand {

    /** "Look at target opponent's hand." — Sorcerous Sight. */
    private val lookAtOpponentHand: Phrase<CardScript> = run {
        val script = CardScript(
            spellEffect = LookAtTargetHandEffect(Targets.bound()),
            targetRequirements = listOf(Targets.opponent()),
        )
        phrase("look at target opponent's hand", name = "look at target opponent's hand") {
            build { script }
            match { if (it == script) bind() else null }
        }
    }

    /**
     * "Target opponent discards a card at random." — Mind Knives; "At the beginning of each player's
     * upkeep, that player discards a card at random." — Bottomless Pit.
     *
     * [discard]'s subject-per-template split over `Patterns.Hand.discardRandom`, a separate recipe
     * because the selection is random rather than chosen. Only the singular: the corpus prints the
     * counted random discard on one card per subject, which is a row with no reader.
     */
    private fun discardAtRandom(
        template: String,
        name: String,
        target: EffectTarget,
        requirements: List<TargetRequirement>,
    ): Phrase<CardScript> {
        val script = CardScript(
            spellEffect = Patterns.Hand.discardRandom(1, target),
            targetRequirements = requirements,
        )
        return phrase(template, name = name) {
            build { script }
            match { if (it == script) bind() else null }
        }
    }

    /**
     * "Each opponent discards a card." — Noxious Toad's death trigger; "Each opponent discards two
     * cards." — Unnerve.
     *
     * `Patterns.Hand.eachOpponentDiscards` is its own recipe (a per-opponent iteration, so each one
     * chooses from their own hand), so this is [discard]'s count split over that facade rather than
     * a subject row of it.
     */
    private fun eachOpponentDiscards(template: String, name: String, count: Int?): Phrase<CardScript> {
        fun scriptFor(cards: Int) = CardScript(spellEffect = Patterns.Hand.eachOpponentDiscards(cards))
        return phrase(template, name = name) {
            if (count == null) slot("n", Cardinals.word)
            build { bindings -> scriptFor(count ?: bindings.int("n")) }
            match { script ->
                val cards = count ?: eachOpponentDiscardedCount(script) ?: return@match null
                if (count == null && !Cardinals.spellable(cards)) return@match null
                if (script != scriptFor(cards)) return@match null
                bind("n" to cards)
            }
        }
    }

    /**
     * "Each player may draw up to two cards. For each card less than two a player draws this way,
     * that player gains 2 life." — Temporary Truce.
     *
     * Two printed sentences and one rule, for [Library.lookAtOpponentTopAndBury]'s reason: the model
     * is a single `ForEachPlayer` iteration whose two inner steps are *linked by a pipeline slot*
     * ("cardsNotDrawn"), so the second sentence has no meaning without the first and the split
     * [Steps.sequence] performs would produce a half that denotes nothing.
     *
     * The card's limit appears twice in the text and once in the model, so the rule takes two slots
     * and refuses to build when they disagree — the honest reading of a sentence that states one
     * number in two places, and the only one that stays invertible.
     */
    private val eachPlayerMayDraw: Phrase<CardScript> = run {
        fun scriptFor(maximum: Int, life: Int) =
            CardScript(spellEffect = Patterns.Hand.eachPlayerMayDraw(maxCards = maximum, lifePerCardNotDrawn = life))
        phrase(
            "each player may draw up to {max} cards. for each card less than {limit} a player " +
                "draws this way, that player gains {life} life",
            name = "each player may draw up to N cards",
        ) {
            slot("max", Cardinals.word)
            slot("limit", Cardinals.word)
            slot("life", Primitives.cardinal)
            build { bindings ->
                val maximum = bindings.int("max")
                if (maximum != bindings.int("limit")) return@build null
                scriptFor(maximum, bindings.int("life"))
            }
            match { script ->
                val body = (script.spellEffect as? ForEachEffect)?.body as? CompositeEffect ?: return@match null
                val maximum = (body.effects.firstOrNull() as? DrawUpToEffect)?.maxCards ?: return@match null
                val gain = (body.effects.getOrNull(1) as? GainLifeEffect)?.amount as? DynamicAmount.Multiply
                    ?: return@match null
                if (!Cardinals.spellable(maximum)) return@match null
                if (script != scriptFor(maximum, gain.multiplier)) return@match null
                bind("max" to maximum, "limit" to maximum, "life" to gain.multiplier)
            }
        }
    }

    /** "Look at target player's hand." — Ingenious Thief; the same effect over the wider target. */
    private val lookAtPlayerHand: Phrase<CardScript> = run {
        val script = CardScript(
            spellEffect = LookAtTargetHandEffect(Targets.bound()),
            targetRequirements = listOf(Targets.player()),
        )
        phrase("look at target player's hand", name = "look at target player's hand") {
            build { script }
            match { if (it == script) bind() else null }
        }
    }

    /**
     * The discards — "discard a card.", "Target player discards two cards.", "Have target opponent
     * discard a card."
     *
     * One recipe (`Patterns.Hand.discardCards`) with two variables, the count and who discards, and
     * English spells the second as the sentence's *subject* rather than as a word in a fixed
     * position — "target player discards" against "have target opponent discard". So the shape is a
     * template per subject, each carrying its own requirement, and the singular and plural counts
     * are two rows for the reason [Steps] gives: the article and the noun both change.
     *
     * "That player" is the player the trigger named — the one dealt combat damage (Blazing
     * Specter), the one whose upkeep began (Necrogen Mists), the one who cast the spell
     * (Oppression) — so it is `Player.TriggeringPlayer` and declares no requirement, the same
     * reading [Steps]' damage recipients give the phrase.
     *
     * ### The causative is the consent gate's own row
     *
     * "You may have target opponent discard a card." is the same discard behind `Effects.May`, and
     * English marks the gate by moving the subject inside "have" — [Steps]' forced-sacrifice
     * argument, one verb over. The row used to be the bare "have target opponent discard a card"
     * with [Steps]' `mayClause` supplying the gate, which was harmless only while it was the one
     * row printing that model: with "target opponent discards a card" beside it, the bare
     * causative would print every plain discard as "have …". So the gate moved into the row, and
     * the composed "you may target opponent discards a card" never wins, since [Hand.clauses]
     * precede `mayClause` in `simpleClause`. `HandTest` asserts both printings.
     *
     * @param causative the "you may have … discard" surface and its `Effects.May` wrapper.
     */
    private fun discard(
        template: String,
        name: String,
        count: Int?,
        target: com.wingedsheep.sdk.scripting.targets.EffectTarget,
        requirements: List<TargetRequirement>,
        causative: Boolean = false,
    ): Phrase<CardScript> {
        fun scriptFor(cards: Int): CardScript {
            val effect = Patterns.Hand.discardCards(cards, target)
            return CardScript(
                spellEffect = if (causative) Effects.May(effect) else effect,
                targetRequirements = requirements,
            )
        }
        return phrase(template, name = name) {
            if (count == null) slot("n", Cardinals.word)
            build { bindings -> scriptFor(count ?: bindings.int("n")) }
            match { script ->
                val cards = count ?: discardedCount(script) ?: return@match null
                if (count == null && !Cardinals.spellable(cards)) return@match null
                if (script != scriptFor(cards)) return@match null
                bind("n" to cards)
            }
        }
    }

    /** How many cards each opponent discards in an `eachOpponentDiscards` iteration, off its select step. */
    private fun eachOpponentDiscardedCount(script: CardScript): Int? {
        val body = (script.spellEffect as? ForEachEffect)?.body as? CompositeEffect ?: return null
        val select = body.effects.filterIsInstance<SelectFromCollectionEffect>().firstOrNull() ?: return null
        val mode = select.selection as? SelectionMode.ChooseExactly ?: return null
        return (mode.count as? DynamicAmount.Fixed)?.amount
    }

    /** How many cards a `discardCards` pipeline discards, read off its select step. */
    private fun discardedCount(script: CardScript): Int? {
        val select = (script.spellEffect as? CompositeEffect)?.effects
            ?.filterIsInstance<SelectFromCollectionEffect>()?.firstOrNull() ?: return null
        val mode = select.selection as? SelectionMode.ChooseExactly ?: return null
        return (mode.count as? DynamicAmount.Fixed)?.amount
    }

    /**
     * The whole-table hand effects — "Each player draws X cards.", "Each player discards any number
     * of cards, then draws that many cards.", the wheel.
     *
     * Each is one published recipe and one printed sentence with no variable in it, so each is a
     * constant rule: the reconstruction *is* the comparison, and a card whose pipeline differs in
     * any field declines rather than printing a sentence it does not mean.
     */
    private fun tableWide(template: String, name: String, effect: Effect): Phrase<CardScript> {
        val script = CardScript(spellEffect = effect)
        return phrase(template, name = name) {
            build { script }
            match { if (it == script) bind() else null }
        }
    }

    /** "Target opponent reveals their hand." — Baleful Stare's first sentence. */
    private val opponentRevealsHand: Phrase<CardScript> = run {
        val script = CardScript(
            spellEffect = RevealHandEffect(Targets.bound()),
            targetRequirements = listOf(Targets.opponent()),
        )
        phrase("target opponent reveals their hand", name = "target opponent reveals their hand") {
            build { script }
            match { if (it == script) bind() else null }
        }
    }

    /**
     * "You may put a creature card with a morph ability from your hand onto the battlefield face up.
     * If you do, return ~ to its owner's hand." — Dermoplasm.
     *
     * Two printed sentences and one recipe, for [Library.lookAtOpponentTopAndBury]'s reason: "if you
     * do" reads the pipeline slot the first sentence stored, so the second sentence has no meaning
     * without it and neither half is a clause on its own. `ConditionalOnCollectionEffect` is the SDK
     * spelling of that read — a branch on whether anything was put, not a condition on the game.
     *
     * "Face up" is a literal: `putFromHand`'s default placement is exactly that, and the phrase
     * exists to distinguish it from morph's face-down alternative rather than to name a field.
     */
    private val putFromHandThenBounce: Phrase<CardScript> = run {
        val script = CardScript(
            spellEffect = Patterns.Hand.putFromHand(filter = GameObjectFilter.Creature.withMorph()) then ConditionalOnCollectionEffect(
                collection = "putting",
                ifNotEmpty = Effects.Move(EffectTarget.Self, Zone.HAND),
            )
        )
        phrase(
            "you may put a creature card with a morph ability from your hand onto the battlefield " +
                "face up. if you do, return {self} to its owner's hand",
            name = "put a morph card from hand and bounce the source",
        ) {
            slot("self", Primitives.self)
            build { script }
            match { if (it == script) bind("self" to Unit) else null }
        }
    }

    /**
     * "That player reveals X cards from their hand and you choose one of them. That player discards
     * that card." — Hollow Specter, after its "you may pay {X}".
     *
     * Two printed sentences and one four-step pipeline whose steps do not line up with the sentence
     * boundary at all: the reveal is a gather plus a choice *by the revealing player*, and the
     * discard is a second choice by the controller followed by a move. The chooser is the field that
     * makes the two sentences one recipe, and it is the reason splitting the text would leave halves
     * that denote nothing.
     */
    private val revealAndChooseDiscard: Phrase<CardScript> = run {
        val script = CardScript(
            spellEffect = GatherCardsEffect(
                source = CardSource.FromZone(Zone.HAND, Player.TriggeringPlayer),
                storeAs = "hand",
            ) then
                SelectFromCollectionEffect(
                    from = "hand",
                    selection = SelectionMode.ChooseExactly(DynamicAmount.XValue),
                    chooser = Chooser.TriggeringPlayer,
                    storeSelected = "revealed",
                ) then
                SelectFromCollectionEffect(
                    from = "revealed",
                    selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                    chooser = Chooser.Controller,
                    storeSelected = "toDiscard",
                ) then
                MoveCollectionEffect(
                    from = "toDiscard",
                    destination = CardDestination.ToZone(Zone.GRAVEYARD, Player.TriggeringPlayer),
                    moveType = MoveType.Discard,
                )
        )
        phrase(
            "that player reveals X cards from their hand and you choose one of them. that player " +
                "discards that card",
            name = "the triggering player reveals X cards and discards one you choose",
        ) {
            build { script }
            match { if (it == script) bind() else null }
        }
    }


    /**
     * "Discard your hand." / "Target player discards their hand." — 84 and 31 printed lines.
     *
     * The whole hand, so there is no count and no selection: `Patterns.Hand.discardHand` gathers the
     * zone and moves all of it, which is a different recipe from [discard]'s choose-exactly-N and not
     * that rule with `n` set to "all". The two rows are the same subject-per-template split the
     * counted family takes — English inflects the verb with its subject and spells the possessive to
     * agree ("your" against "their"), neither of which is a word a slot could supply.
     */
    private fun discardWholeHand(
        template: String,
        name: String,
        target: EffectTarget,
        requirements: List<TargetRequirement>,
    ): Phrase<CardScript> {
        val script = CardScript(
            spellEffect = Patterns.Hand.discardHand(target),
            targetRequirements = requirements,
        )
        return phrase(template, name = name) {
            build { script }
            match { if (it == script) bind() else null }
        }
    }

    /**
     * "Target opponent exiles two cards from their hand." — Aim for the Head, Witness the End,
     * Vessel of Malignity, and the modal bullet on Perfect Intimidation.
     *
     * [discard]'s shape one destination over, and the reason it is a separate family rather than a
     * slot on that one is the SDK's: exiling and discarding are two published recipes
     * (`Patterns.Hand.exileFromHand` against `discardCards`), because a discard is a
     * `MoveType.Discard` that feeds every "whenever you discard" trigger and CR 701.8's turn tally,
     * and an exile from hand is none of those. A destination slot spanning the two would have let
     * the grammar print one as the other.
     *
     * **The chooser is the sentence's subject and the facade derives it.** "Target opponent exiles"
     * means that opponent picks, which `exileFromHand` reads off the same [EffectTarget] it takes
     * for the zone — so the rows differ only in the requirement they declare, and there is no
     * separate chooser word for a slot to spell.
     */
    private fun exileFromHand(
        template: String,
        name: String,
        count: Int?,
        target: EffectTarget,
        requirements: List<TargetRequirement>,
    ): Phrase<CardScript> {
        fun scriptFor(cards: Int) = CardScript(
            spellEffect = Patterns.Hand.exileFromHand(cards, target),
            targetRequirements = requirements,
        )
        return phrase(template, name = name) {
            if (count == null) slot("n", Cardinals.word)
            build { bindings -> scriptFor(count ?: bindings.int("n")) }
            match { script ->
                val cards = count ?: exiledFromHandCount(script) ?: return@match null
                if (count == null && !Cardinals.spellable(cards)) return@match null
                if (script != scriptFor(cards)) return@match null
                bind("n" to cards)
            }
        }
    }

    /** How many cards an `exileFromHand` pipeline exiles, read off its select step. */
    private fun exiledFromHandCount(script: CardScript): Int? {
        val select = (script.spellEffect as? CompositeEffect)?.effects
            ?.filterIsInstance<SelectFromCollectionEffect>()?.firstOrNull() ?: return null
        val mode = select.selection as? SelectionMode.ChooseExactly ?: return null
        return (mode.count as? DynamicAmount.Fixed)?.amount
    }

    val clauses: List<Phrase<CardScript>> = listOf(
        putFromHandThenBounce,
        revealAndChooseDiscard,
        lookAtOpponentHand,
        opponentRevealsHand,
        lookAtPlayerHand,
        discardAtRandom(
            "target opponent discards a card at random", "target opponent discards at random",
            target = Targets.bound(), requirements = listOf(Targets.opponent()),
        ),
        discardAtRandom(
            "that player discards a card at random", "the triggering player discards at random",
            target = EffectTarget.PlayerRef(Player.TriggeringPlayer), requirements = emptyList(),
        ),
        eachOpponentDiscards("each opponent discards a card", "each opponent discards a card", count = 1),
        eachOpponentDiscards("each opponent discards {n} cards", "each opponent discards cards", count = null),
        eachPlayerMayDraw,
        discard(
            "discard a card", "discard a card",
            count = 1, target = EffectTarget.Controller, requirements = emptyList(),
        ),
        discard(
            "discard {n} cards", "discard cards",
            count = null, target = EffectTarget.Controller, requirements = emptyList(),
        ),
        discard(
            "target player discards a card", "target player discards a card",
            count = 1, target = Targets.bound(), requirements = listOf(Targets.player()),
        ),
        discard(
            "target player discards {n} cards", "target player discards cards",
            count = null, target = Targets.bound(), requirements = listOf(Targets.player()),
        ),
        discard(
            "target opponent discards a card", "target opponent discards a card",
            count = 1, target = Targets.bound(), requirements = listOf(Targets.opponent()),
        ),
        discard(
            "target opponent discards {n} cards", "target opponent discards cards",
            count = null, target = Targets.bound(), requirements = listOf(Targets.opponent()),
        ),
        discard(
            "that player discards a card", "the triggering player discards a card",
            count = 1, target = EffectTarget.PlayerRef(Player.TriggeringPlayer), requirements = emptyList(),
        ),
        discard(
            "that player discards {n} cards", "the triggering player discards cards",
            count = null, target = EffectTarget.PlayerRef(Player.TriggeringPlayer), requirements = emptyList(),
        ),
        discard(
            "you may have target opponent discard a card", "you may have target opponent discard a card",
            count = 1, target = Targets.bound(), requirements = listOf(Targets.opponent()), causative = true,
        ),
        discardWholeHand(
            "discard your hand", "discard your hand",
            target = EffectTarget.Controller, requirements = emptyList(),
        ),
        discardWholeHand(
            "target player discards their hand", "target player discards their hand",
            target = Targets.bound(), requirements = listOf(Targets.player()),
        ),
        exileFromHand(
            "target opponent exiles a card from their hand", "target opponent exiles a card from their hand",
            count = 1, target = Targets.bound(), requirements = listOf(Targets.opponent()),
        ),
        exileFromHand(
            "target opponent exiles {n} cards from their hand", "target opponent exiles cards from their hand",
            count = null, target = Targets.bound(), requirements = listOf(Targets.opponent()),
        ),
        exileFromHand(
            "target player exiles a card from their hand", "target player exiles a card from their hand",
            count = 1, target = Targets.bound(), requirements = listOf(Targets.player()),
        ),
        exileFromHand(
            "target player exiles {n} cards from their hand", "target player exiles cards from their hand",
            count = null, target = Targets.bound(), requirements = listOf(Targets.player()),
        ),
        tableWide(
            "each player draws X cards", "each player draws X cards",
            Patterns.Hand.eachPlayerDrawsX(includeController = true, includeOpponents = true),
        ),
        tableWide(
            "each player discards any number of cards, then draws that many cards",
            "each player discards and redraws",
            Patterns.Hand.eachPlayerDiscardsDraws(),
        ),
        tableWide(
            "each player shuffles the cards from their hand into their library, then draws that many cards",
            "the wheel",
            Patterns.Hand.wheelEffect(Player.Each),
        ),
    )
}
