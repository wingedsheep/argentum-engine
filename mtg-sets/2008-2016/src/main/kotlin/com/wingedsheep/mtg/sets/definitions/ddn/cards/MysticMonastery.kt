package com.wingedsheep.mtg.sets.definitions.ddn.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Mystic Monastery
 * Land
 * This land enters tapped.
 * {T}: Add {U}, {R}, or {W}.
 */
val MysticMonastery = card("Mystic Monastery") {
    typeLine = "Land"
    colorIdentity = "WUR"
    oracleText = "This land enters tapped.\n{T}: Add {U}, {R}, or {W}."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.BLUE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.RED)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.WHITE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "73"
        artist = "Florian de Gesincourt"
        flavorText = "When asked how many paths reach enlightenment, the monk kicked a heap of sand. \"Count,\" he smiled, \"and then find more grains.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/8/58fd5f88-2739-493e-9232-610f3a4645c3.jpg?1783939103"
    }
}
