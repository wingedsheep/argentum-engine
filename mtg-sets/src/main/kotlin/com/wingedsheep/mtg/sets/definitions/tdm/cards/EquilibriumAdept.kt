package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.GrantMayPlayFromExileEffect
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Equilibrium Adept
 * {3}{R}
 * Creature — Dog Monk
 * 2/4
 *
 * When this creature enters, exile the top card of your library. Until the end of your
 * next turn, you may play that card.
 * Flurry — Whenever you cast your second spell each turn, this creature gains double
 * strike until end of turn.
 */
val EquilibriumAdept = card("Equilibrium Adept") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dog Monk"
    power = 2
    toughness = 4
    oracleText = "When this creature enters, exile the top card of your library. Until the end of your next turn, you may play that card.\nFlurry — Whenever you cast your second spell each turn, this creature gains double strike until end of turn."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = CompositeEffect(listOf(
            GatherCardsEffect(
                source = CardSource.TopOfLibrary(DynamicAmount.Fixed(1)),
                storeAs = "exiledCard"
            ),
            MoveCollectionEffect(
                from = "exiledCard",
                destination = CardDestination.ToZone(Zone.EXILE)
            ),
            GrantMayPlayFromExileEffect("exiledCard", MayPlayExpiry.UntilEndOfNextTurn)
        ))
    }

    flurry {
        effect = Effects.GrantKeyword(Keyword.DOUBLE_STRIKE, EffectTarget.Self, Duration.EndOfTurn)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "106"
        artist = "Leroy Steinmann"
        flavorText = "He regained balance while his opponent regained consciousness."
        imageUri = "https://cards.scryfall.io/normal/front/a/4/a4ba6d74-c6be-4a5e-8859-b791bb6b8f51.jpg?1743204387"
    }
}
