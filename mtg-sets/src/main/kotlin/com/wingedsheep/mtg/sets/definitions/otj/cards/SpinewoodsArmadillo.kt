package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.effects.WardCost

/**
 * Spinewoods Armadillo
 * {4}{G}{G}
 * Creature — Armadillo
 * 7/7
 *
 * Reach
 * Ward {3}
 * {1}{G}, Discard this card: Search your library for a basic land card or a Desert card,
 * reveal it, put it into your hand, then shuffle. You gain 3 life.
 *
 * The last ability functions from hand: its cost discards this card ([Costs.DiscardSelf]) and
 * it's activated from the hand zone ([activateFromZone] = [Zone.HAND]), so it reads as a
 * land-fetch you can use when this fatty is stranded in hand.
 */
val SpinewoodsArmadillo = card("Spinewoods Armadillo") {
    manaCost = "{4}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Armadillo"
    power = 7
    toughness = 7
    oracleText = "Reach\nWard {3} (Whenever this creature becomes the target of a spell or " +
        "ability an opponent controls, counter it unless that player pays {3}.)\n{1}{G}, Discard " +
        "this card: Search your library for a basic land card or a Desert card, reveal it, put it " +
        "into your hand, then shuffle. You gain 3 life."

    keywords(Keyword.REACH)
    keywordAbility(KeywordAbility.Ward(WardCost.Mana("{3}")))

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{G}"), Costs.DiscardSelf)
        activateFromZone = Zone.HAND
        effect = Effects.Composite(
            Patterns.Library.searchLibrary(
                filter = GameObjectFilter.BasicLand or GameObjectFilter.Land.withSubtype(Subtype.DESERT),
                destination = SearchDestination.HAND,
                reveal = true,
                shuffleAfter = true
            ),
            Effects.GainLife(3)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "182"
        artist = "Iris Compiet"
        imageUri = "https://cards.scryfall.io/normal/front/f/7/f79d63e7-a8c6-4750-91c9-c575a4d0561b.jpg?1712356001"
    }
}
