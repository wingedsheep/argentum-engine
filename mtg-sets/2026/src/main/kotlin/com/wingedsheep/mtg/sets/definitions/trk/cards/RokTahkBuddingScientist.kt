package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Rok-Tahk, Budding Scientist
 * {4}{G}{G}
 * Legendary Creature — Brikar Scientist
 * 5/5
 *
 * Creatures you control have "{T}: Add {G}{G}."
 * When Rok-Tahk enters, create Murf, a legendary 0/1 green Ooze Slug creature token with
 * indestructible.
 */
val RokTahkBuddingScientist = card("Rok-Tahk, Budding Scientist") {
    manaCost = "{4}{G}{G}"
    typeLine = "Legendary Creature — Brikar Scientist"
    colorIdentity = "G"
    oracleText = "Creatures you control have \"{T}: Add {G}{G}.\"\n" +
        "When Rok-Tahk enters, create Murf, a legendary 0/1 green Ooze Slug creature token with indestructible."
    power = 5
    toughness = 5
    staticAbility {
        ability = GrantActivatedAbility(
            ability = ActivatedAbility(
                id = AbilityId.next(),
                cost = Costs.Tap,
                effect = Effects.AddMana(Color.GREEN, 2),
                isManaAbility = true,
                timing = TimingRule.ManaAbility
            ),
            filter = GroupFilter(GameObjectFilter.Creature.youControl())
        )
    }
    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 0, toughness = 1,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Ooze", "Slug"),
            keywords = setOf(Keyword.INDESTRUCTIBLE),
            name = "Murf",
            legendary = true,
            imageUri = "https://cards.scryfall.io/normal/front/d/1/d1b52f55-6147-4da6-983c-6ca3e1867723.jpg?1793363930",
        )
    }
    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "211"
        artist = "Andreas Zafiratos"
        flavorText = "\"You have to expect the unexpected in my field. Because before the right answer, we often find the wrong one. But the wrong one can lead us to entirely new discoveries.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/8/787bd19b-586e-4451-850b-60e0857bada0.jpg?1793273846"
    }
}
