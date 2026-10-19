package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PreventDamage
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Bronze Horse
 * {7}
 * Artifact Creature — Horse
 * 4/4
 * Trample
 * As long as you control another creature, prevent all damage that would be dealt to this
 * creature by spells that target it.
 *
 * A static [PreventDamage] replacement on the Horse itself. "Spells" is the damage source being on
 * the stack (`currentlyIn(Zone.STACK)`, as Hostility reads "a spell"), and "that target it" is
 * that spell having this permanent among its chosen targets (`targetsMatching(sourceItself())`).
 * An ability's damage comes from its permanent source, not a spell, so it is never prevented;
 * neither is damage from an untargeted spell. The "another creature" gate is re-checked each time
 * damage would be dealt.
 */
val BronzeHorse = card("Bronze Horse") {
    manaCost = "{7}"
    typeLine = "Artifact Creature — Horse"
    power = 4
    toughness = 4
    oracleText = "Trample\nAs long as you control another creature, prevent all damage that would be " +
        "dealt to this creature by spells that target it."

    keywords(Keyword.TRAMPLE)

    replacementEffect(
        PreventDamage(
            restrictions = listOf(Conditions.YouControl(GameObjectFilter.Creature, excludeSelf = true)),
            appliesTo = EventPattern.DamageEvent(
                recipient = Recipient.Self,
                source = GameObjectFilter.Any
                    .currentlyIn(Zone.STACK)
                    .targetsMatching(GameObjectFilter.Any.sourceItself())
            )
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "276"
        artist = "Mark Poole"
        imageUri = "https://cards.scryfall.io/normal/front/9/3/936a03a5-73ba-436c-9d49-70e176d118e6.jpg?1783948029"
    }
}
