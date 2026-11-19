package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Stormchaser Chimera
 * {2}{U}{R}
 * Creature — Chimera
 * 2/3
 *
 * Flying
 * {2}{U}{R}: Scry 1, then reveal the top card of your library. This creature gets +X/+0 until end
 * of turn, where X is that card's mana value.
 *
 * The revealed card stays on top of the library (revealing doesn't move it).
 */
val StormchaserChimera = card("Stormchaser Chimera") {
    manaCost = "{2}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Creature — Chimera"
    power = 2
    toughness = 3
    oracleText = "Flying\n" +
        "{2}{U}{R}: Scry 1, then reveal the top card of your library. This creature gets +X/+0 until end of turn, where X is that card's mana value. (To scry 1, look at the top card of your library, then you may put that card on the bottom.)"

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Mana("{2}{U}{R}")
        effect = Effects.Scry(1) then Effects.Pipeline {
            val top = gather(CardSource.TopOfLibrary(1, Player.You))
            reveal(top)
            run(Effects.ModifyStats(DynamicAmounts.manaValueOf(top), DynamicAmounts.fixed(0), EffectTarget.Self))
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "156"
        artist = "Greg Staples"
        imageUri = "https://cards.scryfall.io/normal/front/d/b/db6621dd-7220-4b53-a30f-7dd05b2e0134.jpg?1783939402"
    }
}
