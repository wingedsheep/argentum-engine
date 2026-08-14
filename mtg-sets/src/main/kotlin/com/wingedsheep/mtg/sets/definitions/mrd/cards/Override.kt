package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Override — Mirrodin #45
 * {2}{U} · Instant
 *
 * Counter target spell unless its controller pays {1} for each artifact you control.
 *
 * "You" is Override's controller, so the tax counts *your* artifacts, not the spell controller's.
 * [Effects.CounterUnlessDynamicPays] evaluates the amount when the counter resolves — artifacts
 * that entered or left in response are counted as they stand at that moment (CR 608.2h: the
 * information is determined once, when the effect is applied), and with no artifacts the spell
 * resolves for free rather than being countered.
 */
val Override = card("Override") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target spell unless its controller pays {1} for each artifact you control."

    spell {
        target = Targets.Spell
        effect = Effects.CounterUnlessDynamicPays(
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.Artifact).count()
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "45"
        artist = "Hugh Jamieson"
        flavorText = "\"The Knowledge Pool has all the answers—especially 'No.'\""
        imageUri = "https://cards.scryfall.io/normal/front/3/5/35964fa6-800d-41d6-9f82-fb9c87deee56.jpg?1783944553"
    }
}
