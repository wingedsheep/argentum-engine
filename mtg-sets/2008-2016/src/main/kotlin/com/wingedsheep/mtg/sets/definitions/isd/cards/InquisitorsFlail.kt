package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DoubleDamage
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Inquisitor's Flail
 * {2}
 * Artifact — Equipment
 * If equipped creature would deal combat damage, it deals double that damage instead.
 * If another creature would deal combat damage to equipped creature, it deals double that damage
 * to equipped creature instead.
 * Equip {2}
 *
 * Two [DoubleDamage] replacements, both combat-only and both resolved from the Flail's own
 * attachment (`attachedToBySource()`), so they follow the Equipment and do nothing while it is
 * unattached:
 *  - the source side — damage dealt *by* the equipped creature, to any recipient (as Mjölnir);
 *  - the recipient side — damage dealt *to* the equipped creature by a creature other than it.
 */
val InquisitorsFlail = card("Inquisitor's Flail") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "If equipped creature would deal combat damage, it deals double that damage instead.\n" +
        "If another creature would deal combat damage to equipped creature, it deals double that " +
        "damage to equipped creature instead.\n" +
        "Equip {2}"

    // If equipped creature would deal combat damage, it deals double that damage instead.
    replacementEffect(
        DoubleDamage(
            appliesTo = EventPattern.DamageEvent(
                source = GameObjectFilter.Any.attachedToBySource(),
                damageType = DamageType.Combat
            )
        )
    )

    // If another creature would deal combat damage to equipped creature, it deals double that
    // damage to equipped creature instead.
    replacementEffect(
        DoubleDamage(
            appliesTo = EventPattern.DamageEvent(
                recipient = Recipient.Object(GameObjectFilter.Creature.attachedToBySource()),
                source = GameObjectFilter.Creature.notAttachedToBySource(),
                damageType = DamageType.Combat
            )
        )
    )

    equipAbility("{2}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "227"
        artist = "Rob Alexander"
        imageUri = "https://cards.scryfall.io/normal/front/3/0/3014f59d-9012-473a-8bb1-8085c6e91632.jpg?1783940900"
    }
}
