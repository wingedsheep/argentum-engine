package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.PreventDamage
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Shield of the Avatar
 * {1}
 * Artifact — Equipment
 * If a source would deal damage to equipped creature, prevent X of that damage, where X is the
 * number of creatures you control.
 * Equip {2}
 */
val ShieldOfTheAvatar = card("Shield of the Avatar") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "If a source would deal damage to equipped creature, prevent X of that damage, where X is the number of creatures you control.\n" +
        "Equip {2} ({2}: Attach to target creature you control. Equip only as a sorcery.)"

    replacementEffect(
        PreventDamage(
            amount = DynamicAmounts.creaturesYouControl(),
            appliesTo = EventPattern.DamageEvent(recipient = Recipient.EquippedCreature)
        )
    )

    equipAbility("{2}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "230"
        artist = "Volkan Baǵa"
        flavorText = "We are made stronger by those we fight for.\nDesigned by Richard Garriott"
        imageUri = "https://cards.scryfall.io/normal/front/9/7/97c2cc25-f3cd-4bfb-b9cc-53d1f42cd69a.jpg?1783939155"
    }
}
