package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Edgewall Inn
 * Land
 *
 * This land enters tapped.
 * As this land enters, choose a color.
 * {T}: Add one mana of the chosen color.
 * {3}, {T}, Sacrifice this land: Return target card that has an Adventure from your graveyard to
 * your hand.
 *
 * The mana half is the Uncharted Haven shape. [EntersWithChoice] is an "as this enters" replacement,
 * so the color is locked in before the land is ever on the battlefield to tap, and every activation
 * reads that one recorded choice back — it is not a fresh choice per activation.
 *
 * "A card that has an Adventure" is [Filters.HasAdventure] — `CardPredicate.HasAdventure`, a
 * characteristic of the whole card in every zone. That is deliberately *not* the same thing as
 * "instant or sorcery card": per the WOE rulings an adventurer card in a graveyard is a permanent
 * card with its normal characteristics, and the Adventure half is only visible to effects that ask
 * "has an Adventure". Scoped `ownedByYou()` for "from your graveyard".
 */
val EdgewallInn = card("Edgewall Inn") {
    manaCost = ""
    colorIdentity = ""
    typeLine = "Land"
    oracleText = "This land enters tapped.\n" +
        "As this land enters, choose a color.\n" +
        "{T}: Add one mana of the chosen color.\n" +
        "{3}, {T}, Sacrifice this land: Return target card that has an Adventure from your " +
        "graveyard to your hand."

    replacementEffect(EntersTapped())
    replacementEffect(EntersWithChoice(ChoiceType.COLOR))

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddManaOfChosenColor()
        manaAbility = true
        timing = TimingRule.ManaAbility
        description = "{T}: Add one mana of the chosen color."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}"), Costs.Tap, Costs.SacrificeSelf)
        val adventurer = target(
            "target card that has an Adventure in your graveyard",
            TargetObject(
                filter = TargetFilter(Filters.HasAdventure.ownedByYou(), zone = Zone.GRAVEYARD),
            ),
        )
        effect = Effects.Move(adventurer, Zone.HAND)
        description = "{3}, {T}, Sacrifice this land: Return target card that has an Adventure " +
            "from your graveyard to your hand."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "255"
        artist = "Alayna Danner"
        imageUri = "https://cards.scryfall.io/normal/front/e/c/ec435e54-628a-43bd-8804-cbc37e375bce.jpg?1783915055"
    }
}
