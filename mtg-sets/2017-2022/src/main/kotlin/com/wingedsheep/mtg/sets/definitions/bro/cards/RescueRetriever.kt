package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PreventDamage
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Rescue Retriever
 * {3}{W}{W}
 * Creature — Dog Soldier
 * 3/3
 * Flash
 * When this creature enters, put a +1/+1 counter on each other Soldier you control.
 * Prevent all damage that would be dealt to other attacking Soldiers you control.
 *
 * The prevention is a static [PreventDamage] shield (the Dolmen Gate shape) over a live recipient
 * filter: any damage, combat or not, to an attacking Soldier you control other than this one. It
 * applies whether or not Rescue Retriever itself is attacking, and stops the moment it leaves the
 * battlefield (rulings 2022-10-14).
 */
val RescueRetriever = card("Rescue Retriever") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Dog Soldier"
    power = 3
    toughness = 3
    oracleText = "Flash\nWhen this creature enters, put a +1/+1 counter on each other Soldier you control.\n" +
        "Prevent all damage that would be dealt to other attacking Soldiers you control."

    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.ForEachInGroup(
            filter = GroupFilter(
                baseFilter = GameObjectFilter.Permanent.withSubtype("Soldier").youControl(),
                excludeSelf = true
            ),
            effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity)
        )
    }

    replacementEffect(
        PreventDamage(
            amount = null,
            appliesTo = EventPattern.DamageEvent(
                recipient = Recipient.Object(
                    GameObjectFilter.Creature.withSubtype("Soldier").youControl().attacking().notSourceItself()
                )
            )
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "288"
        artist = "Jesper Ejsing"
        flavorText = "It brings the two things weary soldiers need most: medicine and morale."
        imageUri = "https://cards.scryfall.io/normal/front/5/9/598def2c-003c-4aa4-ac7c-44ffd9639fdc.jpg?1783919996"

        ruling("2022-10-14", "Rescue Retriever prevents damage to other attacking Soldiers even if it isn't also attacking.")
        ruling("2022-10-14", "If Rescue Retriever leaves the battlefield before combat damage is dealt to your other attacking Soldiers, they will take damage as normal. Notably, this means it is usually a bad idea to attack with Rescue Retriever if your opponent could block it with a creature that has first strike and enough power to kill it.")
    }
}
