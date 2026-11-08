package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Stromkirk Condemned
 * {B}{B}
 * Creature — Vampire Horror
 * 2/2
 * Discard a card: Vampires you control get +1/+1 until end of turn. Activate only once each turn.
 *
 * "Vampires you control" is a bare tribal noun — every Vampire *permanent* you control.
 */
val StromkirkCondemned = card("Stromkirk Condemned") {
    manaCost = "{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire Horror"
    oracleText = "Discard a card: Vampires you control get +1/+1 until end of turn. Activate only once each turn."
    power = 2
    toughness = 2

    activatedAbility {
        cost = Costs.DiscardCard
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Permanent.withSubtype(Subtype.VAMPIRE).youControl()),
            Effects.ModifyStats(1, 1, EffectTarget.IterationEntity)
        )
        restrictions = listOf(ActivationRestriction.OncePerTurn)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "106"
        artist = "Magali Villeneuve"
        flavorText = "\"Blood from the vein is the finest vintage to accompany a feast of the mind.\"\n" +
            "—Runo Stromkirk"
        imageUri = "https://cards.scryfall.io/normal/front/c/4/c4a8403e-bf4c-4aae-9102-188f49c61ddf.jpg"
    }
}
