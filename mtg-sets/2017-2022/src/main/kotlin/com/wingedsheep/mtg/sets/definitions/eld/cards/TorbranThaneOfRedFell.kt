package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyDamageAmount
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Torbran, Thane of Red Fell
 * {1}{R}{R}{R}
 * Legendary Creature — Dwarf Noble
 * 2/4
 * If a red source you control would deal damage to an opponent or a permanent an opponent
 * controls, it deals that much damage plus 2 instead.
 *
 * Mechanized Warfare's shape: a plain additive [ModifyDamageAmount] with a "red, you control"
 * source filter, so the extra damage is dealt by the original source.
 */
val TorbranThaneOfRedFell = card("Torbran, Thane of Red Fell") {
    manaCost = "{1}{R}{R}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Dwarf Noble"
    power = 2
    toughness = 4
    oracleText = "If a red source you control would deal damage to an opponent or a permanent an " +
        "opponent controls, it deals that much damage plus 2 instead."

    replacementEffect(
        ModifyDamageAmount(
            modifier = 2,
            appliesTo = EventPattern.DamageEvent(
                source = GameObjectFilter.Any.withColor(Color.RED).youControl(),
                recipient = Recipient.OpponentOrPermanentTheyControl,
            ),
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "147"
        artist = "Grzegorz Rutkowski"
        flavorText = "A dwarf's grudge runs deeper than the mountains' roots."
        imageUri = "https://cards.scryfall.io/normal/front/7/9/79f591cd-d277-4ba5-b1bf-1c09cac9cb8a.jpg?1783932614"
        ruling("2019-10-04", "The additional 2 damage is dealt by the same source as the original source of damage. The damage isn't dealt by Torbran unless Torbran is the original source of damage.")
        ruling("2019-10-04", "If another effect modifies how much damage your red source would deal, including preventing some of it, the player being dealt damage or the controller of the permanent being dealt damage chooses an order in which to apply those effects. If all of the damage is prevented, Torbran's effect no longer applies.")
        ruling("2019-10-04", "If damage dealt by a source you control is being divided or assigned among multiple permanents an opponent controls or among an opponent and one or more permanents they control, divide the original amount before adding 2. For example, if you attack with a 5/5 red creature with trample and your opponent blocks with a 2/2 creature, you can assign 2 damage to the blocker and 3 damage to the defending player. These amounts are then modified to 4 and 5, respectively.")
    }
}
