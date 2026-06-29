package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Earth Kingdom Protectors
 * {W}
 * Creature — Human Soldier Ally
 * 1/1
 *
 * Vigilance
 * Sacrifice this creature: Another target Ally you control gains indestructible until end of turn.
 */
val EarthKingdomProtectors = card("Earth Kingdom Protectors") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier Ally"
    oracleText = "Vigilance\n" +
        "Sacrifice this creature: Another target Ally you control gains indestructible until end of turn. " +
        "(Damage and effects that say \"destroy\" don't destroy it.)"
    power = 1
    toughness = 1

    keywords(Keyword.VIGILANCE)

    activatedAbility {
        cost = Costs.SacrificeSelf
        val t = target(
            "another target Ally you control",
            TargetCreature(
                filter = TargetFilter(
                    GameObjectFilter.Creature.withSubtype(Subtype.ALLY).youControl(),
                    excludeSelf = true
                )
            )
        )
        effect = Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "17"
        artist = "Leonardo Borazio"
        flavorText = "\"Stand your ground! The Earth King must be protected!\""
        imageUri = "https://cards.scryfall.io/normal/front/d/2/d263472e-1d75-4cb9-8f7b-986fa22bc841.jpg?1764119985"
    }
}
