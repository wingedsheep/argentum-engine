package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Cursed Courtier
 * {2}{W}
 * Creature — Human Noble
 * 3/3
 *
 * Lifelink
 * When this creature enters, create a Cursed Role token attached to it. (Enchanted creature is 1/1.)
 *
 * The printed 3/3 is immediately overridden to 1/1 by the Cursed Role's base-P/T-setting static;
 * the Role's 1/1 is what the creature ends up as while enchanted (it keeps lifelink).
 */
val CursedCourtier = card("Cursed Courtier") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Noble"
    power = 3
    toughness = 3
    oracleText = "Lifelink\n" +
        "When this creature enters, create a Cursed Role token attached to it. (Enchanted creature is 1/1.)"

    keywords(Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.CreateRoleToken("Cursed Role", EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "9"
        artist = "Tuan Duong Chu"
        flavorText = "It was the first and last time anyone attempted to levy a tax on the witches of Dunbarrow."
        imageUri = "https://cards.scryfall.io/normal/front/3/d/3d2d5a71-d6e1-4c96-9a53-0e370047a56e.jpg?1783915134"
    }
}
