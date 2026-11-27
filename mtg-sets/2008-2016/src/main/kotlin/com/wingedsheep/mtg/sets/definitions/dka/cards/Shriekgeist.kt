package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Shriekgeist
 * {1}{U}
 * Creature — Spirit
 * 1/1
 *
 * Flying
 * Whenever this creature deals combat damage to a player, that player mills two cards.
 */
val Shriekgeist = card("Shriekgeist") {
    manaCost = "{1}{U}"
    typeLine = "Creature — Spirit"
    power = 1
    toughness = 1
    oracleText = "Flying\nWhenever this creature deals combat damage to a player, that player mills two cards."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Patterns.Library.mill(2, EffectTarget.PlayerRef(Player.TriggeringPlayer))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "49"
        artist = "Raymond Swanland"
        flavorText = "Wounds of the flesh are easy to heal. Wounds of the mind may never be undone."
        imageUri = "https://cards.scryfall.io/normal/front/4/3/435c5218-46b3-456a-aedf-d9586a4bd0a3.jpg?1783940836"
    }
}
