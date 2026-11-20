package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Karametra's Acolyte
 * {3}{G}
 * Creature — Human Druid
 * 1/4
 * {T}: Add an amount of {G} equal to your devotion to green.
 *
 * The Acolyte's own {G} counts toward the devotion while it's on the battlefield, so the ability
 * always adds at least one {G}.
 */
val KarametrasAcolyte = card("Karametra's Acolyte") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Druid"
    power = 1
    toughness = 4
    oracleText = "{T}: Add an amount of {G} equal to your devotion to green. " +
        "(Each {G} in the mana costs of permanents you control counts toward your devotion to green.)"

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.GREEN, DynamicAmounts.devotionTo(Color.GREEN))
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "160"
        artist = "Chase Stone"
        flavorText = "\"The wilds are a garden tended by divine hands.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/7/374a1b7c-ce11-4734-aff2-b0bd00857fad.jpg"
    }
}
