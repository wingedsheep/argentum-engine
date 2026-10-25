package com.wingedsheep.mtg.sets.definitions.clb.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Jaheira, Friend of the Forest
 * {2}{G}
 * Legendary Creature — Human Elf Druid
 * 2/3
 * Tokens you control have "{T}: Add {G}."
 * Choose a Background
 *
 * A [GrantActivatedAbility] static over every token you control — `GameObjectFilter.Token`, not
 * creature tokens only (per the ruling, Treasures and Clues get it too). The granted ability is an
 * ordinary {T} cost, so a creature token still can't use it the turn it arrives without haste,
 * while a noncreature token can. Choose a Background is the deck-construction partner variant.
 */
val JaheiraFriendOfTheForest = card("Jaheira, Friend of the Forest") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Human Elf Druid"
    power = 2
    toughness = 3
    oracleText = "Tokens you control have \"{T}: Add {G}.\"\n" +
        "Choose a Background (You can have a Background as a second commander.)"

    staticAbility {
        ability = GrantActivatedAbility(
            ability = ActivatedAbility(
                id = AbilityId.next(),
                cost = Costs.Tap,
                effect = Effects.AddMana(Color.GREEN),
                isManaAbility = true,
                timing = TimingRule.ManaAbility
            ),
            filter = GroupFilter(GameObjectFilter.Token.youControl())
        )
    }

    keywordAbility(KeywordAbility.chooseABackground())

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "237"
        artist = "Mila Pesic"
        flavorText = "\"The Harpers' movement grows, but it needs sunlight to blossom. We can be that light.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/e/feb7ad1b-4466-48f7-b46d-cc83d4e22b51.jpg?1783922710"
        ruling("2022-06-10", "Jaheira's ability gives the mana ability to all tokens you control, not just creature tokens.")
        ruling("2022-06-10", "You won't be able to activate the mana abilities of creature tokens you control on the turn they're created unless they have haste. Conversely, you will be able to activate the mana abilities of noncreature tokens you control on the turn they're created (provided they're untapped).")
    }
}
