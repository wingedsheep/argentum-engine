package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.ChangeGroupColorEffect
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.GrantActivatedAbilityToGroupEffect
import com.wingedsheep.sdk.scripting.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.SetGroupCreatureSubtypesEffect
import com.wingedsheep.sdk.scripting.Duration

/**
 * Shade's Breath
 * {1}{B}
 * Instant
 * Until end of turn, each creature you control becomes a black Shade and gains
 * "{B}: This creature gets +1/+1 until end of turn."
 */
val ShadesBreath = card("Shade's Breath") {
    manaCost = "{1}{B}"
    typeLine = "Instant"

    spell {
        effect = SetGroupCreatureSubtypesEffect(
            subtypes = setOf("Shade")
        ) then ChangeGroupColorEffect(
            colors = setOf("BLACK")
        ) then GrantActivatedAbilityToGroupEffect(
            ability = ActivatedAbility(
                id = AbilityId.generate(),
                cost = AbilityCost.Mana(ManaCost.parse("{B}")),
                effect = ModifyStatsEffect(
                    powerModifier = 1,
                    toughnessModifier = 1,
                    target = EffectTarget.Self,
                    duration = Duration.EndOfTurn
                )
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "167"
        artist = "Franz Vohwinkel"
        flavorText = "The grim business of the Coliseum casts long shadows."
        imageUri = "https://cards.scryfall.io/large/front/a/3/a37be9a8-ef69-4c62-8455-e129e62fe69a.jpg?1562933592"
    }
}
