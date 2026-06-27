package com.wingedsheep.mtg.sets.definitions.tmt.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalManaOnSourceTap
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Groundchuck & Dirtbag
 * {4}{G}{G}
 * Legendary Creature — Ox Mole Mutant
 * 8/8
 *
 * Trample
 * Whenever you tap a land for mana, add {G}.
 */
val GroundchuckAndDirtbag = card("Groundchuck & Dirtbag") {
    manaCost = "{4}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Ox Mole Mutant"
    oracleText = "Trample\nWhenever you tap a land for mana, add {G}."
    power = 8
    toughness = 8

    keywords(Keyword.TRAMPLE)

    // "Whenever you tap a land for mana, add {G}." is a triggered MANA ability (CR 605.1b): it
    // resolves immediately without using the stack, so the {G} is available for the same payment.
    // AdditionalManaOnSourceTap is the engine's immediate off-stack resolver for exactly this
    // shape (see Badgermole Cub, "...tap a creature for mana..."). The generic landTappedForMana
    // triggered-ability path is NOT wired to off-stack mana resolution, so it adds no mana.
    // The "you tap" wording is the filter's youControl predicate: only the static's controller can
    // activate a land they control as a mana ability.
    staticAbility {
        ability = AdditionalManaOnSourceTap(
            sourceFilter = GameObjectFilter.Land.youControl(),
            color = Color.GREEN
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "115"
        artist = "Nicholas Gregory"
        flavorText = "\"Whatever my lil' man D.B. here can't dig under, I can just about bust through. Ain't no one caging us ever again.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/5/1563592e-0f21-4488-a3ec-ca766386e423.jpg?1769006182"
    }
}
