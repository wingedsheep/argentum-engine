package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Volcanic Vision
 * {5}{R}{R}
 * Sorcery
 * Return target instant or sorcery card from your graveyard to your hand. Volcanic Vision deals
 * damage equal to that card's mana value to each creature your opponents control. Exile Volcanic Vision.
 *
 * The damage reads the returned card's mana value off the same entity after the move — a card keeps
 * its id and CardComponent across the graveyard-to-hand zone change — so {X} in its cost counts as 0
 * (ruling 2015-02-25). If the target is illegal on resolution the whole spell fizzles: no damage, and
 * it goes to the graveyard rather than exile.
 */
val VolcanicVision = card("Volcanic Vision") {
    manaCost = "{5}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Return target instant or sorcery card from your graveyard to your hand. Volcanic Vision " +
        "deals damage equal to that card's mana value to each creature your opponents control. Exile Volcanic Vision."

    spell {
        val returned = target(TargetFilter(GameObjectFilter.InstantOrSorcery.ownedByYou(), zone = Zone.GRAVEYARD))
        effect = Effects.Move(returned, Zone.HAND) then
            Patterns.Group.dealDamageToAll(
                DynamicAmounts.manaValueOf(returned),
                GroupFilter.AllCreaturesOpponentsControl,
            )
        selfExile()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "167"
        artist = "Noah Bradley"
        imageUri = "https://cards.scryfall.io/normal/front/9/7/979da13b-9be6-49cc-a62c-67eeea289612.jpg?1783938584"
        ruling(
            "2015-02-25",
            "If the target instant or sorcery card becomes an illegal target before Volcanic Vision resolves, " +
                "the spell won't resolve and none of its effects will happen. No damage will be dealt and " +
                "Volcanic Vision won't be exiled."
        )
        ruling("2015-02-25", "If the target has {X} in its mana cost, X is 0.")
    }
}
