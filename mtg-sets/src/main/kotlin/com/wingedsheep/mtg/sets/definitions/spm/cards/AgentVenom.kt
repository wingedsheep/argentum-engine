package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern.ZoneChangeEvent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Agent Venom
 * {2}{B}
 * Legendary Creature — Symbiote Soldier Hero
 * 2/3
 * Flash, Menace
 * Whenever another nontoken creature you control dies, you draw a card and lose 1 life.
 */
val AgentVenom = card("Agent Venom") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Symbiote Soldier Hero"
    power = 2
    toughness = 3
    oracleText = "Flash\nMenace\nWhenever another nontoken creature you control dies, you draw a card and lose 1 life."

    keywords(Keyword.FLASH, Keyword.MENACE)

    triggeredAbility {
        trigger = TriggerSpec(
            event = ZoneChangeEvent(
                filter = GameObjectFilter.Creature.youControl().nontoken(),
                from = Zone.BATTLEFIELD,
                to = Zone.GRAVEYARD
            ),
            binding = TriggerBinding.OTHER
        )
        effect = Effects.Composite(
            Effects.DrawCards(1),
            Effects.LoseLife(1, EffectTarget.Controller)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "49"
        artist = "Kevin Sidharta"
        flavorText = "Wounded in action, Corporal Thompson was offered an experimental way to be all he could be."
        imageUri = "https://cards.scryfall.io/normal/front/f/5/f5f80d82-d64c-466f-8874-9cfb00469f02.jpg?1757377054"
    }
}
