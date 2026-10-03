package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.SkipUntapStep
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect

val Stasis = card("Stasis") {
    manaCost = "{1}{U}"
    typeLine = "Enchantment"
    oracleText = "Players skip their untap steps.\nAt the beginning of your upkeep, sacrifice this enchantment unless you pay {U}."
    staticAbility { ability = SkipUntapStep() }
    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.PayOrSuffer(cost = Costs.pay.Mana("{U}"), suffer = SacrificeSelfEffect)
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "80"
        artist = "Fay Jones"
        imageUri = "https://cards.scryfall.io/normal/front/b/6/b6cef408-5b4b-49f6-9531-be544815b93f.jpg?1783948701"
        ruling("2004-10-04", "Does not prevent cards from being untapped outside the untap step.")
        ruling("2004-10-04", "Since there is no untap step, Phasing in/out won't happen.")
    }
}
