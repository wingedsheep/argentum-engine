package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameEvent.SpellCastEvent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Tanufel Rimespeaker
 * {3}{U}
 * Creature — Elemental Wizard
 * 2/4
 * Whenever you cast a spell with mana value 4 or greater, draw a card.
 */
val TanufelRimespeaker = card("Tanufel Rimespeaker") {
    manaCost = "{3}{U}"
    typeLine = "Creature — Elemental Wizard"
    power = 2
    toughness = 4
    oracleText = "Whenever you cast a spell with mana value 4 or greater, draw a card."

    triggeredAbility {
        trigger = TriggerSpec(
            SpellCastEvent(
                spellFilter = GameObjectFilter.Any.manaValueAtLeast(4),
                player = Player.You
            ),
            TriggerBinding.ANY
        )
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "77"
        artist = "Lauren K. Cannon"
        flavorText = "\"A single snowflake can trigger an avalanche. A single insight can do the same.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/3/b357022c-7cd5-4e82-a183-7144f5a84102.jpg?1767871796"
    }
}
