package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Mirkwood — The Hobbit #188
 * Land · Common
 *
 * This land enters tapped.
 * {T}: Add {B} or {G}.
 * {2}{B}{G}, {T}, Sacrifice this land: Put two +1/+1 counters on target Bear, Spider, or Wolf you
 * control. Activate only as a sorcery.
 *
 * The Bear/Spider/Wolf member of the HOB tapland cycle; see [IronHills] for the shape and
 * [GoblinTown] for the multi-tribe target.
 */
val Mirkwood = card("Mirkwood") {
    manaCost = ""
    colorIdentity = "BG"
    typeLine = "Land"
    oracleText = "This land enters tapped.\n" +
        "{T}: Add {B} or {G}.\n" +
        "{2}{B}{G}, {T}, Sacrifice this land: Put two +1/+1 counters on target Bear, Spider, or " +
        "Wolf you control. Activate only as a sorcery."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLACK)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{B}{G}"), Costs.Tap, Costs.SacrificeSelf)
        val beast = target(
            "target Bear, Spider, or Wolf you control",
            TargetCreature(
                filter = TargetFilter(
                    GameObjectFilter.Creature.youControl().withAnySubtype("Bear", "Spider", "Wolf")
                )
            )
        )
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 2, beast)
        timing = TimingRule.SorcerySpeed
        description = "Put two +1/+1 counters on target Bear, Spider, or Wolf you control."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "188"
        artist = "Leon Tukker"
        flavorText = "\"Stick to the forest-track, keep your spirits up, and hope for the best.\"\n" +
            "—Gandalf"
        imageUri = "https://cards.scryfall.io/normal/front/6/1/612cf954-f86c-4629-99df-4874d56fded3.jpg?1785323344"
    }
}
