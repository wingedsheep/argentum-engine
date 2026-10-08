package com.wingedsheep.mtg.sets.definitions.tle.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.firebending
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantFlashToSpellType

/**
 * Mai and Zuko
 * {1}{U}{B}{R}
 * Legendary Creature — Human Noble Ally
 * 3/5
 *
 * Firebending 3
 * You may cast Ally spells and artifact spells as though they had flash.
 *
 * Firebending 3 is the set's combat-mana helper (`firebending(3)`, CR 702.189). The flash
 * permission is the Gandalf the White / Raff Capashen static, [GrantFlashToSpellType], over
 * `Ally ∨ Artifact` spells, controller-only ("you may cast").
 */
val MaiAndZuko = card("Mai and Zuko") {
    manaCost = "{1}{U}{B}{R}"
    colorIdentity = "UBR"
    typeLine = "Legendary Creature — Human Noble Ally"
    power = 3
    toughness = 5
    oracleText = "Firebending 3\n" +
        "You may cast Ally spells and artifact spells as though they had flash."

    firebending(3)

    staticAbility {
        ability = GrantFlashToSpellType(
            filter = GameObjectFilter.Any.withSubtype(Subtype.ALLY) or GameObjectFilter.Artifact,
            controllerOnly = true
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "68"
        artist = "Brian Yuen"
        imageUri = "https://cards.scryfall.io/normal/front/4/b/4b50fe92-e596-4949-929b-54ae25a7992c.jpg?1783904839"
        ruling("2025-10-02", "Multiple instances of firebending on the same creature trigger separately, each granting you the appropriate amount of mana.")
        ruling("2025-10-02", "Firebending abilities aren't mana abilities. They use the stack and can be responded to.")
    }
}
