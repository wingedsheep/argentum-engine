package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Azorius Cluestone
 * {3}
 * Artifact
 *
 * {T}: Add {W} or {U}.
 * {W}{U}, {T}, Sacrifice this artifact: Draw a card.
 */
val AzoriusCluestone = card("Azorius Cluestone") {
    manaCost = "{3}"
    colorIdentity = "WU"
    typeLine = "Artifact"
    oracleText = "{T}: Add {W} or {U}.\n" +
        "{W}{U}, {T}, Sacrifice this artifact: Draw a card."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.WHITE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLUE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{W}{U}"),
            Costs.Tap,
            Costs.SacrificeSelf,
        )
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "136"
        artist = "Raoul Vitale"
        flavorText = "Its three sides represent the Sova, judges and arbitrators; the Jelenn, scribes and elocutors; and the Lyev, lawmages and enforcers."
        imageUri = "https://cards.scryfall.io/normal/front/0/9/09eeb301-bc28-4515-ad69-0b1b5164a5bc.jpg"
    }
}
