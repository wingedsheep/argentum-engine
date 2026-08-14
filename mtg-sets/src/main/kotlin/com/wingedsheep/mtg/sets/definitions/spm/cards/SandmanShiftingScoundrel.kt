package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedBy
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Sandman, Shifting Scoundrel
 * {1}{G}{G}
 * Legendary Creature — Sand Elemental Villain
 * Power/toughness: star/star
 *
 * Sandman's power and toughness are each equal to the number of lands you control.
 * Sandman can't be blocked by creatures with power 2 or less.
 * {3}{G}{G}: Return this card and target land card from your graveyard to the battlefield tapped.
 *
 * The `*`/`*` is a characteristic-defining ability (cf. [com.wingedsheep.mtg.sets.definitions.inv.cards.MolimoMaroSorcerer]):
 * `dynamicStats` sets base power and toughness to the same dynamic value (lands you control) in
 * Layer 7b. Evasion is a [CantBeBlockedBy] static keyed on blocker power via
 * [GameObjectFilter.Creature.powerAtMost]. The recursion ability is a graveyard-zone activated
 * ability (`activateFromZone = Zone.GRAVEYARD`, cf.
 * [com.wingedsheep.mtg.sets.definitions.lci.cards.UchbenbakTheGreatMistake]) that moves Sandman
 * itself and a targeted land card from your graveyard onto the battlefield tapped
 * ([ZonePlacement.Tapped]).
 */
val SandmanShiftingScoundrel = card("Sandman, Shifting Scoundrel") {
    manaCost = "{1}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Sand Elemental Villain"
    oracleText = "Sandman's power and toughness are each equal to the number of lands you control.\n" +
        "Sandman can't be blocked by creatures with power 2 or less.\n" +
        "{3}{G}{G}: Return this card and target land card from your graveyard to the battlefield tapped."

    dynamicStats(DynamicAmounts.landsYouControl())

    staticAbility {
        ability = CantBeBlockedBy(GameObjectFilter.Creature.powerAtMost(2))
    }

    activatedAbility {
        cost = Costs.Mana("{3}{G}{G}")
        activateFromZone = Zone.GRAVEYARD
        val landTarget = target(
            "land",
            TargetObject(filter = TargetFilter(GameObjectFilter.Land.ownedByYou(), zone = Zone.GRAVEYARD)),
        )
        effect = Effects.Composite(
            Effects.Move(
                EffectTarget.Self,
                Zone.BATTLEFIELD,
                placement = ZonePlacement.Tapped,
                fromZone = Zone.GRAVEYARD,
            ),
            Effects.Move(
                landTarget,
                Zone.BATTLEFIELD,
                placement = ZonePlacement.Tapped,
                fromZone = Zone.GRAVEYARD,
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "112"
        artist = "Bartek Fedyczak"
        imageUri = "https://cards.scryfall.io/normal/front/6/0/609ac18c-ec58-4fa7-bbee-3912a69d0ec6.jpg?1783905324"
    }
}
