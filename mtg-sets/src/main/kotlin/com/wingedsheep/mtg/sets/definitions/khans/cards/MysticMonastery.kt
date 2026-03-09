package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.AddManaEffect

/**
 * Mystic Monastery
 * Land
 * This land enters tapped.
 * {T}: Add {U}, {R}, or {W}.
 */
val MysticMonastery = card("Mystic Monastery") {
    typeLine = "Land"
    oracleText = "This land enters tapped.\n{T}: Add {U}, {R}, or {W}."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = AbilityCost.Tap
        effect = AddManaEffect(Color.BLUE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = AddManaEffect(Color.RED)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = AddManaEffect(Color.WHITE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "236"
        artist = "Florian de Gesincourt"
        flavorText = "When asked how many paths reach enlightenment, the monk kicked a heap of sand. \"Count,\" he smiled, \"and then find more grains.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/a/bae51d77-e06b-4e5a-9543-a17dd0b2a333.jpg?1562792634"
    }
}
