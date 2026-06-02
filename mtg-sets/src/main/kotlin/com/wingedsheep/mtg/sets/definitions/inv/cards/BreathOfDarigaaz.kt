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
 * Breath of Darigaaz
 * {1}{R}
 * Sorcery
 * Kicker {2}
 * Breath of Darigaaz deals 1 damage to each creature without flying and each player.
 * If this spell was kicked, it deals 4 damage to each creature without flying and each
 * player instead.
 */
val BreathOfDarigaaz = card("Breath of Darigaaz") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Kicker {2} (You may pay an additional {2} as you cast this spell.)\n" +
        "Breath of Darigaaz deals 1 damage to each creature without flying and each player. " +
        "If this spell was kicked, it deals 4 damage to each creature without flying and each player instead."

    keywordAbility(KeywordAbility.kicker("{2}"))

    fun damageToNonFliersAndPlayers(amount: Int): Effect = Effects.Composite(
        listOf(
            Effects.ForEachInGroup(
                GroupFilter.AllCreatures.withoutKeyword(Keyword.FLYING),
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
            effect = damageToNonFliersAndPlayers(4),
            elseEffect = damageToNonFliersAndPlayers(1)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "138"
        artist = "Greg Hildebrandt & Tim Hildebrandt"
        imageUri = "https://cards.scryfall.io/normal/front/4/8/480bb7e3-df03-454d-ada0-592ef8a4a6f0.jpg?1562909692"
    }
}
