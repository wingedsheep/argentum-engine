package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Reckless Pyrosurfer
 * {1}{R}
 * Creature — Human Scout
 * 2/2
 * Haste
 * Landfall — Whenever a land you control enters, this creature gains battle cry until end of turn.
 * (Whenever this creature attacks, each other attacking creature gets +1/+0 until end of turn.
 * Each instance of battle cry triggers separately.)
 *
 * Battle cry has no engine keyword; it is modeled from its reminder text (Sanguine Evangelist's
 * shape) and granted to this creature as an until-end-of-turn triggered ability. Each landfall
 * appends its own grant to `grantedTriggeredAbilities`, so two lands give two separate battle
 * cry instances that each trigger on attack — "Each instance of battle cry triggers separately."
 */
val RecklessPyrosurfer = card("Reckless Pyrosurfer") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Scout"
    power = 2
    toughness = 2
    oracleText = "Haste\n" +
        "Landfall — Whenever a land you control enters, this creature gains battle cry until end of turn. " +
        "(Whenever this creature attacks, each other attacking creature gets +1/+0 until end of turn. " +
        "Each instance of battle cry triggers separately.)"

    keywords(Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        effect = Effects.GrantTriggeredAbility(
            ability = grantedTriggeredAbility {
                trigger = Triggers.self.attacks()
                effect = Effects.ForEachInGroup(
                    GroupFilter(
                        baseFilter = GameObjectFilter.Creature.attacking(),
                        excludeSelf = true,
                    ),
                    Effects.ModifyStats(1, 0, EffectTarget.IterationEntity),
                )
            },
            target = EffectTarget.Self,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "133"
        artist = "L.A. Draws"
        imageUri = "https://cards.scryfall.io/normal/front/2/d/2d1bb8ac-7125-4537-b2da-e23a8c28df79.jpg?1783911268"
    }
}
