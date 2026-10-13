package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Supertype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CopyExceptions
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ratadrabik of Urborg
 * {2}{W}{B}
 * Legendary Creature — Zombie Wizard
 * 3/3
 * Vigilance, ward {2}
 * Other Zombies you control have vigilance.
 * Whenever another legendary creature you control dies, create a token that's a copy of that
 * creature, except it's not legendary and it's a 2/2 black Zombie in addition to its other colors
 * and types.
 */
val RatadrabikOfUrborg = card("Ratadrabik of Urborg") {
    manaCost = "{2}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Legendary Creature — Zombie Wizard"
    power = 3
    toughness = 3
    oracleText = "Vigilance, ward {2}\n" +
        "Other Zombies you control have vigilance.\n" +
        "Whenever another legendary creature you control dies, create a token that's a copy of that " +
        "creature, except it's not legendary and it's a 2/2 black Zombie in addition to its other " +
        "colors and types."

    keywords(Keyword.VIGILANCE)
    keywordAbility(KeywordAbility.Ward(WardCost.Mana("{2}")))

    staticAbility {
        ability = GrantKeyword(
            Keyword.VIGILANCE,
            GroupFilter.AllCreaturesYouControl.withSubtype("Zombie").other()
        )
    }

    // The dead creature is copied from its departure snapshot (last-known copiable values,
    // CR 608.2h) — counters, Auras and non-copy effects are not copied.
    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.legendary().youControl()).dies()
        effect = Effects.CreateTokenCopyOfTarget(
            target = EffectTarget.TriggeringEntity,
            exceptions = CopyExceptions(
                removedSupertypes = setOf(Supertype.LEGENDARY),
                powerOverride = 2,
                toughnessOverride = 2,
                addedColors = setOf(Color.BLACK),
                addedSubtypes = setOf(Subtype.ZOMBIE),
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "213"
        artist = "Anna Pavleeva"
        imageUri = "https://cards.scryfall.io/normal/front/9/3/9315812d-03e8-4eb4-a693-e7adf281f7fb.jpg?1783921278"
        ruling("2022-09-09", "The token copies the creature as it last existed on the battlefield before it died, not as it exists in the graveyard.")
        ruling("2022-09-09", "Except for power and toughness, the token copies only what was printed on the original creature (unless that creature is copying something else) and adds the color black and the creature type Zombie. It doesn't copy counters, Auras, Equipment, or non-copy effects.")
    }
}
