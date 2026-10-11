package com.wingedsheep.mtg.sets.definitions.c15.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Illusory Ambusher
 * {4}{U}
 * Creature — Cat Illusion
 * 4/1
 * Flash
 * Whenever this creature is dealt damage, draw that many cards.
 *
 * The damage amount rides the trigger context ([DynamicAmounts.triggerDamageAmount]), the same
 * read Coalhauler Swine uses — so damage beyond its toughness still counts in full.
 */
val IllusoryAmbusher = card("Illusory Ambusher") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Cat Illusion"
    oracleText = "Flash (You may cast this spell any time you could cast an instant.)\n" +
        "Whenever this creature is dealt damage, draw that many cards."
    power = 4
    toughness = 1

    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.self.isDealtDamage()
        effect = Effects.DrawCards(DynamicAmounts.triggerDamageAmount())
        description = "Whenever this creature is dealt damage, draw that many cards."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "12"
        artist = "Steve Argyle"
        flavorText = "\"Violence is the food it craves. Feed it if you dare.\"\n—Doriel, mentor of Mistral Isle"
        imageUri = "https://cards.scryfall.io/normal/front/5/1/5178477b-73e6-4497-a9d4-5dedf966e412.jpg?1783938118"
        ruling("2017-11-17", "Creatures may be dealt damage greater than their toughness. For example, if a source deals 5 damage to Illusory Ambusher, you'll draw five cards.")
        ruling("2017-11-17", "If Illusory Ambusher blocks a creature with trample, that creature's controller can assign any amount of damage from 1 to that creature's power to Illusory Ambusher. Excess damage doesn't have to be assigned to the defending player.")
    }
}
