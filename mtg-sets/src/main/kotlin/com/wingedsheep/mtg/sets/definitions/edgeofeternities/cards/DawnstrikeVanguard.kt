package com.wingedsheep.mtg.sets.definitions.edgeofeternities.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.ForEachInGroupEffect
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Dawnstrike Vanguard
 * {5}{W}
 * Creature — Human Knight
 * Lifelink
 * At the beginning of your end step, if you control two or more tapped creatures, put a +1/+1 counter on each creature you control other than this creature.
 */
val DawnstrikeVanguard = card("Dawnstrike Vanguard") {
    manaCost = "{5}{W}"
    typeLine = "Creature — Human Knight"
    power = 4
    toughness = 5
    oracleText = "Lifelink\nAt the beginning of your end step, if you control two or more tapped creatures, put a +1/+1 counter on each creature you control other than this creature."

    // Lifelink keyword
    keywords(Keyword.LIFELINK)

    // End step trigger: put +1/+1 counters on other creatures if you control 2+ tapped creatures
    triggeredAbility {
        trigger = Triggers.YourEndStep
        
        effect = ConditionalEffect(
            condition = Exists(
                com.wingedsheep.sdk.scripting.references.Player.You,
                Zone.BATTLEFIELD,
                GameObjectFilter.Creature.tapped()
            ),
            effect = ForEachInGroupEffect(
                filter = GroupFilter.AllCreaturesYouControl.other(),
                effect = Effects.AddCounters("+1/+1", 1, EffectTarget.Self)
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "10"
        artist = "Arif Wijaya"
        flavorText = "Arrive as the dawn, and banish night through your brilliance!"
        imageUri = "https://cards.scryfall.io/normal/front/5/a/5a041722-9483-469f-9c17-7f0253b0db50.jpg?1752946591"
    }
}
