package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.Effects

/**
 * The Balrog, Flame of Udûn
 * {3}{B}{R}
 * Legendary Creature — Avatar Demon
 * 7/7
 *
 * Trample
 * When a legendary creature an opponent controls dies, put The Balrog on the bottom of its
 * owner's library.
 */
val TheBalrogFlameOfUdun = card("The Balrog, Flame of Udûn") {
    manaCost = "{3}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Legendary Creature — Avatar Demon"
    power = 7
    toughness = 7
    oracleText = "Trample\n" +
        "When a legendary creature an opponent controls dies, put The Balrog on the bottom of its owner's library."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.leavesBattlefield(
            filter = GameObjectFilter.Creature.legendary().opponentControls(),
            to = Zone.GRAVEYARD,
            binding = TriggerBinding.ANY
        )
        effect = Effects.Move(
            EffectTarget.Self,
            Zone.LIBRARY,
            placement = ZonePlacement.Bottom
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "297"
        artist = "John Tedrick"
        flavorText = "An evil of the Ancient World, both a shadow and a flame."
        imageUri = "https://cards.scryfall.io/normal/front/9/0/905e7424-2d0b-4877-a7dc-272ea9392ff0.jpg?1687424899"
    }
}
