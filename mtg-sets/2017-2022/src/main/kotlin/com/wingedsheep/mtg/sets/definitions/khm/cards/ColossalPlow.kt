package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.ManaExpiry

/**
 * Colossal Plow
 * {2}
 * Artifact — Vehicle
 * 6/3
 * Whenever this Vehicle attacks, add {W}{W}{W} and you gain 3 life. Until end of turn, you don't
 * lose this mana as steps and phases end.
 * Crew 6
 */
val ColossalPlow = card("Colossal Plow") {
    manaCost = "{2}"
    typeLine = "Artifact — Vehicle"
    power = 6
    toughness = 3
    oracleText = "Whenever this Vehicle attacks, add {W}{W}{W} and you gain 3 life. Until end of turn, you don't lose this mana as steps and phases end.\nCrew 6 (Tap any number of creatures you control with total power 6 or more: This Vehicle becomes an artifact creature until end of turn.)"

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.AddMana(Color.WHITE, 3, expiry = ManaExpiry.KEPT_UNTIL_END_OF_TURN) then
            Effects.GainLife(3)
    }

    keywordAbility(KeywordAbility.crew(6))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "236"
        artist = "Joe Slucher"
        imageUri = "https://cards.scryfall.io/normal/front/f/b/fba3fe29-067f-4c7e-8b3b-779f3b37712a.jpg?1783928186"
    }
}
