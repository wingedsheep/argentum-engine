package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Heir of the Wilds
 * {1}{G}
 * Creature — Human Warrior
 * 2/2
 * Deathtouch
 * Ferocious — Whenever Heir of the Wilds attacks, if you control a creature with power 4 or greater,
 * Heir of the Wilds gets +1/+1 until end of turn.
 */
val HeirOfTheWilds = card("Heir of the Wilds") {
    manaCost = "{1}{G}"
    typeLine = "Creature — Human Warrior"
    power = 2
    toughness = 2
    oracleText = "Deathtouch\nFerocious — Whenever Heir of the Wilds attacks, if you control a creature with power 4 or greater, Heir of the Wilds gets +1/+1 until end of turn."

    keywords(Keyword.DEATHTOUCH)

    triggeredAbility {
        trigger = Triggers.Attacks
        triggerCondition = Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Creature.powerAtLeast(4))
        effect = ConditionalEffect(
            condition = Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Creature.powerAtLeast(4)),
            effect = Effects.ModifyStats(1, 1)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "134"
        artist = "Winona Nelson"
        flavorText = "In the high caves of the Qal Sisma mountains, young hunters quest to hear the echoes of their fierce ancestors."
        imageUri = "https://cards.scryfall.io/normal/front/0/9/0995e041-fe90-4459-8c70-fd9851ecf830.jpg?1562782282"
    }
}
