package com.wingedsheep.mtg.sets.definitions.dom.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.EventPattern.ZoneChangeEvent
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Slimefoot, the Stowaway
 * {1}{B}{G}
 * Legendary Creature — Fungus
 * 2/3
 * Whenever a Saproling you control dies, Slimefoot, the Stowaway deals 1 damage to each
 * opponent and you gain 1 life.
 * {4}: Create a 1/1 green Saproling creature token.
 */
val SlimefootTheStowaway = card("Slimefoot, the Stowaway") {
    manaCost = "{1}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Legendary Creature — Fungus"
    power = 2
    toughness = 3
    oracleText = "Whenever a Saproling you control dies, Slimefoot, the Stowaway deals 1 damage to each opponent and you gain 1 life.\n{4}: Create a 1/1 green Saproling creature token."

    triggeredAbility {
        trigger = TriggerSpec(
            event = ZoneChangeEvent(
                filter = GameObjectFilter.Creature.youControl().withSubtype("Saproling"),
                from = Zone.BATTLEFIELD,
                to = Zone.GRAVEYARD
            ),
            binding = TriggerBinding.ANY
        )
        effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.EachOpponent)) then
                Effects.GainLife(1)
    }

    activatedAbility {
        cost = Costs.Mana("{4}")
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Saproling")
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "205"
        artist = "Alex Konstad"
        flavorText = "As Jhoira restored the Weatherlight, a mushroom growing in its hold unexpectedly became her first crew member."
        imageUri = "https://cards.scryfall.io/normal/front/e/8/e8815cd9-7032-445a-aebc-cfc19bd51ee4.jpg?1562744768"
    }
}
