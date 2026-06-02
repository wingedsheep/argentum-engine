package com.wingedsheep.mtg.sets.definitions.dom.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.PreventDamage
import com.wingedsheep.sdk.scripting.events.RecipientFilter

/**
 * Shield of the Realm
 * {2}
 * Artifact — Equipment
 * If a source would deal damage to equipped creature, prevent 2 of that damage.
 * Equip {1}
 */
val ShieldOfTheRealm = card("Shield of the Realm") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "If a source would deal damage to equipped creature, prevent 2 of that damage.\nEquip {1}"

    replacementEffect(
        PreventDamage(
            amount = 2,
            appliesTo = EventPattern.DamageEvent(
                recipient = RecipientFilter.EquippedCreature
            )
        )
    )

    equipAbility("{1}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "228"
        artist = "Manuel Castañón"
        flavorText = "Benalish glazeplate is stained with salts from the Rift Era and enchanted to deflect blows."
        imageUri = "https://cards.scryfall.io/normal/front/7/2/7207edf8-8534-4982-969f-df97febcb9fc.jpg?1562737678"
    }
}
