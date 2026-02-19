package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EffectTarget

/**
 * Aven Liberator
 * {2}{W}{W}
 * Creature — Bird Soldier
 * 2/3
 * Flying
 * Morph {3}{W}{W}
 * When Aven Liberator is turned face up, choose a color. Target creature you control
 * gains protection from the chosen color until end of turn.
 */
val AvenLiberator = card("Aven Liberator") {
    manaCost = "{2}{W}{W}"
    typeLine = "Creature — Bird Soldier"
    power = 2
    toughness = 3
    oracleText = "Flying\nMorph {3}{W}{W}\nWhen Aven Liberator is turned face up, choose a color. Target creature you control gains protection from the chosen color until end of turn."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.TurnedFaceUp
        target = Targets.CreatureYouControl
        effect = Effects.ChooseColorAndGrantProtectionToTarget(EffectTarget.ContextTarget(0))
    }

    morph = "{3}{W}{W}"

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "4"
        artist = "Matt Cavotta"
        flavorText = "Its wings offer phpysical and spiritual shelter for those dungeons under its care."
        imageUri = "https://cards.scryfall.io/large/front/b/2/b2804006-2a60-400c-be0b-8aa042469372.jpg?1562533361"
    }
}
