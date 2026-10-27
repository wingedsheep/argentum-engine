package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedActivatedAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Firja's Retribution
 * {1}{W}{W}{B}
 * Enchantment — Saga
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)
 * I — Create a 4/4 white Angel Warrior creature token with flying and vigilance.
 * II — Until end of turn, Angels you control gain "{T}: Destroy target creature with power less
 *      than this creature's power."
 * III — Angels you control gain double strike until end of turn.
 *
 * "Angels you control" is the bare creature-type noun, so the group is every Angel permanent you
 * control at resolution (later Angels gain nothing — the group is locked in by the grant). The granted
 * ability's "this creature" is [EffectTarget.Self] — the Angel the ability is on — and
 * [GameObjectFilter.powerLessThanEntity] compares strictly against its projected power, falling back
 * to last-known power if the Angel has left the battlefield by resolution.
 */
val FirjasRetribution = card("Firja's Retribution") {
    manaCost = "{1}{W}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I — Create a 4/4 white Angel Warrior creature token with flying and vigilance.\n" +
        "II — Until end of turn, Angels you control gain \"{T}: Destroy target creature with power " +
        "less than this creature's power.\"\n" +
        "III — Angels you control gain double strike until end of turn."

    val angelsYouControl = GroupFilter(GameObjectFilter.Permanent.withSubtype("Angel").youControl())

    sagaChapter(1) {
        effect = Effects.CreateToken(
            power = 4,
            toughness = 4,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Angel", "Warrior"),
            keywords = setOf(Keyword.FLYING, Keyword.VIGILANCE),
            imageUri = "https://cards.scryfall.io/normal/front/3/b/3bd871a3-3802-4bdc-8033-3ecfd01c0449.jpg?1783928084",
        )
    }

    sagaChapter(2) {
        effect = Effects.GrantActivatedAbilityToGroup(
            ability = grantedActivatedAbility {
                cost = Costs.Tap
                val creature = target(TargetFilter(GameObjectFilter.Creature.powerLessThanEntity(EffectTarget.Self)))
                effect = Effects.Destroy(creature)
            },
            filter = angelsYouControl,
            duration = Duration.EndOfTurn,
        )
    }

    sagaChapter(3) {
        effect = Effects.ForEachInGroup(
            filter = angelsYouControl,
            effect = Effects.GrantKeyword(Keyword.DOUBLE_STRIKE, EffectTarget.IterationEntity, Duration.EndOfTurn),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "210"
        artist = "Anna Steinbauer"
        imageUri = "https://cards.scryfall.io/normal/front/0/3/03dff23e-d7e3-4d75-a9dc-fdf4a42f31e0.jpg?1783928197"
        ruling(
            "2021-02-05",
            "Only Angels you control as the chapter II ability resolves will gain the activated ability. " +
                "Similarly, only Angels you control as the chapter III ability resolves will gain double strike. " +
                "In both cases, Angels that come under your control later in the turn won't gain the ability."
        )
        ruling(
            "2021-02-05",
            "The power of the target creature must be less than the power of the Angel as you activate the " +
                "ability. As the ability tries to resolve, the powers are compared again. If the power of the " +
                "target creature is no longer less than the power of the Angel, the ability won't resolve. If " +
                "the Angel is no longer on the battlefield at that time, use its power as it last existed on " +
                "the battlefield to determine if the ability resolves."
        )
    }
}
