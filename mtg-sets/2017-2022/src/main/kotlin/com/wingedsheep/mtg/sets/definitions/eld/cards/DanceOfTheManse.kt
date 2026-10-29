package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dance of the Manse — Throne of Eldraine #186
 * {X}{W}{U} · Sorcery
 *
 * Return up to X target artifact and/or non-Aura enchantment cards each with mana value X or less
 * from your graveyard to the battlefield. If X is 6 or more, those permanents are 4/4 creatures in
 * addition to their other types.
 *
 * The target cap reads the announced X (`dynamicMaxCount`, March of Swirling Mist) and each card's
 * mana value is capped by it (`manaValueAtMostX`, Profane Command). Each target returns with the
 * graveyard guard, and when X is 6 or more is made a permanent 4/4 creature that keeps its other
 * types and subtypes — Relive the Past's return-as-creature shape without its added subtype.
 */
val DanceOfTheManse = card("Dance of the Manse") {
    manaCost = "{X}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Sorcery"
    oracleText = "Return up to X target artifact and/or non-Aura enchantment cards each with mana value " +
        "X or less from your graveyard to the battlefield. If X is 6 or more, those permanents are 4/4 " +
        "creatures in addition to their other types."

    spell {
        targets(
            TargetFilter(
                GameObjectFilter.Artifact.manaValueAtMostX().ownedByYou() or
                    GameObjectFilter.Enchantment.notSubtype(Subtype.AURA).manaValueAtMostX().ownedByYou(),
                zone = Zone.GRAVEYARD
            ),
            optional = true,
            dynamicMaxCount = DynamicAmounts.xValue()
        )
        effect = Effects.ForEachTarget(
            Effects.PutOntoBattlefieldFromGraveyard(EffectTarget.ContextTarget(0)) then
                Effects.If(
                    condition = Conditions.CompareAmounts(DynamicAmounts.xValue(), ComparisonOperator.GTE, 6),
                    then = Effects.BecomeCreature(
                        target = EffectTarget.ContextTarget(0),
                        power = 4,
                        toughness = 4,
                        duration = Duration.Permanent
                    )
                )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "186"
        artist = "Yeong-Hao Han"
        imageUri = "https://cards.scryfall.io/normal/front/5/d/5dca90ef-1c17-4dcc-9fef-dab9ee92f590.jpg?1783932599"
        ruling("2019-10-04", "If a card in a player's graveyard has {X} in its mana cost, that X is considered to be 0.")
        ruling("2019-10-04", "An artifact creature or enchantment creature returned this way will be a 4/4 creature.")
        ruling(
            "2019-10-04",
            "An Equipment that's also a creature can't be attached to anything. You can activate its equip " +
                "ability, but it won't become attached."
        )
        ruling(
            "2019-10-04",
            "If another effect causes one of the returned permanents to become a creature and sets its power " +
                "and toughness as it does so, that creature will have that power and toughness; it won't be " +
                "4/4. Notably, crewing a Vehicle does not set its power and toughness, so a Vehicle will " +
                "remain a 4/4 creature if you crew it."
        )
    }
}
