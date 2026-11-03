package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ursine Champion — Core Set 2019 #304 (canonical printing)
 * {1}{G}
 * Creature — Human Berserker
 * 2/2
 * {5}{G}: This creature gets +3/+3 and becomes a Bear Berserker until end of turn. Activate only
 * once each turn.
 *
 * "Becomes a Bear Berserker" overwrites its creature types (losing Human) in layer 4 via
 * `SetCreatureSubtypes`; the +3/+3 is a layer-7c modification. Both last until end of turn.
 */
val UrsineChampion = card("Ursine Champion") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Berserker"
    power = 2
    toughness = 2
    oracleText = "{5}{G}: This creature gets +3/+3 and becomes a Bear Berserker until end of turn. " +
        "Activate only once each turn."

    activatedAbility {
        cost = Costs.Mana("{5}{G}")
        restrictions = listOf(ActivationRestriction.OncePerTurn)
        effect = Effects.ModifyStats(3, 3, EffectTarget.Self) then
            Effects.SetCreatureSubtypes(
                subtypes = setOf(Subtype.BEAR.value, Subtype.BERSERKER.value),
                target = EffectTarget.Self,
                duration = Duration.EndOfTurn
            )
        description = "This creature gets +3/+3 and becomes a Bear Berserker until end of turn. " +
            "Activate only once each turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "304"
        artist = "Alex Konstad"
        flavorText = "\"Would you rather feel the edge of my blade or the sharpness of my fangs?\""
        imageUri = "https://cards.scryfall.io/normal/front/e/6/e60b6c02-c1f1-4ab9-bd62-ae3b447a898f.jpg?1783934479"
    }
}
