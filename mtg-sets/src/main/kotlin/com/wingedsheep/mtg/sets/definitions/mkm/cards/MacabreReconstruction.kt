package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostGating
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Macabre Reconstruction — Murders at Karlov Manor #93
 * {3}{B} · Sorcery
 *
 * This spell costs {2} less to cast if a creature card was put into your graveyard from anywhere
 * this turn.
 * Return up to two target creature cards from your graveyard to your hand.
 *
 * The reduction is the Punishing Punch shape — a self-cast [ModifySpellCost] whose whole
 * modification is gated on a state condition rather than folded into the amount, so only the
 * generic {3} shrinks and the {B} pip survives.
 *
 * The gate is deliberately **turn history, not a graveyard scan**
 * ([Conditions.CreatureCardPutIntoYourGraveyardThisTurn], the creature-typed sibling of descend's
 * tracker): a creature milled and then reanimated earlier in the turn still pays off, and a
 * creature card that has been sitting in your graveyard since last turn does not. "From anywhere"
 * means any origin zone — battlefield, hand, library, stack — and tokens never qualify, since a
 * token isn't a card (CR 111.6). Only *your* graveyard counts.
 *
 * "Up to two target" is a single optional two-slot [TargetObject], so casting it with an empty
 * graveyard is legal and simply returns nothing; each surviving target is moved independently by
 * [ForEachTargetEffect], so one target becoming illegal doesn't cost you the other.
 */
val MacabreReconstruction = card("Macabre Reconstruction") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "This spell costs {2} less to cast if a creature card was put into your graveyard " +
        "from anywhere this turn.\n" +
        "Return up to two target creature cards from your graveyard to your hand."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGeneric(2),
            gating = CostGating.OnlyIf(Conditions.CreatureCardPutIntoYourGraveyardThisTurn()),
        )
    }

    spell {
        target = TargetObject(
            count = 2,
            optional = true,
            filter = TargetFilter.CreatureInYourGraveyard,
        )
        effect = ForEachTargetEffect(
            effects = listOf(Effects.Move(EffectTarget.ContextTarget(0), Zone.HAND)),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "93"
        artist = "Sam Guay"
        flavorText = "\"Eyewitness testimony, coming right up! Just let me finish regenerating " +
            "their eyes.\"\n—Nelo, Agency coroner"
        imageUri = "https://cards.scryfall.io/normal/front/a/b/abb6184c-e3d0-4275-b25b-95e4a64b26f3.jpg?1783912895"
    }
}
