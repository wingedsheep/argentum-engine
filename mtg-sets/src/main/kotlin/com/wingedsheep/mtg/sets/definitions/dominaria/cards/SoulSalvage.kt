package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Soul Salvage
 * {2}{B}
 * Sorcery
 * Return up to two target creature cards from your graveyard to your hand.
 */
val SoulSalvage = card("Soul Salvage") {
    manaCost = "{2}{B}"
    typeLine = "Sorcery"
    oracleText = "Return up to two target creature cards from your graveyard to your hand."

    spell {
        target = TargetObject(
            count = 2,
            optional = true,
            filter = TargetFilter.CreatureInYourGraveyard
        )
        effect = ForEachTargetEffect(
            effects = listOf(MoveToZoneEffect(EffectTarget.ContextTarget(0), Zone.HAND))
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "104"
        artist = "Daarken"
        flavorText = "\"The first mistake the Grimnant knights made was bullying the owner of an Urborg corpse shop. Their second mistake was dying.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/4/347060c0-fef7-45da-b42e-8e11051b2c69.jpg?1562733860"
    }
}
