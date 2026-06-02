package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.Effects

/**
 * Ruinous Rampage
 * {1}{R}{R}
 * Sorcery
 * Choose one —
 * • Ruinous Rampage deals 3 damage to each opponent.
 * • Exile all artifacts with mana value 3 or less.
 */
val RuinousRampage = card("Ruinous Rampage") {
    manaCost = "{1}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Choose one —\n• Ruinous Rampage deals 3 damage to each opponent.\n• Exile all artifacts with mana value 3 or less."

    spell {
        effect = ModalEffect.chooseOne(
            Mode.noTarget(
                DealDamageEffect(3, EffectTarget.PlayerRef(Player.EachOpponent)),
                "Ruinous Rampage deals 3 damage to each opponent"
            ),
            Mode.noTarget(
                Effects.ForEachInGroup(
                    filter = GroupFilter(GameObjectFilter.Artifact.manaValueAtMost(3)),
                    effect = Effects.Move(EffectTarget.Self, Zone.EXILE)
                ),
                "Exile all artifacts with mana value 3 or less"
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "158"
        artist = "David Astruga"
        flavorText = "Kav battle rhetoric teaches that there are no malfunctions on the battlefield, just equipment that need to be utilized differently."
        imageUri = "https://cards.scryfall.io/normal/front/9/1/91d7a4c2-1a4b-4e9f-b543-225b6906752f.jpg?1752947194"
    }
}
