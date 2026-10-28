package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Goma Fada Vanguard — Zendikar Rising #141
 * {1}{R} · Creature — Human Warrior · 2/2
 *
 * Whenever this creature attacks, target creature an opponent controls with power less than or
 * equal to the number of Warriors you control can't block this turn.
 *
 * "Warriors you control" is the bare tribal noun, so it counts Warrior permanents (itself
 * included); the cap is read on targeting and again on resolution.
 */
val GomaFadaVanguard = card("Goma Fada Vanguard") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Warrior"
    oracleText = "Whenever this creature attacks, target creature an opponent controls with power less " +
        "than or equal to the number of Warriors you control can't block this turn."
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val creature = target(
            TargetFilter(
                GameObjectFilter.Creature.opponentControls().powerAtMostDynamic(
                    DynamicAmounts.battlefield(
                        Player.You,
                        GameObjectFilter.Permanent.withSubtype(Subtype.WARRIOR),
                    ).count()
                )
            )
        )
        effect = Effects.CantBlock(target = creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "141"
        artist = "Sean Sevestre"
        flavorText = "\"Her weapon's impressive, sure. But you should see her wield it.\"\n—Gred, expedition warrior"
        imageUri = "https://cards.scryfall.io/normal/front/c/e/ce83962b-445a-4b44-ad8a-fada2c0a15b8.jpg?1783929359"
    }
}
