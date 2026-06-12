package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GrantMayPlayFromExileEffect
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.RecipientFilter
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.dsl.Effects

/**
 * Moria Marauder
 * {R}{R}
 * Creature — Goblin Warrior
 * 1/1
 *
 * Double strike
 * Whenever a Goblin or Orc you control deals combat damage to a player, exile the top card
 * of your library. You may play that card this turn.
 */
val MoriaMarauder = card("Moria Marauder") {
    manaCost = "{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin Warrior"
    power = 1
    toughness = 1
    oracleText = "Double strike\n" +
        "Whenever a Goblin or Orc you control deals combat damage to a player, exile the top card of your library. You may play that card this turn."

    keywords(Keyword.DOUBLE_STRIKE)

    triggeredAbility {
        trigger = Triggers.dealsDamage(
            damageType = DamageType.Combat,
            recipient = RecipientFilter.AnyPlayer,
            sourceFilter = GameObjectFilter.Creature.withAnySubtype("Goblin", "Orc").youControl(),
            binding = TriggerBinding.ANY,
        )
        effect = Effects.Pipeline {
            val exiledCard = gather(
                CardSource.TopOfLibrary(DynamicAmount.Fixed(1)),
                name = "exiledCard"
            )
            move(
                exiledCard,
                destination = CardDestination.ToZone(Zone.EXILE)
            )
            run(GrantMayPlayFromExileEffect("exiledCard"))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "138"
        artist = "Andrea Piparo"
        flavorText = "There was a rush of hoarse laughter, like the fall of sliding stones into a pit; amid the clamor a deep voice was raised in command."
        imageUri = "https://cards.scryfall.io/normal/front/f/3/f33d5394-2248-4654-bec5-33b144752586.jpg?1686969061"
    }
}
