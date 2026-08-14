package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Pollen-Shield Hare // Hare Raising
 * {1}{W}
 * Creature — Rabbit
 * 2/2
 * Creature tokens you control get +1/+1.
 *
 * Adventure: Hare Raising — {G}, Sorcery — Adventure
 * Target creature you control gains vigilance and gets +X/+X until end of turn, where X is the
 * number of creatures you control.
 *
 * The anthem is a plain layer-7c [ModifyStats] over `Creature.youControl().token()`; the Hare
 * itself is not a token, so it never pumps itself. On the Adventure, X is counted on resolution
 * ([DynamicAmounts.creaturesYouControl]) and includes the target itself.
 *
 * (CR 715: Adventure cards. Casting the Adventure exiles the card on resolution and lets the
 * caster cast it as the creature spell while it remains in exile.)
 */
val PollenShieldHare = card("Pollen-Shield Hare") {
    manaCost = "{1}{W}"
    colorIdentity = "GW"
    typeLine = "Creature — Rabbit"
    oracleText = "Creature tokens you control get +1/+1."
    power = 2
    toughness = 2

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(GameObjectFilter.Creature.youControl().token())
        )
    }

    adventure("Hare Raising") {
        manaCost = "{G}"
        typeLine = "Sorcery — Adventure"
        oracleText = "Target creature you control gains vigilance and gets +X/+X until end of " +
            "turn, where X is the number of creatures you control. " +
            "(Then exile this card. You may cast the creature later from exile.)"
        spell {
            val t = target("target", Targets.CreatureYouControl)
            effect = Effects.Composite(
                Effects.GrantKeyword(Keyword.VIGILANCE, t),
                Effects.ModifyStats(
                    DynamicAmounts.creaturesYouControl(),
                    DynamicAmounts.creaturesYouControl(),
                    t
                )
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "233"
        artist = "Olena Richards"
        flavorText = "She can drive off any threat, from owl to hunter to Phyrexian monster."
        imageUri = "https://cards.scryfall.io/normal/front/1/3/139f31f5-28f0-4e90-abdf-3f6ed0992dea.jpg?1783915063"
    }
}
