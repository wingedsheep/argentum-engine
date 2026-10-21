package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Shambling Ghast
 * {B}
 * Creature — Zombie
 * 1/1
 * When this creature dies, choose one —
 * • Target creature an opponent controls gets -1/-1 until end of turn.
 * • Create a Treasure token. (It's an artifact with "{T}, Sacrifice this token: Add one mana of any color.")
 */
val ShamblingGhast = card("Shambling Ghast") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie"
    oracleText = "When this creature dies, choose one —\n" +
        "• Target creature an opponent controls gets -1/-1 until end of turn.\n" +
        "• Create a Treasure token. (It's an artifact with \"{T}, Sacrifice this token: Add one mana of any color.\")"
    power = 1
    toughness = 1
    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = ModalEffect.chooseOne(
            mode("Target creature an opponent controls gets -1/-1 until end of turn.") {
                val creature = target(TargetFilter.CreatureOpponentControls)
                effect = Effects.ModifyStats(-1, -1, creature)
            },
            Mode.noTarget(Effects.CreateTreasure(), "Create a Treasure token."),
        )
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "119"
        artist = "Dave Kendall"
        imageUri = "https://cards.scryfall.io/normal/front/d/9/d96198a7-dd19-4940-bf8f-23135011fc84.jpg?1783926489"
    }
}
