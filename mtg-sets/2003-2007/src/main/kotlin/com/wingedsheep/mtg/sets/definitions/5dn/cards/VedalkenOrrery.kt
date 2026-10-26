package com.wingedsheep.mtg.sets.definitions.`5dn`.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantFlashToSpellType

/**
 * Vedalken Orrery — Fifth Dawn #163 (canonical / earliest real-expansion printing).
 * {4} · Artifact
 *
 * You may cast spells as though they had flash.
 *
 * Leyline of Anticipation's static without the opening-hand clause: [GrantFlashToSpellType] over
 * every spell, scoped to the controller. It only changes when spells may be cast — sorcery-speed
 * activated abilities are untouched, as the 2010-06-15 ruling says.
 */
val VedalkenOrrery = card("Vedalken Orrery") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "You may cast spells as though they had flash."

    staticAbility {
        ability = GrantFlashToSpellType(
            filter = GameObjectFilter.Any,
            controllerOnly = true
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "163"
        artist = "John Avon"
        flavorText = "The model incorporated the fifth sun effortlessly, as if it had always known."
        imageUri = "https://cards.scryfall.io/normal/front/9/4/94b546fa-af65-4afa-bb16-69b33674d3d1.jpg?1783944371"
        ruling(
            "2010-06-15",
            "This applies only to casting spells. It does not, for example, change when you may activate " +
                "abilities that can only be activated \"any time you could cast a sorcery\"."
        )
        ruling(
            "2004-12-01",
            "The controller of Vedalken Orrery can cast nonland artifact, creature, enchantment, and sorcery " +
                "cards any time they could cast an instant. This includes on other players' turns and during " +
                "the upkeep step, draw step, combat phase, and end step."
        )
    }
}
