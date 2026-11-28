package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mindshrieker
 * {1}{U}
 * Creature — Spirit Bird
 * 1/1
 *
 * Flying
 * {2}: Target player mills a card. This creature gets +X/+X until end of turn, where X is the
 * milled card's mana value.
 *
 * The pipeline `mill` (an `isMill = true` gather of the target player's top card, moved to their
 * graveyard) feeds its collection to `manaValueOf`, as Stormchaser Chimera does with a revealed
 * card. An empty library mills nothing, so X is 0.
 */
val Mindshrieker = card("Mindshrieker") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Spirit Bird"
    power = 1
    toughness = 1
    oracleText = "Flying\n" +
        "{2}: Target player mills a card. This creature gets +X/+X until end of turn, where X is the milled card's mana value."

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Mana("{2}")
        target(Targets.Player)
        effect = Effects.Pipeline {
            val milled = mill(1, Player.TargetPlayer)
            val x = DynamicAmounts.manaValueOf(milled)
            run(Effects.ModifyStats(x, x, EffectTarget.Self))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "67"
        artist = "Dave Kendall"
        imageUri = "https://cards.scryfall.io/normal/front/a/b/ab1e52af-6746-4ec6-afbf-008594c874f8.jpg?1783940971"
    }
}
