package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Graveyard Shift
 * {4}{B}
 * Sorcery
 * This spell has flash as long as there are five or more mana values among cards in your graveyard.
 * Return target creature card from your graveyard to the battlefield.
 *
 * The flash clause is `conditionalFlash` gated on [Conditions.DistinctManaValuesInGraveyard] —
 * distinct mana values, so two cards sharing a mana value count once (lands count as 0).
 */
val GraveyardShift = card("Graveyard Shift") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "This spell has flash as long as there are five or more mana values among cards " +
        "in your graveyard.\n" +
        "Return target creature card from your graveyard to the battlefield."

    conditionalFlash = Conditions.DistinctManaValuesInGraveyard(5)

    spell {
        val creature = target(TargetFilter.CreatureInYourGraveyard)
        effect = Effects.PutOntoBattlefield(creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "81"
        artist = "Liiga Smilshkalne"
        flavorText = "\"Up and at 'em, buddy. We've got more work to do.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/9/394d7fb3-44ee-409f-a992-27e5aabd9c2b.jpg?1783923129"
        ruling(
            "2022-04-29",
            "The game checks whether it is legal to cast a spell before costs are determined or mana abilities are activated while casting it. If cards are removed from your graveyard after that point, such as by exiling Jack-o'-Lantern from your graveyard to pay for its mana ability, this won't affect whether you can legally cast Graveyard Shift, even if that reduces the number of mana values in your graveyard to below five.",
        )
        ruling(
            "2022-04-29",
            "If an effect allows you to cast Graveyard Shift from your graveyard, you may cast it any time you could cast an instant if you have five or more mana values among cards in your graveyard immediately before you begin to cast it, even if moving it to the stack would reduce that number to four.",
        )
        ruling(
            "2022-04-29",
            "“Five or more mana values among cards in your graveyard” means there are at least five mana values among those cards.",
        )
    }
}
