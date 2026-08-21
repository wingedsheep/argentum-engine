package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Rune-Cervin Rider
 * {3}{W}
 * Creature — Elf Knight
 * 2 / 2
 *
 * Flying
 * {G/W}{G/W}: This creature gets +1/+1 until end of turn.
 *
 * - Both hybrid symbols stay in the activation cost as written; each `{G/W}` is independently
 *   payable with {G} or {W}.
 * - No target: "this creature" is the source, so the pump uses [EffectTarget.Self].
 */
val RuneCervinRider = card("Rune-Cervin Rider") {
    manaCost = "{3}{W}"
    typeLine = "Creature — Elf Knight"
    power = 2
    toughness = 2
    oracleText = "Flying\n" +
        "{G/W}{G/W}: This creature gets +1/+1 until end of turn."

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Mana("{G/W}{G/W}")
        effect = Effects.ModifyStats(1, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "20"
        artist = "Dan Murayama Scott"
        flavorText = "Things of beauty are in constant peril. The riders whisk them to safety, ahead of the encroaching darkness."
        imageUri = "https://cards.scryfall.io/normal/front/9/d/9d9574f6-40b3-4fe2-950c-234bc358ecf6.jpg?1783942766"
    }
}
