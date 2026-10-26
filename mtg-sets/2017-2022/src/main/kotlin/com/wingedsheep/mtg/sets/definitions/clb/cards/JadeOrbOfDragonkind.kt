package com.wingedsheep.mtg.sets.definitions.clb.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ManaSpellRider
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Jade Orb of Dragonkind
 * {2}{G}
 * Artifact
 *
 * {T}: Add {G}. When you spend this mana to cast a Dragon creature spell, it enters with an
 * additional +1/+1 counter on it and gains hexproof until your next turn.
 *
 * [ManaSpellRider.TriggersWhenSpent] queues the delayed trigger above the Dragon spell. Its counter
 * is owed to the spell and placed as the permanent enters (CR 122.6); the hexproof grant is frozen
 * onto the spell, so the creature has it the moment it exists.
 */
val JadeOrbOfDragonkind = card("Jade Orb of Dragonkind") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Artifact"
    oracleText = "{T}: Add {G}. When you spend this mana to cast a Dragon creature spell, it enters with an " +
        "additional +1/+1 counter on it and gains hexproof until your next turn. (It can't be the target of " +
        "spells or abilities your opponents control.)"

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(
            Color.GREEN,
            riders = setOf(
                ManaSpellRider.TriggersWhenSpent(
                    spellFilter = GameObjectFilter.Creature.withSubtype("Dragon"),
                    effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.TriggeringEntity) then
                        Effects.GrantKeywordToSpell(Keyword.HEXPROOF, EffectTarget.TriggeringEntity, Duration.UntilYourNextTurn),
                )
            ),
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "236"
        artist = "Olena Richards"
        imageUri = "https://cards.scryfall.io/normal/front/1/b/1b32bae4-65e4-4bdb-8bc5-b98a5aac1568.jpg?1783922712"
        ruling("2022-06-10", "The mana created by Jade Orb of Dragonkind can be spent on anything, not just Dragon creature spells.")
        ruling("2022-06-10", "The delayed triggered ability will trigger whether Jade Orb of Dragonkind is still on the battlefield or not.")
        ruling(
            "2022-06-10",
            "If more than one {G} produced by a Jade Orb of Dragonkind is spent to cast a single Dragon creature spell, " +
                "the delayed triggered ability associated with each mana spent will trigger. " +
                "That creature will enter the battlefield with that many +1/+1 counters."
        )
        ruling(
            "2022-06-10",
            "If the mana is spent on a Dragon creature spell, that creature will have hexproof as soon as it enters " +
                "the battlefield. There is no opportunity to target the creature before it gains hexproof."
        )
    }
}
