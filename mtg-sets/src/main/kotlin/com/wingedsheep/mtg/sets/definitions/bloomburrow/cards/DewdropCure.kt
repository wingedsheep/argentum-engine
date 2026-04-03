package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Dewdrop Cure {2}{W}
 * Sorcery
 *
 * Gift a card (You may promise an opponent a gift as you cast this spell.
 * If you do, they draw a card before its other effects.)
 * Return up to two target creature cards each with mana value 2 or less from your
 * graveyard to the battlefield. If the gift was promised, instead return up to three
 * target creature cards each with mana value 2 or less from your graveyard to the battlefield.
 */
val DewdropCure = card("Dewdrop Cure") {
    manaCost = "{2}{W}"
    typeLine = "Sorcery"
    oracleText = "Gift a card (You may promise an opponent a gift as you cast this spell. If you do, they draw a card before its other effects.)\nReturn up to two target creature cards each with mana value 2 or less from your graveyard to the battlefield. If the gift was promised, instead return up to three target creature cards each with mana value 2 or less from your graveyard to the battlefield."

    val graveyardFilter = TargetFilter.CreatureInYourGraveyard.manaValueAtMost(2)
    val returnEffect = ForEachTargetEffect(
        effects = listOf(
            MoveToZoneEffect(EffectTarget.ContextTarget(0), Zone.BATTLEFIELD, fromZone = Zone.GRAVEYARD)
        )
    )

    spell {
        effect = ModalEffect.chooseOne(
            // Mode 1: No gift — return up to 2 creature cards with MV ≤ 2
            Mode(
                effect = returnEffect,
                targetRequirements = listOf(
                    TargetObject(count = 2, optional = true, filter = graveyardFilter)
                ),
                description = "Don't promise a gift — return up to two creature cards with mana value 2 or less from your graveyard to the battlefield"
            ),
            // Mode 2: Gift a card — opponent draws, return up to 3 creature cards with MV ≤ 2
            Mode(
                effect = DrawCardsEffect(1, EffectTarget.PlayerRef(Player.EachOpponent))
                    .then(returnEffect)
                    .then(Effects.GiftGiven()),
                targetRequirements = listOf(
                    TargetObject(count = 3, optional = true, filter = graveyardFilter)
                ),
                description = "Promise a gift — opponent draws a card, return up to three creature cards with mana value 2 or less from your graveyard to the battlefield"
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "10"
        artist = "Chris Rallis"
        imageUri = "https://cards.scryfall.io/normal/front/6/6/666aefc2-44e0-4c27-88d5-7906f245a71f.jpg?1721425815"
    }
}
