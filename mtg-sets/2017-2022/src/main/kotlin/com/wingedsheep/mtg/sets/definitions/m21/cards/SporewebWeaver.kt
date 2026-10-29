package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Sporeweb Weaver
 * {2}{G}
 * Creature — Spider
 * 1/4
 * Reach, hexproof from blue
 * Whenever this creature is dealt damage, you gain 1 life and create a 1/1 green Saproling
 * creature token.
 *
 * Hexproof from blue is Knight of Grace's [KeywordAbility.hexproofFrom]. The damage trigger is
 * Dromad Purebred's `Triggers.self.isDealtDamage()` — once per damage event however many sources
 * dealt damage at once, and it still fires when the damage is lethal (the trigger reads the event,
 * not the creature's continued presence).
 */
val SporewebWeaver = card("Sporeweb Weaver") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Spider"
    power = 1
    toughness = 4
    oracleText = "Reach, hexproof from blue\n" +
        "Whenever this creature is dealt damage, you gain 1 life and create a 1/1 green Saproling creature token."

    keywords(Keyword.REACH)
    keywordAbility(KeywordAbility.hexproofFrom(Color.BLUE))

    triggeredAbility {
        trigger = Triggers.self.isDealtDamage()
        effect = Effects.GainLife(1) then Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Saproling"),
            imageUri = "https://cards.scryfall.io/normal/front/d/7/d71a5aa4-a960-4c42-b8ac-7003f6e83e95.jpg?1783930590",
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "208"
        artist = "Slawomir Maniak"
        flavorText = "It spins its web throughout the undercity, strange growths sprouting in its wake."
        imageUri = "https://cards.scryfall.io/normal/front/e/6/e6ab63c8-0adc-4d74-aee5-a58ce5c0dad8.jpg?1783930666"
        ruling("2020-06-23", "Hexproof from blue means that Sporeweb Weaver can't be the target of blue spells your opponents control or abilities your opponents control of blue sources.")
        ruling("2020-06-23", "Sporeweb Weaver's last ability triggers just once, no matter how many things deal how much damage to it at once (most likely because two or more creatures blocked it).")
        ruling("2020-06-23", "Sporeweb Weaver's last ability triggers even if the damage causes it to die.")
    }
}
