package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CastSpellTypesFromTopOfLibrary
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.LookAtTopOfLibrary
import com.wingedsheep.sdk.scripting.effects.GrantKeywordEffect
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Hakoda, Selfless Commander
 * {3}{W}
 * Legendary Creature — Human Warrior Ally
 * 3/3
 *
 * Vigilance
 * You may look at the top card of your library any time.
 * You may cast Ally spells from the top of your library.
 * Sacrifice Hakoda: Creatures you control get +0/+5 and gain indestructible until end of turn.
 *
 * The two top-of-library clauses are the [LookAtTopOfLibrary] + [CastSpellTypesFromTopOfLibrary]
 * statics. The sacrifice ability mirrors Kamahl, Fist of Krosa's "creatures you control get
 * +X/+Y and gain <keyword> until end of turn" shape — two [Effects.ForEachInGroup] passes over
 * `Creature.youControl()`. Hakoda is already gone (sacrificed as a cost) when the effect
 * resolves, so it correctly excludes itself from the buff.
 */
val HakodaSelflessCommander = card("Hakoda, Selfless Commander") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Human Warrior Ally"
    power = 3
    toughness = 3
    oracleText = "Vigilance\n" +
        "You may look at the top card of your library any time.\n" +
        "You may cast Ally spells from the top of your library.\n" +
        "Sacrifice Hakoda: Creatures you control get +0/+5 and gain indestructible until end of turn."

    keywords(Keyword.VIGILANCE)

    staticAbility {
        ability = LookAtTopOfLibrary
    }
    staticAbility {
        ability = CastSpellTypesFromTopOfLibrary(
            filter = GameObjectFilter.Any.withSubtype(Subtype.ALLY)
        )
    }

    activatedAbility {
        cost = Costs.SacrificeSelf
        effect = Effects.Composite(
            Effects.ForEachInGroup(
                filter = GroupFilter(GameObjectFilter.Creature.youControl()),
                effect = ModifyStatsEffect(0, 5, EffectTarget.Self)
            ),
            Effects.ForEachInGroup(
                filter = GroupFilter(GameObjectFilter.Creature.youControl()),
                effect = GrantKeywordEffect(Keyword.INDESTRUCTIBLE, EffectTarget.Self)
            )
        )
        description = "Sacrifice Hakoda: Creatures you control get +0/+5 and gain indestructible until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "23"
        artist = "Rafater"
        imageUri = "https://cards.scryfall.io/normal/front/9/a/9aef3ddb-9bb7-42c8-975b-b1917955a416.jpg?1764120030"
    }
}
