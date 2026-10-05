package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Sage's Reverie
 * {3}{W}
 * Enchantment — Aura
 * Enchant creature
 * When this Aura enters, draw a card for each Aura you control that's attached to a creature.
 * Enchanted creature gets +1/+1 for each Aura you control that's attached to a creature.
 *
 * The count is any Aura you control whose host is a creature — not only Auras with enchant
 * creature — and it is taken as the trigger resolves, so it includes Sage's Reverie itself if it
 * is still attached then.
 */
val SagesReverie = card("Sage's Reverie") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "When this Aura enters, draw a card for each Aura you control that's attached to a creature.\n" +
        "Enchanted creature gets +1/+1 for each Aura you control that's attached to a creature."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    val aurasOnCreatures = DynamicAmounts.battlefield(
        Player.You,
        GameObjectFilter.Enchantment.withSubtype("Aura").attachedTo(GameObjectFilter.Creature),
    ).count()

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DrawCards(aurasOnCreatures)
    }

    staticAbility {
        ability = GrantDynamicStats(
            filter = GroupFilter.attachedCreature(),
            powerBonus = aurasOnCreatures,
            toughnessBonus = aurasOnCreatures,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "23"
        artist = "Jason Rainville"
        imageUri = "https://cards.scryfall.io/normal/front/0/f/0f22dd6f-807a-48a7-bc69-29aaec7012de.jpg?1783938712"
        ruling(
            "2014-11-24",
            "Count the number of Auras you control attached to creatures as the enters-the-battlefield " +
                "ability resolves to determine how many cards to draw. This will include Sage's Reverie as " +
                "long as it's still on the battlefield at that time.",
        )
        ruling(
            "2014-11-24",
            "An Aura doesn't necessarily need the enchant creature ability for the abilities of Sage's " +
                "Reverie to count it. For example, an Aura with enchant permanent that's attached to a " +
                "creature will count.",
        )
    }
}
