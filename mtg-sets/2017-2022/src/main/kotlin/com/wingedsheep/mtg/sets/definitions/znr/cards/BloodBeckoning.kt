package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetOther
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Blood Beckoning — Zendikar Rising #92
 * {B} · Sorcery
 *
 * The kicked clause swaps the targets (CR 601.2c), so it rides the `kickerTarget` /
 * `kickerEffect` branch like Urborg Repossession: the plain cast targets one creature card, the
 * kicked cast exactly two ("two target", not "up to two") — a kicked cast needs two creature
 * cards in your graveyard, the second slot a [TargetOther] of the first.
 */
val BloodBeckoning = card("Blood Beckoning") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Kicker {3} (You may pay an additional {3} as you cast this spell.)\n" +
        "Return target creature card from your graveyard to your hand. If this spell was kicked, " +
        "instead return two target creature cards from your graveyard to your hand."

    keywordAbility(KeywordAbility.kicker("{3}"))

    spell {
        val creature = target(TargetFilter.CreatureInYourGraveyard)
        effect = Effects.ReturnToHand(creature)

        val first = kickerTarget(TargetFilter.CreatureInYourGraveyard)
        val second = kickerTarget(TargetOther(TargetObject(filter = TargetFilter.CreatureInYourGraveyard)))
        kickerEffect = Effects.ReturnToHand(first) then Effects.ReturnToHand(second)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "92"
        artist = "Cristi Balanescu"
        imageUri = "https://cards.scryfall.io/normal/front/0/c/0cb88075-14f8-4ac9-b947-5ca1d30938f1.jpg?1783929382"
    }
}
