package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Paladin Danse, Steel Maverick — Fallout #20
 * {2}{W} · Legendary Artifact Creature — Synth Knight · 3/3
 *
 * Vigilance, lifelink
 * Exile Paladin Danse: Each creature you control that's an artifact or Human gains indestructible
 * until end of turn.
 *
 * The group is Creature AND (Artifact OR Human), read at resolution from projected state — so
 * Danse himself, already exiled as the cost, isn't in it.
 */
val PaladinDanseSteelMaverick = card("Paladin Danse, Steel Maverick") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Artifact Creature — Synth Knight"
    power = 3
    toughness = 3
    oracleText = "Vigilance, lifelink\n" +
        "Exile Paladin Danse: Each creature you control that's an artifact or Human gains indestructible until end of turn."

    keywords(Keyword.VIGILANCE, Keyword.LIFELINK)

    activatedAbility {
        cost = Costs.ExileSelf
        effect = Patterns.Group.grantKeywordToAll(
            Keyword.INDESTRUCTIBLE,
            GroupFilter(
                GameObjectFilter.Creature.youControl() and
                    (GameObjectFilter.Artifact or GameObjectFilter.Any.withSubtype(Subtype.HUMAN))
            )
        )
        description = "Exile Paladin Danse: Each creature you control that's an artifact or Human gains indestructible until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "20"
        artist = "Dan Murayama Scott"
        flavorText = "\"I need to be the example, not the exception.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/0/f0e7ee3a-18f1-4771-86d6-db1e0b2aa542.jpg?1783912417"
    }
}
