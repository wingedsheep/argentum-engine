package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

val UnnaturalAggression = card("Unnatural Aggression") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Devoid (This card has no color.)\n" +
        "Target creature you control fights target creature an opponent controls. " +
        "If the creature an opponent controls would die this turn, exile it instead."

    keywords(Keyword.DEVOID)

    spell {
        val yours = target(TargetFilter(GameObjectFilter.Creature.youControl()))
        val theirs = target(TargetFilter(GameObjectFilter.Creature.opponentControls()))
        // The replacement applies even when our target is illegal and no fight happens.
        // Install it before damage so lethal fight damage also exiles the opposing creature.
        effect = Effects.MarkExileOnDeath(theirs) then Effects.Fight(yours, theirs)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "168"
        artist = "James Ryman"
        flavorText = "The battle served as a grim reminder of what a final victory for the Eldrazi would mean."
        imageUri = "https://cards.scryfall.io/normal/front/8/2/8293c66d-9a9b-4817-9bc3-ffd57fda290c.jpg?1783938189"
        ruling("2015-08-25", "If the creature an opponent controls survives the fight but would die later in the turn for another reason, it will be exiled instead, whether your creature dealt it damage or not.")
        ruling("2015-08-25", "If the creature you control becomes an illegal target (perhaps due to being destroyed by another spell or ability), Unnatural Aggression will resolve, but the target creature an opponent controls will neither deal nor receive any damage. If that creature would die later in the turn for another reason, it will be exiled instead.")
    }
}
