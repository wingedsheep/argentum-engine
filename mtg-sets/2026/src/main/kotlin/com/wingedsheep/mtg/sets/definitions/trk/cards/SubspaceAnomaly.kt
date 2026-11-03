package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Subspace Anomaly
 * Land
 *
 * {T}: Add {C}.
 * {4}{W}, {T}, Sacrifice this land: Create two 1/1 red Officer creature tokens. Create a tapped
 * Planet land token named New Planet with "{T}: Add one mana of any color."
 */
val SubspaceAnomaly = card("Subspace Anomaly") {
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n{4}{W}, {T}, Sacrifice this land: Create two 1/1 red Officer creature tokens. Create a tapped Planet land token named New Planet with \"{T}: Add one mana of any color.\""
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}{W}"), Costs.Tap, Costs.SacrificeSelf)
        effect = Effects.CreateToken(
            power = 1, toughness = 1,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Officer"),
            count = 2,
            imageUri = "https://cards.scryfall.io/normal/front/d/6/d676cbef-8e31-4ae6-8dfe-6b84160f71e0.jpg?1793363893"
        ) then Effects.CreateNewPlanet()
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "300"
        artist = "Bruce Brenneise"
        flavorText = "\"Can't say why, sir, but that argon's giving me the heebie-jeebies.\"\n—Conn Officer Fredericks"
        imageUri = "https://cards.scryfall.io/normal/front/e/d/ed62600f-84ae-421e-970a-fd0682c8f280.jpg?1793108348"
    }
}
