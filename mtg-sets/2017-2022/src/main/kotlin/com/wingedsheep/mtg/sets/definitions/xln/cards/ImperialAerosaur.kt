package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Imperial Aerosaur
 * {3}{W}
 * Creature — Dinosaur
 * 3/3
 * Flying
 * When this creature enters, another target creature you control gets +1/+1 and gains flying
 * until end of turn.
 *
 * Oliphaunt's shape on an enters trigger: one [TargetFilter.OtherCreatureYouControl] handle feeding
 * a +1/+1 pump then a flying grant, both until end of turn.
 */
val ImperialAerosaur = card("Imperial Aerosaur") {
    manaCost = "{3}{W}"
    typeLine = "Creature — Dinosaur"
    power = 3
    toughness = 3
    oracleText = "Flying\n" +
        "When this creature enters, another target creature you control gets +1/+1 and gains flying until end of turn."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val other = target(TargetFilter.OtherCreatureYouControl)
        effect = Effects.ModifyStats(1, 1, other) then Effects.GrantKeyword(Keyword.FLYING, other)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "14"
        artist = "Jesper Ejsing"
        flavorText = "Its assistance is unnervingly similar to its hunting technique."
        imageUri = "https://cards.scryfall.io/normal/front/7/d/7d08f90b-9b98-4237-9826-578fb812d412.jpg?1783935802"
    }
}
