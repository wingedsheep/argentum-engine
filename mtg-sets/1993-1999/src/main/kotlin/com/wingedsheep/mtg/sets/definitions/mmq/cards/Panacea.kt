package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Panacea
 * {4}
 * Artifact
 *
 * The `{X}{X}` shield: the announced X is the *cost*, so the prevented amount is
 * [DynamicAmount.XValue] and the doubled symbol is nothing but the mana string.
 */
val Panacea = card("Panacea") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{X}{X}, {T}: Prevent the next X damage that would be dealt to any target this turn."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{X}{X}"), Costs.Tap)
        val t = target("target", Targets.Any)
        effect = Effects.PreventNextDamage(DynamicAmount.XValue, t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "308"
        artist = "Donato Giancola"
        flavorText = "A drop numbs the tongue. A sip sends the heart racing. A draught reforges the body."
        imageUri = "https://cards.scryfall.io/normal/front/8/9/89414770-2a19-4baf-9b18-76104b7b0b9a.jpg"
    }
}
