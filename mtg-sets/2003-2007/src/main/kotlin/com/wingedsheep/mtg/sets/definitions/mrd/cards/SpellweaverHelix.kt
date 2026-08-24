package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.AnyCondition
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * "…the same name as [the spell that was just cast]" — shared by the intervening-if gate and the
 * pipeline's selection step, so the two can never drift apart.
 *
 * Declared above [SpellweaverHelix] on purpose: top-level properties initialize in file order, so
 * a constant referenced by a card must be defined before it.
 */
private val NAMED_AS_TRIGGERING_SPELL: GameObjectFilter =
    GameObjectFilter.Any.sharingNameWith(EntityReference.Triggering)

/**
 * Spellweaver Helix — Mirrodin #247
 * {3} · Artifact · Rare
 *
 * Imprint — When this artifact enters, you may exile two target sorcery cards from a single
 * graveyard.
 * Whenever a player casts a card, if it has the same name as one of the cards exiled with this
 * artifact, you may copy the other. If you do, you may cast the copy without paying its mana cost.
 *
 * Modelling notes:
 * - "From a single graveyard" is [TargetObject.sameOwner], the cross-target constraint Arashin
 *   Sunshield already uses; cards in a graveyard are keyed by owner, so same-owner *is*
 *   same-graveyard.
 * - The pile is a *linked* exile (CR 607) written by this artifact's own ETB trigger, so the cast
 *   trigger reads only what this Helix imprinted. It is re-read live: if an imprinted card leaves
 *   exile the pile shrinks on its own, which is the ruling that no copy is made once the imprinted
 *   card is gone.
 * - "The other" is authored as **select one, take the remainder** rather than as a
 *   name-exclusion filter, and that is what makes both of the fiddly rulings fall out of the
 *   pipeline instead of needing a special case:
 *     - two imprints sharing a name, that name cast → `ChooseExactly(1)` matches both, the
 *       controller picks one, the remainder is the other → exactly **one** copy, not two;
 *     - one imprint left, its name cast → the only eligible card is selected, the remainder is
 *       empty → nothing is copied.
 * - The copy is made in exile and cast during this trigger's own resolution — the same
 *   `Copy…IntoCollection` → `CastFromCollectionWithoutPayingCost` chain Isochron Scepter uses,
 *   which is the rule-breaking the card's ruling describes ("you cast the copy while this ability
 *   is resolving, and still on the stack").
 * - The two printed "may"s collapse to one prompt for the reason spelled out on Isochron Scepter:
 *   a copy that is created and not cast is a card-shaped object in exile that the next
 *   state-based-action check removes, and nothing triggers on it, so declining the second "may" is
 *   indistinguishable from declining the first.
 */
val SpellweaverHelix = card("Spellweaver Helix") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "Imprint — When this artifact enters, you may exile two target sorcery cards " +
        "from a single graveyard.\n" +
        "Whenever a player casts a card, if it has the same name as one of the cards exiled with " +
        "this artifact, you may copy the other. If you do, you may cast the copy without paying " +
        "its mana cost."

    // Imprint — When this artifact enters, you may exile two target sorcery cards from a single
    // graveyard.
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        optional = true
        target(
            "two target sorcery cards from a single graveyard",
            TargetObject(
                count = 2,
                filter = TargetFilter(GameObjectFilter.Sorcery, zone = Zone.GRAVEYARD),
                sameOwner = true
            )
        )
        effect = Effects.Composite(
            GatherCardsEffect(source = CardSource.ChosenTargets, storeAs = "helixImprints"),
            MoveCollectionEffect(
                from = "helixImprints",
                destination = CardDestination.ToZone(Zone.EXILE),
                linkToSource = true
            )
        )
        description = "Imprint — When this artifact enters, you may exile two target sorcery " +
            "cards from a single graveyard."
    }

    // Whenever a player casts a card, if it has the same name as one of the cards exiled with this
    // artifact, you may copy the other. If you do, you may cast the copy without paying its mana
    // cost.
    triggeredAbility {
        trigger = Triggers.AnyPlayerCastsSpell
        // "one of the cards exiled with this artifact" — the imprint pile holds two, so the
        // intervening-if asks the same question of each slot.
        interveningIf = AnyCondition(
            listOf(
                Conditions.LinkedExiledCardMatches(NAMED_AS_TRIGGERING_SPELL, index = 0),
                Conditions.LinkedExiledCardMatches(NAMED_AS_TRIGGERING_SPELL, index = 1)
            )
        )
        effect = MayEffect(
            Effects.Composite(
                GatherCardsEffect(source = CardSource.FromLinkedExile(), storeAs = "helixPile"),
                SelectFromCollectionEffect(
                    from = "helixPile",
                    selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                    filter = NAMED_AS_TRIGGERING_SPELL,
                    storeSelected = "helixNamed",
                    storeRemainder = "helixOther",
                    prompt = "Choose the exiled card with the same name as the spell just cast"
                ),
                Effects.CopyCollectionIntoCollection(from = "helixOther", storeAs = "helixCopy"),
                Effects.CastFromCollectionWithoutPayingCost("helixCopy")
            ),
            descriptionOverride = "You may copy the other exiled card and cast the copy without " +
                "paying its mana cost."
        )
        description = "Whenever a player casts a card, if it has the same name as one of the " +
            "cards exiled with this artifact, you may copy the other. If you do, you may cast " +
            "the copy without paying its mana cost."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "247"
        artist = "Luca Zontini"
        imageUri = "https://cards.scryfall.io/normal/front/a/4/a48d44b4-1c3b-4109-aaad-8351bf8a7624.jpg?1783944502"

        ruling(
            "2017-04-18",
            "A split card has the same name as a spell if either of its names is that spell's name."
        )
        ruling("2005-03-01", "The creation of the copy and then the casting of the copy are both optional.")
        ruling(
            "2004-12-01",
            "Spellweaver Helix's second ability creates a copy of the imprinted card in the Exile " +
                "zone (that's where the imprinted sorcery card is), then allows you to cast it " +
                "without paying its mana cost."
        )
        ruling(
            "2004-12-01",
            "You cast the copy while this ability is resolving, and still on the stack. Normally, " +
                "you're not allowed to cast spells or activate abilities at this time. Spellweaver " +
                "Helix's ability breaks this rule. (The card that triggered this ability is also " +
                "still on the stack.)"
        )
        ruling("2004-12-01", "If there's only one imprinted sorcery card, nothing happens.")
        ruling(
            "2004-12-01",
            "You can't cast the copy if an effect prevents you from casting sorceries or from " +
                "casting that particular sorcery."
        )
        ruling("2004-12-01", "You can't cast the copy unless all of its targets can be chosen.")
        ruling(
            "2004-12-01",
            "If you don't want to cast the copy, you can choose not to; the copy ceases to exist " +
                "the next time state-based actions are checked."
        )
        ruling(
            "2004-12-01",
            "You don't pay the spell's mana cost. If a spell has X in its mana cost, X is 0. You do " +
                "pay any additional costs for that spell. You can't use any alternative costs."
        )
        ruling(
            "2004-12-01",
            "If the two imprinted sorcery cards have the same name and a card with that name is " +
                "cast, only one copy is created, not two."
        )
        ruling(
            "2004-10-04",
            "If this card leaves the battlefield while the ability to make a copy is on the stack, " +
                "the ability will still make a copy using the last-known-information rule."
        )
        ruling(
            "2004-10-04",
            "If the imprinted card leaves the exile zone while the ability to make a copy is on " +
                "the stack, then no copy will be made."
        )
    }
}
