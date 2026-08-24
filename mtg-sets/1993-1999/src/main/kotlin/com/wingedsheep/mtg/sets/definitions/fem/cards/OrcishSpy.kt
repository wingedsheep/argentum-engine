package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetPlayer
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Orcish Spy
 * {R}
 * Creature — Orc Rogue
 * 1/1
 * {T}: Look at the top three cards of target player's library.
 *
 * A pure information effect, and the whole ability is the gather: [GatherCardsEffect] only *reads*
 * the top of the library — it never removes the cards — so there is nothing to put back. Following
 * it with a move to the top would relocate each card individually, and placing on top prepends, so
 * the three would come back reversed: a "look" that quietly reorders the target's next three draws.
 */
val OrcishSpy = card("Orcish Spy") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Creature — Orc Rogue"
    oracleText = "{T}: Look at the top three cards of target player's library."
    power = 1
    toughness = 1

    activatedAbility {
        cost = Costs.Tap
        val t = target("target player", TargetPlayer())
        effect = GatherCardsEffect(
            source = CardSource.TopOfLibrary(DynamicAmount.Fixed(3), Player.TargetPlayer),
            storeAs = "spied"
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "61a"
        artist = "Susan Van Camp"
        flavorText = "\"Yeah, they're ugly, they desert in droves, and their personal habits are enough to make you sick. But I'll say this for Orcs: they make great spies.\"\n—Ivra Jursdotter"
        imageUri = "https://cards.scryfall.io/normal/front/c/d/cd3890d1-563d-4519-ab8c-913031d71918.jpg?1783947892"
    }
}
