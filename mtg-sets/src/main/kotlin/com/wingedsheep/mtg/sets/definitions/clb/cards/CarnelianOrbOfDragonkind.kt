package com.wingedsheep.mtg.sets.definitions.clb.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ManaSpellRider

/**
 * Carnelian Orb of Dragonkind
 * {2}{R}
 * Artifact
 *
 * {T}: Add {R}. If that mana is spent on a Dragon creature spell, it gains haste until end of turn.
 *
 * The rider carries the whole ability: the {R} itself is unrestricted (it can pay for anything),
 * and [ManaSpellRider.GrantsKeywordWhenSpent] fires only when the mana lands on a Dragon *creature*
 * spell — matched at payment time, which is what the printed rulings require.
 */
val CarnelianOrbOfDragonkind = card("Carnelian Orb of Dragonkind") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Artifact"
    oracleText = "{T}: Add {R}. If that mana is spent on a Dragon creature spell, it gains haste until end of turn."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(
            Color.RED,
            riders = setOf(
                ManaSpellRider.GrantsKeywordWhenSpent(
                    keyword = Keyword.HASTE,
                    spellFilter = GameObjectFilter.Creature.withSubtype("Dragon"),
                )
            ),
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "166"
        artist = "Olena Richards"
        flavorText = "The essence of a red dragon burns within, always ready to rise with wrath."
        imageUri = "https://cards.scryfall.io/normal/front/e/7/e7e41166-bdaa-4aed-986a-7be1d043240c.jpg?1783922743"
        ruling("2022-06-10", "The mana created by Carnelian Orb of Dragonkind can be spent on anything, not just Dragon creature spells.")
        ruling(
            "2022-06-10",
            "If the mana from Carnelian Orb of Dragonkind is spent to pay any part of the Dragon creature spell's cost, " +
                "including an alternative or additional cost, the Dragon will gain haste until end of turn."
        )
        ruling("2022-06-10", "An instant or sorcery spell is not a creature spell, even if that spell creates Dragon creature tokens.")
        ruling("2022-06-10", "If the mana is spent on a non-Dragon spell that becomes a Dragon later in the turn, that creature won't have haste.")
    }
}
