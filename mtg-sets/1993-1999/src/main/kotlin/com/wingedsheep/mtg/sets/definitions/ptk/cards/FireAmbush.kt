package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.AnyTarget

/**
 * Fire Ambush
 * {1}{R}
 * Sorcery
 * Fire Ambush deals 3 damage to any target.
 */
val FireAmbush = card("Fire Ambush") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Fire Ambush deals 3 damage to any target."

    spell {
        val t = target("target", AnyTarget())
        effect = Effects.DealDamage(3, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "111"
        artist = "Tang Xiaogu"
        flavorText = "\"With fire he broke the battle at Bowang . . . . Striking fear deep into Cao Cao's soul, thus Kongming scored a coup at his debut.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/d/4dd8bdbd-99c9-4fa7-936a-acc7f4238507.jpg"
    }
}
