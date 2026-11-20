package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Champion of Stray Souls — Born of the Gods #63 (canonical printing)
 * {4}{B}{B} · Creature — Skeleton Warrior · 4/4
 *
 * {3}{B}{B}, {T}, Sacrifice X other creatures: Return X target creature cards from your graveyard
 * to the battlefield.
 * {5}{B}{B}: Put this card from your graveyard on top of your library.
 *
 * X is announced by how many creatures the variable sacrifice cost picks (CR 601.2b), and the
 * targets are asked for after that pick with exactly X of them. Targets are chosen before the cost
 * is paid (CR 601.2c before 601.2h), so the creatures being sacrificed can't be among them.
 */
val ChampionOfStraySouls = card("Champion of Stray Souls") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Skeleton Warrior"
    power = 4
    toughness = 4
    oracleText = "{3}{B}{B}, {T}, Sacrifice X other creatures: Return X target creature cards from your " +
        "graveyard to the battlefield.\n{5}{B}{B}: Put this card from your graveyard on top of your library."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{3}{B}{B}"),
            Costs.Tap,
            Costs.SacrificePermanents(GameObjectFilter.Creature, minCount = 0, excludeSelf = true),
        )
        targets(TargetFilter.CreatureInYourGraveyard, exactly = DynamicAmounts.xValue())
        effect = Effects.Pipeline {
            move(gather(CardSource.ChosenTargets), CardDestination.ToZone(Zone.BATTLEFIELD))
        }
        description = "{3}{B}{B}, {T}, Sacrifice X other creatures: Return X target creature cards from " +
            "your graveyard to the battlefield."
    }

    activatedAbility {
        cost = Costs.Mana("{5}{B}{B}")
        activateFromZone = Zone.GRAVEYARD
        effect = Effects.Move(EffectTarget.Self, Zone.LIBRARY, ZonePlacement.Top)
        description = "{5}{B}{B}: Put this card from your graveyard on top of your library."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "63"
        artist = "Aleksi Briclot"
        imageUri = "https://cards.scryfall.io/normal/front/3/4/342196b9-ab65-4922-864e-00f067153261.jpg?1783939558"
    }
}
