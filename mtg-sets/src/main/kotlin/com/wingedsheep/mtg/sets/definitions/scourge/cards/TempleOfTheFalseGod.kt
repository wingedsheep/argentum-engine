package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.AddColorlessManaEffect

/**
 * Temple of the False God
 * Land
 * {T}: Add {C}{C}. Activate only if you control five or more lands.
 */
val TempleOfTheFalseGod = card("Temple of the False God") {
    typeLine = "Land"
    oracleText = "{T}: Add {C}{C}. Activate only if you control five or more lands."

    activatedAbility {
        cost = AbilityCost.Tap
        effect = AddColorlessManaEffect(2)
        manaAbility = true
        timing = TimingRule.ManaAbility
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(Conditions.ControlLandsAtLeast(5))
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "143"
        artist = "Brian Snõddy"
        flavorText = "\"Those who bring nothing to the temple take nothing away.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/7/d7036f51-10a6-4036-8650-9bc12d2a55cb.jpg?1562535240"
    }
}
