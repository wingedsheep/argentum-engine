package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.WasKicked
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.ForEachPlayerEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Canopy Surge
 * {1}{G}
 * Sorcery
 * Kicker {2}
 * Canopy Surge deals 1 damage to each creature with flying and each player.
 * If this spell was kicked, it deals 4 damage to each creature with flying
 * and each player instead.
 */
val CanopySurge = card("Canopy Surge") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Kicker {2} (You may pay an additional {2} as you cast this spell.)\n" +
        "Canopy Surge deals 1 damage to each creature with flying and each player. " +
        "If this spell was kicked, it deals 4 damage to each creature with flying and each player instead."

    keywordAbility(KeywordAbility.kicker("{2}"))

    fun damageToFliersAndPlayers(amount: Int): Effect = Effects.Composite(
        listOf(
            Effects.ForEachInGroup(
                GroupFilter.AllCreatures.withKeyword(Keyword.FLYING),
                DealDamageEffect(amount, EffectTarget.Self)
            ),
            ForEachPlayerEffect(
                players = Player.Each,
                effects = listOf(
                    Effects.DealDamage(amount, EffectTarget.Controller)
                )
            )
        )
    )

    spell {
        effect = ConditionalEffect(
            condition = WasKicked,
            effect = damageToFliersAndPlayers(4),
            elseEffect = damageToFliersAndPlayers(1)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "184"
        artist = "Matt Cavotta"
        imageUri = "https://cards.scryfall.io/normal/front/2/e/2e19d68e-7554-4627-a316-beb1f75fa494.jpg?1562904391"
    }
}
