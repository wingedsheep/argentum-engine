package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Ogre Errant — Throne of Eldraine #132
 * {3}{R} · Creature — Ogre Knight · 3/4
 *
 * Whenever this creature attacks, another target attacking Knight gains menace until end of turn.
 *
 * "Another target attacking Knight" is an attacking creature with the Knight subtype, excluding the
 * Ogre itself via `excludeSelf`.
 */
val OgreErrant = card("Ogre Errant") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Ogre Knight"
    power = 3
    toughness = 4
    oracleText = "Whenever this creature attacks, another target attacking Knight gains menace until end of turn. " +
        "(It can't be blocked except by two or more creatures.)"

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val knight = target(
            TargetFilter(GameObjectFilter.Creature.withSubtype(Subtype.KNIGHT).attacking(), excludeSelf = true)
        )
        effect = Effects.GrantKeyword(Keyword.MENACE, knight)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "132"
        artist = "Mitchell Malloy"
        flavorText = "Knights kept returning from Ogre's Pass wearing nothing but bruises, so they decided to just " +
            "pretend it was on their side."
        imageUri = "https://cards.scryfall.io/normal/front/0/e/0e96f3dc-dc58-4034-8d3d-3ae74fa64562.jpg?1783932621"
    }
}
