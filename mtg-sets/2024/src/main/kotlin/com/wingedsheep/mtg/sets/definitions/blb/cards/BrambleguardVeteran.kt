package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.GrantKeywordEffect
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns

/**
 * Brambleguard Veteran
 * {1}{G}{G}
 * Creature — Raccoon Warrior
 * 3/4
 * Whenever you expend 4, Raccoons you control get +1/+1 and gain vigilance
 * until end of turn.
 */
val BrambleguardVeteran = card("Brambleguard Veteran") {
    manaCost = "{1}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Raccoon Warrior"
    power = 3
    toughness = 4
    oracleText = "Whenever you expend 4, Raccoons you control get +1/+1 and gain vigilance until end of turn. (You expend 4 as you spend your fourth total mana to cast spells during a turn.)"

    // A bare tribal noun names *permanents* of that tribe, not creatures of it — the
    // reading the Assay differential settled across the corpus. Unobservable today, since every
    // printed Raccoon is a creature.
    val raccoonsYouControl = GroupFilter(GameObjectFilter.Permanent.withSubtype("Raccoon").youControl())

    triggeredAbility {
        trigger = Triggers.Expend(4)
        effect = Patterns.Group.pumpAndGrantToAll(1, 1, Keyword.VIGILANCE, raccoonsYouControl)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "165"
        artist = "Jakob Eirich"
        flavorText = "\"Why should the birdfolk get the best view?\""
        imageUri = "https://cards.scryfall.io/normal/front/b/a/bac9f6f8-6797-4580-9fc4-9a825872e017.jpg?1721426774"
    }
}
