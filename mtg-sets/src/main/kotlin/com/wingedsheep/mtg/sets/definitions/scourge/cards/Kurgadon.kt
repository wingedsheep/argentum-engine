package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.events.SpellTypeFilter
import com.wingedsheep.sdk.scripting.GameEvent.SpellCastEvent
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Kurgadon
 * {4}{G}
 * Creature — Beast
 * 3/3
 * Whenever you cast a creature spell with mana value 6 or greater,
 * put three +1/+1 counters on Kurgadon.
 */
val Kurgadon = card("Kurgadon") {
    manaCost = "{4}{G}"
    typeLine = "Creature — Beast"
    power = 3
    toughness = 3
    oracleText = "Whenever you cast a creature spell with mana value 6 or greater, put three +1/+1 counters on Kurgadon."

    triggeredAbility {
        trigger = TriggerSpec(
            SpellCastEvent(
                spellType = SpellTypeFilter.CREATURE,
                manaValueAtLeast = 6,
                player = Player.You
            ),
            TriggerBinding.ANY
        )
        effect = AddCountersEffect(
            counterType = "+1/+1",
            count = 3,
            target = EffectTarget.Self
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "121"
        artist = "Arnie Swekel"
        flavorText = "The Mirari's influence turned even the gentlest creatures into savage behemoths."
        imageUri = "https://cards.scryfall.io/large/front/5/2/52a1758c-849a-4de3-b674-857c3c9bf399.jpg?1562529070"
    }
}
