package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.MayEffect

/**
 * Gustcloak Savior
 * {4}{W}
 * Creature — Bird Soldier
 * 3/4
 * Flying
 * Whenever a creature you control becomes blocked, you may untap that creature and remove it from combat.
 */
val GustcloakSavior = card("Gustcloak Savior") {
    manaCost = "{4}{W}"
    typeLine = "Creature — Bird Soldier"
    power = 3
    toughness = 4
    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.CreatureYouControlBecomesBlocked
        effect = MayEffect(
            Effects.Untap(EffectTarget.TriggeringEntity) then Effects.RemoveFromCombat(EffectTarget.TriggeringEntity)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "36"
        artist = "Jim Nelson"
        flavorText = "\"Our death-arrows flew in high arcs towards the aven. And then . . . nothing.\"\n—Coliseum guard"
        imageUri = "https://cards.scryfall.io/large/front/0/e/0e9d6e81-1869-4ab7-8a4e-477d5c4aed6b.jpg?1562898351"
    }
}
