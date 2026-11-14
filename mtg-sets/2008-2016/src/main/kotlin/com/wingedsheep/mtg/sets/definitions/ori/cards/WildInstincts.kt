package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Wild Instincts
 * {3}{G}
 * Sorcery
 * Target creature you control gets +2/+2 until end of turn. It fights target creature an opponent controls.
 */
val WildInstincts = card("Wild Instincts") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Target creature you control gets +2/+2 until end of turn. It fights target creature an " +
        "opponent controls. (Each deals damage equal to its power to the other.)"

    spell {
        val mine = target(TargetFilter.CreatureYouControl)
        val theirs = target(TargetFilter(GameObjectFilter.Creature.opponentControls()))
        effect = Effects.ModifyStats(2, 2, mine) then Effects.Fight(mine, theirs)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "206"
        artist = "Igor Kieryluk"
        flavorText = "Nissa had only the space of a heartbeat to react, her instincts steering her blade."
        imageUri = "https://cards.scryfall.io/normal/front/5/8/58fafd9d-777f-4d88-ad93-63af6d7ef274.jpg?1783938316"
        ruling("2015-06-22", "You must target both a creature you control and a creature an opponent controls to cast Wild Instincts.")
        ruling("2015-06-22", "If the creature an opponent controls is an illegal target as Wild Instincts tries to resolve, but the creature you control is still a legal target, the creature you control will get +2/+2, but the creatures won't fight. Neither creature will deal or be dealt damage during the resolution of Wild Instincts.")
    }
}
