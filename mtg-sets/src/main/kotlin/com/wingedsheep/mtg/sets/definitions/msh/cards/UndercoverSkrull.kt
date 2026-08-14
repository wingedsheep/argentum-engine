package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CompositeStaticAbility
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.IsAllCreatureTypes
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Undercover Skrull — Marvel Super Heroes #194 (common)
 * {1}{G} · Creature — Skrull Shapeshifter Villain · 1/1
 *
 * As long as there are two or more creature cards in your graveyard, this creature gets +2/+2 and
 * is all creature types.
 * {T}: Add one mana of any color.
 *
 * Implementation notes:
 * - The threshold clause is **one** printed ability whose single continuous effect spans two
 *   Rule 613 layers — Layer 4 (type-changing, [IsAllCreatureTypes]) and Layer 7c (P/T modification,
 *   [ModifyStats]) — so the two parts are bundled into a [CompositeStaticAbility] rather than
 *   written as two `staticAbility { }` blocks. Per CR 613.6 a multi-layer effect locks in the set
 *   of objects it affects when it first applies and keeps applying in every later layer; the bundle
 *   is what tags the parts as one grouped effect so the engine does that instead of treating each
 *   layer as an independent effect.
 * - The whole bundle is wrapped in a [ConditionalStaticAbility] so both layers are gated on the
 *   same "as long as" condition, which the projector re-evaluates as cards enter and leave the
 *   graveyard. `Conditions.CardsInGraveyardMatchingAtLeast(2, Creature)` counts creature *cards* in
 *   **your** graveyard only, matching the printed wording.
 * - [IsAllCreatureTypes] adds every creature type without granting changeling, which is right here:
 *   the card says "is all creature types", not "changeling", so no Changeling badge should show.
 * - The mana ability is the canonical `{T}: Add one mana of any color` — [Effects.AddManaOfChoice]
 *   with its default `ManaColorSet.AnyColor`, flagged as a mana ability so it never uses the stack.
 */
val UndercoverSkrull = card("Undercover Skrull") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Skrull Shapeshifter Villain"
    power = 1
    toughness = 1
    oracleText = "As long as there are two or more creature cards in your graveyard, this " +
        "creature gets +2/+2 and is all creature types.\n" +
        "{T}: Add one mana of any color."

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = CompositeStaticAbility(
                listOf(
                    ModifyStats(2, 2, GroupFilter.source()),
                    IsAllCreatureTypes(GroupFilter.source()),
                )
            ),
            condition = Conditions.CardsInGraveyardMatchingAtLeast(2, GameObjectFilter.Creature),
        )
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddManaOfChoice()
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "194"
        artist = "Svetlin Velinov"
        flavorText = "They're already here."
        imageUri = "https://cards.scryfall.io/normal/front/9/0/90b9a3c0-3fd6-403e-9d6c-201342fea0ad.jpg?1783902909"
    }
}
