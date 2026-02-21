package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.BecomeCreatureTypeEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.conditions.SourceHasSubtype
import com.wingedsheep.sdk.scripting.StaticTarget

/**
 * Mistform Wall
 * {2}{U}
 * Creature — Illusion Wall
 * 1/4
 * This creature has defender as long as it's a Wall.
 * {1}: Mistform Wall becomes the creature type of your choice until end of turn.
 */
val MistformWall = card("Mistform Wall") {
    manaCost = "{2}{U}"
    typeLine = "Creature — Illusion Wall"
    power = 1
    toughness = 4
    oracleText = "This creature has defender as long as it's a Wall.\n{1}: Mistform Wall becomes the creature type of your choice until end of turn."

    staticAbility {
        ability = GrantKeyword(Keyword.DEFENDER, StaticTarget.SourceCreature)
        condition = SourceHasSubtype(Subtype("Wall"))
    }

    activatedAbility {
        cost = Costs.Mana("{1}")
        effect = BecomeCreatureTypeEffect(
            target = EffectTarget.Self
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "99"
        artist = "Franz Vohwinkel"
        flavorText = "\"Fellowship, the fourth myth of reality: In times of war, only the shifting allegiances of allies keep the world safe.\""
        imageUri = "https://cards.scryfall.io/large/front/e/b/ebaa7a26-8516-4d71-a524-77b2d3f030d5.jpg?1562941780"
        ruling("2004-10-04", "As of the Champions of Kamigawa rules update, the Wall creature type no longer inherently prevents attacking. All Walls printed before this update, including Mistform Wall, received errata granting defender conditionally: \"This creature has defender as long as it's a Wall.\" Changing its creature type away from Wall removes defender, allowing it to attack.")
    }
}
