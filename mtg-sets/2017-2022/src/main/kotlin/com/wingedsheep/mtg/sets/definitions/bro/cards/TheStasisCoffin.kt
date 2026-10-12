package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * The Stasis Coffin
 * {3}
 * Legendary Artifact
 * {2}, {T}, Exile The Stasis Coffin: You gain protection from everything until your next turn.
 *
 * The protection is The One Ring's spelling: [Effects.GrantPlayerProtection] defaults to
 * protection from everything, until your next turn, for the ability's controller.
 */
val TheStasisCoffin = card("The Stasis Coffin") {
    manaCost = "{3}"
    typeLine = "Legendary Artifact"
    oracleText = "{2}, {T}, Exile The Stasis Coffin: You gain protection from everything until your next turn."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap, Costs.ExileSelf)
        effect = Effects.GrantPlayerProtection()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "245"
        artist = "Artur Nakhodkin"
        flavorText = "Tawnos built it to be absolutely impermeable to any force inside. Luckily it worked for any force outside as well."
        imageUri = "https://cards.scryfall.io/normal/front/4/5/45537ac3-fc61-4253-a782-7f3e436b2aa6.jpg?1783920013"
    }
}
