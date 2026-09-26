package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ManaSpellRider

/**
 * Hall of the Bandit Lord — Champions of Kamigawa #277
 * Legendary Land
 * Hall of the Bandit Lord enters tapped.
 * {T}, Pay 3 life: Add {C}. If that mana is spent on a creature spell, it gains haste.
 *
 * Carnelian Orb of Dragonkind's rider with no end date: the haste doesn't wear off at end of turn
 * (2004-12-01 ruling), so the permanent keeps it until it leaves the battlefield.
 */
val HallOfTheBanditLord = card("Hall of the Bandit Lord") {
    manaCost = ""
    colorIdentity = ""
    typeLine = "Legendary Land"
    oracleText = "Hall of the Bandit Lord enters tapped.\n" +
        "{T}, Pay 3 life: Add {C}. If that mana is spent on a creature spell, it gains haste."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.PayLife(3))
        effect = Effects.AddColorlessMana(
            1,
            riders = setOf(
                ManaSpellRider.GrantsKeywordWhenSpent(
                    keyword = Keyword.HASTE,
                    spellFilter = GameObjectFilter.Creature,
                    duration = Duration.Permanent,
                )
            )
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "277"
        artist = "Paolo Parente"
        imageUri = "https://cards.scryfall.io/normal/front/5/9/59fa5bab-8626-4b45-a3a3-621f6d9509ab.jpg?1783944274"
        ruling("2004-12-01", "The effect that grants haste doesn’t wear off at end of turn.")
        ruling("2004-12-01", "The creature has haste if the mana is spent to cover any cost of the spell, even an additional cost.")
    }
}
