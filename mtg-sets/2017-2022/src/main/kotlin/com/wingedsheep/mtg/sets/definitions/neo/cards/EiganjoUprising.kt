package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.CollectionSlot
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.minus

private const val SAMURAI_TOKEN_IMAGE =
    "https://cards.scryfall.io/normal/front/f/6/f68e5337-6e44-4f8f-a102-2f97b433beea.jpg?1783923716"

/**
 * Eiganjo Uprising — Kamigawa: Neon Dynasty #217 (canonical printing)
 * {X}{R}{W} · Sorcery
 *
 * Create X 2/2 white Samurai creature tokens with vigilance. They gain menace and haste until
 * end of turn.
 * Each opponent creates X minus one 2/2 white Samurai creature tokens with vigilance.
 *
 * "They" iterates the [CollectionSlot.CreatedTokens] collection published by the first
 * create-token step, so menace and haste land only on the caster's tokens and only until end of
 * turn. The opponents' tokens are made afterwards (the create-token executor fans a
 * multi-player `controller` out per opponent), so they never pick up the grant. With X = 0 the
 * opponents' count is −1, which the executor treats as creating nothing.
 */
val EiganjoUprising = card("Eiganjo Uprising") {
    manaCost = "{X}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Sorcery"
    oracleText = "Create X 2/2 white Samurai creature tokens with vigilance. They gain menace and " +
        "haste until end of turn.\nEach opponent creates X minus one 2/2 white Samurai creature " +
        "tokens with vigilance."

    spell {
        effect = Effects.CreateToken(
            count = DynamicAmounts.xValue(),
            power = 2,
            toughness = 2,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Samurai"),
            keywords = setOf(Keyword.VIGILANCE),
            imageUri = SAMURAI_TOKEN_IMAGE,
        ) then
            Effects.ForEachInCollection(
                CollectionSlot.CreatedTokens,
                Effects.GrantKeyword(Keyword.MENACE, EffectTarget.IterationEntity) then
                    Effects.GrantKeyword(Keyword.HASTE, EffectTarget.IterationEntity)
            ) then
            Effects.CreateToken(
                count = DynamicAmounts.xValue() - 1,
                power = 2,
                toughness = 2,
                colors = setOf(Color.WHITE),
                creatureTypes = setOf("Samurai"),
                keywords = setOf(Keyword.VIGILANCE),
                controller = EffectTarget.PlayerRef(Player.EachOpponent),
                imageUri = SAMURAI_TOKEN_IMAGE,
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "217"
        artist = "Johan Grenier"
        flavorText = "Two different visions of freedom clashed at the gates of the Imperial Palace."
        imageUri = "https://cards.scryfall.io/normal/front/3/5/35203138-5237-49aa-a335-7a32829548a1.jpg?1783923838"
    }
}
