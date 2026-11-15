package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Orc Sureshot
 * {3}{B}
 * Creature — Orc Archer
 * 4/2
 * Whenever another creature you control enters, target creature an opponent controls gets -1/-1 until end of turn.
 */
val OrcSureshot = card("Orc Sureshot") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Orc Archer"
    power = 4
    toughness = 2
    oracleText = "Whenever another creature you control enters, target creature an opponent controls gets -1/-1 until end of turn."

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.youControl()).enters()
        val victim = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.ModifyStats(-1, -1, victim)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "78"
        artist = "Kev Walker"
        flavorText = "\"Words have yet to halt an arrow.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/9/292326cd-4b90-40c6-88b1-f8560fde236c.jpg"
    }
}
