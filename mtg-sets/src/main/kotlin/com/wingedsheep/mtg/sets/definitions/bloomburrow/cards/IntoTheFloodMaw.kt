package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetPermanent
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Into the Flood Maw
 * {U}
 * Instant
 *
 * Gift a tapped Fish (You may promise an opponent a gift as you cast this spell.
 * If you do, they create a tapped 1/1 blue Fish creature token before its other effects.)
 *
 * Return target creature an opponent controls to its owner's hand. If the gift was
 * promised, instead return target nonland permanent an opponent controls to its
 * owner's hand.
 */
val IntoTheFloodMaw = card("Into the Flood Maw") {
    manaCost = "{U}"
    typeLine = "Instant"
    oracleText = "Gift a tapped Fish (You may promise an opponent a gift as you cast this spell. If you do, they create a tapped 1/1 blue Fish creature token before its other effects.)\nReturn target creature an opponent controls to its owner's hand. If the gift was promised, instead return target nonland permanent an opponent controls to its owner's hand."

    spell {
        effect = ModalEffect.chooseOne(
            // Mode 1: No gift — return target creature an opponent controls to hand
            Mode.withTarget(
                Effects.ReturnToHand(EffectTarget.ContextTarget(0)),
                Targets.CreatureOpponentControls,
                "Don't promise a gift — return target creature an opponent controls to its owner's hand"
            ),
            // Mode 2: Gift a tapped Fish — opponent gets Fish token, then return target nonland permanent to hand
            Mode.withTarget(
                CreateTokenEffect(
                    count = DynamicAmount.Fixed(1),
                    power = 1,
                    toughness = 1,
                    colors = setOf(Color.BLUE),
                    creatureTypes = setOf("Fish"),
                    controller = EffectTarget.PlayerRef(Player.EachOpponent),
                    tapped = true,
                    imageUri = "https://cards.scryfall.io/normal/front/d/e/de0d6700-49f0-4233-97ba-cef7821c30ed.jpg?1721431109"
                ).then(Effects.ReturnToHand(EffectTarget.ContextTarget(0))),
                TargetPermanent(filter = TargetFilter.NonlandPermanentOpponentControls),
                "Promise a gift — opponent creates a tapped 1/1 blue Fish token, then return target nonland permanent an opponent controls to its owner's hand"
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "52"
        artist = "Danny Schwartz"
        imageUri = "https://cards.scryfall.io/normal/front/5/0/50b9575a-53d9-4df7-b86c-cda021107d3f.jpg?1721426097"
        ruling("2024-07-26", "For instants and sorceries with gift, the gift is given to the appropriate opponent as part of the resolution of the spell. This happens before any of the spell's other effects would take place.")
        ruling("2024-07-26", "If a spell for which the gift was promised is countered, doesn't resolve, or is otherwise removed from the stack, the gift won't be given. None of its other effects will happen either.")
    }
}
