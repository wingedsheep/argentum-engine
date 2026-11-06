package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.embalm
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Temmet, Vizier of Naktamun — Amonkhet #207
 * {W}{U} · Legendary Creature — Human Cleric · Rare
 * 2/2
 *
 * At the beginning of combat on your turn, target creature token you control gets +1/+1 until end
 * of turn and can't be blocked this turn.
 * Embalm {3}{W}{U}
 *
 * The embalmed token is itself a creature token, so its own trigger may target it.
 */
val TemmetVizierOfNaktamun = card("Temmet, Vizier of Naktamun") {
    manaCost = "{W}{U}"
    colorIdentity = "WU"
    typeLine = "Legendary Creature — Human Cleric"
    power = 2
    toughness = 2
    oracleText = "At the beginning of combat on your turn, target creature token you control gets +1/+1 " +
        "until end of turn and can't be blocked this turn.\n" +
        "Embalm {3}{W}{U} ({3}{W}{U}, Exile this card from your graveyard: Create a token that's a copy of it, " +
        "except it's a white Zombie Human Cleric with no mana cost. Embalm only as a sorcery.)"

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        val t = target(TargetFilter(GameObjectFilter.Creature.token().youControl()))
        effect = Effects.ModifyStats(1, 1, t) then
            Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED, t)
    }

    embalm("{3}{W}{U}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "207"
        artist = "Anna Steinbauer"
        imageUri = "https://cards.scryfall.io/normal/front/6/a/6af12a34-52a1-4940-a8d5-9e9811fcd59f.jpg?1783936460"
    }
}
