package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.GainLifeEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Traveling Minister
 * {W}
 * Creature — Human Cleric
 * 1/1
 * {T}: Target creature gets +1/+0 until end of turn. You gain 1 life. Activate only as a sorcery.
 */
val TravelingMinister = card("Traveling Minister") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Cleric"
    oracleText = "{T}: Target creature gets +1/+0 until end of turn. You gain 1 life. Activate only as a sorcery."
    power = 1
    toughness = 1
    activatedAbility {
        cost = Costs.Tap
        val t = target("target", TargetCreature(filter = TargetFilter.Creature))
        effect = Effects.Composite(
            Effects.ModifyStats(1, 0, t),
            GainLifeEffect(1)
        )
        timing = TimingRule.SorcerySpeed
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "39"
        artist = "Slawomir Maniak"
        flavorText = "Priests of the Sigardian Sect travel to the farthest reaches to spread hope and healing, even in the darkest times."
        imageUri = "https://cards.scryfall.io/normal/front/e/6/e672a05c-5f1a-4aa6-9398-e33df01c7c96.jpg?1782703166"
    }
}
