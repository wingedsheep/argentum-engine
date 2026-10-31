package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Dwarven Mine
 * Land — Mountain
 *
 * ({T}: Add {R}.)
 * This land enters tapped unless you control three or more other Mountains.
 * When this land enters untapped, create a 1/1 red Dwarf creature token.
 *
 * Same shape as Mystic Sanctuary: the Mountain subtype supplies the mana ability, and "enters
 * untapped" is the `untapped()` state predicate on the self enters trigger.
 */
val DwarvenMine = card("Dwarven Mine") {
    manaCost = ""
    colorIdentity = "R"
    typeLine = "Land — Mountain"
    oracleText = "({T}: Add {R}.)\n" +
        "This land enters tapped unless you control three or more other Mountains.\n" +
        "When this land enters untapped, create a 1/1 red Dwarf creature token."

    replacementEffect(EntersTapped(
        unlessCondition = Conditions.YouControlOtherAtLeast(3, GameObjectFilter.Land.withSubtype(Subtype.MOUNTAIN))
    ))

    triggeredAbility {
        trigger = Triggers.self.matching(GameObjectFilter.Any.untapped()).enters()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Dwarf")
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "243"
        artist = "Alexander Forssberg"
        imageUri = "https://cards.scryfall.io/normal/front/5/c/5c83074d-0c9b-4b58-94ca-d75240485579.jpg?1783932578"
        ruling(
            "2019-10-04",
            "As these lands are entering the battlefield, they check for lands that are already on the " +
                "battlefield. They won't see lands that are entering the battlefield at the same time " +
                "(due to Scapeshift, for example).",
        )
        ruling(
            "2019-10-04",
            "If another effect puts these lands onto the battlefield tapped, they enter tapped, even if " +
                "you control enough lands with the appropriate basic land type.",
        )
    }
}
