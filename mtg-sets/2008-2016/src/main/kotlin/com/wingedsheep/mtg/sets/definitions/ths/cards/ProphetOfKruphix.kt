package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantFlashToSpellType
import com.wingedsheep.sdk.scripting.UntapFilteredDuringOtherUntapSteps

/**
 * Prophet of Kruphix
 * {3}{G}{U}
 * Creature — Human Wizard
 * 2/3
 * Untap all creatures and lands you control during each other player's untap step.
 * You may cast creature spells as though they had flash.
 *
 * Seedborn Muse restricted to creatures and lands ([UntapFilteredDuringOtherUntapSteps]), plus a
 * controller-only [GrantFlashToSpellType] over creature spells. A bestow card cast as an Aura is
 * announced with Aura characteristics before the flash check, so it gets no flash from this.
 */
val ProphetOfKruphix = card("Prophet of Kruphix") {
    manaCost = "{3}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Creature — Human Wizard"
    power = 2
    toughness = 3
    oracleText = "Untap all creatures and lands you control during each other player's untap step.\n" +
        "You may cast creature spells as though they had flash."

    staticAbility {
        ability = UntapFilteredDuringOtherUntapSteps(filter = GameObjectFilter.CreatureOrLand)
    }

    staticAbility {
        ability = GrantFlashToSpellType(filter = GameObjectFilter.Creature, controllerOnly = true)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "199"
        artist = "Winona Nelson"
        flavorText = "\"Time is fluid as a dance, and truth as fleeting.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/5/45de923f-fdab-460c-96f4-f62aefa9ad73.jpg?1783939727"
        ruling(
            "2013-09-15",
            "Effects that state a creature or land you control doesn't untap during your untap step won't " +
                "apply during another player's untap step."
        )
        ruling(
            "2013-09-15",
            "Controlling more than one Prophet of Kruphix doesn't allow you to untap any creature or land " +
                "more than once during a single untap step."
        )
        ruling(
            "2013-09-15",
            "If you're casting a creature spell with bestow, whether or not it has flash will depend on if " +
                "you're casting it as a creature or as an Aura. Prophet of Kruphix won't give flash to a " +
                "spell with bestow you're casting as an Aura."
        )
        ruling(
            "2013-09-15",
            "The last ability applies to creature cards in any zone, provided something is allowing you to " +
                "cast them."
        )
    }
}
