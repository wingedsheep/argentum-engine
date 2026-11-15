package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Destructor Dragon
 * {4}{G}{G}
 * Creature — Dragon
 * 4/4
 * Flying
 * When this creature dies, destroy target noncreature permanent.
 */
val DestructorDragon = card("Destructor Dragon") {
    manaCost = "{4}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Dragon"
    power = 4
    toughness = 4
    oracleText = "Flying\n" +
        "When this creature dies, destroy target noncreature permanent."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.dies()
        val permanent = target(TargetFilter.NoncreaturePermanent)
        effect = Effects.Destroy(permanent)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "127"
        artist = "Peter Mohrbacher"
        flavorText = "After countless attacks on the Salt Road where it passes through the frozen tundra, the Abzan began to refer to the area as Atarka territory rather than Temur lands."
        imageUri = "https://cards.scryfall.io/normal/front/a/8/a87cb2f2-00ed-46a3-a69d-5360e1558b03.jpg"
    }
}
