package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.EffectChoice
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Alchemist's Gift
 * {B}
 * Instant
 * Target creature gets +1/+1 and gains your choice of deathtouch or lifelink until end of turn.
 *
 * "Your choice of" is made as the spell resolves, not as it is cast, so it is an
 * [Effects.ChooseAction] after the pump rather than a modal spell (the Silverquill Pledgemage shape).
 */
val AlchemistsGift = card("Alchemist's Gift") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Target creature gets +1/+1 and gains your choice of deathtouch or lifelink until " +
        "end of turn. (Any amount of damage a creature with deathtouch deals to a creature is enough " +
        "to destroy it. Damage dealt by a creature with lifelink also causes its controller to gain " +
        "that much life.)"

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(1, 1, creature) then Effects.ChooseAction(listOf(
            EffectChoice(
                label = "It gains deathtouch until end of turn",
                effect = Effects.GrantKeyword(Keyword.DEATHTOUCH, creature),
            ),
            EffectChoice(
                label = "It gains lifelink until end of turn",
                effect = Effects.GrantKeyword(Keyword.LIFELINK, creature),
            ),
        ))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "87"
        artist = "Cristi Balanescu"
        imageUri = "https://cards.scryfall.io/normal/front/6/e/6e4c9574-1ee3-461e-848f-8f02c6a8b7ee.jpg?1783930712"
    }
}
