package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sanctifier of Souls
 * {3}{W}
 * Creature — Human Cleric
 * 2/3
 * Whenever another creature you control enters, this creature gets +1/+1 until end of turn.
 * {2}{W}, Exile a creature card from your graveyard: Create a 1/1 white Spirit creature token
 * with flying.
 */
val SanctifierOfSouls = card("Sanctifier of Souls") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Cleric"
    oracleText = "Whenever another creature you control enters, this creature gets +1/+1 until end of turn.\n" +
        "{2}{W}, Exile a creature card from your graveyard: Create a 1/1 white Spirit creature token with flying."
    power = 2
    toughness = 3

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.youControl()).enters()
        effect = Effects.ModifyStats(1, 1, EffectTarget.Self)
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}{W}"),
            Costs.ExileFromGraveyard(1, GameObjectFilter.Creature)
        )
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Spirit"),
            keywords = setOf(Keyword.FLYING)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "39"
        artist = "Jesper Ejsing"
        imageUri = "https://cards.scryfall.io/normal/front/8/d/8d839c5a-dc35-4eaa-9cf0-b6e002815726.jpg"
    }
}
