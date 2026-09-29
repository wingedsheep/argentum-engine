package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kogla and Yidaro — March of the Machine #244
 * Legendary Creature — Ape Dinosaur Turtle 7/7 · Rare
 *
 * The discard ability is activated from hand (channel shape, [Costs.DiscardSelf] +
 * `activateFromZone = HAND`). By resolution the card sits in the graveyard, so
 * `ShuffleIntoLibrary(Self)` reads it there; if it has left the graveyard the move is a no-op
 * and the draw still happens. "Up to one" target means the shuffle + draw run with no target.
 */
val KoglaAndYidaro = card("Kogla and Yidaro") {
    manaCost = "{2}{R}{R}{G}{G}"
    colorIdentity = "RG"
    typeLine = "Legendary Creature — Ape Dinosaur Turtle"
    power = 7
    toughness = 7
    oracleText = "When Kogla and Yidaro enters, choose one —\n" +
        "• It gains trample and haste until end of turn.\n" +
        "• It fights target creature you don't control.\n" +
        "{2}{R}{G}, Discard this card: Destroy up to one target artifact or enchantment. " +
        "Shuffle this card into your library from your graveyard, then draw a card."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = ModalEffect.chooseOne(
            mode("It gains trample and haste until end of turn.") {
                effect = Effects.GrantKeyword(Keyword.TRAMPLE, EffectTarget.Self) then
                    Effects.GrantKeyword(Keyword.HASTE, EffectTarget.Self)
            },
            mode("It fights target creature you don't control.") {
                val theirs = target(TargetFilter.CreatureOpponentControls)
                effect = Effects.Fight(EffectTarget.Self, theirs)
            }
        )
        description = "When Kogla and Yidaro enters, choose one — it gains trample and haste until " +
            "end of turn; or it fights target creature you don't control."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{R}{G}"), Costs.DiscardSelf)
        activateFromZone = Zone.HAND
        val t = target(TargetFilter.ArtifactOrEnchantment, optional = true)
        effect = Effects.Destroy(t) then
            Effects.ShuffleIntoLibrary(EffectTarget.Self) then
            Effects.DrawCards(1)
        description = "{2}{R}{G}, Discard this card: Destroy up to one target artifact or enchantment. " +
            "Shuffle this card into your library from your graveyard, then draw a card."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "244"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/front/b/7/b760ebdf-bea6-4c43-a187-4a02ebf95ebf.jpg?1783916942"
    }
}
