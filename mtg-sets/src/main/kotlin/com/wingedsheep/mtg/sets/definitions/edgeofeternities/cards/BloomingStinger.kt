package com.wingedsheep.mtg.sets.definitions.edgeofeternities.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Blooming Stinger
 * {1}{G}
 * Creature — Plant Scorpion
 * Deathtouch
 * When this creature enters, another target creature you control gains deathtouch until end of turn.
 */
val BloomingStinger = card("Blooming Stinger") {
    manaCost = "{1}{G}"
    typeLine = "Creature — Plant Scorpion"
    power = 2
    toughness = 2
    oracleText = "Deathtouch\nWhen this creature enters, another target creature you control gains deathtouch until end of turn."

    keywords(Keyword.DEATHTOUCH)

    // Triggered ability: When this creature enters...
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val creature = target("another target creature you control", TargetCreature(filter = TargetFilter.Creature.youControl().other()))
        effect = Effects.GrantKeyword(
            keyword = Keyword.DEATHTOUCH,
            target = creature,
            duration = com.wingedsheep.sdk.scripting.Duration.EndOfTurn
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "174"
        artist = "Alexandre Honoré"
        flavorText = "Death never smelled so sweet."
        imageUri = "https://cards.scryfall.io/normal/front/2/e/2ee859bd-d99c-4b1d-9372-7ff4fc1e8c6a.jpg?1752947262"
    }
}
