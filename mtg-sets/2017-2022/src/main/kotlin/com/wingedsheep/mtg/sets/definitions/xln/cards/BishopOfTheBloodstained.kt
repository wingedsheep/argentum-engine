package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Bishop of the Bloodstained
 * {3}{B}{B}
 * Creature — Vampire Cleric
 * 3/3
 *
 * When this creature enters, target opponent loses 1 life for each Vampire you control.
 *
 * "Vampire" is the bare tribal noun, so the count is every Vampire *permanent* you control
 * (the Bishop itself included), read on resolution.
 */
val BishopOfTheBloodstained = card("Bishop of the Bloodstained") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire Cleric"
    oracleText = "When this creature enters, target opponent loses 1 life for each Vampire you control."
    power = 3
    toughness = 3

    triggeredAbility {
        trigger = Triggers.self.enters()
        val opponent = target(Targets.Opponent)
        effect = Effects.LoseLife(
            DynamicAmounts
                .battlefield(Player.You, GameObjectFilter.Permanent.withSubtype(Subtype.VAMPIRE))
                .count(),
            opponent
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "91"
        artist = "Yongjae Choi"
        flavorText = "\"They shall give us all they have as penance for their resistance. " +
            "Their ships. Their goods. Their rebellious blood.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/7/373131be-bd66-49f5-80fa-836ced03fedf.jpg?1783935766"
    }
}
