package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val MobLookout = card("Mob Lookout") {
    manaCost = "{1}{U/B}"
    colorIdentity = "UB"
    typeLine = "Creature — Human Rogue Villain"
    power = 0
    toughness = 3
    oracleText = "When this creature enters, target creature you control connives. " +
        "(Draw a card, then discard a card. If you discarded a nonland card, put a +1/+1 counter on that creature.)"

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val creature = target("target creature you control", Targets.CreatureYouControl)
        effect = Effects.Connive(target = creature)
        description = "When this creature enters, target creature you control connives."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "136"
        artist = "David Palumbo"
        flavorText = "\"Hurry up before the bug gets here!\"\n—Hammerhead"
        imageUri = "https://cards.scryfall.io/normal/front/e/0/e0e3b660-d391-454b-ba57-bff4ddcf27b7.jpg?1783905315"
    }
}
