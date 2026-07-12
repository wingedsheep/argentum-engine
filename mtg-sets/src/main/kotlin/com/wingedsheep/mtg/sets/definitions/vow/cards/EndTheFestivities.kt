package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * End the Festivities
 * {R}
 * Sorcery
 * End the Festivities deals 1 damage to each opponent and each creature and planeswalker they control.
 */
val EndTheFestivities = card("End the Festivities") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "End the Festivities deals 1 damage to each opponent and each creature and planeswalker they control."
    spell {
        effect = Effects.Composite(
            // 1 damage to each opponent
            Effects.DealDamage(1, EffectTarget.PlayerRef(Player.EachOpponent)),
            // 1 damage to each creature and planeswalker those opponents control
            Effects.ForEachInGroup(
                GroupFilter(GameObjectFilter.CreatureOrPlaneswalker.opponentControls()),
                DealDamageEffect(1, EffectTarget.Self)
            )
        )
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "155"
        artist = "Chris Rallis"
        flavorText = "When the defenses around Lurenbraum Fortress faltered, the Crimson Ballroom received some unexpected guests."
        imageUri = "https://cards.scryfall.io/normal/front/b/e/bec748e6-7245-4a71-aeee-cefed8346948.jpg?1782703079"
    }
}
