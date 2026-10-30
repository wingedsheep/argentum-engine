package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Perimeter Sergeant
 * {2}{W}
 * Creature — Human Soldier
 * 3/2
 * Whenever this creature attacks, other Humans you control get +1/+0 until end of turn.
 *
 * Bolt Hound's shape, narrowed to Humans ("Humans" is a permanent subtype filter, not creatures).
 */
val PerimeterSergeant = card("Perimeter Sergeant") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 3
    toughness = 2
    oracleText = "Whenever this creature attacks, other Humans you control get +1/+0 until end of turn."

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Patterns.Group.modifyStatsForAll(
            power = 1,
            toughness = 0,
            filter = GroupFilter(GameObjectFilter.Permanent.withSubtype(Subtype.HUMAN).youControl(), excludeSelf = true),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "27"
        artist = "Stepan Alekseev"
        flavorText = "\"Get in there, soldier! I want a pelt on my wall before lunch!\""
        imageUri = "https://cards.scryfall.io/normal/front/2/a/2a174587-d964-48b3-8f69-fa576c1b8bba.jpg"
    }
}
