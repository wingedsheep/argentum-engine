package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Shisato, Whispering Hunter
 * {3}{G}
 * Legendary Creature — Snake Warrior
 * 2/2
 * At the beginning of your upkeep, sacrifice a Snake.
 * Whenever Shisato deals combat damage to a player, that player skips their next untap step.
 *
 * Shisato is a Snake itself, so with no other Snake it eats itself.
 */
val ShisatoWhisperingHunter = card("Shisato, Whispering Hunter") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Snake Warrior"
    power = 2
    toughness = 2
    oracleText = "At the beginning of your upkeep, sacrifice a Snake.\n" +
        "Whenever Shisato deals combat damage to a player, that player skips their next untap step."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.Sacrifice(GameObjectFilter.Permanent.withSubtype("Snake"), 1, EffectTarget.Controller)
        description = "At the beginning of your upkeep, sacrifice a Snake."
    }

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.SkipNextUntapStep(EffectTarget.PlayerRef(Player.TriggeringPlayer))
        description = "Whenever Shisato deals combat damage to a player, that player skips their next untap step."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "242"
        artist = "John Bolton"
        imageUri = "https://cards.scryfall.io/normal/front/5/c/5cc79c9c-f776-4c55-a7d1-9fac33f14630.jpg?1783944282"
    }
}
