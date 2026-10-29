package com.wingedsheep.mtg.sets.definitions.tdc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Nogi, Draco-Zealot — Tarkir: Dragonstorm Commander #226 (canonical printing; its Game Night:
 * Free-for-All debut is a box set, not a scaffoldable expansion)
 * {1}{R}{R} · Legendary Creature — Kobold Shaman · 3/3
 *
 * Dragon spells you cast cost {1} less to cast.
 * Whenever Nogi attacks, if you control three or more Dragons, until end of turn, Nogi becomes a
 * Dragon with base power and toughness 5/5 and gains flying.
 *
 * The cost reduction is Dragonlord's Servant's static. The attack trigger's "if" is an intervening
 * clause, checked when it triggers and again as it resolves. "Becomes a Dragon" (no "in addition to
 * its other types") replaces its creature types (CR 205.1a) — [Effects.SetCreatureSubtypes] in
 * layer 4 — and the 5/5 is a base P/T set in layer 7b, so counters and pumps still apply on top.
 */
val NogiDracoZealot = card("Nogi, Draco-Zealot") {
    manaCost = "{1}{R}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Kobold Shaman"
    power = 3
    toughness = 3
    oracleText = "Dragon spells you cast cost {1} less to cast.\n" +
        "Whenever Nogi attacks, if you control three or more Dragons, until end of turn, Nogi becomes a " +
        "Dragon with base power and toughness 5/5 and gains flying."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.Any.withSubtype("Dragon")),
            modification = CostModification.ReduceGeneric(1),
        )
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        interveningIf = Conditions.YouControlAtLeast(3, GameObjectFilter.Permanent.withSubtype("Dragon"))
        effect = Effects.SetCreatureSubtypes(
            subtypes = setOf(Subtype.DRAGON.value),
            target = EffectTarget.Self,
            duration = Duration.EndOfTurn,
        ) then Effects.SetBasePowerAndToughness(
            power = 5,
            toughness = 5,
            target = EffectTarget.Self,
            duration = Duration.EndOfTurn,
        ) then Effects.GrantKeyword(Keyword.FLYING, EffectTarget.Self, Duration.EndOfTurn)
        description = "Whenever Nogi attacks, if you control three or more Dragons, until end of turn, " +
            "Nogi becomes a Dragon with base power and toughness 5/5 and gains flying."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "226"
        artist = "Dmitry Burmak"
        flavorText = "\"Glory to the Burning Ones! May their mighty fangs devour all foes!\""
        imageUri = "https://cards.scryfall.io/normal/front/8/6/86e576f2-8f9f-4bbf-85c4-b99ecb665412.jpg?1783907063"
    }
}
