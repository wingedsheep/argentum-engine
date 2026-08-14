package com.wingedsheep.mtg.sets.definitions.atq.cards

import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ReduceActivatedAbilityCost
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Power Artifact
 * {U}{U}
 * Enchantment — Aura
 * Enchant artifact
 * Enchanted artifact's activated abilities cost {2} less to activate. This effect can't reduce
 * the mana in that cost to less than one mana.
 *
 * The activated-ability cost-reduction static [ReduceActivatedAbilityCost], keyed to the enchanted
 * permanent via [GroupFilter.attachedCreature] (`Scope.AttachedTo`). Reduces the generic portion of
 * the enchanted artifact's activated-ability costs by {2}, with `manaFloor = 1` so the mana in the
 * cost can't drop below one mana total: a `{1}` ability stays `{1}`, a `{3}` ability becomes `{1}`,
 * and a `{2}{U}` ability becomes `{U}`. Colored pips and non-mana costs ({T}, sacrifice) are
 * untouched.
 */
val PowerArtifact = card("Power Artifact") {
    manaCost = "{U}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant artifact\n" +
        "Enchanted artifact's activated abilities cost {2} less to activate. This effect can't " +
        "reduce the mana in that cost to less than one mana."
    auraTarget = Targets.Artifact

    staticAbility {
        ability = ReduceActivatedAbilityCost(
            filter = GroupFilter.attachedCreature(),
            amount = DynamicAmount.Fixed(2),
            manaFloor = 1
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "11"
        artist = "Douglas Shuler"
        imageUri = "https://cards.scryfall.io/normal/front/e/4/e48bc89e-6da5-43da-b4e0-60d5f850199c.jpg?1562943281"
    }
}
