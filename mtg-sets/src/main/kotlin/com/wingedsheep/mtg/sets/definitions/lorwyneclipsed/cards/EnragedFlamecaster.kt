package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameEvent.SpellCastEvent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Enraged Flamecaster
 * {2}{R}
 * Creature — Elemental Sorcerer
 * 3/2
 *
 * Reach
 * Whenever you cast a spell with mana value 4 or greater, this creature deals 2 damage to each opponent.
 */
val EnragedFlamecaster = card("Enraged Flamecaster") {
    manaCost = "{2}{R}"
    typeLine = "Creature — Elemental Sorcerer"
    power = 3
    toughness = 2
    oracleText = "Reach\nWhenever you cast a spell with mana value 4 or greater, this creature deals 2 damage to each opponent."

    keywords(Keyword.REACH)

    triggeredAbility {
        trigger = TriggerSpec(
            SpellCastEvent(
                spellFilter = GameObjectFilter.Any.manaValueAtLeast(4),
                player = Player.You
            ),
            TriggerBinding.ANY
        )
        effect = Effects.DealDamage(2, EffectTarget.PlayerRef(Player.EachOpponent))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "135"
        artist = "Lars Grant-West"
        flavorText = "He recognized the weapons of his brother who'd gone missing in elven lands. The heat of his grief could be felt for miles."
        imageUri = "https://cards.scryfall.io/normal/front/1/0/104baa2c-75c5-44fc-a3b9-b19efcf3d7c2.jpg?1767871913"
        ruling("2025-11-17", "If a spell has {X} in its mana cost, use the value chosen for that X to determine the mana value of that spell.")
    }
}
