package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Judge's Familiar — Return to Ravnica #218
 * {W/U} · Creature — Bird · 1/1
 *
 * Flying
 * Sacrifice this creature: Counter target instant or sorcery spell unless its controller pays {1}.
 */
val JudgesFamiliar = card("Judge's Familiar") {
    manaCost = "{W/U}"
    colorIdentity = "WU"
    typeLine = "Creature — Bird"
    oracleText = "Flying\n" +
        "Sacrifice this creature: Counter target instant or sorcery spell unless its controller pays {1}."
    power = 1
    toughness = 1

    keywords(Keyword.FLYING)

    activatedAbility {
        target(TargetFilter.InstantOrSorcerySpellOnStack)
        cost = Costs.SacrificeSelf
        effect = Effects.CounterUnlessPays("{1}")
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "218"
        artist = "Jack Wang"
        flavorText = "\"It misses nothing and it has no sense of humor.\"\n—Barvisa, courtroom scribe"
        imageUri = "https://cards.scryfall.io/normal/front/0/f/0fc51899-3970-416b-b7de-fadbc9678955.jpg?1783940327"
    }
}
