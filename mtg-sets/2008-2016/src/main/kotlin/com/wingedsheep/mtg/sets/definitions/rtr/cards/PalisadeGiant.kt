package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.RedirectDamage
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Palisade Giant
 * {4}{W}{W}
 * Creature — Giant Soldier
 * 2/7
 *
 * All damage that would be dealt to you and other permanents you control is dealt to this
 * creature instead.
 *
 * Canonical printing: Return to Ravnica, the card's earliest real printing.
 *
 * The Ancient Adamantoise shape: two static [RedirectDamage] replacements to
 * [EffectTarget.Self] — one for damage to the controller ([Recipient.You]), one for damage to
 * their permanents ([Recipient.Object] over `Permanent.youControl()`). Damage already headed for
 * the Giant itself is left alone (the engine skips a redirect whose destination is the original
 * recipient), which is the "other permanents" clause. Redirection keeps the damage's combat
 * status, per the card's ruling.
 */
val PalisadeGiant = card("Palisade Giant") {
    manaCost = "{4}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Giant Soldier"
    oracleText = "All damage that would be dealt to you and other permanents you control is dealt to this creature instead."
    power = 2
    toughness = 7

    // "All damage that would be dealt to you ... is dealt to this creature instead."
    replacementEffect(
        RedirectDamage(
            redirectTo = EffectTarget.Self,
            appliesTo = EventPattern.DamageEvent(recipient = Recipient.You),
        )
    )
    // "... and other permanents you control ..."
    replacementEffect(
        RedirectDamage(
            redirectTo = EffectTarget.Self,
            appliesTo = EventPattern.DamageEvent(
                recipient = Recipient.Object(GameObjectFilter.Permanent.youControl()),
            ),
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "15"
        artist = "Greg Staples"
        flavorText = "Refusing guild allegiances, she fights for those who can't protect themselves."
        imageUri = "https://cards.scryfall.io/normal/front/f/d/fdfd37ca-e4c9-4674-a75f-15d8ebcce72b.jpg?1783940375"
        ruling("2012-10-01", "Applying this redirection effect doesn't change whether the damage is combat damage.")
        ruling(
            "2012-10-01",
            "If you control more than one Palisade Giant, you choose which redirection effect to apply. " +
                "You can't divide damage dealt by one source. For example, if an attacking creature would deal " +
                "8 damage to you and you control two Palisade Giants, you may have that damage dealt to either " +
                "of the Palisade Giants. You can't have 4 damage dealt to each Giant or choose to have the " +
                "8 damage dealt to you."
        )
    }
}
