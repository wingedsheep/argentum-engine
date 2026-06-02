package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.events.ControllerFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mirkwood Bats
 * {3}{B}
 * Creature — Bat
 * 2/3
 *
 * Flying
 * Whenever you create or sacrifice a token, each opponent loses 1 life.
 */
val MirkwoodBats = card("Mirkwood Bats") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Bat"
    power = 2
    toughness = 3
    oracleText = "Flying\nWhenever you create or sacrifice a token, each opponent loses 1 life."

    keywords(Keyword.FLYING)

    // Whenever you create a token, each opponent loses 1 life.
    triggeredAbility {
        trigger = TriggerSpec(
            event = EventPattern.TokenCreationEvent(controller = ControllerFilter.You),
            binding = TriggerBinding.ANY
        )
        effect = Effects.LoseLife(1, EffectTarget.PlayerRef(Player.EachOpponent))
    }

    // Whenever you sacrifice a token, each opponent loses 1 life.
    triggeredAbility {
        trigger = TriggerSpec(
            event = EventPattern.PermanentsSacrificedEvent(filter = GameObjectFilter.Token),
            binding = TriggerBinding.ANY
        )
        effect = Effects.LoseLife(1, EffectTarget.PlayerRef(Player.EachOpponent))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "95"
        artist = "John Tedrick"
        flavorText = "\"The enemy has many spies and many ways of hearing.\"\n—Gandalf"
        imageUri = "https://cards.scryfall.io/normal/front/1/5/15f035df-784a-4dc8-b7f5-77139a4e6e99.jpg?1686968575"
    }
}
