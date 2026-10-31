package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Syr Gwyn, Hero of Ashvale — Throne of Eldraine #330
 * {3}{R}{W}{B} · Legendary Creature — Human Knight · 5 / 5
 *
 * Vigilance, menace
 * Whenever an equipped creature you control attacks, you draw a card and you lose 1 life.
 * Equipment you control have equip Knight {0}.
 *
 * The attack trigger is per equipped attacker (one draw each, however many Equipment it carries).
 * "Have equip Knight {0}" grants each Equipment an extra "Equip [quality]" ability (CR 702.6c) beside
 * its printed ones — the Puresteel Paladin / Astor grant shape with Steelclaw Lance's Knight filter.
 */
val SyrGwynHeroOfAshvale = card("Syr Gwyn, Hero of Ashvale") {
    manaCost = "{3}{R}{W}{B}"
    colorIdentity = "RWB"
    typeLine = "Legendary Creature — Human Knight"
    power = 5
    toughness = 5
    oracleText = "Vigilance, menace\n" +
        "Whenever an equipped creature you control attacks, you draw a card and you lose 1 life.\n" +
        "Equipment you control have equip Knight {0}."

    keywords(Keyword.VIGILANCE, Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl().equipped()).attacks()
        effect = Effects.DrawCards(1) then Effects.LoseLife(1, EffectTarget.Controller)
    }

    staticAbility {
        ability = GrantActivatedAbility(
            ability = ActivatedAbility.equip(
                ManaCost.parse("{0}"),
                quality = "Knight",
                targetFilter = TargetFilter.CreatureYouControl.withSubtype(Subtype.KNIGHT),
            ),
            filter = GroupFilter(GameObjectFilter.Artifact.withSubtype(Subtype.EQUIPMENT).youControl()),
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "330"
        artist = "Lie Setiawan"
        flavorText = "Squires throughout the realm aspire to her mix of panache and martial prowess."
        imageUri = "https://cards.scryfall.io/normal/front/a/3/a33add37-379d-4a90-9c04-529dff676986.jpg?1783932546"
        ruling(
            "2019-10-04",
            "Syr Gwyn's triggered ability triggers once per attacking creature that's equipped, " +
                "regardless of how many Equipment those creatures each carry.",
        )
        ruling("2019-10-04", "Equipment you control still have their other equip abilities in addition to equip Knight {0}.")
        ruling(
            "2019-10-04",
            "Whether the target creature is a Knight is checked only as the equip Knight ability is activated " +
                "and as that ability resolves. If the creature somehow becomes a non-Knight creature later, " +
                "the Equipment remains attached to it.",
        )
        ruling(
            "2019-10-04",
            "If Syr Gwyn leaves the battlefield after an equip Knight ability has been activated, that ability " +
                "still resolves even though the Equipment has lost that ability.",
        )
    }
}
