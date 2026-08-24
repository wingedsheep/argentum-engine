package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Rishadan Port
 *
 * Land
 *
 * {T}: Add {C}.
 * {1}, {T}: Tap target land.
 *
 * [Targets.Land] carries no controller predicate, matching the Oracle wording — the Port can tap
 * any land, including one of yours. The tapping is [Effects.Tap] on the bound target; the `{T}` in
 * the cost is the Port tapping itself. The second ability targets, so it is not a mana ability.
 */
val RishadanPort = card("Rishadan Port") {
    colorIdentity = ""
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n" +
        "{1}, {T}: Tap target land."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        val land = target("target", Targets.Land)
        effect = Effects.Tap(land)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "324"
        artist = "Jerry Tiritilli"
        flavorText = "Rishada is the gateway to free trade—but the key will cost you."
        imageUri = "https://cards.scryfall.io/normal/front/4/7/477a1f53-5cdf-4b45-b584-2e36b31a3fdb.jpg"
    }
}
