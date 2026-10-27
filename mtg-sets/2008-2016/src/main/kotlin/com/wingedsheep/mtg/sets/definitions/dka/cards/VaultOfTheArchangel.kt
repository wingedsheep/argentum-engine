package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Vault of the Archangel — Dark Ascension #158
 * Land
 * {T}: Add {C}.
 * {2}{W}{B}, {T}: Creatures you control gain deathtouch and lifelink until end of turn.
 *
 * The grant affects only the creatures you control as the ability resolves (the Ankle Shanker
 * shape — two group keyword grants in sequence).
 */
val VaultOfTheArchangel = card("Vault of the Archangel") {
    typeLine = "Land"
    colorIdentity = "WB"
    oracleText = "{T}: Add {C}.\n{2}{W}{B}, {T}: Creatures you control gain deathtouch and lifelink until end of turn."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}{W}{B}"),
            Costs.Tap
        )
        effect = Patterns.Group.grantKeywordToAll(
            keyword = Keyword.DEATHTOUCH,
            filter = GroupFilter.AllCreaturesYouControl
        ) then Patterns.Group.grantKeywordToAll(
            keyword = Keyword.LIFELINK,
            filter = GroupFilter.AllCreaturesYouControl
        )
        description = "Creatures you control gain deathtouch and lifelink until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "158"
        artist = "John Avon"
        flavorText = "\"For centuries my creation kept this world in balance. Now only her shadow remains.\"\n—Sorin Markov"
        imageUri = "https://cards.scryfall.io/normal/front/3/5/35a65437-430a-42ef-854f-6e66f8e1a04a.jpg?1783940789"
        ruling("2011-01-22", "Multiple instances of deathtouch or lifelink on the same creature are redundant.")
        ruling(
            "2011-01-22",
            "Vault of the Archangel's last ability affects only creatures you control when that ability resolves. It won't affect creatures that come under your control later in the turn."
        )
    }
}
