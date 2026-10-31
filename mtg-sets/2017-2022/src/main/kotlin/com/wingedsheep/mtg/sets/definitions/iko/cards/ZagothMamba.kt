package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Zagoth Mamba
 * {B}
 * Creature — Nightmare Snake
 * 1/1
 * Whenever this creature mutates, target creature an opponent controls gets -2/-2 until end of turn.
 */
val ZagothMamba = card("Zagoth Mamba") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Creature — Nightmare Snake"
    power = 1
    toughness = 1
    oracleText = "Whenever this creature mutates, target creature an opponent controls gets -2/-2 until end of turn."

    triggeredAbility {
        trigger = Triggers.self.mutates()
        val t = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.ModifyStats(-2, -2, t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "106"
        artist = "Yeong-Hao Han"
        flavorText = "\"It's just a snake. A many-eyed snake with venom that turns your blood to smoke, but still, just a snake.\"\n—Kinnan, bonder prodigy"
        imageUri = "https://cards.scryfall.io/normal/front/c/e/ce945f7e-ded9-4819-9313-d50d3aad40fa.jpg?1783931054"
    }
}
