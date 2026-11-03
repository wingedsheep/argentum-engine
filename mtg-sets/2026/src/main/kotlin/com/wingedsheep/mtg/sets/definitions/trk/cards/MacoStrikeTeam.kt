package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * MACO Strike Team
 * {G}
 * Creature — Human Soldier
 * 2/1
 *
 * Whenever a creature you control becomes the target of a spell or ability an opponent controls,
 * create a tapped 1/1 red Officer creature token.
 */
val MacoStrikeTeam = card("MACO Strike Team") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 1
    oracleText = "Whenever a creature you control becomes the target of a spell or ability an opponent " +
        "controls, create a tapped 1/1 red Officer creature token."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).becomesTarget(byOpponent = true)
        effect = Effects.CreateToken(
            power = 1, toughness = 1,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Officer"),
            tapped = true,
            imageUri = "https://cards.scryfall.io/normal/front/d/6/d676cbef-8e31-4ae6-8dfe-6b84160f71e0.jpg?1793363893"
        )
        description = "Whenever a creature you control becomes the target of a spell or ability an " +
            "opponent controls, create a tapped 1/1 red Officer creature token."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "206"
        artist = "Aaron J. Riley"
        flavorText = "Starfleet represents humanity's open arms. Military Assault Command Operations " +
            "represents its closed fists."
        imageUri = "https://cards.scryfall.io/normal/front/9/8/98a38e3f-e5ff-402f-9bcd-a03edc264688.jpg?1793188018"
    }
}
