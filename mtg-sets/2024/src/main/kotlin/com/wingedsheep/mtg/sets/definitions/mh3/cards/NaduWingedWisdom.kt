package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Nadu, Winged Wisdom {1}{G}{U}
 * Legendary Creature — Bird Wizard
 * 3/4
 * Flying
 * Creatures you control have "Whenever this creature becomes the target of a spell or ability,
 * reveal the top card of your library. If it's a land card, put it onto the battlefield.
 * Otherwise, put it into your hand. This ability triggers only twice each turn."
 *
 * The granted ability counts per creature and per Nadu (rulings): each creature's copy triggers
 * twice each turn, and a second Nadu grants a second, separately counted ability.
 */
val NaduWingedWisdom = card("Nadu, Winged Wisdom") {
    manaCost = "{1}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Legendary Creature — Bird Wizard"
    power = 3
    toughness = 4
    oracleText = "Flying\n" +
        "Creatures you control have \"Whenever this creature becomes the target of a spell or ability, " +
        "reveal the top card of your library. If it's a land card, put it onto the battlefield. " +
        "Otherwise, put it into your hand. This ability triggers only twice each turn.\""

    keywords(Keyword.FLYING)

    staticAbility {
        ability = GrantTriggeredAbility(
            ability = grantedTriggeredAbility {
                trigger = Triggers.self.becomesTarget()
                effect = Effects.Pipeline {
                    val top = gather(CardSource.TopOfLibrary(1))
                    reveal(top, fromZone = Zone.LIBRARY)
                    val (lands, nonlands) = filterSplit(top, GameObjectFilter.Land)
                    move(lands, CardDestination.ToZone(Zone.BATTLEFIELD))
                    toHand(nonlands)
                }
                triggersPerTurn = 2
                description = "Whenever this creature becomes the target of a spell or ability, reveal the " +
                    "top card of your library. If it's a land card, put it onto the battlefield. Otherwise, " +
                    "put it into your hand. This ability triggers only twice each turn."
            },
            filter = GroupFilter.AllCreaturesYouControl,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "193"
        artist = "Daren Bader"
        imageUri = "https://cards.scryfall.io/normal/front/9/4/94b67489-5eb0-4406-9bf3-27e50dc632eb.jpg?1790212106"
    }
}
