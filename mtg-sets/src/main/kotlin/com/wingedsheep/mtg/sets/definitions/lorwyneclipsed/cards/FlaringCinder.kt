package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameEvent.SpellCastEvent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Flaring Cinder
 * {1}{U/R}{U/R}
 * Creature — Elemental Sorcerer
 * 3/2
 * When this creature enters and whenever you cast a spell with mana value 4 or greater,
 * you may discard a card. If you do, draw a card.
 */
val FlaringCinder = card("Flaring Cinder") {
    manaCost = "{1}{U/R}{U/R}"
    typeLine = "Creature — Elemental Sorcerer"
    power = 3
    toughness = 2
    oracleText = "When this creature enters and whenever you cast a spell with mana value 4 or greater, " +
        "you may discard a card. If you do, draw a card."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = MayEffect(
            effect = EffectPatterns.discardCards(1)
                .then(Effects.DrawCards(1)),
            description_override = "You may discard a card. If you do, draw a card."
        )
    }

    triggeredAbility {
        trigger = TriggerSpec(
            SpellCastEvent(
                spellFilter = GameObjectFilter.Any.manaValueAtLeast(4),
                player = Player.You
            ),
            TriggerBinding.ANY
        )
        effect = MayEffect(
            effect = EffectPatterns.discardCards(1)
                .then(Effects.DrawCards(1)),
            description_override = "You may discard a card. If you do, draw a card."
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "225"
        artist = "Kai Carpenter"
        flavorText = "\"Am I losing myself or realizing my true nature?\""
        imageUri = "https://cards.scryfall.io/normal/front/0/6/0691a1a9-18e7-44b7-9a34-764b1ab45a76.jpg?1767862636"
    }
}
