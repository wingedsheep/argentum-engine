package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.unearth
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Mask of the Jadecrafter — The Brothers' War #201
 * {2} · Artifact · Uncommon
 *
 * {X}, {T}, Sacrifice this artifact: Create an X/X colorless Golem artifact creature token.
 * Activate only as a sorcery.
 * Unearth {2}{G}
 *
 * The token's size is read from the ability's X at resolution and fixed then (`dynamicPower`
 * snapshots into the token's base P/T).
 */
val MaskOfTheJadecrafter = card("Mask of the Jadecrafter") {
    manaCost = "{2}"
    colorIdentity = "G"
    typeLine = "Artifact"
    oracleText = "{X}, {T}, Sacrifice this artifact: Create an X/X colorless Golem artifact creature token. " +
        "Activate only as a sorcery.\n" +
        "Unearth {2}{G} ({2}{G}: Return this card from your graveyard to the battlefield. Exile it at the " +
        "beginning of the next end step or if it would leave the battlefield. Unearth only as a sorcery.)"

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{X}"), Costs.Tap, Costs.SacrificeSelf)
        effect = Effects.CreateToken(
            power = 0,
            toughness = 0,
            creatureTypes = setOf("Golem"),
            artifactToken = true,
            dynamicPower = DynamicAmounts.xValue(),
            dynamicToughness = DynamicAmounts.xValue(),
            imageUri = "https://cards.scryfall.io/normal/front/4/7/474c8cfe-71ce-46c7-8cf4-dfeae3c0e73b.jpg?1783919909",
        )
        timing = TimingRule.SorcerySpeed
        description = "{X}, {T}, Sacrifice this artifact: Create an X/X colorless Golem artifact creature token. " +
            "Activate only as a sorcery."
    }

    unearth("{2}{G}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "201"
        artist = "Alessandra Pisano"
        imageUri = "https://cards.scryfall.io/normal/front/5/d/5db83275-bfd2-42c2-8d0f-e8a9b4f60e78.jpg?1783920038"
        ruling("2022-10-14", "If a permanent returned to the battlefield with unearth would leave the battlefield for any reason, it's exiled instead—unless the spell or ability that's causing the permanent to leave the battlefield is actually trying to exile it! In that case, it succeeds at exiling it.")
    }
}
