package com.wingedsheep.mtg.sets.definitions.atq.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Staff of Zegon
 * {4}
 * Artifact
 * {3}, {T}: Target creature gets -2/-0 until end of turn.
 */
val StaffOfZegon = card("Staff of Zegon") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{3}, {T}: Target creature gets -2/-0 until end of turn."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}"), Costs.Tap)
        val t = target("target creature", Targets.Creature)
        effect = Effects.ModifyStats(-2, 0, t)
        description = "{3}, {T}: Target creature gets -2/-0 until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "65"
        artist = "Mark Poole"
        flavorText = "Though Mishra was impressed by the staves Ashnod had created for Zegon's defense, he understood they only hinted at her full potential."
        imageUri = "https://cards.scryfall.io/normal/front/a/6/a6bf858d-bba9-4a16-9045-55384b1de633.jpg?1562930132"
    }
}
