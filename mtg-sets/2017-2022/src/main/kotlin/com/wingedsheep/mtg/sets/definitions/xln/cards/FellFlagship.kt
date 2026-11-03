package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Fell Flagship
 * {3}
 * Artifact — Vehicle
 * 3/3
 * Pirates you control get +1/+0.
 * Whenever this Vehicle deals combat damage to a player, that player discards a card.
 * Crew 3
 *
 * "Pirates you control" names the subtype, not the card type, so the lord reaches Pirate
 * permanents (Corsair Captain's filter) — including the Flagship itself only if it somehow
 * became a Pirate. The discard is chosen by the damaged player.
 */
val FellFlagship = card("Fell Flagship") {
    manaCost = "{3}"
    typeLine = "Artifact — Vehicle"
    power = 3
    toughness = 3
    oracleText = "Pirates you control get +1/+0.\n" +
        "Whenever this Vehicle deals combat damage to a player, that player discards a card.\n" +
        "Crew 3 (Tap any number of creatures you control with total power 3 or more: This Vehicle becomes an artifact creature until end of turn.)"

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 0,
            filter = GroupFilter(GameObjectFilter.Permanent.withSubtype(Subtype.PIRATE).youControl())
        )
    }

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Patterns.Hand.discardCards(1, EffectTarget.PlayerRef(Player.TriggeringPlayer))
    }

    keywordAbility(KeywordAbility.crew(3))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "238"
        artist = "Titus Lunter"
        imageUri = "https://cards.scryfall.io/normal/front/2/7/2774bdb6-7a02-43c9-9e6f-75d0b09570ca.jpg?1783935703"
    }
}
