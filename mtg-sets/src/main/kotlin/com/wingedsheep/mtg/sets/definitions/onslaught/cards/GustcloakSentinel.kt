package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.MayEffect

/**
 * Gustcloak Sentinel
 * {2}{W}{W}
 * Creature — Human Soldier
 * 3/3
 * Whenever Gustcloak Sentinel becomes blocked, you may untap it and remove it from combat.
 */
val GustcloakSentinel = card("Gustcloak Sentinel") {
    manaCost = "{2}{W}{W}"
    typeLine = "Creature — Human Soldier"
    power = 3
    toughness = 3
    oracleText = "Whenever Gustcloak Sentinel becomes blocked, you may untap it and remove it from combat."

    triggeredAbility {
        trigger = Triggers.BecomesBlocked
        effect = MayEffect(
            Effects.Untap(EffectTarget.Self) then Effects.RemoveFromCombat(EffectTarget.Self)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "37"
        artist = "Mark Zug"
        flavorText = "\"Entire platoons have mysteriously vanished from battle, leaving enemy weapons to slice through empty air.\""
        imageUri = "https://cards.scryfall.io/large/front/b/9/b90da5c3-fd8f-445d-809f-e129870d7449.jpg?1562937912"
    }
}
