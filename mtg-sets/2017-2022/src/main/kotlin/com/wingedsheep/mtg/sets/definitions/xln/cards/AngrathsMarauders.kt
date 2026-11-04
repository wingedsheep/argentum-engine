package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DoubleDamage
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient

val AngrathsMarauders = card("Angrath's Marauders") {
    manaCost = "{5}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Pirate"
    power = 4
    toughness = 4
    oracleText = "If a source you control would deal damage to a permanent or player, it deals double that damage to that permanent or player instead."

    replacementEffect(
        DoubleDamage(
            appliesTo = EventPattern.DamageEvent(
                source = GameObjectFilter.Any.youControl(),
                recipient = Recipient.Any,
            )
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "132"
        artist = "Victor Adame Minguez"
        flavorText = "Captain Angrath cares less about treasure than mayhem. If he can't leave Ixalan, he'll burn the plane down."
        imageUri = "https://cards.scryfall.io/normal/front/f/0/f0bfc9e0-14e8-43ce-8fca-773b7f2387dc.jpg?1783935749"
        ruling("2017-09-29", "If a creature with trample you control would deal combat damage to a blocking creature while you control Angrath's Marauders, you must assign its unmodified damage. For example, a 3/3 creature with trample blocked by a 2/2 creature can have at most 1 damage assigned to the defending player. It will then deal 4 damage to the blocking creature and 2 damage to the defending player.")
        ruling("2017-09-29", "If an effect such as that of Chandra's Pyrohelix asks you to divide damage among targets, you must divide the unmodified damage before doubling it.")
        ruling("2017-09-29", "If you control a second Angrath's Marauders, damage dealt by sources you control will be multiplied by 4. If you control a third, it will be multiplied by 8, and so on.")
    }
}
