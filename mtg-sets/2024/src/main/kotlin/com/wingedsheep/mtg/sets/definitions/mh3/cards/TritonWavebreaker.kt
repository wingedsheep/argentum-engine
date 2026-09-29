package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

val TritonWavebreaker = card("Triton Wavebreaker") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Enchantment Creature — Merfolk Wizard"
    power = 1
    toughness = 1
    oracleText = "Bestow {1}{U} (If you cast this card for its bestow cost, it's an Aura spell with enchant creature. It becomes a creature again if it's not attached.)\nAs long as this permanent is a creature, it has prowess. (Whenever you cast a noncreature spell, this creature gets +1/+1 until end of turn.)\nEnchanted creature gets +1/+1 and has prowess."

    keywordAbility(KeywordAbility.bestow("{1}{U}"))

    // The Aura itself has no prowess; only its enchanted creature does.
    val isCreature = Conditions.SourceMatches(GameObjectFilter.Creature)
    staticAbility {
        ability = ConditionalStaticAbility(
            GrantKeyword(Keyword.PROWESS, GroupFilter.source()), isCreature
        )
    }
    staticAbility {
        ability = ConditionalStaticAbility(
            GrantTriggeredAbility(
                TriggeredAbility.create(
                    trigger = Triggers.you.casts(GameObjectFilter.Noncreature),
                    effect = Effects.ModifyStats(1, 1, EffectTarget.Self)
                ),
                GroupFilter.source()
            ),
            isCreature
        )
    }
    staticAbility { ability = ModifyStats(1, 1) }
    staticAbility { ability = GrantKeyword(Keyword.PROWESS) }
    staticAbility {
        ability = GrantTriggeredAbility(
            TriggeredAbility.create(
                trigger = Triggers.you.casts(GameObjectFilter.Noncreature),
                effect = Effects.ModifyStats(1, 1, EffectTarget.Self)
            )
        )
    }

    metadata {
        ruling("2024-06-07", "Unlike other Auras, an Aura with bestow isn't put into its owner's graveyard if it becomes unattached. Rather, the effect making it an Aura ends, it loses enchant creature, and it remains on the battlefield as an enchantment creature. It can attack (and its {T} abilities can be activated, if it has any) on the turn it becomes unattached if it's been under your control continuously, even as an Aura, since your most recent turn began.")
        ruling("2024-06-07", "Unlike other Aura spells, an Aura spell with bestow isn't countered if its target is illegal as it begins to resolve. Rather, the effect making it an Aura spell ends, it loses enchant creature, it returns to being an enchantment creature spell, and it resolves and enters the battlefield as an enchantment creature.")
        rarity = Rarity.UNCOMMON
        collectorNumber = "74"
        artist = "Christina Kraus"
        imageUri = "https://cards.scryfall.io/normal/front/4/1/41e68700-c71c-46db-b6a4-beae924cf97a.jpg?1783911287"
    }
}
